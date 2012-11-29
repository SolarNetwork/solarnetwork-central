/* ==================================================================
 * NetworkIdentityController.java - Sep 13, 2011 8:06:16 PM
 * 
 * Copyright 2007-2011 SolarNetwork.net Dev Team
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
 * $Id$
 * ==================================================================
 */

package net.solarnetwork.central.in.web;

import net.solarnetwork.central.in.biz.NetworkIdentityBiz;
import net.solarnetwork.domain.NetworkAssociation;
import net.solarnetwork.domain.NetworkAssociationDetails;
import net.solarnetwork.domain.NetworkIdentity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Controller for {@link NetworkIdentityBiz} requests.
 * 
 * @author matt
 * @version $Revision$
 */
@Controller
public class NetworkIdentityController {

	/** The default value for the {@code viewName} property. */
	public static final String DEFAULT_VIEW_NAME = "xml";

	private NetworkIdentityBiz networkIdentityBiz;
	private String viewName = DEFAULT_VIEW_NAME;

	//private final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * Constructor.
	 * 
	 * @param networkIdentityBiz
	 *        the {@link NetworkIdentityBiz} to use
	 */
	@Autowired
	public NetworkIdentityController(NetworkIdentityBiz networkIdentityBiz) {
		this.networkIdentityBiz = networkIdentityBiz;
	}

	/**
	 * Get the network identity, optionally as a {@link NetworkAssociation}.
	 * 
	 * <p>
	 * If both {@code username} and {@code confirmationKey} are non-null, then a
	 * {@link NetworkAssociation} will be returned in the model, rather than a
	 * plain {@link NetworkIdentity}.
	 * </p>
	 * 
	 * @param model
	 *        the model
	 * @param username
	 *        the optional network association username
	 * @param confirmationKey
	 *        the optional network association confirmation key
	 * @return the view name
	 */
	@RequestMapping(value = "/identity.do", method = RequestMethod.GET)
	public String getNetworkIdentityKey(Model model,
			@RequestParam(value = "username", required = false) String username,
			@RequestParam(value = "key", required = false) String confirmationKey) {
		NetworkIdentity ident = networkIdentityBiz.getNetworkIdentity();
		if ( username != null && confirmationKey != null ) {
			NetworkAssociation association = networkIdentityBiz.getNetworkAssociation(username,
					confirmationKey);
			if ( association != null ) {
				NetworkAssociationDetails details = new NetworkAssociationDetails(association);
				details.setHost(ident.getHost());
				details.setIdentityKey(ident.getIdentityKey());
				details.setPort(ident.getPort());
				details.setTermsOfService(ident.getTermsOfService());
				details.setForceTLS(ident.isForceTLS());
				ident = details;
			}
		}
		model.addAttribute(ident);
		return viewName;
	}

	public NetworkIdentityBiz getNetworkIdentityBiz() {
		return networkIdentityBiz;
	}

	public void setNetworkIdentityBiz(NetworkIdentityBiz networkIdentityBiz) {
		this.networkIdentityBiz = networkIdentityBiz;
	}

	public String getViewName() {
		return viewName;
	}

	public void setViewName(String viewName) {
		this.viewName = viewName;
	}

}
