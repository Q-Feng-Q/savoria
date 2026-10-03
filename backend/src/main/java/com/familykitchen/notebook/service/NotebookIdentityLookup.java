package com.familykitchen.notebook.service;

/** Narrow account lookup boundary for contact invitations. */
@FunctionalInterface
public interface NotebookIdentityLookup {
  /** Resolves a normalized login identifier without consulting family membership.
   * @param identifier username, mobile or email
   * @return account ID or null */
  Long findUserId(String identifier);
}
