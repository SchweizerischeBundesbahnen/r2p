-- This prevents deletion of branches that are still referenced by train versions
ALTER TABLE train_version
    ADD CONSTRAINT fk_train_version_branch
    FOREIGN KEY (branch_id) REFERENCES branch(branch_id);

-- This prevents deletion of branches that are still referenced in the changelog
ALTER TABLE changelog
    ADD CONSTRAINT fk_changelog_branch
    FOREIGN KEY (branch_id) REFERENCES branch(branch_id);
