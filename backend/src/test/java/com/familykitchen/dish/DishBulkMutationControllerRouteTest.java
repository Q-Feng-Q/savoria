package com.familykitchen.dish;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.familykitchen.common.security.CurrentUserContext;
import com.familykitchen.common.security.CurrentUserProvider;
import com.familykitchen.dish.controller.DishController;
import com.familykitchen.dish.model.dto.BatchDishMutationRequest;
import com.familykitchen.dish.service.DishApplicationService;
import com.familykitchen.dish.service.DishReviewService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.PostMapping;

/** Verifies exact merchant batch mutation routes and delegation. */
class DishBulkMutationControllerRouteTest {

  @Test
  void exposesBatchDeleteAndRestorePostRoutes() throws Exception {
    assertNotNull(DishController.class
        .getMethod("batchDelete", HttpServletRequest.class, BatchDishMutationRequest.class)
        .getAnnotation(PostMapping.class));
    assertNotNull(DishController.class
        .getMethod("batchRestore", HttpServletRequest.class, BatchDishMutationRequest.class)
        .getAnnotation(PostMapping.class));

    CurrentUserContext user = new CurrentUserContext(
        17L, 29L, null, null, null, Set.of("MERCHANT_ADMIN"), Set.of());
    CurrentUserProvider users = mock(CurrentUserProvider.class);
    DishApplicationService service = mock(DishApplicationService.class);
    HttpServletRequest servlet = mock(HttpServletRequest.class);
    BatchDishMutationRequest request = new BatchDishMutationRequest(List.of(2L, 7L));
    when(users.require(servlet)).thenReturn(user);
    DishController controller = new DishController(users, service, mock(DishReviewService.class));

    controller.batchDelete(servlet, request);
    controller.batchRestore(servlet, request);

    verify(service).bulkDelete(user, request);
    verify(service).bulkRestore(user, request);
  }
}
