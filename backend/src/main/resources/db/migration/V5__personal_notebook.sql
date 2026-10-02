ALTER TABLE system_settings
  ADD COLUMN notebook_max_query_months INT NOT NULL DEFAULT 36 COMMENT '记事单次查询最长日历月份';

CREATE TABLE notebook_events (
  id BIGINT NOT NULL AUTO_INCREMENT,
  owner_user_id BIGINT NOT NULL,
  name VARCHAR(120) NOT NULL,
  category VARCHAR(80) DEFAULT NULL,
  description TEXT,
  sort_order INT NOT NULL DEFAULT 0,
  starred TINYINT(1) NOT NULL DEFAULT 0,
  archived TINYINT(1) NOT NULL DEFAULT 0,
  current_template_version INT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_notebook_events_owner (owner_user_id, archived, starred, sort_order),
  CONSTRAINT fk_notebook_events_owner FOREIGN KEY (owner_user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE notebook_template_versions (
  id BIGINT NOT NULL AUTO_INCREMENT,
  event_id BIGINT NOT NULL,
  version INT NOT NULL,
  fields_json TEXT NOT NULL,
  published_by_user_id BIGINT NOT NULL,
  published_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uq_notebook_template_event_version (event_id, version),
  CONSTRAINT fk_notebook_template_event FOREIGN KEY (event_id) REFERENCES notebook_events(id) ON DELETE CASCADE,
  CONSTRAINT fk_notebook_template_publisher FOREIGN KEY (published_by_user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE notebook_records (
  id BIGINT NOT NULL AUTO_INCREMENT,
  event_id BIGINT NOT NULL,
  owner_user_id BIGINT NOT NULL,
  created_by_user_id BIGINT NOT NULL,
  updated_by_user_id BIGINT NOT NULL,
  occurred_from DATETIME NOT NULL,
  occurred_to DATETIME NOT NULL,
  title VARCHAR(200) NOT NULL,
  note TEXT,
  template_version INT NOT NULL,
  values_json TEXT NOT NULL,
  lock_version INT NOT NULL DEFAULT 0,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_notebook_records_event_range (event_id, occurred_from, occurred_to),
  KEY idx_notebook_records_owner_range (owner_user_id, occurred_from, occurred_to),
  CONSTRAINT fk_notebook_record_event FOREIGN KEY (event_id) REFERENCES notebook_events(id) ON DELETE CASCADE,
  CONSTRAINT fk_notebook_record_owner FOREIGN KEY (owner_user_id) REFERENCES users(id),
  CONSTRAINT fk_notebook_record_creator FOREIGN KEY (created_by_user_id) REFERENCES users(id),
  CONSTRAINT fk_notebook_record_editor FOREIGN KEY (updated_by_user_id) REFERENCES users(id),
  CONSTRAINT fk_notebook_record_template FOREIGN KEY (event_id, template_version)
    REFERENCES notebook_template_versions(event_id, version) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE notebook_record_revisions (
  id BIGINT NOT NULL AUTO_INCREMENT,
  record_id BIGINT NOT NULL,
  revision INT NOT NULL,
  template_version INT NOT NULL,
  title VARCHAR(200) NOT NULL,
  note TEXT,
  occurred_from DATETIME NOT NULL,
  occurred_to DATETIME NOT NULL,
  values_json TEXT NOT NULL,
  edited_by_user_id BIGINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uq_notebook_revision (record_id, revision),
  CONSTRAINT fk_notebook_revision_record FOREIGN KEY (record_id) REFERENCES notebook_records(id) ON DELETE CASCADE,
  CONSTRAINT fk_notebook_revision_editor FOREIGN KEY (edited_by_user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE notebook_contacts (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  contact_user_id BIGINT NOT NULL,
  source VARCHAR(30) NOT NULL,
  confirmed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uq_notebook_contact_pair (user_id, contact_user_id),
  KEY idx_notebook_contact_reverse (contact_user_id, user_id),
  CONSTRAINT fk_notebook_contact_user FOREIGN KEY (user_id) REFERENCES users(id),
  CONSTRAINT fk_notebook_contact_other FOREIGN KEY (contact_user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE notebook_contact_invites (
  id BIGINT NOT NULL AUTO_INCREMENT,
  inviter_user_id BIGINT NOT NULL,
  invitee_user_id BIGINT DEFAULT NULL,
  code_hash VARCHAR(128) DEFAULT NULL,
  status VARCHAR(20) NOT NULL,
  expires_at DATETIME NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  responded_at DATETIME DEFAULT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_notebook_invite_code (code_hash),
  KEY idx_notebook_invite_recipient (invitee_user_id, status),
  CONSTRAINT fk_notebook_invite_sender FOREIGN KEY (inviter_user_id) REFERENCES users(id),
  CONSTRAINT fk_notebook_invite_recipient FOREIGN KEY (invitee_user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE notebook_grants (
  id BIGINT NOT NULL AUTO_INCREMENT,
  event_id BIGINT NOT NULL,
  owner_user_id BIGINT NOT NULL,
  grantee_user_id BIGINT NOT NULL,
  data_from DATE NOT NULL,
  data_to DATE NOT NULL,
  valid_from DATETIME NOT NULL,
  valid_to DATETIME NOT NULL,
  can_create TINYINT(1) NOT NULL DEFAULT 0,
  can_edit TINYINT(1) NOT NULL DEFAULT 0,
  can_export TINYINT(1) NOT NULL DEFAULT 0,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uq_notebook_grant_event_grantee (event_id, grantee_user_id),
  KEY idx_notebook_grants_grantee (grantee_user_id, status, valid_to),
  CONSTRAINT fk_notebook_grant_event FOREIGN KEY (event_id) REFERENCES notebook_events(id) ON DELETE CASCADE,
  CONSTRAINT fk_notebook_grant_owner FOREIGN KEY (owner_user_id) REFERENCES users(id),
  CONSTRAINT fk_notebook_grant_grantee FOREIGN KEY (grantee_user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE notebook_audit (
  id BIGINT NOT NULL AUTO_INCREMENT,
  actor_user_id BIGINT DEFAULT NULL,
  owner_user_id BIGINT DEFAULT NULL,
  event_id BIGINT DEFAULT NULL,
  record_id BIGINT DEFAULT NULL,
  action VARCHAR(40) NOT NULL,
  outcome VARCHAR(30) NOT NULL,
  detail VARCHAR(500) DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_notebook_audit_owner_time (owner_user_id, created_at),
  KEY idx_notebook_audit_event_time (event_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE notebook_images (
  id BIGINT NOT NULL AUTO_INCREMENT,
  record_id BIGINT NOT NULL,
  owner_user_id BIGINT NOT NULL,
  storage_key VARCHAR(500) NOT NULL,
  original_name VARCHAR(255) NOT NULL,
  content_type VARCHAR(100) NOT NULL,
  byte_size BIGINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_notebook_images_record (record_id),
  CONSTRAINT fk_notebook_image_record FOREIGN KEY (record_id) REFERENCES notebook_records(id) ON DELETE CASCADE,
  CONSTRAINT fk_notebook_image_owner FOREIGN KEY (owner_user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
