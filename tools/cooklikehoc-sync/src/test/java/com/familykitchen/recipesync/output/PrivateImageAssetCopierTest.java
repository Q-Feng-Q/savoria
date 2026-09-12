package com.familykitchen.recipesync.output;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.familykitchen.recipesync.model.SourceRecipe;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Covers private-only source image staging. */
class PrivateImageAssetCopierTest {

  @Test
  void copiesDeclaredImageUsingAStablePrivateStorageKey(@TempDir Path tempDir) throws Exception {
    Path sourceRoot = Files.createDirectories(tempDir.resolve("source"));
    Path recipeDirectory = Files.createDirectories(sourceRoot.resolve("炒菜"));
    Path imageDirectory = Files.createDirectories(sourceRoot.resolve("images"));
    Files.write(imageDirectory.resolve("示例.png"), new byte[] {(byte) 0x89, 0x50, 0x4e, 0x47});
    SourceRecipe recipe = new SourceRecipe("炒菜/示例.md", "示例", "炒菜", "../images/示例.png",
        List.of(), List.of(), List.of(), List.of());
    Path privateRoot = tempDir.resolve("private");

    PrivateImageAsset asset = new PrivateImageAssetCopier().copy(
        sourceRoot, recipeDirectory.resolve("示例.md"), recipe, privateRoot,
        "f7a91c2db0ce9b6a41eaf06e5ce64cbde5a831ed").orElseThrow();

    assertEquals("images/示例.png", asset.sourceImagePath());
    assertTrue(asset.internalStorageKey().startsWith("cooklikehoc/f7a91c2d/"));
    assertFalse(asset.internalStorageKey().contains(tempDir.toString()));
    assertTrue(Files.exists(privateRoot.resolve(asset.internalStorageKey())));
    assertEquals("INTERNAL_REVIEW", asset.assetStatus());
  }

  @Test
  void recipeWithoutImageCreatesNoAsset(@TempDir Path tempDir) {
    SourceRecipe recipe = new SourceRecipe("炒菜/无图.md", "无图", "炒菜", null,
        List.of(), List.of(), List.of(), List.of());

    assertTrue(new PrivateImageAssetCopier().copy(tempDir, tempDir.resolve("炒菜/无图.md"),
        recipe, tempDir.resolve("private"),
        "f7a91c2db0ce9b6a41eaf06e5ce64cbde5a831ed").isEmpty());
  }
}
