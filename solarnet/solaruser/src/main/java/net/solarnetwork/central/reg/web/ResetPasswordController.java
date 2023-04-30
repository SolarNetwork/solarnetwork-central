/* ==================================================================
 * ResetPasswordController.java - Mar 19, 2013 6:34:43 AM
 * 
 * Copyright 2007-2013 SolarNetwork.net Dev Team
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

import static java.nio.charset.StandardCharsets.UTF_8;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.util.Base64;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.springframework.context.MessageSource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.UriComponentsBuilder;
import net.solarnetwork.central.mail.MailService;
import net.solarnetwork.central.mail.support.BasicMailAddress;
import net.solarnetwork.central.mail.support.ClasspathResourceMessageTemplateDataSource;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.security.SecurityUtils;
import net.solarnetwork.central.user.biz.RegistrationBiz;
import net.solarnetwork.central.user.domain.PasswordEntry;
import net.solarnetwork.domain.BasicRegistrationReceipt;
import net.solarnetwork.domain.RegistrationReceipt;

/**
 * Controller for managing the reset password functionality.
 * 
 * @author matt
 * @version 1.2
 */
@Controller
@RequestMapping("/u/resetPassword")
public class ResetPasswordController extends ControllerSupport {

	private final RegistrationBiz registrationBiz;
	private final MailService mailService;
	private final MessageSource messageSource;
	private final AuthenticationManager authenticationManager;

	/**
	 * Constructor.
	 * 
	 * @param registrationBiz
	 *        the registration service
	 * @param mailService
	 *        the mail service
	 * @param messageSource
	 *        the message source
	 * @param authenticationManager
	 *        the authentication manager
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public ResetPasswordController(RegistrationBiz registrationBiz, MailService mailService,
			MessageSource messageSource, AuthenticationManager authenticationManager) {
		super();
		this.registrationBiz = requireNonNullArgument(registrationBiz, "registrationBiz");
		this.mailService = requireNonNullArgument(mailService, "mailService");
		this.messageSource = requireNonNullArgument(messageSource, "messageSource");
		this.authenticationManager = requireNonNullArgument(authenticationManager,
				"authenticationManager");
	}

	/**
	 * View the reset password start form.
	 * 
	 * @return the view name
	 */
	@RequestMapping(value = "", method = RequestMethod.GET)
	public String home() {
		return "resetpass/start";
	}

	/**
	 * Generate an account reset email with a reset link.
	 * 
	 * @param email
	 *        the account email to generate a reset link for
	 * @param locale
	 *        the desired locale for the mail message
	 * @param uriBuilder
	 *        the application URI builder to generate the reset link with
	 * @return the view name
	 */
	@RequestMapping(value = "/generate", method = RequestMethod.POST)
	public ModelAndView generateResetCode(@RequestParam("email") String email, Locale locale,
			UriComponentsBuilder uriBuilder) {
		RegistrationReceipt receipt;
		try {
			receipt = registrationBiz.generateResetPasswordReceipt(email);

			// set up the confirmation URL
			uriBuilder.pathSegment("u", "resetPassword", "confirm");
			uriBuilder.replaceQuery(null);
			uriBuilder.queryParam("c", receipt.getConfirmationCode());

			// Base64 encode email to avoid issues with browsers decoding + character as space
			uriBuilder.queryParam("m", Base64.getUrlEncoder().encodeToString(email.getBytes(UTF_8)));

			Map<String, Object> mailModel = new HashMap<String, Object>(2);
			mailModel.put("receipt", receipt);
			mailModel.put("url", uriBuilder.build().encode().toUriString());

			mailService.sendMail(new BasicMailAddress(null, receipt.getUsername()),
					new ClasspathResourceMessageTemplateDataSource(locale,
							messageSource.getMessage("user.resetpassword.mail.subject", null, locale),
							"net/solarnetwork/central/reg/web/reset-password.txt", mailModel));
		} catch ( AuthorizationException e ) {
			// don't want to let anyone know about failures here... just pretend nothing happened
			log.info("Ignoring password reset request for {}: {}", email, e.getReason());
			receipt = new BasicRegistrationReceipt(email, "");
		}

		return new ModelAndView("resetpass/generated", "receipt", receipt);
	}

	/**
	 * Confirm a reset password link, and return a form view to set a new
	 * password.
	 * 
	 * @param confirmationCode
	 *        the confirmation code generated in
	 *        {@link #generateResetCode(String, Locale, UriComponentsBuilder)}
	 * @param emailBase64
	 *        the Base64-encoded email address being reset
	 * @return the view name
	 */
	@RequestMapping(value = "confirm", method = RequestMethod.GET)
	public ModelAndView confirmResetPassword(@RequestParam("c") String confirmationCode,
			@RequestParam("m") String emailBase64) {
		String email = new String(Base64.getDecoder().decode(emailBase64), UTF_8);
		PasswordEntry form = new PasswordEntry();
		form.setConfirmationCode(confirmationCode);
		form.setUsername(email);
		return new ModelAndView("resetpass/confirm", "form", form);
	}

	/**
	 * Reset an account password, login, and return the logged-in home view.
	 * 
	 * @param form
	 *        the reset form with the confirmation code and new password
	 *        details.
	 * @param req
	 *        the request
	 * @return the view name
	 */
	@RequestMapping(value = "reset", method = RequestMethod.POST)
	public ModelAndView resetPassword(PasswordEntry form, HttpServletRequest req) {
		try {
			registrationBiz.resetPassword(
					new BasicRegistrationReceipt(form.getUsername(), form.getConfirmationCode()), form);
		} catch ( AuthorizationException e ) {
			// go back to confirm
			ModelAndView result = new ModelAndView("resetpass/confirm", "form", form);
			result.addObject(WebConstants.MODEL_KEY_ERROR_MSG, "user.resetpassword.confirm.error");
			return result;
		}

		// automatically log the user in now, and then redirect to home
		SecurityUtils.authenticate(authenticationManager, form.getUsername(), form.getPassword());
		req.getSession().setAttribute(WebConstants.MODEL_KEY_STATUS_MSG,
				"user.resetpassword.reset.message");
		return new ModelAndView("redirect:/u/sec/home");
	}

}
