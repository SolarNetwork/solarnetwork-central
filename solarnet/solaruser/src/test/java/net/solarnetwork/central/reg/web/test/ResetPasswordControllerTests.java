/* ==================================================================
 * ResetPasswordControllerTests.java - 1/05/2023 7:25:04 am
 * 
 * Copyright 2023 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.reg.web.test;

import static java.net.URLEncoder.encode;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Base64.getUrlEncoder;
import static java.util.regex.Pattern.quote;
import static net.solarnetwork.central.user.config.RegistrationBizConfig.EMAIL_THROTTLE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesRegex;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import javax.cache.Cache;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.test.web.servlet.MockMvc;
import net.solarnetwork.central.mail.mock.MockMailSender;
import net.solarnetwork.central.reg.web.ResetPasswordController;
import net.solarnetwork.central.test.AbstractJUnit5CentralTransactionalTest;
import net.solarnetwork.central.user.biz.RegistrationBiz;
import net.solarnetwork.central.user.domain.PasswordEntry;
import net.solarnetwork.domain.RegistrationReceipt;

/**
 * Test cases for the {@link ResetPasswordController}.
 * 
 * @author matt
 * @version 1.0
 */
@SpringBootTest
@AutoConfigureMockMvc
public class ResetPasswordControllerTests extends AbstractJUnit5CentralTransactionalTest {

	private static final Long TEST_USER_ID = 1L;

	@Autowired
	private MockMvc mvc;

	@Autowired
	private RegistrationBiz registrationBiz;

	@Autowired
	@Qualifier(EMAIL_THROTTLE)
	private Cache<String, Boolean> emailThrottleCache;

	@Autowired
	private MockMailSender mailSender;

	@Test
	public void generateResetCode_withPlus() throws Exception {
		// GIVEN
		final String email = "foo+bar@localhost";
		setupTestUser(TEST_USER_ID, email);

		// THEN
		// @formatter:off
		mvc.perform(post("/u/resetPassword/generate").with(csrf())
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("email", email)
				)
			.andExpect(status().isOk())
			.andExpect(view().name("resetpass/generated"))
			.andExpect(model().attribute("receipt", is(instanceOf(RegistrationReceipt.class))))
			.andExpect(model().attribute("receipt", hasProperty("username", equalTo(email))))
			.andExpect(model().attribute("receipt", hasProperty("usernameURLComponent", equalTo("foo%2Bbar%40localhost"))))
			.andExpect(model().attribute("receipt", hasProperty("confirmationCode", not(emptyOrNullString()))))
			;
		
		// @formatter:on

		String expectedLinkRegex = "(?s).*" + quote("http://localhost/u/resetPassword/confirm?c=")
				+ "[0-9a-zA-Z]+"
				+ quote("&m=" + encode(getUrlEncoder().encodeToString(email.getBytes(UTF_8)), UTF_8))
				+ ".*";

		SimpleMailMessage msg = (SimpleMailMessage) mailSender.getSent().poll();
		assertThat("Email sent to reset email", msg.getTo(), is(arrayContaining(email)));
		assertThat("Email body contains link with Base64 encoded email parameter", msg.getText(),
				matchesRegex(expectedLinkRegex));
	}

	@Test
	public void confirmResetPassword_withPlus() throws Exception {
		// GIVEN
		emailThrottleCache.clear();

		final String email = "foo+bar@localhost";
		setupTestUser(TEST_USER_ID, email);

		RegistrationReceipt receipt = registrationBiz.generateResetPasswordReceipt(email);

		// THEN
		// @formatter:off
		mvc.perform(get("/u/resetPassword/confirm").with(csrf())
				.param("c", receipt.getConfirmationCode())
				.param("m", getUrlEncoder().encodeToString(email.getBytes(UTF_8)))
				)
			.andExpect(status().isOk())
			.andExpect(view().name("resetpass/confirm"))
			.andExpect(model().attribute("form", is(instanceOf(PasswordEntry.class))))
			.andExpect(model().attribute("form", hasProperty("username", equalTo(email))))
			.andExpect(model().attribute("form", hasProperty("confirmationCode", equalTo(receipt.getConfirmationCode()))))
			;
		
		// @formatter:on
	}

}
