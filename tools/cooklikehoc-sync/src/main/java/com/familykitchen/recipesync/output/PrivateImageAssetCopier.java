package com.familykitchen.recipesync.output;

import com.familykitchen.recipesync.model.SourceRecipe;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.Locale;
import java.util.Optional;

/** Copies source-declared images to private review storage without publishing a URL. */
public final class PrivateImageAssetCopier {

  /** Returns no asset when the source recipe has no image declaration. */
  public Optional<PrivateImageAsset> copy(Path sourceRoot, Path recipeFile, SourceRecipe recipe,
      Path privateRoot, String sourceRevision) {
    if (recipe.imagePath() == null || recipe.imagePath().isBlank()) {
      return Optional.empty();
    }
    Path normalizedRoot = sourceRoot.toAbsolutePath().normalize();
    String decoded = URLDecoder.decode(recipe.imagePath(), StandardCharsets.UTF_8);
    Path imageFile = decoded.startsWith("/")
        ? normalizedRoot.resolve(decoded.substring(1)).normalize()
        : recipeFile.toAbsolutePath().normalize().getParent().resolve(decoded).normalize();
    if (!imageFile.startsWith(normalizedRoot) || !Files.isRegularFile(imageFile)) {
      throw new IllegalArgumentException("Declared source image is missing or outside source root: "
          + recipe.sourceKey() + " -> " + recipe.imagePath());
    }
    try {
      byte[] content = Files.readAllBytes(imageFile);
      String hash = sha256(content);
      String extension = extension(imageFile.getFileName().toString());
      String storageKey = "cooklikehoc/" + sourceRevision.substring(0, 8) + "/" + hash + extension;
      Path normalizedPrivateRoot = privateRoot.toAbsolutePath().normalize();
      Path target = normalizedPrivateRoot.resolve(storageKey).normalize();
      if (!target.startsWith(normalizedPrivateRoot)) {
        throw new IllegalArgumentException("Invalid private image storage key: " + storageKey);
      }
      Files.createDirectories(target.getParent());
      Files.copy(imageFile, target, StandardCopyOption.REPLACE_EXISTING);
      String sourceImagePath = Normalizer.normalize(
          normalizedRoot.relativize(imageFile).toString().replace('\\', '/'), Normalizer.Form.NFC);
      return Optional.of(new PrivateImageAsset(storageKey, hash, mimeType(extension), content.length,
          sourceImagePath, sourceRevision, "INTERNAL_REVIEW"));
    } catch (IOException exception) {
      throw new IllegalArgumentException("Unable to stage source image: " + imageFile, exception);
    }
  }

  private String extension(String name) {
    int dot = name.lastIndexOf('.');
    return dot < 0 ? "" : name.substring(dot).toLowerCase(Locale.ROOT);
  }

  private String mimeType(String extension) {
    return switch (extension) {
      case ".png" -> "image/png";
      case ".jpg", ".jpeg" -> "image/jpeg";
      case ".webp" -> "image/webp";
      case ".gif" -> "image/gif";
      default -> "application/octet-stream";
    };
  }

  private String sha256(byte[] value) {
    try {
      byte[] digest = MessageDigest.getInstance("SHA-256").digest(value);
      StringBuilder hex = new StringBuilder(digest.length * 2);
      for (byte current : digest) {
        hex.append(String.format("%02x", current));
      }
      return hex.toString();
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 is unavailable", exception);
    }
  }
}
