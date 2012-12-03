/* ==================================================================
 * MyNodesController.java - Nov 22, 2012 7:25:44 AM
 * 
 * Copyright 2007-2012 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.reg.web;

import java.util.List;
import net.solarnetwork.central.security.SecurityUtils;
import net.solarnetwork.central.user.biz.AuthorizationException;
import net.solarnetwork.central.user.biz.RegistrationBiz;
import net.solarnetwork.central.user.biz.UserBiz;
import net.solarnetwork.central.user.domain.UserNode;
import net.solarnetwork.central.user.domain.UserNodeCertificate;
import net.solarnetwork.central.user.domain.UserNodeConfirmation;
import net.solarnetwork.domain.NetworkAssociation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

/**
 * Controller for "my nodes".
 * 
 * @author matt
 * @version $Revision$
 */
@Controller
@RequestMapping("/sec/my-nodes")
public class MyNodesController {

	public final UserBiz userBiz;
	public final RegistrationBiz registrationBiz;

	/**
	 * Constructor.
	 * 
	 * @param userBiz
	 *        the UserBiz
	 * @param registrationBiz
	 *        the RegistrationBiz
	 */
	@Autowired
	public MyNodesController(UserBiz userBiz, RegistrationBiz registrationBiz) {
		super();
		this.userBiz = userBiz;
		this.registrationBiz = registrationBiz;
	}

	/**
	 * View a "home" page for the "my nodes" section.
	 * 
	 * @return model and view
	 */
	@RequestMapping(value = "", method = RequestMethod.GET)
	public ModelAndView viewMyNodes() {
		List<UserNode> nodes = userBiz.getUserNodes(SecurityUtils.getCurrentUser().getUserId());
		List<UserNodeConfirmation> pendingConfirmationList = userBiz
				.getPendingUserNodeConfirmations(SecurityUtils.getCurrentUser().getUserId());
		ModelAndView mv = new ModelAndView("my-nodes/my-nodes");
		mv.addObject("userNodesList", nodes);
		mv.addObject("pendingUserNodeConfirmationsList", pendingConfirmationList);
		return mv;
	}

	/**
	 * Generate a new node confirmation code.
	 * 
	 * @param userId
	 *        the optional user ID to generate the code for; defaults to the
	 *        acting user
	 * @param securityPhrase
	 *        a security phrase to associate with the invitation
	 * @return model and view
	 */
	@RequestMapping("/new")
	public ModelAndView newNodeAssociation(
			@RequestParam(value = "userId", required = false) Long userId,
			@RequestParam("phrase") String securityPhrase) {
		if ( userId == null ) {
			userId = SecurityUtils.getCurrentUser().getUserId();
		}
		NetworkAssociation details = registrationBiz.createNodeAssociation(userId, securityPhrase);
		return new ModelAndView("my-nodes/invitation", "details", details);
	}

	@RequestMapping("/invitation")
	public ModelAndView viewConfirmation(@RequestParam(value = "id") Long userNodeConfirmationId) {
		NetworkAssociation details = registrationBiz.getNodeAssociation(userNodeConfirmationId);
		return new ModelAndView("my-nodes/invitation", "details", details);
	}

	@RequestMapping("/cancelInvitation")
	public String cancelConfirmation(@RequestParam(value = "id") Long userNodeConfirmationId) {
		registrationBiz.cancelNodeAssociation(userNodeConfirmationId);
		return "redirect:/u/sec/my-nodes";
	}

	/**
	 * Get a certificate, either as a {@link UserNodeCertificate} object or the
	 * PEM encoded value file attachment.
	 * 
	 * @param certId
	 *        the ID of the certificate to get
	 * @param download
	 *        if TRUE, then download the certificate as a PEM file
	 * @return the response data
	 */
	@RequestMapping("/cert")
	@ResponseBody
	public Object viewCert(@RequestParam("id") Long certId,
			@RequestParam(value = "download", required = false) Boolean download) {
		UserNodeCertificate cert = userBiz.getUserNodeCertificate(certId);
		if ( cert == null ) {
			throw new AuthorizationException(null, AuthorizationException.Reason.ACCESS_DENIED);
		}
		if ( !Boolean.TRUE.equals(download) ) {
			return cert;
		}

		String pem = cert.getPEMValue();

		HttpHeaders headers = new HttpHeaders();
		headers.setContentLength(pem.length());
		headers.setContentType(MediaType.parseMediaType("application/x-pem-file"));
		headers.setLastModified(System.currentTimeMillis());
		headers.setCacheControl("no-cache");

		// TODO: i18n report file name, create WebUtilities method to encode file name properly
		headers.set("Content-Disposition", "attachment; filename=solarnode-" + cert.getNode().getId()
				+ ".pem");

		return new ResponseEntity<String>(pem, headers, HttpStatus.OK);

	}

}
