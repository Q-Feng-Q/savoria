package com.familykitchen.notebook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.familykitchen.common.error.BusinessException;
import com.familykitchen.notebook.mapper.NotebookContactMapper;
import com.familykitchen.notebook.mapper.NotebookGrantMapper;
import com.familykitchen.notebook.service.NotebookContactService;
import com.familykitchen.notebook.service.NotebookIdentityLookup;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

/** Invitations create account contacts only after recipient confirmation. */
class NotebookContactServiceTest {
  private NotebookContactService contacts;
  private JdbcTemplate sql;

  @BeforeEach void database() throws Exception {
    var source = new JdbcDataSource();
    source.setURL("jdbc:h2:mem:contacts_" + java.util.UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
    sql = new JdbcTemplate(source);
    sql.execute("CREATE TABLE users (id BIGINT PRIMARY KEY)");
    sql.execute("CREATE TABLE system_settings (id BIGINT PRIMARY KEY)");
    for (String migration : List.of("V5__personal_notebook.sql", "V6__notebook_image_cleanup_queue.sql",
        "V7__notebook_grant_data_zone.sql")) {
      try (var stream = getClass().getResourceAsStream("/db/migration/" + migration)) {
        for (String command : new String(stream.readAllBytes(), StandardCharsets.UTF_8).split(";")) {
          if (!command.isBlank()) sql.execute(command);
        }
      }
    }
    sql.update("INSERT INTO users(id) VALUES (1),(2),(3)");
    NotebookIdentityLookup lookup = identifier -> switch (identifier) {
      case "alice" -> 1L; case "bob" -> 2L; case "carol" -> 3L; default -> null;
    };
    contacts = new NotebookContactService(new NotebookContactMapper(sql),
        new NotebookGrantMapper(sql), lookup);
  }

  @Test void rejectsSelfInviteAndRequiresTargetTokenConfirmation() {
    assertThatThrownBy(() -> contacts.invite(1, "alice")).isInstanceOf(BusinessException.class);
    var invite = contacts.invite(1, "bob");
    assertThat(contacts.list(1)).isEmpty();
    assertThatThrownBy(() -> contacts.accept(3, invite.id(), invite.token()))
        .isInstanceOf(BusinessException.class);
    assertThatThrownBy(() -> contacts.accept(2, invite.id(), "wrong-token"))
        .isInstanceOf(BusinessException.class);
    contacts.accept(2, invite.id(), invite.token());
    assertThat(contacts.list(1)).extracting(item -> item.contactUserId()).containsExactly(2L);
    assertThat(contacts.list(2)).extracting(item -> item.contactUserId()).containsExactly(1L);
    assertThatThrownBy(() -> contacts.accept(2, invite.id(), invite.token()))
        .isInstanceOf(BusinessException.class);
    assertThat(sql.queryForObject("SELECT code_hash FROM notebook_contact_invites WHERE id=?",
        String.class, invite.id())).isNotEqualTo(invite.token());
  }

  @Test void expiresInviteAndRejectsReplayAfterRejection() {
    var expired = contacts.invite(1, "bob");
    sql.update("UPDATE notebook_contact_invites SET expires_at='2000-01-01' WHERE id=?", expired.id());
    assertThatThrownBy(() -> contacts.accept(2, expired.id(), expired.token()))
        .isInstanceOf(BusinessException.class);
    var rejected = contacts.invite(1, "carol");
    contacts.reject(3, rejected.id(), rejected.token());
    assertThatThrownBy(() -> contacts.accept(3, rejected.id(), rejected.token()))
        .isInstanceOf(BusinessException.class);
  }

  @Test void externalAccountContactHasNoFamilyLinkAndRemovalRevokesGrants() {
    var invite = contacts.invite(1, "bob");
    contacts.accept(2, invite.id(), invite.token());
    assertThat(sql.queryForObject("SELECT source FROM notebook_contacts WHERE user_id=1 AND contact_user_id=2",
        String.class)).isEqualTo("LOGIN");
    sql.update("INSERT INTO notebook_events(owner_user_id,name) VALUES (1,'Journal')");
    sql.update("INSERT INTO notebook_grants(event_id,owner_user_id,grantee_user_id,data_from,data_to,"
        + "data_time_zone,valid_from,valid_to) VALUES (1,1,2,'2026-01-01','2026-01-31',"
        + "'Asia/Shanghai','2026-01-01','2027-01-01')");
    contacts.remove(1, 2);
    assertThat(contacts.list(1)).isEmpty();
    assertThat(sql.queryForObject("SELECT status FROM notebook_grants WHERE event_id=1", String.class))
        .isEqualTo("REVOKED");
  }

  @Test void removalInvalidatesBothDirectionsOfOldPendingInvitations() {
    var aliceInvitesBob = contacts.invite(1, "bob");
    var bobInvitesAlice = contacts.invite(2, "alice");
    contacts.accept(2, aliceInvitesBob.id(), aliceInvitesBob.token());
    contacts.remove(1, 2);
    assertThatThrownBy(() -> contacts.accept(1, bobInvitesAlice.id(), bobInvitesAlice.token()))
        .isInstanceOf(BusinessException.class);
    assertThat(contacts.list(1)).isEmpty();
    assertThat(contacts.list(2)).isEmpty();
  }
}
