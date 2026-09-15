/* ==================================================================
 * HttpSessionTests.java - 16/09/2026 9:12:40 am
 *
 * Copyright 2026 SolarNetwork.net Dev Team
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 * 02111-1307 USA
 * ==================================================================
 */

package net.solarnetwork.central.reg.config.test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.BDDAssertions.then;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcOperations;

/**
 * Test cases for HTTP sessions, using the embedded servlet container.
 *
 * @author matt
 * @version 1.0
 */
// use a separate persistent cache directory, as other test contexts lock the default one
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT,
		properties = "app.cache.persistence.path=build/tmp/HttpSessionTests/cache")
public class HttpSessionTests {

	private static final String SESSION_COOKIE_NAME = "SESSION";

	@Value("${local.server.port}")
	private int port;

	@Value("${server.servlet.context-path:}")
	private String contextPath;

	@Value("${server.servlet.session.timeout}")
	private Duration sessionTimeout;

	@Autowired
	private JdbcOperations jdbcOperations;

	private final List<String> sessionIds = new ArrayList<>();

	@AfterEach
	public void teardown() {
		for ( String sessionId : sessionIds ) {
			jdbcOperations.update("DELETE FROM solaruser.http_session WHERE session_id = ?", sessionId);
		}
	}

	private record SessionCookie(String header, String sessionId) {

	}

	private SessionCookie loginPageSessionCookie() throws Exception {
		final HttpClient client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
		final HttpRequest req = HttpRequest
				.newBuilder(URI.create("http://localhost:" + port + contextPath + "/login")).build();

		// the login page stores its CSRF token in a new session
		final HttpResponse<Void> res = client.send(req, BodyHandlers.discarding());
		then(res.statusCode()).as("Login page returned").isEqualTo(200);

		final List<String> cookies = res.headers().allValues(HttpHeaders.SET_COOKIE).stream()
				.filter(c -> c.startsWith(SESSION_COOKIE_NAME + "=")).toList();
		then(cookies).as("Session cookie set").hasSize(1);

		final String header = cookies.getFirst();
		final int valueStart = SESSION_COOKIE_NAME.length() + 1;
		final int valueEnd = header.indexOf(';', valueStart);
		final String value = header.substring(valueStart, valueEnd < 0 ? header.length() : valueEnd);
		final String sessionId = new String(Base64.getDecoder().decode(value), UTF_8);
		sessionIds.add(sessionId);
		return new SessionCookie(header, sessionId);
	}

	@Test
	public void sessionCookie() throws Exception {
		// WHEN
		final SessionCookie cookie = loginPageSessionCookie();

		// THEN
		then(cookie.header())
				.as("Session cookie restricted to same-site requests and hidden from scripts")
				.contains("SameSite=Lax").contains("HttpOnly");
	}

	@Test
	public void sessionPersisted() throws Exception {
		// WHEN
		final SessionCookie cookie = loginPageSessionCookie();

		// THEN
		final List<Map<String, Object>> sessions = jdbcOperations.queryForList(
				"SELECT primary_id, max_inactive_interval FROM solaruser.http_session "
						+ "WHERE session_id = ?",
				cookie.sessionId());
		then(sessions).as("Session saved to database").hasSize(1);
		then(sessions.getFirst())
				.as("Session timeout from the server.servlet.session.timeout setting")
				.containsEntry("max_inactive_interval", (int) sessionTimeout.toSeconds());

		final Integer attributeCount = jdbcOperations.queryForObject(
				"SELECT count(*) FROM solaruser.http_session_attributes WHERE session_primary_id = ?",
				Integer.class, sessions.getFirst().get("primary_id"));
		then(attributeCount).as("Session attributes saved to database").isPositive();
	}

}
