package com.familykitchen.recipesync.rewrite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.familykitchen.recipesync.model.SourceStep;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Ensures rewritten steps retain every structured cooking fact. */
class StepFactRewriterTest {

  private final StepFactRewriter rewriter = new StepFactRewriter();

  @Test
  void rewritesWordingWhileRetainingOrderQuantityTimeTemperatureHeatAndCompletion() {
    SourceStep source = new SourceStep(2, "烧制",
        "下入 500g 豆腐，在 160℃ 下中火烧 3 分 30 秒，汤汁浓稠后出品。",
        210, "160℃", "中火");

    RewrittenStep rewritten = rewriter.rewrite(source);

    assertNotEquals(source.content(), rewritten.content());
    assertTrue(rewritten.content().contains("500g"));
    assertTrue(rewritten.content().contains("160℃"));
    assertTrue(rewritten.content().contains("中火"));
    assertTrue(rewritten.content().contains("3 分 30 秒"));
    assertTrue(rewritten.content().contains("汤汁浓稠"));
    assertEquals(210, rewritten.durationSeconds());
  }

  @Test
  void identicalTextScoresOne() {
    SimilarityGate gate = new SimilarityGate();

    assertEquals(1.0, gate.score("加入豆腐翻炒", "加入豆腐翻炒"));
    assertTrue(gate.score("加入豆腐翻炒", "烤箱预热后烘烤") < 0.5);
  }

  @Test
  void highSimilarityRequiresAnApprovedReviewedDisposition() {
    RewriteReviewGate gate = new RewriteReviewGate();
    StepReviewCandidate candidate = new StepReviewCandidate("炒菜/示例.md", 1, 0.92);

    assertEquals(List.of("MISSING_REWRITE_REVIEW:炒菜/示例.md#1"),
        gate.unresolved(List.of(candidate), 0.82, List.of()));
    assertEquals(List.of("REWRITE_REQUIRED:炒菜/示例.md#1"), gate.unresolved(
        List.of(candidate), 0.82,
        List.of(new RewriteReview("炒菜/示例.md", 1, "reviewer",
            Instant.parse("2026-09-01T00:00:00Z"), "REWRITE_REQUIRED", null))));
    assertTrue(gate.unresolved(List.of(candidate), 0.82,
        List.of(new RewriteReview("炒菜/示例.md", 1, "reviewer",
            Instant.parse("2026-09-01T00:00:00Z"), "APPROVED", null))).isEmpty());
  }
}
