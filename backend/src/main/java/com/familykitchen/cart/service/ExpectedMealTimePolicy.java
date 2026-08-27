package com.familykitchen.cart.service;

import com.familykitchen.common.error.BusinessException;
import com.familykitchen.common.error.ErrorCode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.springframework.stereotype.Component;

/** Enforces today-only expected meal times with a fifteen-minute lead and grid. */
@Component
public class ExpectedMealTimePolicy {
  /** Shanghai business time zone. */
  public static final ZoneId BUSINESS_ZONE=ZoneId.of("Asia/Shanghai");
  /** Time grid size. */
  public static final int STEP_MINUTES=15;
  private final Clock clock;

  /** Creates the production policy with the Shanghai system clock. */
  public ExpectedMealTimePolicy(){this(Clock.system(BUSINESS_ZONE));}

  /** Creates a deterministic policy.
   * @param clock business clock
   */
  public ExpectedMealTimePolicy(Clock clock){this.clock=clock.withZone(BUSINESS_ZONE);}

  /** Returns authoritative time-selection metadata.
   * @return current time and minimum selectable time
   */
  public Snapshot snapshot(){
    LocalDateTime now=LocalDateTime.now(clock);LocalDate today=now.toLocalDate();
    LocalDateTime minimum=ceilQuarter(now.plusMinutes(STEP_MINUTES));
    boolean ended=!minimum.toLocalDate().equals(today);
    return new Snapshot(now,today,ended?null:minimum,STEP_MINUTES,ended);
  }

  /** Validates a client-selected expected time against the authoritative clock.
   * @param selected selected local business time
   * @return validated time
   */
  public LocalDateTime requireValid(LocalDateTime selected){
    Snapshot state=snapshot();
    if(selected==null)bad("预计用餐时间不能为空");
    if(state.bookingEnded())bad("今天已无可预约用餐时间");
    if(!selected.toLocalDate().equals(state.serverDate()))bad("预计用餐时间只能选择今天");
    if(selected.getSecond()!=0||selected.getNano()!=0||selected.getMinute()%STEP_MINUTES!=0)bad("预计用餐时间必须按 15 分钟选择");
    if(selected.isBefore(state.minimumExpectedMealTime()))bad("预计用餐时间过早");
    return selected;
  }

  private static LocalDateTime ceilQuarter(LocalDateTime value){LocalDateTime minute=value.withSecond(0).withNano(0);int remainder=minute.getMinute()%STEP_MINUTES;return remainder==0?minute:minute.plusMinutes(STEP_MINUTES-remainder);}
  private static void bad(String message){throw new BusinessException(ErrorCode.BAD_REQUEST,message);}

  /** Authoritative booking metadata.
   * @param serverNow current business time
   * @param serverDate current business date
   * @param minimumExpectedMealTime minimum selectable time, or null when ended
   * @param timeStepMinutes grid size
   * @param bookingEnded whether today has no selectable time
   */
  public record Snapshot(LocalDateTime serverNow,LocalDate serverDate,
      LocalDateTime minimumExpectedMealTime,int timeStepMinutes,boolean bookingEnded) {}
}
