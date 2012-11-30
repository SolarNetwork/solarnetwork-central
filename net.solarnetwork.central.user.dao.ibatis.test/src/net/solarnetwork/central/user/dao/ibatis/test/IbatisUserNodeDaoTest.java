/* ==================================================================
 * IbatisUserNodeDaoTest.java - Jan 29, 2010 12:21:07 PM
 * 
 * Copyright 2007-2010 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.dao.ibatis.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import java.util.List;
import net.solarnetwork.central.dao.ibatis.IbatisSolarNodeDao;
import net.solarnetwork.central.domain.SolarNode;
import net.solarnetwork.central.user.dao.ibatis.IbatisUserNodeCertificateDao;
import net.solarnetwork.central.user.dao.ibatis.IbatisUserNodeDao;
import net.solarnetwork.central.user.domain.User;
import net.solarnetwork.central.user.domain.UserNode;
import net.solarnetwork.central.user.domain.UserNodeCertificate;
import net.solarnetwork.central.user.domain.UserNodeCertificateStatus;
import org.apache.commons.codec.digest.DigestUtils;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Test case for the {@link IbatisUserNodeDao} class.
 * 
 * @author matt
 * @version $Id$
 */
public class IbatisUserNodeDaoTest extends AbstractIbatisUserDaoTestSupport {

	/**
	 * The tables to delete from at the start of the tests (within a
	 * transaction).
	 */
	private static final String[] DELETE_TABLES = new String[] { "solaruser.user_node",
			"solaruser.user_user" };

	private static final String TEST_EMAIL_2 = "foo2@localhost.localdomain";
	private static final String TEST_DESC = "Test description";
	private static final Long TEST_ID_2 = -2L;
	private static final byte[] TEST_CERT = "test cert".getBytes();
	private static final String TEST_CONF_KEY = DigestUtils.sha256Hex("test conf key");

	@Autowired
	private IbatisUserNodeDao userNodeDao;

	@Autowired
	private IbatisSolarNodeDao solarNodeDao;

	@Autowired
	private IbatisUserNodeCertificateDao userNodeCertificateDao;

	private User user = null;
	private SolarNode node = null;
	private Long userNodeId = null;

	@Before
	public void setUp() throws Exception {
		setupTestNode();
		this.node = solarNodeDao.get(TEST_NODE_ID);
		assertNotNull(this.node);
		deleteFromTables(DELETE_TABLES);
		storeNewUser();
		assertNotNull(this.user);
		userNodeId = null;
	}

	/**
	 * Test able to store a new UserNode.
	 */
	@Test
	public void storeNewUserNode() {
		UserNode newUserNode = new UserNode();
		newUserNode.setNode(this.node);
		newUserNode.setCreated(new DateTime());
		newUserNode.setDescription(TEST_DESC);
		newUserNode.setName(TEST_NAME);
		newUserNode.setNode(this.node);
		newUserNode.setUser(this.user);
		Long id = userNodeDao.store(newUserNode);
		assertNotNull(id);
		this.userNodeId = id;
	}

	/**
	 * Test able to get a user node.
	 */
	@Test
	public void getByPrimaryKey() {
		storeNewUserNode();
		UserNode userNode = userNodeDao.get(this.userNodeId);
		assertNotNull(userNode);
		assertEquals(this.userNodeId, userNode.getId());
		assertEquals(TEST_NAME, userNode.getName());
		assertEquals(TEST_DESC, userNode.getDescription());
		assertNotNull(userNode.getNode());
		assertEquals(TEST_NODE_ID, userNode.getNode().getId());
		assertNotNull(userNode.getUser());
	}

	/**
	 * Test able to update an existing UserNode.
	 */
	@Test
	public void updateUserNode() {
		storeNewUserNode();
		Long user2Id = storeNewUser(TEST_EMAIL_2);
		UserNode userNode = userNodeDao.get(this.userNodeId);
		userNode.setDescription("New description");
		userNode.setName("New name");
		userNode.setUser(userDao.get(user2Id));
		Long id = userNodeDao.store(userNode);
		assertNotNull(id);
		assertEquals(this.userNodeId, id);
		UserNode updated = userNodeDao.get(this.userNodeId);
		assertEquals(userNode.getDescription(), updated.getDescription());
		assertEquals(userNode.getName(), updated.getName());
		assertEquals(TEST_NODE_ID, updated.getNode().getId());
	}

	/**
	 * Test able to find for a user with multiple results.
	 */
	@Test
	public void findForUserMultipleResults() {
		storeNewUserNode();

		// create 2nd node for user
		setupTestNode(TEST_ID_2);
		UserNode newUserNode = new UserNode();
		newUserNode.setCreated(new DateTime());
		newUserNode.setDescription(TEST_DESC);
		newUserNode.setName(TEST_NAME);
		newUserNode.setNode(solarNodeDao.get(TEST_ID_2));
		newUserNode.setUser(this.user);
		Long userNode2 = userNodeDao.store(newUserNode);
		assertNotNull(userNode2);

		List<UserNode> results = userNodeDao.findUserNodesForUser(this.user);
		assertNotNull(results);
		assertEquals(2, results.size());

		UserNode n1 = results.get(0);
		assertNotNull(n1);
		assertEquals(this.userNodeId, n1.getId());
		assertEquals(this.node, n1.getNode());
		assertEquals(this.user, n1.getUser());
	}

	private void storeNewUser() {
		this.user = createNewUser(TEST_EMAIL);
	}

	private UserNodeCertificate storeNewCert(String key, UserNodeCertificateStatus status) {
		UserNodeCertificate newUserNodeCert = new UserNodeCertificate();
		newUserNodeCert.setCreated(new DateTime());
		newUserNodeCert.setNode(this.node);
		newUserNodeCert.setUser(this.user);
		newUserNodeCert.setConfirmationKey(key);
		newUserNodeCert.setCertificate(TEST_CERT);
		newUserNodeCert.setStatus(status);
		Long id = userNodeCertificateDao.store(newUserNodeCert);
		assertNotNull(id);
		return userNodeCertificateDao.get(id);
	}

	@Test
	public void findForUserWithCertificates() {
		final UserNodeCertificate cert1 = storeNewCert(TEST_CONF_KEY, UserNodeCertificateStatus.v);

		List<UserNode> results = userNodeDao.findUserNodesAndCertificatesForUser(user.getId());
		assertNotNull(results);
		assertEquals(1, results.size());
		assertEquals(cert1, results.get(0).getCertificate());

		// add a second cert for the same user, with higher status
		UserNodeCertificate newUserNodeCert = new UserNodeCertificate();
		newUserNodeCert.setCreated(new DateTime());
		newUserNodeCert.setNode(this.node);
		newUserNodeCert.setUser(this.user);
		newUserNodeCert.setConfirmationKey(DigestUtils.sha256Hex(TEST_CONF_KEY));
		newUserNodeCert.setCertificate(TEST_CERT);
		newUserNodeCert.setStatus(UserNodeCertificateStatus.a);
		Long id = userNodeCertificateDao.store(newUserNodeCert);
		assertNotNull(id);

		final UserNodeCertificate cert2 = userNodeCertificateDao.get(id);

		results = userNodeDao.findUserNodesAndCertificatesForUser(user.getId());
		assertNotNull(results);
		assertEquals(1, results.size());
		assertEquals(cert2, results.get(0).getCertificate());
	}

}
