package com.familykitchen.notebook;

import static org.assertj.core.api.Assertions.assertThat;

import com.familykitchen.notebook.mapper.NotebookGrantMapper;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/** Shared discovery includes only the event label needed to identify a live grant. */
class NotebookSharedNameTest {
  @Test void recipientSeesEventNameWithGrantButNotEventDescription() {
    var dataSource = new DriverManagerDataSource("jdbc:h2:mem:notebook_shared_name;DB_CLOSE_DELAY=-1", "sa", "");
    var sql = new JdbcTemplate(dataSource);
    sql.execute("CREATE TABLE notebook_events(id BIGINT PRIMARY KEY,name VARCHAR(100),description VARCHAR(100))");
    sql.execute("CREATE TABLE notebook_grants(id BIGINT PRIMARY KEY,event_id BIGINT,owner_user_id BIGINT,"
        + "grantee_user_id BIGINT,data_from DATE,data_to DATE,data_time_zone VARCHAR(100),"
        + "valid_from TIMESTAMP,valid_to TIMESTAMP,can_create BOOLEAN,can_edit BOOLEAN,"
        + "can_export BOOLEAN,status VARCHAR(20))");
    sql.update("INSERT INTO notebook_events VALUES (7,'生理期','隐私描述')");
    sql.update("INSERT INTO notebook_grants VALUES (8,7,1,2,'2026-01-01','2026-01-31',"
        + "'Asia/Shanghai','2026-01-01 00:00:00','2027-01-01 00:00:00',false,false,false,'ACTIVE')");

    var shared = new NotebookGrantMapper(sql).forAccount(2);

    assertThat(shared).hasSize(1);
    assertThat(shared.get(0).eventName()).isEqualTo("生理期");
    assertThat(shared.get(0).toString()).doesNotContain("隐私描述");
  }
}
