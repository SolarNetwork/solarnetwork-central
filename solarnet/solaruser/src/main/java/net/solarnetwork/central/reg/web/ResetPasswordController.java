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

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
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
 * @version 1.1
 */
@Controller
@RequestMapping("/u/resetPassword")
public class ResetPasswordController extends ControllerSupport {

	@Autowired
	private RegistrationBiz registrationBiz;

	@Autowired
	private MailService mailService;

	@Autowired
	private MessageSource messageSource;

	@Autowired
	private AuthenticationManager authenticationManager;

	@RequestMapping(value = "", method = RequestMethod.GET)
	public String home() {
		return "resetpass/start";
	}

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
			uriBuilder.queryParam("m", email);

			Map<String, Object> mailModel = new HashMap<String, Object>(2);
			mailModel.put("receipt", receipt);
			mailModel.put("url", uriBuilder.build().toUriString());

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

	@RequestMapping(value = "confirm", method = RequestMethod.GET)
	public ModelAndView confirmResetPassword(@RequestParam("c") String confirmationCode,
			@RequestParam("m") String email) {
		PasswordEntry form = new PasswordEntry();
		form.setConfirmationCode(confirmationCode);
		form.setUsername(email);
		return new ModelAndView("resetpass/confirm", "form", form);
	}

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

	public void setRegistrationBiz(RegistrationBiz registrationBiz) {
		this.registrationBiz = registrationBiz;
	}

	public void setMailService(MailService mailService) {
		this.mailService = mailService;
	}

	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	public void setAuthenticationManager(AuthenticationManager authenticationManager) {
		this.authenticationManager = authenticationManager;
	}

}
