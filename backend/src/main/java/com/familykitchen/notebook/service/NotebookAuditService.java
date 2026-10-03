package com.familykitchen.notebook.service;

import com.familykitchen.common.error.BusinessException;
import com.familykitchen.common.error.ErrorCode;
import com.familykitchen.notebook.NotebookRangePolicy;
import com.familykitchen.notebook.mapper.NotebookAuditMapper;
import com.familykitchen.notebook.mapper.NotebookAuditMapper.Entry;
import com.familykitchen.system.service.SystemSettingService;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** Owner-only, bounded, metadata-only notebook audit operations. */
@Service
public class NotebookAuditService {
  /** Stable audit page.
   * @param items action entries
   * @param page zero-based page
   * @param size page size
   * @param hasMore whether a next page exists */
  public record Page(List<Entry> items, int page, int size, boolean hasMore) {}

  private final NotebookAuditMapper mapper;
  private final NotebookAccessPolicy access;
  private final NotebookRangePolicy ranges;

  /** Creates audit operations with the live month limit.
   *
   * @param mapper audit persistence
   * @param access account policy
   * @param settings system settings */
  @Autowired
  public NotebookAuditService(NotebookAuditMapper mapper, NotebookAccessPolicy access,
      SystemSettingService settings) {
    this(mapper, access, new NotebookRangePolicy(settings::notebookMaxQueryMonths));
  }

  /** Creates audit operations with an explicit range policy.
   *
   * @param mapper audit persistence
   * @param access account policy
   * @param ranges range policy */
  public NotebookAuditService(NotebookAuditMapper mapper, NotebookAccessPolicy access,
      NotebookRangePolicy ranges) {
    this.mapper = mapper; this.access = access; this.ranges = ranges;
  }

  /** Writes one metadata-only action after its authorization boundary.
   *
   * @param actor actor ID
   * @param owner owner ID
   * @param event event ID
   *
   * @param record optional record ID
   * @param action fixed action code */
  public void record(long actor, long owner, long event, Long record, String action) {
    mapper.write(actor, owner, event, record, action);
  }

  /** Lists one owner's bounded audit page.
   *
   * @param actor owner ID
   * @param event event ID
   * @param from first local date
   *
   * @param to last local date
   * @param timeZone IANA time zone
   * @param page zero-based page
   *
   * @param size page size
   * @return metadata page */
  public Page list(long actor, long event, LocalDate from, LocalDate to, String timeZone,
      int page, int size) {
    ranges.requireAllowed(from, to);
    if (timeZone == null || !ZoneId.getAvailableZoneIds().contains(timeZone)
        || to.equals(LocalDate.MAX) || page < 0 || size < 1 || size > 100
        || (long) page * size > Integer.MAX_VALUE) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "Invalid notebook audit query");
    }
    access.requireOwner(actor, event);
    ZoneId zone = ZoneId.of(timeZone);
    var rows = mapper.page(actor, event, from.atStartOfDay(zone).toInstant(),
        to.plusDays(1).atStartOfDay(zone).toInstant(), size + 1, (long) page * size);
    return new Page(rows.stream().limit(size).toList(), page, size, rows.size() > size);
  }
}
