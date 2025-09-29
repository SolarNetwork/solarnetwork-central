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

import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.BDDAssertions.then;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.jdbc.JdbcTestUtils;
import net.solarnetwork.central.dao.mybatis.MyBatisSolarNodeDao;
import net.solarnetwork.central.domain.SolarNode;
import net.solarnetwork.central.security.SecurityTokenStatus;
import net.solarnetwork.central.security.SecurityTokenType;
import net.solarnetwork.central.user.dao.mybatis.MyBatisUserAuthTokenDao;
import net.solarnetwork.central.user.dao.mybatis.MyBatisUserNodeCertificateDao;
import net.solarnetwork.central.user.dao.mybatis.MyBatisUserNodeDao;
import net.solarnetwork.central.user.domain.User;
import net.solarnetwork.central.user.domain.UserAuthToken;
import net.solarnetwork.central.user.domain.UserNode;
import net.solarnetwork.central.user.domain.UserNodeCertificate;
import net.solarnetwork.central.user.domain.UserNodeCertificateStatus;
import net.solarnetwork.central.user.domain.UserNodePK;
import net.solarnetwork.central.user.domain.UserNodeTransfer;
import net.solarnetwork.domain.BasicSecurityPolicy;

/**
 * Test cases for the {@link MyBatisUserNodeDao} class.
 * 
 * @author matt
 * @version 2.2
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
	private static final Long TEST_NODE_2 = -99999L;

	private MyBatisUserNodeDao userNodeDao;
	private MyBatisSolarNodeDao solarNodeDao;
	private MyBatisUserNodeCertificateDao userNodeCertificateDao;
	private MyBatisUserAuthTokenDao userAuthTokenDao;

	private User user = null;
	private SolarNode node = null;
	private Long userNodeId = null;

	@BeforeEach
	public void setUp() throws Exception {
		userNodeCertificateDao = new MyBatisUserNodeCertificateDao();
		userNodeCertificateDao.setSqlSessionFactory(getSqlSessionFactory());
		solarNodeDao = new MyBatisSolarNodeDao();
		solarNodeDao.setSqlSessionFactory(getSqlSessionFactory());
		userAuthTokenDao = new MyBatisUserAuthTokenDao();
		userAuthTokenDao.setSqlSessionFactory(getSqlSessionFactory());
		userNodeDao = new MyBatisUserNodeDao();
		userNodeDao.setSqlSessionFactory(getSqlSessionFactory());
		setupTestNode();
		this.node = solarNodeDao.get(TEST_NODE_ID);
		then(this.node).isNotNull();
		JdbcTestUtils.deleteFromTables(jdbcTemplate, DELETE_TABLES);
		storeNewUser();
		then(this.user).isNotNull();
		userNodeId = null;
	}

	/**
	 * Test able to store a new UserNode.
	 */
	@Test
	public void storeNewUserNode() {
		UserNode newUserNode = new UserNode();
		newUserNode.setNode(this.node);
		newUserNode.setCreated(Instant.now());
		newUserNode.setDescription(TEST_DESC);
		newUserNode.setName(TEST_NAME);
		newUserNode.setNode(this.node);
		newUserNode.setUser(this.user);
		Long id = userNodeDao.save(newUserNode);
		then(id).isNotNull();
		this.userNodeId = id;
	}

	/**
	 * Test able to get a user node.
	 */
	@Test
	public void getByPrimaryKey() {
		storeNewUserNode();
		UserNode userNode = userNodeDao.get(this.userNodeId);
		// @formatter:off
		then(userNode)
			.isNotNull()
			.returns(userNodeId, from(UserNode::getId))
			.returns(TEST_NAME, from(UserNode::getName))
			.returns(TEST_DESC, from(UserNode::getDescription))
			.returns(node, from(UserNode::getNode))
			.satisfies(un -> then(un.getUser()).isNotNull())
			;
		// @formatter:on
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
		Long id = userNodeDao.save(userNode);
		then(id).isNotNull().isEqualTo(userNodeId);
		UserNode updated = userNodeDao.get(this.userNodeId);
		// @formatter:off
		then(updated)
			.isNotNull()
			.returns(userNodeId, from(UserNode::getId))
			.returns(userNode.getName(), from(UserNode::getName))
			.returns(userNode.getDescription(), from(UserNode::getDescription))
			.returns(node, from(UserNode::getNode))
			;
		// @formatter:on

	}

	@Test
	public void updateUserNodeWithCertificateAssociations() {
		// given
		storeNewUserNode();
		UserNodeCertificate unCert = storeNewCert(UserNodeCertificateStatus.v);

		// set up other user to update UserNode to
		User user2 = createNewUser("user2@localhost");

		// when
		// transfer UserNode to other user
		UserNode un = userNodeDao.get(this.node.getId());
		un.setUser(user2);
		Long updatedNodeId = userNodeDao.save(un);

		userNodeDao.getSqlSession().clearCache();

		UserNode updatedUserNode = userNodeDao.get(this.node.getId());

		// then

		assertThat("Node ID unchanged", updatedNodeId, equalTo(this.node.getId()));
		assertThat("UserNode moved user", updatedUserNode.getUserId(), equalTo(user2.getId()));

		UserNodeCertificate oldUnCert = userNodeCertificateDao.get(unCert.getId());
		UserNodeCertificate newUnCert = userNodeCertificateDao
				.get(new UserNodePK(user2.getId(), this.node.getId()));
		assertThat("UserNodeCertificate gone from old user", oldUnCert, nullValue());
		assertThat("UserNodeCertificate moved to new user", newUnCert, notNullValue());
		assertThat("UserNodeCertificate moved to new user", newUnCert.getCreated(),
				equalTo(unCert.getCreated()));
	}

	/**
	 * Test able to find for a user with multiple results.
	 */
	@Test
	public void findForUserMultipleResults() throws InterruptedException {
		storeNewUserNode();

		// create 2nd node for user
		Thread.sleep(100); // to give users different create dates

		setupTestNode(TEST_ID_2);
		UserNode newUserNode = new UserNode();
		newUserNode.setCreated(Instant.now());
		newUserNode.setDescription(TEST_DESC);
		newUserNode.setName(TEST_NAME);
		newUserNode.setNode(solarNodeDao.get(TEST_ID_2));
		newUserNode.setUser(this.user);
		Long userNode2 = userNodeDao.save(newUserNode);
		then(userNode2).isNotNull();

		List<UserNode> results = userNodeDao.findUserNodesForUser(this.user);
		then(results).isNotNull().hasSize(2);

		UserNode n1 = results.get(0);
		// @formatter:off
		then(n1)
			.isNotNull()
			.returns(userNodeId, from(UserNode::getId))
			.returns(node, from(UserNode::getNode))
			.returns(user, from(UserNode::getUser))
			.satisfies(un -> then(un.getCertificate()).isNull());
			;
		// @formatter:on
	}

	@Test
	public void archiveNode() {
		storeNewUserNode();
		userNodeDao.updateUserNodeArchivedStatus(user.getId(), new Long[] { node.getId() }, true);
	}

	@Test
	public void unArchiveNode() {
		archiveNode();
		userNodeDao.updateUserNodeArchivedStatus(user.getId(), new Long[] { node.getId() }, false);
	}

	@Test
	public void findArchivedForUser() {
		archiveNode();

		// create 2nd node for user
		setupTestNode(TEST_ID_2);
		UserNode newUserNode = new UserNode();
		newUserNode.setCreated(Instant.now());
		newUserNode.setDescription(TEST_DESC);
		newUserNode.setName(TEST_NAME);
		newUserNode.setNode(solarNodeDao.get(TEST_ID_2));
		newUserNode.setUser(this.user);
		Long userNode2 = userNodeDao.save(newUserNode);
		then(userNode2).isNotNull();

		List<UserNode> results = userNodeDao.findArchivedUserNodesForUser(this.user.getId());
		then(results).isNotNull().hasSize(1);

		UserNode n1 = results.get(0);
		// @formatter:off
		then(n1)
			.isNotNull()
			.returns(node, from(UserNode::getNode))
			.returns(user, from(UserNode::getUser))
			.satisfies(un -> then(un.getCertificate()).isNull());
			;
		// @formatter:on
	}

	@Test
	public void findForUserNoArchived() {
		archiveNode();

		// create 2nd node for user
		setupTestNode(TEST_ID_2);
		UserNode newUserNode = new UserNode();
		newUserNode.setCreated(Instant.now());
		newUserNode.setDescription(TEST_DESC);
		newUserNode.setName(TEST_NAME);
		newUserNode.setNode(solarNodeDao.get(TEST_ID_2));
		newUserNode.setUser(this.user);
		Long userNode2 = userNodeDao.save(newUserNode);
		then(userNode2).isNotNull();

		List<UserNode> results = userNodeDao.findUserNodesForUser(this.user);
		then(results).isNotNull().hasSize(1);

		UserNode n1 = results.get(0);
		// @formatter:off
		then(n1)
			.isNotNull()
			.returns(TEST_ID_2, from(UserNode::getId))
			.returns(user, from(UserNode::getUser))
			.satisfies(un -> then(un.getCertificate()).isNull());
			;
		// @formatter:on
	}

	@Test
	public void findForUserUnArchived() {
		archiveNode();

		List<UserNode> results = userNodeDao.findUserNodesForUser(this.user);
		then(results).isNotNull().isEmpty();

		userNodeDao.updateUserNodeArchivedStatus(this.user.getId(), new Long[] { this.node.getId() },
				false);

		results = userNodeDao.findUserNodesForUser(this.user);
		then(results).isNotNull().hasSize(1);

		UserNode n1 = results.get(0);
		// @formatter:off
		then(n1)
			.isNotNull()
			.returns(node, from(UserNode::getNode))
			.returns(user, from(UserNode::getUser))
			.satisfies(un -> then(un.getCertificate()).isNull());
			;
		// @formatter:on
	}

	private void storeNewUser() {
		this.user = createNewUser(TEST_EMAIL);
	}

	private UserNodeCertificate storeNewCert(UserNodeCertificateStatus status) {
		UserNodeCertificate newUserNodeCert = new UserNodeCertificate();
		newUserNodeCert.setCreated(Instant.now());
		newUserNodeCert.setNodeId(this.node.getId());
		newUserNodeCert.setUserId(this.user.getId());
		newUserNodeCert.setKeystoreData(TEST_CERT);
		newUserNodeCert.setStatus(status);
		newUserNodeCert.setRequestId("test req ID");
		UserNodePK id = userNodeCertificateDao.save(newUserNodeCert);
		then(id).isNotNull();
		return userNodeCertificateDao.get(id);
	}

	@Test
	public void findForUserWithNoCertificate() {
		storeNewUserNode();
		List<UserNode> results = userNodeDao.findUserNodesAndCertificatesForUser(user.getId());
		then(results).isNotNull().hasSize(1).first().as("Certificate not present").returns(null,
				UserNode::getCertificate);
	}

	@Test
	public void findForUserWithCertificate() {
		storeNewUserNode();
		final UserNodeCertificate cert1 = storeNewCert(UserNodeCertificateStatus.v);

		List<UserNode> results = userNodeDao.findUserNodesAndCertificatesForUser(user.getId());
		then(results).isNotNull().hasSize(1).first().returns(cert1, UserNode::getCertificate);
	}

	private UserNodeTransfer storeNewTransfer(Long nodeId) {
		UserNodeTransfer newUserNodeTransfer = new UserNodeTransfer();
		newUserNodeTransfer.setCreated(Instant.now());
		newUserNodeTransfer.setNodeId(nodeId);
		newUserNodeTransfer.setUserId(this.user.getId());
		newUserNodeTransfer.setEmail(TEST_EMAIL_2);
		userNodeDao.storeUserNodeTransfer(newUserNodeTransfer);
		UserNodeTransfer stored = userNodeDao.getUserNodeTransfer(newUserNodeTransfer.getId());
		then(stored).as("Inserted UserNodeTransfer").isNotNull();
		return stored;
	}

	@Test
	public void insertUserNodeTransfer() {
		storeNewUserNode();
		final UserNodeTransfer xfer1 = storeNewTransfer(this.node.getId());
		// @formatter:off
		then(xfer1)
			.isNotNull()
			.returns(node.getId(), from(UserNodeTransfer::getNodeId))
			.returns(user.getId(), from(UserNodeTransfer::getUserId))
			.returns(TEST_EMAIL_2, from(UserNodeTransfer::getEmail))
			.satisfies(x -> then(x.getCreated()).isNotNull())
			;
		// @formatter:on
	}

	@Test
	public void updateUserNodeTransfer() {
		insertUserNodeTransfer();
		UserNodeTransfer xfer1 = userNodeDao
				.getUserNodeTransfer(new UserNodePK(this.user.getId(), this.node.getId()));
		xfer1.setEmail("test.email.3@localhost");
		userNodeDao.storeUserNodeTransfer(xfer1);

		UserNodeTransfer xfer2 = userNodeDao.getUserNodeTransfer(xfer1.getId());

		// @formatter:off
		then(xfer2)
			.isNotNull()
			.as("Creation date unchanged")
			.returns(xfer1.getCreated(), from(UserNodeTransfer::getCreated))
			.returns(xfer1.getId(), from(UserNodeTransfer::getId))
			.returns(xfer1.getEmail(), from(UserNodeTransfer::getEmail))
			;
		// @formatter:on
	}

	@Test
	public void deleteUserNodeTransfer() {
		insertUserNodeTransfer();
		UserNodeTransfer xfer1 = userNodeDao
				.getUserNodeTransfer(new UserNodePK(this.user.getId(), this.node.getId()));
		userNodeDao.deleteUserNodeTransfer(xfer1);
		xfer1 = userNodeDao.getUserNodeTransfer(xfer1.getId());
		then(xfer1).as("UserNodeTransfer deleted").isNull();
	}

	@Test
	public void findUserNodeTransferForEmailNoMatch() {
		List<UserNodeTransfer> results = userNodeDao
				.findUserNodeTransferRequestsForEmail(this.user.getEmail());
		then(results).as("UserNodeTransfers for email results").isNotNull().isEmpty();
	}

	@Test
	public void findUserNodeTransferForEmail() throws Exception {
		insertUserNodeTransfer();

		// set up a 2nd node
		setupTestNode(TEST_NODE_2);
		SolarNode node2 = solarNodeDao.get(TEST_NODE_2);
		UserNode userNode2 = new UserNode(user, node2);
		userNode2 = userNodeDao.get(userNodeDao.save(userNode2));

		// set up a 2nd node transfer request
		storeNewTransfer(node2.getId());

		List<UserNodeTransfer> results = userNodeDao.findUserNodeTransferRequestsForEmail(TEST_EMAIL_2);
		then(results).as("UserNodeTransfers for email results").isNotNull().hasSize(2);

		// results will have same creation time from unit test transaction, so sorted by node ID ascending
		then(results).extracting(UserNodeTransfer::getNodeId).as("Results sorted by node ID ascending")
				.containsExactly(node2.getId(), node.getId());

		then(results).extracting(UserNodeTransfer::getEmail).containsOnly(TEST_EMAIL_2);
	}

	@Test
	public void findForUserWithTransferRequest() {
		insertUserNodeTransfer();
		List<UserNode> results = userNodeDao.findUserNodesAndCertificatesForUser(user.getId());
		then(results).isNotNull().hasSize(1).first().extracting(UserNode::getTransfer)
				.returns(user.getId(), from(UserNodeTransfer::getUserId))
				.returns(node.getId(), from(UserNodeTransfer::getNodeId))
				.returns(TEST_EMAIL_2, from(UserNodeTransfer::getEmail));
	}

	@Test
	public void findNodeIdsForUser() {
		storeNewUserNode();

		// create 2nd node for user
		setupTestNode(TEST_ID_2);
		UserNode newUserNode = new UserNode();
		newUserNode.setCreated(Instant.now());
		newUserNode.setDescription(TEST_DESC);
		newUserNode.setName(TEST_NAME);
		newUserNode.setNode(solarNodeDao.get(TEST_ID_2));
		newUserNode.setUser(this.user);
		Long userNode2 = userNodeDao.save(newUserNode);
		assertThat("UserNode created", userNode2, notNullValue());

		Set<Long> results = userNodeDao.findNodeIdsForUser(this.user.getId());
		assertThat("Node IDs", results, contains(TEST_ID_2, TEST_NODE_ID));
	}

	/*-
	private void storeAuthTokenWithNodeIds() {
		final Long nodeId2 = -2L;
		setupTestNode(nodeId2);
		UserAuthToken authToken = new UserAuthToken();
		authToken.setCreated(Instant.now());
		authToken.setUserId(this.user.getId());
		authToken.setAuthSecret(TEST_SECRET);
		authToken.setAuthToken(TEST_TOKEN);
		authToken.setStatus(SecurityTokenStatus.Active);
		authToken.setType(SecurityTokenType.ReadNodeData);
		authToken.setPolicy(new BasicSecurityPolicy.Builder()
				.withNodeIds(new HashSet<Long>(Arrays.asList(node.getId(), nodeId2))).build());
		String id = userAuthTokenDao.save(authToken);
		assertNotNull(id);
		this.userAuthToken = authToken;
	}*/

	private String randomTokenId() {
		return java.util.UUID.randomUUID().toString().replaceAll("-", "").substring(0, 20);
	}

	private UserAuthToken tokenForUser(SecurityTokenType type) {
		final String tokenId = randomTokenId();
		UserAuthToken authToken = new UserAuthToken();
		authToken.setCreated(Instant.now());
		authToken.setUserId(this.user.getId());
		authToken.setAuthSecret("password");
		authToken.setAuthToken(tokenId);
		authToken.setStatus(SecurityTokenStatus.Active);
		authToken.setType(type);
		return authToken;
	}

	@Test
	public void findNodeIdsForUserTokenNoNodes() {
		// create some OTHER user with a node, to be sure
		storeNewUserNode();

		// create a new user without any nodes
		this.user = createNewUser(TEST_EMAIL_2);
		final UserAuthToken authToken = tokenForUser(SecurityTokenType.User);
		userAuthTokenDao.save(authToken);

		Set<Long> nodeIds = userNodeDao.findNodeIdsForToken(authToken.getId());
		assertThat("No nodes returned", nodeIds, hasSize(0));
	}

	@Test
	public void findNodeIdsForUserTokenSingleNode() {
		storeNewUserNode();
		final UserAuthToken authToken = tokenForUser(SecurityTokenType.User);
		userAuthTokenDao.save(authToken);

		Set<Long> nodeIds = userNodeDao.findNodeIdsForToken(authToken.getId());
		assertThat(nodeIds, contains(this.node.getId()));
	}

	@Test
	public void findNodeIdsForUserTokenSingleNodeTokenDisabled() {
		storeNewUserNode();
		final UserAuthToken authToken = tokenForUser(SecurityTokenType.User);
		authToken.setStatus(SecurityTokenStatus.Disabled);
		userAuthTokenDao.save(authToken);

		Set<Long> nodeIds = userNodeDao.findNodeIdsForToken(authToken.getId());
		assertThat("No nodes returned", nodeIds, hasSize(0));
	}

	@Test
	public void findNodeIdsForUserTokenMultipleNodes() {
		Set<Long> expectedNodeIds = new LinkedHashSet<Long>();
		for ( int i = 0; i < 3; i++ ) {
			Long nodeId = TEST_ID_2 - i;
			setupTestNode(nodeId);
			UserNode newUserNode = new UserNode();
			newUserNode.setCreated(Instant.now());
			newUserNode.setDescription(TEST_DESC);
			newUserNode.setName(TEST_NAME);
			newUserNode.setNode(solarNodeDao.get(nodeId));
			newUserNode.setUser(this.user);
			userNodeDao.save(newUserNode);
			expectedNodeIds.add(nodeId);
		}

		final UserAuthToken authToken = tokenForUser(SecurityTokenType.User);
		userAuthTokenDao.save(authToken);

		Set<Long> nodeIds = userNodeDao.findNodeIdsForToken(authToken.getId());
		assertThat("User nodes", nodeIds, equalTo(expectedNodeIds));
	}

	@Test
	public void findNodeIdsForUserTokenMultipleNodesFilteredByPolicy() {
		for ( int i = 0; i < 3; i++ ) {
			Long nodeId = TEST_ID_2 - i;
			setupTestNode(nodeId);
			UserNode newUserNode = new UserNode();
			newUserNode.setCreated(Instant.now());
			newUserNode.setDescription(TEST_DESC);
			newUserNode.setName(TEST_NAME);
			newUserNode.setNode(solarNodeDao.get(nodeId));
			newUserNode.setUser(this.user);
			userNodeDao.save(newUserNode);
		}

		final UserAuthToken authToken = tokenForUser(SecurityTokenType.User);
		authToken.setPolicy(new BasicSecurityPolicy.Builder()
				.withNodeIds(new HashSet<Long>(Arrays.asList(TEST_ID_2, TEST_ID_2 - 1))).build());
		userAuthTokenDao.save(authToken);

		Set<Long> nodeIds = userNodeDao.findNodeIdsForToken(authToken.getId());
		assertThat("Policy filtered user nodes", nodeIds, contains(TEST_ID_2 - 1, TEST_ID_2));
	}

	@Test
	public void findNodeIdsForReadNodeDataTokenMultipleNodesFilteredByPolicy() {
		for ( int i = 0; i < 3; i++ ) {
			Long nodeId = TEST_ID_2 - i;
			setupTestNode(nodeId);
			UserNode newUserNode = new UserNode();
			newUserNode.setCreated(Instant.now());
			newUserNode.setDescription(TEST_DESC);
			newUserNode.setName(TEST_NAME);
			newUserNode.setNode(solarNodeDao.get(nodeId));
			newUserNode.setUser(this.user);
			userNodeDao.save(newUserNode);
		}

		final UserAuthToken authToken = tokenForUser(SecurityTokenType.ReadNodeData);
		authToken.setPolicy(new BasicSecurityPolicy.Builder()
				.withNodeIds(new HashSet<Long>(Arrays.asList(TEST_ID_2, TEST_ID_2 - 1))).build());
		userAuthTokenDao.save(authToken);

		Set<Long> nodeIds = userNodeDao.findNodeIdsForToken(authToken.getId());
		assertThat("Policy filtered user nodes", nodeIds, contains(TEST_ID_2 - 1, TEST_ID_2));
	}
}
