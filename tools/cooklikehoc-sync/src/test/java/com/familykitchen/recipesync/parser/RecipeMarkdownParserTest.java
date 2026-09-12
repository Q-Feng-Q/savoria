package com.familykitchen.recipesync.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Covers the source Markdown structures used by CookLikeHOC. */
class RecipeMarkdownParserTest {

  private final Path fixtures = Path.of("src/test/resources/fixtures").toAbsolutePath();
  private final RecipeMarkdownParser parser = new RecipeMarkdownParser();

  @Test
  void parsesTitleImageIngredientsAndOrderedSteps() {
    var recipe = parser.parse(fixtures, fixtures.resolve("炒菜/标准菜谱.md"));

    assertEquals("炒菜/标准菜谱.md", recipe.sourceKey());
    assertEquals("炒菜", recipe.category());
    assertEquals("西红柿炒鸡蛋", recipe.title());
    assertEquals("../images/西红柿炒鸡蛋.jpg", recipe.imagePath());
    assertEquals(2, recipe.ingredients().size());
    assertEquals("西红柿", recipe.ingredients().get(0).name());
    assertEquals("500g", recipe.ingredients().get(0).sourceQuantityText());
    assertEquals("炒菜/标准菜谱.md#ingredient-1", recipe.ingredients().get(0).sourceLineKey());
    assertEquals(3, recipe.steps().size());
    assertEquals(1, recipe.steps().get(0).stepNo());
    assertTrue(recipe.steps().get(2).content().contains("翻匀"));
  }

  @Test
  void resolvesComponentLinksWithoutTurningThemIntoIngredientNames() {
    var recipe = parser.parse(fixtures, fixtures.resolve("炒菜/组件引用菜谱.md"));

    assertEquals(1, recipe.componentLinks().size());
    assertEquals("配料/组件菜谱.md", recipe.componentLinks().get(0));
    assertEquals("鸡腿肉", recipe.ingredients().get(0).name());
  }

  @Test
  void acceptsKnownNonStandardSectionHeadings() {
    var recipe = parser.parse(fixtures, fixtures.resolve("非标准/无标准标题.md"));

    assertEquals(2, recipe.ingredients().size());
    assertEquals(2, recipe.steps().size());
    assertTrue(recipe.issues().isEmpty());
  }

  @Test
  void preservesStepSubsectionsAndResolvesRootRelativeComponents() {
    var recipe = parser.parse(fixtures, fixtures.resolve("砂锅菜/真实结构菜谱.md"));

    assertEquals(List.of("配料/组件菜谱.md"), recipe.componentLinks());
    assertEquals(3, recipe.steps().size());
    assertEquals("第一段", recipe.steps().get(0).title());
    assertEquals("第二段", recipe.steps().get(2).title());
    assertEquals(210, recipe.steps().get(1).durationSeconds());
    assertEquals("160℃", recipe.steps().get(0).temperatureText());
    assertEquals("大火", recipe.steps().get(2).heatLevel());
  }

  @Test
  void rejectsMalformedUtf8() throws IOException {
    Path invalidFile = fixtures.resolve("非标准/非法编码.md");
    Files.write(invalidFile, new byte[] {(byte) 0xC3, (byte) 0x28});
    try {
      var error = assertThrows(IllegalArgumentException.class,
          () -> parser.parse(fixtures, invalidFile));
      assertTrue(error.getMessage().contains("not valid UTF-8"));
    } finally {
      Files.deleteIfExists(invalidFile);
    }
  }
}
