package ch.rail.com.web;

import ch.rail.com.api.HelloworldApi;
import ch.rail.com.dto.HelloWorldResponse;
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
