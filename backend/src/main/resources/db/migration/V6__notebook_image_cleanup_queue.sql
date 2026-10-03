CREATE TABLE notebook_image_cleanup (
  id BIGINT NOT NULL AUTO_INCREMENT,
  event_id BIGINT NOT NULL,
  owner_user_id BIGINT NOT NULL,
  storage_key VARCHAR(500) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_notebook_image_cleanup_event (event_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
