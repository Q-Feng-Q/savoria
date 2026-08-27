package com.familykitchen.common.idempotency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.familykitchen.common.error.BusinessException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/** Verifies claim, replay, and payload-conflict behavior. */
class CommandIdempotencyServiceTest {
  @Test void executesClaimOnceAndPersistsResult(){
    CommandIdempotencyMapper mapper=mock(CommandIdempotencyMapper.class);when(mapper.insertClaim(3,7,"SUBMIT","r1",hash())).thenReturn(1);
    when(mapper.lockClaim(3,7,"SUBMIT","r1")).thenReturn(Map.of("id",9L,"payloadHash",hash(),"state","PROCESSING"));when(mapper.complete(9,"order",91L,"{}" )).thenReturn(1);
    var service=new CommandIdempotencyService(mapper);var result=service.execute(command("dish=1"),()->new CommandIdempotencyService.Result("order",91L,"{}"));
    assertThat(result.resourceId()).isEqualTo(91L);verify(mapper).complete(9,"order",91L,"{}");
  }
  @Test void replaysCommittedResultWithoutRunningAction(){
    CommandIdempotencyMapper mapper=mock(CommandIdempotencyMapper.class);when(mapper.insertClaim(3,7,"SUBMIT","r1",hash())).thenReturn(0);
    when(mapper.lockClaim(3,7,"SUBMIT","r1")).thenReturn(Map.of("id",9L,"payloadHash",hash(),"state","COMPLETED","resourceType","order","resourceId",91L,"resultBody","{}"));
    AtomicInteger calls=new AtomicInteger();var result=new CommandIdempotencyService(mapper).execute(command("dish=1"),()->{calls.incrementAndGet();return null;});
    assertThat(result.resourceId()).isEqualTo(91L);assertThat(calls).hasValue(0);verify(mapper,never()).complete(9,"order",91L,"{}");
  }
  @Test void sameKeyWithDifferentPayloadConflicts(){
    CommandIdempotencyMapper mapper=mock(CommandIdempotencyMapper.class);when(mapper.insertClaim(3,7,"SUBMIT","r1",hash())).thenReturn(0);
    when(mapper.lockClaim(3,7,"SUBMIT","r1")).thenReturn(Map.of("id",9L,"payloadHash","different","state","COMPLETED"));
    assertThatThrownBy(()->new CommandIdempotencyService(mapper).execute(command("dish=1"),()->null)).isInstanceOf(BusinessException.class);
  }
  private static CommandIdempotencyService.Command command(String payload){return new CommandIdempotencyService.Command(3,7,"SUBMIT","r1",payload);}
  private static String hash(){return "7c6e72f0ed019252eec7863bb9624d3ca5c86262d5d6324a009753765c48f75e";}
}
