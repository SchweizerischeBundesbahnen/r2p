-- Drop existing foreign key constraints that were created without CASCADE DELETE
ALTER TABLE train_version
    DROP CONSTRAINT fk_train_version_branch;

ALTER TABLE changelog
    DROP CONSTRAINT fk_changelog_branch;

-- Recreate foreign key constraints with CASCADE DELETE to automatically clean up related data
-- When a branch is deleted, all train versions and changelog entries for that branch are also deleted
ALTER TABLE train_version
    ADD CONSTRAINT fk_train_version_branch
    FOREIGN KEY (branch_id) REFERENCES branch(branch_id) ON DELETE CASCADE;

ALTER TABLE changelog
    ADD CONSTRAINT fk_changelog_branch
    FOREIGN KEY (branch_id) REFERENCES branch(branch_id) ON DELETE CASCADE;

