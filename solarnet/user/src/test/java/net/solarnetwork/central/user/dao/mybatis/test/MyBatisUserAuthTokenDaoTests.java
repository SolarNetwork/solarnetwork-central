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

import static org.assertj.core.api.BDDAssertions.then;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.central.dao.mybatis.MyBatisSolarNodeDao;
import net.solarnetwork.central.domain.SolarNode;
import net.solarnetwork.central.security.BasicSecurityPolicy;
import net.solarnetwork.central.security.SecurityToken;
import net.solarnetwork.central.security.SecurityTokenStatus;
import net.solarnetwork.central.security.SecurityTokenType;
import net.solarnetwork.central.user.dao.mybatis.MyBatisUserAuthTokenDao;
import net.solarnetwork.central.user.domain.User;
import net.solarnetwork.central.user.domain.UserAuthToken;
import net.solarnetwork.security.Snws2AuthorizationBuilder;

/**
 * Test cases for the {@link MyBatisUserAuthTokenDao} class.
 * 
 * @author matt
 * @version 2.1
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
		assertThat("Node available", this.node, is(notNullValue()));
		deleteFromTables(DELETE_TABLES);
		this.user = createNewUser(TEST_EMAIL);
		assertThat("User available", this.user, is(notNullValue()));
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
		assertThat("ID returned", id, is(notNullValue()));
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
		assertThat("ID returned", id, is(notNullValue()));
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
		assertThat("ID returned", id, is(notNullValue()));
		this.userAuthToken = authToken;
	}

	@Test
	public void storeNewWithInfo() {
		UserAuthToken authToken = new UserAuthToken();
		authToken.setCreated(Instant.now());
		authToken.setUserId(this.user.getId());
		authToken.setAuthSecret(TEST_SECRET);
		authToken.setAuthToken(TEST_TOKEN);
		authToken.setStatus(SecurityTokenStatus.Active);
		authToken.setType(SecurityTokenType.User);
		authToken.setName(UUID.randomUUID().toString());
		authToken.setDescription(UUID.randomUUID().toString());
		String id = userAuthTokenDao.store(authToken);
		assertThat("ID returned", id, is(notNullValue()));
		this.userAuthToken = authToken;
	}

	private void assertEqual(UserAuthToken entity, UserAuthToken expected) {
		assertThat("UserAuthToken should exist", entity, is(notNullValue()));
		assertThat("Created date should be set", entity.getCreated(), is(notNullValue()));
		assertThat("Token ID", entity.getId(), is(equalTo(expected.getId())));
		assertThat("Status", entity.getStatus(), is(equalTo(expected.getStatus())));
		assertThat("Auth token", entity.getAuthToken(), is(equalTo(expected.getAuthToken())));
		assertThat("Auth secret not returned", entity.getAuthSecret(), is(nullValue()));
		assertThat("Node IDs", entity.getNodeIds(), is(equalTo(expected.getNodeIds())));
		assertThat("Name", entity.getName(), is(equalTo(expected.getName())));
		assertThat("Description", entity.getDescription(), is(equalTo(expected.getDescription())));
	}

	@Test
	public void getByPrimaryKey() {
		storeNew();
		UserAuthToken token = userAuthTokenDao.get(userAuthToken.getId());
		assertEqual(token, this.userAuthToken);
	}

	@Test
	public void getByPrimaryKeyWithNodeId() {
		storeNewWithNodeId();
		UserAuthToken token = userAuthTokenDao.get(userAuthToken.getId());
		assertEqual(token, this.userAuthToken);
	}

	@Test
	public void securityToken_withNodeId() {
		storeNewWithNodeId();
		SecurityToken token = userAuthTokenDao.securityTokenForId(userAuthToken.getId());

		// @formatter:off
		then(token)
			.as("Token returned")
			.isNotNull()
			.as("Token ID")
			.returns(this.userAuthToken.getId(), SecurityToken::getToken)
			.as("Token type")
			.returns(this.userAuthToken.getType(), SecurityToken::getTokenType)
			.as("User ID")
			.returns(this.userAuthToken.getUserId(), SecurityToken::getUserId)
			.as("Policy")
			.returns(this.userAuthToken.getPolicy(), SecurityToken::getPolicy)
			.as("Is token")
			.returns(true, SecurityToken::isAuthenticatedWithToken)
			;
		// @formatter:on
	}

	@Test
	public void securityToken_notFound() {
		storeNewWithNodeId();
		SecurityToken token = userAuthTokenDao.securityTokenForId(UUID.randomUUID().toString());

		// @formatter:off
		then(token)
			.as("Token is null")
			.isNull()
			;
		// @formatter:on
	}

	@Test
	public void getByPrimaryKeyWithNodeIds() {
		storeNewWithNodeIds();
		UserAuthToken token = userAuthTokenDao.get(userAuthToken.getId());
		assertEqual(token, this.userAuthToken);
	}

	@Test
	public void getByPrimaryKeyWithInfo() {
		storeNewWithInfo();
		UserAuthToken token = userAuthTokenDao.get(userAuthToken.getId());
		assertEqual(token, this.userAuthToken);
	}

	@Test
	public void update_status() {
		storeNew();
		UserAuthToken token = userAuthTokenDao.get(userAuthToken.getId());
		token.setStatus(SecurityTokenStatus.Disabled);
		UserAuthToken updated = userAuthTokenDao.get(userAuthTokenDao.store(token));
		assertEqual(updated, token);
	}

	@Test
	public void update_info() {
		storeNew();
		UserAuthToken token = userAuthTokenDao.get(userAuthToken.getId());
		token.setName(UUID.randomUUID().toString());
		token.setDescription(UUID.randomUUID().toString());
		UserAuthToken updated = userAuthTokenDao.get(userAuthTokenDao.store(token));
		assertEqual(updated, token);
	}

	@Test
	public void delete() {
		storeNew();
		UserAuthToken token = userAuthTokenDao.get(userAuthToken.getId());
		userAuthTokenDao.delete(token);
		token = userAuthTokenDao.get(token.getId());
		assertThat("Token no longer available", token, is(nullValue()));
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
		assertThat("Results available", results, is(notNullValue()));
		assertThat("All results for user returned", results, hasSize(2));
		assertThat(results.get(0), is(equalTo(this.userAuthToken)));
		assertThat(results.get(1), is(equalTo(authToken2)));

		results = userAuthTokenDao.findUserAuthTokensForUser(user2.getId());
		assertThat("Results available", results, is(notNullValue()));
		assertThat("All results for user 2 returned", results, hasSize(1));
		assertThat(results.get(0), is(equalTo(authToken3)));
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
