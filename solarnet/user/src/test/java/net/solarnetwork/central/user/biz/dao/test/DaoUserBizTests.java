/* ==================================================================
 * DaoUserBizTests.java - Dec 12, 2012 3:46:49 PM
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

import static net.solarnetwork.central.test.CommonTestUtils.RNG;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static org.assertj.core.api.BDDAssertions.and;
import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.BDDAssertions.thenExceptionOfType;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.cache.Cache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import net.solarnetwork.central.dao.SolarLocationDao;
import net.solarnetwork.central.dao.SolarNodeDao;
import net.solarnetwork.central.domain.Location;
import net.solarnetwork.central.domain.SolarLocation;
import net.solarnetwork.central.domain.SolarNode;
import net.solarnetwork.central.domain.UserStringCompositePK;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.security.SecurityTokenStatus;
import net.solarnetwork.central.security.SecurityTokenType;
import net.solarnetwork.central.user.biz.dao.DaoUserBiz;
import net.solarnetwork.central.user.biz.dao.UserBizConstants;
import net.solarnetwork.central.user.dao.BasicUserAuthTokenFilter;
import net.solarnetwork.central.user.dao.UserAlertDao;
import net.solarnetwork.central.user.dao.UserAuthTokenDao;
import net.solarnetwork.central.user.dao.UserAuthTokenFilter;
import net.solarnetwork.central.user.dao.UserDao;
import net.solarnetwork.central.user.dao.UserNodeDao;
import net.solarnetwork.central.user.domain.User;
import net.solarnetwork.central.user.domain.UserAuthToken;
import net.solarnetwork.central.user.domain.UserNode;
import net.solarnetwork.central.user.domain.UserNodePK;
import net.solarnetwork.central.user.domain.UserNodeTransfer;
import net.solarnetwork.dao.BasicFilterResults;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.BasicSecurityPolicy;
import net.solarnetwork.domain.LocationPrecision;
import net.solarnetwork.domain.SecurityPolicy;
import net.solarnetwork.domain.datum.Aggregation;
import net.solarnetwork.security.Snws2AuthorizationBuilder;

/**
 * Test cases for the {@link DaoUserBiz} class.
 * 
 * @author matt
 * @version 2.4
 */
@ExtendWith(MockitoExtension.class)
@SuppressWarnings("static-access")
public class DaoUserBizTests {

	private static final Long TEST_USER_ID = -1L;
	private static final String TEST_EMAIL = "test@localhost";
	private static final String TEST_ENC_PASSWORD = "encrypted.password";
	private static final String TEST_NAME = "Test User";
	private static final String TEST_ROLE = "ROLE_TEST";
	private static final String TEST_AUTH_TOKEN = "12345678901234567890";
	private static final String TEST_AUTH_SECRET = "123";
	private static final Long TEST_LOC_ID = -2L;
	private static final Long TEST_NODE_ID = -3L;
	private static final Long TEST_NODE_ID_2 = -4L;
	private static final Long TEST_USER_ID_2 = -5L;

	@Mock
	private SolarLocationDao solarLocationDao;

	@Mock
	private SolarNodeDao solarNodeDao;

	@Mock
	private UserDao userDao;

	@Mock
	private UserAuthTokenDao userAuthTokenDao;

	@Mock
	private UserNodeDao userNodeDao;

	@Mock
	private UserAlertDao userAlertDao;

	@Mock
	private Cache<UserStringCompositePK, UserAuthToken> tokenCache;

	@Captor
	private ArgumentCaptor<UserAuthToken> tokenCaptor;

	@Captor
	private ArgumentCaptor<SolarNode> nodeCaptor;

	@Captor
	private ArgumentCaptor<Location> locationCaptor;

	@Captor
	private ArgumentCaptor<UserAuthTokenFilter> userAuthTokenFilterCaptor;

	private SolarNode testNode;
	private User testUser;
	private Set<String> testUserRoles;

	private DaoUserBiz userBiz;

	@BeforeEach
	public void setup() throws Exception {
		testUser = new User();
		testUser.setEmail(TEST_EMAIL);
		testUser.setId(TEST_USER_ID);
		testUser.setName(TEST_NAME);
		testUser.setPassword(TEST_ENC_PASSWORD);

		testNode = new SolarNode();
		testNode.setId(TEST_NODE_ID);
		testNode.setLocationId(TEST_LOC_ID);

		testUserRoles = new HashSet<String>();
		testUserRoles.add(TEST_ROLE);

		userBiz = new DaoUserBiz();
		userBiz.setSolarLocationDao(solarLocationDao);
		userBiz.setSolarNodeDao(solarNodeDao);
		userBiz.setUserDao(userDao);
		userBiz.setUserAuthTokenDao(userAuthTokenDao);
		userBiz.setUserNodeDao(userNodeDao);
		userBiz.setUserAlertDao(userAlertDao);
		userBiz.setUserAuthTokenCache(tokenCache);
	}

	@Test
	public void generateUserAuthToken() {
		// GIVEN
		given(userAuthTokenDao.get(any())).willReturn(null);
		given(userAuthTokenDao.save(any())).willReturn(TEST_AUTH_TOKEN);

		// WHEN
		UserAuthToken generated = userBiz.generateUserAuthToken(TEST_USER_ID, SecurityTokenType.User,
				(SecurityPolicy) null);

		// THEN
		and.then(generated).isNotNull();
		and.then(generated.getAuthToken()).isNotNull();
		and.then(generated.getAuthToken()).as("Auth token should be exactly 20 characters").hasSize(20);
		and.then(generated.getAuthSecret()).isNotNull();
		and.then(generated.getUserId()).isEqualTo(TEST_USER_ID);
		and.then(generated.getStatus()).isEqualTo(SecurityTokenStatus.Active);
	}

	@Test
	public void updateUserAuthTokenInfo() {
		// GIVEN
		final String tokenId = UserBizConstants.generateRandomAuthToken(RNG);
		final String name = UUID.randomUUID().toString();
		final String desc = UUID.randomUUID().toString();

		final UserAuthToken entity = new UserAuthToken();
		entity.setUserId(TEST_USER_ID);
		entity.setAuthToken(tokenId);

		given(userAuthTokenDao.get(tokenId)).willReturn(entity);
		given(userAuthTokenDao.save(entity)).willReturn(tokenId);

		// WHEN
		UserAuthToken info = new UserAuthToken();
		info.setName(name);
		info.setDescription(desc);
		UserAuthToken updated = userBiz.updateUserAuthTokenInfo(TEST_USER_ID, tokenId, info);

		// THEN
		assertThat("Entity token returned", updated, is(sameInstance(entity)));
		assertThat("Name set", updated.getName(), is(equalTo(name)));
		assertThat("Description set", updated.getDescription(), is(equalTo(desc)));
	}

	@Test
	public void updateUserAuthTokenInfo_unchanged() {
		// GIVEN
		final String tokenId = UserBizConstants.generateRandomAuthToken(RNG);
		final String name = UUID.randomUUID().toString();
		final String desc = UUID.randomUUID().toString();

		final UserAuthToken entity = new UserAuthToken();
		entity.setUserId(TEST_USER_ID);
		entity.setAuthToken(tokenId);
		entity.setName(name);
		entity.setDescription(desc);

		given(userAuthTokenDao.get(tokenId)).willReturn(entity);

		// WHEN
		UserAuthToken info = new UserAuthToken();
		info.setName(name);
		info.setDescription(desc);
		UserAuthToken updated = userBiz.updateUserAuthTokenInfo(TEST_USER_ID, tokenId, info);

		// THEN
		assertThat("Entity token returned", updated, is(sameInstance(entity)));
	}

	@Test
	public void deleteUserAuthToken() {
		// GIVEN
		final UserAuthToken token = new UserAuthToken(TEST_AUTH_TOKEN, TEST_USER_ID, TEST_AUTH_SECRET,
				SecurityTokenType.User);
		given(userAuthTokenDao.get(TEST_AUTH_TOKEN)).willReturn(token);

		userAuthTokenDao.delete(same(token));

		// WHEN
		userBiz.deleteUserAuthToken(TEST_USER_ID, TEST_AUTH_TOKEN);
	}

	@Test
	public void deleteUserAuthTokenNotFound() {
		// GIVEN
		given(userAuthTokenDao.get(TEST_AUTH_TOKEN)).willReturn(null);

		userBiz.deleteUserAuthToken(TEST_USER_ID, TEST_AUTH_TOKEN);

	}

	@Test
	public void deleteUserAuthTokenWrongUser() {
		final UserAuthToken token = new UserAuthToken(TEST_AUTH_TOKEN, TEST_USER_ID, TEST_AUTH_SECRET,
				SecurityTokenType.User);
		given(userAuthTokenDao.get(TEST_AUTH_TOKEN)).willReturn(token);

		thenExceptionOfType(AuthorizationException.class)
				.isThrownBy(() -> userBiz.deleteUserAuthToken(TEST_USER_ID - 1L, TEST_AUTH_TOKEN))
				.returns(AuthorizationException.Reason.ACCESS_DENIED,
						from(AuthorizationException::getReason))
				.returns(TEST_AUTH_TOKEN, from(AuthorizationException::getId));
	}

	@Test
	public void updateSecurityTokenStatus() {
		// GIVEN
		final UserAuthToken token = new UserAuthToken(TEST_AUTH_TOKEN, TEST_USER_ID, TEST_AUTH_SECRET,
				SecurityTokenType.User);
		given(userAuthTokenDao.get(TEST_AUTH_TOKEN)).willReturn(token);

		given(userAuthTokenDao.save(any())).willReturn(TEST_AUTH_TOKEN);

		// WHEN
		UserAuthToken updated = userBiz.updateUserAuthTokenStatus(TEST_USER_ID, TEST_AUTH_TOKEN,
				SecurityTokenStatus.Disabled);

		// THEN
		then(userAuthTokenDao).should().save(tokenCaptor.capture());
		and.then(tokenCaptor.getValue()).as("Returned entity same as saved to DAO").isSameAs(updated);

		and.then(updated).as("Updated token").isNotNull();
		and.then(updated.getAuthToken()).as("Updated token ID").isEqualTo(TEST_AUTH_TOKEN);
		and.then(updated.getAuthSecret()).as("Token secret").isEqualTo(TEST_AUTH_SECRET);
		and.then(updated.getUserId()).as("Token user").isEqualTo(TEST_USER_ID);
		and.then(updated.getStatus()).as("Token state").isEqualTo(SecurityTokenStatus.Disabled);
	}

	@Test
	public void replaceUserAuthTokenPolicy() {
		// GIVEN
		final UserAuthToken token = new UserAuthToken(TEST_AUTH_TOKEN, TEST_USER_ID, TEST_AUTH_SECRET,
				SecurityTokenType.User);
		final BasicSecurityPolicy policy = new BasicSecurityPolicy.Builder()
				.withMinAggregation(Aggregation.Day).withMinLocationPrecision(LocationPrecision.Block)
				.build();
		token.setPolicy(policy);

		given(userAuthTokenDao.get(TEST_AUTH_TOKEN)).willReturn(token);

		given(userAuthTokenDao.save(any())).willReturn(TEST_AUTH_TOKEN);

		// WHEN
		final BasicSecurityPolicy newPolicy = new BasicSecurityPolicy.Builder()
				.withMinAggregation(Aggregation.Month).build();
		UserAuthToken updated = userBiz.updateUserAuthTokenPolicy(TEST_USER_ID, TEST_AUTH_TOKEN,
				newPolicy, true);

		// THEN
		then(userAuthTokenDao).should().save(tokenCaptor.capture());
		and.then(tokenCaptor.getValue()).as("Returned entity same as saved to DAO").isSameAs(updated);

		and.then(updated).as("Updated token").isNotNull();
		and.then(updated.getAuthToken()).as("Updated token ID").isEqualTo(TEST_AUTH_TOKEN);
		and.then(updated.getAuthSecret()).as("Token secret").isEqualTo(TEST_AUTH_SECRET);
		and.then(updated.getUserId()).as("Token user").isEqualTo(TEST_USER_ID);
		and.then(updated.getPolicy()).as("Token policy").isEqualTo(newPolicy);
	}

	@Test
	public void mergeUserAuthTokenPolicy() {
		// GIVEN
		final UserAuthToken token = new UserAuthToken(TEST_AUTH_TOKEN, TEST_USER_ID, TEST_AUTH_SECRET,
				SecurityTokenType.User);
		final BasicSecurityPolicy policy = new BasicSecurityPolicy.Builder()
				.withMinAggregation(Aggregation.Day).withMinLocationPrecision(LocationPrecision.Block)
				.build();
		token.setPolicy(policy);

		given(userAuthTokenDao.get(TEST_AUTH_TOKEN)).willReturn(token);

		given(userAuthTokenDao.save(any())).willReturn(TEST_AUTH_TOKEN);

		final BasicSecurityPolicy policyPatch = new BasicSecurityPolicy.Builder()
				.withMinAggregation(Aggregation.Month).build();
		UserAuthToken updated = userBiz.updateUserAuthTokenPolicy(TEST_USER_ID, TEST_AUTH_TOKEN,
				policyPatch, false);

		// THEN
		then(userAuthTokenDao).should().save(tokenCaptor.capture());
		and.then(tokenCaptor.getValue()).as("Returned entity same as saved to DAO").isSameAs(updated);

		and.then(updated).as("Updated token").isNotNull();
		and.then(updated.getAuthToken()).as("Updated token ID").isEqualTo(TEST_AUTH_TOKEN);
		and.then(updated.getAuthSecret()).as("Token secret").isEqualTo(TEST_AUTH_SECRET);
		and.then(updated.getUserId()).as("Token user").isEqualTo(TEST_USER_ID);

		BasicSecurityPolicy expectedPolicy = new BasicSecurityPolicy.Builder()
				.withMinAggregation(Aggregation.Month).withMinLocationPrecision(LocationPrecision.Block)
				.build();

		and.then(updated.getPolicy()).as("Token policy").isEqualTo(expectedPolicy);
	}

	@Test
	public void saveUserNodeNoLocationChange() {
		// GIVEN
		final UserNode userNode = new UserNode();
		userNode.setCreated(Instant.now());
		userNode.setDescription("Test user node");
		userNode.setName("Test UserNode");
		userNode.setRequiresAuthorization(true);
		userNode.setUser(testUser);
		userNode.setNode(testNode);

		SolarLocation loc = new SolarLocation();
		loc.setId(testNode.getLocationId());
		loc.setName("foo");
		loc.setTimeZoneId("UTC");
		loc.setCountry("GB");

		given(userNodeDao.get(testNode.getId())).willReturn(userNode);
		given(solarLocationDao.getSolarLocationForLocation(any(loc.getClass()))).willReturn(loc);
		given(userNodeDao.save(userNode)).willReturn(testNode.getId());

		UserNode entry = new UserNode(testUser, testNode.clone());
		entry.getNode().setLocation(loc);

		// WHEN
		UserNode result = userBiz.saveUserNode(entry);

		// THEN
		and.then(result).as("DAO result returned").isSameAs(userNode);
	}

	@Test
	public void saveUserNodeLocationChange() {
		// GIVEN
		final UserNode userNode = new UserNode();
		userNode.setCreated(Instant.now());
		userNode.setDescription("Test user node");
		userNode.setName("Test UserNode");
		userNode.setRequiresAuthorization(true);
		userNode.setUser(testUser);
		userNode.setNode(testNode);

		SolarLocation loc = new SolarLocation();
		loc.setId(testNode.getLocationId());
		loc.setName("foo");
		loc.setTimeZoneId("UTC");
		loc.setCountry("GB");

		SolarLocation locMatch = new SolarLocation();
		locMatch.setId(-9L);
		locMatch.setName("bar");

		given(userNodeDao.get(testNode.getId())).willReturn(userNode);
		given(solarLocationDao.getSolarLocationForLocation(any())).willReturn(locMatch);
		given(solarNodeDao.save(any())).willReturn(testNode.getId());
		given(userNodeDao.save(userNode)).willReturn(testNode.getId());

		// WHEN
		UserNode entry = new UserNode(testUser, testNode.clone());
		entry.getNode().setLocation(loc);

		UserNode result = userBiz.saveUserNode(entry);

		// THEN
		// @formatter:off
		then(solarLocationDao).should().getSolarLocationForLocation(locationCaptor.capture());
		and.then(locationCaptor.getValue())
			.asInstanceOf(type(SolarLocation.class))
			.as("Given node location ID not used as criteria")
			.returns(null, from(SolarLocation::getId))
			.as("Given node location name used as criteria")
			.returns(loc.getName(), from(SolarLocation::getName))
			;
		
		then(solarNodeDao).should().save(nodeCaptor.capture());
		and.then(nodeCaptor.getValue())
			.as("Persisted node ID as given")
			.returns(testNode.getId(), from(SolarNode::getId))
			.as("Persisted location ID as given")
			.returns(locMatch.getId(), SolarNode::getLocationId)
			;

		and.then(result)
			.as("Result equals given")
			.isEqualTo(userNode)
			;
		// @formatter:on
	}

	@Test
	public void saveUserNodeNewLocation() {
		// GIVEN
		final UserNode userNode = new UserNode();
		userNode.setCreated(Instant.now());
		userNode.setDescription("Test user node");
		userNode.setName("Test UserNode");
		userNode.setRequiresAuthorization(true);
		userNode.setUser(testUser);
		userNode.setNode(testNode);

		SolarLocation loc = new SolarLocation();
		loc.setId(testNode.getLocationId());
		loc.setName("foo");
		loc.setTimeZoneId("UTC");
		loc.setCountry("GB");

		SolarLocation newLoc = new SolarLocation();
		newLoc.setId(-99L);

		given(userNodeDao.get(testNode.getId())).willReturn(userNode);
		given(solarLocationDao.getSolarLocationForLocation(any())).willReturn(null);
		given(solarLocationDao.save(any())).willReturn(newLoc.getId());
		given(solarLocationDao.get(newLoc.getId())).willReturn(newLoc);
		given(solarNodeDao.save(any())).willReturn(testNode.getId());
		given(userNodeDao.save(userNode)).willReturn(testNode.getId());

		UserNode entry = new UserNode(testUser, testNode.clone());
		entry.getNode().setLocation(loc);

		// WHEN
		UserNode result = userBiz.saveUserNode(entry);

		// THEN
		// @formatter:off
		then(solarLocationDao).should().getSolarLocationForLocation(locationCaptor.capture());
		and.then(locationCaptor.getValue())
			.asInstanceOf(type(SolarLocation.class))
			.as("Given node location ID not used as criteria")
			.returns(null, from(SolarLocation::getId))
			.as("Given node location name used as criteria")
			.returns(loc.getName(), from(SolarLocation::getName))
			;
		
		then(solarNodeDao).should().save(nodeCaptor.capture());
		and.then(nodeCaptor.getValue())
			.as("Persisted node ID as given")
			.returns(testNode.getId(), from(SolarNode::getId))
			.as("Persisted location ID as given")
			.returns(newLoc.getId(), SolarNode::getLocationId)
			;

		and.then(result)
			.as("Result equals given")
			.isEqualTo(userNode)
			;
		// @formatter:on
	}

	@Test
	public void saveUserNodeLocationNoCountry() {
		// GIVEN
		final UserNode userNode = new UserNode();
		userNode.setCreated(Instant.now());
		userNode.setDescription("Test user node");
		userNode.setName("Test UserNode");
		userNode.setRequiresAuthorization(true);
		userNode.setUser(testUser);
		userNode.setNode(testNode);

		SolarLocation loc = new SolarLocation();
		loc.setId(testNode.getLocationId());
		loc.setName("foo");

		given(userNodeDao.get(testNode.getId())).willReturn(userNode);
		given(userNodeDao.save(userNode)).willReturn(testNode.getId());

		// WHEN
		UserNode entry = new UserNode(testUser, testNode.clone());
		entry.getNode().setLocation(loc);

		UserNode result = userBiz.saveUserNode(entry);

		// THEN
		// @formatter:off
		then(solarLocationDao).shouldHaveNoInteractions();
		
		then(solarNodeDao).shouldHaveNoInteractions();

		and.then(result)
			.as("Result equals given")
			.isEqualTo(userNode)
			;
		// @formatter:on
	}

	@Test
	public void confirmTransferWithAuthTokenMatchingNodeId() {
		// GIVEN
		// lookup required user/node data
		UserNodeTransfer userNodeXfer = new UserNodeTransfer(TEST_USER_ID, TEST_NODE_ID,
				"recipient@localhost");
		UserNodePK userNodePk = new UserNodePK(TEST_USER_ID, TEST_NODE_ID);
		given(userNodeDao.getUserNodeTransfer(userNodePk)).willReturn(userNodeXfer);

		UserNode userNode = new UserNode(testUser, testNode);
		given(userNodeDao.get(TEST_NODE_ID)).willReturn(userNode);

		User recipient = new User(TEST_USER_ID_2, userNodeXfer.getEmail());
		given(userDao.getUserByEmail(userNodeXfer.getEmail())).willReturn(recipient);

		// delete the xfer
		userNodeDao.deleteUserNodeTransfer(userNodeXfer);

		// delete alerts associated with node
		given(userAlertDao.deleteAllAlertsForNode(TEST_USER_ID, TEST_NODE_ID)).willReturn(0);

		// find auth token that has multiple node IDs in policy
		UserAuthToken userAuthToken = new UserAuthToken("abc123", TEST_USER_ID, "secret",
				SecurityTokenType.ReadNodeData);
		BasicSecurityPolicy policy = new BasicSecurityPolicy.Builder()
				.withNodeIds(new LinkedHashSet<Long>(Arrays.asList(TEST_NODE_ID))).build();
		userAuthToken.setPolicy(policy);
		given(userAuthTokenDao.findUserAuthTokensForUser(TEST_USER_ID))
				.willReturn(Arrays.asList(userAuthToken));

		// then delete the token
		userAuthTokenDao.delete(same(userAuthToken));

		// then store the updated UserNode
		given(userNodeDao.save(userNode)).willReturn(TEST_NODE_ID);

		// WHEN
		UserNodeTransfer xfer = userBiz.confirmNodeOwnershipTransfer(TEST_USER_ID, TEST_NODE_ID, true);

		// THEN
		assertThat(xfer, sameInstance(userNodeXfer));
		assertThat("UserNode now owned by recipient", userNode.getUser(), sameInstance(recipient));
	}

	@Test
	public void confirmTransferWithAuthTokenContainingOtherNodeId() {
		// GIVEN
		// lookup required user/node data
		UserNodeTransfer userNodeXfer = new UserNodeTransfer(TEST_USER_ID, TEST_NODE_ID,
				"recipient@localhost");
		UserNodePK userNodePk = new UserNodePK(TEST_USER_ID, TEST_NODE_ID);
		given(userNodeDao.getUserNodeTransfer(userNodePk)).willReturn(userNodeXfer);

		UserNode userNode = new UserNode(testUser, testNode);
		given(userNodeDao.get(TEST_NODE_ID)).willReturn(userNode);

		User recipient = new User(TEST_USER_ID_2, userNodeXfer.getEmail());
		given(userDao.getUserByEmail(userNodeXfer.getEmail())).willReturn(recipient);

		// delete the xfer
		userNodeDao.deleteUserNodeTransfer(userNodeXfer);

		// delete alerts associated with node
		given(userAlertDao.deleteAllAlertsForNode(TEST_USER_ID, TEST_NODE_ID)).willReturn(0);

		// find auth token that has multiple node IDs in policy
		UserAuthToken userAuthToken = new UserAuthToken("abc123", TEST_USER_ID, "secret",
				SecurityTokenType.ReadNodeData);
		BasicSecurityPolicy policy = new BasicSecurityPolicy.Builder()
				.withNodeIds(new LinkedHashSet<Long>(Arrays.asList(TEST_NODE_ID, TEST_NODE_ID_2)))
				.build();
		userAuthToken.setPolicy(policy);
		given(userAuthTokenDao.findUserAuthTokensForUser(TEST_USER_ID))
				.willReturn(Arrays.asList(userAuthToken));

		// then store the updated token
		given(userAuthTokenDao.save(same(userAuthToken))).willReturn("abc123");

		// then store the updated UserNode
		given(userNodeDao.save(userNode)).willReturn(TEST_NODE_ID);

		// WHEN
		UserNodeTransfer xfer = userBiz.confirmNodeOwnershipTransfer(TEST_USER_ID, TEST_NODE_ID, true);

		// THEN
		assertThat(xfer, sameInstance(userNodeXfer));
		assertThat("UserNode now owned by recipient", userNode.getUser(), sameInstance(recipient));
		assertThat("Auth token no longer contains transferred node", userAuthToken.getNodeIds(),
				hasItems(TEST_NODE_ID_2));
	}

	@Test
	public void createAuthBuilder() {
		// GIVEN
		UserAuthToken token = new UserAuthToken(TEST_AUTH_TOKEN, TEST_USER_ID, TEST_AUTH_SECRET,
				SecurityTokenType.User);
		given(userAuthTokenDao.get(TEST_AUTH_TOKEN)).willReturn(token);

		Instant signingDate = LocalDateTime.of(2017, 1, 1, 0, 0).toInstant(ZoneOffset.UTC);
		Snws2AuthorizationBuilder builder = new Snws2AuthorizationBuilder(TEST_AUTH_TOKEN);
		given(userAuthTokenDao.createSnws2AuthorizationBuilder(TEST_AUTH_TOKEN, signingDate))
				.willReturn(builder);

		// WHEN
		Snws2AuthorizationBuilder result = userBiz.createSnws2AuthorizationBuilder(TEST_USER_ID,
				TEST_AUTH_TOKEN, signingDate);

		// THEN
		assertThat("Builder", result, sameInstance(builder));
	}

	@Test
	public void listTokensForUser_noCache() {
		// GIVEN
		userBiz.setUserAuthTokenCache(null);

		final UserAuthToken token = new UserAuthToken(randomString(), TEST_USER_ID, randomString(),
				SecurityTokenType.User);

		var daoResult = new BasicFilterResults<>(List.of(token));
		given(userAuthTokenDao.findFiltered(any())).willReturn(daoResult);

		// WHEN
		BasicUserAuthTokenFilter filter = new BasicUserAuthTokenFilter();
		filter.setActive(true);

		FilterResults<UserAuthToken, String> result = userBiz.listUserAuthTokensForUser(TEST_USER_ID,
				filter);

		// THEN
		// @formatter:off
		then(tokenCache).shouldHaveNoInteractions();
		
		then(userAuthTokenDao).should().findFiltered(userAuthTokenFilterCaptor.capture());
		and.then(userAuthTokenFilterCaptor.getValue())
			.as("Filter passed to DAO not same as given")
			.isNotSameAs(filter)
			.as("User ID added to DAO filter")
			.returns(new Long[] {TEST_USER_ID}, from(UserAuthTokenFilter::getUserIds))
			.as("Active included in DAO filter")
			.returns(filter.getActive(), from(UserAuthTokenFilter::getActive))
			;
		
		and.then(result)
			.as("Result from DAO returned")
			.isSameAs(daoResult)
			;
		// @formatter:on
	}

	@Test
	public void listTokensForUser_one_cacheMiss() {
		// GIVEN
		final String tokenId = randomString();
		final UserAuthToken token = new UserAuthToken(tokenId, TEST_USER_ID, randomString(),
				SecurityTokenType.User);

		var daoResult = new BasicFilterResults<>(List.of(token));
		given(userAuthTokenDao.findFiltered(any())).willReturn(daoResult);

		// WHEN
		BasicUserAuthTokenFilter filter = new BasicUserAuthTokenFilter();
		filter.setIdentifier(tokenId);

		FilterResults<UserAuthToken, String> result = userBiz.listUserAuthTokensForUser(TEST_USER_ID,
				filter);

		// THEN
		final var cacheKey = new UserStringCompositePK(TEST_USER_ID, tokenId);
		// @formatter:off
		then(tokenCache).should().get(cacheKey);
		
		then(userAuthTokenDao).should().findFiltered(userAuthTokenFilterCaptor.capture());
		and.then(userAuthTokenFilterCaptor.getValue())
			.as("Filter passed to DAO not same as given")
			.isNotSameAs(filter)
			.as("User ID added to DAO filter")
			.returns(new Long[] {TEST_USER_ID}, from(UserAuthTokenFilter::getUserIds))
			.as("Identifier included in DAO filter")
			.returns(filter.getIdentifiers(), from(UserAuthTokenFilter::getIdentifiers))
			;
		
		then(tokenCache).should().put(eq(cacheKey), tokenCaptor.capture());
		and.then(tokenCaptor.getValue())
			.as("Should cache the token returned from DAO")
			.isSameAs(token)
			;
		
		and.then(result)
			.as("Result from DAO returned")
			.isSameAs(daoResult)
			;
		// @formatter:on
	}

	@Test
	public void listTokensForUser_one_cacheHit() {
		// GIVEN
		final String tokenId = randomString();
		final UserAuthToken cacheResult = new UserAuthToken(tokenId, TEST_USER_ID, randomString(),
				SecurityTokenType.User);

		final var cacheKey = new UserStringCompositePK(TEST_USER_ID, tokenId);
		given(tokenCache.get(cacheKey)).willReturn(cacheResult);

		// WHEN
		BasicUserAuthTokenFilter filter = new BasicUserAuthTokenFilter();
		filter.setIdentifier(tokenId);

		FilterResults<UserAuthToken, String> result = userBiz.listUserAuthTokensForUser(TEST_USER_ID,
				filter);

		// THEN
		// @formatter:off
		then(userAuthTokenDao).shouldHaveNoInteractions();
		
		then(tokenCache).shouldHaveNoMoreInteractions();
		
		and.then(result)
			.as("Single result returned for cache hit")
			.hasSameElementsAs(List.of(cacheResult))
			;
		// @formatter:on
	}

}
