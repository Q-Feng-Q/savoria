package com.familykitchen.notebook.service;

import com.familykitchen.user.mapper.UserMapper;
import org.springframework.stereotype.Component;

/** Production identity adapter for notebook contact lookup. */
@Component
public class NotebookUserLookup implements NotebookIdentityLookup {
  private final UserMapper users;

  /** Creates the account lookup adapter.
   * @param users existing account mapper */
  public NotebookUserLookup(UserMapper users) { this.users = users; }

  /** Finds an account by its existing login identifier.
   * @param identifier normalized username or verified email
   * @return account ID or null */
  @Override public Long findUserId(String identifier) {
    var user = users.findByLoginIdentifier(identifier);
    return user == null ? null : user.getId();
  }
}
