CREATE TABLE notebook_staged_images (
  id BIGINT NOT NULL AUTO_INCREMENT,
  event_id BIGINT NOT NULL,
  owner_user_id BIGINT NOT NULL,
  uploaded_by_user_id BIGINT NOT NULL,
  storage_key VARCHAR(500) NOT NULL,
  original_name VARCHAR(255) NOT NULL,
  content_type VARCHAR(100) NOT NULL,
  byte_size BIGINT NOT NULL,
  expires_at DATETIME NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uq_notebook_staged_key (storage_key),
  KEY idx_notebook_staged_expiry (expires_at, id),
  KEY idx_notebook_staged_event (event_id, id),
  CONSTRAINT fk_notebook_staged_event FOREIGN KEY (event_id) REFERENCES notebook_events(id) ON DELETE CASCADE,
  CONSTRAINT fk_notebook_staged_owner FOREIGN KEY (owner_user_id) REFERENCES users(id),
  CONSTRAINT fk_notebook_staged_uploader FOREIGN KEY (uploaded_by_user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
