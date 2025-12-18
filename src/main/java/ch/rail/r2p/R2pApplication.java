package ch.rail.r2p;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;

@SpringBootApplication
@EnableJdbcRepositories
public class R2pApplication {
	public static void main(String[] args) {
		SpringApplication.run(R2pApplication.class, args);
	}
}
