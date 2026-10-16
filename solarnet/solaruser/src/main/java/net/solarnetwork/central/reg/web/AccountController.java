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
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import net.solarnetwork.central.security.SecurityUtils;
import net.solarnetwork.central.user.account.biz.UserAccountBiz;
import net.solarnetwork.central.user.account.domain.SnAccountInfo;

/**
 * Controller for the account pages.
 *
 * @author matt
 * @version 1.1
 */
@GlobalServiceController
public class AccountController {

	private final @Nullable UserAccountBiz userAccountBiz;
	private final MessageSource messageSource;

	/**
	 * Constructor.
	 *
	 * @param userAccountBiz
	 *        the user account biz
	 * @param messageSource
	 *        the message source to resolve the invoice consolidation problem
	 *        of the account with
	 * @throws IllegalArgumentException
	 *         if {@code messageSource} is {@code null}
	 */
	public AccountController(@Autowired(required = false) @Nullable UserAccountBiz userAccountBiz,
			MessageSource messageSource) {
		super();
		this.userAccountBiz = userAccountBiz;
		this.messageSource = requireNonNullArgument(messageSource, "messageSource");
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
	 * Provide the active user's Account entity, if available.
	 *
	 * @return the account entity for the active user, or {@code null} if none
	 *         available
	 */
	@ModelAttribute(binding = false, name = "account")
	public @Nullable SnAccountInfo account() {
		try {
			return biz().getAccountForUser(SecurityUtils.getCurrentActorUserId());
		} catch ( Exception e ) {
			return null;
		}
	}

	/**
	 * Render the account main page.
	 *
	 * <p>
	 * The problem with the invoice consolidation group of the account, if any,
	 * is provided as the {@code consolidationProblem} model attribute, resolved
	 * in the locale of the request.
	 * </p>
	 *
	 * @param model
	 *        the model
	 * @param locale
	 *        the request locale
	 * @return the main page reference
	 */
	@RequestMapping(value = "/u/sec/account", method = RequestMethod.GET)
	public String home(Model model, Locale locale) {
		if ( model.getAttribute("account") instanceof SnAccountInfo info ) {
			final MessageSourceResolvable problem = info.consolidationProblem();
			if ( problem != null ) {
				model.addAttribute("consolidationProblem", messageSource.getMessage(problem, locale));
			}
		}
		return "sec/account/account";
	}

}
