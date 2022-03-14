/* ==================================================================
 * MyBatisUserAuthTokenDaoTests.java - Nov 11, 2014 6:54:26 AM
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.central.dao.mybatis.MyBatisSolarNodeDao;
import net.solarnetwork.central.domain.SolarNode;
import net.solarnetwork.central.security.BasicSecurityPolicy;
import net.solarnetwork.central.security.SecurityTokenType;
import net.solarnetwork.central.security.SecurityTokenStatus;
import net.solarnetwork.central.user.dao.mybatis.MyBatisUserAuthTokenDao;
import net.solarnetwork.central.user.domain.User;
import net.solarnetwork.central.user.domain.UserAuthToken;
import net.solarnetwork.security.Snws2AuthorizationBuilder;

/**
 * Test cases for the {@link MyBatisUserAuthTokenDao} class.
 * 
 * @author matt
 * @version 2.0
 */
public class MyBatisUserAuthTokenDaoTests extends AbstractMyBatisUserDaoTestSupport {

	private static final String[] DELETE_TABLES = new String[] { "solaruser.user_auth_token",
			"solaruser.user_node", "solaruser.user_user" };

	private static final String TEST_TOKEN = "public.token12345678";
	private static final String TEST_SECRET = "secret.token12345678";
	private static final String TEST_TOKEN2 = "public.token12345679";
	private static final String TEST_TOKEN3 = "public.token12345677";

	private MyBatisSolarNodeDao solarNodeDao;

	private MyBatisUserAuthTokenDao userAuthTokenDao;

	private User user = null;
	private SolarNode node = null;
	private UserAuthToken userAuthToken = null;

	@Before
	public void setUp() throws Exception {
		solarNodeDao = new MyBatisSolarNodeDao();
		solarNodeDao.setSqlSessionFactory(getSqlSessionFactory());
		userAuthTokenDao = new MyBatisUserAuthTokenDao();
		userAuthTokenDao.setSqlSessionFactory(getSqlSessionFactory());

		setupTestNode();
		this.node = solarNodeDao.get(TEST_NODE_ID);
		assertNotNull(this.node);
		deleteFromTables(DELETE_TABLES);
		this.user = createNewUser(TEST_EMAIL);
		assertNotNull(this.user);
		userAuthToken = null;
	}

	@Test
	public void storeNew() {
		UserAuthToken authToken = new UserAuthToken();
		authToken.setCreated(Instant.now());
		authToken.setUserId(this.user.getId());
		authToken.setAuthSecret(TEST_SECRET);
		authToken.setAuthToken(TEST_TOKEN);
		authToken.setStatus(SecurityTokenStatus.Active);
		authToken.setType(SecurityTokenType.User);
		String id = userAuthTokenDao.store(authToken);
		assertNotNull(id);
		this.userAuthToken = authToken;
	}

	@Test
	public void storeNewWithNodeId() {
		UserAuthToken authToken = new UserAuthToken();
		authToken.setCreated(Instant.now());
		authToken.setUserId(this.user.getId());
		authToken.setAuthSecret(TEST_SECRET);
		authToken.setAuthToken(TEST_TOKEN);
		authToken.setStatus(SecurityTokenStatus.Active);
		authToken.setType(SecurityTokenType.ReadNodeData);
		authToken.setPolicy(new BasicSecurityPolicy.Builder()
				.withNodeIds(Collections.singleton(node.getId())).build());
		String id = userAuthTokenDao.store(authToken);
		assertNotNull(id);
		this.userAuthToken = authToken;
	}

	@Test
	public void storeNewWithNodeIds() {
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
		String id = userAuthTokenDao.store(authToken);
		assertNotNull(id);
		this.userAuthToken = authToken;
	}

	private void validate(UserAuthToken token, UserAuthToken entity) {
		assertNotNull("UserAuthToken should exist", entity);
		assertNotNull("Created date should be set", entity.getCreated());
		assertEquals(token.getId(), entity.getId());
		assertEquals(token.getStatus(), entity.getStatus());
		assertEquals(token.getAuthToken(), entity.getAuthToken());
		assertNull(entity.getAuthSecret()); // the secret is NOT returned
		assertEquals(token.getNodeIds(), entity.getNodeIds());
	}

	@Test
	public void getByPrimaryKey() {
		storeNew();
		UserAuthToken token = userAuthTokenDao.get(userAuthToken.getId());
		validate(this.userAuthToken, token);
	}

	@Test
	public void getByPrimaryKeyWithNodeId() {
		storeNewWithNodeId();
		UserAuthToken token = userAuthTokenDao.get(userAuthToken.getId());
		validate(this.userAuthToken, token);
	}

	@Test
	public void getByPrimaryKeyWithNodeIds() {
		storeNewWithNodeIds();
		UserAuthToken token = userAuthTokenDao.get(userAuthToken.getId());
		validate(this.userAuthToken, token);
	}

	@Test
	public void update() {
		storeNew();
		UserAuthToken token = userAuthTokenDao.get(userAuthToken.getId());
		token.setStatus(SecurityTokenStatus.Disabled);
		UserAuthToken updated = userAuthTokenDao.get(userAuthTokenDao.store(token));
		validate(token, updated);
	}

	@Test
	public void delete() {
		storeNew();
		UserAuthToken token = userAuthTokenDao.get(userAuthToken.getId());
		userAuthTokenDao.delete(token);
		token = userAuthTokenDao.get(token.getId());
		assertNull(token);
	}

	@Test
	public void findForUser() {
		storeNew();
		UserAuthToken authToken2 = new UserAuthToken(TEST_TOKEN2, this.user.getId(), TEST_SECRET,
				SecurityTokenType.User);
		userAuthTokenDao.store(authToken2);
		User user2 = createNewUser(TEST_EMAIL + "2");
		UserAuthToken authToken3 = new UserAuthToken(TEST_TOKEN3, user2.getId(), TEST_SECRET,
				SecurityTokenType.User);
		userAuthTokenDao.store(authToken3);

		List<UserAuthToken> results = userAuthTokenDao.findUserAuthTokensForUser(this.user.getId());
		assertNotNull(results);
		assertEquals(2, results.size());
		assertEquals(this.userAuthToken, results.get(0));
		assertEquals(authToken2, results.get(1));

		results = userAuthTokenDao.findUserAuthTokensForUser(user2.getId());
		assertNotNull(results);
		assertEquals(1, results.size());
		assertEquals(authToken3, results.get(0));
	}

	private Instant getTestDate() {
		return LocalDateTime.of(2017, 3, 25, 14, 30, 0).atZone(ZoneOffset.UTC).toInstant();
	}

	@Test
	public void createAuthBuilder() {
		storeNew();
		Instant date = getTestDate();
		Snws2AuthorizationBuilder builder = userAuthTokenDao.createSnws2AuthorizationBuilder(TEST_TOKEN,
				date);

		assertThat("Builder", builder, notNullValue());
		assertThat("Signing key", builder.signingKeyHex(),
				equalTo("4ffe547294445f9f4e89f80c4c0801b95d4329a9936389582ae5a6a5758c70fe"));

		builder.host("localhost").path("/api/test");

		final String result = builder.build();
		assertThat("Authorization header", result, equalTo(
				"SNWS2 Credential=public.token12345678,SignedHeaders=date;host,Signature=535124f5f333c0aebe42996a429a2a6e4b347dcc2ac2d8cbb8ac6b641fafbab7"));

	}

}
