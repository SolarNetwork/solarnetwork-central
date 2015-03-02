/* ==================================================================
 * MyBatisUserNodeDaoTests.java - Nov 11, 2014 7:28:50 AM
 * 
 * Copyright 2007-2014 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.dao.mybatis.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import java.util.List;
import net.solarnetwork.central.dao.mybatis.MyBatisSolarNodeDao;
import net.solarnetwork.central.domain.SolarNode;
import net.solarnetwork.central.user.dao.mybatis.MyBatisUserNodeCertificateDao;
import net.solarnetwork.central.user.dao.mybatis.MyBatisUserNodeDao;
import net.solarnetwork.central.user.domain.User;
import net.solarnetwork.central.user.domain.UserNode;
import net.solarnetwork.central.user.domain.UserNodeCertificate;
import net.solarnetwork.central.user.domain.UserNodeCertificateStatus;
import net.solarnetwork.central.user.domain.UserNodePK;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

/**
 * Test cases for the {@link MyBatisUserNodeDao} class.
 * 
 * @author matt
 * @version 1.0
 */
public class MyBatisUserNodeDaoTests extends AbstractMyBatisUserDaoTestSupport {

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

	private MyBatisUserNodeDao userNodeDao;
	private MyBatisSolarNodeDao solarNodeDao;
	private MyBatisUserNodeCertificateDao userNodeCertificateDao;

	private User user = null;
	private SolarNode node = null;
	private Long userNodeId = null;

	@Before
	public void setUp() throws Exception {
		userNodeCertificateDao = new MyBatisUserNodeCertificateDao();
		userNodeCertificateDao.setSqlSessionFactory(getSqlSessionFactory());
		solarNodeDao = new MyBatisSolarNodeDao();
		solarNodeDao.setSqlSessionFactory(getSqlSessionFactory());
		userNodeDao = new MyBatisUserNodeDao();
		userNodeDao.setSqlSessionFactory(getSqlSessionFactory());
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

		assertNull("Certificate", n1.getCertificate());
	}

	private void storeNewUser() {
		this.user = createNewUser(TEST_EMAIL);
	}

	private UserNodeCertificate storeNewCert(UserNodeCertificateStatus status) {
		UserNodeCertificate newUserNodeCert = new UserNodeCertificate();
		newUserNodeCert.setCreated(new DateTime());
		newUserNodeCert.setNodeId(this.node.getId());
		newUserNodeCert.setUserId(this.user.getId());
		newUserNodeCert.setKeystoreData(TEST_CERT);
		newUserNodeCert.setStatus(status);
		newUserNodeCert.setRequestId("test req ID");
		UserNodePK id = userNodeCertificateDao.store(newUserNodeCert);
		assertNotNull(id);
		return userNodeCertificateDao.get(id);
	}

	@Test
	public void findForUserWithNoCertificate() {
		storeNewUserNode();
		List<UserNode> results = userNodeDao.findUserNodesAndCertificatesForUser(user.getId());
		assertNotNull(results);
		assertEquals(1, results.size());
		assertNull("Certificate not present", results.get(0).getCertificate());
	}

	@Test
	public void findForUserWithCertificate() {
		storeNewUserNode();
		final UserNodeCertificate cert1 = storeNewCert(UserNodeCertificateStatus.v);

		List<UserNode> results = userNodeDao.findUserNodesAndCertificatesForUser(user.getId());
		assertNotNull(results);
		assertEquals(1, results.size());
		assertEquals(cert1, results.get(0).getCertificate());
	}

}
