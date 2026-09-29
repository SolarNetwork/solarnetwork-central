/* ==================================================================
 * DatumExportControllerWebTests.java - 30 Sept 2026 9:12:04 am
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

package net.solarnetwork.central.reg.web.api.v1.test;

import static net.solarnetwork.central.test.security.WithMockSecurityUser.DEFAULT_NAME;
import static net.solarnetwork.central.test.security.WithMockSecurityUser.DEFAULT_USERNAME;
import static net.solarnetwork.central.test.security.WithMockSecurityUser.DEFAULT_USER_ID;
import static org.assertj.core.api.BDDAssertions.and;
import static org.assertj.core.api.BDDAssertions.from;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import net.solarnetwork.central.reg.web.api.v1.DatumExportController;
import net.solarnetwork.central.security.AuthenticatedUser;
import net.solarnetwork.central.test.AbstractJUnit5CentralTransactionalTest;
import net.solarnetwork.central.user.datum.export.biz.UserExportBiz;
import net.solarnetwork.central.user.datum.export.domain.UserDatumExportConfiguration;

/**
 * Web API level integration tests for timestamp handling in the
 * {@link DatumExportController} class.
 *
 * <p>
 * These tests pin down the timestamp format the web layer accepts and renders,
 * using the {@code minimumExportDate} property of a datum export configuration
 * as a representative {@link Instant} property. SolarUser renders timestamps
 * with a space date/time separator, but accepts either a space or a
 * {@literal T} separator on input.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class DatumExportControllerWebTests extends AbstractJUnit5CentralTransactionalTest {

	/** The export configuration save URL. */
	private static final String CONFIGS_URL = "/api/v1/sec/user/export/configs";

	/**
	 * The instant every variation in this test is expected to resolve to:
	 * 2026-03-01T10:30:49.346827Z.
	 */
	private static final Instant EXPECTED_DATE = Instant.parse("2026-03-01T10:30:49.346827Z");

	/** The rendered form of {@link #EXPECTED_DATE}, with a space separator. */
	private static final String EXPECTED_DATE_RENDERED = "2026-03-01 10:30:49.346827Z";

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private UserExportBiz userExportBiz;

	/**
	 * Get an authentication for a user with the {@code ROLE_EXPORT} authority
	 * required by the datum export endpoints.
	 *
	 * @return the authentication
	 */
	private static Authentication exportUser() {
		var user = new User(DEFAULT_USERNAME, "password",
				AuthorityUtils.createAuthorityList("ROLE_USER", "ROLE_EXPORT"));
		var principal = new AuthenticatedUser(user, DEFAULT_USER_ID, DEFAULT_NAME, false);
		return new UsernamePasswordAuthenticationToken(principal, "password",
				principal.getAuthorities());
	}

	/**
	 * Get an export configuration request body with a given minimum export
	 * date.
	 *
	 * @param minimumExportDate
	 *        the minimum export date, as a JSON string value
	 * @return the request body
	 */
	private static String configJson(String minimumExportDate) {
		// @formatter:off
		return """
				{
					"name":"Test Export",
					"scheduleKey":"d",
					"minimumExportDate":"%s"
				}
				""".formatted(minimumExportDate);
		// @formatter:on
	}

	private MockHttpServletRequestBuilder saveConfigRequest(String json) {
		// @formatter:off
		return post(CONFIGS_URL)
				.content(json)
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.with(authentication(exportUser()))
				.with(csrf())
				;
		// @formatter:on
	}

	@Test
	public void save_spaceDateTimeSeparator() throws Exception {
		// GIVEN
		given(userExportBiz.saveDatumExportConfiguration(any())).willReturn(1L);

		// WHEN
		// @formatter:off
		mvc.perform(saveConfigRequest(configJson("2026-03-01 10:30:49.346827Z")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			;
		// @formatter:on

		// THEN
		// @formatter:off
		var exportConfigCaptor = ArgumentCaptor.forClass(UserDatumExportConfiguration.class);
		then(userExportBiz).should().saveDatumExportConfiguration(exportConfigCaptor.capture());

		and.then(exportConfigCaptor.getValue())
			.as("The space date/time separator is accepted on input")
			.returns(EXPECTED_DATE, from(UserDatumExportConfiguration::getMinimumExportDate))
			;
		// @formatter:on
	}

	@Test
	public void save_tDateTimeSeparator() throws Exception {
		// GIVEN
		given(userExportBiz.saveDatumExportConfiguration(any())).willReturn(1L);

		// WHEN
		// the ISO 8601 "T" date/time separator is accepted as well
		// @formatter:off
		mvc.perform(saveConfigRequest(configJson("2026-03-01T10:30:49.346827Z")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			;
		// @formatter:on

		// THEN
		// @formatter:off
		var exportConfigCaptor = ArgumentCaptor.forClass(UserDatumExportConfiguration.class);
		then(userExportBiz).should().saveDatumExportConfiguration(exportConfigCaptor.capture());

		and.then(exportConfigCaptor.getValue())
			.as("The T date/time separator is accepted on input, and parses to the same instant")
			.returns(EXPECTED_DATE, from(UserDatumExportConfiguration::getMinimumExportDate))
			;
		// @formatter:on
	}

	@Test
	public void save_offsetDateTimeAccepted() throws Exception {
		// GIVEN
		given(userExportBiz.saveDatumExportConfiguration(any())).willReturn(1L);

		// WHEN
		// a UTC offset other than Z is accepted, and normalized to UTC
		// @formatter:off
		mvc.perform(saveConfigRequest(configJson("2026-03-01T23:30:49.346827+13:00")))
			.andExpect(status().isOk())
			;
		// @formatter:on

		// THEN
		// @formatter:off
		var exportConfigCaptor = ArgumentCaptor.forClass(UserDatumExportConfiguration.class);
		then(userExportBiz).should().saveDatumExportConfiguration(exportConfigCaptor.capture());

		and.then(exportConfigCaptor.getValue())
			.as("An offset date/time is accepted on input, and normalized to UTC")
			.returns(EXPECTED_DATE, from(UserDatumExportConfiguration::getMinimumExportDate))
			;
		// @formatter:on
	}

	@Test
	public void save_responseRendersSpaceDateTimeSeparator() throws Exception {
		// GIVEN
		given(userExportBiz.saveDatumExportConfiguration(any())).willReturn(1L);

		// WHEN
		// post using the T separator, to show the response format does not
		// depend on the format used on input
		// @formatter:off
		mvc.perform(saveConfigRequest(configJson("2026-03-01T10:30:49.346827Z")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.minimumExportDate").value(EXPECTED_DATE_RENDERED))
			;
		// @formatter:on
	}

}
