package com.familykitchen.recipesync.merge;

/** Explicit merge failure that must be resolved in versioned configuration. */
public final class MergeFailure extends IllegalArgumentException {

  private final String code;

  public MergeFailure(String code, String message) {
    super(message);
    this.code = code;
  }

  public String code() {
    return code;
  }
}
