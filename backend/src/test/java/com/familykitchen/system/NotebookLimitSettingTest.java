package com.familykitchen.system;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.familykitchen.notebook.NotebookRangePolicy;
import com.familykitchen.system.mapper.SystemAuditMapper;
import com.familykitchen.system.mapper.SystemSettingMapper;
import com.familykitchen.system.model.dto.SystemSettingRequest;
import com.familykitchen.system.model.entity.SystemSettingDO;
import com.familykitchen.system.security.PlatformSecretCipher;
import com.familykitchen.system.service.impl.SystemSettingServiceImpl;
import jakarta.validation.Validation;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Notebook query limits and administrator setting boundaries. */
class NotebookLimitSettingTest {
  @Test void countsTouchedCalendarMonths() {
    var policy = new NotebookRangePolicy(() -> 36);
    policy.requireAllowed(LocalDate.of(2024, 1, 1), LocalDate.of(2026, 12, 31));
    assertThatThrownBy(() -> policy.requireAllowed(
        LocalDate.of(2024, 1, 1), LocalDate.of(2027, 1, 1))).hasMessageContaining("36");
    assertThatThrownBy(() -> policy.requireAllowed(
        LocalDate.of(2026, 2, 1), LocalDate.of(2026, 1, 31))).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new NotebookRangePolicy(() -> 1).requireAllowed(
        LocalDate.of(2026, 1, 31), LocalDate.of(2026, 2, 1))).hasMessageContaining("1");
  }

  @Test void fallsBackTo36ForNullMissingOrFailedSettingRead() {
    var mapper = mock(SystemSettingMapper.class);
    when(mapper.selectCurrent()).thenReturn(null);
    assertThat(service(mapper).notebookMaxQueryMonths()).isEqualTo(36);
    var missing = new SystemSettingDO();
    when(mapper.selectCurrent()).thenReturn(missing);
    assertThat(service(mapper).notebookMaxQueryMonths()).isEqualTo(36);
    when(mapper.selectCurrent()).thenThrow(new IllegalStateException("database unavailable"));
    assertThat(service(mapper).notebookMaxQueryMonths()).isEqualTo(36);
  }

  @Test void failedReadDoesNotReuseStaleCachedLimit() {
    var mapper = mock(SystemSettingMapper.class);
    var row = new SystemSettingDO();
    row.setNotebookMaxQueryMonths(2);
    when(mapper.selectCurrent()).thenReturn(row).thenThrow(new IllegalStateException("offline"));
    var service = service(mapper);
    assertThat(service.current().notebookMaxQueryMonths()).isEqualTo(2);
    assertThat(service.notebookMaxQueryMonths()).isEqualTo(36);
  }

  @Test void adminAcceptsBoundaryValuesAndPreservesLegacyOmissions() throws Exception {
    var mapper = mock(SystemSettingMapper.class);
    var current = new SystemSettingDO();
    current.setNotebookMaxQueryMonths(12);
    when(mapper.selectCurrent()).thenReturn(current);
    var service = service(mapper);
    for (int limit : new int[] {1, 36}) {
      service.update(7L, request(limit));
      var row = ArgumentCaptor.forClass(SystemSettingDO.class);
      verify(mapper, org.mockito.Mockito.atLeastOnce()).update(row.capture());
      assertThat(row.getValue().getNotebookMaxQueryMonths()).isEqualTo(limit);
    }
    service.update(7L, request(null));
    var row = ArgumentCaptor.forClass(SystemSettingDO.class);
    verify(mapper, org.mockito.Mockito.atLeastOnce()).update(row.capture());
    assertThat(row.getValue().getNotebookMaxQueryMonths()).isNull();
  }

  @Test void legacyOmissionDoesNotOverwriteStoredLimitWhenSettingsReadFails() throws Exception {
    var mapper = mock(SystemSettingMapper.class);
    var stored = new SystemSettingDO();
    stored.setNotebookMaxQueryMonths(12);
    when(mapper.selectCurrent()).thenReturn(stored)
        .thenThrow(new IllegalStateException("temporary settings read failure"));
    var service = service(mapper);
    service.current();

    assertThatThrownBy(() -> service.update(7L, request(null)))
        .isInstanceOf(IllegalStateException.class);

    var row = ArgumentCaptor.forClass(SystemSettingDO.class);
    verify(mapper).update(row.capture());
    assertThat(row.getValue().getNotebookMaxQueryMonths()).isNull();
  }

  @Test void adminRejectsOutOfRangeValues() throws Exception {
    try (var factory = Validation.buildDefaultValidatorFactory()) {
      for (int limit : new int[] {0, 37}) {
        assertThat(factory.getValidator().validate(request(limit)))
            .extracting(violation -> violation.getPropertyPath().toString())
            .contains("notebookMaxQueryMonths");
      }
    }
  }

  private static SystemSettingRequest request(Integer months) throws Exception {
    return new ObjectMapper().readValue("""
        {"siteName":"Kitchen","maintenanceMessage":"Maintenance","notebookMaxQueryMonths":%s}
        """.formatted(months == null ? "null" : months), SystemSettingRequest.class);
  }

  private static SystemSettingServiceImpl service(SystemSettingMapper mapper) {
    return new SystemSettingServiceImpl(mapper, mock(SystemAuditMapper.class),
        new PlatformSecretCipher("test-secret"));
  }
}
