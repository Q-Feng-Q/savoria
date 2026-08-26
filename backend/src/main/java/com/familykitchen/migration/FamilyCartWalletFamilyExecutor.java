package com.familykitchen.migration;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Owns the independently committed, resumable unit of work for one family. */
@Service
public class FamilyCartWalletFamilyExecutor {
  private final FamilyCartWalletMigrationMapper mapper;

  /**
   * Creates the independent family transaction executor.
   *
   * @param mapper migration mapper
   */
  public FamilyCartWalletFamilyExecutor(FamilyCartWalletMigrationMapper mapper) {
    this.mapper = mapper;
  }

  /**
   * Migrates one family in a new transaction.
   *
   * @param batchId batch identifier
   * @param epoch drain epoch
   * @param familyId family identifier
   * @return whether the family transaction completed
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public boolean execute(long batchId, long epoch, long familyId) {
    List<Map<String, Object>> wallets = mapper.selectWalletSources(familyId);
    mapper.ensureFamilyWallet(familyId);
    for (Map<String, Object> wallet : wallets) {
      long userId = number(wallet, "userId").longValue();
      BigDecimal available = FamilyCartWalletMigrationService.decimal(wallet.get("availableAmount"));
      BigDecimal frozen = FamilyCartWalletMigrationService.decimal(wallet.get("frozenAmount"));
      if (mapper.insertWalletSource(batchId, userId, familyId, available, frozen) == 0) continue;
      mapper.creditFamilyWallet(familyId, available, frozen);
      mapper.zeroMemberWallet(userId);
      mapper.insertTransferLedger(batchId, userId, available, frozen);
      mapper.insertFamilyLedger(batchId, userId, familyId, available, frozen);
    }
    mapper.insertOrderHolds(familyId);
    mergeCarts(batchId, familyId);
    return true;
  }

  private void mergeCarts(long batchId, long familyId) {
    List<Map<String, Object>> carts = new ArrayList<>(mapper.selectActiveCarts(batchId, familyId));
    if (carts.isEmpty()) return;
    carts.sort(Comparator.comparing(row -> number(row, "id").longValue()));
    long targetId = number(carts.get(0), "id").longValue();
    String newestRemark = null;
    for (Map<String, Object> cart : carts) {
      String cartRemark = FamilyCartWalletMigrationService.text(cart.get("remark"));
      if (!cartRemark.isBlank()) newestRemark = cartRemark;
      long sourceId = number(cart, "id").longValue();
      long ownerId = number(cart, "userId").longValue();
      for (Map<String, Object> item : mapper.selectCartItems(sourceId)) {
        long dishId = number(item, "dishId").longValue();
        int quantity = number(item, "quantity").intValue();
        BigDecimal price = FamilyCartWalletMigrationService.decimal(item.get("currentPrice"));
        String itemRemark = FamilyCartWalletMigrationService.text(item.get("itemRemark"));
        Long targetItemId = mapper.findTargetItem(targetId, dishId);
        if (targetItemId == null) {
          mapper.insertTargetItem(targetId, dishId,
              FamilyCartWalletMigrationService.text(item.get("dishName")), quantity, price, itemRemark);
          targetItemId = mapper.lastInsertId();
        } else if (sourceId != targetId) {
          mapper.updateTargetItem(targetItemId, quantity, price, itemRemark);
        }
        mapper.upsertSelection(targetItemId, ownerId, quantity, itemRemark);
      }
      mapper.markCartMigrated(batchId, sourceId, targetId, familyId);
      if (sourceId != targetId) mapper.absorbCart(sourceId);
    }
    mapper.updateTargetCart(targetId, newestRemark);
  }

  private static Number number(Map<String, Object> row, String key) {
    Object value = row.get(key);
    if (!(value instanceof Number number)) throw new IllegalStateException("missing numeric " + key);
    return number;
  }
}
