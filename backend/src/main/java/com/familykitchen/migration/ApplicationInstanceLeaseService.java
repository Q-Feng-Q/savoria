package com.familykitchen.migration;

import jakarta.annotation.PreDestroy;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/** Publishes compatibility-instance build heartbeats used by migration drain proof. */
@Service
@ConditionalOnProperty(name = "family-kitchen.instance.lease-enabled", havingValue = "true",
    matchIfMissing = true)
public class ApplicationInstanceLeaseService {
  private final FamilyCartWalletMigrationMapper mapper;
  private final String instanceId;
  private final String build;

  /**
   * Creates the instance lease publisher.
   *
   * @param mapper migration mapper
   * @param instanceId unique process identifier
   * @param build compatibility build identifier
   */
  public ApplicationInstanceLeaseService(FamilyCartWalletMigrationMapper mapper,
      @Value("${family-kitchen.instance.id:${random.uuid}}") String instanceId,
      @Value("${family-kitchen.instance.build:unknown}") String build) {
    this.mapper = mapper;
    this.instanceId = instanceId;
    this.build = build;
  }

  /** Publishes the initial lease before the instance begins serving traffic. */
  @PostConstruct
  public void register() {
    heartbeat();
  }

  /** Renews the compatibility-instance lease. */
  @Scheduled(fixedDelayString = "${family-kitchen.instance.heartbeat-ms:10000}")
  public void heartbeat() {
    mapper.heartbeat(instanceId, build, "web", 30);
  }

  /** Removes the lease during orderly shutdown. */
  @PreDestroy
  public void release() {
    mapper.removeLease(instanceId);
  }
}
