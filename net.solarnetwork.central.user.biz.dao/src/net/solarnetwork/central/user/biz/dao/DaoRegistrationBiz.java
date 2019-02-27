/* ==================================================================
 * DaoRegistrationBiz.java - Dec 18, 2009 4:16:11 PM
 * 
 * Copyright 2007-2009 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.biz.dao;

import static net.solarnetwork.central.user.biz.dao.UserBizConstants.getOriginalEmail;
import static net.solarnetwork.central.user.biz.dao.UserBizConstants.getUnconfirmedEmail;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.zip.GZIPOutputStream;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Base64OutputStream;
import org.apache.commons.codec.digest.DigestUtils;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.solarnetwork.central.ValidationException;
import net.solarnetwork.central.dao.SolarLocationDao;
import net.solarnetwork.central.dao.SolarNodeDao;
import net.solarnetwork.central.domain.SolarLocation;
import net.solarnetwork.central.domain.SolarNode;
import net.solarnetwork.central.in.biz.NetworkIdentityBiz;
import net.solarnetwork.central.instructor.biz.InstructorBiz;
import net.solarnetwork.central.instructor.domain.Instruction;
import net.solarnetwork.central.instructor.domain.InstructionParameter;
import net.solarnetwork.central.instructor.domain.NodeInstruction;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.security.AuthorizationException.Reason;
import net.solarnetwork.central.security.PasswordEncoder;
import net.solarnetwork.central.security.SecurityNode;
import net.solarnetwork.central.security.SecurityUtils;
import net.solarnetwork.central.user.biz.NodePKIBiz;
import net.solarnetwork.central.user.biz.RegistrationBiz;
import net.solarnetwork.central.user.dao.UserDao;
import net.solarnetwork.central.user.dao.UserNodeCertificateDao;
import net.solarnetwork.central.user.dao.UserNodeConfirmationDao;
import net.solarnetwork.central.user.dao.UserNodeDao;
import net.solarnetwork.central.user.domain.BasicUserNodeCertificateRenewal;
import net.solarnetwork.central.user.domain.NewNodeRequest;
import net.solarnetwork.central.user.domain.PasswordEntry;
import net.solarnetwork.central.user.domain.User;
import net.solarnetwork.central.user.domain.UserNode;
import net.solarnetwork.central.user.domain.UserNodeCertificate;
import net.solarnetwork.central.user.domain.UserNodeCertificateInstallationStatus;
import net.solarnetwork.central.user.domain.UserNodeCertificateRenewal;
import net.solarnetwork.central.user.domain.UserNodeCertificateStatus;
import net.solarnetwork.central.user.domain.UserNodeConfirmation;
import net.solarnetwork.central.user.domain.UserNodePK;
import net.solarnetwork.domain.BasicRegistrationReceipt;
import net.solarnetwork.domain.NetworkAssociation;
import net.solarnetwork.domain.NetworkAssociationDetails;
import net.solarnetwork.domain.NetworkCertificate;
import net.solarnetwork.domain.NetworkIdentity;
import net.solarnetwork.domain.RegistrationReceipt;
import net.solarnetwork.support.CertificateException;
import net.solarnetwork.support.CertificateService;
import net.solarnetwork.util.JavaBeanXmlSerializer;

/**
 * DAO-based implementation of {@link RegistrationBiz}.
 * 
 * <p>
 * The configurable properties of this class are:
 * </p>
 * 
 * <dl class="class-properties">
 * <dt>userDao</dt>
 * <dd>The {@link UserDao} to use for persisting users.</dd>
 * 
 * <dt>userValidator</dt>
 * <dd>A {@link Validator} to use for validating user objects.</dd>
 * 
 * <dt>passwordEncoder</dt>
 * <dd>The {@link PasswordEncoder} to use for encrypting passwords.</dd>
 * </dl>
 * 
 * @author matt
 * @version 1.9
 */
public class DaoRegistrationBiz implements RegistrationBiz {

	public static final SortedSet<String> DEFAULT_CONFIRMED_USER_ROLES = Collections
			.unmodifiableSortedSet(new TreeSet<String>(Arrays.asList("ROLE_USER")));

	/**
	 * Instruction topic for sending a renewed certificate to a node.
	 * 
	 * @since 1.8
	 */
	public static final String INSTRUCTION_TOPIC_RENEW_CERTIFICATE = "RenewCertificate";

	/**
	 * Instruction parameter for certificate data. Since instruction parameters
	 * are limited in length, there can be more than one parameter of the same
	 * key, with the full data being the concatenation of all parameter values.
	 * 
	 * @since 1.8
	 */
	public static final String INSTRUCTION_PARAM_CERTIFICATE = "Certificate";

	/**
	 * The default maximum length for instruction parameter values.
	 * 
	 * @since 1.8
	 */
	public static final int INSTRUCTION_PARAM_DEFAULT_MAX_LENGTH = 256;

	private final Logger log = LoggerFactory.getLogger(getClass());

	private UserDao userDao;
	private UserNodeDao userNodeDao;
	private UserNodeConfirmationDao userNodeConfirmationDao;
	private UserNodeCertificateDao userNodeCertificateDao;
	private Validator userValidator;
	private SolarNodeDao solarNodeDao;
	private SolarLocationDao solarLocationDao;
	private NetworkIdentityBiz networkIdentityBiz;
	private PasswordEncoder passwordEncoder;
	private Set<String> confirmedUserRoles = DEFAULT_CONFIRMED_USER_ROLES;
	private JavaBeanXmlSerializer xmlSerializer = new JavaBeanXmlSerializer();
	private Ehcache emailThrottleCache;
	private CertificateService certificateService;
	private NodePKIBiz nodePKIBiz;
	private int nodePrivateKeySize = 2048;
	private String nodeKeystoreAlias = "node";
	private ExecutorService executorService = Executors.newCachedThreadPool();
	private int approveCSRMaximumWaitSecs = 15;
	private InstructorBiz instructorBiz;
	private int instructionParamMaxLength = INSTRUCTION_PARAM_DEFAULT_MAX_LENGTH;

	private Period invitationExpirationPeriod = new Period(0, 0, 1, 0, 0, 0, 0, 0); // 1 week
	private Period nodeCertificateRenewalPeriod = new Period(0, 3, 0, 0, 0, 0, 0, 0); // 3 months
	private String defaultSolarLocationName = "Unknown";
	private String networkCertificateSubjectDNFormat = "UID=%s,O=SolarNetwork";

	private User getCurrentUser() {
		String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
		User user = null;
		if ( currentUserEmail != null ) {
			user = userDao.getUserByEmail(currentUserEmail);
		}
		return user;
	}

	@Override
	public RegistrationReceipt createReceipt(String username, String confirmationCode) {
		return new BasicRegistrationReceipt(username, confirmationCode);
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public RegistrationReceipt registerUser(User user) throws AuthorizationException {

		// perform service-side validation
		if ( this.userValidator != null ) {
			Errors errors = new BindException(user, "user");
			this.userValidator.validate(user, errors);
			if ( errors.hasErrors() ) {
				throw new ValidationException(errors);
			}
		}

		User clone = (User) user.clone();

		// store user
		prepareUserForStorage(clone);

		// adjust email so we know they are not confirmed
		clone.setEmail(getUnconfirmedEmail(clone.getEmail()));

		User entity;
		try {
			entity = userDao.get(userDao.store(clone));
		} catch ( DataIntegrityViolationException e ) {
			if ( log.isWarnEnabled() ) {
				log.warn("Duplicate user registration: " + clone.getEmail());
			}
			throw new AuthorizationException(user.getEmail(),
					AuthorizationException.Reason.DUPLICATE_EMAIL);
		}

		// generate confirmation string
		String conf = calculateConfirmationCode(entity);
		if ( log.isInfoEnabled() ) {
			log.info("Registered user '" + entity.getEmail() + "' with confirmation '" + conf + "'");
		}

		return new BasicRegistrationReceipt(entity.getEmail(), conf);
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public User confirmRegisteredUser(RegistrationReceipt receipt) throws AuthorizationException {
		final String confirmedEmail;
		try {
			confirmedEmail = getOriginalEmail(receipt.getUsername());
		} catch ( IllegalArgumentException e ) {
			throw new AuthorizationException(receipt.getUsername(),
					AuthorizationException.Reason.UNKNOWN_EMAIL);
		}

		// first check if already registered
		User entity = userDao.getUserByEmail(confirmedEmail);
		if ( entity != null ) {
			throw new AuthorizationException(receipt.getUsername(),
					AuthorizationException.Reason.REGISTRATION_ALREADY_CONFIRMED);
		}

		final String unregEmail = receipt.getUsername();
		entity = userDao.getUserByEmail(unregEmail);
		if ( entity == null ) {
			throw new AuthorizationException(receipt.getUsername(),
					AuthorizationException.Reason.UNKNOWN_EMAIL);
		}

		// validate confirmation code
		String entityConf = calculateConfirmationCode(entity);
		if ( !entityConf.equals(receipt.getConfirmationCode()) ) {
			throw new AuthorizationException(receipt.getUsername(),
					AuthorizationException.Reason.REGISTRATION_NOT_CONFIRMED);
		}

		// change their email to "registered"
		entity.setEmail(confirmedEmail);

		// update confirmed user
		entity = userDao.get(userDao.store(entity));

		// store initial user roles
		userDao.storeUserRoles(entity, confirmedUserRoles);
		entity.setRoles(userDao.getUserRoles(entity));

		return entity;
	}

	private String calculateConfirmationCode(User user) {
		return DigestUtils.sha256Hex(
				user.getCreated().getMillis() + user.getId() + user.getEmail() + user.getPassword());
	}

	private void prepareUserForStorage(User user) throws AuthorizationException {

		// check for "unchanged" password value
		if ( user.getId() != null && DO_NOT_CHANGE_VALUE.equals(user.getPassword()) ) {
			// retrieve user from back-end and copy that password onto our user
			User realUser = userDao.get(user.getId());
			user.setPassword(realUser.getPassword());
		}

		// check password is encrypted
		if ( !passwordEncoder.isPasswordEncrypted(user.getPassword()) ) {
			// encrypt the password now
			String encryptedPass = passwordEncoder.encode(user.getPassword());
			user.setPassword(encryptedPass);
		}
		if ( user.getCreated() == null ) {
			user.setCreated(new DateTime());
		}

		// verify email not already in use, after trimming
		if ( user.getEmail() != null && user.getEmail().trim().equals(user.getEmail()) == false ) {
			user.setEmail(user.getEmail().trim());
		}
		User existingUser = userDao.getUserByEmail(user.getEmail());
		if ( existingUser != null && !existingUser.getId().equals(user.getId()) ) {
			throw new AuthorizationException(user.getEmail(),
					AuthorizationException.Reason.DUPLICATE_EMAIL);
		}

		// sent enabled if not configured
		if ( user.getEnabled() == null ) {
			user.setEnabled(Boolean.TRUE);
		}
	}

	private String encodeNetworkAssociationDetails(NetworkAssociationDetails details) {
		ByteArrayOutputStream byos = new ByteArrayOutputStream();
		Base64OutputStream b64 = new Base64OutputStream(byos, true);
		GZIPOutputStream zip = null;
		try {
			zip = new GZIPOutputStream(b64);
			xmlSerializer.renderBean(details, zip);
		} catch ( IOException e ) {
			throw new RuntimeException(e);
		} finally {
			if ( zip != null ) {
				try {
					zip.flush();
					zip.close();
				} catch ( IOException e2 ) {
					// ignore me
				}
			}
			if ( b64 != null ) {
				try {
					b64.flush();
					b64.close();
				} catch ( IOException e ) {
					// ignore me
				}
			}
		}
		return byos.toString();
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public NetworkAssociation createNodeAssociation(final NewNodeRequest request) {
		User user = null;
		if ( request.getUserId() == null ) {
			user = getCurrentUser();
		} else {
			user = userDao.get(request.getUserId());
		}
		assert user != null;

		DateTime now = new DateTime();
		NetworkIdentity ident = networkIdentityBiz.getNetworkIdentity();

		NetworkAssociationDetails details = new NetworkAssociationDetails();
		details.setHost(ident.getHost());
		details.setPort(ident.getPort());
		details.setForceTLS(ident.isForceTLS());
		details.setIdentityKey(ident.getIdentityKey());
		details.setUsername(user.getEmail());
		details.setExpiration(now.plus(invitationExpirationPeriod).toDate());

		String confKey = DigestUtils.sha256Hex(
				String.valueOf(now.getMillis()) + details.getIdentityKey() + details.getTermsOfService()
						+ details.getUsername() + details.getExpiration() + request.getSecurityPhrase());
		details.setConfirmationKey(confKey);

		String xml = encodeNetworkAssociationDetails(details);
		details.setConfirmationKey(xml);

		// the following are not encoded into confirmation XML
		details.setSecurityPhrase(request.getSecurityPhrase());
		details.setTermsOfService(ident.getTermsOfService());

		// create UserNodeConfirmation now
		UserNodeConfirmation conf = new UserNodeConfirmation();
		conf.setCreated(now);
		conf.setUser(user);
		conf.setConfirmationKey(confKey);
		conf.setSecurityPhrase(request.getSecurityPhrase());
		conf.setCountry(request.getLocale().getCountry());
		conf.setTimeZoneId(request.getTimeZone().getID());
		userNodeConfirmationDao.store(conf);

		return details;
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public UserNode createNodeManually(NewNodeRequest request) {
		final User user = userDao.get(request.getUserId());
		if ( user == null ) {
			throw new AuthorizationException(Reason.UNKNOWN_OBJECT, request.getUserId());
		}

		final Long nodeId = solarNodeDao.getUnusedNodeId();
		return createNewNode(request.getLocale().getCountry(), request.getTimeZone().getID(), user,
				nodeId, request.getSecurityPhrase());
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public NetworkAssociation getNodeAssociation(final Long userNodeConfirmationId)
			throws AuthorizationException {
		final UserNodeConfirmation conf = userNodeConfirmationDao.get(userNodeConfirmationId);
		if ( conf == null ) {
			return null;
		}
		final NetworkIdentity ident = networkIdentityBiz.getNetworkIdentity();
		NetworkAssociationDetails details = new NetworkAssociationDetails();
		details.setHost(ident.getHost());
		details.setPort(ident.getPort());
		details.setForceTLS(ident.isForceTLS());
		details.setNetworkId(conf.getNodeId());
		details.setIdentityKey(ident.getIdentityKey());
		details.setUsername(conf.getUser().getEmail());
		details.setExpiration(conf.getCreated().plus(invitationExpirationPeriod).toDate());
		details.setConfirmationKey(conf.getConfirmationKey());

		String xml = encodeNetworkAssociationDetails(details);
		details.setConfirmationKey(xml);

		// the following are not encoded into confirmation XML
		details.setSecurityPhrase(conf.getSecurityPhrase());
		details.setTermsOfService(ident.getTermsOfService());

		return details;
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void cancelNodeAssociation(Long userNodeConfirmationId) throws AuthorizationException {
		final UserNodeConfirmation conf = userNodeConfirmationDao.get(userNodeConfirmationId);
		if ( conf == null ) {
			return;
		}
		userNodeConfirmationDao.delete(conf);
	}

	private String calculateNodeAssociationConfirmationCode(DateTime date, Long nodeId) {
		return DigestUtils.sha256Hex(String.valueOf(date.getMillis()) + String.valueOf(nodeId));
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public NetworkCertificate confirmNodeAssociation(final String username,
			final String confirmationKey) {
		assert username != null;
		assert confirmationKey != null;
		return confirmNodeAssociation(new NetworkAssociationDetails(username, confirmationKey, null));
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public NetworkCertificate getNodeCertificate(final NetworkAssociation association) {
		if ( association == null ) {
			throw new IllegalArgumentException("NetworkAssociation must be provided.");
		}
		final String username = association.getUsername();
		final String confirmationKey = association.getConfirmationKey();
		final String keystorePassword = association.getKeystorePassword();
		if ( username == null ) {
			throw new AuthorizationException(AuthorizationException.Reason.UNKNOWN_OBJECT, null);
		}
		if ( confirmationKey == null ) {
			throw new AuthorizationException(AuthorizationException.Reason.UNKNOWN_OBJECT, null);
		}
		if ( keystorePassword == null ) {
			throw new AuthorizationException(AuthorizationException.Reason.UNKNOWN_OBJECT, null);
		}

		final User user = userDao.getUserByEmail(username);
		if ( user == null ) {
			throw new AuthorizationException(Reason.UNKNOWN_EMAIL, username);
		}

		final UserNodeConfirmation conf = userNodeConfirmationDao.getConfirmationForKey(user.getId(),
				confirmationKey);
		if ( conf == null ) {
			throw new AuthorizationException(AuthorizationException.Reason.UNKNOWN_OBJECT,
					confirmationKey);
		}

		final Long nodeId = conf.getNodeId();
		if ( nodeId == null ) {
			throw new AuthorizationException(AuthorizationException.Reason.UNKNOWN_OBJECT, null);
		}

		final UserNodeCertificate cert = userNodeCertificateDao
				.get(new UserNodePK(user.getId(), nodeId));
		if ( cert == null ) {
			throw new AuthorizationException(AuthorizationException.Reason.UNKNOWN_OBJECT, null);
		}

		final KeyStore keystore;
		try {
			keystore = cert.getKeyStore(keystorePassword);
		} catch ( CertificateException e ) {
			throw new AuthorizationException(AuthorizationException.Reason.ACCESS_DENIED, null);
		}

		final NetworkAssociationDetails details = new NetworkAssociationDetails();

		final X509Certificate certificate = cert.getNodeCertificate(keystore);
		if ( certificate != null ) {
			details.setNetworkCertificateSubjectDN(certificate.getSubjectX500Principal().getName());

			// if the certificate has been signed by a CA, then include the entire .p12 in the response (Base 64 encoded)
			if ( certificate.getIssuerX500Principal()
					.equals(certificate.getSubjectX500Principal()) == false ) {
				details.setNetworkCertificate(Base64.encodeBase64String(cert.getKeystoreData()));
			}
		}

		details.setNetworkId(nodeId);
		details.setConfirmationKey(confirmationKey);
		details.setNetworkCertificateStatus(cert.getStatus().getValue());
		return details;
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public UserNodeCertificateRenewal renewNodeCertificate(final UserNode userNode,
			final String keystorePassword) {
		if ( userNode == null ) {
			throw new IllegalArgumentException("UserNode must be provided.");
		}
		if ( keystorePassword == null ) {
			throw new IllegalArgumentException("Keystore password must be provided.");
		}

		final Long nodeId = userNode.getId();
		if ( nodeId == null ) {
			throw new AuthorizationException(AuthorizationException.Reason.UNKNOWN_OBJECT, null);
		}
		final UserNodeCertificate cert = userNodeCertificateDao
				.get(new UserNodePK(userNode.getUser().getId(), nodeId));
		return renewNodeCertificate(cert, keystorePassword);
	}

	private UserNodeCertificateRenewal renewNodeCertificate(final UserNodeCertificate cert,
			final String keystorePassword) {
		if ( cert == null ) {
			throw new AuthorizationException(AuthorizationException.Reason.UNKNOWN_OBJECT, null);
		}
		final User user = cert.getUser();
		if ( user == null ) {
			throw new AuthorizationException(AuthorizationException.Reason.UNKNOWN_OBJECT, null);
		}
		final Long nodeId = cert.getNodeId();
		if ( nodeId == null ) {
			throw new AuthorizationException(AuthorizationException.Reason.UNKNOWN_OBJECT, null);
		}

		final KeyStore keystore;
		try {
			keystore = cert.getKeyStore(keystorePassword);
		} catch ( CertificateException e ) {
			throw new AuthorizationException(AuthorizationException.Reason.ACCESS_DENIED, null);
		}

		final X509Certificate certificate = cert.getNodeCertificate(keystore);
		if ( certificate == null ) {
			throw new AuthorizationException(AuthorizationException.Reason.UNKNOWN_OBJECT, null);
		}

		if ( nodeCertificateRenewalPeriod != null ) {
			if ( new DateTime(certificate.getNotAfter()).minus(nodeCertificateRenewalPeriod)
					.isAfterNow() ) {
				throw new AuthorizationException(AuthorizationException.Reason.ACCESS_DENIED, null);
			}
		}

		String renewRequestID = nodePKIBiz.submitRenewalRequest(certificate);
		if ( renewRequestID == null ) {
			log.error("No renew request ID returned for {}", certificate.getSubjectDN());
			throw new CertificateException("No CSR request ID returned");
		}

		// update the UserNodeCert's request ID to the renewal ID
		cert.setRequestId(renewRequestID);
		cert.setStatus(UserNodeCertificateStatus.a);

		final String certSubjectDN = String.format(networkCertificateSubjectDNFormat, nodeId.toString());

		final Future<UserNodeCertificate> approval = approveCSR(certSubjectDN, keystorePassword, user,
				cert);
		Instruction installInstruction = null;
		try {
			UserNodeCertificate renewedCert = approval.get(approveCSRMaximumWaitSecs, TimeUnit.SECONDS);
			cert.setStatus(renewedCert.getStatus());
			cert.setCreated(renewedCert.getCreated());
			cert.setKeystoreData(renewedCert.getKeystoreData());
			installInstruction = queueRenewedNodeCertificateInstruction(renewedCert, keystorePassword);
		} catch ( TimeoutException e ) {
			log.debug("Timeout waiting for {} cert renewal approval", certSubjectDN);
			// save to DB when we do get our reply
			executorService.submit(new Runnable() {

				@Override
				public void run() {
					try {
						UserNodeCertificate renewedCert = approval.get();
						cert.setStatus(renewedCert.getStatus());
						cert.setCreated(renewedCert.getCreated());
						cert.setKeystoreData(renewedCert.getKeystoreData());
						userNodeCertificateDao.store(cert);
						queueRenewedNodeCertificateInstruction(renewedCert, keystorePassword);
					} catch ( Exception e ) {
						log.error("Error approving cert {}", certSubjectDN, e);
					}
				}
			});
		} catch ( InterruptedException e ) {
			log.debug("Interrupted waiting for {} cert renewal approval", certSubjectDN);
			// just continue
		} catch ( ExecutionException e ) {
			log.error("CSR {} approval threw an exception: {}", certSubjectDN, e.getMessage());
			throw new CertificateException("Error approving cert renewal", e);
		}

		// update the request ID to the instruction ID, if available; then we can query for 
		// the instruction later
		if ( installInstruction != null && installInstruction.getId() != null ) {
			cert.setRequestId(installInstruction.getId().toString());
		}

		userNodeCertificateDao.store(cert);

		BasicUserNodeCertificateRenewal details = new BasicUserNodeCertificateRenewal();
		details.setNetworkId(nodeId);
		if ( cert != null ) {
			details.setNetworkCertificateStatus(cert.getStatus().getValue());
			if ( cert.getStatus() == UserNodeCertificateStatus.v ) {
				details.setNetworkCertificate(getCertificateAsString(cert.getKeystoreData()));
			}
		}
		details.setNetworkCertificateSubjectDN(certSubjectDN);

		// provide the instruction ID as the confirmation ID
		if ( installInstruction != null && installInstruction.getId() != null ) {
			details.setConfirmationKey(installInstruction.getId().toString());
		}

		return details;
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public UserNodeCertificateRenewal getPendingNodeCertificateRenewal(UserNode userNode,
			String confirmationKey) {
		Long instructionId;
		try {
			instructionId = Long.valueOf(confirmationKey);
		} catch ( RuntimeException e ) {
			throw new AuthorizationException(AuthorizationException.Reason.UNKNOWN_OBJECT,
					confirmationKey);
		}
		NodeInstruction instruction = instructorBiz.getInstruction(instructionId);
		if ( instruction == null ) {
			return null;
		}

		// verify the node ID matches
		Long nodeId = userNode.getId();
		if ( nodeId == null && userNode.getNode() != null ) {
			nodeId = userNode.getNode().getId();
		}
		if ( !instruction.getNodeId().equals(nodeId) ) {
			throw new AuthorizationException(AuthorizationException.Reason.ACCESS_DENIED,
					confirmationKey);
		}

		BasicUserNodeCertificateRenewal details = new BasicUserNodeCertificateRenewal();
		details.setNetworkId(userNode.getId());
		details.setConfirmationKey(instructionId.toString());

		UserNodeCertificateInstallationStatus installStatus;
		switch (instruction.getState()) {
			case Queued:
				installStatus = UserNodeCertificateInstallationStatus.RequestQueued;
				break;

			case Received:
			case Executing:
				installStatus = UserNodeCertificateInstallationStatus.RequestReceived;
				break;

			case Completed:
				installStatus = UserNodeCertificateInstallationStatus.Installed;
				break;

			case Declined:
				installStatus = UserNodeCertificateInstallationStatus.Declined;
				break;

			default:
				installStatus = null;
		}

		details.setInstallationStatus(installStatus);

		if ( instruction.getParameters() != null ) {
			StringBuilder buf = new StringBuilder();
			for ( InstructionParameter param : instruction.getParameters() ) {
				if ( INSTRUCTION_PARAM_CERTIFICATE.equals(param.getName()) ) {
					buf.append(param.getValue());
				}
			}
			if ( buf.length() > 0 ) {
				details.setNetworkCertificate(buf.toString());
			}
		}

		return details;
	}

	/**
	 * Create a new node instruction with the renewed node certificate. Only the
	 * certificate will be queued, not the private key.
	 * 
	 * @param cert
	 *        The renewed certificate the node should download.
	 * @param keystorePassword
	 *        The password to read the keystore with.
	 */
	private Instruction queueRenewedNodeCertificateInstruction(final UserNodeCertificate cert,
			final String keystorePassword) {
		final InstructorBiz instructor = instructorBiz;
		final CertificateService certService = certificateService;
		if ( instructor == null || certService == null ) {
			log.debug(
					"Either InstructorBiz or CertificateService are null, cannot queue cert renewal instruction.");
			return null;
		}
		if ( keystorePassword == null ) {
			throw new IllegalArgumentException("Keystore password must be provided.");
		}
		final KeyStore keystore;
		try {
			keystore = cert.getKeyStore(keystorePassword);
		} catch ( CertificateException e ) {
			throw new AuthorizationException(AuthorizationException.Reason.ACCESS_DENIED, null);
		}

		final X509Certificate certificate = cert.getNodeCertificate(keystore);
		if ( certificate == null ) {
			throw new AuthorizationException(AuthorizationException.Reason.UNKNOWN_OBJECT, null);
		}
		String pem = certService
				.generatePKCS7CertificateChainString(new X509Certificate[] { certificate });
		Instruction instr = new Instruction(INSTRUCTION_TOPIC_RENEW_CERTIFICATE, new DateTime());
		final int max = instructionParamMaxLength;
		int i = 0, len = pem.length();
		while ( i < len ) {
			int end = i + (i + max < len ? max : (len - i));
			String val = pem.substring(i, end);
			instr.addParameter(INSTRUCTION_PARAM_CERTIFICATE, val);
			i += max;
		}
		return instructor.queueInstruction(cert.getNodeId(), instr);
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public NetworkCertificate renewNodeCertificate(final InputStream pkcs12InputStream,
			final String keystorePassword) throws IOException {
		if ( pkcs12InputStream == null ) {
			throw new IllegalArgumentException("Keystore must be provided.");
		}
		if ( keystorePassword == null ) {
			throw new AuthorizationException(AuthorizationException.Reason.UNKNOWN_OBJECT, null);
		}

		SecurityNode actor = SecurityUtils.getCurrentNode();
		final Long nodeId = actor.getNodeId();
		if ( nodeId == null ) {
			throw new AuthorizationException(AuthorizationException.Reason.UNKNOWN_OBJECT, null);
		}

		final UserNode userNode = userNodeDao.get(nodeId);
		if ( userNode == null ) {
			throw new AuthorizationException(AuthorizationException.Reason.UNKNOWN_OBJECT, null);
		}

		// get existing UserNodeCertificate, else create a new one
		final UserNodePK userNodePK = new UserNodePK(userNode.getUser().getId(), nodeId);
		UserNodeCertificate userNodeCert = userNodeCertificateDao.get(userNodePK);
		if ( userNodeCert == null ) {
			userNodeCert = new UserNodeCertificate();
			userNodeCert.setId(userNodePK);
			userNodeCert.setUser(userNode.getUser());
			userNodeCert.setNode(userNode.getNode());
			userNodeCert.setCreated(new DateTime());
			userNodeCert.setStatus(UserNodeCertificateStatus.a);
		}

		// extract the existing node certificate
		userNodeCert.setKeystoreData(FileCopyUtils.copyToByteArray(pkcs12InputStream));

		return renewNodeCertificate(userNodeCert, keystorePassword);
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public NetworkCertificate confirmNodeAssociation(NetworkAssociation association)
			throws AuthorizationException {
		if ( association == null ) {
			throw new IllegalArgumentException("NetworkAssociation must be provided.");
		}
		final String username = association.getUsername();
		final String confirmationKey = association.getConfirmationKey();
		if ( username == null ) {
			throw new AuthorizationException(AuthorizationException.Reason.UNKNOWN_OBJECT, null);
		}
		if ( confirmationKey == null ) {
			throw new AuthorizationException(AuthorizationException.Reason.UNKNOWN_OBJECT, null);
		}
		final User user = userDao.getUserByEmail(username);
		if ( user == null ) {
			throw new AuthorizationException(Reason.UNKNOWN_EMAIL, null);
		}

		UserNodeConfirmation conf = userNodeConfirmationDao.getConfirmationForKey(user.getId(),
				confirmationKey);
		if ( conf == null ) {
			log.info("Association failed: confirmation not found for username {} key {}", username,
					confirmationKey);
			throw new AuthorizationException(username,
					AuthorizationException.Reason.REGISTRATION_NOT_CONFIRMED);
		}

		// security check: user must be the same that invited node
		if ( !user.equals(conf.getUser()) ) {
			log.info("Association failed: confirmation user {} != confirming user {}",
					conf.getUser().getId(), user.getId());
			throw new AuthorizationException(username,
					AuthorizationException.Reason.REGISTRATION_NOT_CONFIRMED);
		}

		// security check: must not be expired
		DateTime expiry = conf.getCreated().plus(invitationExpirationPeriod);
		if ( expiry.isBeforeNow() ) {
			log.info("Association failed: confirmation expired on {}", expiry);
			throw new AuthorizationException(username,
					AuthorizationException.Reason.REGISTRATION_NOT_CONFIRMED);
		}

		// security check: already confirmed?
		if ( conf.getConfirmationDate() != null ) {
			log.info("Association failed: confirmation already confirmed on {}",
					conf.getConfirmationDate());
			throw new AuthorizationException(username,
					AuthorizationException.Reason.REGISTRATION_ALREADY_CONFIRMED);
		}

		final Long nodeId = (conf.getNodeId() == null ? solarNodeDao.getUnusedNodeId()
				: conf.getNodeId());
		conf.setConfirmationDate(new DateTime());
		conf.setNodeId(nodeId);
		userNodeConfirmationDao.store(conf);

		UserNode userNode = createNewNode(conf.getCountry(), conf.getTimeZoneId(), user, nodeId,
				association.getKeystorePassword());

		NetworkAssociationDetails details = new NetworkAssociationDetails();
		details.setNetworkId(nodeId);
		details.setConfirmationKey(
				calculateNodeAssociationConfirmationCode(conf.getConfirmationDate(), nodeId));
		if ( userNode.getCertificate() != null ) {
			details.setNetworkCertificateStatus(userNode.getCertificate().getStatus().getValue());
			if ( userNode.getCertificate().getStatus() == UserNodeCertificateStatus.v ) {
				details.setNetworkCertificate(
						getCertificateAsString(userNode.getCertificate().getKeystoreData()));
			}
		}
		final String certSubjectDN = String.format(networkCertificateSubjectDNFormat, nodeId.toString());
		details.setNetworkCertificateSubjectDN(certSubjectDN);
		return details;
	}

	private UserNode createNewNode(String countryCode, String timeZoneId, User user, Long nodeId,
			String keystorePassword) {
		// find or create SolarLocation now, for country + time zone
		SolarLocation loc = solarLocationDao.getSolarLocationForTimeZone(countryCode, timeZoneId);
		if ( loc == null ) {
			// create location now
			loc = new SolarLocation();
			loc.setName(countryCode + " - " + timeZoneId);
			loc.setCountry(countryCode);
			loc.setTimeZoneId(timeZoneId);
			loc = solarLocationDao.get(solarLocationDao.store(loc));
		}
		assert loc != null;

		SolarNode node = solarNodeDao.get(nodeId);
		if ( node == null ) {
			node = new SolarNode();
			node.setId(nodeId);
			node.setLocation(loc);
			solarNodeDao.store(node);
		}

		// create UserNode now if it doesn't already exist
		UserNode userNode = userNodeDao.get(nodeId);
		if ( userNode == null ) {
			userNode = new UserNode();
			userNode.setNode(node);
			userNode.setUser(user);
			userNodeDao.store(userNode);
		}

		//		conf.setConfirmationDate(new DateTime());
		//		conf.setNodeId(nodeId);
		//		userNodeConfirmationDao.store(conf);

		final String certSubjectDN = String.format(networkCertificateSubjectDNFormat, nodeId.toString());

		UserNodeCertificate cert = null;
		if ( keystorePassword != null ) {
			// we must become the User now for CSR to be generated
			SecurityUtils.becomeUser(user.getEmail(), user.getName(), user.getId());

			// we'll generate a key and CSR for the user, encrypting with the provided password
			cert = generateNodeCSR(keystorePassword, certSubjectDN);
			if ( cert.getRequestId() == null ) {
				log.error("No CSR request ID returned for {}", certSubjectDN);
				throw new CertificateException("No CSR request ID returned");
			}

			cert.setCreated(new DateTime());
			cert.setNodeId(node.getId());
			cert.setUserId(user.getId());

			final Future<UserNodeCertificate> approval = approveCSR(certSubjectDN, keystorePassword,
					user, cert);
			try {
				cert = approval.get(approveCSRMaximumWaitSecs, TimeUnit.SECONDS);
			} catch ( TimeoutException e ) {
				log.warn("Timeout waiting for {} CSR approval", certSubjectDN);
				// save to DB when we do get our reply
				executorService.submit(new Runnable() {

					@Override
					public void run() {
						try {
							UserNodeCertificate approvedCert = approval.get();
							userNodeCertificateDao.store(approvedCert);
						} catch ( Exception e ) {
							log.error("Error approving cert {}", certSubjectDN, e);
						}
					}
				});
			} catch ( InterruptedException e ) {
				log.debug("Interrupted waiting for {} CSR approval", certSubjectDN);
				// just continue
			} catch ( ExecutionException e ) {
				log.error("CSR {} approval threw an exception: {}", certSubjectDN, e.getMessage());
				throw new CertificateException("Error approving CSR", e);
			}

			userNodeCertificateDao.store(cert);

			userNode.setCertificate(cert);
		}

		return userNode;
	}

	private String getCertificateAsString(byte[] data) {
		return Base64.encodeBase64String(data);
	}

	private Future<UserNodeCertificate> approveCSR(final String certSubjectDN,
			final String keystorePassword, final User user, final UserNodeCertificate cert) {
		return executorService.submit(new Callable<UserNodeCertificate>() {

			@Override
			public UserNodeCertificate call() throws Exception {
				SecurityUtils.becomeUser(user.getEmail(), user.getName(), user.getId());
				log.debug("Approving CSR {} request ID {}", certSubjectDN, cert.getRequestId());
				X509Certificate[] chain = nodePKIBiz.approveCSR(cert.getRequestId());
				saveNodeSignedCertificate(keystorePassword, cert, chain);
				return cert;
			}
		});
	}

	private void saveNodeSignedCertificate(final String keystorePassword, UserNodeCertificate cert,
			X509Certificate[] chain) throws CertificateException {
		log.debug("Saving approved certificate {}",
				(chain != null && chain.length > 0 ? chain[0].getSubjectDN().getName() : null));
		KeyStore keyStore = cert.getKeyStore(keystorePassword);
		Key key;
		try {
			key = keyStore.getKey(UserNodeCertificate.KEYSTORE_NODE_ALIAS,
					keystorePassword.toCharArray());
		} catch ( GeneralSecurityException e ) {
			throw new CertificateException("Error opening node private key", e);
		}
		X509Certificate nodeCert = cert.getNodeCertificate(keyStore);
		if ( nodeCert == null ) {
			throw new CertificateException(
					"UserNodeCertificate " + cert.getId() + " does not have a private key.");
		}

		log.info("Installing node certificate reply {} issued by {}",
				(chain != null && chain.length > 0 ? chain[0].getSubjectDN().getName() : null),
				(chain != null && chain.length > 0 ? chain[0].getIssuerDN().getName() : null));
		try {
			keyStore.setKeyEntry(UserNodeCertificate.KEYSTORE_NODE_ALIAS, key,
					keystorePassword.toCharArray(), chain);
		} catch ( KeyStoreException e ) {
			throw new CertificateException("Error opening node certificate", e);
		}

		ByteArrayOutputStream byos = new ByteArrayOutputStream();
		storeKeyStore(keyStore, keystorePassword, byos);
		cert.setKeystoreData(byos.toByteArray());
		cert.setStatus(UserNodeCertificateStatus.v);
	}

	private UserNodeCertificate generateNodeCSR(String keystorePassword, final String certSubjectDN) {
		log.info("Generating private key and CSR for node DN: {}", certSubjectDN);
		try {
			KeyStore keystore = loadKeyStore(keystorePassword, null);

			KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
			keyGen.initialize(nodePrivateKeySize, new SecureRandom());
			KeyPair keypair = keyGen.generateKeyPair();
			X509Certificate certificate = nodePKIBiz.generateCertificate(certSubjectDN,
					keypair.getPublic(), keypair.getPrivate());
			keystore.setKeyEntry(nodeKeystoreAlias, keypair.getPrivate(), keystorePassword.toCharArray(),
					new Certificate[] { certificate });

			log.debug("Submitting CSR {} to CA", certSubjectDN);

			String csrID = nodePKIBiz.submitCSR(certificate, keypair.getPrivate());

			log.debug("Submitted CSR {} to CA, got request ID {}", certSubjectDN, csrID);

			ByteArrayOutputStream byos = new ByteArrayOutputStream();
			storeKeyStore(keystore, keystorePassword, byos);

			UserNodeCertificate cert = new UserNodeCertificate();
			cert.setStatus(UserNodeCertificateStatus.a);
			cert.setRequestId(csrID);
			cert.setKeystoreData(byos.toByteArray());
			return cert;
		} catch ( GeneralSecurityException e ) {
			log.error("Error creating node CSR {}: {}", certSubjectDN, e.getMessage());
			throw new CertificateException("Unable to create node CSR " + certSubjectDN, e);
		}
	}

	private KeyStore loadKeyStore(String password, InputStream in) {
		KeyStore keyStore = null;
		try {
			keyStore = KeyStore.getInstance("pkcs12");
			keyStore.load(in, password.toCharArray());
			return keyStore;
		} catch ( KeyStoreException e ) {
			throw new CertificateException("Error loading certificate key store", e);
		} catch ( NoSuchAlgorithmException e ) {
			throw new CertificateException("Error loading certificate key store", e);
		} catch ( java.security.cert.CertificateException e ) {
			throw new CertificateException("Error loading certificate key store", e);
		} catch ( IOException e ) {
			String msg;
			if ( e.getCause() instanceof UnrecoverableKeyException ) {
				msg = "Invalid password loading key store";
			} else {
				msg = "Error loading certificate key store";
			}
			throw new CertificateException(msg, e);
		} finally {
			if ( in != null ) {
				try {
					in.close();
				} catch ( IOException e ) {
					// ignore this one
				}
			}
		}
	}

	private void storeKeyStore(KeyStore keystore, String password, OutputStream out) {
		final char[] pass = (password == null ? new char[0] : password.toCharArray());
		try {
			keystore.store(out, pass);
		} catch ( IOException e ) {
			throw new CertificateException("Unable to serialize keystore", e);
		} catch ( GeneralSecurityException e ) {
			throw new CertificateException("Unable to serialize keystore", e);
		} finally {
			try {
				out.flush();
				out.close();
			} catch ( IOException e ) {
				// ignore this one
			}
		}
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public User updateUser(User userEntry) {
		assert userEntry != null;
		assert userEntry.getId() != null;

		User entity = userDao.get(userEntry.getId());
		if ( entity == null ) {
			throw new AuthorizationException(userEntry.getEmail(), Reason.UNKNOWN_EMAIL);
		}

		if ( StringUtils.hasText(userEntry.getEmail()) ) {
			entity.setEmail(userEntry.getEmail());
		}
		if ( StringUtils.hasText(userEntry.getName()) ) {
			entity.setName(userEntry.getName());
		}
		if ( StringUtils.hasText(userEntry.getPassword())
				&& !DO_NOT_CHANGE_VALUE.equals(userEntry.getPassword()) ) {
			entity.setPassword(userEntry.getPassword());
		}

		prepareUserForStorage(entity);

		try {
			entity = userDao.get(userDao.store(entity));
		} catch ( DataIntegrityViolationException e ) {
			if ( log.isWarnEnabled() ) {
				log.warn("Duplicate user registration: " + entity.getEmail());
			}
			throw new AuthorizationException(entity.getEmail(),
					AuthorizationException.Reason.DUPLICATE_EMAIL);
		}
		return entity;
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public RegistrationReceipt generateResetPasswordReceipt(final String email)
			throws AuthorizationException {
		if ( emailThrottleCache != null && emailThrottleCache.isKeyInCache(email) ) {
			log.debug("Email {} in throttle cache; not generating reset password receipt", email);
			throw new AuthorizationException(email, Reason.ACCESS_DENIED);
		}
		final User entity = userDao.getUserByEmail(email);
		if ( entity == null ) {
			throw new AuthorizationException(email, Reason.UNKNOWN_EMAIL);
		}

		if ( emailThrottleCache != null ) {
			emailThrottleCache.put(new Element(email, Boolean.TRUE));
		}

		final String conf = calculateResetPasswordConfirmationCode(entity, null);
		return new BasicRegistrationReceipt(email, conf);
	}

	private String calculateResetPasswordConfirmationCode(User entity, String salt) {
		StringBuilder buf = new StringBuilder();
		if ( salt == null ) {
			// generate 8-byte salt
			final SecureRandom random = new SecureRandom();
			final int start = 97;
			final int end = 122;
			final int range = end - start;
			for ( int i = 0; i < 8; i++ ) {
				buf.append((char) (random.nextInt(range) + start));
			}
			salt = buf.toString();
		} else {
			buf.append(salt);
		}

		// use data from the existing user to create the confirmation hash
		buf.append(entity.getId()).append(entity.getCreated().getMillis()).append(entity.getPassword());

		return salt + DigestUtils.sha256Hex(buf.toString());
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void resetPassword(RegistrationReceipt receipt, PasswordEntry password) {
		if ( receipt == null || receipt.getUsername() == null || receipt.getConfirmationCode() == null
				|| receipt.getConfirmationCode().length() != 72 || password.getPassword() == null
				|| !password.getPassword().equals(password.getPasswordConfirm()) ) {
			throw new AuthorizationException(receipt.getUsername(),
					Reason.FORGOTTEN_PASSWORD_NOT_CONFIRMED);
		}

		final User entity = userDao.getUserByEmail(receipt.getUsername());
		if ( entity == null ) {
			throw new AuthorizationException(receipt.getUsername(), Reason.UNKNOWN_EMAIL);
		}

		final String salt = receipt.getConfirmationCode().substring(0, 8);
		final String expectedCode = calculateResetPasswordConfirmationCode(entity, salt);
		if ( !expectedCode.equals(receipt.getConfirmationCode()) ) {
			throw new AuthorizationException(receipt.getUsername(),
					Reason.FORGOTTEN_PASSWORD_NOT_CONFIRMED);
		}

		// ok, the conf code matches, let's reset the password
		final String encryptedPass = passwordEncoder.encode(password.getPassword());
		entity.setPassword(encryptedPass);
		userDao.store(entity);
	}

	public Set<String> getConfirmedUserRoles() {
		return confirmedUserRoles;
	}

	public void setConfirmedUserRoles(Set<String> confirmedUserRoles) {
		this.confirmedUserRoles = confirmedUserRoles;
	}

	public Period getInvitationExpirationPeriod() {
		return invitationExpirationPeriod;
	}

	public void setInvitationExpirationPeriod(Period invitationExpirationPeriod) {
		this.invitationExpirationPeriod = invitationExpirationPeriod;
	}

	public String getDefaultSolarLocationName() {
		return defaultSolarLocationName;
	}

	public void setDefaultSolarLocationName(String defaultSolarLocationName) {
		this.defaultSolarLocationName = defaultSolarLocationName;
	}

	public void setUserDao(UserDao userDao) {
		this.userDao = userDao;
	}

	public void setUserNodeDao(UserNodeDao userNodeDao) {
		this.userNodeDao = userNodeDao;
	}

	public void setUserNodeConfirmationDao(UserNodeConfirmationDao userNodeConfirmationDao) {
		this.userNodeConfirmationDao = userNodeConfirmationDao;
	}

	public void setUserValidator(Validator userValidator) {
		this.userValidator = userValidator;
	}

	public void setSolarNodeDao(SolarNodeDao solarNodeDao) {
		this.solarNodeDao = solarNodeDao;
	}

	public void setSolarLocationDao(SolarLocationDao solarLocationDao) {
		this.solarLocationDao = solarLocationDao;
	}

	public void setNetworkIdentityBiz(NetworkIdentityBiz networkIdentityBiz) {
		this.networkIdentityBiz = networkIdentityBiz;
	}

	public void setUserNodeCertificateDao(UserNodeCertificateDao userNodeCertificateDao) {
		this.userNodeCertificateDao = userNodeCertificateDao;
	}

	public void setNetworkCertificateSubjectDNFormat(String networkCertificateSubjectDNFormat) {
		this.networkCertificateSubjectDNFormat = networkCertificateSubjectDNFormat;
	}

	public void setPasswordEncoder(PasswordEncoder passwordEncoder) {
		this.passwordEncoder = passwordEncoder;
	}

	public void setXmlSerializer(JavaBeanXmlSerializer xmlSerializer) {
		this.xmlSerializer = xmlSerializer;
	}

	public void setEmailThrottleCache(Ehcache emailThrottleCache) {
		this.emailThrottleCache = emailThrottleCache;
	}

	public void setNodePKIBiz(NodePKIBiz nodePKIBiz) {
		this.nodePKIBiz = nodePKIBiz;
	}

	public void setNodePrivateKeySize(int nodePrivateKeySize) {
		this.nodePrivateKeySize = nodePrivateKeySize;
	}

	public void setNodeKeystoreAlias(String nodeKeystoreAlias) {
		this.nodeKeystoreAlias = nodeKeystoreAlias;
	}

	public void setExecutorService(ExecutorService executorService) {
		this.executorService = executorService;
	}

	public void setApproveCSRMaximumWaitSecs(int approveCSRMaximumWaitSecs) {
		this.approveCSRMaximumWaitSecs = approveCSRMaximumWaitSecs;
	}

	@Override
	public Period getNodeCertificateRenewalPeriod() {
		return nodeCertificateRenewalPeriod;
	}

	public void setNodeCertificateRenewalPeriod(Period nodeCertificateRenewalPeriod) {
		this.nodeCertificateRenewalPeriod = nodeCertificateRenewalPeriod;
	}

	/**
	 * Configure the node certificate renewal period as a number of months.
	 * 
	 * This is a convenience method that simply calls
	 * {@link #setNodeCertificateRenewalPeriod(Period)} with an appropriate
	 * {@code Period} for the provided months, or {@code null} if {@code months}
	 * is less than {@code 1}.
	 * 
	 * @param months
	 *        The number of months to set the renewal period to, or {@code 0} to
	 *        not enforce any limit.
	 * @since 1.8
	 */
	public void setNodeCertificateRenewalPeriodMonths(int months) {
		setNodeCertificateRenewalPeriod(months > 0 ? new Period(0, months, 0, 0, 0, 0, 0, 0) : null);
	}

	/**
	 * Set the InstructorBiz to use for queuing instructions.
	 * 
	 * @param instructorBiz
	 *        The service to use.
	 * @since 1.8
	 */
	public void setInstructorBiz(InstructorBiz instructorBiz) {
		this.instructorBiz = instructorBiz;
	}

	/**
	 * Set the {@link CertificateService} to use.
	 * 
	 * @param certificateService
	 *        The service to use.
	 * @since 1.8
	 */
	public void setCertificateService(CertificateService certificateService) {
		this.certificateService = certificateService;
	}

	/**
	 * The maximum length to use for instruction parameter values.
	 * 
	 * @param instructionParamMaxLength
	 *        The maximum length.
	 * 
	 * @since 1.8
	 */
	public void setInstructionParamMaxLength(int instructionParamMaxLength) {
		this.instructionParamMaxLength = instructionParamMaxLength;
	}

}
