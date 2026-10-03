package com.familykitchen.notebook.model;

import java.time.OffsetDateTime;

/** One-time invite secret returned only when an invitation is created.
 * @param id invite ID
 * @param inviteeUserId recipient account
 * @param token one-time plaintext secret
 * @param expiresAt expiration time */
public record NotebookInviteCreated(long id, long inviteeUserId, String token,
    OffsetDateTime expiresAt) {}
