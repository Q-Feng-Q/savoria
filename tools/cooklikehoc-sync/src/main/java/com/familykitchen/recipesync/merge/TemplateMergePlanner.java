package com.familykitchen.recipesync.merge;

import com.familykitchen.recipesync.model.SourceRecipe;
import com.familykitchen.recipesync.normalize.RecipeNormalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Matches source recipes to local templates without using filesystem metadata. */
public final class TemplateMergePlanner {

  /** Produces a stable merge ordered by permanent template ID. */
  public List<MergedTemplate> plan(MergeInputs inputs) {
    validateAllocations(inputs.allocations(), inputs.allocationHighWaterMark());
    Map<Long, LocalTemplate> localsById = indexLocals(inputs.localTemplates());
    Map<String, LocalTemplate> localsBySourceKey = new HashMap<>();
    Map<String, LocalTemplate> localsByName = new HashMap<>();
    for (LocalTemplate local : inputs.localTemplates()) {
      if (local.sourceKey() != null) {
        localsBySourceKey.put(local.sourceKey(), local);
      }
      String normalizedName = RecipeNormalizer.normalizeName(local.name());
      if (localsByName.putIfAbsent(normalizedName, local) != null) {
        throw new MergeFailure("LOCAL_NAME_COLLISION", normalizedName);
      }
    }

    Map<String, List<SourceRecipe>> groups = groupSources(inputs);
    Set<Long> claimedLocalIds = new HashSet<>();
    List<MergedTemplate> merged = new ArrayList<>();
    for (Map.Entry<String, List<SourceRecipe>> entry : groups.entrySet()) {
      List<SourceRecipe> group = entry.getValue();
      SourceRecipe primary = primarySource(entry.getKey(), group, inputs);
      List<String> sourceKeys = orderedSourceKeys(primary.sourceKey(), group);
      LocalTemplate local = matchLocal(primary, sourceKeys, inputs, localsById,
          localsBySourceKey, localsByName);
      if (local != null) {
        if (!claimedLocalIds.add(local.id())) {
          throw new MergeFailure("LOCAL_TEMPLATE_ALREADY_CLAIMED", Long.toString(local.id()));
        }
        merged.add(new MergedTemplate(local.id(), local.templateCode(), primary.title(),
            local.description(), local.referencePrice(), "COOK_LIKE_HOC", sourceKeys, primary));
      } else {
        TemplateIdentity identity = inputs.allocations().get(primary.sourceKey());
        if (identity == null) {
          throw new MergeFailure("MISSING_ALLOCATION", primary.sourceKey());
        }
        merged.add(new MergedTemplate(identity.id(), identity.templateCode(), primary.title(),
            null, null, "COOK_LIKE_HOC", sourceKeys, primary));
      }
    }

    for (LocalTemplate local : inputs.localTemplates()) {
      if (!claimedLocalIds.contains(local.id())) {
        merged.add(new MergedTemplate(local.id(), local.templateCode(), local.name(),
            local.description(), local.referencePrice(), "LOCAL_EXTENSION", List.of(), null));
      }
    }
    merged.sort(Comparator.comparingLong(MergedTemplate::id));
    return List.copyOf(merged);
  }

  private Map<String, List<SourceRecipe>> groupSources(MergeInputs inputs) {
    List<SourceRecipe> sorted = inputs.sourceRecipes().stream()
        .sorted(Comparator.comparing(SourceRecipe::sourceKey))
        .toList();
    Map<String, List<SourceRecipe>> groups = new LinkedHashMap<>();
    for (SourceRecipe source : sorted) {
      groups.computeIfAbsent(RecipeNormalizer.normalizeName(source.title()), ignored -> new ArrayList<>())
          .add(source);
    }
    for (Map.Entry<String, List<SourceRecipe>> entry : groups.entrySet()) {
      if (entry.getValue().size() > 1
          && !inputs.primarySourceByNormalizedTitle().containsKey(entry.getKey())) {
        throw new MergeFailure("DUPLICATE_SOURCE_TITLE", entry.getKey());
      }
    }
    return groups;
  }

  private SourceRecipe primarySource(String normalizedTitle, List<SourceRecipe> group,
      MergeInputs inputs) {
    String configured = inputs.primarySourceByNormalizedTitle().get(normalizedTitle);
    if (configured == null) {
      return group.get(0);
    }
    return group.stream()
        .filter(source -> source.sourceKey().equals(configured))
        .findFirst()
        .orElseThrow(() -> new MergeFailure("INVALID_PRIMARY_SOURCE", configured));
  }

  private List<String> orderedSourceKeys(String primaryKey, List<SourceRecipe> group) {
    List<String> keys = group.stream().map(SourceRecipe::sourceKey).sorted().toList();
    List<String> result = new ArrayList<>();
    result.add(primaryKey);
    keys.stream().filter(key -> !key.equals(primaryKey)).forEach(result::add);
    return List.copyOf(result);
  }

  private LocalTemplate matchLocal(SourceRecipe primary, List<String> sourceKeys,
      MergeInputs inputs, Map<Long, LocalTemplate> localsById,
      Map<String, LocalTemplate> localsBySourceKey, Map<String, LocalTemplate> localsByName) {
    for (String sourceKey : sourceKeys) {
      LocalTemplate match = localsBySourceKey.get(sourceKey);
      if (match != null) {
        return match;
      }
    }
    for (String sourceKey : sourceKeys) {
      Long localId = inputs.explicitSourceMappings().get(sourceKey);
      if (localId != null) {
        LocalTemplate match = localsById.get(localId);
        if (match == null) {
          throw new MergeFailure("UNKNOWN_EXPLICIT_TEMPLATE", Long.toString(localId));
        }
        return match;
      }
    }
    String normalizedTitle = RecipeNormalizer.normalizeName(primary.title());
    LocalTemplate byName = localsByName.get(normalizedTitle);
    if (byName != null) {
      return byName;
    }
    Long aliasId = inputs.normalizedNameAliases().get(normalizedTitle);
    if (aliasId == null) {
      return null;
    }
    LocalTemplate match = localsById.get(aliasId);
    if (match == null) {
      throw new MergeFailure("UNKNOWN_ALIAS_TEMPLATE", Long.toString(aliasId));
    }
    return match;
  }

  private Map<Long, LocalTemplate> indexLocals(List<LocalTemplate> locals) {
    Map<Long, LocalTemplate> result = new HashMap<>();
    for (LocalTemplate local : locals) {
      if (result.putIfAbsent(local.id(), local) != null) {
        throw new MergeFailure("LOCAL_ID_COLLISION", Long.toString(local.id()));
      }
    }
    return result;
  }

  private void validateAllocations(Map<String, TemplateIdentity> allocations, long highWaterMark) {
    Set<Long> ids = new HashSet<>();
    Set<String> codes = new HashSet<>();
    long maximum = 0;
    for (Map.Entry<String, TemplateIdentity> entry : allocations.entrySet()) {
      TemplateIdentity identity = entry.getValue();
      if (!ids.add(identity.id()) || !codes.add(identity.templateCode())) {
        throw new MergeFailure("ALLOCATION_COLLISION", entry.getKey());
      }
      maximum = Math.max(maximum, identity.id());
    }
    if (maximum > highWaterMark) {
      throw new MergeFailure("ALLOCATION_ABOVE_HIGH_WATER_MARK", Long.toString(maximum));
    }
  }
}
