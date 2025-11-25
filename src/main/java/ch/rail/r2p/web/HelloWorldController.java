package ch.rail.r2p.web;

import ch.rail.r2p.api.HelloworldApi;
import ch.rail.r2p.api.dto.HelloWorldResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloWorldController implements HelloworldApi {

	@Override
	public ResponseEntity<HelloWorldResponse> getHelloWorld() {
		HelloWorldResponse response = new HelloWorldResponse("Hello World");
		return ResponseEntity.ok(response);
	}
}
