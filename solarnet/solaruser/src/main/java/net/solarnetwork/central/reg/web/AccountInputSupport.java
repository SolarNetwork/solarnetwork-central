/* ==================================================================
 * AccountInputSupport.java - 15/10/2026 6:31:52 pm
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

import java.util.function.Supplier;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.ObjectError;
import net.solarnetwork.central.ValidationException;
import net.solarnetwork.central.security.SecurityUtils;
import net.solarnetwork.central.user.account.biz.UserAccountBiz;
import net.solarnetwork.central.user.account.domain.SnAccountCreationInput;
import net.solarnetwork.central.user.account.domain.SnAccountLinkInput;

/**
 * Support for the controllers of the account input forms.
 *
 * @author matt
 * @version 1.0
 */
final class AccountInputSupport {

	/** The account input field of the parent account email. */
	static final String PARENT_ACCOUNT_USERNAME_FIELD = "parentAccount.username";

	/** The error code for a parent account problem without a code. */
	private static final String PARENT_ACCOUNT_ERROR_CODE = "account.consolidated.error";

	private AccountInputSupport() {
		// not available
	}

	/**
	 * Verify the parent account credentials of an account input, if any.
	 *
	 * <p>
	 * The credentials are verified when the form is submitted for review, so
	 * the password does not have to be kept in the session with the input until
	 * the account is saved. A problem is rejected as an error of the parent
	 * account email field.
	 * </p>
	 *
	 * @param biz
	 *        the service to verify the credentials with, for the active user
	 * @param input
	 *        the account input
	 * @param errors
	 *        the errors to reject problems with
	 */
	static void verifyParentAccount(Supplier<UserAccountBiz> biz, SnAccountCreationInput input,
			Errors errors) {
		final SnAccountLinkInput link = input.getParentAccount();
		final String username = (link != null ? link.getUsername() : null);
		if ( link == null || link.getVerifiedAccount() != null || username == null
				|| username.isBlank() ) {
			return;
		}
		try {
			biz.get().verifyParentAccount(SecurityUtils.getCurrentActorUserId(), link);
		} catch ( ValidationException e ) {
			final Errors source = e.getErrors();
			final ObjectError error = (source != null ? source.getGlobalError() : null);
			final String code = (error != null ? error.getCode() : null);
			errors.rejectValue(PARENT_ACCOUNT_USERNAME_FIELD,
					code != null ? code : PARENT_ACCOUNT_ERROR_CODE,
					error != null ? error.getArguments() : null, e.getMessage());
		}
	}

	/**
	 * Add the errors of a validation exception to a binding result.
	 *
	 * <p>
	 * Errors of another object, such as those a service reports, are added as
	 * global errors.
	 * </p>
	 *
	 * @param errors
	 *        the binding result to add the errors to
	 * @param e
	 *        the exception
	 */
	static void addErrors(BindingResult errors, ValidationException e) {
		final Errors source = e.getErrors();
		if ( source != null && errors.getObjectName().equals(source.getObjectName()) ) {
			errors.addAllErrors(source);
		} else if ( source != null && source.hasErrors() ) {
			for ( ObjectError error : source.getAllErrors() ) {
				errors.addError(new ObjectError(errors.getObjectName(), error.getCodes(),
						error.getArguments(), error.getDefaultMessage()));
			}
		} else {
			errors.addError(new ObjectError(errors.getObjectName(), e.getMessage()));
		}
	}

}
