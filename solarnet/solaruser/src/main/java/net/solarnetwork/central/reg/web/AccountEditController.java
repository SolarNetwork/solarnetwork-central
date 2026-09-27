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
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionStatus;
import jakarta.validation.Valid;
import net.solarnetwork.central.ValidationException;
import net.solarnetwork.central.security.SecurityUtils;
import net.solarnetwork.central.user.account.biz.UserAccountBiz;
import net.solarnetwork.central.user.account.domain.SnAccountCreationInput;
import net.solarnetwork.central.user.account.domain.SnAccountInfo;
import net.solarnetwork.central.user.account.domain.SnAccountInput;
import net.solarnetwork.central.user.account.domain.SnAddressInput;

/**
 * Controller for the account registration pages.
 *
 * @author matt
 * @version 1.0
 */
@SessionAttributes({ AccountEditController.ACCOUNT_INPUT })
@RequestMapping(value = "/u/sec/account/edit")
@GlobalServiceController
public class AccountEditController {

	/** The model key to use for the account input object. */
	public static final String ACCOUNT_INPUT = "accountInput";

	private final @Nullable UserAccountBiz userAccountBiz;

	/**
	 * Constructor.
	 *
	 * @param userAccountBiz
	 *        the user account biz
	 */
	public AccountEditController(@Autowired(required = false) @Nullable UserAccountBiz userAccountBiz) {
		super();
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

		final SnAccountInfo info = userAccountBiz
				.getAccountForUser(SecurityUtils.getCurrentActorUserId());

		final var addr = SnAddressInput.forAddress(info.account().getAddress());
		form.setAddress(addr);

		final var acct = SnAccountInput.forAccount(info.account());
		form.setAccount(acct);

		form.setRequestedEntitlements(info.entitlements());

		return form;
	}

	/**
	 * Render the account edit page.
	 *
	 * @return the account edit page reference
	 */
	@RequestMapping(value = "", method = RequestMethod.GET)
	public String edit() {
		return "sec/account/edit";
	}

	/**
	 * Submit the account edit form for confirmation (review).
	 *
	 * @param input
	 *        the form to confirm
	 * @param errors
	 *        validation errors
	 * @return the account edit confirmation page reference
	 */
	@RequestMapping(value = "", method = RequestMethod.POST)
	public String submit(
			@Valid @ModelAttribute(AccountEditController.ACCOUNT_INPUT) SnAccountCreationInput input,
			Errors errors) {
		if ( errors.hasErrors() ) {
			return "sec/account/edit";
		}
		return "sec/account/edit-confirm";
	}

	/**
	 * Submit the account update (after review).
	 *
	 * @param input
	 *        the form to save
	 * @param errors
	 *        validation errors
	 * @param sessionStatus
	 *        the session status
	 * @return the destination page reference
	 */
	@RequestMapping(value = "/confirm", method = RequestMethod.POST)
	public String confirm(
			@Valid @ModelAttribute(AccountEditController.ACCOUNT_INPUT) SnAccountCreationInput input,
			BindingResult errors, SessionStatus sessionStatus) {
		if ( errors.hasErrors() ) {
			return "sec/account/edit";
		}
		try {
			biz().updateAccount(SecurityUtils.getCurrentActorUserId(), input);
		} catch ( ValidationException e ) {
			errors.addAllErrors(e.getErrors());
		} catch ( Exception e ) {
			errors.addError(new ObjectError(ACCOUNT_INPUT, e.getMessage()));
		}
		if ( errors.hasErrors() ) {
			return "sec/account/edit";
		}
		sessionStatus.setComplete();
		return "redirect:/u/sec/account";
	}

}
