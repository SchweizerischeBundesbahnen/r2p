package ch.rail.r2p.domain;

import ch.rail.r2p.api.dto.BranchState;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;

@AllArgsConstructor
@Builder(toBuilder = true)
@Getter
@Setter
public class Branch {

    public static final String MAIN_BRANCH_NAME = "main";

    @Id
    UUID branchId;
    UUID trainId;
    String name;
    String owner;
    BranchState state;

    public static Branch createBranch(UUID trainId, String name, String owner) {
        return new Branch(UUID.randomUUID(), trainId, name, owner, BranchState.OPEN);
    }

    public boolean isMainBranch() {
        return MAIN_BRANCH_NAME.equals(name);
    }

    public String getPrefixedName() {
        return "%s-%s".formatted(owner, name);
    }
}
