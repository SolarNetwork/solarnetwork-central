/* ==================================================================
 * NewNodeController.java - 15/10/2016 7:14:56 AM
 *
 * Copyright 2007-2016 SolarNetwork.net Dev Team
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.servlet.http.HttpServletRequest;
import net.solarnetwork.central.user.biz.RegistrationBiz;
import net.solarnetwork.domain.NetworkAssociationDetails;
import net.solarnetwork.domain.NetworkCertificate;
import net.solarnetwork.web.jakarta.support.WebUtils;

/**
 * Web controller for confirming node association.
 *
 * @version 1.1
 */
@Controller
public class NewNodeController extends ControllerSupport {

	/** The model key for the primary result object. */
	public static final String MODEL_KEY_RESULT = "result";

	/** The default view name. */
	public static final String DEFAULT_VIEW_NAME = "xml";

	private final RegistrationBiz registrationBiz;

	/**
	 * Constructor.
	 *
	 * @param regBiz
	 *        the RegistrationBiz to use
	 */
	@Autowired
	public NewNodeController(RegistrationBiz regBiz) {
		super();
		this.registrationBiz = regBiz;
	}

	@InitBinder
	public void initBinder(WebDataBinder binder) {
		binder.setIgnoreInvalidFields(true);
	}

	/**
	 * Confirm a node association
	 *
	 * @param request
	 *        the servlet request
	 * @param username
	 *        the username
	 * @param key
	 *        the confirmation key
	 * @param model
	 *        the model
	 * @return view name
	 */
	@RequestMapping(method = RequestMethod.POST, value = "/associate.*", params = { "username", "key" })
	public String confirmNodeAssociation(HttpServletRequest request,
			@RequestParam("username") String username, @RequestParam("key") String key, Model model) {
		NetworkAssociationDetails details = new NetworkAssociationDetails(username, key, null);
		return confirmNodeAssociation(request, details, model);
	}

	/**
	 * Confirm a node association
	 *
	 * @param request
	 *        the servlet request
	 * @param details
	 *        the association details
	 * @param model
	 *        the model
	 * @return view name
	 */
	@RequestMapping(method = RequestMethod.POST, value = "/associate.*",
			params = { "username", "confirmationKey" })
	public String confirmNodeAssociation(HttpServletRequest request, NetworkAssociationDetails details,
			Model model) {
		NetworkCertificate receipt = registrationBiz.confirmNodeAssociation(details);
		model.asMap().clear();
		model.addAttribute(MODEL_KEY_RESULT, receipt);
		return WebUtils.resolveViewFromUrlExtension(request, null);
	}

	/**
	 * Confirm a node association
	 *
	 * @param request
	 *        the servlet request
	 * @param details
	 *        the association details
	 * @param model
	 *        the model
	 * @return view name
	 * @since 1.1
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/cert.*",
			params = { "username", "confirmationKey", "keystorePassword" })
	public String getNodeCertificate(HttpServletRequest request, NetworkAssociationDetails details,
			Model model) {
		NetworkCertificate cert = registrationBiz.getNodeCertificate(details);
		model.asMap().clear();
		model.addAttribute(MODEL_KEY_RESULT, cert);
		return WebUtils.resolveViewFromUrlExtension(request, null);
	}

}
