package com.familykitchen.notebook;

import com.familykitchen.common.error.BusinessException;
import com.familykitchen.common.error.ErrorCode;
import java.time.LocalDate;
import java.util.Objects;
import java.util.function.IntSupplier;

/** Applies the current platform limit to inclusive notebook date ranges. */
public final class NotebookRangePolicy {
  private final IntSupplier maxMonths;

  /** Creates a policy backed by the current effective month limit.
   * @param maxMonths provider of the current limit */
  public NotebookRangePolicy(IntSupplier maxMonths) {
    this.maxMonths = Objects.requireNonNull(maxMonths);
  }

  /** Validates the inclusive dates by counting every touched calendar month.
   * @param from first date
   * @param to last date */
  public void requireAllowed(LocalDate from, LocalDate to) {
    if (from == null || to == null || from.isAfter(to)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "Invalid notebook date range");
    }
    int limit = maxMonths.getAsInt();
    if (limit < 1 || limit > 36) limit = 36;
    long months = (long) (to.getYear() - from.getYear()) * 12
        + to.getMonthValue() - from.getMonthValue() + 1;
    if (months > limit) {
      throw new BusinessException(ErrorCode.BAD_REQUEST,
          "Notebook query range exceeds " + limit + " calendar months");
    }
  }
}
