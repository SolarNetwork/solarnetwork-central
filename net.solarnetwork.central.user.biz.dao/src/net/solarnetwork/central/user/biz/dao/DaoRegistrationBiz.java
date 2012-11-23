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
 * $Id$
 * ==================================================================
 */

package net.solarnetwork.central.user.biz.dao;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.zip.GZIPOutputStream;
import net.solarnetwork.central.ValidationException;
import net.solarnetwork.central.dao.SolarLocationDao;
import net.solarnetwork.central.dao.SolarNodeDao;
import net.solarnetwork.central.domain.SolarLocation;
import net.solarnetwork.central.domain.SolarNode;
import net.solarnetwork.central.in.biz.NetworkIdentityBiz;
import net.solarnetwork.central.user.biz.AuthorizationException;
import net.solarnetwork.central.user.biz.AuthorizationException.Reason;
import net.solarnetwork.central.user.biz.RegistrationBiz;
import net.solarnetwork.central.user.biz.UserBiz;
import net.solarnetwork.central.user.dao.UserDao;
import net.solarnetwork.central.user.dao.UserNodeConfirmationDao;
import net.solarnetwork.central.user.dao.UserNodeDao;
import net.solarnetwork.central.user.domain.User;
import net.solarnetwork.central.user.domain.UserNode;
import net.solarnetwork.central.user.domain.UserNodeConfirmation;
import net.solarnetwork.domain.BasicRegistrationReceipt;
import net.solarnetwork.domain.NetworkAssociationDetails;
import net.solarnetwork.domain.NetworkIdentity;
import net.solarnetwork.domain.RegistrationReceipt;
import net.solarnetwork.util.JavaBeanXmlSerializer;
import org.apache.commons.codec.binary.Base64OutputStream;
import org.apache.commons.codec.digest.DigestUtils;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

/**
 * DAO-based implementation of {@link RegistrationBiz}.
 * 
 * <p>The configurable properties of this class are:</p>
 * 
 * <dl class="class-properties">
 *   <dt>userDao</dt>
 *   <dd>The {@link UserDao} to use for persisting users.</dd>
 *   
 *   <dt>userValidator</dt>
 *   <dd>A {@link Validator} to use for validating user objects.</dd>
 * </dl>
 * 
 * @author matt
 * @version $Id$
 */
@Service
public class DaoRegistrationBiz implements RegistrationBiz, UserBiz {

	public static final SortedSet<String> DEFAULT_CONFIRMED_USER_ROLES
		= Collections.unmodifiableSortedSet(new TreeSet<String>(
				Arrays.asList("ROLE_USER")));
	
	private static final String UNCONFIRMED_EMAIL_PREFIX = "UNCONFIRMED@";

	@Autowired private UserDao userDao;
	@Autowired private UserNodeDao userNodeDao;
	@Autowired private UserNodeConfirmationDao userNodeConfirmationDao;
	@Autowired private Validator userValidator;
	@Autowired private SolarNodeDao solarNodeDao;
	@Autowired private SolarLocationDao solarLocationDao;
	@Autowired private NetworkIdentityBiz networkIdentityBiz;

	@Autowired(required = false)
	private Set<String> confirmedUserRoles = DEFAULT_CONFIRMED_USER_ROLES; 
	
	@Autowired(required = false)
	private final JavaBeanXmlSerializer xmlSerializer = new JavaBeanXmlSerializer();
	
	private Period invitationExpirationPeriod = new Period(0, 0, 1, 0, 0, 0, 0, 0); // 1 week
	private String defaultSolarLocationName = "Unknown";
	
	private final Logger log = LoggerFactory.getLogger(DaoRegistrationBiz.class);
	
	private User getCurrentUser() {
		String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
		User user = null;
		if ( currentUserEmail != null ) {
			user = userDao.getUserByEmail(currentUserEmail);
		}
		return user;
	}

	@Override
	public RegistrationReceipt createReceipt(String username,
			String confirmationCode) {
		return new BasicRegistrationReceipt(username, confirmationCode);
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public RegistrationReceipt registerUser(User user)
	throws AuthorizationException {
		
		// perform service-side validation
		if ( this.userValidator != null ) {
			Errors errors = new BindException(user, "user");
			this.userValidator.validate(user,errors);
			if ( errors.hasErrors() ) {
				throw new ValidationException(errors);
			}
		}
		
		User clone = (User)user.clone();
		
		// store user
		prepareUserForStorage(clone);
		
		// adjust email so we know they are not confirmed
		clone.setEmail(getUnconfirmedEmail(clone.getEmail()));
		
		User entity;
		try {
			entity = userDao.get(userDao.store(clone));
		} catch ( DataIntegrityViolationException e ) {
			if ( log.isWarnEnabled() ) {
				log.warn("Duplicate user registration: " +clone.getEmail());
			}
			throw new AuthorizationException(user.getEmail(), 
					AuthorizationException.Reason.DUPLICATE_EMAIL);
		}
		
		// generate confirmation string
		String conf = calculateConfirmationCode(entity);
		if ( log.isInfoEnabled() ) {
			log.info("Registered user '" +entity.getEmail() 
					+"' with confirmation '" +conf +"'");
		}
		
		return new BasicRegistrationReceipt(user.getEmail(), conf);
	}
	
	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public User confirmRegisteredUser(RegistrationReceipt receipt)
	throws AuthorizationException {
		// first check if already registered
		User entity = userDao.getUserByEmail(receipt.getUsername());
		if ( entity != null ) {
			throw new AuthorizationException(receipt.getUsername(),
					AuthorizationException.Reason.REGISTRATION_ALREADY_CONFIRMED);
		}
		
		String unregEmail = getUnconfirmedEmail(receipt.getUsername());
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
		entity.setEmail(receipt.getUsername());
		
		// update confirmed user
		entity = userDao.get(userDao.store(entity));
		
		// store initial user roles
		userDao.storeUserRoles(entity, confirmedUserRoles);
		entity.setRoles(userDao.getUserRoles(entity));
		
		return entity;
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	public User logonUser(String email, String password)
	throws AuthorizationException {
		if ( log.isInfoEnabled() ) {
			log.info("Login attempt: " +email);
		}
		
		// do not allow logon attempt of unconfirmed email
		if ( isUnconfirmedEmail(email) ) {
			throw new AuthorizationException(email, AuthorizationException.Reason.UNKNOWN_EMAIL);
		}
		
		User entity = userDao.getUserByEmail(email);
		if ( entity == null ) {
			// first check if user waiting confirmation still
			String unconfirmedEmail = getUnconfirmedEmail(email);
			entity = userDao.getUserByEmail(unconfirmedEmail);
			if ( entity != null ) {
				throw new AuthorizationException(email, 
						AuthorizationException.Reason.REGISTRATION_NOT_CONFIRMED);
			}
			throw new AuthorizationException(email, AuthorizationException.Reason.UNKNOWN_EMAIL);
		}
		
		String encPassword = encryptPassword(password);
		if ( !entity.getPassword().equals(encPassword) ) {
			throw new AuthorizationException(email, AuthorizationException.Reason.BAD_PASSWORD);
		}
		
		if ( log.isInfoEnabled() ) {
			log.info("Login successful: " +email);
		}
		
		// populate roles
		entity.setRoles(userDao.getUserRoles(entity));
		
		return entity;
	}
	
	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public User getUser(Long id) {
		return userDao.get(id);
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public User getUser(String email) {
		return userDao.getUserByEmail(email);
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public List<UserNode> getUserNodes(Long userId) {
		return userNodeDao.findUserNodesForUser(userDao.get(userId));
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public List<UserNodeConfirmation> getPendingUserNodeConfirmations(Long userId) {
		User user = userDao.get(userId);
		return userNodeConfirmationDao.findPendingConfirmationsForUser(user);
	}

	private boolean isUnconfirmedEmail(String email) {
		// validate email starts with unconfirmed key and also contains
		// another @ character, in case somebody does have an email name
		// the same as our unconfirmed key
		return email != null 
			&& email.startsWith(UNCONFIRMED_EMAIL_PREFIX) 
			&& email.length() > UNCONFIRMED_EMAIL_PREFIX.length()
			&& email.indexOf('@', UNCONFIRMED_EMAIL_PREFIX.length()) != -1;
	}

	private String getUnconfirmedEmail(String email) {
		return UNCONFIRMED_EMAIL_PREFIX +email;
	}
	
	private String calculateConfirmationCode(User user) {
		return DigestUtils.sha256Hex(
				user.getCreated().getMillis()
				+user.getId()
				+user.getEmail()
				+user.getPassword());
	}

	private void prepareUserForStorage(User user) throws AuthorizationException {
		
		// check for "unchanged" password value
		if ( user.getId() != null && DO_NOT_CHANGE_VALUE.equals(user.getPassword()) ) {
			// retrieve user from back-end and copy that password onto our user
			User realUser = userDao.get(user.getId());
			user.setPassword(realUser.getPassword());
		}
		
		// check password is encrypted
		if ( user.getPassword() != null && !user.getPassword().startsWith("{SHA}") ) {
			// encrypt the password now
			String encryptedPass = encryptPassword(user.getPassword());
			user.setPassword(encryptedPass);
		}
		if ( user.getCreated() == null ) {
			user.setCreated(new DateTime());
		}

		// verify email not already in use
		User existingUser = userDao.getUserByEmail(user.getEmail());
		if ( existingUser != null && !existingUser.getId().equals(user.getId())) {
			throw new AuthorizationException(user.getEmail(),
					AuthorizationException.Reason.DUPLICATE_EMAIL);
		}

		// sent enabled if not configured
		if ( user.getEnabled() == null ) {
			user.setEnabled(Boolean.TRUE);
		}
	}
	
	private String encryptPassword(String password) {
		return password == null ? null : "{SHA}"+DigestUtils.sha256Hex(password);
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
	public NetworkAssociationDetails createNodeAssociation(User user) {
		if ( user == null ) {
			user = getCurrentUser();
		}
		assert user != null;

		DateTime now = new DateTime();
		NetworkIdentity ident = networkIdentityBiz.getNetworkIdentity();

		Long nodeId = solarNodeDao.getUnusedNodeId();
		NetworkAssociationDetails details = new NetworkAssociationDetails();
		details.setNodeId(nodeId);
		details.setIdentity(ident.getIdentityKey());
		details.setTos(ident.getTermsOfService());
		details.setUsername(user.getEmail());
		details.setExpiration(now.plus(invitationExpirationPeriod).toDate());
		String confKey = DigestUtils.sha256Hex(
				String.valueOf(now.getMillis())
				+ nodeId
				+ details.getIdentity()
				+ details.getTos()
				+ details.getUsername()
				+ details.getExpiration());
		details.setConfirmationKey(confKey);
		String xml = encodeNetworkAssociationDetails(details);
		details.setConfirmationKey(xml);
		
		// create UserNodeConfirmation now
		UserNodeConfirmation conf = new UserNodeConfirmation();
		conf.setCreated(now);
		conf.setUser(user);
		conf.setConfirmationKey(confKey);
		conf.setNodeId(nodeId);
		userNodeConfirmationDao.store(conf);
		
		return details;
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public RegistrationReceipt confirmNodeAssociation(User user, Long nodeId,
			String confirmationKey) {
		assert user != null;
		assert nodeId != null;
		assert confirmationKey != null;
		
		UserNodeConfirmation conf = userNodeConfirmationDao.getConfirmationForKey(
				nodeId, confirmationKey);
		if ( conf == null ) {
			log.info("Node association failed: UserNodeConfirmation not found for nodeId {} confirmationKey {}",
					nodeId, confirmationKey);
			throw new AuthorizationException(String.valueOf(nodeId),
					AuthorizationException.Reason.REGISTRATION_NOT_CONFIRMED);
		}
		
		// security check: user must be the same that invited node
		if ( !user.equals(conf.getUser()) ) {
			log.info("Node association failed: UserNodeConfirmation user {} != confirming user {}",
					conf.getUser().getId(), user.getId());
			throw new AuthorizationException(String.valueOf(nodeId),
					AuthorizationException.Reason.REGISTRATION_NOT_CONFIRMED);
		}
		
		// security check: must not be expired
		DateTime expiry = conf.getCreated().plus(invitationExpirationPeriod);
		if ( expiry.isBeforeNow() ) {
			log.info("Node association failed: confirmation expired on {}", expiry);
			throw new AuthorizationException(String.valueOf(nodeId),
					AuthorizationException.Reason.REGISTRATION_NOT_CONFIRMED);
		}
		
		// security check: already confirmed?
		if ( conf.getConfirmationDate() != null ) {
			log.info("Node association failed: confirmation already confirmed on {}", 
					conf.getConfirmationDate());
			throw new AuthorizationException(String.valueOf(nodeId),
					AuthorizationException.Reason.REGISTRATION_ALREADY_CONFIRMED);
		}
		
		// security check: node should not exist
		SolarNode node = solarNodeDao.get(nodeId);
		if ( node != null ) {
			log.info("Node association failed: node {} already exists", 
					conf.getConfirmationDate());
			throw new AuthorizationException(String.valueOf(nodeId),
					AuthorizationException.Reason.ACCESS_DENIED);
		}
	
		// confirmed! set up details as such
		
		// create SolarNode now
		SolarLocation loc = solarLocationDao.getSolarLocationForName(defaultSolarLocationName);
		assert loc != null;
		
		node = new SolarNode();
		node.setId(nodeId);
		node.setLocation(loc);
		solarNodeDao.store(node);
		
		// create UserNode now
		UserNode userNode = new UserNode();
		userNode.setNode(node);
		userNode.setUser(user);
		userNodeDao.store(userNode);
		
		conf.setConfirmationDate(new DateTime());
		// TODO: generate confirmation code some how... not just this way?
		String code = String.valueOf(conf.getConfirmationDate().getMillis())
				+String.valueOf(user.getId()) +String.valueOf(nodeId);
		conf.setConfirmationValue(code);
		
		userNodeConfirmationDao.store(conf);
		
		return new BasicRegistrationReceipt(conf.getUser().getEmail(), code);
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
	
}
