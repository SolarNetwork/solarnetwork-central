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

import static net.solarnetwork.central.security.AuthorizationException.requireNonNullObject;
import static net.solarnetwork.central.user.biz.dao.UserBizConstants.getOriginalEmail;
import static net.solarnetwork.central.user.biz.dao.UserBizConstants.getUnconfirmedEmail;
import static net.solarnetwork.util.ObjectUtils.nonnull;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
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
import java.time.Instant;
import java.time.Period;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.zip.GZIPOutputStream;
import javax.cache.Cache;
import org.apache.commons.codec.digest.DigestUtils;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import net.solarnetwork.central.ValidationException;
import net.solarnetwork.central.biz.NetworkIdentificationBiz;
import net.solarnetwork.central.dao.SolarLocationDao;
import net.solarnetwork.central.dao.SolarNodeDao;
import net.solarnetwork.central.domain.SolarLocation;
import net.solarnetwork.central.domain.SolarNode;
import net.solarnetwork.central.instructor.biz.InstructorBiz;
import net.solarnetwork.central.instructor.domain.Instruction;
import net.solarnetwork.central.instructor.domain.InstructionParameter;
import net.solarnetwork.central.instructor.domain.NodeInstruction;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.security.AuthorizationException.Reason;
import net.solarnetwork.central.security.BasicSecurityException;
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
import net.solarnetwork.codec.JavaBeanXmlSerializer;
import net.solarnetwork.domain.BasicRegistrationReceipt;
import net.solarnetwork.domain.NetworkAssociation;
import net.solarnetwork.domain.NetworkAssociationDetails;
import net.solarnetwork.domain.NetworkCertificate;
import net.solarnetwork.domain.NetworkIdentity;
import net.solarnetwork.domain.RegistrationReceipt;
import net.solarnetwork.service.CertificateException;
import net.solarnetwork.service.CertificateService;
import net.solarnetwork.service.PasswordEncoder;

/**
 * DAO-based implementation of {@link RegistrationBiz}.
 *
 * @author matt
 * @version 3.0
 */
public class DaoRegistrationBiz implements RegistrationBiz {

	/**
	 * The default roles assigned to new users.
	 */
	public static final SortedSet<String> DEFAULT_CONFIRMED_USER_ROLES = Collections
			.unmodifiableSortedSet(new TreeSet<>(List.of("ROLE_USER")));

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

	/**
	 * The {@code networkCertificateSubjectFormat} property default value.
	 *
	 * @since 2.0
	 */
	public static final String DEFAULT_CERT_SUBJECT_FORMAT = "UID=%s,O=SolarNetwork";

	/**
	 * The {@code approveCsrMaximumWaitSecs} property default value.
	 *
	 * @since 2.0
	 */
	public static final int DEFAULT_APPROVE_CSR_MAX_WAIT_SECS = 15;

	// number of salt bytes to add to reset password confirmation codes
	private static final int RESET_PASSWORD_SALT_LENGTH = 16;

	// expected length is SHA-256 Hex + salt
	private static final int RESET_PASSWORD_CONF_CODE_LENGTH = 64 + RESET_PASSWORD_SALT_LENGTH;

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final UserDao userDao;
	private final UserNodeDao userNodeDao;
	private final UserNodeConfirmationDao userNodeConfirmationDao;
	private final UserNodeCertificateDao userNodeCertificateDao;
	private final SolarNodeDao solarNodeDao;
	private final SolarLocationDao solarLocationDao;
	private final NetworkIdentificationBiz networkIdentificationBiz;
	private final CertificateService certificateService;
	private final NodePKIBiz nodePKIBiz;
	private final InstructorBiz instructorBiz;
	private final PasswordEncoder passwordEncoder;
	private @Nullable Validator userValidator;
	private Set<String> confirmedUserRoles = DEFAULT_CONFIRMED_USER_ROLES;
	private JavaBeanXmlSerializer xmlSerializer = new JavaBeanXmlSerializer();
	private @Nullable Cache<String, Boolean> emailThrottleCache;
	private int nodePrivateKeySize = 2048;
	private String nodeKeystoreAlias = "node";
	private ExecutorService executorService = Executors.newCachedThreadPool();
	private int approveCsrMaximumWaitSecs = DEFAULT_APPROVE_CSR_MAX_WAIT_SECS;
	private int instructionParamMaxLength = INSTRUCTION_PARAM_DEFAULT_MAX_LENGTH;

	private Period invitationExpirationPeriod = Period.ofWeeks(1);
	private @Nullable Period nodeCertificateRenewalPeriod = Period.ofMonths(3);
	private String defaultSolarLocationName = "Unknown";
	private String networkCertificateSubjectFormat = DEFAULT_CERT_SUBJECT_FORMAT;

	/**
	 * Constructor.
	 * 
	 * @param userDao
	 *        the user DAO
	 * @param userNodeDao
	 *        the user node DAO
	 * @param userNodeConfirmationDao
	 *        the user node confirmation DAO
	 * @param userNodeCertificateDao
	 *        the user node certificate DAO
	 * @param solarNodeDao
	 *        the SolarNode DAO
	 * @param solarLocationDao
	 *        the location DAO
	 * @param networkIdentificationBiz
	 *        the network identification service
	 * @param certificateService
	 *        the certificate service
	 * @param nodePKIBiz
	 *        the node PKI service
	 * @param instructorBiz
	 *        the instruction service
	 * @param passwordEncoder
	 *        the password encoder
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public DaoRegistrationBiz(UserDao userDao, UserNodeDao userNodeDao,
			UserNodeConfirmationDao userNodeConfirmationDao,
			UserNodeCertificateDao userNodeCertificateDao, SolarNodeDao solarNodeDao,
			SolarLocationDao solarLocationDao, NetworkIdentificationBiz networkIdentificationBiz,
			CertificateService certificateService, NodePKIBiz nodePKIBiz, InstructorBiz instructorBiz,
			PasswordEncoder passwordEncoder) {
		super();
		this.userDao = requireNonNullArgument(userDao, "userDao");
		this.userNodeDao = requireNonNullArgument(userNodeDao, "userNodeDao");
		this.userNodeConfirmationDao = requireNonNullArgument(userNodeConfirmationDao,
				"userNodeConfirmationDao");
		this.userNodeCertificateDao = requireNonNullArgument(userNodeCertificateDao,
				"userNodeCertificateDao");
		this.solarNodeDao = requireNonNullArgument(solarNodeDao, "solarNodeDao");
		this.solarLocationDao = requireNonNullArgument(solarLocationDao, "solarLocationDao");
		this.networkIdentificationBiz = requireNonNullArgument(networkIdentificationBiz,
				"networkIdentificationBiz");
		this.certificateService = requireNonNullArgument(certificateService, "certificateService");
		this.nodePKIBiz = requireNonNullArgument(nodePKIBiz, "nodePKIBiz");
		this.instructorBiz = requireNonNullArgument(instructorBiz, "instructorBiz");
		this.passwordEncoder = requireNonNullArgument(passwordEncoder, "passwordEncoder");
	}

	private User getCurrentUser() {
		String currentUserEmail = SecurityUtils.getCurrentUser().getEmail();
		return nonnull(userDao.getUserByEmail(currentUserEmail), "Current user");
	}

	@Override
	public RegistrationReceipt createReceipt(String username, String confirmationCode) {
		return new BasicRegistrationReceipt(username, confirmationCode);
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED)
	public RegistrationReceipt registerUser(User user) throws AuthorizationException {

		// perform service-side validation
		if ( this.userValidator != null ) {
			Errors errors = new BindException(user, "user");
			this.userValidator.validate(user, errors);
			if ( errors.hasErrors() ) {
				throw new ValidationException(errors);
			}
		}

		User clone = user.clone();

		// store user
		prepareUserForStorage(clone);

		// adjust email so we know they are not confirmed
		clone.setEmail(getUnconfirmedEmail(clone.getEmail()));

		User entity;
		try {
			entity = nonnull(userDao.get(userDao.save(clone)), "user");
		} catch ( DataIntegrityViolationException e ) {
			log.warn("Duplicate user registration: {}", clone.getEmail());
			throw new AuthorizationException(user.getEmail(),
					AuthorizationException.Reason.DUPLICATE_EMAIL);
		}

		// generate confirmation string
		String conf = calculateConfirmationCode(entity);
		log.info("Registered user '{}' with confirmation '{}'", entity.getEmail(), conf);

		return new BasicRegistrationReceipt(entity.getEmail(), conf);
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED)
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
		entity = (unregEmail != null ? userDao.getUserByEmail(unregEmail) : null);
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
		entity = nonnull(userDao.get(userDao.save(entity)), "user");

		// store initial user roles
		userDao.storeUserRoles(entity, confirmedUserRoles);
		entity.setRoles(userDao.getUserRoles(entity));

		return entity;
	}

	private String calculateConfirmationCode(User user) {
		final Instant ts = nonnull(user.getCreated(), "created");
		final Long id = nonnull(user.getId(), "id");
		final String email = nonnull(user.getEmail(), "email");
		final String pw = nonnull(user.getPassword(), "password");
		return DigestUtils.sha256Hex(ts.toEpochMilli() + id + email + pw);
	}

	private void prepareUserForStorage(User user) throws AuthorizationException {

		// check for "unchanged" password value
		if ( user.getId() != null && DO_NOT_CHANGE_VALUE.equals(user.getPassword()) ) {
			// retrieve user from back-end and copy that password onto our user
			User realUser = nonnull(userDao.get(user.getId()), "user");
			user.setPassword(realUser.getPassword());
		}

		// check password is encrypted
		if ( !passwordEncoder.isPasswordEncrypted(user.getPassword()) ) {
			// encrypt the password now
			String encryptedPass = passwordEncoder.encode(user.getPassword());
			user.setPassword(encryptedPass);
		}
		if ( user.getCreated() == null ) {
			user.setCreated(Instant.now());
		}

		// verify email not already in use, after trimming
		if ( user.getEmail() != null && !user.getEmail().trim().equals(user.getEmail()) ) {
			user.setEmail(user.getEmail().trim());
		}
		User existingUser = userDao.getUserByEmail(user.getEmail());
		if ( existingUser != null && !nonnull(existingUser.getId(), "id").equals(user.getId()) ) {
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
		OutputStream b64 = Base64.getMimeEncoder().wrap(byos);
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
		return byos.toString(StandardCharsets.US_ASCII);
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED)
	public NetworkAssociation createNodeAssociation(final NewNodeRequest request) {
		User user;
		if ( request.getUserId() == null ) {
			user = getCurrentUser();
		} else {
			user = requireNonNullObject(userDao.get(request.getUserId()), request.getUserId());
		}

		Instant now = Instant.now();
		NetworkIdentity ident = networkIdentificationBiz.getNetworkIdentity();

		NetworkAssociationDetails details = new NetworkAssociationDetails();
		details.setHost(ident.getHost());
		details.setPort(ident.getPort());
		details.setForceTLS(ident.isForceTLS());
		details.setIdentityKey(ident.getIdentityKey());
		details.setUsername(user.getEmail());
		details.setExpiration(now.plus(invitationExpirationPeriod));

		String confKey = DigestUtils.sha256Hex(String.valueOf(now.toEpochMilli())
				+ details.getIdentityKey() + details.getTermsOfService() + details.getUsername()
				+ details.getExpiration() + request.getSecurityPhrase());
		details.setConfirmationKey(confKey);

		String xml = encodeNetworkAssociationDetails(details);
		details.setConfirmationKey(xml);

		// the following are not encoded into confirmation XML
		details.setSecurityPhrase(request.getSecurityPhrase());
		details.setTermsOfService(ident.getTermsOfService());

		// create UserNodeConfirmation now
		UserNodeConfirmation conf = new UserNodeConfirmation(user);
		conf.setCreated(now);
		conf.setConfirmationKey(confKey);
		conf.setSecurityPhrase(request.getSecurityPhrase());
		conf.setCountry(request.getLocale().getCountry());
		conf.setTimeZoneId(request.getTimeZone().getID());
		userNodeConfirmationDao.save(conf);

		return details;
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED)
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
		final UserNodeConfirmation conf = requireNonNullObject(
				userNodeConfirmationDao.get(userNodeConfirmationId), userNodeConfirmationId);
		final NetworkIdentity ident = networkIdentificationBiz.getNetworkIdentity();
		final User user = nonnull(conf.getUser(), "user");

		final NetworkAssociationDetails details = new NetworkAssociationDetails();
		details.setHost(ident.getHost());
		details.setPort(ident.getPort());
		details.setForceTLS(ident.isForceTLS());
		details.setNetworkId(conf.getNodeId());
		details.setIdentityKey(ident.getIdentityKey());
		details.setUsername(user.getEmail());
		details.setExpiration(nonnull(conf.getCreated(), "created").plus(invitationExpirationPeriod));
		details.setConfirmationKey(conf.getConfirmationKey());

		String xml = encodeNetworkAssociationDetails(details);
		details.setConfirmationKey(xml);

		// the following are not encoded into confirmation XML
		details.setSecurityPhrase(conf.getSecurityPhrase());
		details.setTermsOfService(ident.getTermsOfService());

		return details;
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED)
	public void cancelNodeAssociation(Long userNodeConfirmationId) throws AuthorizationException {
		final UserNodeConfirmation conf = userNodeConfirmationDao.get(userNodeConfirmationId);
		if ( conf != null ) {
			userNodeConfirmationDao.delete(conf);
		}
	}

	private String calculateNodeAssociationConfirmationCode(Instant date, Long nodeId) {
		return DigestUtils.sha256Hex(String.valueOf(date.toEpochMilli()) + String.valueOf(nodeId));
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public NetworkCertificate getNodeCertificate(final NetworkAssociation association) {
		if ( association == null ) {
			throw new IllegalArgumentException("NetworkAssociation must be provided.");
		}
		final String username = requireNonNullObject(association.getUsername(), "username");
		final String confirmationKey = requireNonNullObject(association.getConfirmationKey(),
				"confirmationKey");
		final String keystorePassword = requireNonNullObject(association.getKeystorePassword(),
				"keystorePassword");

		final User user = userDao.getUserByEmail(username);
		if ( user == null ) {
			throw new AuthorizationException(Reason.UNKNOWN_EMAIL, username);
		}
		final Long userId = nonnull(user.getId(), "User ID");

		final UserNodeConfirmation conf = requireNonNullObject(
				userNodeConfirmationDao.getConfirmationForKey(userId, confirmationKey), confirmationKey);

		final Long nodeId = requireNonNullObject(conf.getNodeId(), conf.getNodeId());

		final var userNodeId = new UserNodePK(userId, nodeId);
		final UserNodeCertificate cert = requireNonNullObject(userNodeCertificateDao.get(userNodeId),
				userNodeId);

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
			if ( !certificate.getIssuerX500Principal().equals(certificate.getSubjectX500Principal()) ) {
				details.setNetworkCertificate(
						Base64.getEncoder().encodeToString(cert.getKeystoreData()));
			}
		}

		details.setNetworkId(nodeId);
		details.setConfirmationKey(confirmationKey);
		details.setNetworkCertificateStatus(nonnull(cert.getStatus(), "Certificate status").getValue());
		return details;
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED)
	public UserNodeCertificateRenewal renewNodeCertificate(final UserNode userNode,
			final String keystorePassword) {
		if ( userNode == null ) {
			throw new IllegalArgumentException("UserNode must be provided.");
		}
		if ( keystorePassword == null ) {
			throw new IllegalArgumentException("Keystore password must be provided.");
		}

		final Long nodeId = requireNonNullObject(userNode.getId(), userNode.getId());
		final var userNodeId = new UserNodePK(userNode.getUser().getId(), nodeId);
		final UserNodeCertificate cert = userNodeCertificateDao.get(userNodeId);
		return renewNodeCertificate(cert, keystorePassword);
	}

	private UserNodeCertificateRenewal renewNodeCertificate(final @Nullable UserNodeCertificate cert,
			final String keystorePassword) {
		if ( cert == null ) {
			throw new AuthorizationException(AuthorizationException.Reason.UNKNOWN_OBJECT, null);
		}
		final User user = requireNonNullObject(cert.getUser(), "user");
		final Long nodeId = requireNonNullObject(cert.getNodeId(), "nodeId");

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
			if ( certificate.getNotAfter().toInstant().atZone(ZoneOffset.UTC)
					.minus(nodeCertificateRenewalPeriod).toInstant().isAfter(Instant.now()) ) {
				throw new AuthorizationException(AuthorizationException.Reason.ACCESS_DENIED, null);
			}
		}

		String renewRequestID = nodePKIBiz.submitRenewalRequest(certificate);
		if ( renewRequestID == null ) {
			log.error("No renew request ID returned for {}", certificate.getSubjectX500Principal());
			throw new CertificateException("No CSR request ID returned");
		}

		// update the UserNodeCert's request ID to the renewal ID
		cert.setRequestId(renewRequestID);
		cert.setStatus(UserNodeCertificateStatus.a);

		final String certSubjectDN = String.format(networkCertificateSubjectFormat, nodeId);

		final Future<UserNodeCertificate> approval = approveCSR(certSubjectDN, keystorePassword, user,
				cert);
		NodeInstruction installInstruction = null;
		try {
			UserNodeCertificate renewedCert = approval.get(approveCsrMaximumWaitSecs, TimeUnit.SECONDS);
			cert.setStatus(renewedCert.getStatus());
			cert.setCreated(renewedCert.getCreated());
			cert.setKeystoreData(renewedCert.getKeystoreData());
			installInstruction = queueRenewedNodeCertificateInstruction(renewedCert, keystorePassword);
		} catch ( TimeoutException e ) {
			log.debug("Timeout waiting for {} cert renewal approval", certSubjectDN);
			// save to DB when we do get our reply
			var _ = executorService.submit(() -> {
				try {
					UserNodeCertificate renewedCert = approval.get();
					cert.setStatus(renewedCert.getStatus());
					cert.setCreated(renewedCert.getCreated());
					cert.setKeystoreData(renewedCert.getKeystoreData());
					userNodeCertificateDao.save(cert);
					queueRenewedNodeCertificateInstruction(renewedCert, keystorePassword);
				} catch ( Exception e1 ) {
					log.error("Error approving cert {}", certSubjectDN, e1);
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

		userNodeCertificateDao.save(cert);

		BasicUserNodeCertificateRenewal details = new BasicUserNodeCertificateRenewal();
		details.setNetworkId(nodeId);
		details.setNetworkCertificateStatus(nonnull(cert.getStatus(), "Certificate status").getValue());
		if ( cert.getStatus() == UserNodeCertificateStatus.v ) {
			details.setNetworkCertificate(getCertificateAsString(
					nonnull(cert.getKeystoreData(), "Certificate keystore data")));
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
	public @Nullable UserNodeCertificateRenewal getPendingNodeCertificateRenewal(UserNode userNode,
			String confirmationKey) {
		Long instructionId;
		try {
			instructionId = Long.valueOf(confirmationKey);
		} catch ( NumberFormatException e ) {
			// might be ID from certificate creation and not a renewal instruction number
			return null;
		}
		NodeInstruction instruction = instructorBiz.getInstruction(instructionId);
		if ( instruction == null ) {
			return null;
		}

		// verify the node ID matches and actually a renew instruction
		Long nodeId = userNode.getId();
		if ( nodeId == null && userNode.getNode() != null ) {
			nodeId = userNode.getNode().getId();
		}
		if ( !(instruction.getNodeId().equals(nodeId) && INSTRUCTION_TOPIC_RENEW_CERTIFICATE
				.equals(instruction.getInstruction().getTopic())) ) {
			return null;
		}

		BasicUserNodeCertificateRenewal details = new BasicUserNodeCertificateRenewal();
		details.setNetworkId(userNode.getId());
		details.setConfirmationKey(instructionId.toString());

		UserNodeCertificateInstallationStatus installStatus = switch (instruction.getInstruction()
				.getState()) {
			case Queued -> UserNodeCertificateInstallationStatus.RequestQueued;
			case Received, Executing -> UserNodeCertificateInstallationStatus.RequestReceived;
			case Completed -> UserNodeCertificateInstallationStatus.Installed;
			case Declined -> UserNodeCertificateInstallationStatus.Declined;
			default -> null;
		};

		details.setInstallationStatus(installStatus);

		if ( instruction.getInstruction().getParameters() != null ) {
			StringBuilder buf = new StringBuilder();
			for ( InstructionParameter param : instruction.getInstruction().getParameters() ) {
				if ( INSTRUCTION_PARAM_CERTIFICATE.equals(param.getName()) ) {
					buf.append(param.getValue());
				}
			}
			if ( !buf.isEmpty() ) {
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
	private @Nullable NodeInstruction queueRenewedNodeCertificateInstruction(
			final UserNodeCertificate cert, final String keystorePassword) {
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
		String pem = certificateService
				.generatePKCS7CertificateChainString(new X509Certificate[] { certificate });
		Instruction instr = new Instruction(INSTRUCTION_TOPIC_RENEW_CERTIFICATE, Instant.now());
		final int max = instructionParamMaxLength;
		int i = 0, len = pem.length();
		while ( i < len ) {
			int end = i + (i + max < len ? max : (len - i));
			String val = pem.substring(i, end);
			instr.addParameter(INSTRUCTION_PARAM_CERTIFICATE, val);
			i += max;
		}
		return instructorBiz.queueInstruction(cert.getNodeId(), instr);
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED)
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
			userNodeCert.setCreated(Instant.now());
			userNodeCert.setStatus(UserNodeCertificateStatus.a);
		}

		// extract the existing node certificate
		userNodeCert.setKeystoreData(FileCopyUtils.copyToByteArray(pkcs12InputStream));

		return renewNodeCertificate(userNodeCert, keystorePassword);
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED)
	public NetworkCertificate confirmNodeAssociation(NetworkAssociation association)
			throws AuthorizationException {
		if ( association == null ) {
			throw new IllegalArgumentException("NetworkAssociation must be provided.");
		}
		final String username = requireNonNullObject(association.getUsername(), "username");
		final String confirmationKey = requireNonNullObject(association.getConfirmationKey(),
				"confirmationKey");
		final User user = userDao.getUserByEmail(username);
		if ( user == null ) {
			throw new AuthorizationException(Reason.UNKNOWN_EMAIL, null);
		}
		final Long userId = nonnull(user.getId(), "User ID");

		UserNodeConfirmation conf = userNodeConfirmationDao.getConfirmationForKey(userId,
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
					(conf.getUser() != null ? conf.getUser().getId() : null), userId);
			throw new AuthorizationException(username,
					AuthorizationException.Reason.REGISTRATION_NOT_CONFIRMED);
		}

		// security check: must not be expired
		Instant expiry = nonnull(conf.getCreated(), "Confirmation created date")
				.plus(invitationExpirationPeriod);
		if ( expiry.isBefore(Instant.now()) ) {
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
		conf.setConfirmationDate(Instant.now());
		conf.setNodeId(nodeId);
		userNodeConfirmationDao.save(conf);

		UserNode userNode = createNewNode(nonnull(conf.getCountry(), "Confirmation country"),
				nonnull(conf.getTimeZoneId(), "Confirmation time zone"), user, nodeId,
				association.getKeystorePassword());

		NetworkAssociationDetails details = new NetworkAssociationDetails();
		details.setNetworkId(nodeId);
		details.setConfirmationKey(calculateNodeAssociationConfirmationCode(
				nonnull(conf.getConfirmationDate(), "Confirmation date"), nodeId));
		if ( userNode.getCertificate() != null ) {
			final UserNodeCertificateStatus status = nonnull(userNode.getCertificate().getStatus(),
					"Certificate status");
			details.setNetworkCertificateStatus(status.getValue());
			if ( status == UserNodeCertificateStatus.v
					&& userNode.getCertificate().getKeystoreData() != null ) {
				details.setNetworkCertificate(
						getCertificateAsString(userNode.getCertificate().getKeystoreData()));
			}
		}
		final String certSubjectDN = String.format(networkCertificateSubjectFormat, nodeId.toString());
		details.setNetworkCertificateSubjectDN(certSubjectDN);
		return details;
	}

	private UserNode createNewNode(String countryCode, String timeZoneId, User user, Long nodeId,
			@Nullable String keystorePassword) {
		// find or create SolarLocation now, for country + time zone
		SolarLocation loc = solarLocationDao.getSolarLocationForTimeZone(countryCode, timeZoneId);
		if ( loc == null ) {
			// create location now
			loc = new SolarLocation();
			loc.setName(countryCode + " - " + timeZoneId);
			loc.setCountry(countryCode);
			loc.setTimeZoneId(timeZoneId);
			loc = solarLocationDao.get(solarLocationDao.save(loc));
		}
		assert loc != null;

		SolarNode node = solarNodeDao.get(nodeId);
		if ( node == null ) {
			node = new SolarNode();
			node.setId(nodeId);
			node.setLocation(loc);
			solarNodeDao.save(node);
		}

		// create UserNode now if it doesn't already exist
		UserNode userNode = userNodeDao.get(nodeId);
		if ( userNode == null ) {
			userNode = new UserNode(user, node);
			userNodeDao.save(userNode);
		}

		final String certSubjectDN = String.format(networkCertificateSubjectFormat, nodeId.toString());

		UserNodeCertificate cert;
		if ( keystorePassword != null ) {
			// we must become the User now for CSR to be generated (if we are a node or token actor)
			try {
				SecurityUtils.getCurrentUser();
			} catch ( BasicSecurityException e ) {
				SecurityUtils.becomeUser(user.getEmail(), user.getName(),
						nonnull(user.getId(), "User ID"));
			}

			// we'll generate a key and CSR for the user, encrypting with the provided password
			cert = generateNodeCSR(keystorePassword, certSubjectDN);
			if ( cert.getRequestId() == null ) {
				log.error("No CSR request ID returned for {}", certSubjectDN);
				throw new CertificateException("No CSR request ID returned");
			}

			cert.setCreated(Instant.now());
			cert.setNodeId(node.getId());
			cert.setUserId(user.getId());

			final Future<UserNodeCertificate> approval = approveCSR(certSubjectDN, keystorePassword,
					user, cert);
			try {
				cert = approval.get(approveCsrMaximumWaitSecs, TimeUnit.SECONDS);
			} catch ( TimeoutException e ) {
				log.warn("Timeout waiting for {} CSR approval", certSubjectDN);
				// save to DB when we do get our reply
				var _ = executorService.submit(() -> {
					try {
						UserNodeCertificate approvedCert = approval.get();
						userNodeCertificateDao.save(approvedCert);
					} catch ( Exception e1 ) {
						log.error("Error approving cert {}", certSubjectDN, e1);
					}
				});
			} catch ( InterruptedException e ) {
				log.debug("Interrupted waiting for {} CSR approval", certSubjectDN);
				// just continue
			} catch ( ExecutionException e ) {
				log.error("CSR {} approval threw an exception: {}", certSubjectDN, e.getMessage());
				throw new CertificateException("Error approving CSR", e);
			}

			userNodeCertificateDao.save(cert);

			userNode.setCertificate(cert);
		}

		return userNode;
	}

	private String getCertificateAsString(byte[] data) {
		return Base64.getEncoder().encodeToString(data);
	}

	private Future<UserNodeCertificate> approveCSR(final String certSubjectDN,
			final String keystorePassword, final User user, final UserNodeCertificate cert) {
		return executorService.submit(() -> {
			SecurityUtils.becomeUser(user.getEmail(), user.getName(), nonnull(user.getId(), "User ID"));
			log.debug("Approving CSR {} request ID {}", certSubjectDN, cert.getRequestId());
			X509Certificate[] chain = nodePKIBiz
					.approveCSR(nonnull(cert.getRequestId(), "Certificate request ID"));
			saveNodeSignedCertificate(keystorePassword, cert, chain);
			return cert;
		});
	}

	private void saveNodeSignedCertificate(final String keystorePassword, UserNodeCertificate cert,
			X509Certificate[] chain) throws CertificateException {
		log.debug("Saving approved certificate {}",
				(chain != null && chain.length > 0 ? chain[0].getSubjectX500Principal().getName()
						: null));
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

		log.info("Saving node certificate reply {} issued by {}",
				(chain != null && chain.length > 0 ? chain[0].getSubjectX500Principal().getName()
						: null),
				(chain != null && chain.length > 0 ? chain[0].getIssuerX500Principal().getName()
						: null));
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
			KeyStore keystore = createKeyStore(keystorePassword);

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

	private KeyStore createKeyStore(String password) {
		KeyStore keyStore;
		try {
			keyStore = KeyStore.getInstance("pkcs12");
			keyStore.load(null, password.toCharArray());
			return keyStore;
		} catch ( KeyStoreException | NoSuchAlgorithmException
				| java.security.cert.CertificateException e ) {
			throw new CertificateException("Error loading certificate key store", e);
		} catch ( IOException e ) {
			String msg;
			if ( e.getCause() instanceof UnrecoverableKeyException ) {
				msg = "Invalid password loading key store";
			} else {
				msg = "Error loading certificate key store";
			}
			throw new CertificateException(msg, e);
		}
	}

	private void storeKeyStore(KeyStore keystore, String password, OutputStream out) {
		final char[] pass = (password == null ? new char[0] : password.toCharArray());
		try {
			keystore.store(out, pass);
		} catch ( IOException | GeneralSecurityException e ) {
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
	@Transactional(propagation = Propagation.REQUIRED)
	public User updateUser(User userEntry) {
		assert userEntry != null;
		assert userEntry.getId() != null;

		User entity = userDao.get(requireNonNullArgument(userEntry.getId(), "user.id"));
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
			entity = nonnull(userDao.get(userDao.save(entity)), "User");
		} catch ( DataIntegrityViolationException e ) {
			log.warn("Duplicate user registration: {}", entity.getEmail());
			throw new AuthorizationException(entity.getEmail(),
					AuthorizationException.Reason.DUPLICATE_EMAIL);
		}
		return entity;
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public RegistrationReceipt generateResetPasswordReceipt(final String email)
			throws AuthorizationException {
		if ( emailThrottleCache != null && emailThrottleCache.containsKey(email) ) {
			log.debug("Email {} in throttle cache; not generating reset password receipt", email);
			throw new AuthorizationException(email, Reason.ACCESS_DENIED);
		}
		final User entity = userDao.getUserByEmail(email);
		if ( entity == null ) {
			throw new AuthorizationException(email, Reason.UNKNOWN_EMAIL);
		}

		if ( emailThrottleCache != null ) {
			emailThrottleCache.put(email, Boolean.TRUE);
		}

		final String conf = calculateResetPasswordConfirmationCode(entity, null);
		return new BasicRegistrationReceipt(email, conf);
	}

	private String calculateResetPasswordConfirmationCode(User entity, @Nullable String salt) {
		StringBuilder buf = new StringBuilder();
		if ( salt == null ) {
			// generate 8-byte salt of "safe" ASCII characters a-z
			final SecureRandom random = new SecureRandom();
			final int start = 97;
			final int end = 122;
			final int range = end - start;
			for ( int i = 0; i < RESET_PASSWORD_SALT_LENGTH; i++ ) {
				buf.append((char) (random.nextInt(range) + start));
			}
			salt = buf.toString();
		} else {
			buf.append(salt);
		}

		// use data from the existing user to create the confirmation hash
		buf.append(salt).append(entity.getId())
				.append(nonnull(entity.getCreated(), "User created date").toEpochMilli())
				.append(entity.getPassword());

		return salt + DigestUtils.sha256Hex(buf.toString());
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED)
	public void resetPassword(RegistrationReceipt receipt, PasswordEntry password) {
		if ( receipt == null || receipt.getUsername() == null || receipt.getConfirmationCode() == null
				|| receipt.getConfirmationCode().length() != RESET_PASSWORD_CONF_CODE_LENGTH
				|| password.getPassword() == null
				|| !password.getPassword().equals(password.getPasswordConfirm()) ) {
			throw new AuthorizationException(receipt != null ? receipt.getUsername() : null,
					Reason.FORGOTTEN_PASSWORD_NOT_CONFIRMED);
		}

		final User entity = userDao.getUserByEmail(receipt.getUsername());
		if ( entity == null ) {
			throw new AuthorizationException(receipt.getUsername(), Reason.UNKNOWN_EMAIL);
		}

		final String salt = receipt.getConfirmationCode().substring(0, RESET_PASSWORD_SALT_LENGTH);
		final String expectedCode = calculateResetPasswordConfirmationCode(entity, salt);
		if ( !expectedCode.equals(receipt.getConfirmationCode()) ) {
			throw new AuthorizationException(receipt.getUsername(),
					Reason.FORGOTTEN_PASSWORD_NOT_CONFIRMED);
		}

		// ok, the conf code matches, let's reset the password
		final String encryptedPass = passwordEncoder.encode(password.getPassword());
		entity.setPassword(encryptedPass);
		userDao.save(entity);
	}

	public final Set<String> getConfirmedUserRoles() {
		return confirmedUserRoles;
	}

	public final void setConfirmedUserRoles(Set<String> confirmedUserRoles) {
		this.confirmedUserRoles = requireNonNullArgument(confirmedUserRoles, "confirmedUserRoles");
	}

	public final Period getInvitationExpirationPeriod() {
		return invitationExpirationPeriod;
	}

	public final void setInvitationExpirationPeriod(Period invitationExpirationPeriod) {
		this.invitationExpirationPeriod = requireNonNullArgument(invitationExpirationPeriod,
				"invitationExpirationPeriod");
	}

	public final String getDefaultSolarLocationName() {
		return defaultSolarLocationName;
	}

	public final void setDefaultSolarLocationName(String defaultSolarLocationName) {
		this.defaultSolarLocationName = requireNonNullArgument(defaultSolarLocationName,
				"defaultSolarLocationName");
	}

	public final void setUserValidator(@Nullable Validator userValidator) {
		this.userValidator = userValidator;
	}

	public final void setNetworkCertificateSubjectFormat(String networkCertificateSubjectFormat) {
		this.networkCertificateSubjectFormat = networkCertificateSubjectFormat;
	}

	public final void setXmlSerializer(JavaBeanXmlSerializer xmlSerializer) {
		this.xmlSerializer = xmlSerializer;
	}

	public final void setEmailThrottleCache(@Nullable Cache<String, Boolean> emailThrottleCache) {
		this.emailThrottleCache = emailThrottleCache;
	}

	public final void setNodePrivateKeySize(int nodePrivateKeySize) {
		this.nodePrivateKeySize = nodePrivateKeySize;
	}

	public final void setNodeKeystoreAlias(String nodeKeystoreAlias) {
		this.nodeKeystoreAlias = nodeKeystoreAlias;
	}

	public final void setExecutorService(ExecutorService executorService) {
		this.executorService = executorService;
	}

	public final void setApproveCsrMaximumWaitSecs(int approveCsrMaximumWaitSecs) {
		this.approveCsrMaximumWaitSecs = approveCsrMaximumWaitSecs;
	}

	@Override
	public final @Nullable Period getNodeCertificateRenewalPeriod() {
		return nodeCertificateRenewalPeriod;
	}

	public final void setNodeCertificateRenewalPeriod(@Nullable Period nodeCertificateRenewalPeriod) {
		this.nodeCertificateRenewalPeriod = nodeCertificateRenewalPeriod;
	}

	/**
	 * Configure the node certificate renewal period as a number of months.
	 *
	 * <p>
	 * This is a convenience method that simply calls
	 * {@link #setNodeCertificateRenewalPeriod(Period)} with an appropriate
	 * {@code Period} for the provided months, or {@code null} if {@code months}
	 * is less than {@code 1}.
	 * </p>
	 *
	 * @param months
	 *        The number of months to set the renewal period to, or {@code 0} to
	 *        not enforce any limit.
	 * @since 1.8
	 */
	public final void setNodeCertificateRenewalPeriodMonths(int months) {
		setNodeCertificateRenewalPeriod(months > 0 ? Period.ofMonths(months) : null);
	}

	/**
	 * The maximum length to use for instruction parameter values.
	 *
	 * @param instructionParamMaxLength
	 *        The maximum length.
	 * @since 1.8
	 */
	public final void setInstructionParamMaxLength(int instructionParamMaxLength) {
		this.instructionParamMaxLength = instructionParamMaxLength;
	}

}
