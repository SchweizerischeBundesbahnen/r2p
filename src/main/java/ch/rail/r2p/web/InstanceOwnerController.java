package ch.rail.r2p.web;

import ch.rail.r2p.api.ConfigApi;
import ch.rail.r2p.api.dto.InstanceOwner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class InstanceOwnerController implements ConfigApi {

    @Value("${r2p.instance-owner}")
    private String instanceOwner;

    @Override
    public ResponseEntity<InstanceOwner> getInstanceOwner() {
        return ResponseEntity.ok(new InstanceOwner(instanceOwner));
    }
}
