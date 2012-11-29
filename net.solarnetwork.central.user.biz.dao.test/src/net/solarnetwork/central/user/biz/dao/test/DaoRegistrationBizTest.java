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
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import net.solarnetwork.central.dao.SolarLocationDao;
import net.solarnetwork.central.dao.SolarNodeDao;
import net.solarnetwork.central.domain.SolarLocation;
import net.solarnetwork.central.domain.SolarNode;
import net.solarnetwork.central.in.biz.NetworkIdentityBiz;
import net.solarnetwork.central.user.biz.dao.DaoRegistrationBiz;
import net.solarnetwork.central.user.dao.UserDao;
import net.solarnetwork.central.user.dao.UserNodeCertificateDao;
import net.solarnetwork.central.user.dao.UserNodeConfirmationDao;
import net.solarnetwork.central.user.dao.UserNodeDao;
import net.solarnetwork.central.user.domain.User;
import net.solarnetwork.central.user.domain.UserNode;
import net.solarnetwork.central.user.domain.UserNodeCertificate;
import net.solarnetwork.central.user.domain.UserNodeCertificateStatus;
import net.solarnetwork.central.user.domain.UserNodeConfirmation;
import net.solarnetwork.domain.BasicNetworkIdentity;
import net.solarnetwork.domain.NetworkAssociation;
import net.solarnetwork.domain.NetworkAssociationDetails;
import net.solarnetwork.domain.NetworkCertificate;
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
 * @version 1.0
 */
public class DaoRegistrationBizTest {

	private final Long TEST_USER_ID = -1L;
	private final Long TEST_CONF_ID = -2L;
	private final Long TEST_NODE_ID = -3L;
	private final Long TEST_LOC_ID = -4L;
	private final Long TEST_CERT_ID = -5L;
	private final String TEST_EMAIL = "test@localhost";
	private final String TEST_SECURITY_PHRASE = "test phrase";
	private final String TEST_CONF_KEY = "test conf key";

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

	@Before
	public void setup() {
		networkIdentity = new BasicNetworkIdentity("key", "tos", "host", 80, false);
		networkIdentityBiz = EasyMock.createMock(NetworkIdentityBiz.class);
		testUser = new User();
		testUser.setEmail(TEST_EMAIL);
		testUser.setId(TEST_USER_ID);
		userDao = EasyMock.createMock(UserDao.class);
		userNodeDao = EasyMock.createMock(UserNodeDao.class);
		userNodeConfirmationDao = EasyMock.createMock(UserNodeConfirmationDao.class);
		userNodeCertificateDao = EasyMock.createMock(UserNodeCertificateDao.class);
		nodeDao = EasyMock.createMock(SolarNodeDao.class);
		solarLocationDao = EasyMock.createMock(SolarLocationDao.class);
		registrationBiz = new DaoRegistrationBiz();
		registrationBiz.setNetworkIdentityBiz(networkIdentityBiz);
		registrationBiz.setUserDao(userDao);
		registrationBiz.setUserNodeConfirmationDao(userNodeConfirmationDao);
		registrationBiz.setSolarNodeDao(nodeDao);
		registrationBiz.setSolarLocationDao(solarLocationDao);
		registrationBiz.setUserNodeCertificateDao(userNodeCertificateDao);
		registrationBiz.setUserNodeDao(userNodeDao);
	}

	@Test
	public void createNodeAssociation() throws IOException {
		expect(networkIdentityBiz.getNetworkIdentity()).andReturn(networkIdentity);
		expect(userDao.get(TEST_USER_ID)).andReturn(testUser);
		expect(userNodeConfirmationDao.store(EasyMock.anyObject(UserNodeConfirmation.class))).andReturn(
				TEST_CONF_ID);
		replay(networkIdentityBiz, userDao, userNodeConfirmationDao);

		final NetworkAssociation result = registrationBiz.createNodeAssociation(TEST_USER_ID,
				TEST_SECURITY_PHRASE);

		verify(networkIdentityBiz, userDao, userNodeConfirmationDao);

		assertEquals(networkIdentity.getHost(), result.getHost());
		assertEquals(networkIdentity.getPort(), result.getPort());
		assertEquals(networkIdentity.getIdentityKey(), result.getIdentityKey());
		assertEquals(networkIdentity.getPort(), result.getPort());
		assertEquals(networkIdentity.getTermsOfService(), result.getTermsOfService());
		assertEquals(TEST_SECURITY_PHRASE, result.getSecurityPhrase());
		assertEquals(TEST_EMAIL, result.getUsername());
		assertEquals(Boolean.FALSE, Boolean.valueOf(result.isForceTLS()));
		assertEquals(NetworkAssociationDetails.class, result.getClass());
		assertNull(((NetworkAssociationDetails) result).getNetworkId());

		Map<String, Object> detailMap = decodeAssociationDetails(result.getConfirmationKey());
		assertEquals(8, detailMap.size());
		assertNotNull("Confirmation key must be present", detailMap.get("confirmationKey"));
		assertNotNull("Expiration date must be present", detailMap.get("expiration"));
		assertEquals("false", detailMap.get("forceTLS"));
		assertEquals("host", detailMap.get("host"));
		assertEquals("key", detailMap.get("identityKey"));
		assertEquals("80", detailMap.get("port"));
		assertEquals("tos", detailMap.get("termsOfService"));
		assertEquals(TEST_EMAIL, detailMap.get("username"));
	}

	@Test
	public void cancelNodeAssociation() {
		final Long testConfId = -1L;
		final UserNodeConfirmation conf = new UserNodeConfirmation();
		conf.setId(testConfId);
		expect(userNodeConfirmationDao.get(testConfId)).andReturn(conf);
		userNodeConfirmationDao.delete(conf);
		replay(userNodeConfirmationDao);

		registrationBiz.cancelNodeAssociation(testConfId);

		verify(userNodeConfirmationDao);
	}

	@Test
	public void confirmNodeAssociation() throws IOException {
		final UserNodeConfirmation conf = new UserNodeConfirmation();
		conf.setUser(testUser);
		conf.setNodeId(TEST_NODE_ID);
		conf.setCreated(new DateTime());
		final SolarLocation loc = new SolarLocation();
		loc.setId(TEST_LOC_ID);
		final UserNode userNode = new UserNode();
		userNode.setId(TEST_NODE_ID);

		final DateTime now = new DateTime();

		// to confirm a node, we must look up the UserNodeConfirmation by userId+key, then
		// create the new SolarNode using a default SolarLocation, followed by
		// a new UserNode and UserNodeCertificate. The original UserNodeConfirmation should
		// have its 

		expect(userDao.get(TEST_USER_ID)).andReturn(testUser);
		expect(userNodeConfirmationDao.getConfirmationForKey(TEST_USER_ID, TEST_CONF_KEY)).andReturn(
				conf);
		expect(solarLocationDao.getSolarLocationForName(EasyMock.anyObject(String.class)))
				.andReturn(loc);
		expect(nodeDao.getUnusedNodeId()).andReturn(TEST_NODE_ID);
		expect(nodeDao.store(EasyMock.anyObject(SolarNode.class))).andReturn(TEST_NODE_ID);
		expect(userNodeDao.store(EasyMock.anyObject(UserNode.class))).andReturn(TEST_NODE_ID);
		expect(userNodeConfirmationDao.store(conf)).andReturn(TEST_NODE_ID);
		expect(userNodeCertificateDao.store(EasyMock.anyObject(UserNodeCertificate.class))).andReturn(
				TEST_CERT_ID);

		replay(solarLocationDao, nodeDao, userDao, userNodeDao, userNodeConfirmationDao,
				userNodeCertificateDao);

		NetworkCertificate cert = registrationBiz.confirmNodeAssociation(TEST_USER_ID, TEST_CONF_KEY);

		verify(solarLocationDao, nodeDao, userDao, userNodeDao, userNodeConfirmationDao,
				userNodeCertificateDao);

		assertNotNull(cert);
		assertNotNull(cert.getConfirmationKey());
		assertEquals(UserNodeCertificateStatus.r.getValue(), cert.getNetworkCertificateStatus());
		assertEquals(TEST_NODE_ID, cert.getNetworkId());
		assertNotNull(conf.getConfirmationDate());
		assertFalse("The confirmation date must be >= now", now.isAfter(conf.getConfirmationDate()));
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
}
