/* ==================================================================
 * DaoRegistrationBizTests.java - Nov 30, 2012 6:21:59 AM
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

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.zip.GZIPInputStream;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Base64InputStream;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import net.solarnetwork.central.biz.NetworkIdentificationBiz;
import net.solarnetwork.central.dao.SolarLocationDao;
import net.solarnetwork.central.dao.SolarNodeDao;
import net.solarnetwork.central.domain.SolarLocation;
import net.solarnetwork.central.domain.SolarNode;
import net.solarnetwork.central.instructor.biz.InstructorBiz;
import net.solarnetwork.central.instructor.domain.Instruction;
import net.solarnetwork.central.instructor.domain.InstructionParameter;
import net.solarnetwork.central.instructor.domain.NodeInstruction;
import net.solarnetwork.central.security.AuthenticatedNode;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.security.AuthorizationException.Reason;
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
import net.solarnetwork.central.user.domain.UserNodeCertificateInstallationStatus;
import net.solarnetwork.central.user.domain.UserNodeCertificateRenewal;
import net.solarnetwork.central.user.domain.UserNodeCertificateStatus;
import net.solarnetwork.central.user.domain.UserNodeConfirmation;
import net.solarnetwork.central.user.domain.UserNodePK;
import net.solarnetwork.codec.JavaBeanXmlSerializer;
import net.solarnetwork.domain.BasicNetworkIdentity;
import net.solarnetwork.domain.InstructionStatus.InstructionState;
import net.solarnetwork.domain.NetworkAssociation;
import net.solarnetwork.domain.NetworkAssociationDetails;
import net.solarnetwork.domain.NetworkCertificate;
import net.solarnetwork.domain.RegistrationReceipt;
import net.solarnetwork.pki.bc.BCCertificateService;
import net.solarnetwork.service.CertificateException;
import net.solarnetwork.service.PasswordEncoder;

/**
 * Unit tests for the {@link DaoRegistrationBiz}.
 * 
 * @author matt
 * @version 2.1
 */
public class DaoRegistrationBizTests {

	private static final Long TEST_USER_ID = -1L;
	private static final Long TEST_CONF_ID = -2L;
	private static final Long TEST_NODE_ID = -3L;
	private static final Long TEST_LOC_ID = -4L;
	private static final Long TEST_INSTRUCTION_ID = -5L;
	private static final UserNodePK TEST_CERT_ID = new UserNodePK(TEST_USER_ID, TEST_NODE_ID);
	private static final String TEST_CA_DN = "CN=Test CA, O=SolarTest";
	private static final String TEST_ENC_PASSWORD = "encoded.password";
	private static final String TEST_EMAIL = "test@localhost";
	private static final String TEST_SECURITY_PHRASE = "test phrase";
	private static final String TEST_CONF_KEY = "test conf key";
	private static final String TEST_DN_FORMAT = "UID=%s, OU=Unit Test, O=SolarNetwork";
	private static final String TEST_KEYSTORE_PASS = "test password";
	private static final String TEST_CERT_REQ_ID = "test req ID";
	private static final String TEST_CERT_RENEWAL_ID = "test renewal ID";

	private SolarLocationDao solarLocationDao;
	private SolarNodeDao nodeDao;
	private UserDao userDao;
	private UserNodeDao userNodeDao;
	private User testUser;
	private UserNodeConfirmationDao userNodeConfirmationDao;
	private UserNodeCertificateDao userNodeCertificateDao;
	private NetworkIdentificationBiz networkIdentityBiz;
	private DaoRegistrationBiz registrationBiz;
	private BasicNetworkIdentity networkIdentity;
	private PasswordEncoder passwordEncoder;
	private NodePKIBiz nodePKIBiz;
	private ExecutorService executorService;
	private InstructorBiz instructorBiz;

	private final BCCertificateService certificateService = new BCCertificateService();

	@Before
	public void setup() {
		networkIdentity = new BasicNetworkIdentity("key", "tos", "host", 80, false);
		networkIdentityBiz = EasyMock.createMock(NetworkIdentificationBiz.class);
		testUser = new User();
		testUser.setEmail(TEST_EMAIL);
		testUser.setId(TEST_USER_ID);
		testUser.setCreated(Instant.now());
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
		instructorBiz = EasyMock.createMock(InstructorBiz.class);
		registrationBiz = new DaoRegistrationBiz();
		registrationBiz.setNetworkIdentificationBiz(networkIdentityBiz);
		registrationBiz.setUserDao(userDao);
		registrationBiz.setUserNodeConfirmationDao(userNodeConfirmationDao);
		registrationBiz.setSolarNodeDao(nodeDao);
		registrationBiz.setSolarLocationDao(solarLocationDao);
		registrationBiz.setUserNodeCertificateDao(userNodeCertificateDao);
		registrationBiz.setUserNodeDao(userNodeDao);
		registrationBiz.setNetworkCertificateSubjectFormat(TEST_DN_FORMAT);
		registrationBiz.setPasswordEncoder(passwordEncoder);
		registrationBiz.setNodePKIBiz(nodePKIBiz);
		registrationBiz.setExecutorService(executorService);
		registrationBiz.setCertificateService(certificateService);
		registrationBiz.setNodeCertificateRenewalPeriod(null); // disable renew period check
		registrationBiz.setInstructorBiz(instructorBiz);
	}

	private void replayAll() {
		replay(networkIdentityBiz, userDao, userNodeDao, userNodeConfirmationDao, userNodeCertificateDao,
				nodeDao, solarLocationDao, passwordEncoder, nodePKIBiz, executorService, instructorBiz);
	}

	private void verifyAll() {
		verify(networkIdentityBiz, userDao, userNodeDao, userNodeConfirmationDao, userNodeCertificateDao,
				nodeDao, solarLocationDao, passwordEncoder, nodePKIBiz, executorService, instructorBiz);
	}

	@Test
	public void registerNewUser() {
		final String encodedPass = "encrypted.password";
		final User newUser = (User) testUser.clone();
		newUser.setId(-2L);
		newUser.setEmail("unconfirmed@" + TEST_EMAIL);

		expect(passwordEncoder.isPasswordEncrypted(testUser.getPassword())).andReturn(Boolean.FALSE);
		expect(passwordEncoder.encode(testUser.getPassword())).andReturn(encodedPass);
		expect(userDao.getUserByEmail(testUser.getEmail())).andReturn(null);
		expect(userDao.store(EasyMock.anyObject(User.class))).andReturn(newUser.getId());
		expect(userDao.get(newUser.getId())).andReturn(newUser);
		replayAll();

		final RegistrationReceipt receipt = registrationBiz.registerUser(testUser);
		assertNotNull(receipt);
		assertNotNull(receipt.getConfirmationCode());
		assertEquals(newUser.getEmail(), receipt.getUsername());

		verifyAll();
	}

	@Test
	public void registerDuplicateNewUser() {
		final String encodedPass = "encrypted.password";
		final User existingUser = (User) testUser.clone();
		existingUser.setId(-2L);
		existingUser.setEmail(TEST_EMAIL);

		expect(passwordEncoder.isPasswordEncrypted(testUser.getPassword())).andReturn(Boolean.FALSE);
		expect(passwordEncoder.encode(testUser.getPassword())).andReturn(encodedPass);
		expect(userDao.getUserByEmail(testUser.getEmail())).andReturn(existingUser);
		replayAll();

		try {
			registrationBiz.registerUser(testUser);
			fail("Expected AuthorizationException for duplicate user");
		} catch ( AuthorizationException e ) {
			assertEquals(e.getEmail(), testUser.getEmail());
			assertEquals(e.getReason(), Reason.DUPLICATE_EMAIL);
		}

		verifyAll();
	}

	@Test
	public void createNodeAssociation() throws IOException {
		expect(networkIdentityBiz.getNetworkIdentity()).andReturn(networkIdentity);
		expect(userDao.get(TEST_USER_ID)).andReturn(testUser);
		expect(userNodeConfirmationDao.store(EasyMock.anyObject(UserNodeConfirmation.class)))
				.andReturn(TEST_CONF_ID);
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

	@SuppressWarnings("unchecked")
	@Test
	public void createNewNodeManually() throws Exception {
		// given
		expect(userDao.get(TEST_USER_ID)).andReturn(testUser);

		expect(nodeDao.getUnusedNodeId()).andReturn(TEST_NODE_ID);

		final SolarLocation loc = new SolarLocation();
		loc.setId(TEST_LOC_ID);
		loc.setCountry("NZ");
		loc.setTimeZoneId("Pacific/Auckland");
		expect(solarLocationDao.getSolarLocationForTimeZone(loc.getCountry(), loc.getTimeZoneId()))
				.andReturn(loc);

		expect(nodeDao.get(TEST_NODE_ID)).andReturn(null);
		expect(nodeDao.store(EasyMock.anyObject(SolarNode.class))).andReturn(TEST_NODE_ID);

		expect(userNodeDao.get(TEST_NODE_ID)).andReturn(null);
		expect(userNodeDao.store(EasyMock.anyObject(UserNode.class))).andReturn(TEST_NODE_ID);

		final String nodeSubjectDN = String.format(TEST_DN_FORMAT, TEST_NODE_ID.toString());

		final KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
		keyGen.initialize(1024, new SecureRandom());
		final KeyPair keypair = keyGen.generateKeyPair();

		final X509Certificate certificate = certificateService.generateCertificate(nodeSubjectDN,
				keypair.getPublic(), keypair.getPrivate());
		expect(nodePKIBiz.generateCertificate(eq(nodeSubjectDN), anyObject(PublicKey.class),
				anyObject(PrivateKey.class))).andReturn(certificate);
		expect(nodePKIBiz.submitCSR(EasyMock.eq(certificate), EasyMock.anyObject(PrivateKey.class)))
				.andReturn(TEST_CERT_REQ_ID);
		expect(userNodeCertificateDao.store(anyObject(UserNodeCertificate.class)))
				.andReturn(TEST_CERT_ID);

		final UserNodeCertificate userNodeCertificate = new UserNodeCertificate();
		userNodeCertificate.setUser(testUser);
		userNodeCertificate.setStatus(UserNodeCertificateStatus.a);
		expect(executorService.submit(anyObject(Callable.class)))
				.andReturn(new Future<UserNodeCertificate>() {

					@Override
					public boolean cancel(boolean mayInterruptIfRunning) {
						return false;
					}

					@Override
					public boolean isCancelled() {
						return false;
					}

					@Override
					public boolean isDone() {
						return true;
					}

					@Override
					public UserNodeCertificate get() throws InterruptedException, ExecutionException {
						return userNodeCertificate;
					}

					@Override
					public UserNodeCertificate get(long timeout, TimeUnit unit)
							throws InterruptedException, ExecutionException, TimeoutException {
						return userNodeCertificate;
					}
				});

		// when
		replayAll();

		NewNodeRequest req = new NewNodeRequest(testUser.getId(), "foobar",
				TimeZone.getTimeZone(loc.getTimeZoneId()), Locale.of("en", loc.getCountry()));
		UserNode result = registrationBiz.createNodeManually(req);

		// then
		assertThat("UserNode created", result, notNullValue());
		assertThat("UserNode user", result.getUser(), sameInstance(testUser));
		assertThat("UserNode certificate returned", result.getCertificate(),
				sameInstance(userNodeCertificate));
	}

	@Test
	public void confirmNodeAssociation() throws IOException {
		final UserNodeConfirmation conf = new UserNodeConfirmation();
		conf.setUser(testUser);
		conf.setCreated(Instant.now());
		conf.setCountry("NZ");
		conf.setTimeZoneId("Pacific/Auckland");
		final SolarLocation loc = new SolarLocation();
		loc.setId(TEST_LOC_ID);
		final UserNode userNode = new UserNode();
		userNode.setId(TEST_NODE_ID);

		final Instant now = Instant.now();

		// to confirm a node, we must look up the UserNodeConfirmation by userId+key, then
		// create the new SolarNode using a default SolarLocation, followed by
		// a new UserNode and UserNodeCertificate. The original UserNodeConfirmation should
		// have its 

		expect(userDao.getUserByEmail(TEST_EMAIL)).andReturn(testUser);
		expect(userNodeConfirmationDao.getConfirmationForKey(TEST_USER_ID, TEST_CONF_KEY))
				.andReturn(conf);
		expect(solarLocationDao.getSolarLocationForTimeZone(conf.getCountry(), conf.getTimeZoneId()))
				.andReturn(loc);
		expect(nodeDao.getUnusedNodeId()).andReturn(TEST_NODE_ID);
		expect(nodeDao.get(TEST_NODE_ID)).andReturn(null);
		expect(nodeDao.store(EasyMock.anyObject(SolarNode.class))).andReturn(TEST_NODE_ID);
		expect(userNodeDao.get(TEST_NODE_ID)).andReturn(null);
		expect(userNodeDao.store(EasyMock.anyObject(UserNode.class))).andReturn(TEST_NODE_ID);
		expect(userNodeConfirmationDao.store(conf)).andReturn(TEST_NODE_ID);

		replayAll();

		NetworkAssociationDetails details = new NetworkAssociationDetails();
		details.setUsername(TEST_EMAIL);
		details.setConfirmationKey(TEST_CONF_KEY);
		NetworkCertificate cert = registrationBiz.confirmNodeAssociation(details);

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
		conf.setCreated(Instant.now());
		conf.setCountry("NZ");
		conf.setTimeZoneId("Pacific/Auckland");
		final SolarLocation loc = new SolarLocation();
		loc.setId(TEST_LOC_ID);
		loc.setCountry(conf.getCountry());
		loc.setTimeZoneId(conf.getTimeZoneId());
		final UserNode userNode = new UserNode();
		userNode.setId(TEST_NODE_ID);

		final Instant now = Instant.now();

		// to confirm a node, we must look up the UserNodeConfirmation by userId+key, then
		// create the new SolarNode using a default SolarLocation, followed by
		// a new UserNode and UserNodeCertificate. The original UserNodeConfirmation should
		// have its 

		expect(userDao.getUserByEmail(TEST_EMAIL)).andReturn(testUser);
		expect(userNodeConfirmationDao.getConfirmationForKey(TEST_USER_ID, TEST_CONF_KEY))
				.andReturn(conf);

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

		NetworkAssociationDetails details = new NetworkAssociationDetails();
		details.setUsername(TEST_EMAIL);
		details.setConfirmationKey(TEST_CONF_KEY);
		NetworkCertificate cert = registrationBiz.confirmNodeAssociation(details);

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
		conf.setCreated(Instant.now());
		conf.setCountry("NZ");
		conf.setTimeZoneId("Pacific/Auckland");
		conf.setNodeId(TEST_NODE_ID); // pre-assign node ID
		final SolarLocation loc = new SolarLocation();
		loc.setId(TEST_LOC_ID);
		final UserNode userNode = new UserNode();
		userNode.setId(TEST_NODE_ID);

		final Instant now = Instant.now();

		// to confirm a node, we must look up the UserNodeConfirmation by userId+key, then
		// create the new SolarNode using a default SolarLocation, followed by
		// a new UserNode and UserNodeCertificate. The original UserNodeConfirmation should
		// have its 

		expect(userDao.getUserByEmail(TEST_EMAIL)).andReturn(testUser);
		expect(userNodeConfirmationDao.getConfirmationForKey(TEST_USER_ID, TEST_CONF_KEY))
				.andReturn(conf);
		expect(solarLocationDao.getSolarLocationForTimeZone(conf.getCountry(), conf.getTimeZoneId()))
				.andReturn(loc);
		expect(nodeDao.get(TEST_NODE_ID)).andReturn(null);
		expect(nodeDao.store(EasyMock.anyObject(SolarNode.class))).andReturn(TEST_NODE_ID);
		expect(userNodeDao.get(TEST_NODE_ID)).andReturn(null);
		expect(userNodeDao.store(EasyMock.anyObject(UserNode.class))).andReturn(TEST_NODE_ID);
		expect(userNodeConfirmationDao.store(conf)).andReturn(TEST_NODE_ID);

		replayAll();

		NetworkAssociationDetails details = new NetworkAssociationDetails();
		details.setUsername(TEST_EMAIL);
		details.setConfirmationKey(TEST_CONF_KEY);
		NetworkCertificate cert = registrationBiz.confirmNodeAssociation(details);

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
		conf.setCreated(Instant.now());
		conf.setCountry("NZ");
		conf.setTimeZoneId("Pacific/Auckland");
		conf.setNodeId(TEST_NODE_ID); // pre-assign node ID
		final SolarNode node = new SolarNode();
		node.setId(TEST_NODE_ID);
		final SolarLocation loc = new SolarLocation();
		loc.setId(TEST_LOC_ID);
		final UserNode userNode = new UserNode();
		userNode.setId(TEST_NODE_ID);

		final Instant now = Instant.now();

		// to confirm a node, we must look up the UserNodeConfirmation by userId+key, then
		// create the new SolarNode using a default SolarLocation, followed by
		// a new UserNode and UserNodeCertificate. The original UserNodeConfirmation should
		// have its 

		expect(userDao.getUserByEmail(TEST_EMAIL)).andReturn(testUser);
		expect(userNodeConfirmationDao.getConfirmationForKey(TEST_USER_ID, TEST_CONF_KEY))
				.andReturn(conf);
		expect(solarLocationDao.getSolarLocationForTimeZone(conf.getCountry(), conf.getTimeZoneId()))
				.andReturn(loc);
		expect(nodeDao.get(TEST_NODE_ID)).andReturn(node);
		expect(userNodeDao.get(TEST_NODE_ID)).andReturn(null);
		expect(userNodeDao.store(EasyMock.anyObject(UserNode.class))).andReturn(TEST_NODE_ID);
		expect(userNodeConfirmationDao.store(conf)).andReturn(TEST_NODE_ID);

		replayAll();

		NetworkAssociationDetails details = new NetworkAssociationDetails();
		details.setUsername(TEST_EMAIL);
		details.setConfirmationKey(TEST_CONF_KEY);
		NetworkCertificate cert = registrationBiz.confirmNodeAssociation(details);

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
		conf.setCreated(Instant.now());
		conf.setCountry("NZ");
		conf.setTimeZoneId("Pacific/Auckland");
		conf.setNodeId(TEST_NODE_ID); // pre-assign node ID
		final SolarNode node = new SolarNode();
		node.setId(TEST_NODE_ID);
		final SolarLocation loc = new SolarLocation();
		loc.setId(TEST_LOC_ID);
		final UserNode userNode = new UserNode();
		userNode.setId(TEST_NODE_ID);

		final Instant now = Instant.now();

		// to confirm a node, we must look up the UserNodeConfirmation by userId+key, then
		// create the new SolarNode using a default SolarLocation, followed by
		// a new UserNode and UserNodeCertificate. The original UserNodeConfirmation should
		// have its 

		expect(userDao.getUserByEmail(TEST_EMAIL)).andReturn(testUser);
		expect(userNodeConfirmationDao.getConfirmationForKey(TEST_USER_ID, TEST_CONF_KEY))
				.andReturn(conf);
		expect(solarLocationDao.getSolarLocationForTimeZone(conf.getCountry(), conf.getTimeZoneId()))
				.andReturn(loc);
		expect(nodeDao.get(TEST_NODE_ID)).andReturn(node);
		expect(userNodeDao.get(TEST_NODE_ID)).andReturn(userNode);
		expect(userNodeConfirmationDao.store(conf)).andReturn(TEST_NODE_ID);

		replayAll();

		NetworkAssociationDetails details = new NetworkAssociationDetails();
		details.setUsername(TEST_EMAIL);
		details.setConfirmationKey(TEST_CONF_KEY);
		NetworkCertificate cert = registrationBiz.confirmNodeAssociation(details);

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
			NetworkAssociationDetails details = new NetworkAssociationDetails();
			details.setUsername(TEST_EMAIL);
			details.setConfirmationKey(BAD_CONF_KEY);
			registrationBiz.confirmNodeAssociation(details);
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
		conf.setCreated(Instant.now());
		conf.setConfirmationDate(Instant.now()); // mark as confirmed

		expect(userDao.getUserByEmail(TEST_EMAIL)).andReturn(testUser);
		expect(userNodeConfirmationDao.getConfirmationForKey(TEST_USER_ID, TEST_CONF_KEY))
				.andReturn(conf);
		replayAll();
		try {
			NetworkAssociationDetails details = new NetworkAssociationDetails();
			details.setUsername(TEST_EMAIL);
			details.setConfirmationKey(TEST_CONF_KEY);
			registrationBiz.confirmNodeAssociation(details);
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

	@SuppressWarnings("unchecked")
	@Test
	public void confirmNodeAssociationWithCertificateRequest()
			throws IOException, NoSuchAlgorithmException {
		final UserNodeConfirmation conf = new UserNodeConfirmation();
		conf.setUser(testUser);
		conf.setCreated(Instant.now());
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

		final Instant now = Instant.now();

		final UserNodeCertificate userNodeCertificate = new UserNodeCertificate();
		userNodeCertificate.setUser(testUser);
		userNodeCertificate.setStatus(UserNodeCertificateStatus.a);

		// to confirm a node, we must look up the UserNodeConfirmation by userId+key, then
		// create the new SolarNode using a default SolarLocation, followed by
		// a new UserNode and UserNodeCertificate. The original UserNodeConfirmation should
		// have its 

		expect(userDao.getUserByEmail(TEST_EMAIL)).andReturn(testUser);
		expect(userNodeConfirmationDao.getConfirmationForKey(TEST_USER_ID, TEST_CONF_KEY))
				.andReturn(conf);

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
		expect(nodePKIBiz.generateCertificate(EasyMock.eq(nodeSubjectDN),
				EasyMock.anyObject(PublicKey.class), EasyMock.anyObject(PrivateKey.class)))
						.andReturn(certificate);
		expect(nodePKIBiz.submitCSR(EasyMock.eq(certificate), EasyMock.anyObject(PrivateKey.class)))
				.andReturn(TEST_CERT_REQ_ID);
		expect(userNodeCertificateDao.store(EasyMock.anyObject(UserNodeCertificate.class)))
				.andReturn(TEST_CERT_ID);
		expect(executorService.submit(EasyMock.anyObject(Callable.class)))
				.andReturn(new Future<UserNodeCertificate>() {

					@Override
					public boolean cancel(boolean mayInterruptIfRunning) {
						return false;
					}

					@Override
					public boolean isCancelled() {
						return false;
					}

					@Override
					public boolean isDone() {
						return true;
					}

					@Override
					public UserNodeCertificate get() throws InterruptedException, ExecutionException {
						return userNodeCertificate;
					}

					@Override
					public UserNodeCertificate get(long timeout, TimeUnit unit)
							throws InterruptedException, ExecutionException, TimeoutException {
						return userNodeCertificate;
					}
				});

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

	private UserNodeCertificate createTestSignedUserNodeCertificate(KeyPair keypair,
			X509Certificate caCert, KeyPair caKeypair, KeyStore keystore) throws KeyStoreException {
		final String nodeSubjectDN = String.format(TEST_DN_FORMAT, TEST_NODE_ID.toString());

		final X509Certificate certificate = certificateService.generateCertificate(nodeSubjectDN,
				keypair.getPublic(), keypair.getPrivate());

		// save self-signed cert
		keystore.setKeyEntry(UserNodeCertificate.KEYSTORE_NODE_ALIAS, keypair.getPrivate(),
				TEST_KEYSTORE_PASS.toCharArray(), new Certificate[] { certificate });

		// create signed cert
		X509Certificate signedCert = certificateService.signCertificate(certificateService
				.generatePKCS10CertificateRequestString(certificate, keypair.getPrivate()), caCert,
				caKeypair.getPrivate());

		try {
			keystore.setKeyEntry(UserNodeCertificate.KEYSTORE_NODE_ALIAS, keypair.getPrivate(),
					TEST_KEYSTORE_PASS.toCharArray(), new X509Certificate[] { signedCert, caCert });
		} catch ( KeyStoreException e ) {
			throw new CertificateException("Error opening node certificate", e);
		}

		ByteArrayOutputStream byos = new ByteArrayOutputStream();
		storeKeyStore(keystore, TEST_KEYSTORE_PASS, byos);

		final UserNodeCertificate userNodeCertificate = new UserNodeCertificate();
		userNodeCertificate.setNodeId(TEST_NODE_ID);
		userNodeCertificate.setUser(testUser);
		userNodeCertificate.setStatus(UserNodeCertificateStatus.a);
		userNodeCertificate.setKeystoreData(byos.toByteArray());
		return userNodeCertificate;
	}

	private UserNodeCertificate createTestRenewedUserNodeCertificate(KeyPair keypair,
			X509Certificate caCert, KeyPair caKeypair, KeyStore keystore) throws KeyStoreException {
		final X509Certificate certificate = (X509Certificate) keystore
				.getCertificate(UserNodeCertificate.KEYSTORE_NODE_ALIAS);

		// create signed cert
		X509Certificate signedCert = certificateService.signCertificate(certificateService
				.generatePKCS10CertificateRequestString(certificate, keypair.getPrivate()), caCert,
				caKeypair.getPrivate());

		try {
			keystore.setKeyEntry(UserNodeCertificate.KEYSTORE_NODE_ALIAS, keypair.getPrivate(),
					TEST_KEYSTORE_PASS.toCharArray(), new X509Certificate[] { signedCert, caCert });
		} catch ( KeyStoreException e ) {
			throw new CertificateException("Error opening node certificate", e);
		}

		ByteArrayOutputStream byos = new ByteArrayOutputStream();
		storeKeyStore(keystore, TEST_KEYSTORE_PASS, byos);

		final UserNodeCertificate userNodeCertificate = new UserNodeCertificate();
		userNodeCertificate.setUser(testUser);
		userNodeCertificate.setStatus(UserNodeCertificateStatus.a);
		userNodeCertificate.setKeystoreData(byos.toByteArray());
		return userNodeCertificate;
	}

	@SuppressWarnings("unchecked")
	@Test
	public void renewCertificateForUserNode() throws Exception {
		final KeyStore keystore = loadKeyStore(TEST_KEYSTORE_PASS, null);
		final KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
		keyGen.initialize(1024, new SecureRandom());
		final KeyPair keypair = keyGen.generateKeyPair();
		final KeyPair caKeypair = keyGen.generateKeyPair();
		final X509Certificate caCert = certificateService.generateCertificationAuthorityCertificate(
				TEST_CA_DN, caKeypair.getPublic(), caKeypair.getPrivate());

		final UserNode userNode = new UserNode();
		userNode.setId(TEST_NODE_ID);
		userNode.setUser(testUser);

		final UserNodePK userNodePK = new UserNodePK(TEST_USER_ID, TEST_NODE_ID);

		final UserNodeCertificate originalCertificate = createTestSignedUserNodeCertificate(keypair,
				caCert, caKeypair, keystore);

		final KeyStore renewedKeystore = loadKeyStore(TEST_KEYSTORE_PASS,
				new ByteArrayInputStream(originalCertificate.getKeystoreData()));
		final UserNodeCertificate renewedCertificate = createTestRenewedUserNodeCertificate(keypair,
				caCert, caKeypair, renewedKeystore);
		renewedCertificate.setNodeId(originalCertificate.getNodeId());
		renewedCertificate.setUser(originalCertificate.getUser());
		renewedCertificate.setStatus(UserNodeCertificateStatus.v);

		// get the original cert
		expect(userNodeCertificateDao.get(userNodePK)).andReturn(originalCertificate);

		// renew the cert
		expect(nodePKIBiz.submitRenewalRequest(originalCertificate.getNodeCertificate(keystore)))
				.andReturn(TEST_CERT_RENEWAL_ID);

		expect(executorService.submit(EasyMock.anyObject(Callable.class)))
				.andReturn(new Future<UserNodeCertificate>() {

					@Override
					public boolean cancel(boolean mayInterruptIfRunning) {
						return false;
					}

					@Override
					public boolean isCancelled() {
						return false;
					}

					@Override
					public boolean isDone() {
						return true;
					}

					@Override
					public UserNodeCertificate get() throws InterruptedException, ExecutionException {
						return renewedCertificate;
					}

					@Override
					public UserNodeCertificate get(long timeout, TimeUnit unit)
							throws InterruptedException, ExecutionException, TimeoutException {
						return renewedCertificate;
					}
				});

		expect(userNodeCertificateDao.store(originalCertificate)).andReturn(TEST_CERT_ID);

		Capture<Instruction> instrCap = new Capture<Instruction>();
		NodeInstruction nodeInstr = new NodeInstruction();
		nodeInstr.setId(TEST_INSTRUCTION_ID);
		expect(instructorBiz.queueInstruction(EasyMock.eq(originalCertificate.getNodeId()),
				EasyMock.capture(instrCap))).andReturn(nodeInstr);

		replayAll();

		UserNodeCertificateRenewal cert = registrationBiz.renewNodeCertificate(userNode,
				TEST_KEYSTORE_PASS);

		verifyAll();

		assertNotNull(cert);
		assertEquals(TEST_INSTRUCTION_ID.toString(), cert.getConfirmationKey());
		assertEquals(String.format(TEST_DN_FORMAT, TEST_NODE_ID.toString()),
				cert.getNetworkCertificateSubjectDN());
		assertEquals(UserNodeCertificateStatus.v.getValue(), cert.getNetworkCertificateStatus());
		assertEquals(TEST_NODE_ID, cert.getNetworkId());
		assertEquals(Base64.encodeBase64String(renewedCertificate.getKeystoreData()),
				cert.getNetworkCertificate());

		Instruction instr = instrCap.getValue();
		assertEquals(DaoRegistrationBiz.INSTRUCTION_TOPIC_RENEW_CERTIFICATE, instr.getTopic());
		assertNotNull(instr.getParameters());
		assertEquals(4, instr.getParameters().size());
		StringBuilder generatedPem = new StringBuilder();
		for ( InstructionParameter param : instr.getParameters() ) {
			assertEquals(DaoRegistrationBiz.INSTRUCTION_PARAM_CERTIFICATE, param.getName());
			generatedPem.append(param.getValue());
		}

		String expectedPem = certificateService.generatePKCS7CertificateChainString(
				new X509Certificate[] { renewedCertificate.getNodeCertificate(renewedKeystore) });
		assertEquals(expectedPem, generatedPem.toString());

		// our stored certificate should have the request ID set to the instruction ID
		assertEquals(TEST_INSTRUCTION_ID.toString(), originalCertificate.getRequestId());
	}

	private AuthenticatedNode setAuthenticatedNode(final Long nodeId) {
		AuthenticatedNode node = new AuthenticatedNode(nodeId, null, false);
		TestingAuthenticationToken auth = new TestingAuthenticationToken(node, "foobar", "ROLE_NODE");
		SecurityContextHolder.getContext().setAuthentication(auth);
		return node;
	}

	@SuppressWarnings("unchecked")
	@Test
	public void renewCertificateForNode() throws Exception {
		final KeyStore keystore = loadKeyStore(TEST_KEYSTORE_PASS, null);
		final KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
		keyGen.initialize(1024, new SecureRandom());
		final KeyPair keypair = keyGen.generateKeyPair();
		final KeyPair caKeypair = keyGen.generateKeyPair();
		final X509Certificate caCert = certificateService.generateCertificationAuthorityCertificate(
				TEST_CA_DN, caKeypair.getPublic(), caKeypair.getPrivate());

		final UserNodeCertificate originalCertificate = createTestSignedUserNodeCertificate(keypair,
				caCert, caKeypair, keystore);

		final KeyStore renewedKeystore = loadKeyStore(TEST_KEYSTORE_PASS,
				new ByteArrayInputStream(originalCertificate.getKeystoreData()));
		final UserNodeCertificate renewedCertificate = createTestRenewedUserNodeCertificate(keypair,
				caCert, caKeypair, renewedKeystore);
		renewedCertificate.setNodeId(originalCertificate.getNodeId());
		renewedCertificate.setUser(originalCertificate.getUser());
		renewedCertificate.setStatus(UserNodeCertificateStatus.v);

		setAuthenticatedNode(TEST_NODE_ID);

		final SolarNode testNode = new SolarNode(TEST_NODE_ID, TEST_LOC_ID);
		final UserNode userNode = new UserNode(testUser, testNode);
		final UserNodePK userNodePK = new UserNodePK(TEST_USER_ID, TEST_NODE_ID);

		expect(userNodeDao.get(TEST_NODE_ID)).andReturn(userNode);
		expect(userNodeCertificateDao.get(userNodePK)).andReturn(null); // no cert in DB
		expect(nodePKIBiz.submitRenewalRequest(originalCertificate.getNodeCertificate(keystore)))
				.andReturn(TEST_CERT_RENEWAL_ID);

		expect(executorService.submit(EasyMock.anyObject(Callable.class)))
				.andReturn(new Future<UserNodeCertificate>() {

					@Override
					public boolean cancel(boolean mayInterruptIfRunning) {
						return false;
					}

					@Override
					public boolean isCancelled() {
						return false;
					}

					@Override
					public boolean isDone() {
						return true;
					}

					@Override
					public UserNodeCertificate get() throws InterruptedException, ExecutionException {
						return renewedCertificate;
					}

					@Override
					public UserNodeCertificate get(long timeout, TimeUnit unit)
							throws InterruptedException, ExecutionException, TimeoutException {
						return renewedCertificate;
					}
				});

		Capture<UserNodeCertificate> userNodeCertCap = new Capture<UserNodeCertificate>();
		expect(userNodeCertificateDao.store(EasyMock.capture(userNodeCertCap))).andReturn(TEST_CERT_ID);

		Capture<Instruction> instrCap = new Capture<Instruction>();
		NodeInstruction nodeInstr = new NodeInstruction();
		nodeInstr.setId(TEST_INSTRUCTION_ID);
		expect(instructorBiz.queueInstruction(EasyMock.eq(originalCertificate.getNodeId()),
				EasyMock.capture(instrCap))).andReturn(nodeInstr);

		replayAll();

		NetworkCertificate result = registrationBiz.renewNodeCertificate(
				new ByteArrayInputStream(originalCertificate.getKeystoreData()), TEST_KEYSTORE_PASS);

		verifyAll();

		assertNotNull(result);

		Instruction instr = instrCap.getValue();
		assertEquals(DaoRegistrationBiz.INSTRUCTION_TOPIC_RENEW_CERTIFICATE, instr.getTopic());
		assertNotNull(instr.getParameters());
		assertEquals(4, instr.getParameters().size());
		StringBuilder generatedPem = new StringBuilder();
		for ( InstructionParameter param : instr.getParameters() ) {
			assertEquals(DaoRegistrationBiz.INSTRUCTION_PARAM_CERTIFICATE, param.getName());
			generatedPem.append(param.getValue());
		}

		UserNodeCertificate userNodeCert = userNodeCertCap.getValue();
		assertNotNull(userNodeCert);

		// our stored certificate should have the request ID set to the instruction ID
		assertEquals(TEST_INSTRUCTION_ID.toString(), userNodeCert.getRequestId());
	}

	@Test
	public void checkRenewalStatusQueued() throws Exception {

		final String confirmationKey = TEST_INSTRUCTION_ID.toString();

		final UserNode userNode = new UserNode();
		userNode.setId(TEST_NODE_ID);
		userNode.setUser(testUser);

		NodeInstruction instr = new NodeInstruction(
				DaoRegistrationBiz.INSTRUCTION_TOPIC_RENEW_CERTIFICATE, Instant.now(), userNode.getId());
		instr.setId(TEST_INSTRUCTION_ID);
		instr.setState(InstructionState.Queued);
		instr.addParameter(DaoRegistrationBiz.INSTRUCTION_PARAM_CERTIFICATE, "1");
		instr.addParameter(DaoRegistrationBiz.INSTRUCTION_PARAM_CERTIFICATE, "2");
		instr.addParameter(DaoRegistrationBiz.INSTRUCTION_PARAM_CERTIFICATE, "3");

		expect(instructorBiz.getInstruction(TEST_INSTRUCTION_ID)).andReturn(instr);

		replayAll();

		UserNodeCertificateRenewal status = registrationBiz.getPendingNodeCertificateRenewal(userNode,
				confirmationKey);

		verifyAll();

		assertNotNull(status);
		assertEquals(confirmationKey, status.getConfirmationKey());
		assertEquals(UserNodeCertificateInstallationStatus.RequestQueued,
				status.getInstallationStatus());
		assertEquals("123", status.getNetworkCertificate());
	}

	@Test
	public void checkRenewalStatusReceived() throws Exception {

		final String confirmationKey = TEST_INSTRUCTION_ID.toString();

		final UserNode userNode = new UserNode();
		userNode.setId(TEST_NODE_ID);
		userNode.setUser(testUser);

		NodeInstruction instr = new NodeInstruction(
				DaoRegistrationBiz.INSTRUCTION_TOPIC_RENEW_CERTIFICATE, Instant.now(), userNode.getId());
		instr.setId(TEST_INSTRUCTION_ID);
		instr.setState(InstructionState.Received);

		expect(instructorBiz.getInstruction(TEST_INSTRUCTION_ID)).andReturn(instr);

		replayAll();

		UserNodeCertificateRenewal status = registrationBiz.getPendingNodeCertificateRenewal(userNode,
				confirmationKey);

		verifyAll();

		assertNotNull(status);
		assertEquals(confirmationKey, status.getConfirmationKey());
		assertEquals(UserNodeCertificateInstallationStatus.RequestReceived,
				status.getInstallationStatus());
	}

	@Test
	public void checkRenewalStatusExecuting() throws Exception {

		final String confirmationKey = TEST_INSTRUCTION_ID.toString();

		final UserNode userNode = new UserNode();
		userNode.setId(TEST_NODE_ID);
		userNode.setUser(testUser);

		NodeInstruction instr = new NodeInstruction(
				DaoRegistrationBiz.INSTRUCTION_TOPIC_RENEW_CERTIFICATE, Instant.now(), userNode.getId());
		instr.setId(TEST_INSTRUCTION_ID);
		instr.setState(InstructionState.Executing);

		expect(instructorBiz.getInstruction(TEST_INSTRUCTION_ID)).andReturn(instr);

		replayAll();

		UserNodeCertificateRenewal status = registrationBiz.getPendingNodeCertificateRenewal(userNode,
				confirmationKey);

		verifyAll();

		assertNotNull(status);
		assertEquals(confirmationKey, status.getConfirmationKey());
		assertEquals(UserNodeCertificateInstallationStatus.RequestReceived,
				status.getInstallationStatus());
	}

	@Test
	public void checkRenewalStatusCompleted() throws Exception {

		final String confirmationKey = TEST_INSTRUCTION_ID.toString();

		final UserNode userNode = new UserNode();
		userNode.setId(TEST_NODE_ID);
		userNode.setUser(testUser);

		NodeInstruction instr = new NodeInstruction(
				DaoRegistrationBiz.INSTRUCTION_TOPIC_RENEW_CERTIFICATE, Instant.now(), userNode.getId());
		instr.setId(TEST_INSTRUCTION_ID);
		instr.setState(InstructionState.Completed);

		expect(instructorBiz.getInstruction(TEST_INSTRUCTION_ID)).andReturn(instr);

		replayAll();

		UserNodeCertificateRenewal status = registrationBiz.getPendingNodeCertificateRenewal(userNode,
				confirmationKey);

		verifyAll();

		assertNotNull(status);
		assertEquals(confirmationKey, status.getConfirmationKey());
		assertEquals(UserNodeCertificateInstallationStatus.Installed, status.getInstallationStatus());
	}

	@Test
	public void checkRenewalStatusDeclined() throws Exception {

		final String confirmationKey = TEST_INSTRUCTION_ID.toString();

		final UserNode userNode = new UserNode();
		userNode.setId(TEST_NODE_ID);
		userNode.setUser(testUser);

		NodeInstruction instr = new NodeInstruction(
				DaoRegistrationBiz.INSTRUCTION_TOPIC_RENEW_CERTIFICATE, Instant.now(), userNode.getId());
		instr.setId(TEST_INSTRUCTION_ID);
		instr.setState(InstructionState.Declined);

		expect(instructorBiz.getInstruction(TEST_INSTRUCTION_ID)).andReturn(instr);

		replayAll();

		UserNodeCertificateRenewal status = registrationBiz.getPendingNodeCertificateRenewal(userNode,
				confirmationKey);

		verifyAll();

		assertNotNull(status);
		assertEquals(confirmationKey, status.getConfirmationKey());
		assertEquals(UserNodeCertificateInstallationStatus.Declined, status.getInstallationStatus());
	}

}
