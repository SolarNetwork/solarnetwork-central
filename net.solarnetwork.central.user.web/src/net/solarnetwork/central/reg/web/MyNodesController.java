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

import static net.solarnetwork.web.domain.Response.response;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.security.SecurityUser;
import net.solarnetwork.central.security.SecurityUtils;
import net.solarnetwork.central.user.biz.RegistrationBiz;
import net.solarnetwork.central.user.biz.UserBiz;
import net.solarnetwork.central.user.domain.NewNodeRequest;
import net.solarnetwork.central.user.domain.UserNode;
import net.solarnetwork.central.user.domain.UserNodeCertificate;
import net.solarnetwork.central.user.domain.UserNodeConfirmation;
import net.solarnetwork.domain.NetworkAssociation;
import net.solarnetwork.support.CertificateService;
import net.solarnetwork.web.domain.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.PathVariable;
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
public class MyNodesController extends ControllerSupport {

	public final UserBiz userBiz;
	public final RegistrationBiz registrationBiz;
	public final CertificateService certificateService;

	/**
	 * Constructor.
	 * 
	 * @param userBiz
	 *        the UserBiz
	 * @param registrationBiz
	 *        the RegistrationBiz
	 */
	@Autowired
	public MyNodesController(UserBiz userBiz, RegistrationBiz registrationBiz,
			CertificateService certificateService) {
		super();
		this.userBiz = userBiz;
		this.registrationBiz = registrationBiz;
		this.certificateService = certificateService;
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
	 * @param timeZoneName
	 *        the time zone to associate the node with
	 * @param country
	 *        the country to associate the node with
	 * @return model and view
	 */
	@RequestMapping("/new")
	public ModelAndView newNodeAssociation(
			@RequestParam(value = "userId", required = false) Long userId,
			@RequestParam("phrase") String securityPhrase,
			@RequestParam("timeZone") String timeZoneName, @RequestParam("country") String countryCode) {
		if ( userId == null ) {
			userId = SecurityUtils.getCurrentUser().getUserId();
		}
		final TimeZone timeZone = TimeZone.getTimeZone(timeZoneName);
		String lang = "en";
		for ( Locale locale : Locale.getAvailableLocales() ) {
			if ( locale.getCountry().equals(countryCode) ) {
				lang = locale.getLanguage();
			}
		}
		final Locale locale = new Locale(lang, countryCode);
		final NetworkAssociation details = registrationBiz.createNodeAssociation(new NewNodeRequest(
				userId, securityPhrase, timeZone, locale));
		return new ModelAndView("my-nodes/invitation", "details", details);
	}

	@RequestMapping("/tzpicker.html")
	public String tzpicker() {
		return "tzpicker-500";
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
	@RequestMapping("/cert/{nodeId}")
	@ResponseBody
	public Object viewCert(@PathVariable("nodeId") Long nodeId,
			@RequestParam(value = "password", required = false) String password,
			@RequestParam(value = "download", required = false) Boolean download) {
		SecurityUser actor = SecurityUtils.getCurrentUser();
		UserNodeCertificate cert = userBiz.getUserNodeCertificate(actor.getUserId(), nodeId);
		if ( cert == null ) {
			throw new AuthorizationException(AuthorizationException.Reason.ACCESS_DENIED, nodeId);
		}

		final byte[] data = cert.getKeystoreData();

		if ( !Boolean.TRUE.equals(download) ) {
			String pkcs7 = "";
			if ( data != null ) {
				KeyStore keystore = cert.getKeyStore(password);
				X509Certificate[] chain = cert.getNodeCertificateChain(keystore);
				pkcs7 = certificateService.generatePKCS7CertificateChainString(chain);
			}
			return new UserNodeCertificateDecoded(cert, pkcs7);
		}

		HttpHeaders headers = new HttpHeaders();
		headers.setContentLength(data.length);
		headers.setContentType(MediaType.parseMediaType("application/x-pkcs12"));
		headers.setLastModified(System.currentTimeMillis());
		headers.setCacheControl("no-cache");

		headers.set("Content-Disposition", "attachment; filename=solarnode-" + cert.getNode().getId()
				+ ".p12");

		return new ResponseEntity<byte[]>(data, headers, HttpStatus.OK);
	}

	public static class UserNodeCertificateDecoded extends UserNodeCertificate {

		private static final long serialVersionUID = 4591637182934678849L;

		private final String pemValue;

		private UserNodeCertificateDecoded(UserNodeCertificate cert, String pkcs7) {
			super();
			setCreated(cert.getCreated());
			setId(cert.getId());
			setNodeId(cert.getNodeId());
			setRequestId(cert.getRequestId());
			setUserId(cert.getUserId());
			this.pemValue = pkcs7;
		}

		public String getPemValue() {
			return pemValue;
		}

	}

	@RequestMapping(value = "/editNode", method = RequestMethod.GET)
	public String editNodeView(@RequestParam("userId") Long userId, @RequestParam("nodeId") Long nodeId,
			Model model) {
		model.addAttribute("userNode", userBiz.getUserNode(userId, nodeId));
		return "my-nodes/edit-node";
	}

	@ResponseBody
	@RequestMapping(value = "/node", method = RequestMethod.GET)
	public Response<UserNode> getUserNode(@RequestParam("userId") Long userId,
			@RequestParam("nodeId") Long nodeId) {
		return response(userBiz.getUserNode(userId, nodeId));
	}

	@ResponseBody
	@RequestMapping(value = "/updateNode", method = RequestMethod.POST)
	public UserNode editNodeSave(UserNode userNode, Errors userNodeErrors, Model model) {
		return userBiz.saveUserNode(userNode);
	}

}
