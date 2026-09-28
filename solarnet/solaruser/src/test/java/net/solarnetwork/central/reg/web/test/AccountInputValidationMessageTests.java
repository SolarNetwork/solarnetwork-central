/* ==================================================================
 * AccountInputValidationMessageTests.java - 28 Sept 2026 1:34:57 pm
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

import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.BDDAssertions.and;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.FieldError;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.ServletRequestDataBinder;
import net.solarnetwork.central.reg.web.AccountEditController;
import net.solarnetwork.central.user.account.domain.SnAccountCreationInput;
import net.solarnetwork.central.user.account.domain.SnAccountInput;
import net.solarnetwork.central.user.account.domain.SnAddressInput;

/**
 * Verify the application messages provide a message for every
 * {@link SnAccountCreationInput} constraint violation the account web pages can
 * produce.
 *
 * <p>
 * Message codes include the object name of the bound form bean, so these bind
 * to the same form bean name the account controllers use.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
public class AccountInputValidationMessageTests {

	private LocalValidatorFactoryBean validator;
	private MessageSource messageSource;

	@BeforeEach
	public void setup() {
		validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();

		// the same basename and encoding Spring Boot configures by default
		var msg = new ResourceBundleMessageSource();
		msg.setBasename("messages");
		msg.setDefaultEncoding("UTF-8");
		messageSource = msg;
	}

	/**
	 * Bind and validate form parameters, returning the resolved message of each
	 * field error, keyed by field path.
	 *
	 * @param params
	 *        the form parameters to bind
	 * @return the resolved messages
	 */
	private Map<String, String> fieldErrorMessages(Map<String, String> params) {
		final var binder = new ServletRequestDataBinder(new SnAccountCreationInput(),
				AccountEditController.ACCOUNT_INPUT);
		binder.setValidator(validator);

		final var request = new MockHttpServletRequest();
		params.forEach(request::addParameter);

		binder.bind(request);
		binder.validate();

		return binder.getBindingResult().getFieldErrors().stream().collect(toMap(FieldError::getField,
				e -> messageSource.getMessage(e, Locale.ENGLISH)));
	}

	@Test
	public void requiredFields() {
		// GIVEN
		// an empty form, as the browser posts blank inputs
		final var params = Map.of("address.name", "", "address.email", "", "address.country", "",
				"address.timeZoneId", "", "account.locale", "");

		// WHEN
		final Map<String, String> messages = fieldErrorMessages(params);

		// THEN
		// @formatter:off
		and.then(messages)
			.as("A message is resolved for every required field")
			.containsOnlyKeys("address.name", "address.email", "address.street", "address.country",
					"address.timeZoneId", "account.currency", "account.locale")
			.as("Address name required message resolved")
			.containsEntry("address.name", "The name is required.")
			.as("Address email required message resolved")
			.containsEntry("address.email", "The email address is required.")
			.as("Address country required message resolved")
			.containsEntry("address.country", "The country code is required.")
			.as("Address time zone required message resolved")
			.containsEntry("address.timeZoneId", "The time zone is required.")
			.as("Account currency required message resolved")
			.containsEntry("account.currency", "The currency is required.")
			.as("Account locale required message resolved")
			.containsEntry("account.locale", "The language is required.")
			.as("Address street required message resolved")
			.containsEntry("address.street", "The street address is required.")
			;
		// @formatter:on
	}

	@Test
	public void invalidFields() {
		// GIVEN
		final var params = Map.of("address.name", "Tester Dude", "address.email", "not-an-email",
				"address.country", "XX", "address.timeZoneId", "Nowhere/Nothing", "account.locale",
				"!not a language tag!");

		// WHEN
		final Map<String, String> messages = fieldErrorMessages(params);

		// THEN
		// @formatter:off
		and.then(messages)
			.as("Address email invalid message resolved")
			.containsEntry("address.email", "The email address is not a valid email address.")
			.as("Address country invalid message resolved")
			.containsEntry("address.country", "The country code is not a valid two-letter ISO 3166"
					+ " country code, such as US or NZ.")
			.as("Address time zone invalid message resolved")
			.containsEntry("address.timeZoneId", "The time zone is not a valid time zone ID, such as"
					+ " America/New_York or Pacific/Auckland.")
			.as("Account locale invalid message resolved")
			.containsEntry("account.locale", "The language is not a valid IETF language tag, such as"
					+ " en-US or fr-CA.")
			;
		// @formatter:on
	}

	@Test
	public void blankStreetLines() {
		// GIVEN
		// the form always posts both street inputs, so a missing street arrives as blank lines
		// rather than as an absent parameter
		final var params = Map.of("address.name", "Tester Dude", "address.email", "test@localhost",
				"address.country", "NZ", "address.timeZoneId", "Pacific/Auckland", "address.street1", "",
				"address.street2", "", "account.locale", "en-NZ");

		// WHEN
		final Map<String, String> messages = fieldErrorMessages(params);

		// THEN
		// @formatter:off
		and.then(messages)
			.as("Blank street lines are a field error, not an exception from SnAddressInput.street()")
			.containsEntry("address.street", "The street address is required.")
			;
		// @formatter:on
	}

	@Test
	public void streetLineSupplied() {
		// GIVEN
		// one non-blank line is enough, the second line stays optional
		final var params = Map.of("address.name", "Tester Dude", "address.email", "test@localhost",
				"address.country", "NZ", "address.timeZoneId", "Pacific/Auckland", "address.street1",
				"123 Main Street", "address.street2", "", "account.locale", "en-NZ");

		// WHEN
		final Map<String, String> messages = fieldErrorMessages(params);

		// THEN
		// @formatter:off
		and.then(messages)
			.as("Currency is the only field left unsatisfied")
			.containsOnlyKeys("account.currency")
			;
		// @formatter:on
	}

	@Test
	public void missingNestedInputs() {
		// GIVEN
		// nothing bound at all, so the nested input beans themselves are missing
		final var binder = new ServletRequestDataBinder(new SnAccountCreationInput(),
				AccountEditController.ACCOUNT_INPUT);
		binder.setValidator(validator);

		// WHEN
		binder.validate();

		final Map<String, String> messages = binder.getBindingResult().getFieldErrors().stream()
				.collect(toMap(FieldError::getField, e -> messageSource.getMessage(e, Locale.ENGLISH)));

		// THEN
		// @formatter:off
		and.then(messages)
			.as("A message is resolved for each missing nested input bean")
			.containsOnlyKeys("account", "address")
			.as("Account input required message resolved")
			.containsEntry("account", "The account details are required.")
			.as("Address input required message resolved")
			.containsEntry("address", "The address details are required.")
			;
		// @formatter:on
	}

	@Test
	public void restObjectNameMessages() {
		// GIVEN
		// the REST API binds the same input under a name derived from its class
		final var binder = new ServletRequestDataBinder(new SnAccountCreationInput(),
				"snAccountCreationInput");
		binder.setValidator(validator);

		// WHEN
		binder.validate();

		final Map<String, String> messages = binder.getBindingResult().getFieldErrors().stream()
				.collect(toMap(FieldError::getField, e -> messageSource.getMessage(e, Locale.ENGLISH)));

		// THEN
		// @formatter:off
		and.then(messages)
			.as("Account input required message resolved for the REST object name too")
			.containsEntry("account", "The account details are required.")
			.as("Address input required message resolved for the REST object name too")
			.containsEntry("address", "The address details are required.")
			;
		// @formatter:on
	}

	@Test
	public void nestedInputMessagesAreObjectNameIndependent() {
		// GIVEN
		// the nested field messages are keyed by property path alone, so they
		// resolve whatever the bound form bean is called
		final var input = new SnAccountCreationInput();
		input.setAccount(new SnAccountInput());
		input.setAddress(new SnAddressInput());

		final var binder = new ServletRequestDataBinder(input, "snAccountCreationInput");
		binder.setValidator(validator);

		// WHEN
		binder.validate();

		final Map<String, String> messages = binder.getBindingResult().getFieldErrors().stream()
				.collect(toMap(FieldError::getField, e -> messageSource.getMessage(e, Locale.ENGLISH)));

		// THEN
		// @formatter:off
		and.then(messages)
			.as("A message is resolved for every required nested field")
			.containsOnlyKeys("address.name", "address.email", "address.street", "address.country",
					"address.timeZoneId", "account.currency", "account.locale")
			.as("Address name required message resolved")
			.containsEntry("address.name", "The name is required.")
			.as("Account currency required message resolved")
			.containsEntry("account.currency", "The currency is required.")
			;
		// @formatter:on
	}

}
