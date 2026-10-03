package com.familykitchen.notebook.model;

import java.time.OffsetDateTime;

/** Invite metadata without its token or hash.
 * @param id invite ID
 * @param inviterUserId sender
 * @param inviteeUserId recipient
 * @param status current status
 * @param expiresAt expiration time */
public record NotebookInviteView(long id, long inviterUserId, long inviteeUserId,
    String status, OffsetDateTime expiresAt) {}
