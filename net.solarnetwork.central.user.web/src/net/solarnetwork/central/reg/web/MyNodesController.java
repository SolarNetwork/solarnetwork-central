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
 */

package net.solarnetwork.central.reg.web;

import static net.solarnetwork.web.domain.Response.response;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import javax.servlet.http.HttpServletResponse;
import net.solarnetwork.central.RepeatableTaskException;
import net.solarnetwork.central.mail.MailService;
import net.solarnetwork.central.mail.support.BasicMailAddress;
import net.solarnetwork.central.mail.support.ClasspathResourceMessageTemplateDataSource;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.security.SecurityUser;
import net.solarnetwork.central.security.SecurityUtils;
import net.solarnetwork.central.user.biz.NodeOwnershipBiz;
import net.solarnetwork.central.user.biz.RegistrationBiz;
import net.solarnetwork.central.user.biz.UserBiz;
import net.solarnetwork.central.user.domain.NewNodeRequest;
import net.solarnetwork.central.user.domain.User;
import net.solarnetwork.central.user.domain.UserAlertStatus;
import net.solarnetwork.central.user.domain.UserAlertType;
import net.solarnetwork.central.user.domain.UserNode;
import net.solarnetwork.central.user.domain.UserNodeCertificate;
import net.solarnetwork.central.user.domain.UserNodeConfirmation;
import net.solarnetwork.central.user.domain.UserNodeTransfer;
import net.solarnetwork.domain.NetworkAssociation;
import net.solarnetwork.domain.NetworkCertificate;
import net.solarnetwork.support.CertificateException;
import net.solarnetwork.support.CertificateService;
import net.solarnetwork.web.domain.Response;
import org.joda.time.DateTime;
import org.joda.time.ReadablePeriod;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Controller for "my nodes".
 * 
 * @author matt
 * @version 1.2
 */
@Controller
@RequestMapping("/sec/my-nodes")
public class MyNodesController extends ControllerSupport {

	private final UserBiz userBiz;
	private final RegistrationBiz registrationBiz;
	private final NodeOwnershipBiz nodeOwnershipBiz;
	private final CertificateService certificateService;

	@Autowired(required = false)
	private MailService mailService;

	@Autowired
	private MessageSource messageSource;

	/**
	 * Constructor.
	 * 
	 * @param userBiz
	 *        The {@link UserBiz} to use.
	 * @param registrationBiz
	 *        The {@link RegistrationBiz} to use.
	 * @param nodeOwnershipBiz
	 *        the {@link NodeOwnershipBiz} to use.
	 * @param certificateService
	 *        The {@link CertificateService} to use.
	 */
	@Autowired
	public MyNodesController(UserBiz userBiz, RegistrationBiz registrationBiz,
			NodeOwnershipBiz nodeOwnershipBiz, CertificateService certificateService) {
		super();
		this.userBiz = userBiz;
		this.registrationBiz = registrationBiz;
		this.certificateService = certificateService;
		this.nodeOwnershipBiz = nodeOwnershipBiz;
	}

	/**
	 * Set a {@link MailService} to use.
	 * 
	 * @param mailService
	 *        The service to use.
	 */
	public void setMailService(MailService mailService) {
		this.mailService = mailService;
	}

	/**
	 * The {@link MessageSource} to use in conjunction with
	 * {@link #setMailService(MailService)}.
	 * 
	 * @param messageSource
	 *        A message source to use.
	 */
	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	@ModelAttribute("nodeDataAlertTypes")
	public List<UserAlertType> nodeDataAlertTypes() {
		// now now, only one alert type!
		return Collections.singletonList(UserAlertType.NodeStaleData);
	}

	@ModelAttribute("alertStatuses")
	public UserAlertStatus[] alertStatuses() {
		return UserAlertStatus.values();
	}

	/**
	 * View a "home" page for the "my nodes" section.
	 * 
	 * @return model and view
	 */
	@RequestMapping(value = "", method = RequestMethod.GET)
	public ModelAndView viewMyNodes() {
		final SecurityUser actor = SecurityUtils.getCurrentUser();
		List<UserNode> nodes = userBiz.getUserNodes(SecurityUtils.getCurrentUser().getUserId());

		// move any nodes with pending transfer into own list
		List<UserNode> pendingTransferNodes = new ArrayList<UserNode>(nodes == null ? 0 : nodes.size());
		if ( nodes != null ) {
			for ( Iterator<UserNode> itr = nodes.iterator(); itr.hasNext(); ) {
				UserNode node = itr.next();
				if ( node.getTransfer() != null ) {
					itr.remove();
					pendingTransferNodes.add(node);
				}
			}
		}

		List<UserNodeConfirmation> pendingConfirmationList = userBiz
				.getPendingUserNodeConfirmations(actor.getUserId());
		List<UserNodeTransfer> pendingNodeOwnershipRequests = nodeOwnershipBiz
				.pendingNodeOwnershipTransfersForEmail(actor.getEmail());
		ModelAndView mv = new ModelAndView("my-nodes/my-nodes");
		mv.addObject("userNodesList", nodes);
		mv.addObject("pendingUserNodeConfirmationsList", pendingConfirmationList);
		mv.addObject("pendingUserNodeTransferList", pendingTransferNodes);
		mv.addObject("pendingNodeOwnershipRequests", pendingNodeOwnershipRequests);
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
			X509Certificate nodeCert = null;
			if ( data != null ) {
				KeyStore keystore = cert.getKeyStore(password);
				X509Certificate[] chain = cert.getNodeCertificateChain(keystore);
				if ( chain != null && chain.length > 0 ) {
					nodeCert = chain[0];
				}
				pkcs7 = certificateService.generatePKCS7CertificateChainString(chain);
			}
			return new UserNodeCertificateDecoded(cert, nodeCert, pkcs7,
					registrationBiz.getNodeCertificateRenewalPeriod());
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

	/**
	 * AuthorizationException handler.
	 * 
	 * <p>
	 * Logs a WARN log and returns HTTP 403 (Forbidden).
	 * </p>
	 * 
	 * @param e
	 *        the exception
	 * @param res
	 *        the servlet response
	 */
	@ExceptionHandler(CertificateException.class)
	public void handleCertificateException(CertificateException e, HttpServletResponse res) {
		if ( log.isWarnEnabled() ) {
			log.warn("Certificate exception: " + e.getMessage());
		}
		res.setStatus(HttpServletResponse.SC_FORBIDDEN);
	}

	@RequestMapping(value = "/cert/renew/{nodeId}", method = RequestMethod.POST)
	@ResponseBody
	public Object renewCert(@PathVariable("nodeId") final Long nodeId,
			@RequestParam("password") final String password) {
		SecurityUser actor = SecurityUtils.getCurrentUser();
		UserNode userNode = userBiz.getUserNode(actor.getUserId(), nodeId);
		if ( userNode == null ) {
			throw new AuthorizationException(AuthorizationException.Reason.ACCESS_DENIED, nodeId);
		}
		NetworkCertificate renewed = registrationBiz.renewNodeCertificate(userNode, password);
		if ( renewed != null && renewed.getNetworkCertificate() != null ) {
			return viewCert(nodeId, password, Boolean.FALSE);
		}
		throw new RepeatableTaskException("Certificate renewal processing");
	}

	public static class UserNodeCertificateDecoded extends UserNodeCertificate {

		private static final long serialVersionUID = -2314002517991208690L;

		private final String pemValue;
		private final X509Certificate nodeCert;
		private final DateTime renewAfter;

		private UserNodeCertificateDecoded(UserNodeCertificate cert, X509Certificate nodeCert,
				String pkcs7, ReadablePeriod renewPeriod) {
			super();
			setCreated(cert.getCreated());
			setId(cert.getId());
			setNodeId(cert.getNodeId());
			setRequestId(cert.getRequestId());
			setUserId(cert.getUserId());
			this.pemValue = pkcs7;
			this.nodeCert = nodeCert;
			if ( nodeCert != null ) {
				if ( renewPeriod != null ) {
					this.renewAfter = new DateTime(nodeCert.getNotAfter()).minus(renewPeriod);
				} else {
					this.renewAfter = new DateTime();
				}
			} else {
				this.renewAfter = null;
			}
		}

		public String getPemValue() {
			return pemValue;
		}

		/**
		 * Get a hexidecimal string value of the certificate serial number.
		 * 
		 * @return The certificate serial number.
		 */
		public String getCertificateSerialNumber() {
			return (nodeCert != null ? "0x" + nodeCert.getSerialNumber().toString(16) : null);
		}

		/**
		 * Get the date the certificate is valid from.
		 * 
		 * @return The valid from date.
		 */
		public DateTime getCertificateValidFromDate() {
			return (nodeCert != null ? new DateTime(nodeCert.getNotBefore()) : null);
		}

		/**
		 * Get the date the certificate is valid until.
		 * 
		 * @return The valid until date.
		 */
		public DateTime getCertificateValidUntilDate() {
			return (nodeCert != null ? new DateTime(nodeCert.getNotAfter()) : null);
		}

		/**
		 * Get the certificate subject DN.
		 * 
		 * @return The certificate subject DN.
		 */
		public String getCertificateSubjectDN() {
			return (nodeCert != null ? nodeCert.getSubjectDN().getName() : null);
		}

		/**
		 * Get the certificate issuer DN.
		 * 
		 * @return The certificate issuer DN.
		 */
		public String getCertificateIssuerDN() {
			return (nodeCert != null ? nodeCert.getIssuerDN().getName() : null);
		}

		/**
		 * Get a date after which the certificate may be renewed.
		 * 
		 * @return A renewal minimum date.
		 */
		public DateTime getCertificateRenewAfterDate() {
			return renewAfter;
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

	/**
	 * Request an ownership transfer of a node to another SolarNetwork account.
	 * 
	 * @param userId
	 *        The user ID of the current node owner.
	 * @param nodeId
	 *        The ID of the node to transfer ownership of.
	 * @param email
	 *        The recipient of the node ownership request.
	 * @param locale
	 *        The request locale to use in the generated email content.
	 * @param uriBuilder
	 *        A URI builder to assist in the generated email content.
	 * @return A {@code TRUE} value on success.
	 */
	@ResponseBody
	@RequestMapping(value = "/requestNodeTransfer", method = RequestMethod.POST)
	public Response<Boolean> requestNodeOwnershipTransfer(@RequestParam("userId") Long userId,
			@RequestParam("nodeId") Long nodeId, @RequestParam("recipient") String email, Locale locale,
			UriComponentsBuilder uriBuilder) {
		nodeOwnershipBiz.requestNodeOwnershipTransfer(userId, nodeId, email);
		if ( mailService != null ) {
			try {
				User actor = userBiz.getUser(SecurityUtils.getCurrentActorUserId());

				uriBuilder.pathSegment("sec", "my-nodes");

				Map<String, Object> mailModel = new HashMap<String, Object>(2);
				mailModel.put("actor", actor);
				mailModel.put("recipient", email);
				mailModel.put("nodeId", nodeId);
				mailModel.put("url", uriBuilder.build().toUriString());

				mailService.sendMail(
						new BasicMailAddress(null, email),
						new ClasspathResourceMessageTemplateDataSource(locale, messageSource.getMessage(
								"my-nodes.transferOwnership.mail.subject", null, locale),
								"/net/solarnetwork/central/reg/web/transfer-ownership.txt", mailModel));
			} catch ( RuntimeException e ) {
				// ignore this other than log
				log.warn("Error sending ownership transfer mail message to {}: {}", email,
						e.getMessage(), e);
			}
		}
		return response(Boolean.TRUE);
	}

	@ResponseBody
	@RequestMapping(value = "/cancelNodeTransferRequest", method = RequestMethod.POST)
	public Response<Boolean> cancelNodeOwnershipTransfer(@RequestParam("userId") Long userId,
			@RequestParam("nodeId") Long nodeId, Locale locale) {
		UserNodeTransfer xfer = nodeOwnershipBiz.getNodeOwnershipTransfer(userId, nodeId);
		if ( xfer != null ) {
			nodeOwnershipBiz.cancelNodeOwnershipTransfer(userId, nodeId);
			if ( mailService != null ) {
				// notify the recipient about the cancellation
				try {
					User actor = userBiz.getUser(SecurityUtils.getCurrentActorUserId());

					Map<String, Object> mailModel = new HashMap<String, Object>(2);
					mailModel.put("actor", actor);
					mailModel.put("transfer", xfer);

					mailService
							.sendMail(
									new BasicMailAddress(null, xfer.getEmail()),
									new ClasspathResourceMessageTemplateDataSource(
											locale,
											messageSource.getMessage(
													"my-nodes.transferOwnership.mail.subject.cancelled",
													null, locale),
											"/net/solarnetwork/central/reg/web/transfer-ownership-cancelled.txt",
											mailModel));
				} catch ( RuntimeException e ) {
					// ignore this other than log
					log.warn("Error sending ownership transfer mail message to {}: {}", xfer.getEmail(),
							e.getMessage(), e);
				}
			}
		}
		return response(Boolean.TRUE);
	}

	@ResponseBody
	@RequestMapping(value = "/confirmNodeTransferRequest", method = RequestMethod.POST)
	public Response<Boolean> confirmNodeOwnershipTransfer(@RequestParam("userId") Long userId,
			@RequestParam("nodeId") Long nodeId, @RequestParam("accept") boolean accept, Locale locale) {
		UserNodeTransfer xfer = nodeOwnershipBiz.confirmNodeOwnershipTransfer(userId, nodeId, accept);
		if ( xfer != null ) {
			if ( mailService != null ) {
				// notify the recipient about the cancellation
				try {
					User actor = userBiz.getUser(SecurityUtils.getCurrentActorUserId());

					Map<String, Object> mailModel = new HashMap<String, Object>(2);
					mailModel.put("actor", actor);
					mailModel.put("transfer", xfer);

					mailService
							.sendMail(
									new BasicMailAddress(null, xfer.getUser().getEmail()),
									new ClasspathResourceMessageTemplateDataSource(
											locale,
											messageSource
													.getMessage(
															("my-nodes.transferOwnership.mail.subject." + (accept ? "accepted"
																	: "declined")), null, locale),
											("/net/solarnetwork/central/reg/web/transfer-ownership-"
													+ (accept ? "accepted" : "declined") + ".txt"),
											mailModel));
				} catch ( RuntimeException e ) {
					// ignore this other than log
					log.warn("Error sending ownership transfer mail message to {}: {}", xfer.getEmail(),
							e.getMessage(), e);
				}
			}
		}
		return response(Boolean.TRUE);
	}

}
