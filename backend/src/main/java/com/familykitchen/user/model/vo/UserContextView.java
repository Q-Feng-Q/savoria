package com.familykitchen.user.model.vo;

import java.util.List;

/**
 * Real-time account identity and permission context returned to clients.
 * @param userId user identifier
 * @param familyId active family identifier
 * @param familyRole role in the active family
 * @param merchantId active merchant identifier
 * @param merchantRole role in the active merchant
 * @param platformRoles platform role codes
 * @param availableModes ordered mini-program workspaces available to the account
 * @param permissionCodes ordered explicit permission codes for client presentation
 */
public record UserContextView(Long userId, Long familyId, String familyRole, Long merchantId,
                              String merchantRole, List<String> platformRoles,
                              List<String> availableModes, List<String> permissionCodes) {
  /**
   * Keeps internal legacy callers source-compatible while the HTTP context uses explicit permissions.
   * @param userId user identifier
   * @param familyId active family identifier
   * @param familyRole role in the active family
   * @param merchantId active merchant identifier
   * @param merchantRole role in the active merchant
   * @param platformRoles platform role codes
   */
  public UserContextView(Long userId, Long familyId, String familyRole, Long merchantId,
                         String merchantRole, List<String> platformRoles) {
    this(userId, familyId, familyRole, merchantId, merchantRole, platformRoles,
        List.of(), List.of());
  }
}
