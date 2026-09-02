package com.familykitchen.recipesync.normalize;

import java.text.Normalizer;
import org.commonmark.node.AbstractVisitor;
import org.commonmark.node.Code;
import org.commonmark.node.Node;
import org.commonmark.node.SoftLineBreak;
import org.commonmark.node.Text;
import org.commonmark.parser.Parser;

/** Applies the versioned, deterministic normalization rules used by matching. */
public final class RecipeNormalizer {

  private static final Parser MARKDOWN_PARSER = Parser.builder().build();

  private RecipeNormalizer() {
  }

  /** Normalizes a human-visible recipe or ingredient name. */
  public static String normalizeName(String value) {
    if (value == null) {
      return null;
    }
    StringBuilder visibleText = new StringBuilder();
    Node document = MARKDOWN_PARSER.parse(value);
    document.accept(new AbstractVisitor() {
      @Override
      public void visit(Text text) {
        visibleText.append(text.getLiteral());
      }

      @Override
      public void visit(Code code) {
        visibleText.append(code.getLiteral());
      }

      @Override
      public void visit(SoftLineBreak lineBreak) {
        visibleText.append(' ');
      }
    });
    return Normalizer.normalize(visibleText, Normalizer.Form.NFC)
        .replace('（', '(')
        .replace('）', ')')
        .replace('\u00a0', ' ')
        .replaceAll("\\s+", " ")
        .trim();
  }
}
