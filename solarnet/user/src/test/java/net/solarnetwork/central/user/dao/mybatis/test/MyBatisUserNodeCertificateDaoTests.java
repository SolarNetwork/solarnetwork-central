/* ==================================================================
 * MyBatisUserNodeCertificateDaoTests.java - Nov 11, 2014 7:22:44 AM
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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import java.time.Instant;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.central.dao.mybatis.MyBatisSolarNodeDao;
import net.solarnetwork.central.domain.SolarNode;
import net.solarnetwork.central.user.dao.mybatis.MyBatisUserNodeCertificateDao;
import net.solarnetwork.central.user.dao.mybatis.MyBatisUserNodeDao;
import net.solarnetwork.central.user.domain.User;
import net.solarnetwork.central.user.domain.UserNode;
import net.solarnetwork.central.user.domain.UserNodeCertificate;
import net.solarnetwork.central.user.domain.UserNodeCertificateStatus;
import net.solarnetwork.central.user.domain.UserNodePK;

/**
 * Test cases for the {@link MyBatisUserNodeCertificateDao} class.
 * 
 * @author matt
 * @version 2.0
 */
public class MyBatisUserNodeCertificateDaoTests extends AbstractMyBatisUserDaoTestSupport {

	/**
	 * The tables to delete from at the start of the tests (within a
	 * transaction).
	 */
	private static final String[] DELETE_TABLES = new String[] { "solaruser.user_node_cert",
			"solaruser.user_node", "solaruser.user_user" };

	private static final byte[] TEST_CERT = "test cert".getBytes();
	private static final String TEST_REQ_KEY = "test req key";

	private MyBatisUserNodeDao userNodeDao;
	private MyBatisUserNodeCertificateDao userNodeCertificateDao;

	private MyBatisSolarNodeDao solarNodeDao;

	private User user = null;
	private SolarNode node = null;
	private UserNodeCertificate userNodeCert = null;

	@Before
	public void setUp() throws Exception {
		userNodeDao = new MyBatisUserNodeDao();
		userNodeDao.setSqlSessionFactory(getSqlSessionFactory());
		userNodeCertificateDao = new MyBatisUserNodeCertificateDao();
		userNodeCertificateDao.setSqlSessionFactory(getSqlSessionFactory());
		solarNodeDao = new MyBatisSolarNodeDao();
		solarNodeDao.setSqlSessionFactory(getSqlSessionFactory());

		setupTestNode();
		this.node = solarNodeDao.get(TEST_NODE_ID);
		assertNotNull(this.node);
		deleteFromTables(DELETE_TABLES);
		this.user = createNewUser(TEST_EMAIL);
		assertNotNull(this.user);
		UserNode un = new UserNode(this.user, this.node);
		userNodeDao.store(un);

		userNodeCert = null;
	}

	@Test
	public void storeNew() {
		UserNodeCertificate newUserNodeCert = new UserNodeCertificate();
		newUserNodeCert.setCreated(Instant.now());
		newUserNodeCert.setNodeId(this.node.getId());
		newUserNodeCert.setUserId(this.user.getId());
		newUserNodeCert.setRequestId(TEST_REQ_KEY);
		newUserNodeCert.setKeystoreData(TEST_CERT);
		newUserNodeCert.setStatus(UserNodeCertificateStatus.v);
		UserNodePK id = userNodeCertificateDao.store(newUserNodeCert);
		assertNotNull(id);
		this.userNodeCert = userNodeCertificateDao.get(id);
	}

	private void validate(UserNodeCertificate cert, UserNodeCertificate entity) {
		assertNotNull("UserNodeCertificate should exist", entity);
		assertNotNull("Created date should be set", entity.getCreated());
		assertEquals(cert.getId(), entity.getId());
		assertArrayEquals(cert.getKeystoreData(), entity.getKeystoreData());
		assertEquals(cert.getStatus(), entity.getStatus());
		assertEquals(cert.getNode(), entity.getNode());
		assertEquals(cert.getUser(), entity.getUser());
	}

	@Test
	public void getByPrimaryKey() {
		storeNew();
		UserNodeCertificate cert = userNodeCertificateDao.get(userNodeCert.getId());
		validate(this.userNodeCert, cert);
	}

}
