package ch.rail.r2p.web;

import static org.assertj.core.api.Assertions.assertThat;

import ch.rail.r2p.NoAuthSpringBootTest;
import ch.rail.r2p.api.dto.InstanceOwner;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;

@NoAuthSpringBootTest
class InstanceOwnerTests {

    @Autowired
    private InstanceOwnerController instanceOwnerController;

    @Value("${r2p.instance-owner}")
    private String configuredInstanceOwner;

    @Test
    void should_return_configured_instance_owner() {
        ResponseEntity<InstanceOwner> response = instanceOwnerController.getInstanceOwner();

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getName()).isEqualTo(configuredInstanceOwner);
    }
}
