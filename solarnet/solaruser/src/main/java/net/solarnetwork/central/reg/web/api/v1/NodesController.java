/* ==================================================================
 * NodesController.java - Jan 23, 2015 3:42:36 PM
 * 
 * Copyright 2007-2015 SolarNetwork.net Dev Team
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

import static net.solarnetwork.web.jakarta.domain.Response.response;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.Period;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import net.solarnetwork.central.RepeatableTaskException;
import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.security.SecurityUtils;
import net.solarnetwork.central.support.BasicFilterResults;
import net.solarnetwork.central.user.biz.RegistrationBiz;
import net.solarnetwork.central.user.biz.UserBiz;
import net.solarnetwork.central.user.domain.NewNodeRequest;
import net.solarnetwork.central.user.domain.UserNode;
import net.solarnetwork.central.user.domain.UserNodeCertificate;
import net.solarnetwork.central.user.domain.UserNodeCertificateInstallationStatus;
import net.solarnetwork.central.user.domain.UserNodeCertificateRenewal;
import net.solarnetwork.central.user.domain.UserNodeConfirmation;
import net.solarnetwork.central.web.GlobalExceptionRestController;
import net.solarnetwork.domain.NetworkCertificate;
import net.solarnetwork.service.CertificateException;
import net.solarnetwork.service.CertificateService;
import net.solarnetwork.web.jakarta.domain.Response;

/**
 * Controller for user nodes web service API.
 * 
 * @author matt
 * @version 2.1
 */
@GlobalExceptionRestController
@Controller("v1nodesController") // note no @RequestMapping because of getMyNodes() same path with MyNodesController
public class NodesController {

	private final Logger log = LoggerFactory.getLogger(NodesController.class);

	private final UserBiz userBiz;
	private final CertificateService certificateService;
	private final RegistrationBiz registrationBiz;

	/**
	 * Constructor.
	 * 
	 * @param userBiz
	 *        The {@link UserBiz}.
	 * @param certificateService
	 *        The {@link CertificateService}.
	 * @param registrationBiz
	 *        the {@link RegistrationBiz}
	 */
	@Autowired
	public NodesController(UserBiz userBiz, CertificateService certificateService,
			RegistrationBiz registrationBiz) {
		super();
		this.userBiz = userBiz;
		this.certificateService = certificateService;
		this.registrationBiz = registrationBiz;
	}

	/**
	 * Get a listing of nodes for the active user.
	 * 
	 * @return The list of nodes available to the active user.
	 */
	@RequestMapping(value = { "/api/v1/sec/nodes", "/api/v1/sec/nodes/" }, method = RequestMethod.GET)
	@ResponseBody
	public Response<FilterResults<UserNode>> getMyNodes() {
		List<UserNode> nodes = userBiz.getUserNodes(SecurityUtils.getCurrentActorUserId());
		FilterResults<UserNode> result = new BasicFilterResults<UserNode>(nodes, (long) nodes.size(), 0,
				nodes.size());
		return response(result);
	}

	/**
	 * Get a listing of pending node confirmations for the active user.
	 * 
	 * @return The list of pending node confirmations for the active user.
	 */
	@RequestMapping(value = { "/u/sec/my-nodes/pending",
			"/api/v1/sec/nodes/pending" }, method = RequestMethod.GET)
	@ResponseBody
	public Response<FilterResults<UserNodeConfirmation>> getPendingNodes() {
		List<UserNodeConfirmation> pending = userBiz
				.getPendingUserNodeConfirmations(SecurityUtils.getCurrentActorUserId());
		FilterResults<UserNodeConfirmation> result = new BasicFilterResults<UserNodeConfirmation>(
				pending, (long) pending.size(), 0, pending.size());
		return response(result);
	}

	/**
	 * Get a list of all archived nodes.
	 * 
	 * @return All archived nodes.
	 * @since 1.1
	 */
	@RequestMapping(value = { "/u/sec/my-nodes/archived",
			"/api/v1/sec/nodes/archived" }, method = RequestMethod.GET)
	@ResponseBody
	public Response<List<UserNode>> getArchivedNodes() {
		List<UserNode> nodes = userBiz.getArchivedUserNodes(SecurityUtils.getCurrentActorUserId());
		return response(nodes);
	}

	/**
	 * Update the archived status of a set of nodes.
	 * 
	 * @param nodeIds
	 *        The node IDs to update the archived status of.
	 * @param archived
	 *        {@code true} to archive, {@code false} to un-archive
	 * @return A success response.
	 * @since 1.1
	 */
	@RequestMapping(value = { "/u/sec/my-nodes/archived",
			"/api/v1/sec/nodes/archived" }, method = RequestMethod.POST)
	@ResponseBody
	public Response<Object> updateArchivedStatus(@RequestParam("nodeIds") Long[] nodeIds,
			@RequestParam("archived") boolean archived) {
		userBiz.updateUserNodeArchivedStatus(SecurityUtils.getCurrentActorUserId(), nodeIds, archived);
		return response(null);
	}

	/**
	 * Manually create a new node, without going through the
	 * invitation/association process.
	 * 
	 * @param timeZoneId
	 *        the time zone ID
	 * @param countryCode
	 *        the country code
	 * @param keystorePassword
	 *        the password to use for the certificate store
	 * @return the new node details
	 * @since 1.2
	 */
	@RequestMapping(value = { "/u/sec/my-nodes/create-cert",
			"/api/v1/sec/nodes/create-cert" }, method = RequestMethod.POST)
	@ResponseBody
	public Response<UserNode> manuallyCreateNode(@RequestParam("timeZone") String timeZoneId,
			@RequestParam("country") String countryCode,
			@RequestParam("keystorePassword") String keystorePassword) {
		String lang = "en";
		for ( Locale locale : Locale.getAvailableLocales() ) {
			if ( locale.getCountry().equals(countryCode) ) {
				lang = locale.getLanguage();
			}
		}
		final Locale locale = new Locale(lang, countryCode);
		final TimeZone timeZone = TimeZone.getTimeZone(timeZoneId);
		NewNodeRequest req = new NewNodeRequest(SecurityUtils.getCurrentActorUserId(), keystorePassword,
				timeZone, locale);
		return response(registrationBiz.createNodeManually(req));
	}

	/**
	 * Handle an {@link CertificateException}.
	 * 
	 * @param e
	 *        the exception
	 * @return an error response object
	 */
	@ExceptionHandler(CertificateException.class)
	@ResponseBody
	@ResponseStatus(code = HttpStatus.FORBIDDEN)
	public Response<?> handleAuthorizationException(CertificateException e) {
		log.debug("CertificateException in {} controller: {}", getClass().getSimpleName(),
				e.getMessage());
		return new Response<Object>(Boolean.FALSE, null, e.getMessage(), null);
	}

	/**
	 * Get a certificate as a PEM encoded value file attachment.
	 * 
	 * @param nodeId
	 *        the ID of the node to get the certificate for
	 * @return the response data
	 * @since 1.2
	 */
	@RequestMapping(value = { "/u/sec/my-nodes/cert/{nodeId}",
			"/api/v1/sec/nodes/cert/{nodeId}" }, method = RequestMethod.GET)
	@ResponseBody
	public ResponseEntity<byte[]> viewCert(@PathVariable("nodeId") Long nodeId) {
		UserNodeCertificate cert = userBiz.getUserNodeCertificate(SecurityUtils.getCurrentActorUserId(),
				nodeId);
		if ( cert == null ) {
			throw new AuthorizationException(AuthorizationException.Reason.ACCESS_DENIED, nodeId);
		}

		final byte[] data = cert.getKeystoreData();

		HttpHeaders headers = new HttpHeaders();
		headers.setContentLength(data.length);
		headers.setContentType(MediaType.parseMediaType("application/x-pkcs12"));
		headers.setLastModified(System.currentTimeMillis());
		headers.setCacheControl("no-cache");

		headers.set("Content-Disposition",
				"attachment; filename=solarnode-" + cert.getNode().getId() + ".p12");

		return new ResponseEntity<byte[]>(data, headers, HttpStatus.OK);
	}

	/**
	 * Get a certificate as a {@link UserNodeCertificate} object.
	 * 
	 * @param nodeId
	 *        the ID of the node to get the certificate for
	 * @param password
	 *        the password to decrypt the certificate store with
	 * @return the response data
	 * @since 1.2
	 */
	@RequestMapping(value = { "/u/sec/my-nodes/cert/{nodeId}",
			"/api/v1/sec/nodes/cert/{nodeId}" }, method = RequestMethod.POST)
	@ResponseBody
	public UserNodeCertificate viewCert(@PathVariable("nodeId") Long nodeId,
			@RequestParam(value = "password") String password) {
		UserNodeCertificate cert = userBiz.getUserNodeCertificate(SecurityUtils.getCurrentActorUserId(),
				nodeId);
		if ( cert == null ) {
			throw new AuthorizationException(AuthorizationException.Reason.ACCESS_DENIED, nodeId);
		}

		final byte[] data = cert.getKeystoreData();

		// see if a renewal is pending
		UserNodeCertificateInstallationStatus installationStatus = null;
		if ( cert.getRequestId() != null ) {
			UserNode userNode = new UserNode(cert.getUser(), cert.getNode());
			UserNodeCertificateRenewal renewal = registrationBiz
					.getPendingNodeCertificateRenewal(userNode, cert.getRequestId());
			if ( renewal != null ) {
				installationStatus = renewal.getInstallationStatus();
			}
		}

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
		return new UserNodeCertificateDecoded(cert, installationStatus, nodeCert, pkcs7,
				registrationBiz.getNodeCertificateRenewalPeriod());
	}

	/**
	 * Renew a certificate.
	 * 
	 * @param nodeId
	 *        the node ID
	 * @param password
	 *        the private key password
	 * @return the renewed certificate
	 * @since 1.2
	 */
	@RequestMapping(value = { "/u/sec/my-nodes/cert/renew/{nodeId}",
			"/api/v1/sec/nodes/cert/renew/{nodeId}" }, method = RequestMethod.POST)
	@ResponseBody
	public UserNodeCertificate renewCert(@PathVariable("nodeId") final Long nodeId,
			@RequestParam("password") final String password) {
		UserNode userNode = userBiz.getUserNode(SecurityUtils.getCurrentActorUserId(), nodeId);
		if ( userNode == null ) {
			throw new AuthorizationException(AuthorizationException.Reason.ACCESS_DENIED, nodeId);
		}
		NetworkCertificate renewed = registrationBiz.renewNodeCertificate(userNode, password);
		if ( renewed != null && renewed.getNetworkCertificate() != null ) {
			return viewCert(nodeId, password);
		}
		throw new RepeatableTaskException("Certificate renewal processing");
	}

	public static class UserNodeCertificateDecoded extends UserNodeCertificate {

		private static final long serialVersionUID = -2314002517991208690L;

		private final UserNodeCertificateInstallationStatus installationStatus;
		private final String pemValue;
		private final X509Certificate nodeCert;
		private final Instant renewAfter;

		private UserNodeCertificateDecoded(UserNodeCertificate cert,
				UserNodeCertificateInstallationStatus installationStatus, X509Certificate nodeCert,
				String pkcs7, Period renewPeriod) {
			super();
			setCreated(cert.getCreated());
			setId(cert.getId());
			setNodeId(cert.getNodeId());
			setRequestId(cert.getRequestId());
			setUserId(cert.getUserId());
			this.installationStatus = installationStatus;
			this.pemValue = pkcs7;
			this.nodeCert = nodeCert;
			if ( nodeCert != null ) {
				if ( renewPeriod != null ) {
					this.renewAfter = nodeCert.getNotAfter().toInstant().atZone(ZoneOffset.UTC)
							.minus(renewPeriod).toInstant();
				} else {
					this.renewAfter = null;
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
		public Instant getCertificateValidFromDate() {
			return (nodeCert != null ? nodeCert.getNotBefore().toInstant() : null);
		}

		/**
		 * Get the date the certificate is valid until.
		 * 
		 * @return The valid until date.
		 */
		public Instant getCertificateValidUntilDate() {
			return (nodeCert != null ? nodeCert.getNotAfter().toInstant() : null);
		}

		/**
		 * Get the certificate subject DN.
		 * 
		 * @return The certificate subject DN.
		 */
		public String getCertificateSubjectDN() {
			return (nodeCert != null ? nodeCert.getSubjectX500Principal().getName() : null);
		}

		/**
		 * Get the certificate issuer DN.
		 * 
		 * @return The certificate issuer DN.
		 */
		public String getCertificateIssuerDN() {
			return (nodeCert != null ? nodeCert.getIssuerX500Principal().getName() : null);
		}

		/**
		 * Get a date after which the certificate may be renewed.
		 * 
		 * @return A renewal minimum date.
		 */
		public Instant getCertificateRenewAfterDate() {
			return renewAfter;
		}

		/**
		 * Get the status of the installation process, if available.
		 * 
		 * @return The installation status, or <em>null</em>.
		 */
		public UserNodeCertificateInstallationStatus getInstallationStatus() {
			return installationStatus;
		}

	}
}
