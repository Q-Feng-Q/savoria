package com.familykitchen.migration;

import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Persistent cross-process ownership for every migration runner invocation. */
@Service
public class FamilyWalletMigrationLeaseService {
  private static final long LEASE_SECONDS = 60;
  private final FamilyCartWalletMigrationMapper mapper;

  /**
   * Creates the persistent runner lease service.
   *
   * @param mapper migration persistence mapper
   */
  public FamilyWalletMigrationLeaseService(FamilyCartWalletMigrationMapper mapper) {
    this.mapper = mapper;
  }

  /**
   * Acquires the singleton lease without stealing an unexpired owner.
   *
   * @param batchId migration batch identifier, or zero before a batch exists
   * @param epoch drain epoch, or zero before drain
   * @param mode requested migration mode
   * @return opaque lease owner token
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public String acquire(long batchId, long epoch, String mode) {
    String token = UUID.randomUUID().toString();
    mapper.acquireRunnerLease(token, batchId, epoch, mode, LEASE_SECONDS);
    requireOwned(token, batchId, epoch);
    return token;
  }

  /**
   * Atomically locks and validates owner, batch, epoch, and expiry in the current transaction.
   *
   * @param token runner owner token
   * @param batchId migration batch identifier
   * @param epoch drain epoch
   */
  public void requireOwned(String token, long batchId, long epoch) {
    Map<String, Object> lease = mapper.lockAndVerifyRunnerLease(token, batchId, epoch);
    if (lease == null) throw new IllegalStateException("stale or concurrent migration runner lease");
  }

  /**
   * Renews only the exact still-live owner.
   *
   * @param token runner owner token
   * @param batchId migration batch identifier
   * @param epoch drain epoch
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void renew(String token, long batchId, long epoch) {
    if (mapper.renewRunnerLease(token, batchId, epoch, LEASE_SECONDS) != 1) {
      throw new IllegalStateException("migration runner lease renewal rejected");
    }
  }

  /**
   * Extends ownership before an implicit-commit DDL statement.
   *
   * @param token runner owner token
   * @param batchId migration batch identifier
   * @param epoch drain epoch
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void renewForDdl(String token, long batchId, long epoch) {
    if (mapper.renewRunnerLease(token, batchId, epoch, 3600) != 1) {
      throw new IllegalStateException("migration runner DDL lease renewal rejected");
    }
  }
  /**
   * Releases only the exact owner token.
   *
   * @param token runner owner token
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void release(String token) {
    if (token != null) mapper.releaseRunnerLease(token);
  }
}
