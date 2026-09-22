/* ==================================================================
 * UserBizSecurityPolicyTests.java - 22/09/2026 6:48:05 pm
 *
 * Copyright 2026 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.aop.test;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.MILLIS;
import static java.util.stream.Collectors.toSet;
import static net.solarnetwork.central.test.CommonDbTestUtils.insertUserNodeConfirmation;
import static net.solarnetwork.central.test.aop.SecuredProxies.securedProxy;
import static net.solarnetwork.util.ObjectUtils.nonnull;
import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenExceptionOfType;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.StreamSupport;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import net.solarnetwork.central.common.dao.jdbc.JdbcSolarNodeOwnershipDao;
import net.solarnetwork.central.dao.mybatis.MyBatisSolarLocationDao;
import net.solarnetwork.central.dao.mybatis.MyBatisSolarNodeDao;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.security.SecurityTokenStatus;
import net.solarnetwork.central.security.SecurityTokenType;
import net.solarnetwork.central.test.tenant.SecurityContextExtension;
import net.solarnetwork.central.test.tenant.TestActor;
import net.solarnetwork.central.test.tenant.TestTenant;
import net.solarnetwork.central.test.tenant.TestTenants;
import net.solarnetwork.central.test.tenant.TestToken;
import net.solarnetwork.central.user.aop.UserAuthTokenSecurityAspect;
import net.solarnetwork.central.user.aop.UserSecurityAspect;
import net.solarnetwork.central.user.biz.UserBiz;
import net.solarnetwork.central.user.biz.dao.DaoUserBiz;
import net.solarnetwork.central.user.dao.BasicUserAuthTokenFilter;
import net.solarnetwork.central.user.dao.BasicUserNodeFilter;
import net.solarnetwork.central.user.dao.mybatis.MyBatisUserAlertDao;
import net.solarnetwork.central.user.dao.mybatis.MyBatisUserAuthTokenDao;
import net.solarnetwork.central.user.dao.mybatis.MyBatisUserNodeCertificateDao;
import net.solarnetwork.central.user.dao.mybatis.MyBatisUserNodeConfirmationDao;
import net.solarnetwork.central.user.dao.mybatis.MyBatisUserNodeDao;
import net.solarnetwork.central.user.dao.mybatis.test.AbstractMyBatisUserDaoTestSupport;
import net.solarnetwork.central.user.domain.UserAuthToken;
import net.solarnetwork.central.user.domain.UserNode;
import net.solarnetwork.central.user.domain.UserNodeConfirmation;
import net.solarnetwork.central.user.domain.UserNodeInfo;
import net.solarnetwork.codec.jackson.JsonUtils;
import net.solarnetwork.domain.BasicSecurityPolicy;
import net.solarnetwork.domain.SecurityPolicy;

/**
 * Verify security policies are enforced on {@link UserBiz} with the
 * {@code UserSecurityAspect} and {@code UserAuthTokenSecurityAspect} aspects
 * applied, using the database.
 *
 * <p>
 * The restricted token's policy allows only tenant A's other private node.
 * Several checks here are made by {@code DaoUserBiz} rather than the aspects,
 * such as the ownership of tokens being managed and of another user's public
 * node, which every actor can read.
 * </p>
 *
 * <p>
 * Managing tokens requires an unrestricted policy, but an unrestricted policy
 * can still have an expiry date and deny refreshing. {@code DaoUserBiz}
 * applies those constraints of the actor's policy to every token policy the
 * actor creates or updates, so they cannot be widened or removed.
 * </p>
 *
 * @author matt
 * @version 1.1
 */
@ExtendWith(SecurityContextExtension.class)
public class UserBizSecurityPolicyTests extends AbstractMyBatisUserDaoTestSupport {

	private TestTenants tenants;
	private TestTenant a;
	private TestTenant b;
	private UserBiz biz;

	@BeforeEach
	public void setupBiz() {
		tenants = new TestTenants();
		a = tenants.a();
		b = tenants.b();
		tenants.insert(jdbcTemplate);

		final MyBatisUserNodeDao userNodeDao = new MyBatisUserNodeDao();
		userNodeDao.setSqlSessionFactory(getSqlSessionFactory());
		final MyBatisUserNodeConfirmationDao confirmationDao = new MyBatisUserNodeConfirmationDao();
		confirmationDao.setSqlSessionFactory(getSqlSessionFactory());
		final MyBatisUserNodeCertificateDao certificateDao = new MyBatisUserNodeCertificateDao();
		certificateDao.setSqlSessionFactory(getSqlSessionFactory());
		final MyBatisSolarNodeDao solarNodeDao = new MyBatisSolarNodeDao();
		solarNodeDao.setSqlSessionFactory(getSqlSessionFactory());
		final MyBatisSolarLocationDao solarLocationDao = new MyBatisSolarLocationDao();
		solarLocationDao.setSqlSessionFactory(getSqlSessionFactory());
		final MyBatisUserAuthTokenDao userAuthTokenDao = new MyBatisUserAuthTokenDao();
		userAuthTokenDao.setSqlSessionFactory(getSqlSessionFactory());
		final MyBatisUserAlertDao userAlertDao = new MyBatisUserAlertDao();
		userAlertDao.setSqlSessionFactory(getSqlSessionFactory());

		final JdbcSolarNodeOwnershipDao ownershipDao = new JdbcSolarNodeOwnershipDao(jdbcTemplate);
		biz = securedProxy(
				(UserBiz) new DaoUserBiz(userDao, userNodeDao, confirmationDao, certificateDao,
						solarNodeDao, solarLocationDao, userAuthTokenDao, userAlertDao),
				new UserSecurityAspect(ownershipDao), new UserAuthTokenSecurityAspect(ownershipDao))
				.proxy();
	}

	private static Set<Long> nodeIds(List<UserNode> nodes) {
		return nodes.stream().map(n -> n.getNode().getId()).collect(toSet());
	}

	private static Set<Long> nodeInfoIds(Iterable<UserNodeInfo> nodes) {
		return StreamSupport.stream(nodes.spliterator(), false).map(UserNodeInfo::nodeId)
				.collect(toSet());
	}

	private static Set<String> tokenIds(Iterable<UserAuthToken> tokens) {
		return StreamSupport.stream(tokens.spliterator(), false).map(UserAuthToken::getId)
				.collect(toSet());
	}

	private static BasicUserNodeFilter nodes(Long... nodeIds) {
		final BasicUserNodeFilter filter = new BasicUserNodeFilter();
		filter.setNodeIds(nodeIds);
		return filter;
	}

	private static BasicUserAuthTokenFilter token(String tokenId) {
		final BasicUserAuthTokenFilter filter = new BasicUserAuthTokenFilter();
		filter.setIdentifier(tokenId);
		return filter;
	}

	private boolean archived(Long nodeId) {
		return nonnull(jdbcTemplate.queryForObject(
				"SELECT archived FROM solaruser.user_node WHERE node_id = ?", Boolean.class, nodeId),
				"Archived");
	}

	private long tokenCount(Long userId) {
		return nonnull(jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM solaruser.user_auth_token WHERE user_id = ?", Long.class,
				userId), "Count");
	}

	private @Nullable String tokenColumn(String column, String tokenId) {
		return jdbcTemplate.queryForObject(
				"SELECT %s::text FROM solaruser.user_auth_token WHERE auth_token = ?".formatted(column),
				String.class, tokenId);
	}

	private @Nullable SecurityPolicy storedPolicy(String tokenId) {
		final String json = tokenColumn("jpolicy", tokenId);
		return (json != null ? JsonUtils.getObjectFromJSON(json, SecurityPolicy.class) : null);
	}

	private static SecurityPolicy emptyPolicy() {
		return BasicSecurityPolicy.builder().build();
	}

	private static SecurityPolicy expiringPolicy(Instant notAfter) {
		return BasicSecurityPolicy.builder().withNotAfter(notAfter).build();
	}

	private static SecurityPolicy refreshPolicy(boolean refreshAllowed) {
		return BasicSecurityPolicy.builder().withRefreshAllowed(refreshAllowed).build();
	}

	private SecurityPolicy nodePolicy() {
		return BasicSecurityPolicy.builder().withNodeIds(Set.of(a.privateNodeId())).build();
	}

	private static Instant expiry() {
		return Instant.now().truncatedTo(MILLIS).plus(1, DAYS);
	}

	private TestToken constrainedToken(SecurityPolicy policy) {
		final TestToken token = a.newToken(SecurityTokenType.User, policy);
		token.insert(jdbcTemplate);
		return token;
	}

	private TestToken expiringToken() {
		return constrainedToken(expiringPolicy(expiry()));
	}

	private TestToken noRefreshToken() {
		return constrainedToken(refreshPolicy(false));
	}

	private static Instant notAfter(TestToken token) {
		return nonnull(nonnull(token.policy(), "Policy").getNotAfter(), "Expiry");
	}

	private UserAuthToken generate(SecurityTokenType type, SecurityPolicy policy) {
		return nonnull(biz.generateUserAuthToken(a.userId(), type, policy), "Token");
	}

	@Test
	public void getUserNodes_user_ownActiveNodes() {
		// GIVEN
		a.userActor().become();

		// WHEN
		final List<UserNode> results = biz.getUserNodes(a.userId());

		// THEN
		// @formatter:off
		then(nodeIds(results))
			.as("Only the user's active nodes returned")
			.containsExactlyInAnyOrder(a.privateNodeId(), a.otherPrivateNodeId(), a.publicNodeId())
			;
		// @formatter:on
	}

	@Test
	public void getUserNodes_restrictedToken_policyNodesOnly() {
		// GIVEN
		a.restrictedTokenActor().become();

		// WHEN
		final List<UserNode> results = biz.getUserNodes(a.userId());

		// THEN
		// @formatter:off
		then(nodeIds(results))
			.as("Only the policy node returned")
			.containsExactly(a.otherPrivateNodeId())
			;
		// @formatter:on
	}

	@Test
	public void getArchivedUserNodes_restrictedToken_policyNodesOnly() {
		// GIVEN
		a.tokenActor().become();
		final List<UserNode> tokenResults = biz.getArchivedUserNodes(a.userId());
		a.restrictedTokenActor().become();

		// WHEN
		final List<UserNode> results = biz.getArchivedUserNodes(a.userId());

		// THEN
		// @formatter:off
		then(nodeIds(tokenResults))
			.as("Unrestricted token has the archived node")
			.containsExactly(a.archivedNodeId())
			;
		then(results)
			.as("Archived node is not a policy node")
			.isEmpty()
			;
		// @formatter:on
	}

	@Test
	public void findUserNodeInfos_restrictedToken_policyNodesOnly() {
		// GIVEN
		a.restrictedTokenActor().become();

		// WHEN
		var results = biz.findUserNodeInfos(a.userId(), new BasicUserNodeFilter());

		// THEN
		// @formatter:off
		then(nodeInfoIds(results))
			.as("Only the policy node found")
			.containsExactly(a.otherPrivateNodeId())
			;
		// @formatter:on
	}

	@Test
	public void findUserNodeInfos_restrictedToken_nonPolicyNode_denied() {
		// GIVEN
		a.restrictedTokenActor().become();

		// THEN
		// @formatter:off
		thenExceptionOfType(AuthorizationException.class)
			.as("Finding a non-policy node denied")
			.isThrownBy(() -> biz.findUserNodeInfos(a.userId(), nodes(a.privateNodeId())))
			;
		// @formatter:on
	}

	@Test
	public void findUserNodeInfos_user_otherUserNodes_notFound() {
		// GIVEN
		a.userActor().become();

		// WHEN
		var results = biz.findUserNodeInfos(a.userId(),
				nodes(a.privateNodeId(), b.privateNodeId(), b.publicNodeId()));

		// THEN
		// @formatter:off
		then(nodeInfoIds(results))
			.as("Other user's nodes not found")
			.containsExactly(a.privateNodeId())
			;
		// @formatter:on
	}

	@Test
	public void getPendingUserNodeConfirmations_restrictedToken_none() {
		// GIVEN
		final Long confirmationId = insertUserNodeConfirmation(jdbcTemplate, a.userId());
		insertUserNodeConfirmation(jdbcTemplate, b.userId());
		a.tokenActor().become();
		final List<UserNodeConfirmation> tokenResults = biz
				.getPendingUserNodeConfirmations(a.userId());
		a.restrictedTokenActor().become();

		// WHEN
		final List<UserNodeConfirmation> results = biz.getPendingUserNodeConfirmations(a.userId());

		// THEN
		// @formatter:off
		then(tokenResults)
			.as("Unrestricted token has only the user's confirmation")
			.extracting(UserNodeConfirmation::getId)
			.containsExactly(confirmationId)
			;
		then(results)
			.as("Pending confirmations have no node, so none match the policy")
			.isEmpty()
			;
		// @formatter:on
	}

	@Test
	public void getUserNode_otherUserPublicNode_denied() {
		// GIVEN
		a.userActor().become();

		// THEN
		// @formatter:off
		thenExceptionOfType(AuthorizationException.class)
			.as("Another user's public node denied")
			.isThrownBy(() -> biz.getUserNode(a.userId(), b.publicNodeId()))
			;
		// @formatter:on
	}

	@Test
	public void updateUserNodeArchivedStatus_restrictedToken_policyNode() {
		// GIVEN
		a.restrictedTokenActor().become();

		// WHEN
		biz.updateUserNodeArchivedStatus(a.userId(), new Long[] { a.otherPrivateNodeId() }, true);

		// THEN
		// @formatter:off
		then(archived(a.otherPrivateNodeId()))
			.as("Policy node archived")
			.isTrue()
			;
		// @formatter:on
	}

	@Test
	public void updateUserNodeArchivedStatus_restrictedToken_nonPolicyNode_denied() {
		// GIVEN
		a.restrictedTokenActor().become();

		// THEN
		// @formatter:off
		thenExceptionOfType(AuthorizationException.class)
			.as("Archiving a non-policy node denied")
			.isThrownBy(() -> biz.updateUserNodeArchivedStatus(a.userId(),
					new Long[] { a.otherPrivateNodeId(), a.privateNodeId() }, true))
			;
		then(archived(a.otherPrivateNodeId()))
			.as("Policy node not archived")
			.isFalse()
			;
		then(archived(a.privateNodeId()))
			.as("Non-policy node not archived")
			.isFalse()
			;
		// @formatter:on
	}

	@Test
	public void getAllUserAuthTokens_token_ownUserTokensOnly() {
		// GIVEN
		a.tokenActor().become();

		// WHEN
		final List<UserAuthToken> results = biz.getAllUserAuthTokens(a.userId());

		// THEN
		// @formatter:off
		then(tokenIds(results))
			.as("All of the user's tokens returned")
			.containsExactlyInAnyOrder(a.userToken().tokenId(), a.restrictedUserToken().tokenId(),
					a.dataToken().tokenId())
			;
		// @formatter:on
	}

	@Test
	public void getAllUserAuthTokens_restrictedToken_ownTokenOnly() {
		// GIVEN
		a.restrictedTokenActor().become();

		// WHEN
		final List<UserAuthToken> results = biz.getAllUserAuthTokens(a.userId());

		// THEN
		// @formatter:off
		then(tokenIds(results))
			.as("Only the restricted token itself returned")
			.containsExactly(a.restrictedUserToken().tokenId())
			;
		// @formatter:on
	}

	@Test
	public void listUserAuthTokensForUser_restrictedToken_ownTokenOnly() {
		// GIVEN
		a.restrictedTokenActor().become();

		// WHEN
		var results = biz.listUserAuthTokensForUser(a.userId(), token(a.userToken().tokenId()));

		// THEN
		// @formatter:off
		then(tokenIds(results))
			.as("Only the restricted token itself returned")
			.containsExactly(a.restrictedUserToken().tokenId())
			;
		// @formatter:on
	}

	@Test
	public void listUserAuthTokensForUser_user_otherUserToken_notFound() {
		// GIVEN
		a.userActor().become();

		// WHEN
		var results = biz.listUserAuthTokensForUser(a.userId(), token(b.userToken().tokenId()));

		// THEN
		// @formatter:off
		then(tokenIds(results))
			.as("Other user's token not found")
			.isEmpty()
			;
		// @formatter:on
	}

	@Test
	public void generateUserAuthToken_restrictedToken_denied() {
		// GIVEN
		final long count = tokenCount(a.userId());
		a.restrictedTokenActor().become();

		// THEN
		// @formatter:off
		thenExceptionOfType(AuthorizationException.class)
			.as("Restricted token cannot create tokens")
			.isThrownBy(() -> biz.generateUserAuthToken(a.userId(), SecurityTokenType.User,
					BasicSecurityPolicy.builder().build()))
			;
		then(tokenCount(a.userId()))
			.as("No token created")
			.isEqualTo(count)
			;
		// @formatter:on
	}

	@Test
	public void generateUserAuthToken_otherUserPolicyNode_denied() {
		// GIVEN
		final long count = tokenCount(a.userId());
		a.userActor().become();

		// THEN
		// @formatter:off
		thenExceptionOfType(AuthorizationException.class)
			.as("Token policy cannot include another user's node")
			.isThrownBy(() -> biz.generateUserAuthToken(a.userId(), SecurityTokenType.ReadNodeData,
					BasicSecurityPolicy.builder().withNodeIds(Set.of(b.publicNodeId())).build()))
			;
		then(tokenCount(a.userId()))
			.as("No token created")
			.isEqualTo(count)
			;
		// @formatter:on
	}

	@Test
	public void deleteUserAuthToken_otherUserToken_denied() {
		// GIVEN
		a.userActor().become();

		// THEN
		// @formatter:off
		thenExceptionOfType(AuthorizationException.class)
			.as("Deleting another user's token denied")
			.isThrownBy(() -> biz.deleteUserAuthToken(a.userId(), b.userToken().tokenId()))
			;
		then(tokenColumn("status", b.userToken().tokenId()))
			.as("Other user's token not deleted")
			.isEqualTo(SecurityTokenStatus.Active.name())
			;
		// @formatter:on
	}

	@Test
	public void updateUserAuthTokenStatus_otherUserToken_denied() {
		// GIVEN
		a.userActor().become();

		// THEN
		// @formatter:off
		thenExceptionOfType(AuthorizationException.class)
			.as("Disabling another user's token denied")
			.isThrownBy(() -> biz.updateUserAuthTokenStatus(a.userId(), b.userToken().tokenId(),
					SecurityTokenStatus.Disabled))
			;
		then(tokenColumn("status", b.userToken().tokenId()))
			.as("Other user's token still active")
			.isEqualTo(SecurityTokenStatus.Active.name())
			;
		// @formatter:on
	}

	@Test
	public void updateUserAuthTokenPolicy_otherUserToken_denied() {
		// GIVEN
		final String tokenId = b.restrictedUserToken().tokenId();
		final String policy = tokenColumn("jpolicy", tokenId);
		a.userActor().become();

		// THEN
		// @formatter:off
		thenExceptionOfType(AuthorizationException.class)
			.as("Removing another user's token policy denied")
			.isThrownBy(() -> biz.updateUserAuthTokenPolicy(a.userId(), tokenId,
					BasicSecurityPolicy.builder().build(), true))
			;
		then(tokenColumn("jpolicy", tokenId))
			.as("Other user's token policy unchanged")
			.isNotNull()
			.isEqualTo(policy)
			;
		// @formatter:on
	}

	@Test
	public void updateUserAuthTokenInfo_otherUserToken_denied() {
		// GIVEN
		final String tokenId = b.userToken().tokenId();
		final UserAuthToken info = new UserAuthToken(tokenId, a.userId(), "secret",
				SecurityTokenType.User);
		info.setName("Mine now");
		a.userActor().become();

		// THEN
		// @formatter:off
		thenExceptionOfType(AuthorizationException.class)
			.as("Renaming another user's token denied")
			.isThrownBy(() -> biz.updateUserAuthTokenInfo(a.userId(), tokenId, info))
			;
		then(tokenColumn("disp_name", tokenId))
			.as("Other user's token name unchanged")
			.isNull()
			;
		// @formatter:on
	}

	@Test
	public void getAllUserAuthTokens_expiringToken_allUserTokens() {
		// GIVEN
		final TestToken expiring = expiringToken();
		TestActor.token("expiring token", expiring).become();

		// WHEN
		final List<UserAuthToken> results = biz.getAllUserAuthTokens(a.userId());

		// THEN
		// @formatter:off
		then(tokenIds(results))
			.as("A policy with only an expiry is unrestricted, so all of the user's tokens returned")
			.containsExactlyInAnyOrder(a.userToken().tokenId(), a.restrictedUserToken().tokenId(),
					a.dataToken().tokenId(), expiring.tokenId())
			;
		// @formatter:on
	}

	@Test
	public void generateUserAuthToken_expiringToken_equalOrShorterExpiry() {
		// GIVEN
		final TestToken expiring = expiringToken();
		final Instant notAfter = notAfter(expiring);
		TestActor.token("expiring token", expiring).become();

		// WHEN
		final UserAuthToken equal = generate(SecurityTokenType.User, expiringPolicy(notAfter));
		final UserAuthToken shorter = generate(SecurityTokenType.ReadNodeData,
				expiringPolicy(notAfter.minus(1, HOURS)));

		// THEN
		// @formatter:off
		then(storedPolicy(equal.getId()))
			.as("Token with an equal expiry created")
			.isNotNull()
			.returns(notAfter, from(SecurityPolicy::getNotAfter))
			;
		then(storedPolicy(shorter.getId()))
			.as("Token with a shorter expiry created")
			.isNotNull()
			.returns(notAfter.minus(1, HOURS), from(SecurityPolicy::getNotAfter))
			;
		// @formatter:on
	}

	@Test
	public void generateUserAuthToken_expiringToken_noExpiry_expiryApplied() {
		// GIVEN
		final TestToken expiring = expiringToken();
		TestActor.token("expiring token", expiring).become();

		// WHEN
		final UserAuthToken result = generate(SecurityTokenType.User, emptyPolicy());

		// THEN
		// @formatter:off
		then(storedPolicy(result.getId()))
			.as("Actor's expiry applied to token requested without one")
			.isNotNull()
			.returns(notAfter(expiring), from(SecurityPolicy::getNotAfter))
			;
		// @formatter:on
	}

	@Test
	public void generateUserAuthToken_expiringToken_laterExpiry_expiryCapped() {
		// GIVEN
		final TestToken expiring = expiringToken();
		TestActor.token("expiring token", expiring).become();

		// WHEN
		final UserAuthToken result = generate(SecurityTokenType.User,
				expiringPolicy(notAfter(expiring).plus(1, DAYS)));

		// THEN
		// @formatter:off
		then(storedPolicy(result.getId()))
			.as("Requested expiry capped at the actor's expiry")
			.isNotNull()
			.returns(notAfter(expiring), from(SecurityPolicy::getNotAfter))
			;
		// @formatter:on
	}

	@Test
	public void generateUserAuthToken_expiringToken_restrictedPolicy_expiryApplied() {
		// GIVEN
		final TestToken expiring = expiringToken();
		TestActor.token("expiring token", expiring).become();

		// WHEN
		final UserAuthToken result = generate(SecurityTokenType.ReadNodeData, nodePolicy());

		// THEN
		// @formatter:off
		then(storedPolicy(result.getId()))
			.as("Requested restrictions kept")
			.isNotNull()
			.returns(Set.of(a.privateNodeId()), from(SecurityPolicy::getNodeIds))
			.as("Actor's expiry applied")
			.returns(notAfter(expiring), from(SecurityPolicy::getNotAfter))
			;
		// @formatter:on
	}

	@Test
	public void updateUserAuthTokenPolicy_expiringToken_ownExpiryKept() {
		// GIVEN
		final TestToken expiring = expiringToken();
		TestActor.token("expiring token", expiring).become();

		// WHEN
		biz.updateUserAuthTokenPolicy(a.userId(), expiring.tokenId(), nodePolicy(), false);

		// THEN
		// @formatter:off
		then(storedPolicy(expiring.tokenId()))
			.as("Policy updated")
			.isNotNull()
			.returns(Set.of(a.privateNodeId()), from(SecurityPolicy::getNodeIds))
			.as("Token expiry kept")
			.returns(notAfter(expiring), from(SecurityPolicy::getNotAfter))
			;
		// @formatter:on
	}

	@Test
	public void updateUserAuthTokenPolicy_expiringToken_ownExpiryRemoval_expiryKept() {
		// GIVEN
		final TestToken expiring = expiringToken();
		TestActor.token("expiring token", expiring).become();

		// WHEN
		biz.updateUserAuthTokenPolicy(a.userId(), expiring.tokenId(), emptyPolicy(), true);

		// THEN
		// @formatter:off
		then(storedPolicy(expiring.tokenId()))
			.as("Token expiry kept when replacing the policy with one without an expiry")
			.isNotNull()
			.returns(notAfter(expiring), from(SecurityPolicy::getNotAfter))
			;
		// @formatter:on
	}

	@Test
	public void updateUserAuthTokenPolicy_expiringToken_ownLaterExpiry_expiryCapped() {
		// GIVEN
		final TestToken expiring = expiringToken();
		TestActor.token("expiring token", expiring).become();

		// WHEN
		biz.updateUserAuthTokenPolicy(a.userId(), expiring.tokenId(),
				expiringPolicy(notAfter(expiring).plus(1, DAYS)), false);

		// THEN
		// @formatter:off
		then(storedPolicy(expiring.tokenId()))
			.as("Token expiry not extended")
			.isNotNull()
			.returns(notAfter(expiring), from(SecurityPolicy::getNotAfter))
			;
		// @formatter:on
	}

	@Test
	public void updateUserAuthTokenPolicy_expiringToken_createdTokenExpiryRemoval_expiryKept() {
		// GIVEN
		final TestToken expiring = expiringToken();
		TestActor.token("expiring token", expiring).become();
		final UserAuthToken created = generate(SecurityTokenType.User,
				expiringPolicy(notAfter(expiring)));

		// WHEN
		biz.updateUserAuthTokenPolicy(a.userId(), created.getId(), emptyPolicy(), true);

		// THEN
		// @formatter:off
		then(storedPolicy(created.getId()))
			.as("Expiry of a token created by the actor cannot be removed either")
			.isNotNull()
			.returns(notAfter(expiring), from(SecurityPolicy::getNotAfter))
			;
		// @formatter:on
	}

	@Test
	public void updateUserAuthTokenPolicy_expiringToken_otherTokenExpiryApplied() {
		// GIVEN
		final TestToken expiring = expiringToken();
		TestActor.token("expiring token", expiring).become();

		// WHEN
		biz.updateUserAuthTokenPolicy(a.userId(), a.dataToken().tokenId(), nodePolicy(), false);

		// THEN
		// @formatter:off
		then(storedPolicy(a.dataToken().tokenId()))
			.as("Policy updated")
			.isNotNull()
			.returns(Set.of(a.privateNodeId()), from(SecurityPolicy::getNodeIds))
			.as("Actor's expiry applied to a token that did not expire")
			.returns(notAfter(expiring), from(SecurityPolicy::getNotAfter))
			;
		// @formatter:on
	}

	@Test
	public void generateUserAuthToken_noRefreshToken_noPolicy_refreshDenied() {
		// GIVEN
		TestActor.token("no refresh token", noRefreshToken()).become();

		// WHEN
		final UserAuthToken result = generate(SecurityTokenType.User, emptyPolicy());

		// THEN
		// @formatter:off
		then(storedPolicy(result.getId()))
			.as("Actor's refresh constraint applied to token requested without a policy")
			.isNotNull()
			.returns(false, from(SecurityPolicy::getRefreshAllowed))
			;
		// @formatter:on
	}

	@Test
	public void generateUserAuthToken_noRefreshToken_refreshAllowed_refreshDenied() {
		// GIVEN
		TestActor.token("no refresh token", noRefreshToken()).become();

		// WHEN
		final UserAuthToken result = generate(SecurityTokenType.User, refreshPolicy(true));

		// THEN
		// @formatter:off
		then(storedPolicy(result.getId()))
			.as("Requested refresh denied")
			.isNotNull()
			.returns(false, from(SecurityPolicy::getRefreshAllowed))
			;
		// @formatter:on
	}

	@Test
	public void generateUserAuthToken_noRefreshToken_restrictedPolicy_refreshDenied() {
		// GIVEN
		TestActor.token("no refresh token", noRefreshToken()).become();

		// WHEN
		final UserAuthToken result = generate(SecurityTokenType.ReadNodeData, nodePolicy());

		// THEN
		// @formatter:off
		then(storedPolicy(result.getId()))
			.as("Requested restrictions kept")
			.isNotNull()
			.returns(Set.of(a.privateNodeId()), from(SecurityPolicy::getNodeIds))
			.as("Actor's refresh constraint applied")
			.returns(false, from(SecurityPolicy::getRefreshAllowed))
			;
		// @formatter:on
	}

	@Test
	public void updateUserAuthTokenPolicy_noRefreshToken_ownRefreshAllowed_refreshDenied() {
		// GIVEN
		final TestToken noRefresh = noRefreshToken();
		TestActor.token("no refresh token", noRefresh).become();

		// WHEN
		biz.updateUserAuthTokenPolicy(a.userId(), noRefresh.tokenId(), refreshPolicy(true), true);

		// THEN
		// @formatter:off
		then(storedPolicy(noRefresh.tokenId()))
			.as("Token refresh still denied")
			.isNotNull()
			.returns(false, from(SecurityPolicy::getRefreshAllowed))
			;
		// @formatter:on
	}

	@Test
	public void updateUserAuthTokenPolicy_noRefreshToken_createdTokenRefreshAllowed_refreshDenied() {
		// GIVEN
		TestActor.token("no refresh token", noRefreshToken()).become();
		final UserAuthToken created = generate(SecurityTokenType.ReadNodeData, nodePolicy());

		// WHEN
		biz.updateUserAuthTokenPolicy(a.userId(), created.getId(), refreshPolicy(true), false);

		// THEN
		// @formatter:off
		then(storedPolicy(created.getId()))
			.as("Refresh of a token created by the actor cannot be allowed either")
			.isNotNull()
			.returns(false, from(SecurityPolicy::getRefreshAllowed))
			;
		// @formatter:on
	}

	@Test
	public void generateUserAuthToken_expiringNoRefreshToken_bothConstraintsApplied() {
		// GIVEN
		final Instant notAfter = expiry();
		TestActor.token("expiring no refresh token", constrainedToken(BasicSecurityPolicy.builder()
				.withNotAfter(notAfter).withRefreshAllowed(false).build())).become();

		// WHEN
		final UserAuthToken result = generate(SecurityTokenType.User, refreshPolicy(true));

		// THEN
		// @formatter:off
		then(storedPolicy(result.getId()))
			.as("Actor's expiry applied")
			.isNotNull()
			.returns(notAfter, from(SecurityPolicy::getNotAfter))
			.as("Actor's refresh constraint applied")
			.returns(false, from(SecurityPolicy::getRefreshAllowed))
			;
		// @formatter:on
	}

	@Test
	public void generateUserAuthToken_unconstrainedActors_noPolicy() {
		// GIVEN
		final List<TestActor> actors = List.of(a.userActor(), a.tokenActor(),
				TestActor.token("refresh token", constrainedToken(refreshPolicy(true))));

		for ( TestActor actor : actors ) {
			actor.become();

			// WHEN
			final UserAuthToken result = generate(SecurityTokenType.User, emptyPolicy());

			// THEN
			// @formatter:off
			then(tokenColumn("jpolicy", result.getId()))
				.as("No policy saved for token created by %s", actor)
				.isNull()
				;
			// @formatter:on
		}
	}

}
