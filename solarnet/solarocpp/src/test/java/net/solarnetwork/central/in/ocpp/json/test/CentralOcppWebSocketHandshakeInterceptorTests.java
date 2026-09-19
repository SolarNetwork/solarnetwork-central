/* ==================================================================
 * CentralOcppWebSocketHandshakeInterceptorTests.java - 25/05/2024 11:35:28 am
 * 
 * Copyright 2024 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.in.ocpp.json.test;

import static net.solarnetwork.central.in.ocpp.config.OcppV16WebSocketConfig.BASIC_CLIENT_ID_REGEX;
import static net.solarnetwork.central.in.ocpp.config.OcppV16WebSocketConfig.HID_BASIC_CLIENT_ID_REGEX;
import static net.solarnetwork.central.in.ocpp.config.OcppV16WebSocketConfig.HID_BASIC_HID_REGEX;
import static net.solarnetwork.central.in.ocpp.config.OcppV16WebSocketConfig.HID_PATH_CLIENT_ID_REGEX;
import static net.solarnetwork.central.in.ocpp.config.OcppV16WebSocketConfig.HID_PATH_CREDS_REGEX;
import static net.solarnetwork.central.in.ocpp.config.OcppV16WebSocketConfig.HID_PATH_HID_REGEX;
import static net.solarnetwork.central.in.ocpp.config.OcppV16WebSocketConfig.PATH_CLIENT_ID_REGEX;
import static net.solarnetwork.central.in.ocpp.config.OcppV16WebSocketConfig.PATH_CREDS_REGEX;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static org.assertj.core.api.BDDAssertions.and;
import static org.assertj.core.api.InstanceOfAssertFactories.array;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.socket.SubProtocolCapable;
import org.springframework.web.socket.WebSocketHandler;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.domain.LogEventInfo;
import net.solarnetwork.central.in.ocpp.json.CentralOcppWebSocketHandshakeInterceptor;
import net.solarnetwork.central.ocpp.dao.CentralSystemUserDao;
import net.solarnetwork.central.ocpp.dao.UserSettingsDao;
import net.solarnetwork.central.ocpp.domain.CentralSystemUser;
import net.solarnetwork.central.ocpp.domain.UserSettings;
import net.solarnetwork.codec.jackson.JsonUtils;
import net.solarnetwork.ocpp.json.WebSocketSubProtocol;
import net.solarnetwork.service.PasswordEncoder;

/**
 * Test cases for the {@link CentralOcppWebSocketHandshakeInterceptor} class.
 * 
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class CentralOcppWebSocketHandshakeInterceptorTests {

	@Mock
	private CentralSystemUserDao systemUserDao;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private UserSettingsDao userSettingsDao;

	@Mock
	private UserEventAppenderBiz userEventAppenderBiz;

	@Mock(extraInterfaces = SubProtocolCapable.class)
	private WebSocketHandler handler;

	@Captor
	private ArgumentCaptor<LogEventInfo> userEventCaptor;

	private CentralOcppWebSocketHandshakeInterceptor createInterceptor(Pattern clientIdUriPattern,
			Pattern credentialsUriPattern, Pattern hidUriPattern) {
		CentralOcppWebSocketHandshakeInterceptor interceptor = new CentralOcppWebSocketHandshakeInterceptor(
				systemUserDao, passwordEncoder, userSettingsDao, credentialsUriPattern, hidUriPattern);
		interceptor.setClientIdUriPattern(clientIdUriPattern);
		interceptor.setUserEventAppenderBiz(userEventAppenderBiz);

		return interceptor;
	}

	@BeforeEach
	public void setup() {
		// anything?
	}

	@Test
	public void connectBasicCredentials() throws Exception {
		// GIVEN
		final var interceptor = createInterceptor(BASIC_CLIENT_ID_REGEX, null, null);
		final var chargerIdent = randomString();
		final var username = "foo";
		final var password = "bar";

		// verify sub-protocol
		given(((SubProtocolCapable) handler).getSubProtocols())
				.willReturn(Arrays.asList(WebSocketSubProtocol.OCPP_V16.getValue()));

		// lookup system user based on given username + charger identifier
		final CentralSystemUser sysUser = new CentralSystemUser(randomLong(), Instant.now(), username,
				"supersecret");
		given(systemUserDao.getForUsernameAndChargePoint(username, chargerIdent)).willReturn(sysUser);

		// validate password
		given(passwordEncoder.matches(password, sysUser.getPassword())).willReturn(true);

		// @formatter:off
		final MockHttpServletRequest req = MockMvcRequestBuilders
				.get("http://localhost/ocpp/j/v16/{ident}", chargerIdent)
				.header("Connection", "Upgrade")
				.header("Upgrade", "websocket")
				.header("Sec-WebSocket-Protocol", "ocpp1.6")
				.header("Authorization", "Basic %s".formatted(Base64.getEncoder()
						.encodeToString("%s:%s".formatted(
								username, password).getBytes(StandardCharsets.US_ASCII))))
				.buildRequest(null);
		// @formatter:on

		final MockHttpServletResponse res = new MockHttpServletResponse();

		final var attr = new LinkedHashMap<String, Object>(8);

		// WHEN
		boolean result = interceptor.beforeHandshake(new ServletServerHttpRequest(req),
				new ServletServerHttpResponse(res), handler, attr);

		// THEN
		// user event generated
		then(userEventAppenderBiz).shouldHaveNoInteractions();

		// OK result
		and.then(result).as("Handshake OK").isTrue();
	}

	@Test
	public void connectBasicCredentials_badPassword() throws Exception {
		// GIVEN
		final var interceptor = createInterceptor(BASIC_CLIENT_ID_REGEX, null, null);
		final var chargerIdent = randomString();
		final var username = "foo";
		final var password = "bar";

		// verify sub-protocol
		given(((SubProtocolCapable) handler).getSubProtocols())
				.willReturn(Arrays.asList(WebSocketSubProtocol.OCPP_V16.getValue()));

		// lookup system user based on given username + charger identifier
		final CentralSystemUser sysUser = new CentralSystemUser(randomLong(), Instant.now(), username,
				"supersecret");
		given(systemUserDao.getForUsernameAndChargePoint(username, chargerIdent)).willReturn(sysUser);

		// validate password: INVALID!
		given(passwordEncoder.matches(password, sysUser.getPassword())).willReturn(false);

		// @formatter:off
		final MockHttpServletRequest req = MockMvcRequestBuilders
				.get("http://localhost/ocpp/j/v16/{ident}", chargerIdent)
				.header("Connection", "Upgrade")
				.header("Upgrade", "websocket")
				.header("Sec-WebSocket-Protocol", "ocpp1.6")
				.header("Authorization", "Basic %s".formatted(Base64.getEncoder()
						.encodeToString("%s:%s".formatted(
								username, password).getBytes(StandardCharsets.US_ASCII))))
				.buildRequest(null);
		// @formatter:on

		final MockHttpServletResponse res = new MockHttpServletResponse();

		final var attr = new LinkedHashMap<String, Object>(8);

		// WHEN
		boolean result = interceptor.beforeHandshake(new ServletServerHttpRequest(req),
				new ServletServerHttpResponse(res), handler, attr);

		// THEN
		// user event generated
		then(userEventAppenderBiz).should().addEvent(eq(sysUser.getUserId()), userEventCaptor.capture());
		// @formatter:off
		and.then(userEventCaptor.getValue())
			.satisfies(e -> {
				and.then(e.getData())
					.as("Data provided")
					.isNotNull()
					;
				Map<String, Object> data = JsonUtils.getStringMap(e.getData());
				and.then(data)
					.as("Charger identify provided")
					.containsEntry("cp", chargerIdent)
					.as("Username provided")
					.containsEntry("username", username)
					.hasEntrySatisfying("error", err -> {
						and.then(err)
							.as("Error message provided")
							.asInstanceOf(InstanceOfAssertFactories.STRING)
							.containsPattern("password mismatch")
							;
					})
					;
			})
			.as("Success tags")
			.extracting(LogEventInfo::getTags, array(String[].class))
				.containsExactly("ocpp", "charger", "forbidden")
			;
		// @formatter:on

		// OK result
		and.then(result).as("Handshake fail").isFalse();
	}

	@Test
	public void connectPathCredentials() throws Exception {
		// GIVEN
		final var interceptor = createInterceptor(PATH_CLIENT_ID_REGEX, PATH_CREDS_REGEX, null);
		final var chargerIdent = randomString();
		final var username = "foo";
		final var password = "bar";

		// verify sub-protocol
		given(((SubProtocolCapable) handler).getSubProtocols())
				.willReturn(Arrays.asList(WebSocketSubProtocol.OCPP_V16.getValue()));

		// lookup system user based on given username + charger identifier
		final CentralSystemUser sysUser = new CentralSystemUser(randomLong(), Instant.now(), username,
				"supersecret");
		given(systemUserDao.getForUsernameAndChargePoint(username, chargerIdent)).willReturn(sysUser);

		// validate password
		given(passwordEncoder.matches(password, sysUser.getPassword())).willReturn(true);

		// @formatter:off
		final MockHttpServletRequest req = MockMvcRequestBuilders
				.get("http://localhost/ocpp/j/v16u/{username}/{password}/{ident}",
						username, password, chargerIdent)
				.header("Connection", "Upgrade")
				.header("Upgrade", "websocket")
				.header("Sec-WebSocket-Protocol", "ocpp1.6")
				.buildRequest(null);
		// @formatter:on

		final MockHttpServletResponse res = new MockHttpServletResponse();

		final var attr = new LinkedHashMap<String, Object>(8);

		// WHEN
		boolean result = interceptor.beforeHandshake(new ServletServerHttpRequest(req),
				new ServletServerHttpResponse(res), handler, attr);

		// THEN
		and.then(result).as("Handshake OK").isTrue();
	}

	@Test
	public void hidConnectBasicCredentials() throws Exception {
		// GIVEN
		final var interceptor = createInterceptor(HID_BASIC_CLIENT_ID_REGEX, null, HID_BASIC_HID_REGEX);
		final var hid = randomString();
		final var chargerIdent = randomString();
		final var username = "foo";
		final var password = "bar";

		// verify sub-protocol
		given(((SubProtocolCapable) handler).getSubProtocols())
				.willReturn(Arrays.asList(WebSocketSubProtocol.OCPP_V16.getValue()));

		// lookup system user based on given username + charger identifier
		final CentralSystemUser sysUser = new CentralSystemUser(randomLong(), Instant.now(), username,
				"supersecret");
		given(systemUserDao.getForUsernameAndChargePoint(username, chargerIdent)).willReturn(sysUser);

		// validate password
		given(passwordEncoder.matches(password, sysUser.getPassword())).willReturn(true);

		// @formatter:off
		final MockHttpServletRequest req = MockMvcRequestBuilders
				.get("http://localhost/ocpp/j/v16h/{hid}/{ident}", hid, chargerIdent)
				.header("Connection", "Upgrade")
				.header("Upgrade", "websocket")
				.header("Sec-WebSocket-Protocol", "ocpp1.6")
				.header("Authorization", "Basic %s".formatted(Base64.getEncoder()
						.encodeToString("%s:%s".formatted(
								username, password).getBytes(StandardCharsets.US_ASCII))))
				.buildRequest(null);
		// @formatter:on

		final MockHttpServletResponse res = new MockHttpServletResponse();

		final var attr = new LinkedHashMap<String, Object>(8);

		// WHEN
		boolean result = interceptor.beforeHandshake(new ServletServerHttpRequest(req),
				new ServletServerHttpResponse(res), handler, attr);

		// THEN
		// user event generated
		then(userEventAppenderBiz).shouldHaveNoInteractions();

		// OK result
		and.then(result).as("Handshake OK").isTrue();
	}

	@Test
	public void hidConnectPathCredentials() throws Exception {
		// GIVEN
		final var interceptor = createInterceptor(HID_PATH_CLIENT_ID_REGEX, HID_PATH_CREDS_REGEX,
				HID_PATH_HID_REGEX);
		final var hid = randomString();
		final var chargerIdent = randomString();
		final var username = "foo";
		final var password = "bar";

		// verify sub-protocol
		given(((SubProtocolCapable) handler).getSubProtocols())
				.willReturn(Arrays.asList(WebSocketSubProtocol.OCPP_V16.getValue()));

		// lookup system user based on given username + charger identifier
		final CentralSystemUser sysUser = new CentralSystemUser(randomLong(), Instant.now(), username,
				"supersecret");
		given(systemUserDao.getForUsernameAndChargePoint(username, chargerIdent)).willReturn(sysUser);

		// validate password
		given(passwordEncoder.matches(password, sysUser.getPassword())).willReturn(true);

		// @formatter:off
		final MockHttpServletRequest req = MockMvcRequestBuilders
				.get("http://localhost/ocpp/j/v16hu/{hid}/{username}/{password}/{ident}",
						hid, username, password, chargerIdent)
				.header("Connection", "Upgrade")
				.header("Upgrade", "websocket")
				.header("Sec-WebSocket-Protocol", "ocpp1.6")
				.buildRequest(null);
		// @formatter:on

		final MockHttpServletResponse res = new MockHttpServletResponse();

		final var attr = new LinkedHashMap<String, Object>(8);

		// WHEN
		boolean result = interceptor.beforeHandshake(new ServletServerHttpRequest(req),
				new ServletServerHttpResponse(res), handler, attr);

		// THEN
		and.then(result).as("Handshake OK").isTrue();
	}

	@Test
	public void hidConnectBasicCredentials_unknownUser() throws Exception {
		// GIVEN
		final var interceptor = createInterceptor(HID_BASIC_CLIENT_ID_REGEX, null, HID_BASIC_HID_REGEX);
		final var hid = randomString();
		final var chargerIdent = randomString();
		final var username = "foo";
		final var password = "bar";

		// verify sub-protocol
		given(((SubProtocolCapable) handler).getSubProtocols())
				.willReturn(Arrays.asList(WebSocketSubProtocol.OCPP_V16.getValue()));

		// lookup system user based on given username + charger identifier (not found)
		given(systemUserDao.getForUsernameAndChargePoint(username, chargerIdent)).willReturn(null);

		// lookup user settings for hid
		UserSettings settings = new UserSettings(randomLong(), Instant.now(), hid);
		given(userSettingsDao.getForHid(hid)).willReturn(settings);

		// @formatter:off
		final MockHttpServletRequest req = MockMvcRequestBuilders
				.get("http://localhost/ocpp/j/v16h/{hid}/{ident}", hid, chargerIdent)
				.header("Connection", "Upgrade")
				.header("Upgrade", "websocket")
				.header("Sec-WebSocket-Protocol", "ocpp1.6")
				.header("Authorization", "Basic %s".formatted(Base64.getEncoder()
						.encodeToString("%s:%s".formatted(
								username, password).getBytes(StandardCharsets.US_ASCII))))
				.buildRequest(null);
		// @formatter:on

		final MockHttpServletResponse res = new MockHttpServletResponse();

		final var attr = new LinkedHashMap<String, Object>(8);

		// WHEN
		boolean result = interceptor.beforeHandshake(new ServletServerHttpRequest(req),
				new ServletServerHttpResponse(res), handler, attr);

		// THEN
		// user event generated
		then(userEventAppenderBiz).should().addEvent(eq(settings.getUserId()),
				userEventCaptor.capture());
		// @formatter:off
		and.then(userEventCaptor.getValue())
			.satisfies(e -> {
				and.then(e.getData())
					.as("Data provided")
					.isNotNull()
					;
				Map<String, Object> data = JsonUtils.getStringMap(e.getData());
				and.then(data)
					.as("Charger identify provided")
					.containsEntry("cp", chargerIdent)
					.as("Username provided")
					.containsEntry("username", username)
					.hasEntrySatisfying("error", err -> {
						and.then(err)
							.as("Error message provided")
							.asInstanceOf(InstanceOfAssertFactories.STRING)
							.containsPattern("not available, or not allowed")
							;
					})
					;
			})
			.as("Success tags")
			.extracting(LogEventInfo::getTags, array(String[].class))
				.containsExactly("ocpp", "charger", "forbidden")
			;
		// @formatter:on

		// OK result
		and.then(result).as("Handshake fail").isFalse();
	}

}
