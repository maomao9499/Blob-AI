CREATE TABLE knowledge_category (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(50) NOT NULL,
  description VARCHAR(500) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_knowledge_category_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE knowledge_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(200) NOT NULL,
  content_md MEDIUMTEXT NOT NULL,
  summary VARCHAR(1000) NULL,
  category_id BIGINT NULL,
  source_journal_id BIGINT NULL,
  source_journal_title VARCHAR(200) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  INDEX idx_knowledge_updated (updated_at DESC, id DESC),
  INDEX idx_knowledge_category_updated (category_id, updated_at DESC, id DESC),
  INDEX idx_knowledge_source_journal (source_journal_id),
  CONSTRAINT fk_knowledge_category FOREIGN KEY (category_id) REFERENCES knowledge_category(id) ON DELETE RESTRICT,
  CONSTRAINT fk_knowledge_source_journal FOREIGN KEY (source_journal_id) REFERENCES journal_entry(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE knowledge_item_tag (
  knowledge_item_id BIGINT NOT NULL,
  tag_id BIGINT NOT NULL,
  PRIMARY KEY (knowledge_item_id, tag_id),
  INDEX idx_knowledge_tag_reverse (tag_id, knowledge_item_id),
  CONSTRAINT fk_kit_knowledge FOREIGN KEY (knowledge_item_id) REFERENCES knowledge_item(id) ON DELETE CASCADE,
  CONSTRAINT fk_kit_tag FOREIGN KEY (tag_id) REFERENCES tag(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE knowledge_relation (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  source_knowledge_id BIGINT NOT NULL,
  target_knowledge_id BIGINT NOT NULL,
  relation_type VARCHAR(20) NOT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_knowledge_relation (source_knowledge_id, target_knowledge_id, relation_type),
  INDEX idx_knowledge_relation_target (target_knowledge_id, id),
  CONSTRAINT ck_knowledge_relation_order CHECK (source_knowledge_id < target_knowledge_id),
  CONSTRAINT ck_knowledge_relation_type CHECK (relation_type = 'RELATED'),
  CONSTRAINT fk_kr_source FOREIGN KEY (source_knowledge_id) REFERENCES knowledge_item(id) ON DELETE CASCADE,
  CONSTRAINT fk_kr_target FOREIGN KEY (target_knowledge_id) REFERENCES knowledge_item(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
