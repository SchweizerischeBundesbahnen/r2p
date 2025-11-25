package ch.rail.r2p;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import org.springframework.security.test.context.support.WithMockUser;

@Retention(RetentionPolicy.RUNTIME)
@WithMockUser(authorities = {"SCOPE_r2p", "ROLE_r2p_user"})
public @interface WithMockR2PUser {
}
