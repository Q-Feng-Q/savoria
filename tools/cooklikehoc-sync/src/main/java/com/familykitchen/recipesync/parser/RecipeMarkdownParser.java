package com.familykitchen.recipesync.parser;

import com.familykitchen.recipesync.model.SourceIngredient;
import com.familykitchen.recipesync.model.SourceRecipe;
import com.familykitchen.recipesync.model.SourceStep;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.commonmark.node.AbstractVisitor;
import org.commonmark.node.Code;
import org.commonmark.node.Heading;
import org.commonmark.node.Image;
import org.commonmark.node.Link;
import org.commonmark.node.ListBlock;
import org.commonmark.node.ListItem;
import org.commonmark.node.Node;
import org.commonmark.node.Paragraph;
import org.commonmark.node.SoftLineBreak;
import org.commonmark.node.Text;
import org.commonmark.parser.Parser;

/** Parses CookLikeHOC Markdown through an AST and preserves source provenance. */
public final class RecipeMarkdownParser {

  private static final Pattern QUANTITY = Pattern.compile(
      "(?i)(\\d+(?:\\.\\d+)?)\\s*(kg|g|克|千克|斤|个|只|条|ml|毫升|升|l)(?![a-z])");
  private static final Pattern LEADING_STEP_NUMBER = Pattern.compile(
      "^\\s*(?:\\d+[.、．]|[①②③④⑤⑥⑦⑧⑨⑩])\\s*");
  private static final Pattern MINUTES = Pattern.compile("(\\d+)\\s*分(?:钟)?");
  private static final Pattern SECONDS = Pattern.compile("(\\d+)\\s*秒");
  private static final Pattern TEMPERATURE = Pattern.compile("(\\d+)\\s*(?:℃|°C|度)",
      Pattern.CASE_INSENSITIVE);

  private final Parser parser = Parser.builder().build();
  private final RecipeSectionClassifier sectionClassifier = new RecipeSectionClassifier();

  /** Parses one source file relative to the source root. */
  public SourceRecipe parse(Path sourceRoot, Path file) {
    Path normalizedRoot = sourceRoot.toAbsolutePath().normalize();
    Path normalizedFile = file.toAbsolutePath().normalize();
    if (!normalizedFile.startsWith(normalizedRoot)) {
      throw new IllegalArgumentException("Recipe file is outside source root: " + file);
    }
    String sourceKey = normalizePath(normalizedRoot.relativize(normalizedFile));
    Node document = parser.parse(readUtf8Strict(normalizedFile));
    String title = null;
    String imagePath = firstImageDestination(document);
    List<SourceIngredient> ingredients = new ArrayList<>();
    List<SourceStep> steps = new ArrayList<>();
    List<String> components = new ArrayList<>();
    List<String> issues = new ArrayList<>();
    RecipeSectionClassifier.Section section = RecipeSectionClassifier.Section.NONE;
    String stepTitle = null;

    for (Node node = document.getFirstChild(); node != null; node = node.getNext()) {
      if (node instanceof Heading heading) {
        String headingText = textOf(heading).trim();
        if (heading.getLevel() == 1 && title == null) {
          title = headingText;
        }
        RecipeSectionClassifier.Section classified = sectionClassifier.classify(headingText);
        if (classified != RecipeSectionClassifier.Section.NONE) {
          section = classified;
          stepTitle = null;
        } else if (heading.getLevel() > 2 && section == RecipeSectionClassifier.Section.STEPS) {
          stepTitle = LEADING_STEP_NUMBER.matcher(headingText).replaceFirst("").trim();
        } else if (heading.getLevel() <= 2) {
          section = RecipeSectionClassifier.Section.NONE;
          stepTitle = null;
        }
        continue;
      }
      if (section == RecipeSectionClassifier.Section.INGREDIENTS) {
        for (Node itemNode : itemNodes(node)) {
          String item = textOf(itemNode);
          List<String> itemComponents = componentLinks(normalizedRoot, normalizedFile, itemNode);
          if (!itemComponents.isEmpty()) {
            for (String component : itemComponents) {
              if (!components.contains(component)) {
                components.add(component);
              }
            }
            continue;
          }
          if (!item.isBlank()) {
            ingredients.add(toIngredient(sourceKey, ingredients.size() + 1, item));
          }
        }
      } else if (section == RecipeSectionClassifier.Section.STEPS) {
        for (Node itemNode : itemNodes(node)) {
          String item = textOf(itemNode);
          if (!item.isBlank()) {
            steps.add(toStep(steps.size() + 1, stepTitle, item));
          }
        }
      }
    }

    if (title == null || title.isBlank()) {
      issues.add("MISSING_TITLE");
      title = normalizedFile.getFileName().toString().replaceFirst("\\.md$", "");
    }
    String category = sourceKey.contains("/") ? sourceKey.substring(0, sourceKey.indexOf('/')) : "";
    return new SourceRecipe(sourceKey, title, category, imagePath, List.copyOf(ingredients),
        List.copyOf(steps), List.copyOf(components), List.copyOf(issues));
  }

  private SourceIngredient toIngredient(String sourceKey, int index, String sourceText) {
    String normalized = cleanItem(sourceText);
    Matcher matcher = QUANTITY.matcher(normalized);
    boolean hasQuantity = matcher.find();
    String quantity = hasQuantity ? matcher.group().replaceAll("\\s+", "") : null;
    String name = hasQuantity
        ? normalized.substring(0, matcher.start()).replaceAll("[，,：:、\\s]+$", "").trim()
        : normalized;
    if (name.isBlank()) {
      name = normalized;
    }
    return new SourceIngredient(sourceKey + "#ingredient-" + index, name, normalized, quantity);
  }

  private SourceStep toStep(int stepNo, String title, String sourceText) {
    String content = LEADING_STEP_NUMBER.matcher(cleanItem(sourceText)).replaceFirst("");
    return new SourceStep(stepNo, title, content, durationSeconds(content),
        firstMatch(TEMPERATURE, content), heatLevel(content));
  }

  private Integer durationSeconds(String content) {
    Matcher minutes = MINUTES.matcher(content);
    int duration = minutes.find() ? Integer.parseInt(minutes.group(1)) * 60 : 0;
    Matcher seconds = SECONDS.matcher(content);
    duration += seconds.find() ? Integer.parseInt(seconds.group(1)) : 0;
    return duration == 0 ? null : duration;
  }

  private String heatLevel(String content) {
    for (String level : List.of("大火", "中火", "小火", "微火")) {
      if (content.contains(level)) {
        return level;
      }
    }
    return null;
  }

  private String firstMatch(Pattern pattern, String content) {
    Matcher matcher = pattern.matcher(content);
    return matcher.find() ? matcher.group() : null;
  }

  private List<Node> itemNodes(Node node) {
    List<Node> values = new ArrayList<>();
    if (node instanceof ListBlock) {
      for (Node child = node.getFirstChild(); child != null; child = child.getNext()) {
        if (child instanceof ListItem) {
          values.add(child);
        }
      }
    } else if (node instanceof Paragraph) {
      values.add(node);
    }
    return values;
  }

  private List<String> componentLinks(Path root, Path file, Node node) {
    List<String> links = new ArrayList<>();
    node.accept(new AbstractVisitor() {
      @Override
      public void visit(Link link) {
        String destination = URLDecoder.decode(link.getDestination(), StandardCharsets.UTF_8);
        if (destination.toLowerCase(Locale.ROOT).endsWith(".md")) {
          Path resolved = destination.startsWith("/")
              ? root.resolve(destination.substring(1)).normalize()
              : file.getParent().resolve(destination).normalize();
          if (resolved.startsWith(root) && normalizePath(root.relativize(resolved)).startsWith("配料/")) {
            links.add(normalizePath(root.relativize(resolved)));
          }
        }
        visitChildren(link);
      }
    });
    return links;
  }

  private String firstImageDestination(Node document) {
    List<String> destinations = new ArrayList<>(1);
    document.accept(new AbstractVisitor() {
      @Override
      public void visit(Image image) {
        if (destinations.isEmpty()) {
          destinations.add(image.getDestination());
        }
        visitChildren(image);
      }
    });
    return destinations.isEmpty() ? null : destinations.get(0);
  }

  private String textOf(Node node) {
    StringBuilder value = new StringBuilder();
    node.accept(new AbstractVisitor() {
      @Override
      public void visit(Text text) {
        value.append(text.getLiteral());
      }

      @Override
      public void visit(Code code) {
        value.append(code.getLiteral());
      }

      @Override
      public void visit(SoftLineBreak lineBreak) {
        value.append(' ');
      }
    });
    return value.toString();
  }

  private String cleanItem(String value) {
    return Normalizer.normalize(value, Normalizer.Form.NFC)
        .replace('\u00a0', ' ')
        .replaceAll("\\s+", " ")
        .trim();
  }

  private String normalizePath(Path path) {
    return Normalizer.normalize(path.toString().replace('\\', '/'), Normalizer.Form.NFC);
  }

  private String readUtf8Strict(Path file) {
    try {
      byte[] bytes = Files.readAllBytes(file);
      return StandardCharsets.UTF_8.newDecoder()
          .onMalformedInput(CodingErrorAction.REPORT)
          .onUnmappableCharacter(CodingErrorAction.REPORT)
          .decode(ByteBuffer.wrap(bytes))
          .toString();
    } catch (CharacterCodingException exception) {
      throw new IllegalArgumentException("Recipe is not valid UTF-8: " + file, exception);
    } catch (IOException exception) {
      throw new IllegalArgumentException("Unable to read recipe: " + file, exception);
    }
  }
}
