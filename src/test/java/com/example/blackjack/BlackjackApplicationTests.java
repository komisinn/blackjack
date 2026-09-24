package com.example.blackjack;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.springframework.boot.test.web.server.LocalServerPort;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"local", "test"})
class BlackjackApplicationTests {
	@Autowired JdbcTemplate jdbc;
	@LocalServerPort int port;

	@Test
	void contextLoads() {
		assertEquals(1, jdbc.queryForObject("SELECT 1", Integer.class));
	}

	@Test
	void localHealthEndpointChecksDatabase() throws Exception {
		try (var client = HttpClient.newHttpClient()) {
			var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/dev/health")).GET().build();
			var response = client.send(request, HttpResponse.BodyHandlers.ofString());
			assertEquals(200, response.statusCode());
			assertTrue(response.body().contains("\"status\":\"UP\""));
			assertTrue(response.body().contains("\"database\":\"CONNECTED\""));
		}
	}

}
