package com.familykitchen.dish;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.familykitchen.common.error.BusinessException;
import com.familykitchen.dish.mapper.DishMapper;
import com.familykitchen.dish.mapper.DishReviewMapper;
import com.familykitchen.dish.model.dto.DishRequest;
import com.familykitchen.dish.model.entity.DishEntity;
import com.familykitchen.dish.model.entity.DishReviewSubmissionDO;
import com.familykitchen.dish.service.MerchantDishMutationLock;
import com.familykitchen.dish.service.impl.DishReviewServiceImpl;
import com.familykitchen.family.mapper.FamilyMapper;
import com.familykitchen.system.mapper.SystemAuditMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

/** Review approval must share the same merchant-to-dish mutation lock. */
class DishReviewFeaturedStatusTest {

  @Test
  void existingDishApprovalLocksAndRevalidatesBeforeApplyingInactiveSnapshot() throws Exception {
    DishReviewMapper reviews = mock(DishReviewMapper.class);
    DishMapper dishes = mock(DishMapper.class);
    MerchantDishMutationLock lock = mock(MerchantDishMutationLock.class);
    DishReviewSubmissionDO review = review(7L, 2L, 8L, "inactive");
    when(reviews.selectById(7L)).thenReturn(review);
    when(reviews.approve(7L, 99L, "ok")).thenReturn(1);
    when(dishes.countCategoryOwnership(2L, 3L)).thenReturn(1);
    when(lock.lock(2L, 8L)).thenReturn(new DishEntity());
    when(dishes.updateDish(any())).thenReturn(1);

    new DishReviewServiceImpl(reviews, dishes, new ObjectMapper(),
        mock(SystemAuditMapper.class), lock, mock(FamilyMapper.class)).approve(7L, 99L, "ok");

    InOrder order = inOrder(lock, dishes);
    order.verify(lock).lock(2L, 8L);
    order.verify(dishes).updateDish(any());
  }

  @Test
  void existingDishApprovalStopsWhenLockedDishNoLongerExists() throws Exception {
    DishReviewMapper reviews = mock(DishReviewMapper.class);
    DishMapper dishes = mock(DishMapper.class);
    MerchantDishMutationLock lock = mock(MerchantDishMutationLock.class);
    when(reviews.selectById(7L)).thenReturn(review(7L, 2L, 8L, "active"));
    when(dishes.countCategoryOwnership(2L, 3L)).thenReturn(1);
    when(lock.lock(2L, 8L)).thenThrow(new BusinessException(
        com.familykitchen.common.error.ErrorCode.NOT_FOUND, "未找到菜品"));

    assertThrows(BusinessException.class, () -> new DishReviewServiceImpl(reviews, dishes,
        new ObjectMapper(), mock(SystemAuditMapper.class), lock, mock(FamilyMapper.class)).approve(7L, 99L, "ok"));

    verify(dishes, never()).updateDish(any());
    verify(reviews, never()).approve(any(), any(), any());
  }

  @Test
  void newDishApprovalDoesNotTakeDishMutationLock() throws Exception {
    DishReviewMapper reviews = mock(DishReviewMapper.class);
    DishMapper dishes = mock(DishMapper.class);
    MerchantDishMutationLock lock = mock(MerchantDishMutationLock.class);
    when(reviews.selectById(7L)).thenReturn(review(7L, 2L, null, "inactive"));
    when(reviews.approve(7L, 99L, "ok")).thenReturn(1);
    when(dishes.countCategoryOwnership(2L, 3L)).thenReturn(1);

    new DishReviewServiceImpl(reviews, dishes, new ObjectMapper(),
        mock(SystemAuditMapper.class), lock, mock(FamilyMapper.class)).approve(7L, 99L, "ok");

    verify(lock, never()).lock(any(), any());
    verify(dishes).insertDish(any());
  }

  @Test
  void newActiveDishApprovalEnablesDishForEveryActiveMerchantFamily() throws Exception {
    DishReviewMapper reviews = mock(DishReviewMapper.class);
    DishMapper dishes = mock(DishMapper.class);
    FamilyMapper families = mock(FamilyMapper.class);
    when(reviews.selectById(7L)).thenReturn(review(7L, 2L, null, "active"));
    when(reviews.approve(7L, 99L, "ok")).thenReturn(1);
    when(dishes.countCategoryOwnership(2L, 3L)).thenReturn(1);
    doAnswer(invocation -> {
      DishEntity inserted = invocation.getArgument(0);
      inserted.setId(8L);
      return 1;
    }).when(dishes).insertDish(any());

    new DishReviewServiceImpl(reviews, dishes, new ObjectMapper(),
        mock(SystemAuditMapper.class), mock(MerchantDishMutationLock.class), families)
        .approve(7L, 99L, "ok");

    verify(families).enableDishForActiveFamilies(2L, 8L);
  }

  @Test
  void fullDishUpdateClearsFeaturedInSameInactiveStatement() throws Exception {
    String xml = Files.readString(Path.of("src/main/resources/mapper/dish/DishMapper.xml"))
        .toLowerCase().replaceAll("\\s+", " ");

    assertTrue(xml.contains("featured_at = case when lower(#{status}) = 'inactive' then null else featured_at end"));
    assertTrue(xml.contains("update dishes set status = 'inactive', featured_at = null"));
  }

  private static DishReviewSubmissionDO review(Long id, Long merchantId, Long dishId,
                                                String status) throws Exception {
    DishRequest request = new DishRequest("鱼", 3L, "", "", BigDecimal.TEN,
        List.of(), List.of(), status);
    DishReviewSubmissionDO review = new DishReviewSubmissionDO();
    review.setId(id);
    review.setMerchantId(merchantId);
    review.setTargetDishId(dishId);
    review.setStatus("PENDING");
    review.setSnapshotJson(new ObjectMapper().writeValueAsString(request));
    return review;
  }
}
