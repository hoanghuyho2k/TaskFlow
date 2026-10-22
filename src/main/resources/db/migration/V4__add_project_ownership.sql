ALTER TABLE projects ADD COLUMN owner_id BIGINT;

ALTER TABLE projects
    ADD CONSTRAINT projects_owner_fk
    FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE;

-- Keep legacy rows in place but reject any new unowned project rows.
ALTER TABLE projects
    ADD CONSTRAINT projects_owner_required CHECK (owner_id IS NOT NULL) NOT VALID;

CREATE INDEX projects_owner_id_id_idx ON projects (owner_id, id);
