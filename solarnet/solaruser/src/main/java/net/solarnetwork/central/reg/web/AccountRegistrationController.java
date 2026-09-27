/* ==================================================================
 * AccountController.java - 27 Sept 2026 11:49:25 am
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

package net.solarnetwork.central.reg.web;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.util.Locale;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.SessionAttributes;
import jakarta.validation.Valid;
import net.solarnetwork.central.security.SecurityUtils;
import net.solarnetwork.central.user.account.biz.UserAccountBiz;
import net.solarnetwork.central.user.account.domain.SnAccountCreationInput;
import net.solarnetwork.central.user.account.domain.SnAccountInput;
import net.solarnetwork.central.user.account.domain.SnAddressInput;
import net.solarnetwork.central.user.biz.UserBiz;
import net.solarnetwork.central.user.domain.User;

/**
 * Controller for the account registration pages.
 *
 * @author matt
 * @version 1.0
 */
@SessionAttributes({ AccountRegistrationController.ACCOUNT_INPUT })
@RequestMapping(value = "/u/sec/account/register")
@GlobalServiceController
public class AccountRegistrationController {

	/** The model key to use for the account input object. */
	public static final String ACCOUNT_INPUT = "accountInput";

	private final UserBiz userBiz;
	private final @Nullable UserAccountBiz userAccountBiz;

	/**
	 * Constructor.
	 *
	 * @param userBiz
	 *        the user biz
	 * @param userAccountBiz
	 *        the user account biz
	 */
	public AccountRegistrationController(UserBiz userBiz,
			@Autowired(required = false) @Nullable UserAccountBiz userAccountBiz) {
		super();
		this.userBiz = requireNonNullArgument(userBiz, "userBiz");
		this.userAccountBiz = requireNonNullArgument(userAccountBiz, "userAccountBiz");
	}

	/**
	 * Get the {@link userAccountBiz}.
	 *
	 * @return the service; never {@code null}
	 * @throws UnsupportedOperationException
	 *         if the service is not available
	 */
	private UserAccountBiz biz() {
		if ( userAccountBiz == null ) {
			throw new UnsupportedOperationException("UserAccountBiz service not available.");
		}
		return userAccountBiz;
	}

	/**
	 * Provide the account creation input form bean.
	 *
	 * @return the account creation input form bean
	 */
	@ModelAttribute(binding = true, name = "accountInput")
	public SnAccountCreationInput accountInput() {
		final var form = new SnAccountCreationInput();

		// populate some default values based on the active user
		final User user = userBiz.getUser(SecurityUtils.getCurrentActorUserId());
		final var addr = new SnAddressInput();
		form.setAddress(addr);
		addr.setEmail(user.getEmail());
		addr.setCountry(user.getCountry());
		addr.setTimeZoneId(user.timeZone().getId());

		final var acct = new SnAccountInput();
		form.setAccount(acct);
		Locale locale = Locale
				.forLanguageTag("%s-%s".formatted(user.lang(), user.getCountry().toUpperCase()));
		acct.setLocale(locale.toLanguageTag());

		return form;
	}

	/**
	 * Render the account registration page.
	 *
	 * @return
	 */
	@RequestMapping(value = "", method = RequestMethod.GET)
	public String register() {
		return "sec/account/register";
	}

	/**
	 * Render the account main page.
	 *
	 * @return
	 */
	@RequestMapping(value = "", method = RequestMethod.POST)
	public String submit(
			@Valid @ModelAttribute(AccountRegistrationController.ACCOUNT_INPUT) SnAccountCreationInput input,
			Errors errors) {
		if ( errors.hasErrors() ) {
			return "sec/account/register";
		}
		return "sec/account/register-confirm";
	}

	/**
	 * Render the account main page.
	 *
	 * @return
	 */
	@RequestMapping(value = "/confirm", method = RequestMethod.POST)
	public String confirm(
			@Valid @ModelAttribute(AccountRegistrationController.ACCOUNT_INPUT) SnAccountCreationInput input) {
		return "sec/account/register-confirm";
	}

}
