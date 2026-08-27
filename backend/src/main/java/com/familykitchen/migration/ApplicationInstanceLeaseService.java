package com.familykitchen.migration;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Publishes compatibility-instance heartbeats only when explicitly enabled. */
@Service
@ConditionalOnProperty(name = "family-kitchen.instance.lease-enabled", havingValue = "true",
    matchIfMissing = false)
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

  /** Serializes renewal with barrier publication; no lease can appear after maintenance is visible. */
  @Scheduled(fixedDelayString = "${family-kitchen.instance.heartbeat-ms:10000}")
  @Transactional
  public void heartbeat() {
    mapper.ensureCutover();
    Map<String, Object> cutover = mapper.lockCutover();
    Object enabled = cutover == null ? null : cutover.get("maintenanceEnabled");
    boolean maintenance = enabled instanceof Boolean b ? b
        : enabled instanceof Number n && n.intValue() != 0;
    if (!maintenance) mapper.heartbeat(instanceId, build, "web", 30);
  }

  /** Releases this compatibility instance''s lease during orderly shutdown. */
  @PreDestroy
  public void release() { mapper.removeLease(instanceId); }
}
