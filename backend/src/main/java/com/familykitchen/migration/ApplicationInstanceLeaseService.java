package com.familykitchen.migration;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Publishes compatibility-instance heartbeats only when explicitly enabled. */
@Service
@ConditionalOnExpression("${family-kitchen.instance.lease-enabled:false}"
    + " && '${family-kitchen.migration.mode:OFF}'.equalsIgnoreCase('OFF')")
public class ApplicationInstanceLeaseService {
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

  /** Registers this compatibility instance when the opt-in bean starts. */
  @PostConstruct
  public void register() { heartbeat(); }

  /**
   * Serializes renewal with barrier publication and keeps reporting a live compatibility instance.
   * A maintenance barrier blocks business writes; it must not make a still-running process look
   * drained. Only orderly shutdown or lease expiry after actual process death removes the lease.
   */
  @Scheduled(fixedDelayString = "${family-kitchen.instance.heartbeat-ms:10000}")
  @Transactional
  public void heartbeat() {
    mapper.ensureCutover();
    Map<String, Object> cutover = mapper.lockCutover();
    mapper.heartbeat(instanceId, build, "web", 30);
  }

  /** Releases this compatibility instance''s lease during orderly shutdown. */
  @PreDestroy
  public void release() { mapper.removeLease(instanceId); }
}
