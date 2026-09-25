package com.familykitchen.feedback;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.familykitchen.common.error.*;
import com.familykitchen.common.security.*;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

/** Endpoint authorization checks without Boot, a network listener, or a database. */
class FeedbackControllerTest {
  final CurrentUserProvider users=mock(CurrentUserProvider.class);
  final FeedbackMapper mapper=mock(FeedbackMapper.class);
  final FeedbackService service=new FeedbackService(mapper);
  final FeedbackImageService images=mock(FeedbackImageService.class);
  final MockHttpServletRequest request=new MockHttpServletRequest();
  @Test void allUserAndAdminEndpointsRequireAuthentication() {
    when(users.require(request)).thenThrow(new BusinessException(ErrorCode.UNAUTHORIZED,"请登录"));
    var user=new FeedbackController(users,service,images);var admin=new AdminFeedbackController(users,service);
    assertThatThrownBy(()->user.list(request,1,20)).hasMessage("请登录");
    assertThatThrownBy(()->user.detail(request,1)).hasMessage("请登录");
    assertThatThrownBy(()->user.create(request,null)).hasMessage("请登录");
    assertThatThrownBy(()->user.upload(request,null)).hasMessage("请登录");
    assertThatThrownBy(()->user.image(request,"x")).hasMessage("请登录");
    assertThatThrownBy(()->admin.list(request,null,null,1,20)).hasMessage("请登录");
    assertThatThrownBy(()->admin.detail(request,1)).hasMessage("请登录");
    assertThatThrownBy(()->admin.update(request,1,null)).hasMessage("请登录");
    verifyNoInteractions(mapper,images);
  }
  @Test void merchantCannotUseAnyAdminEndpoint() {
    when(users.require(request)).thenReturn(new CurrentUserContext(1L,9L,null,1L,"user",Set.of("MERCHANT_ADMIN"),Set.of()));
    var admin=new AdminFeedbackController(users,service);
    assertThatThrownBy(()->admin.list(request,null,null,1,20)).hasMessageContaining("平台");
    assertThatThrownBy(()->admin.detail(request,1)).hasMessageContaining("平台");
    assertThatThrownBy(()->admin.update(request,1,null)).hasMessageContaining("平台");verifyNoInteractions(mapper);
  }
  @Test void imageResponsesArePrivateAndNosniff() {
    var user=new CurrentUserContext(1L,null,null,1L,"user",Set.of(),Set.of());when(users.require(request)).thenReturn(user);
    when(images.read(user,"x")).thenReturn(new FeedbackImageService.ImageData("image/png",new byte[]{1,2}));
    var response=new FeedbackController(users,service,images).image(request,"x");
    assertThat(response.getHeaders().getCacheControl()).isEqualTo("no-store");
    assertThat(response.getHeaders().getFirst("X-Content-Type-Options")).isEqualTo("nosniff");
    assertThat(response.getHeaders().getContentType().toString()).isEqualTo("image/png");
  }
}
