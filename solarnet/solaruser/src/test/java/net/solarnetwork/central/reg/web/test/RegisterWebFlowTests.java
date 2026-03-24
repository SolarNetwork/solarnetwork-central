/* ==================================================================
 * RegisterWebFlowTests.java - 23/03/2026 1:25:45 pm
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

package net.solarnetwork.central.reg.web.test;

import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.SharedHttpSessionConfigurer.sharedHttpSession;
import java.net.URI;
import java.util.Arrays;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.MimeMailMessage;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage.RecipientType;
import net.solarnetwork.central.mail.mock.MockMailSender;
import net.solarnetwork.central.user.biz.RegistrationBiz;
import net.solarnetwork.central.user.domain.User;
import net.solarnetwork.domain.RegistrationReceipt;

/**
 * Registration WebFlow tests.
 *
 * @author matt
 * @version 1.0
 */
@SpringBootTest
//@AutoConfigureMockMvc
@Transactional
@Rollback
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("logging-user-event-appender")
public class RegisterWebFlowTests {

	@Autowired
	private WebApplicationContext webApplicationContext;

	@Autowired
	private MockMailSender mailSender;

	@Autowired
	private RegistrationBiz registrationBiz;

	private MockMvc mvc;

	@BeforeEach
	public void setup() {
		// @formatter:off
		mvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
				.apply(sharedHttpSession())
				.build()
				;
		// @formatter:on
	}

	@Test
	public void submitNewRegistrationForm() throws Exception {
		// GIVEN
		final String name = randomString();
		final String email = randomString() + "@localhost";
		final String password = randomString();

		// WHEN
		// 1) request form
		// @formatter:off
		var initialResponse = mvc.perform(get("/register")
				.accept(MediaType.TEXT_HTML)
			)
			.andExpect(status().is3xxRedirection())
			.andReturn()
			.getResponse()
			;

		var nextLoc = initialResponse.getHeader("Location");
		var nextUri = URI.create(nextLoc);

		// 2) load form
		mvc.perform(get(nextUri)
				.accept(MediaType.TEXT_HTML)
				//.cookie(cookies)
			)
			.andExpect(status().isOk())
			;

		// 3) post form
		var submitFormResponse = mvc.perform(post(nextUri)
				.accept(MediaType.TEXT_HTML)
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.formField("name", name)
				.formField("email", email)
				.formField("password", password)
				.formField("_eventId_next", "")
				.with(csrf())
			)
			.andExpect(status().is3xxRedirection())
			.andReturn()
			.getResponse()
			;

		nextLoc = submitFormResponse.getHeader("Location");
		nextUri = URI.create(nextLoc);

		// 4) get confirm form
		mvc.perform(get(nextUri)
				.accept(MediaType.TEXT_HTML)
			)
			.andExpect(status().isOk())
			;

		// 5) post confirm form
		mvc.perform(post(nextUri)
				.accept(MediaType.TEXT_HTML)
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.formField("_eventId_next", "")
				.with(csrf())
			)
			.andExpect(status().isOk())
			.andReturn()
			.getResponse()
			;


		// THEN
		then(initialResponse)
			.isNotNull()
			;

		then(mailSender.getSent())
			.as("Registration confirmation link sent")
			.hasSize(1)
			.element(0, type(MimeMailMessage.class))
			.extracting(MimeMailMessage::getMimeMessage)
			.as("Email with registration details sent to given email")
			.returns(new String[] {email}, msg -> {
				try {
					return Arrays.stream(msg.getRecipients(RecipientType.TO))
							.map(InternetAddress.class::cast)
							.map(InternetAddress::getAddress)
							.toArray(String[]::new);
				} catch ( Exception e ) {
					throw new RuntimeException(e);
				}
			})
			.extracting(msg -> {
				try {
					return msg.getContent();
				} catch ( Exception e ) {
					throw new RuntimeException(e);
				}
			})
			.asInstanceOf(InstanceOfAssertFactories.STRING)
			.as("Email body contains link to confirmation flow")
			.contains("/register/confirm.do")
			;
		// @formatter:on
	}

	@Test
	public void confirmNewRegistrationForm() throws Exception {
		// GIVEN
		// register new user
		final var email = randomString() + "@localhost";
		final var regUser = new User(email);
		regUser.setName(randomString());
		regUser.setPassword(randomString());
		final RegistrationReceipt receipt = registrationBiz.registerUser(regUser);

		// WHEN
		// @formatter:off
		final UriComponents confirmUri = UriComponentsBuilder.fromPath("/register/confirm")
			.queryParam("m", receipt.getUsername())
			.queryParam("c", receipt.getConfirmationCode())
			.build()
			;

		mvc.perform(get(confirmUri.toUriString())
				.accept(MediaType.TEXT_HTML)
			)
			.andExpect(status().isOk())
			;
		// @formatter:on
	}

}
