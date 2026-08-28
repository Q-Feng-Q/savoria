package com.familykitchen.cart.service;

import com.familykitchen.cart.mapper.CartMapper;
import com.familykitchen.cart.model.entity.CartEntity;
import com.familykitchen.cart.model.entity.CartItemEntity;
import com.familykitchen.cart.model.entity.CartItemSelectionEntity;
import com.familykitchen.family.mapper.FamilyRelationMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Removes invalid member selections from the locked active family cart. */
@Service
public class ActiveCartMemberCleanupService {
  private final CartMapper mapper;
  private final FamilyRelationMapper relations;

  /**
   * Creates the cleanup service.
   *
   * @param mapper cart mapper
   * @param relations family relation mapper
   */
  public ActiveCartMemberCleanupService(CartMapper mapper, FamilyRelationMapper relations) {
    this.mapper = mapper;
    this.relations = relations;
  }

  /**
   * Removes one member's selections and recomputes aggregate quantities.
   *
   * @param familyId family identifier
   * @param memberId exiting member identifier
   * @return whether the active cart changed
   */
  @Transactional
  public boolean removeMember(Long familyId, Long memberId) {
    CartEntity cart = mapper.selectFamilyActiveCartForUpdate(familyId);
    if (cart == null) {
      return false;
    }
    List<CartItemEntity> items = mapper.selectCartItemsForUpdate(cart.getId());
    Map<Long, List<CartItemSelectionEntity>> selections = lockSelections(items);
    lockParticipants(familyId, selections);
    boolean changed = false;
    for (CartItemEntity item : items) {
      List<CartItemSelectionEntity> rows = selections.getOrDefault(item.getId(), List.of());
      if (rows.stream().noneMatch(row -> memberId.equals(row.userId))) {
        continue;
      }
      mapper.deleteSelection(item.getId(), memberId);
      int aggregate = rows.stream().filter(row -> !memberId.equals(row.userId))
          .mapToInt(row -> row.quantity).sum();
      if (aggregate == 0) {
        mapper.deleteAggregateItem(item.getId());
      } else {
        item.setQuantity(aggregate);
        mapper.updateCartItem(item);
      }
      changed = true;
    }
    if (changed && mapper.bumpVersion(
        cart.getId(), familyId, cart.getVersion() == null ? 0 : cart.getVersion()) != 1) {
      throw new IllegalStateException("active cart version changed while locked");
    }
    return changed;
  }

  /**
   * Removes all active-cart selections before a family is dissolved.
   *
   * @param familyId family identifier
   * @return whether the active cart changed
   */
  @Transactional
  public boolean removeFamily(Long familyId) {
    CartEntity cart = mapper.selectFamilyActiveCartForUpdate(familyId);
    if (cart == null) {
      return false;
    }
    List<CartItemEntity> items = mapper.selectCartItemsForUpdate(cart.getId());
    if (items.isEmpty()) return false;
    Map<Long, List<CartItemSelectionEntity>> selections = lockSelections(items);
    lockParticipants(familyId, selections);
    mapper.deleteAllSelections(cart.getId());
    mapper.deleteAllAggregateItems(cart.getId());
    if (mapper.bumpVersion(
        cart.getId(), familyId, cart.getVersion() == null ? 0 : cart.getVersion()) != 1) {
      throw new IllegalStateException("active cart version changed while locked");
    }
    return true;
  }

  private Map<Long, List<CartItemSelectionEntity>> lockSelections(List<CartItemEntity> items) {
    Map<Long, List<CartItemSelectionEntity>> rows = new LinkedHashMap<>();
    for (CartItemEntity item : items) {
      rows.put(item.getId(), mapper.selectSelectionsForUpdate(item.getId()));
    }
    return rows;
  }

  private void lockParticipants(
      Long familyId, Map<Long, List<CartItemSelectionEntity>> selections) {
    Set<Long> participantIds = new LinkedHashSet<>();
    for (List<CartItemSelectionEntity> rows : selections.values()) {
      for (CartItemSelectionEntity row : rows) participantIds.add(row.userId);
    }
    if (!participantIds.isEmpty()) {
      relations.lockActiveParticipants(familyId, new ArrayList<>(participantIds));
    }
  }
}
