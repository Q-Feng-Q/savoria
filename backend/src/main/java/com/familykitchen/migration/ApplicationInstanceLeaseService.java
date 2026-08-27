package com.familykitchen.migration;

import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.core.Ordered;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Publishes compatibility-instance heartbeats only when explicitly enabled. */
@Service
@ConditionalOnExpression("${family-kitchen.instance.lease-enabled:false}"
    + " && '${family-kitchen.migration.mode:OFF}'.equalsIgnoreCase('OFF')")
public class ApplicationInstanceLeaseService implements ApplicationRunner, Ordered {
  private final FamilyCartWalletMigrationMapper mapper;
  private final String instanceId;
  private final String build;

  /**
   * Creates a compatibility-instance lease publisher.
   *
   * @param mapper migration persistence mapper
   * @param instanceId unique application instance identifier
   * @param build deployed build identifier
   */
  public ApplicationInstanceLeaseService(FamilyCartWalletMigrationMapper mapper,
      @Value("${family-kitchen.instance.id:${random.uuid}}") String instanceId,
      @Value("${family-kitchen.instance.build:unknown}") String build) {
    this.mapper = mapper;
    this.instanceId = instanceId;
    this.build = build;
  }

  /** Registers this compatibility instance atomically before ordinary startup runners execute. */
  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    publishHeartbeat();
  }

  /**
   * Serializes renewal with barrier publication and keeps reporting a live compatibility instance.
   * A maintenance barrier blocks business writes; it must not make a still-running process look
   * drained. Only orderly shutdown or lease expiry after actual process death removes the lease.
   */
  @Scheduled(fixedDelayString = "${family-kitchen.instance.heartbeat-ms:10000}")
  @Transactional
  public void heartbeat() {
    publishHeartbeat();
  }

  private void publishHeartbeat() {
    mapper.ensureCutover();
    mapper.lockCutover();
    mapper.heartbeat(instanceId, build, "web", 30);
  }

  /** Runs after the one-shot migration runner but before default-priority business initializers. */
  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE + 100;
  }

  /** Releases this compatibility instance''s lease during orderly shutdown. */
  @PreDestroy
  public void release() { mapper.removeLease(instanceId); }
}
