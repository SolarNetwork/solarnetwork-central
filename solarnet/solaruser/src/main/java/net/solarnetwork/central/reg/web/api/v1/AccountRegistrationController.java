/* ==================================================================
 * AccountRegistrationController.java - 28 Sept 2026 7:56:54 am
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

package net.solarnetwork.central.reg.web.api.v1;

import static net.solarnetwork.domain.Result.success;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import net.solarnetwork.central.security.SecurityUtils;
import net.solarnetwork.central.user.account.biz.UserAccountBiz;
import net.solarnetwork.central.user.account.domain.SnAccountCreationInput;
import net.solarnetwork.central.user.account.domain.SnAccountInfo;
import net.solarnetwork.central.web.GlobalExceptionRestController;
import net.solarnetwork.domain.Result;

/**
 * Web service API for billing account registration.
 *
 * @author matt
 * @version 1.0
 */
@RestController("v1AccountRegistrationController")
@RequestMapping(value = "/api/v1/sec/user/account/register")
@GlobalExceptionRestController
public class AccountRegistrationController {

	private final @Nullable UserAccountBiz userAccountBiz;

	/**
	 * Constructor.
	 *
	 * @param userAccountBiz
	 *        the user account biz
	 */
	public AccountRegistrationController(
			@Autowired(required = false) @Nullable UserAccountBiz userAccountBiz) {
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
	 * Submit the account registration.
	 *
	 * @param input
	 *        the form to save
	 * @return the new account
	 */
	@RequestMapping(value = "", method = RequestMethod.POST)
	public Result<SnAccountInfo> register(@Valid @RequestBody SnAccountCreationInput input) {
		var account = biz().createAccount(SecurityUtils.getCurrentActorUserId(), input);
		return success(account);
	}

}
