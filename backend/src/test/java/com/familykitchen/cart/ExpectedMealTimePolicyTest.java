package com.familykitchen.cart;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.familykitchen.cart.service.ExpectedMealTimePolicy;
import com.familykitchen.common.error.BusinessException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

/** Verifies today-only Shanghai expected-meal-time boundaries. */
class ExpectedMealTimePolicyTest {
  private static final ZoneId SHANGHAI=ZoneId.of("Asia/Shanghai");

  @Test void roundsFourteenOhEightToFourteenThirty(){assertThat(policy("2026-08-27T06:08:00Z").snapshot().minimumExpectedMealTime()).isEqualTo(LocalDateTime.of(2026,8,27,14,30));}
  @Test void exactQuarterStillRequiresFifteenMinuteLead(){assertThat(policy("2026-08-27T06:15:00Z").snapshot().minimumExpectedMealTime()).isEqualTo(LocalDateTime.of(2026,8,27,14,30));}
  @Test void rejectsYesterdayTomorrowPastAndNonQuarterHour(){ExpectedMealTimePolicy p=policy("2026-08-27T06:08:00Z");for(LocalDateTime invalid:new LocalDateTime[]{LocalDateTime.of(2026,8,26,15,0),LocalDateTime.of(2026,8,28,15,0),LocalDateTime.of(2026,8,27,14,15),LocalDateTime.of(2026,8,27,14,31)})assertThatThrownBy(()->p.requireValid(invalid)).isInstanceOf(BusinessException.class);}
  @Test void reportsBookingEndedWhenNoTodaySlotRemains(){var snapshot=policy("2026-08-27T15:31:00Z").snapshot();assertThat(snapshot.bookingEnded()).isTrue();assertThat(snapshot.minimumExpectedMealTime()).isNull();}
  private static ExpectedMealTimePolicy policy(String instant){return new ExpectedMealTimePolicy(Clock.fixed(Instant.parse(instant),SHANGHAI));}
}
