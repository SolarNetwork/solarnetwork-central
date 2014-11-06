/* ==================================================================
 * DaoRegistrationBizTest.java - Nov 30, 2012 6:21:59 AM
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

package net.solarnetwork.central.user.biz.dao.test;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.zip.GZIPInputStream;
import net.solarnetwork.central.dao.SolarLocationDao;
import net.solarnetwork.central.dao.SolarNodeDao;
import net.solarnetwork.central.domain.SolarLocation;
import net.solarnetwork.central.domain.SolarNode;
import net.solarnetwork.central.in.biz.NetworkIdentityBiz;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.security.PasswordEncoder;
import net.solarnetwork.central.user.biz.NodePKIBiz;
import net.solarnetwork.central.user.biz.dao.DaoRegistrationBiz;
import net.solarnetwork.central.user.dao.UserDao;
import net.solarnetwork.central.user.dao.UserNodeCertificateDao;
import net.solarnetwork.central.user.dao.UserNodeConfirmationDao;
import net.solarnetwork.central.user.dao.UserNodeDao;
import net.solarnetwork.central.user.domain.NewNodeRequest;
import net.solarnetwork.central.user.domain.PasswordEntry;
import net.solarnetwork.central.user.domain.User;
import net.solarnetwork.central.user.domain.UserNode;
import net.solarnetwork.central.user.domain.UserNodeCertificate;
import net.solarnetwork.central.user.domain.UserNodeCertificateStatus;
import net.solarnetwork.central.user.domain.UserNodeConfirmation;
import net.solarnetwork.central.user.domain.UserNodePK;
import net.solarnetwork.domain.BasicNetworkIdentity;
import net.solarnetwork.domain.NetworkAssociation;
import net.solarnetwork.domain.NetworkAssociationDetails;
import net.solarnetwork.domain.NetworkCertificate;
import net.solarnetwork.domain.RegistrationReceipt;
import net.solarnetwork.pki.bc.BCCertificateService;
import net.solarnetwork.util.JavaBeanXmlSerializer;
import org.apache.commons.codec.binary.Base64InputStream;
import org.easymock.EasyMock;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for the {@link DaoRegistrationBiz}.
 * 
 * @author matt
 * @version 1.1
 */
public class DaoRegistrationBizTest {

	private static final Long TEST_USER_ID = -1L;
	private static final Long TEST_CONF_ID = -2L;
	private static final Long TEST_NODE_ID = -3L;
	private static final Long TEST_LOC_ID = -4L;
	private static final UserNodePK TEST_CERT_ID = new UserNodePK(TEST_USER_ID, TEST_NODE_ID);
	private static final String TEST_ENC_PASSWORD = "encoded.password";
	private static final String TEST_EMAIL = "test@localhost";
	private static final String TEST_SECURITY_PHRASE = "test phrase";
	private static final String TEST_CONF_KEY = "test conf key";
	private static final String TEST_DN_FORMAT = "UID=%s, OU=Unit Test, O=SolarNetwork";
	private static final String TEST_KEYSTORE_PASS = "test password";
	private static final String TEST_CERT_REQ_ID = "test req ID";

	private SolarLocationDao solarLocationDao;
	private SolarNodeDao nodeDao;
	private UserDao userDao;
	private UserNodeDao userNodeDao;
	private User testUser;
	private UserNodeConfirmationDao userNodeConfirmationDao;
	private UserNodeCertificateDao userNodeCertificateDao;
	private NetworkIdentityBiz networkIdentityBiz;
	private DaoRegistrationBiz registrationBiz;
	private BasicNetworkIdentity networkIdentity;
	private PasswordEncoder passwordEncoder;
	private NodePKIBiz nodePKIBiz;
	private ExecutorService executorService;

	private final BCCertificateService certificateService = new BCCertificateService();

	@Before
	public void setup() {
		networkIdentity = new BasicNetworkIdentity("key", "tos", "host", 80, false);
		networkIdentityBiz = EasyMock.createMock(NetworkIdentityBiz.class);
		testUser = new User();
		testUser.setEmail(TEST_EMAIL);
		testUser.setId(TEST_USER_ID);
		testUser.setCreated(new DateTime());
		testUser.setPassword(TEST_ENC_PASSWORD);
		userDao = EasyMock.createMock(UserDao.class);
		userNodeDao = EasyMock.createMock(UserNodeDao.class);
		userNodeConfirmationDao = EasyMock.createMock(UserNodeConfirmationDao.class);
		userNodeCertificateDao = EasyMock.createMock(UserNodeCertificateDao.class);
		nodeDao = EasyMock.createMock(SolarNodeDao.class);
		solarLocationDao = EasyMock.createMock(SolarLocationDao.class);
		passwordEncoder = EasyMock.createMock(PasswordEncoder.class);
		nodePKIBiz = EasyMock.createMock(NodePKIBiz.class);
		executorService = EasyMock.createMock(ExecutorService.class);
		registrationBiz = new DaoRegistrationBiz();
		registrationBiz.setNetworkIdentityBiz(networkIdentityBiz);
		registrationBiz.setUserDao(userDao);
		registrationBiz.setUserNodeConfirmationDao(userNodeConfirmationDao);
		registrationBiz.setSolarNodeDao(nodeDao);
		registrationBiz.setSolarLocationDao(solarLocationDao);
		registrationBiz.setUserNodeCertificateDao(userNodeCertificateDao);
		registrationBiz.setUserNodeDao(userNodeDao);
		registrationBiz.setNetworkCertificateSubjectDNFormat(TEST_DN_FORMAT);
		registrationBiz.setPasswordEncoder(passwordEncoder);
		registrationBiz.setNodePKIBiz(nodePKIBiz);
		registrationBiz.setExecutorService(executorService);
	}

	private void replayAll() {
		replay(networkIdentityBiz, userDao, userNodeDao, userNodeConfirmationDao,
				userNodeCertificateDao, nodeDao, solarLocationDao, passwordEncoder, nodePKIBiz,
				executorService);
	}

	private void verifyAll() {
		verify(networkIdentityBiz, userDao, userNodeDao, userNodeConfirmationDao,
				userNodeCertificateDao, nodeDao, solarLocationDao, passwordEncoder, nodePKIBiz,
				executorService);
	}

	@Test
	public void createNodeAssociation() throws IOException {
		expect(networkIdentityBiz.getNetworkIdentity()).andReturn(networkIdentity);
		expect(userDao.get(TEST_USER_ID)).andReturn(testUser);
		expect(userNodeConfirmationDao.store(EasyMock.anyObject(UserNodeConfirmation.class))).andReturn(
				TEST_CONF_ID);
		replayAll();

		final NetworkAssociation result = registrationBiz.createNodeAssociation(new NewNodeRequest(
				TEST_USER_ID, TEST_SECURITY_PHRASE, TimeZone.getDefault(), Locale.getDefault()));

		verifyAll();

		assertEquals(networkIdentity.getHost(), result.getHost());
		assertEquals(networkIdentity.getPort(), result.getPort());
		assertEquals(networkIdentity.getIdentityKey(), result.getIdentityKey());
		assertEquals(networkIdentity.getPort(), result.getPort());
		assertEquals(TEST_SECURITY_PHRASE, result.getSecurityPhrase());
		assertEquals(TEST_EMAIL, result.getUsername());
		assertEquals(Boolean.FALSE, Boolean.valueOf(result.isForceTLS()));
		assertEquals(NetworkAssociationDetails.class, result.getClass());
		assertNull(((NetworkAssociationDetails) result).getNetworkId());

		Map<String, Object> detailMap = decodeAssociationDetails(result.getConfirmationKey());
		assertEquals(7, detailMap.size());
		assertNotNull("Confirmation key must be present", detailMap.get("confirmationKey"));
		assertNotNull("Expiration date must be present", detailMap.get("expiration"));
		assertEquals("false", detailMap.get("forceTLS"));
		assertEquals("host", detailMap.get("host"));
		assertEquals("key", detailMap.get("identityKey"));
		assertEquals("80", detailMap.get("port"));
		assertEquals(TEST_EMAIL, detailMap.get("username"));
	}

	@Test
	public void cancelNodeAssociation() {
		final Long testConfId = -1L;
		final UserNodeConfirmation conf = new UserNodeConfirmation();
		conf.setId(testConfId);
		expect(userNodeConfirmationDao.get(testConfId)).andReturn(conf);
		userNodeConfirmationDao.delete(conf);
		replayAll();

		registrationBiz.cancelNodeAssociation(testConfId);

		verifyAll();
	}

	@Test
	public void confirmNodeAssociation() throws IOException {
		final UserNodeConfirmation conf = new UserNodeConfirmation();
		conf.setUser(testUser);
		conf.setCreated(new DateTime());
		conf.setCountry("NZ");
		conf.setTimeZoneId("Pacific/Auckland");
		final SolarLocation loc = new SolarLocation();
		loc.setId(TEST_LOC_ID);
		final UserNode userNode = new UserNode();
		userNode.setId(TEST_NODE_ID);

		final DateTime now = new DateTime();

		// to confirm a node, we must look up the UserNodeConfirmation by userId+key, then
		// create the new SolarNode using a default SolarLocation, followed by
		// a new UserNode and UserNodeCertificate. The original UserNodeConfirmation should
		// have its 

		expect(userDao.getUserByEmail(TEST_EMAIL)).andReturn(testUser);
		expect(userNodeConfirmationDao.getConfirmationForKey(TEST_USER_ID, TEST_CONF_KEY)).andReturn(
				conf);
		expect(solarLocationDao.getSolarLocationForTimeZone(conf.getCountry(), conf.getTimeZoneId()))
				.andReturn(loc);
		expect(nodeDao.getUnusedNodeId()).andReturn(TEST_NODE_ID);
		expect(nodeDao.get(TEST_NODE_ID)).andReturn(null);
		expect(nodeDao.store(EasyMock.anyObject(SolarNode.class))).andReturn(TEST_NODE_ID);
		expect(userNodeDao.get(TEST_NODE_ID)).andReturn(null);
		expect(userNodeDao.store(EasyMock.anyObject(UserNode.class))).andReturn(TEST_NODE_ID);
		expect(userNodeConfirmationDao.store(conf)).andReturn(TEST_NODE_ID);

		replayAll();

		NetworkCertificate cert = registrationBiz.confirmNodeAssociation(TEST_EMAIL, TEST_CONF_KEY);

		verifyAll();

		assertNotNull(cert);
		assertNotNull(cert.getConfirmationKey());
		assertEquals(String.format(TEST_DN_FORMAT, TEST_NODE_ID.toString()),
				cert.getNetworkCertificateSubjectDN());
		assertNull(cert.getNetworkCertificateStatus());
		assertEquals(TEST_NODE_ID, cert.getNetworkId());
		assertNotNull(conf.getConfirmationDate());
		assertNotNull(conf.getNodeId());
		assertFalse("The confirmation date must be >= now", now.isAfter(conf.getConfirmationDate()));
	}

	@Test
	public void confirmNodeAssociationForNewSolarLocation() throws IOException {
		final UserNodeConfirmation conf = new UserNodeConfirmation();
		conf.setUser(testUser);
		conf.setCreated(new DateTime());
		conf.setCountry("NZ");
		conf.setTimeZoneId("Pacific/Auckland");
		final SolarLocation loc = new SolarLocation();
		loc.setId(TEST_LOC_ID);
		loc.setCountry(conf.getCountry());
		loc.setTimeZoneId(conf.getTimeZoneId());
		final UserNode userNode = new UserNode();
		userNode.setId(TEST_NODE_ID);

		final DateTime now = new DateTime();

		// to confirm a node, we must look up the UserNodeConfirmation by userId+key, then
		// create the new SolarNode using a default SolarLocation, followed by
		// a new UserNode and UserNodeCertificate. The original UserNodeConfirmation should
		// have its 

		expect(userDao.getUserByEmail(TEST_EMAIL)).andReturn(testUser);
		expect(userNodeConfirmationDao.getConfirmationForKey(TEST_USER_ID, TEST_CONF_KEY)).andReturn(
				conf);

		// here SolarLocation doesn't exist for the requested country+time zone, so we create it now
		expect(solarLocationDao.getSolarLocationForTimeZone(conf.getCountry(), conf.getTimeZoneId()))
				.andReturn(null);
		expect(solarLocationDao.store(EasyMock.anyObject(SolarLocation.class))).andReturn(TEST_LOC_ID);
		expect(solarLocationDao.get(TEST_LOC_ID)).andReturn(loc);

		expect(nodeDao.getUnusedNodeId()).andReturn(TEST_NODE_ID);
		expect(nodeDao.get(TEST_NODE_ID)).andReturn(null);
		expect(nodeDao.store(EasyMock.anyObject(SolarNode.class))).andReturn(TEST_NODE_ID);
		expect(userNodeDao.get(TEST_NODE_ID)).andReturn(null);
		expect(userNodeDao.store(EasyMock.anyObject(UserNode.class))).andReturn(TEST_NODE_ID);
		expect(userNodeConfirmationDao.store(conf)).andReturn(TEST_NODE_ID);

		replayAll();

		NetworkCertificate cert = registrationBiz.confirmNodeAssociation(TEST_EMAIL, TEST_CONF_KEY);

		verifyAll();

		assertNotNull(cert);
		assertNotNull(cert.getConfirmationKey());
		assertEquals(String.format(TEST_DN_FORMAT, TEST_NODE_ID.toString()),
				cert.getNetworkCertificateSubjectDN());
		assertNull(cert.getNetworkCertificateStatus());
		assertEquals(TEST_NODE_ID, cert.getNetworkId());
		assertNotNull(conf.getConfirmationDate());
		assertNotNull(conf.getNodeId());
		assertFalse("The confirmation date must be >= now", now.isAfter(conf.getConfirmationDate()));
	}

	@Test
	public void confirmNodeAssociationPreassignedNodeId() throws IOException {
		final UserNodeConfirmation conf = new UserNodeConfirmation();
		conf.setUser(testUser);
		conf.setCreated(new DateTime());
		conf.setCountry("NZ");
		conf.setTimeZoneId("Pacific/Auckland");
		conf.setNodeId(TEST_NODE_ID); // pre-assign node ID
		final SolarLocation loc = new SolarLocation();
		loc.setId(TEST_LOC_ID);
		final UserNode userNode = new UserNode();
		userNode.setId(TEST_NODE_ID);

		final DateTime now = new DateTime();

		// to confirm a node, we must look up the UserNodeConfirmation by userId+key, then
		// create the new SolarNode using a default SolarLocation, followed by
		// a new UserNode and UserNodeCertificate. The original UserNodeConfirmation should
		// have its 

		expect(userDao.getUserByEmail(TEST_EMAIL)).andReturn(testUser);
		expect(userNodeConfirmationDao.getConfirmationForKey(TEST_USER_ID, TEST_CONF_KEY)).andReturn(
				conf);
		expect(solarLocationDao.getSolarLocationForTimeZone(conf.getCountry(), conf.getTimeZoneId()))
				.andReturn(loc);
		expect(nodeDao.get(TEST_NODE_ID)).andReturn(null);
		expect(nodeDao.store(EasyMock.anyObject(SolarNode.class))).andReturn(TEST_NODE_ID);
		expect(userNodeDao.get(TEST_NODE_ID)).andReturn(null);
		expect(userNodeDao.store(EasyMock.anyObject(UserNode.class))).andReturn(TEST_NODE_ID);
		expect(userNodeConfirmationDao.store(conf)).andReturn(TEST_NODE_ID);

		replayAll();

		NetworkCertificate cert = registrationBiz.confirmNodeAssociation(TEST_EMAIL, TEST_CONF_KEY);

		verifyAll();

		assertNotNull(cert);
		assertNotNull(cert.getConfirmationKey());
		assertEquals(String.format(TEST_DN_FORMAT, TEST_NODE_ID.toString()),
				cert.getNetworkCertificateSubjectDN());
		assertNull(cert.getNetworkCertificateStatus());
		assertEquals(TEST_NODE_ID, cert.getNetworkId());
		assertNotNull(conf.getConfirmationDate());
		assertEquals(TEST_NODE_ID, conf.getNodeId());
		assertFalse("The confirmation date must be >= now", now.isAfter(conf.getConfirmationDate()));
	}

	@Test
	public void confirmNodeAssociationPrepopulatedNode() throws IOException {
		final UserNodeConfirmation conf = new UserNodeConfirmation();
		conf.setUser(testUser);
		conf.setCreated(new DateTime());
		conf.setCountry("NZ");
		conf.setTimeZoneId("Pacific/Auckland");
		conf.setNodeId(TEST_NODE_ID); // pre-assign node ID
		final SolarNode node = new SolarNode();
		node.setId(TEST_NODE_ID);
		final SolarLocation loc = new SolarLocation();
		loc.setId(TEST_LOC_ID);
		final UserNode userNode = new UserNode();
		userNode.setId(TEST_NODE_ID);

		final DateTime now = new DateTime();

		// to confirm a node, we must look up the UserNodeConfirmation by userId+key, then
		// create the new SolarNode using a default SolarLocation, followed by
		// a new UserNode and UserNodeCertificate. The original UserNodeConfirmation should
		// have its 

		expect(userDao.getUserByEmail(TEST_EMAIL)).andReturn(testUser);
		expect(userNodeConfirmationDao.getConfirmationForKey(TEST_USER_ID, TEST_CONF_KEY)).andReturn(
				conf);
		expect(solarLocationDao.getSolarLocationForTimeZone(conf.getCountry(), conf.getTimeZoneId()))
				.andReturn(loc);
		expect(nodeDao.get(TEST_NODE_ID)).andReturn(node);
		expect(userNodeDao.get(TEST_NODE_ID)).andReturn(null);
		expect(userNodeDao.store(EasyMock.anyObject(UserNode.class))).andReturn(TEST_NODE_ID);
		expect(userNodeConfirmationDao.store(conf)).andReturn(TEST_NODE_ID);

		replayAll();

		NetworkCertificate cert = registrationBiz.confirmNodeAssociation(TEST_EMAIL, TEST_CONF_KEY);

		verifyAll();

		assertNotNull(cert);
		assertNotNull(cert.getConfirmationKey());
		assertEquals(String.format(TEST_DN_FORMAT, TEST_NODE_ID.toString()),
				cert.getNetworkCertificateSubjectDN());
		assertNull(cert.getNetworkCertificateStatus());
		assertEquals(TEST_NODE_ID, cert.getNetworkId());
		assertNotNull(conf.getConfirmationDate());
		assertEquals(TEST_NODE_ID, conf.getNodeId());
		assertFalse("The confirmation date must be >= now", now.isAfter(conf.getConfirmationDate()));
	}

	@Test
	public void confirmNodeAssociationPrepopulatedNodeAndUserNode() throws IOException {
		final UserNodeConfirmation conf = new UserNodeConfirmation();
		conf.setUser(testUser);
		conf.setCreated(new DateTime());
		conf.setCountry("NZ");
		conf.setTimeZoneId("Pacific/Auckland");
		conf.setNodeId(TEST_NODE_ID); // pre-assign node ID
		final SolarNode node = new SolarNode();
		node.setId(TEST_NODE_ID);
		final SolarLocation loc = new SolarLocation();
		loc.setId(TEST_LOC_ID);
		final UserNode userNode = new UserNode();
		userNode.setId(TEST_NODE_ID);

		final DateTime now = new DateTime();

		// to confirm a node, we must look up the UserNodeConfirmation by userId+key, then
		// create the new SolarNode using a default SolarLocation, followed by
		// a new UserNode and UserNodeCertificate. The original UserNodeConfirmation should
		// have its 

		expect(userDao.getUserByEmail(TEST_EMAIL)).andReturn(testUser);
		expect(userNodeConfirmationDao.getConfirmationForKey(TEST_USER_ID, TEST_CONF_KEY)).andReturn(
				conf);
		expect(solarLocationDao.getSolarLocationForTimeZone(conf.getCountry(), conf.getTimeZoneId()))
				.andReturn(loc);
		expect(nodeDao.get(TEST_NODE_ID)).andReturn(node);
		expect(userNodeDao.get(TEST_NODE_ID)).andReturn(userNode);
		expect(userNodeConfirmationDao.store(conf)).andReturn(TEST_NODE_ID);

		replayAll();

		NetworkCertificate cert = registrationBiz.confirmNodeAssociation(TEST_EMAIL, TEST_CONF_KEY);

		verifyAll();

		assertNotNull(cert);
		assertNotNull(cert.getConfirmationKey());
		assertEquals(String.format(TEST_DN_FORMAT, TEST_NODE_ID.toString()),
				cert.getNetworkCertificateSubjectDN());
		assertNull(cert.getNetworkCertificateStatus());
		assertEquals(TEST_NODE_ID, cert.getNetworkId());
		assertNotNull(conf.getConfirmationDate());
		assertEquals(TEST_NODE_ID, conf.getNodeId());
		assertFalse("The confirmation date must be >= now", now.isAfter(conf.getConfirmationDate()));
	}

	@Test
	public void confirmNodeAssociationBadConfirmationKey() {
		final String BAD_CONF_KEY = "bad conf key";
		expect(userDao.getUserByEmail(TEST_EMAIL)).andReturn(testUser);
		expect(userNodeConfirmationDao.getConfirmationForKey(TEST_USER_ID, BAD_CONF_KEY))
				.andReturn(null);

		replayAll();
		try {
			registrationBiz.confirmNodeAssociation(TEST_EMAIL, BAD_CONF_KEY);
			fail("Expected AuthorizationException for bad node ID");
		} catch ( AuthorizationException e ) {
			assertEquals(AuthorizationException.Reason.REGISTRATION_NOT_CONFIRMED, e.getReason());
		}
		verifyAll();
	}

	@Test
	public void confirmNodeAssociationAlreadyConfirmed() throws IOException {
		final UserNodeConfirmation conf = new UserNodeConfirmation();
		conf.setUser(testUser);
		conf.setNodeId(TEST_NODE_ID);
		conf.setCreated(new DateTime());
		conf.setConfirmationDate(new DateTime()); // mark as confirmed

		expect(userDao.getUserByEmail(TEST_EMAIL)).andReturn(testUser);
		expect(userNodeConfirmationDao.getConfirmationForKey(TEST_USER_ID, TEST_CONF_KEY)).andReturn(
				conf);
		replayAll();
		try {
			registrationBiz.confirmNodeAssociation(TEST_EMAIL, TEST_CONF_KEY);
			fail("Expected AuthorizationException for already confirmed");
		} catch ( AuthorizationException e ) {
			assertEquals(AuthorizationException.Reason.REGISTRATION_ALREADY_CONFIRMED, e.getReason());
		}

		verifyAll();
	}

	private Map<String, Object> decodeAssociationDetails(String code) throws IOException {
		JavaBeanXmlSerializer xmlHelper = new JavaBeanXmlSerializer();
		InputStream in = null;
		Map<String, Object> associationData = null;
		try {
			in = new GZIPInputStream(new Base64InputStream(new ByteArrayInputStream(code.getBytes())));
			associationData = xmlHelper.parseXml(in);
		} finally {
			if ( in != null ) {
				try {
					in.close();
				} catch ( IOException e ) {
					// ignore this
				}
			}
		}
		assertNotNull(associationData);
		return associationData;
	}

	@Test
	public void generateResetPasswordReceipt() {
		// must get the existing user to generate the conf code
		expect(userDao.getUserByEmail(TEST_EMAIL)).andReturn(testUser);

		replayAll();

		//BasicRegistrationReceipt receipt = new BasicRegistrationReceipt(TEST_EMAIL, TEST_CONF_KEY);
		RegistrationReceipt receipt = registrationBiz.generateResetPasswordReceipt(TEST_EMAIL);
		assertNotNull("Receipt must not be null", receipt);
		assertEquals("The username should be the email", TEST_EMAIL, receipt.getUsername());

		verifyAll();
	}

	@Test
	public void resetPassword() {
		final PasswordEntry pass = new PasswordEntry("new.test.password");
		final String encodedPass = "new.encoded.password";

		// first generate receipt, then must get the existing user to generate the conf code
		expect(userDao.getUserByEmail(TEST_EMAIL)).andReturn(testUser).times(2);
		expect(passwordEncoder.encode(pass.getPassword())).andReturn(encodedPass);

		// then must save the updated user
		expect(userDao.store(testUser)).andReturn(testUser.getId());

		replayAll();

		RegistrationReceipt receipt = registrationBiz.generateResetPasswordReceipt(TEST_EMAIL);
		registrationBiz.resetPassword(receipt, pass);

		verifyAll();

		assertEquals("The user's password should be changed", encodedPass, testUser.getPassword());
	}

	@Test
	public void confirmNodeAssociationWithCertificate() throws IOException, NoSuchAlgorithmException {
		final UserNodeConfirmation conf = new UserNodeConfirmation();
		conf.setUser(testUser);
		conf.setCreated(new DateTime());
		conf.setCountry("NZ");
		conf.setTimeZoneId("Pacific/Auckland");
		final SolarLocation loc = new SolarLocation();
		loc.setId(TEST_LOC_ID);
		loc.setCountry(conf.getCountry());
		loc.setTimeZoneId(conf.getTimeZoneId());
		final UserNode userNode = new UserNode();
		userNode.setId(TEST_NODE_ID);

		final String nodeSubjectDN = String.format(TEST_DN_FORMAT, TEST_NODE_ID.toString());

		final KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
		keyGen.initialize(1024, new SecureRandom());
		final KeyPair keypair = keyGen.generateKeyPair();

		final X509Certificate certificate = certificateService.generateCertificate(nodeSubjectDN,
				keypair.getPublic(), keypair.getPrivate());

		final DateTime now = new DateTime();

		// to confirm a node, we must look up the UserNodeConfirmation by userId+key, then
		// create the new SolarNode using a default SolarLocation, followed by
		// a new UserNode and UserNodeCertificate. The original UserNodeConfirmation should
		// have its 

		expect(userDao.getUserByEmail(TEST_EMAIL)).andReturn(testUser);
		expect(userNodeConfirmationDao.getConfirmationForKey(TEST_USER_ID, TEST_CONF_KEY)).andReturn(
				conf);

		// here SolarLocation doesn't exist for the requested country+time zone, so we create it now
		expect(solarLocationDao.getSolarLocationForTimeZone(conf.getCountry(), conf.getTimeZoneId()))
				.andReturn(null);
		expect(solarLocationDao.store(EasyMock.anyObject(SolarLocation.class))).andReturn(TEST_LOC_ID);
		expect(solarLocationDao.get(TEST_LOC_ID)).andReturn(loc);

		expect(nodeDao.getUnusedNodeId()).andReturn(TEST_NODE_ID);
		expect(nodeDao.get(TEST_NODE_ID)).andReturn(null);
		expect(nodeDao.store(EasyMock.anyObject(SolarNode.class))).andReturn(TEST_NODE_ID);
		expect(userNodeDao.get(TEST_NODE_ID)).andReturn(null);
		expect(userNodeDao.store(EasyMock.anyObject(UserNode.class))).andReturn(TEST_NODE_ID);
		expect(userNodeConfirmationDao.store(conf)).andReturn(TEST_NODE_ID);
		expect(
				nodePKIBiz.generateCertificate(EasyMock.eq(nodeSubjectDN),
						EasyMock.anyObject(PublicKey.class), EasyMock.anyObject(PrivateKey.class)))
				.andReturn(certificate);
		expect(nodePKIBiz.submitCSR(EasyMock.eq(certificate), EasyMock.anyObject(PrivateKey.class)))
				.andReturn(TEST_CERT_REQ_ID);
		expect(userNodeCertificateDao.store(EasyMock.anyObject(UserNodeCertificate.class))).andReturn(
				TEST_CERT_ID);
		expect(executorService.submit(EasyMock.anyObject(Runnable.class))).andReturn(null);

		replayAll();

		NetworkAssociationDetails req = new NetworkAssociationDetails(TEST_EMAIL, TEST_CONF_KEY,
				TEST_KEYSTORE_PASS);
		NetworkCertificate cert = registrationBiz.confirmNodeAssociation(req);

		verifyAll();

		assertNotNull(cert);
		assertNotNull(cert.getConfirmationKey());
		assertEquals(String.format(TEST_DN_FORMAT, TEST_NODE_ID.toString()),
				cert.getNetworkCertificateSubjectDN());
		assertEquals(UserNodeCertificateStatus.a.getValue(), cert.getNetworkCertificateStatus());
		assertEquals(TEST_NODE_ID, cert.getNetworkId());
		assertNotNull(conf.getConfirmationDate());
		assertNotNull(conf.getNodeId());
		assertFalse("The confirmation date must be >= now", now.isAfter(conf.getConfirmationDate()));
	}
}
