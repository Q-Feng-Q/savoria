package com.familykitchen.feedback;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.familykitchen.common.security.CurrentUserContext;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class FeedbackServiceTest {
  final FeedbackMapper mapper = mock(FeedbackMapper.class);
  final FeedbackService service = new FeedbackService(mapper);
  final CurrentUserContext user = new CurrentUserContext(1L,null,null,1L,"user",Set.of(),Set.of());
  @Test void trimsAndCreatesForUserWithoutFamily() {
    when(mapper.lockUser(1L)).thenReturn(1L);
    doAnswer(i -> { ((FeedbackRow)i.getArgument(0)).id=8L; return 1; }).when(mapper).insert(any());
    var result=service.create(user,new FeedbackService.Create("req","BUG","  broken  ",List.of()));
    assertThat(result.get("content")).isEqualTo("broken");
    assertThat(result.get("status")).isEqualTo("OPEN");
  }
  @Test void cannotReadOtherOwner() {
    var row=new FeedbackRow();row.ownerUserId=2L;when(mapper.find(8L)).thenReturn(row);
    assertThatThrownBy(()->service.detail(user,8L,false)).hasMessageContaining("不存在");
  }
  @Test void merchantCannotReadAdminList() {
    assertThatThrownBy(()->service.list(user,true,null,null,1,20)).hasMessageContaining("平台");
  }
  @Test void rejectsDuplicateImagesAndInvalidContent() {
    assertThatThrownBy(()->service.create(user,new FeedbackService.Create("req","BUG","ok",List.of("a","a")))).hasMessageContaining("图片");
    assertThatThrownBy(()->service.create(user,new FeedbackService.Create("req","BUG"," ",List.of()))).hasMessageContaining("描述");
  }
  @Test void quotaStopsInsert() {
    when(mapper.lockUser(1L)).thenReturn(1L);when(mapper.countRecentFeedback(eq(1L),any())).thenReturn(10);
    assertThatThrownBy(()->service.create(user,new FeedbackService.Create("r","BUG","ok",List.of()))).hasMessageContaining("10");
    verify(mapper,never()).insert(any());
  }
  @Test void sameRequestReturnsOriginalButChangedRequestConflicts() {
    when(mapper.lockUser(1L)).thenReturn(1L);
    var old=new FeedbackRow();old.id=7L;old.ownerUserId=1L;old.type="BUG";old.content="ok";old.status="OPEN";
    when(mapper.byRequest(1L,"r")).thenReturn(old);when(mapper.images(7L)).thenReturn(List.of());
    assertThat(service.create(user,new FeedbackService.Create("r","BUG"," ok ",List.of())).get("feedbackId")).isEqualTo(7L);
    assertThatThrownBy(()->service.create(user,new FeedbackService.Create("r","BUG","changed",List.of()))).hasMessageContaining("requestId");
    verify(mapper,never()).countRecentFeedback(anyLong(),any());
  }
  @Test void resolvedAndClosedRequireReplyAndVersion() {
    var admin=new CurrentUserContext(2L,null,null,2L,"user",Set.of("PLATFORM_ADMIN"),Set.of());
    for(String status:List.of("RESOLVED","CLOSED"))assertThatThrownBy(()->service.update(admin,1,new FeedbackService.Update(status," ",0))).hasMessageContaining("回复");
    assertThatThrownBy(()->service.update(admin,1,new FeedbackService.Update("OPEN","x".repeat(2001),0))).hasMessageContaining("2000");
    verify(mapper,never()).update(any());
  }
}
