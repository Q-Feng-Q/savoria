package com.familykitchen.migration;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Owns one independently committed and lease-fenced family unit of work. */
@Service
public class FamilyCartWalletFamilyExecutor {
  private final FamilyCartWalletMigrationMapper mapper;
  private final FamilyWalletMigrationLeaseService leases;

  /**
   * Creates an independently transactional family executor.
   *
   * @param mapper migration persistence mapper
   * @param leases persistent runner lease service
   */
  public FamilyCartWalletFamilyExecutor(FamilyCartWalletMigrationMapper mapper,
      FamilyWalletMigrationLeaseService leases) {
    this.mapper = mapper;
    this.leases = leases;
  }

  /**
   * Migrates one eligible family under the current database fence.
   *
   * @param ownerToken runner owner token
   * @param batchId migration batch identifier
   * @param epoch verified drain epoch
   * @param familyId family identifier
   * @return {@code true} after the family is migrated or was already migrated
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public boolean execute(String ownerToken, long batchId, long epoch, long familyId) {
    leases.requireOwned(ownerToken, batchId, epoch);
    Map<String, Object> batch = required(mapper.lockBatch(batchId), "unknown batch");
    String status = text(batch.get("status"));
    if (!status.equals("BARRIER_PREFLIGHT") && !status.equals("EXECUTING")) {
      throw new IllegalStateException("EXECUTE requires barrier preflight");
    }
    Map<String, Object> cutover = required(mapper.lockCutover(), "missing cutover");
    if (!truth(cutover.get("maintenanceEnabled")) || !"DRAINED".equals(text(cutover.get("state")))
        || number(cutover, "activeBatchId") != batchId || number(cutover, "drainEpoch") != epoch
        || mapper.countActiveLeases() != 0) {
      throw new IllegalStateException("current drain proof is invalid");
    }
    Map<String, Object> progress = required(mapper.lockFamilyProgress(batchId, familyId),
        "family is not eligible for this batch");
    if ("MIGRATED".equals(text(progress.get("status")))) return true;
    if (!"PENDING".equals(text(progress.get("status")))) {
      throw new IllegalStateException("illegal family migration status");
    }

    List<Map<String, Object>> wallets = mapper.selectWalletSources(familyId);
    mapper.ensureFamilyWallet(familyId);
    for (Map<String, Object> wallet : wallets) {
      long userId = number(wallet, "userId");
      BigDecimal available = FamilyCartWalletMigrationService.decimal(wallet.get("availableAmount"));
      BigDecimal frozen = FamilyCartWalletMigrationService.decimal(wallet.get("frozenAmount"));
      if (available.scale() > 2 || frozen.scale() > 2 || available.signum() < 0 || frozen.signum() < 0) {
        throw new IllegalStateException("wallet changed after preflight");
      }
      if (mapper.insertWalletSource(batchId, userId, familyId, available, frozen) == 0) continue;
      mapper.creditFamilyWallet(familyId, available, frozen);
      mapper.zeroMemberWallet(userId);
      mapper.insertTransferLedger(batchId, userId, available, frozen);
      mapper.insertFamilyLedger(batchId, userId, familyId, available, frozen);
    }
    mapper.insertOrderHolds(familyId);
    mergeCarts(batchId, familyId);
    mapper.markFamilyMigrated(batchId, familyId);
    return true;
  }

  private void mergeCarts(long batchId, long familyId) {
    List<Map<String, Object>> carts = new ArrayList<>(mapper.selectActiveCarts(batchId, familyId));
    if (carts.isEmpty()) return;
    carts.sort(Comparator.<Map<String, Object>, String>comparing(row -> orderKey(row, "updatedAt"))
        .thenComparingLong(row -> number(row, "id")));
    Map<Long, List<Map<String, Object>>> itemsByCart = new HashMap<>();
    for (Map<String, Object> cart : carts) {
      if (!truth(cart.get("userExists")) || !truth(cart.get("activeMember"))) {
        throw new IllegalStateException("cart owner changed after preflight: " + number(cart, "id"));
      }
      List<Map<String, Object>> items = mapper.selectCartItems(number(cart, "id"));
      for (Map<String, Object> item : items) {
        if (number(item, "quantity") <= 0 || !truth(item.get("dishActive"))
            || !truth(item.get("menuEnabled")) || item.get("currentPrice") == null) {
          throw new IllegalStateException("cart item changed after preflight: " + number(item, "id"));
        }
      }
      itemsByCart.put(number(cart, "id"), items);
    }
    Map<String, Object> target = carts.stream().filter(c -> !itemsByCart.get(number(c, "id")).isEmpty())
        .reduce((older, newer) -> newer).orElse(carts.get(carts.size() - 1));
    long targetId = number(target, "id");
    String newestRemark = null;
    for (Map<String, Object> cart : carts) {
      String cartRemark = text(cart.get("remark"));
      if (!cartRemark.isBlank()) newestRemark = cartRemark;
      long sourceId = number(cart, "id");
      long ownerId = number(cart, "userId");
      for (Map<String, Object> item : itemsByCart.get(sourceId)) {
        long dishId = number(item, "dishId");
        int quantity = (int) number(item, "quantity");
        BigDecimal price = FamilyCartWalletMigrationService.decimal(item.get("currentPrice"));
        String remark = text(item.get("itemRemark"));
        Long targetItemId = mapper.findTargetItem(targetId, dishId);
        if (targetItemId == null) {
          mapper.insertTargetItem(targetId, dishId, text(item.get("dishName")), quantity, price, remark);
          targetItemId = mapper.lastInsertId();
        } else if (sourceId != targetId) {
          mapper.updateTargetItem(targetItemId, quantity, price, remark);
        }
        mapper.upsertSelection(targetItemId, ownerId, quantity, remark);
      }
      mapper.markCartMigrated(batchId, sourceId, targetId, familyId);
      if (sourceId != targetId) mapper.absorbCart(sourceId);
    }
    mapper.updateTargetCart(targetId, newestRemark);
  }

  private static Map<String, Object> required(Map<String, Object> row, String message) {
    if (row == null) throw new IllegalStateException(message);
    return row;
  }
  private static long number(Map<String, Object> row, String key) {
    Object value = row.get(key);
    if (!(value instanceof Number n)) throw new IllegalStateException("missing numeric " + key);
    return n.longValue();
  }
  private static String orderKey(Map<String, Object> row, String key) {
    Object value = row.get(key);
    return value == null ? "" : value.toString();
  }
  private static String text(Object value) { return value == null ? "" : value.toString(); }
  private static boolean truth(Object value) {
    return value instanceof Boolean b ? b : value instanceof Number n && n.intValue() != 0;
  }
}
