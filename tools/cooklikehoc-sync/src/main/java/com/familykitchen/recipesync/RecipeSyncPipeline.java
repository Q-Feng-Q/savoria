package com.familykitchen.recipesync;

import com.familykitchen.recipesync.config.BaselineIngredient;
import com.familykitchen.recipesync.config.BaselineTemplate;
import com.familykitchen.recipesync.config.LocalTemplateBaseline;
import com.familykitchen.recipesync.config.SourceSyncSettings;
import com.familykitchen.recipesync.config.SyncConfig;
import com.familykitchen.recipesync.config.TemplateIdAllocation;
import com.familykitchen.recipesync.config.TemplateIdAllocations;
import com.familykitchen.recipesync.config.TemplateNameMappings;
import com.familykitchen.recipesync.model.SourceIngredient;
import com.familykitchen.recipesync.model.SourceRecipe;
import com.familykitchen.recipesync.model.SourceStep;
import com.familykitchen.recipesync.normalize.RecipeNormalizer;
import com.familykitchen.recipesync.output.PrivateImageAsset;
import com.familykitchen.recipesync.output.PrivateImageAssetCopier;
import com.familykitchen.recipesync.output.SqlRenderer;
import com.familykitchen.recipesync.output.SyncArtifactWriter;
import com.familykitchen.recipesync.output.SyncArtifacts;
import com.familykitchen.recipesync.parser.RecipeMarkdownParser;
import com.familykitchen.recipesync.rewrite.RewriteReview;
import com.familykitchen.recipesync.rewrite.RewriteReviewGate;
import com.familykitchen.recipesync.rewrite.RewrittenStep;
import com.familykitchen.recipesync.rewrite.SimilarityGate;
import com.familykitchen.recipesync.rewrite.StepFactRewriter;
import com.familykitchen.recipesync.rewrite.StepReviewCandidate;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Connects the deterministic parser, review gates and artifact writers into one CLI run. */
final class RecipeSyncPipeline {

  private static final ObjectMapper JSON = new ObjectMapper()
      .registerModule(new JavaTimeModule())
      .enable(SerializationFeature.INDENT_OUTPUT);
  private static final Pattern QUANTITY = Pattern.compile(
      "(?i)(\\d+(?:\\.\\d+)?)(kg|g|克|千克|斤|个|只|条|ml|毫升|升|l)$");
  private static final String SOURCE_BASE_URL = "https://cooklikehoc.soilzhu.su/";
  private final RecipeMarkdownParser parser = new RecipeMarkdownParser();
  private final StepFactRewriter stepRewriter = new StepFactRewriter();
  private final SimilarityGate similarityGate = new SimilarityGate();
  private final SqlRenderer sql = new SqlRenderer();

  /** Executes draft review generation or guarded release generation. */
  void run(SyncConfig config, SourceSyncSettings settings, LocalTemplateBaseline baseline,
      TemplateIdAllocations allocations, TemplateNameMappings mappings) {
    List<ParsedRecipe> parsed = parseAll(config.sourceDir(), settings.categoryMappings().keySet());
    List<String> parserIssues = parsed.stream()
        .flatMap(item -> item.recipe().issues().stream()
            .map(issue -> item.recipe().sourceKey() + ":" + issue))
        .sorted().toList();
    List<RecipeGroup> groups = group(parsed, mappings);
    AllocationPlan allocationPlan = allocationPlan(groups, baseline, allocations, mappings);
    List<StepReviewCandidate> reviewCandidates = reviewCandidates(parsed);

    if (config.mode() == SyncConfig.Mode.DRAFT) {
      writeJson(config.outputDir().resolve("template-id-allocation-proposals.json"),
          Map.of("schemaVersion", 1, "proposals", allocationPlan.proposals()));
      writeJson(config.outputDir().resolve("rewrite-review-queue.json"),
          Map.of("schemaVersion", 1, "candidates", reviewCandidates));
      writeJson(config.outputDir().resolve("parser-merge-issues.json"),
          Map.of("schemaVersion", 1, "issues", parserIssues));
      if (!allocationPlan.proposals().isEmpty()) {
        return;
      }
    }

    List<String> unresolved = new ArrayList<>(parserIssues);
    if (!allocationPlan.proposals().isEmpty()) {
      allocationPlan.proposals().forEach(item -> unresolved.add("MISSING_ALLOCATION:" + item.stableKey()));
    }
    List<RewriteReview> reviews = loadReviews(config.configDir().resolve("rewrite-reviews.json"));
    unresolved.addAll(new RewriteReviewGate().unresolved(reviewCandidates,
        settings.similarityThreshold().doubleValue(), reviews));
    BuildResult result = buildArtifacts(config, settings, baseline, groups, allocationPlan.identities(),
        mappings, unresolved);
    SyncArtifacts artifacts = new SyncArtifacts(json(result.manifest()), json(result.report()), result.sql());
    if (config.mode() == SyncConfig.Mode.DRAFT) {
      new SyncArtifactWriter().writeDraft(config.outputDir(), artifacts);
    } else {
      new SyncArtifactWriter().writeRelease(config.v4File(), config.manifestFile(),
          config.qualityReportFile(), artifacts, unresolved);
    }
  }

  private List<ParsedRecipe> parseAll(Path root, Set<String> categories) {
    try {
      List<Path> files = new ArrayList<>();
      for (String category : categories) {
        Path directory = root.resolve(category);
        try (var stream = Files.walk(directory)) {
          stream.filter(Files::isRegularFile)
              .filter(file -> file.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".md"))
              .filter(file -> !file.getFileName().toString().equalsIgnoreCase("README.md"))
              .forEach(files::add);
        }
      }
      files.sort(Comparator.comparing(file -> normalizePath(root.relativize(file))));
      return files.stream().map(file -> new ParsedRecipe(file, parser.parse(root, file))).toList();
    } catch (IOException exception) {
      throw new IllegalArgumentException("Unable to discover source recipes", exception);
    }
  }

  private List<RecipeGroup> group(List<ParsedRecipe> parsed, TemplateNameMappings mappings) {
    Map<String, List<ParsedRecipe>> byTitle = new LinkedHashMap<>();
    for (ParsedRecipe item : parsed) {
      byTitle.computeIfAbsent(RecipeNormalizer.normalizeName(item.recipe().title()), ignored -> new ArrayList<>())
          .add(item);
    }
    List<RecipeGroup> groups = new ArrayList<>();
    for (Map.Entry<String, List<ParsedRecipe>> entry : byTitle.entrySet()) {
      List<ParsedRecipe> items = entry.getValue().stream()
          .sorted(Comparator.comparing(item -> item.recipe().sourceKey())).toList();
      String primaryKey = mappings.primarySourceByNormalizedTitle().get(entry.getKey());
      ParsedRecipe primary = primaryKey == null ? items.get(0) : items.stream()
          .filter(item -> item.recipe().sourceKey().equals(primaryKey)).findFirst()
          .orElseThrow(() -> new IllegalArgumentException("INVALID_PRIMARY_SOURCE:" + primaryKey));
      groups.add(new RecipeGroup(entry.getKey(), primary, items));
    }
    groups.sort(Comparator.comparing(group -> group.primary().recipe().sourceKey()));
    return List.copyOf(groups);
  }

  private AllocationPlan allocationPlan(List<RecipeGroup> groups, LocalTemplateBaseline baseline,
      TemplateIdAllocations allocations, TemplateNameMappings mappings) {
    Map<Long, BaselineTemplate> localById = new HashMap<>();
    Map<String, BaselineTemplate> localByName = new HashMap<>();
    baseline.templates().forEach(item -> {
      localById.put(item.id(), item);
      localByName.putIfAbsent(RecipeNormalizer.normalizeName(item.name()), item);
    });
    Map<String, TemplateIdAllocation> configured = new HashMap<>();
    allocations.allocations().forEach(item -> configured.put(item.stableKey(), item));
    long next = allocations.highWaterMark();
    List<TemplateIdAllocation> proposals = new ArrayList<>();
    Map<String, TemplateIdentity> identities = new LinkedHashMap<>();
    Set<Long> claimedLocals = new LinkedHashSet<>();
    for (RecipeGroup group : groups) {
      SourceRecipe source = group.primary().recipe();
      Long explicitId = group.items().stream().map(item -> mappings.explicitSourceMappings()
          .get(item.recipe().sourceKey())).filter(java.util.Objects::nonNull).findFirst().orElse(null);
      BaselineTemplate local = explicitId == null ? localByName.get(group.normalizedTitle()) : localById.get(explicitId);
      if (local == null) {
        Long aliasId = mappings.normalizedAliases().get(group.normalizedTitle());
        if (aliasId != null) local = localById.get(aliasId);
      }
      if (local != null && !claimedLocals.add(local.id())) {
        throw new IllegalArgumentException("LOCAL_TEMPLATE_ALREADY_CLAIMED:" + local.id());
      }
      if (local != null) {
        identities.put(source.sourceKey(), new TemplateIdentity(local.id(), local.templateCode(), local));
      } else {
        String stableKey = "source:" + source.sourceKey();
        TemplateIdAllocation allocation = configured.get(stableKey);
        if (allocation == null) {
          allocation = new TemplateIdAllocation(stableKey, ++next, String.format("CLH_%03d", next));
          proposals.add(allocation);
        }
        identities.put(source.sourceKey(), new TemplateIdentity(allocation.id(), allocation.templateCode(), null));
      }
    }
    return new AllocationPlan(Map.copyOf(identities), List.copyOf(proposals));
  }

  private List<StepReviewCandidate> reviewCandidates(List<ParsedRecipe> parsed) {
    List<StepReviewCandidate> result = new ArrayList<>();
    for (ParsedRecipe item : parsed) {
      for (SourceStep step : item.recipe().steps()) {
        RewrittenStep rewritten = stepRewriter.rewrite(step);
        result.add(new StepReviewCandidate(item.recipe().sourceKey(), step.stepNo(),
            similarityGate.score(step.content(), rewritten.content())));
      }
    }
    result.sort(Comparator.comparing(StepReviewCandidate::sourceKey)
        .thenComparingInt(StepReviewCandidate::stepNo));
    return List.copyOf(result);
  }

  private BuildResult buildArtifacts(SyncConfig config, SourceSyncSettings settings,
      LocalTemplateBaseline baseline, List<RecipeGroup> groups, Map<String, TemplateIdentity> identities,
      TemplateNameMappings mappings, List<String> unresolved) {
    Map<Long, BaselineTemplate> baselineById = new HashMap<>();
    baseline.templates().forEach(item -> baselineById.put(item.id(), item));
    Map<String, Long> templateIdBySource = new HashMap<>();
    for (RecipeGroup group : groups) {
      long id = identities.get(group.primary().recipe().sourceKey()).id();
      group.items().forEach(item -> templateIdBySource.put(item.recipe().sourceKey(), id));
    }
    StringBuilder generatedSql = new StringBuilder();
    List<Map<String, Object>> manifestRecipes = new ArrayList<>();
    List<String> buildIssues = new ArrayList<>();
    int sourceRecordId = 100_000;
    int imageAssetId = 100_000;
    int declaredImages = 0;
    int stagedImages = 0;
    int stepCount = 0;
    int ingredientCount = 0;
    Path privateRoot = backendRoot(config.outputDir()).resolve(
        "runtime-data/private/dish-template-review-assets");
    for (RecipeGroup group : groups.stream().sorted(Comparator.comparingLong(
        value -> identities.get(value.primary().recipe().sourceKey()).id())).toList()) {
      SourceRecipe source = group.primary().recipe();
      TemplateIdentity identity = identities.get(source.sourceKey());
      BaselineTemplate local = identity.local();
      long categoryId = settings.categoryMappings().get(source.category());
      boolean component = "配料".equals(source.category());
      List<IngredientRow> ingredients = ingredients(source, local, templateIdBySource);
      boolean procurementReady = ingredients.stream().allMatch(IngredientRow::procurementReady)
          && (!ingredients.isEmpty() || component);
      BigDecimal price = local == null ? null : local.referencePrice();
      String dataStatus = dataStatus(component || price != null, procurementReady);
      boolean hasDeclaration = group.items().stream().anyMatch(item ->
          item.recipe().imagePath() != null && !item.recipe().imagePath().isBlank());
      String sourceUrl = sourceUrl(source.sourceKey());
      Map<String, Object> templateValues = templateValues(identity, categoryId, source, local,
          component, price, dataStatus, procurementReady, hasDeclaration, settings.sourceRevision());
      if (local == null) {
        generatedSql.append(sql.insert("dish_templates", templateValues));
      } else {
        generatedSql.append(updateTemplate(templateValues, identity.id()));
      }
      generatedSql.append("DELETE FROM dish_template_ingredients WHERE template_id=")
          .append(identity.id()).append(";\n");
      generatedSql.append("DELETE FROM dish_template_cooking_steps WHERE template_id=")
          .append(identity.id()).append(";\n");
      for (IngredientRow ingredient : ingredients) {
        generatedSql.append(sql.insert("dish_template_ingredients", ingredient.values(identity.id())));
        ingredientCount++;
      }
      for (SourceStep sourceStep : source.steps()) {
        RewrittenStep step = stepRewriter.rewrite(sourceStep);
        generatedSql.append(sql.insert("dish_template_cooking_steps", stepValues(identity.id(), source,
            step, templateIdBySource)));
        stepCount++;
      }
      Map<String, Integer> sourceRecordIds = new HashMap<>();
      for (ParsedRecipe item : group.items()) {
        int currentRecordId = ++sourceRecordId;
        sourceRecordIds.put(item.recipe().sourceKey(), currentRecordId);
        generatedSql.append(sql.insert("dish_template_source_records", sourceRecordValues(
            currentRecordId, identity.id(), item, item == group.primary(), settings.sourceRevision())));
      }
      for (ParsedRecipe imageSource : group.items()) {
        if (imageSource.recipe().imagePath() == null || imageSource.recipe().imagePath().isBlank()) continue;
        declaredImages++;
        try {
          PrivateImageAsset asset = new PrivateImageAssetCopier().copy(config.sourceDir(),
              imageSource.file(), imageSource.recipe(), privateRoot, settings.sourceRevision()).orElseThrow();
          generatedSql.append(sql.insert("dish_template_image_assets", imageAssetValues(++imageAssetId,
              identity.id(), sourceRecordIds.get(imageSource.recipe().sourceKey()), asset,
              sourceUrl(imageSource.recipe().sourceKey()))));
          stagedImages++;
        } catch (RuntimeException exception) {
          buildIssues.add("IMAGE_ASSET:" + imageSource.recipe().sourceKey() + ":" + exception.getMessage());
        }
      }
      manifestRecipes.add(manifestRecipe(identity, source, group, ingredients, price, dataStatus,
          procurementReady));
    }
    unresolved.addAll(buildIssues);
    Map<String, Object> manifest = new LinkedHashMap<>();
    manifest.put("schemaVersion", 1);
    manifest.put("sourceRevision", settings.sourceRevision());
    manifest.put("recipes", manifestRecipes);
    Map<String, Object> report = new LinkedHashMap<>();
    report.put("schemaVersion", 1);
    report.put("sourceRevision", settings.sourceRevision());
    report.put("sourceFileCount", groups.stream().mapToInt(group -> group.items().size()).sum());
    report.put("uniqueTitleCount", groups.size());
    report.put("categoryCount", settings.categoryMappings().size());
    report.put("repositoryImageCount", countRepositoryImages(config.sourceDir()));
    report.put("declaredImageCount", declaredImages);
    report.put("stagedImageCount", stagedImages);
    report.put("ingredientCount", ingredientCount);
    report.put("cookingStepCount", stepCount);
    report.put("unresolvedIssueCount", unresolved.size());
    report.put("unresolvedIssues", unresolved.stream().sorted().toList());
    return new BuildResult(generatedSql.toString(), manifest, report);
  }

  private List<IngredientRow> ingredients(SourceRecipe source, BaselineTemplate local,
      Map<String, Long> componentIds) {
    Map<String, BaselineIngredient> baselineByName = new HashMap<>();
    if (local != null) local.ingredients().forEach(item -> baselineByName.put(
        RecipeNormalizer.normalizeName(item.ingredientName()), item));
    List<IngredientRow> rows = new ArrayList<>();
    int order = 0;
    for (SourceIngredient sourceIngredient : source.ingredients()) {
      BaselineIngredient previous = baselineByName.get(RecipeNormalizer.normalizeName(sourceIngredient.name()));
      Quantity quantity = parsedQuantity(sourceIngredient.sourceQuantityText());
      if (quantity == null && previous != null && previous.quantity() != null
          && previous.quantity().signum() > 0) {
        quantity = new Quantity(previous.quantity(), previous.unit(), previous.calcType());
      }
      String status = quantity == null ? "MISSING" : "VERIFIED";
      rows.add(new IngredientRow(sourceIngredient.sourceLineKey(), sourceIngredient.name(),
          ingredientCategory(sourceIngredient.name()), status, quantity, sourceIngredient.sourceText(),
          sourceIngredient.sourceQuantityText(), null, null, ++order));
    }
    int componentIndex = 0;
    for (String componentKey : source.componentLinks()) {
      Long componentId = componentIds.get(componentKey);
      rows.add(new IngredientRow(source.sourceKey() + "#component-" + (++componentIndex),
          componentName(componentKey), "调味料", "SOURCE_BATCH", null, componentKey,
          "按来源配方", componentId, null, ++order));
    }
    return List.copyOf(rows);
  }

  private Map<String, Object> templateValues(TemplateIdentity identity, long categoryId,
      SourceRecipe source, BaselineTemplate local, boolean component, BigDecimal price,
      String dataStatus, boolean procurementReady, boolean hasDeclaration, String revision) {
    Map<String, Object> values = new LinkedHashMap<>();
    values.put("id", identity.id()); values.put("template_code", identity.code());
    values.put("category_id", categoryId); values.put("name", source.title());
    values.put("description", local == null ? null : local.description());
    if (local == null) {
      values.put("image_url", null); values.put("image_source_url", null);
      values.put("image_author", null); values.put("image_license", null);
    }
    values.put("reference_price", price);
    values.put("taste_tags", json(local == null ? List.of() : local.tasteTags()).trim());
    values.put("meal_tags", json(local == null ? List.of("LUNCH", "DINNER") : local.mealTags()).trim());
    values.put("template_type", component ? "COMPONENT" : "DISH");
    values.put("source_type", "COOK_LIKE_HOC"); values.put("source_key", source.sourceKey());
    values.put("source_url", sourceUrl(source.sourceKey())); values.put("source_revision", revision);
    values.put("source_category", source.category()); values.put("source_yield_text", null);
    values.put("data_status", dataStatus); values.put("procurement_ready", procurementReady);
    values.put("image_rights_status", local != null && local.imageUrl() != null ? "DECLARED"
        : hasDeclaration ? "UNDECLARED" : "NONE");
    values.put("sort_order", local == null ? (int) identity.id() : local.sortOrder());
    values.put("enabled", local == null || local.enabled());
    return values;
  }

  private String updateTemplate(Map<String, Object> values, long id) {
    StringBuilder statement = new StringBuilder("UPDATE dish_templates SET ");
    int index = 0;
    for (Map.Entry<String, Object> entry : values.entrySet()) {
      if ("id".equals(entry.getKey()) || "template_code".equals(entry.getKey())
          || entry.getKey().startsWith("image_")) continue;
      if (index++ > 0) statement.append(',');
      statement.append(entry.getKey()).append('=').append(literal(entry.getValue()));
    }
    return statement.append(" WHERE id=").append(id).append(";\n").toString();
  }

  private Map<String, Object> stepValues(long templateId, SourceRecipe source, RewrittenStep step,
      Map<String, Long> componentIds) {
    Map<String, Object> values = new LinkedHashMap<>();
    values.put("item_key", source.sourceKey() + "#step-" + step.stepNo());
    values.put("template_id", templateId); values.put("step_no", step.stepNo());
    values.put("title", step.title()); values.put("content", step.content());
    values.put("source_text", source.sourceKey() + "#step-" + step.stepNo());
    values.put("duration_seconds", step.durationSeconds());
    values.put("temperature_text", step.temperatureText()); values.put("heat_level", step.heatLevel());
    values.put("component_template_id", null);
    return values;
  }

  private Map<String, Object> sourceRecordValues(int id, long templateId, ParsedRecipe item,
      boolean primary, String revision) {
    SourceRecipe source = item.recipe();
    Map<String, Object> values = new LinkedHashMap<>();
    values.put("id", id); values.put("template_id", templateId); values.put("source_key", source.sourceKey());
    values.put("source_title", source.title()); values.put("source_category", source.category());
    values.put("source_path", source.sourceKey()); values.put("source_url", sourceUrl(source.sourceKey()));
    values.put("source_revision", revision); values.put("record_type", primary ? "PRIMARY" : "SOURCE_ALIAS");
    values.put("alias_reason", primary ? null : "同名来源文件合并");
    values.put("content_sha256", sha256(item.file()));
    return values;
  }

  private Map<String, Object> imageAssetValues(int id, long templateId, int sourceRecordId,
      PrivateImageAsset asset, String sourceUrl) {
    Map<String, Object> values = new LinkedHashMap<>();
    values.put("id", id); values.put("template_id", templateId);
    values.put("source_record_id", sourceRecordId); values.put("internal_storage_key", asset.internalStorageKey());
    values.put("content_sha256", asset.contentSha256()); values.put("mime_type", asset.mimeType());
    values.put("file_size", asset.fileSize()); values.put("source_image_path", asset.sourceImagePath());
    values.put("source_url", sourceUrl); values.put("source_revision", asset.sourceRevision());
    values.put("asset_status", asset.assetStatus()); values.put("rejection_reason", null);
    return values;
  }

  private Map<String, Object> manifestRecipe(TemplateIdentity identity, SourceRecipe source,
      RecipeGroup group, List<IngredientRow> ingredients, BigDecimal price, String dataStatus,
      boolean procurementReady) {
    Map<String, Object> item = new LinkedHashMap<>();
    item.put("templateId", identity.id()); item.put("templateCode", identity.code());
    item.put("name", source.title()); item.put("sourceKey", source.sourceKey());
    item.put("sourceKeys", group.items().stream().map(value -> value.recipe().sourceKey()).toList());
    item.put("sourceCategory", source.category()); item.put("referencePrice", price);
    item.put("dataStatus", dataStatus); item.put("procurementReady", procurementReady);
    item.put("ingredients", ingredients.stream().map(IngredientRow::manifest).toList());
    item.put("cookingSteps", source.steps().stream().map(stepRewriter::rewrite).toList());
    return item;
  }

  private List<RewriteReview> loadReviews(Path file) {
    try {
      RewriteReviewDocument document = JSON.readValue(file.toFile(), RewriteReviewDocument.class);
      return document.reviews() == null ? List.of() : document.reviews();
    } catch (IOException exception) {
      throw new IllegalArgumentException("Unable to read rewrite reviews: " + file, exception);
    }
  }

  private Quantity parsedQuantity(String value) {
    if (value == null) return null;
    Matcher matcher = QUANTITY.matcher(value.replaceAll("\\s+", ""));
    if (!matcher.find()) return null;
    return new Quantity(new BigDecimal(matcher.group(1)), matcher.group(2), "FIXED");
  }

  private String ingredientCategory(String name) {
    if (name.matches(".*(肉|排骨|鸡|鸭|鹅|鱼|虾|蟹|蛤|蛎|贝|肠|培根).*")) return "肉禽水产";
    if (name.matches(".*(蛋|奶|芝士|乳).*")) return "蛋奶";
    if (name.matches(".*(米|面|粉|馒头|饼|豆|麦).*")) return "粮油主食";
    if (name.matches(".*(油|盐|糖|酱|醋|酒|椒|姜|葱|蒜|香料|味精).*")) return "调味料";
    return "蔬菜及其他";
  }

  private String componentName(String key) {
    String file = key.substring(key.lastIndexOf('/') + 1);
    return file.replaceFirst("\\.md$", "");
  }

  private String dataStatus(boolean hasPrice, boolean procurementReady) {
    if (hasPrice && procurementReady) return "READY";
    if (!hasPrice && !procurementReady) return "NEEDS_BOTH";
    return hasPrice ? "NEEDS_PURCHASE_DATA" : "NEEDS_PRICE";
  }

  private String sourceUrl(String sourceKey) {
    return SOURCE_BASE_URL + sourceKey.replaceFirst("\\.md$", "");
  }

  private Path backendRoot(Path outputDir) {
    Path absolute = outputDir.toAbsolutePath().normalize();
    Path current = absolute;
    while (current != null && !"backend".equals(current.getFileName() == null ? "" : current.getFileName().toString())) {
      current = current.getParent();
    }
    if (current == null) throw new IllegalArgumentException("Output directory must be under backend: " + outputDir);
    return current;
  }

  private int countRepositoryImages(Path sourceRoot) {
    try (var stream = Files.walk(sourceRoot.resolve("images"))) {
      return (int) stream.filter(Files::isRegularFile).count();
    } catch (IOException exception) {
      throw new IllegalArgumentException("Unable to count repository images", exception);
    }
  }

  private String literal(Object value) {
    if (value == null) return "NULL";
    if (value instanceof Boolean bool) return bool ? "1" : "0";
    if (value instanceof Number number) return number.toString();
    return "'" + value.toString().replace("'", "''").replace('\n', ' ').replace('\r', ' ') + "'";
  }

  private String json(Object value) {
    try {
      return JSON.writeValueAsString(value).replace("\r\n", "\n") + "\n";
    } catch (IOException exception) {
      throw new IllegalArgumentException("Unable to serialize sync artifact", exception);
    }
  }

  private void writeJson(Path file, Object value) {
    try {
      Files.createDirectories(file.toAbsolutePath().getParent());
      Files.writeString(file, json(value), StandardCharsets.UTF_8);
    } catch (IOException exception) {
      throw new IllegalArgumentException("Unable to write draft review file: " + file, exception);
    }
  }

  private String sha256(Path file) {
    try {
      byte[] digest = MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file));
      StringBuilder result = new StringBuilder();
      for (byte current : digest) result.append(String.format("%02x", current));
      return result.toString();
    } catch (IOException | NoSuchAlgorithmException exception) {
      throw new IllegalArgumentException("Unable to hash source file: " + file, exception);
    }
  }

  private String normalizePath(Path value) {
    return value.toString().replace('\\', '/');
  }

  private record ParsedRecipe(Path file, SourceRecipe recipe) { }
  private record RecipeGroup(String normalizedTitle, ParsedRecipe primary, List<ParsedRecipe> items) { }
  private record TemplateIdentity(long id, String code, BaselineTemplate local) { }
  private record AllocationPlan(Map<String, TemplateIdentity> identities,
                                List<TemplateIdAllocation> proposals) { }
  private record Quantity(BigDecimal value, String unit, String calcType) { }
  private record BuildResult(String sql, Map<String, Object> manifest, Map<String, Object> report) { }
  private record RewriteReviewDocument(int schemaVersion, List<RewriteReview> reviews) { }

  private record IngredientRow(String sourceLineKey, String name, String category, String status,
                               Quantity quantity, String sourceText, String sourceQuantityText,
                               Long componentTemplateId, BigDecimal componentMultiplier, int sortOrder) {
    boolean procurementReady() { return "VERIFIED".equals(status) || "NOT_APPLICABLE".equals(status); }
    Map<String, Object> values(long templateId) {
      Map<String, Object> values = new LinkedHashMap<>();
      values.put("template_id", templateId); values.put("ingredient_name", name);
      values.put("ingredient_category", category);
      values.put("quantity", quantity == null ? null : quantity.value());
      values.put("unit", quantity == null ? null : quantity.unit());
      values.put("calc_type", quantity == null ? null : quantity.calcType());
      values.put("source_text", sourceText); values.put("source_quantity_text", sourceQuantityText);
      values.put("quantity_status", status); values.put("component_template_id", componentTemplateId);
      values.put("source_line_key", sourceLineKey);
      values.put("component_occurrence_key", componentTemplateId == null ? null : sourceLineKey);
      values.put("component_multiplier", componentMultiplier); values.put("sort_order", sortOrder);
      return values;
    }
    Map<String, Object> manifest() {
      Map<String, Object> values = new LinkedHashMap<>();
      values.put("sourceLineKey", sourceLineKey); values.put("ingredientName", name);
      values.put("ingredientCategory", category); values.put("quantityStatus", status);
      values.put("quantity", quantity == null ? null : quantity.value());
      values.put("unit", quantity == null ? null : quantity.unit());
      values.put("sourceText", sourceText); values.put("sourceQuantityText", sourceQuantityText);
      values.put("componentTemplateId", componentTemplateId); return values;
    }
  }
}
