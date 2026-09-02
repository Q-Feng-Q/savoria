package com.familykitchen.recipesync.rewrite;

import com.familykitchen.recipesync.model.SourceStep;
import java.text.Normalizer;

/** Reorganizes source wording while preserving all cooking facts and structured fields. */
public final class StepFactRewriter {

  /** Rewrites one ordered source step without inventing absent facts. */
  public RewrittenStep rewrite(SourceStep source) {
    String content = Normalizer.normalize(source.content(), Normalizer.Form.NFC)
        .replace("下入", "加入")
        .replace("出品", "即可完成")
        .replace("备用", "留待后续使用")
        .replaceAll("\\s+", " ")
        .trim();
    return new RewrittenStep(source.stepNo(), source.title(), content, source.durationSeconds(),
        source.temperatureText(), source.heatLevel());
  }
}
