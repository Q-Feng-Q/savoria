package com.familykitchen.recipesync.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Reads legacy-style template rows to prove the committed baseline matches consolidated V4 data. */
public final class LegacyMigrationBaselineExtractor {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final String TEMPLATE_INSERT = "INSERT INTO dish_templates ";
  private static final String INGREDIENT_INSERT = "INSERT INTO dish_template_ingredients ";
  private static final String IMAGE_UPDATE = "UPDATE dish_templates SET ";
  private static final String GENERATED_SECTION_START = "-- BEGIN GENERATED COOKLIKEHOC DATA";

  private LegacyMigrationBaselineExtractor() {
  }

  /** Extracts legacy rows and applies later image metadata updates in migration order. */
  public static LocalTemplateBaseline extract(List<Path> migrations) {
    Map<Long, MutableTemplate> templates = new LinkedHashMap<>();
    for (Path migration : migrations) {
      for (String line : readUtf8Strict(migration).split("\\R")) {
        if (GENERATED_SECTION_START.equals(line)) {
          break;
        } else if (line.startsWith(TEMPLATE_INSERT)) {
          addTemplate(templates, parseInsertValues(line));
        } else if (line.startsWith(INGREDIENT_INSERT)) {
          addIngredient(templates, parseInsertValues(line));
        } else if (line.startsWith(IMAGE_UPDATE)) {
          applyImageUpdate(templates, line);
        }
      }
    }
    List<BaselineTemplate> values = templates.values().stream()
        .map(MutableTemplate::toBaseline)
        .sorted(Comparator.comparingLong(BaselineTemplate::id))
        .toList();
    List<String> names = migrations.stream().map(path -> path.getFileName().toString()).toList();
    return new LocalTemplateBaseline(1, names, values);
  }

  /** Generates the initial JSON file: output path first, followed by ordered migration paths. */
  public static void main(String[] args) {
    if (args.length < 2) {
      throw new IllegalArgumentException("Usage: <output-json> <migration>...");
    }
    Path output = Path.of(args[0]);
    List<Path> migrations = new ArrayList<>();
    for (int index = 1; index < args.length; index++) {
      migrations.add(Path.of(args[index]));
    }
    extract(migrations).write(output);
  }

  private static void addTemplate(Map<Long, MutableTemplate> templates, List<String> values) {
    if (values.size() != 14) {
      throw new IllegalArgumentException("Unexpected dish_templates value count: " + values.size());
    }
    long id = Long.parseLong(values.get(0));
    MutableTemplate template = new MutableTemplate(
        id,
        values.get(1),
        Long.parseLong(values.get(2)),
        values.get(3),
        values.get(4),
        values.get(5),
        values.get(6),
        values.get(7),
        values.get(8),
        new BigDecimal(values.get(9)),
        parseStringList(values.get(10)),
        parseStringList(values.get(11)),
        Integer.parseInt(values.get(12)),
        "1".equals(values.get(13)));
    if (templates.putIfAbsent(id, template) != null) {
      throw new IllegalArgumentException("Duplicate legacy template ID: " + id);
    }
  }

  private static void addIngredient(Map<Long, MutableTemplate> templates, List<String> values) {
    if (values.size() != 7) {
      throw new IllegalArgumentException(
          "Unexpected dish_template_ingredients value count: " + values.size());
    }
    long templateId = Long.parseLong(values.get(0));
    MutableTemplate template = templates.get(templateId);
    if (template == null) {
      throw new IllegalArgumentException("Ingredient references unknown template: " + templateId);
    }
    template.ingredients.add(new BaselineIngredient(
        values.get(1), values.get(2), new BigDecimal(values.get(3)), values.get(4),
        values.get(5), Integer.parseInt(values.get(6))));
  }

  private static void applyImageUpdate(Map<Long, MutableTemplate> templates, String line) {
    int whereIndex = line.indexOf(" WHERE id=");
    if (whereIndex < 0) {
      throw new IllegalArgumentException("Invalid image update: " + line);
    }
    String where = line.substring(whereIndex + " WHERE id=".length());
    long id = Long.parseLong(where.substring(0, where.indexOf(' ')));
    MutableTemplate template = templates.get(id);
    if (template == null || !where.contains("template_code='" + template.templateCode + "'")) {
      throw new IllegalArgumentException("Image update identity mismatch: " + line);
    }
    for (String assignment : splitSqlValues(line.substring(IMAGE_UPDATE.length(), whereIndex))) {
      int equalsIndex = assignment.indexOf('=');
      String column = assignment.substring(0, equalsIndex).trim();
      String value = unquote(assignment.substring(equalsIndex + 1).trim());
      switch (column) {
        case "image_source_url" -> template.imageSourceUrl = value;
        case "image_author" -> template.imageAuthor = value;
        case "image_license" -> template.imageLicense = value;
        default -> throw new IllegalArgumentException("Unexpected image update column: " + column);
      }
    }
  }

  private static List<String> parseInsertValues(String line) {
    int start = line.indexOf(" VALUES (");
    int end = line.lastIndexOf(");");
    if (start < 0 || end < start) {
      throw new IllegalArgumentException("Invalid INSERT statement: " + line);
    }
    return splitSqlValues(line.substring(start + " VALUES (".length(), end)).stream()
        .map(LegacyMigrationBaselineExtractor::unquote)
        .toList();
  }

  private static List<String> splitSqlValues(String source) {
    List<String> values = new ArrayList<>();
    StringBuilder value = new StringBuilder();
    boolean quoted = false;
    for (int index = 0; index < source.length(); index++) {
      char character = source.charAt(index);
      if (character == '\'' && quoted && index + 1 < source.length()
          && source.charAt(index + 1) == '\'') {
        value.append("''");
        index++;
      } else if (character == '\'') {
        quoted = !quoted;
        value.append(character);
      } else if (character == ',' && !quoted) {
        values.add(value.toString().trim());
        value.setLength(0);
      } else {
        value.append(character);
      }
    }
    if (quoted) {
      throw new IllegalArgumentException("Unclosed SQL string: " + source);
    }
    values.add(value.toString().trim());
    return values;
  }

  private static String unquote(String value) {
    if (value.length() >= 2 && value.charAt(0) == '\'' && value.charAt(value.length() - 1) == '\'') {
      return value.substring(1, value.length() - 1).replace("''", "'");
    }
    return value;
  }

  private static List<String> parseStringList(String json) {
    try {
      return OBJECT_MAPPER.readValue(json, new TypeReference<>() { });
    } catch (IOException exception) {
      throw new IllegalArgumentException("Invalid legacy JSON array: " + json, exception);
    }
  }

  private static String readUtf8Strict(Path file) {
    try {
      return StandardCharsets.UTF_8.newDecoder()
          .onMalformedInput(CodingErrorAction.REPORT)
          .onUnmappableCharacter(CodingErrorAction.REPORT)
          .decode(ByteBuffer.wrap(Files.readAllBytes(file)))
          .toString();
    } catch (IOException exception) {
      throw new IllegalArgumentException("Unable to read legacy migration: " + file, exception);
    }
  }

  private static final class MutableTemplate {
    private final long id;
    private final String templateCode;
    private final long categoryId;
    private final String name;
    private final String description;
    private final String imageUrl;
    private String imageSourceUrl;
    private String imageAuthor;
    private String imageLicense;
    private final BigDecimal referencePrice;
    private final List<String> tasteTags;
    private final List<String> mealTags;
    private final int sortOrder;
    private final boolean enabled;
    private final List<BaselineIngredient> ingredients = new ArrayList<>();

    private MutableTemplate(long id, String templateCode, long categoryId, String name,
        String description, String imageUrl, String imageSourceUrl, String imageAuthor,
        String imageLicense, BigDecimal referencePrice, List<String> tasteTags,
        List<String> mealTags, int sortOrder, boolean enabled) {
      this.id = id;
      this.templateCode = templateCode;
      this.categoryId = categoryId;
      this.name = name;
      this.description = description;
      this.imageUrl = imageUrl;
      this.imageSourceUrl = imageSourceUrl;
      this.imageAuthor = imageAuthor;
      this.imageLicense = imageLicense;
      this.referencePrice = referencePrice;
      this.tasteTags = tasteTags;
      this.mealTags = mealTags;
      this.sortOrder = sortOrder;
      this.enabled = enabled;
    }

    private BaselineTemplate toBaseline() {
      ingredients.sort(Comparator.comparingInt(BaselineIngredient::sortOrder));
      return new BaselineTemplate(id, templateCode, categoryId, name, description, imageUrl,
          imageSourceUrl, imageAuthor, imageLicense, referencePrice, List.copyOf(tasteTags),
          List.copyOf(mealTags), sortOrder, enabled, List.copyOf(ingredients));
    }
  }
}
