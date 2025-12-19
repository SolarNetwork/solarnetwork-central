/* ==================================================================
 * SecurityUtilsTests.java - Oct 20, 2014 9:18:50 AM
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

package net.solarnetwork.central.security.test;

import static net.solarnetwork.central.domain.BasicSolarNodeOwnership.ownershipFor;
import static net.solarnetwork.central.security.SecurityUtils.becomeNode;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static org.assertj.core.api.BDDAssertions.and;
import static org.assertj.core.api.BDDAssertions.catchThrowableOfType;
import static org.assertj.core.api.BDDAssertions.from;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.is;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;
import net.solarnetwork.central.domain.SolarNodeOwnership;
import net.solarnetwork.central.security.AuthenticatedToken;
import net.solarnetwork.central.security.AuthenticatedUser;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.security.SecurityTokenType;
import net.solarnetwork.central.security.SecurityUtils;
import net.solarnetwork.domain.BasicSecurityPolicy;
import net.solarnetwork.domain.LocationPrecision;
import net.solarnetwork.domain.SecurityPolicy;
import net.solarnetwork.domain.datum.Aggregation;

/**
 * Test cases for the {@link SecurityUtils} class.
 * 
 * @author matt
 * @version 2.4
 */
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class SecurityUtilsTests {

	private static final Long TEST_USER_ID = -1L;

	@Mock
	private SolarNodeOwnershipDao nodeOwnershipDao;

	@Mock
	private TextEncryptor textEncryptor;

	@Mock
	private Function<String, String> fn;

	private void becomeUser(String... roles) {
		User userDetails = new User("test@localhost", "foobar", AuthorityUtils.NO_AUTHORITIES);
		AuthenticatedUser user = new AuthenticatedUser(userDetails, TEST_USER_ID, "Test User", false);
		TestingAuthenticationToken auth = new TestingAuthenticationToken(user, "foobar", roles);
		SecurityContextHolder.getContext().setAuthentication(auth);
	}

	private void becomeToken(SecurityTokenType type, Set<Long> nodeIds, String... roles) {
		User userDetails = new User("abc123", "def456", AuthorityUtils.NO_AUTHORITIES);
		SecurityPolicy policy = null;
		if ( nodeIds != null ) {
			policy = BasicSecurityPolicy.builder().withNodeIds(nodeIds).build();
		}
		AuthenticatedToken token = new AuthenticatedToken(userDetails, type, TEST_USER_ID, policy);
		TestingAuthenticationToken auth = new TestingAuthenticationToken(token, "foobar", roles);
		SecurityContextHolder.getContext().setAuthentication(auth);
	}

	@AfterEach
	public void cleanup() {
		SecurityContextHolder.getContext().setAuthentication(null);
	}

	@Test
	public void requireAnyRoleNoAuthentication() {
		Assertions.assertThrows(AuthorizationException.class, () -> {
			SecurityUtils.requireAnyRole(Collections.singleton("ROLE_USER"));
		});
	}

	@Test
	public void requireAnyRoleNoMatchSingle() {
		becomeUser("ROLE_USER");
		Assertions.assertThrows(AuthorizationException.class, () -> {
			SecurityUtils.requireAnyRole(Collections.singleton("ROLE_FOO"));
		});
	}

	@Test
	public void requireAnyRoleNoMatchMultiple() {
		becomeUser("ROLE_USER", "ROLE_DUDE");
		Assertions.assertThrows(AuthorizationException.class, () -> {
			SecurityUtils.requireAnyRole(Collections.singleton("ROLE_FOO"));
		});
	}

	@Test
	public void requireAnyRoleMatchSingle() {
		becomeUser("ROLE_USER");
		SecurityUtils.requireAnyRole(Collections.singleton("ROLE_USER"));
	}

	public void requireAnyRoleMatchMulitple() {
		becomeUser("ROLE_USER", "ROLE_DUDE");
		SecurityUtils.requireAnyRole(new HashSet<String>(Arrays.asList("ROLE_FOO", "ROLE_DUDE")));
	}

	@Test
	public void requireAllRoleNoAuthentication() {
		Assertions.assertThrows(AuthorizationException.class, () -> {
			SecurityUtils.requireAllRoles(Collections.singleton("ROLE_USER"));
		});
	}

	@Test
	public void requireAllRoleNoMatchSingle() {
		becomeUser("ROLE_USER");
		Assertions.assertThrows(AuthorizationException.class, () -> {
			SecurityUtils.requireAllRoles(Collections.singleton("ROLE_FOO"));
		});
	}

	@Test
	public void requireAllRoleNoMatchMultiple() {
		becomeUser("ROLE_USER", "ROLE_DUDE");
		Assertions.assertThrows(AuthorizationException.class, () -> {
			SecurityUtils.requireAllRoles(new HashSet<String>(Arrays.asList("ROLE_USER", "ROLE_FOO")));
		});
	}

	@Test
	public void requireAllRoleMatchSingle() {
		becomeUser("ROLE_USER");
		SecurityUtils.requireAllRoles(Collections.singleton("ROLE_USER"));
	}

	@Test
	public void requireAllRoleMatchMultiple() {
		becomeUser("ROLE_USER", "ROLE_DUDE", "ROLE_DUDETTE");
		SecurityUtils.requireAllRoles(new HashSet<String>(Arrays.asList("ROLE_USER", "ROLE_DUDE")));
	}

	@Test
	public void authorizedNodeIds_node() {
		// GIVEN
		final Long nodeId = 123L;
		becomeNode(nodeId);

		// WHEN
		Long[] nodeIds = SecurityUtils.authorizedNodeIdsForCurrentActor(nodeOwnershipDao);

		// THEN
		assertThat("Node actor gets own node ID without checking DAO", nodeIds,
				is(arrayContaining(nodeId)));
	}

	@Test
	public void authorizedNodeIds_user() {
		List<SolarNodeOwnership> ownerships = Arrays.asList(ownershipFor(123L, TEST_USER_ID),
				ownershipFor(234L, TEST_USER_ID));
		becomeUser("ROLE_USER");
		given(nodeOwnershipDao.ownershipsForUserId(TEST_USER_ID))
				.willReturn(ownerships.toArray(SolarNodeOwnership[]::new));

		// WHEN
		Long[] nodeIds = SecurityUtils.authorizedNodeIdsForCurrentActor(nodeOwnershipDao);

		// THEN
		Long[] expected = ownerships.stream().map(SolarNodeOwnership::getNodeId).toArray(Long[]::new);
		assertThat("User actor gets node IDs returned from DAO", nodeIds, is(arrayContaining(expected)));

	}

	@Test
	public void authorizedNodeIds_token_user() {
		List<SolarNodeOwnership> ownerships = Arrays.asList(ownershipFor(123L, TEST_USER_ID),
				ownershipFor(234L, TEST_USER_ID));
		becomeToken(SecurityTokenType.User, null, "ROLE_USER");
		given(nodeOwnershipDao.ownershipsForUserId(TEST_USER_ID))
				.willReturn(ownerships.toArray(SolarNodeOwnership[]::new));

		// WHEN
		Long[] nodeIds = SecurityUtils.authorizedNodeIdsForCurrentActor(nodeOwnershipDao);

		// THEN
		Long[] expected = ownerships.stream().map(SolarNodeOwnership::getNodeId).toArray(Long[]::new);
		assertThat("User token actor without policy gets node IDs returned from DAO", nodeIds,
				is(arrayContaining(expected)));
	}

	@Test
	public void authorizedNodeIds_token_user_policy() {
		List<SolarNodeOwnership> ownerships = Arrays.asList(ownershipFor(123L, TEST_USER_ID),
				ownershipFor(234L, TEST_USER_ID));
		becomeToken(SecurityTokenType.User, Collections.singleton(ownerships.get(0).getNodeId()),
				"ROLE_USER");
		given(nodeOwnershipDao.ownershipsForUserId(TEST_USER_ID))
				.willReturn(ownerships.toArray(SolarNodeOwnership[]::new));

		// WHEN
		Long[] nodeIds = SecurityUtils.authorizedNodeIdsForCurrentActor(nodeOwnershipDao);

		// THEN
		Long[] expected = new Long[] { ownerships.get(0).getNodeId() };
		assertThat("User token actor with policy gets node IDs returned from policy, restricted by DAO",
				nodeIds, is(arrayContaining(expected)));
	}

	@Test
	public void encrpytedMap() {
		// GIVEN
		var data = new LinkedHashMap<String, String>(8);
		data.put("foo", "bar");
		data.put("bim", "bam");

		var secureKeys = Set.of("foo");

		var encryptedValue = "foo-encrypted";
		given(textEncryptor.encrypt(data.get("foo"))).willReturn(encryptedValue);

		// WHEN
		var result = SecurityUtils.encryptedMap(data, secureKeys, textEncryptor);

		// THEN
		then(textEncryptor).shouldHaveNoMoreInteractions();

		// @formatter:off
		and.then(result)
			.as("Result map provided")
			.isNotNull()
			.as("New map instance returned")
			.isNotSameAs(data)
			.as("Has same keys as input map")
			.containsOnlyKeys(data.keySet())
			.as("Non-secure value left alone")
			.containsEntry("bim", data.get("bim"))
			.as("Secure value encrypted")
			.containsEntry("foo", encryptedValue)
			;
		// @formatter:on
	}

	@Test
	public void encryptedMap_noSecureKeys() {
		// GIVEN
		var data = new LinkedHashMap<String, String>(8);
		data.put("foo", "bar");
		data.put("bim", "bam");

		var secureKeys = Set.of("other");

		// WHEN
		var result = SecurityUtils.encryptedMap(data, secureKeys, textEncryptor);

		// THEN
		then(textEncryptor).shouldHaveNoInteractions();

		// @formatter:off
		and.then(result)
			.as("Result map is same instance as provided")
			.isSameAs(data)
			;
		// @formatter:on
	}

	@Test
	public void encrpytedMap_exception() {
		// GIVEN
		var data = new LinkedHashMap<String, String>(8);
		data.put("foo", "bar");
		data.put("bim", "bam");

		var secureKeys = Set.of("foo");

		var ex = new RuntimeException();
		given(textEncryptor.encrypt(data.get("foo"))).willThrow(ex);

		// WHEN
		var result = SecurityUtils.encryptedMap(data, secureKeys, textEncryptor);

		// THEN
		then(textEncryptor).shouldHaveNoMoreInteractions();

		// @formatter:off
		and.then(result)
			.as("Result map provided")
			.isNotNull()
			.as("New map instance returned")
			.isNotSameAs(data)
			.as("Has same keys as input map")
			.containsOnlyKeys(data.keySet())
			.as("Non-secure value left alone")
			.containsEntry("bim", data.get("bim"))
			.as("Secure value that failed to encrypt returned as-is")
			.containsEntry("foo", data.get("foo"))
			;
		// @formatter:on
	}

	@Test
	public void decrpytedMap() {
		// GIVEN
		var data = new LinkedHashMap<String, String>(8);
		data.put("foo", "bar");
		data.put("bim", "bam");

		var secureKeys = Set.of("foo");

		var decryptedValue = "foo-decrypted";
		given(textEncryptor.decrypt(data.get("foo"))).willReturn(decryptedValue);

		// WHEN
		var result = SecurityUtils.decryptedMap(data, secureKeys, textEncryptor);

		// THEN
		then(textEncryptor).shouldHaveNoMoreInteractions();

		// @formatter:off
		and.then(result)
			.as("Result map provided")
			.isNotNull()
			.as("New map instance returned")
			.isNotSameAs(data)
			.as("Has same keys as input map")
			.containsOnlyKeys(data.keySet())
			.as("Non-secure value left alone")
			.containsEntry("bim", data.get("bim"))
			.as("Secure value decrypted")
			.containsEntry("foo", decryptedValue)
			;
		// @formatter:on
	}

	@Test
	public void decryptedMap_noSecureKeys() {
		// GIVEN
		var data = new LinkedHashMap<String, String>(8);
		data.put("foo", "bar");
		data.put("bim", "bam");

		var secureKeys = Set.of("other");

		// WHEN
		var result = SecurityUtils.decryptedMap(data, secureKeys, textEncryptor);

		// THEN
		then(textEncryptor).shouldHaveNoInteractions();

		// @formatter:off
		and.then(result)
			.as("Result map is same instance as provided")
			.isSameAs(data)
			;
		// @formatter:on
	}

	@Test
	public void decrpytedMap_exception() {
		// GIVEN
		var data = new LinkedHashMap<String, String>(8);
		data.put("foo", "bar");
		data.put("bim", "bam");

		var secureKeys = Set.of("foo");

		var ex = new RuntimeException();
		given(textEncryptor.decrypt(data.get("foo"))).willThrow(ex);

		// WHEN
		var result = SecurityUtils.decryptedMap(data, secureKeys, textEncryptor);

		// THEN
		then(textEncryptor).shouldHaveNoMoreInteractions();

		// @formatter:off
		and.then(result)
			.as("Result map provided")
			.isNotNull()
			.as("New map instance returned")
			.isNotSameAs(data)
			.as("Has same keys as input map")
			.containsOnlyKeys(data.keySet())
			.as("Non-secure value left alone")
			.containsEntry("bim", data.get("bim"))
			.as("Secure value that failed to decrypt returned as-is")
			.containsEntry("foo", data.get("foo"))
			;
		// @formatter:on
	}

	@Test
	public void encrpytedMap_function() {
		// GIVEN
		var data = new LinkedHashMap<String, String>(8);
		data.put("foo", "bar");
		data.put("bim", "bam");

		var secureKeys = Set.of("foo");

		var encryptedValue = "foo-encrypted";
		given(fn.apply(data.get("foo"))).willReturn(encryptedValue);

		// WHEN
		var result = SecurityUtils.encryptedMap(data, secureKeys, fn);

		// THEN
		then(textEncryptor).shouldHaveNoMoreInteractions();

		// @formatter:off
		and.then(result)
			.as("Result map provided")
			.isNotNull()
			.as("New map instance returned")
			.isNotSameAs(data)
			.as("Has same keys as input map")
			.containsOnlyKeys(data.keySet())
			.as("Non-secure value left alone")
			.containsEntry("bim", data.get("bim"))
			.as("Secure value encrypted")
			.containsEntry("foo", encryptedValue)
			;
		// @formatter:on
	}

	@Test
	public void decryptedMap_function() {
		// GIVEN
		var data = new LinkedHashMap<String, String>(8);
		data.put("foo", "bar");
		data.put("bim", "bam");

		var secureKeys = Set.of("foo");

		var decryptedValue = "foo-decrypted";
		given(fn.apply(data.get("foo"))).willReturn(decryptedValue);

		// WHEN
		var result = SecurityUtils.decryptedMap(data, secureKeys, fn);

		// THEN
		then(textEncryptor).shouldHaveNoMoreInteractions();

		// @formatter:off
		and.then(result)
			.as("Result map provided")
			.isNotNull()
			.as("New map instance returned")
			.isNotSameAs(data)
			.as("Has same keys as input map")
			.containsOnlyKeys(data.keySet())
			.as("Non-secure value left alone")
			.containsEntry("bim", data.get("bim"))
			.as("Secure value decrypted")
			.containsEntry("foo", decryptedValue)
			;
		// @formatter:on
	}

	@Test
	public void restrictNodeIds_nullNodeIds_nullPolicy() {
		// WHEN
		Long[] result = SecurityUtils.restrictNodeIds(null, null);

		// THEN
		and.then(result).as("Input returned for empty policy").isNull();
	}

	@Test
	public void restrictNodeIds_emptyNodeIds_emptyPolicy() {
		// WHEN
		Long[] result = SecurityUtils.restrictNodeIds(new Long[0],
				BasicSecurityPolicy.builder().build());

		// THEN
		and.then(result).as("Input returned for empty policy").isEmpty();
	}

	@Test
	public void restrictNodeIds_nodeIds_noPolicy() {
		// GIVEN
		final Long[] origNodeIds = new Long[] { randomLong(), randomLong() };

		// WHEN
		final Long[] nodeIds = Arrays.copyOf(origNodeIds, origNodeIds.length);
		Long[] result = SecurityUtils.restrictNodeIds(nodeIds, null);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Input returned for empty policy")
			.isSameAs(nodeIds)
			.as("Input unchanged")
			.containsExactly(origNodeIds)
			;
		// @formatter:on
	}

	@Test
	public void restrictNodeIds_nodeIds_supersetPolicy() {
		// GIVEN
		final Long[] origNodeIds = new Long[] { randomLong(), randomLong() };

		// create policy node IDs superset
		final Long[] policyIds = Arrays.copyOf(origNodeIds, origNodeIds.length + 1);
		policyIds[origNodeIds.length] = randomLong();

		final SecurityPolicy policy = BasicSecurityPolicy.builder()
				.withNodeIds(new LinkedHashSet<>(Arrays.asList(policyIds))).build();

		// WHEN
		final Long[] nodeIds = Arrays.copyOf(origNodeIds, origNodeIds.length);
		Long[] result = SecurityUtils.restrictNodeIds(nodeIds, policy);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Input returned for for subset of policy")
			.isSameAs(nodeIds)
			.as("Input unchanged")
			.containsExactly(origNodeIds)
			;
		// @formatter:on
	}

	@Test
	public void restrictNodeIds_nodeIds_subsetPolicy() {
		// GIVEN
		final Long[] origNodeIds = new Long[] { randomLong(), randomLong(), randomLong() };

		// create policy node IDs subset
		final Long[] policyIds = Arrays.copyOf(origNodeIds, origNodeIds.length - 1);

		final SecurityPolicy policy = BasicSecurityPolicy.builder()
				.withNodeIds(new LinkedHashSet<>(Arrays.asList(policyIds))).build();

		// WHEN
		final Long[] nodeIds = Arrays.copyOf(origNodeIds, origNodeIds.length);
		Long[] result = SecurityUtils.restrictNodeIds(nodeIds, policy);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Input restricted to intersection with policy")
			.containsExactly(origNodeIds[0], origNodeIds[1])
			;
		// @formatter:on
	}

	@Test
	public void restrictNodeIds_nodeIds_disjointPolicy() {
		// GIVEN
		final Long[] origNodeIds = new Long[] { 1L, 2L, 3L };

		// create policy node IDs subset
		final Long[] policyIds = new Long[] { 4L, 5L, 6L };

		final SecurityPolicy policy = BasicSecurityPolicy.builder()
				.withNodeIds(new LinkedHashSet<>(Arrays.asList(policyIds))).build();

		// WHEN
		final Long[] nodeIds = Arrays.copyOf(origNodeIds, origNodeIds.length);
		final AuthorizationException ex = catchThrowableOfType(AuthorizationException.class, () -> {
			SecurityUtils.restrictNodeIds(nodeIds, policy);
		});

		// THEN
		// @formatter:off
		and.then(ex)
			.as("Reason is deined")
			.returns(AuthorizationException.Reason.ACCESS_DENIED, from(AuthorizationException::getReason))
			.as("Input node IDs is exception ID")
			.returns(nodeIds, from(AuthorizationException::getId))
			;
		// @formatter:on
	}

	@Test
	public void restrictNodeIds_nodeIds_disjointPolicy_singleNodeId() {
		// GIVEN
		final Long[] origNodeIds = new Long[] { 1L };

		// create policy node IDs subset
		final Long[] policyIds = new Long[] { 4L, 5L, 6L };

		final SecurityPolicy policy = BasicSecurityPolicy.builder()
				.withNodeIds(new LinkedHashSet<>(Arrays.asList(policyIds))).build();

		// WHEN
		final Long[] nodeIds = Arrays.copyOf(origNodeIds, origNodeIds.length);
		final AuthorizationException ex = catchThrowableOfType(AuthorizationException.class, () -> {
			SecurityUtils.restrictNodeIds(nodeIds, policy);
		});

		// THEN
		// @formatter:off
		and.then(ex)
			.as("Reason is deined")
			.returns(AuthorizationException.Reason.ACCESS_DENIED, from(AuthorizationException::getReason))
			.as("Input node ID is exception ID")
			.returns(nodeIds[0], from(AuthorizationException::getId))
			;
		// @formatter:on
	}

	@Test
	public void policyIsUnrestricted_null() {
		// GIVEN
		final SecurityPolicy policy = null;

		// THEN
		and.then(SecurityUtils.policyIsUnrestricted(policy)).as("Null policy is treated as unrestricted")
				.isTrue();
	}

	@Test
	public void policyIsUnrestricted_empty() {
		// GIVEN
		final SecurityPolicy policy = BasicSecurityPolicy.builder().build();

		// THEN
		and.then(SecurityUtils.policyIsUnrestricted(policy))
				.as("Empty policy is treated as unrestricted").isTrue();
	}

	@Test
	public void policyIsUnrestricted_aggregations() {
		// GIVEN
		final SecurityPolicy policy = BasicSecurityPolicy.builder()
				.withAggregations(Set.of(Aggregation.Hour)).build();

		// THEN
		and.then(SecurityUtils.policyIsUnrestricted(policy)).as("Policy with aggregations is restricted")
				.isFalse();
	}

	@Test
	public void policyIsUnrestricted_apiPaths() {
		// GIVEN
		final SecurityPolicy policy = BasicSecurityPolicy.builder().withApiPaths(Set.of("foo")).build();

		// THEN
		and.then(SecurityUtils.policyIsUnrestricted(policy)).as("Policy with API paths is restricted")
				.isFalse();
	}

	@Test
	public void policyIsUnrestricted_locationPrecisions() {
		// GIVEN
		final SecurityPolicy policy = BasicSecurityPolicy.builder()
				.withLocationPrecisions(Set.of(LocationPrecision.Country)).build();

		// THEN
		and.then(SecurityUtils.policyIsUnrestricted(policy))
				.as("Policy with location precision is restricted").isFalse();
	}

	@Test
	public void policyIsUnrestricted_minAggregation() {
		// GIVEN
		final SecurityPolicy policy = BasicSecurityPolicy.builder().withMinAggregation(Aggregation.Hour)
				.build();

		// THEN
		and.then(SecurityUtils.policyIsUnrestricted(policy))
				.as("Policy with min aggregation is restricted").isFalse();
	}

	@Test
	public void policyIsUnrestricted_minLocationPrecision() {
		// GIVEN
		final SecurityPolicy policy = BasicSecurityPolicy.builder()
				.withMinLocationPrecision(LocationPrecision.Country).build();

		// THEN
		and.then(SecurityUtils.policyIsUnrestricted(policy))
				.as("Policy with min location precision is restricted").isFalse();
	}

	@Test
	public void policyIsUnrestricted_nodeIds() {
		// GIVEN
		final SecurityPolicy policy = BasicSecurityPolicy.builder().withNodeIds(Set.of(randomLong()))
				.build();

		// THEN
		and.then(SecurityUtils.policyIsUnrestricted(policy)).as("Policy with node IDs is restricted")
				.isFalse();
	}

	@Test
	public void policyIsUnrestricted_nodeMetadataPaths() {
		// GIVEN
		final SecurityPolicy policy = BasicSecurityPolicy.builder().withNodeMetadataPaths(Set.of("foo"))
				.build();

		// THEN
		and.then(SecurityUtils.policyIsUnrestricted(policy))
				.as("Policy with node metadata paths is restricted").isFalse();
	}

	@Test
	public void policyIsUnrestricted_notAFter() {
		// GIVEN
		final SecurityPolicy policy = BasicSecurityPolicy.builder().withNotAfter(Instant.now()).build();

		// THEN
		and.then(SecurityUtils.policyIsUnrestricted(policy)).as("Policy with notAfter is unrestricted")
				.isTrue();
	}

	@Test
	public void policyIsUnrestricted_refreshAllowed() {
		// GIVEN
		final SecurityPolicy policy = BasicSecurityPolicy.builder().withRefreshAllowed(false).build();

		// THEN
		and.then(SecurityUtils.policyIsUnrestricted(policy))
				.as("Policy with refresh allowed is restricted").isFalse();
	}

	@Test
	public void policyIsUnrestricted_sourceIds() {
		// GIVEN
		final SecurityPolicy policy = BasicSecurityPolicy.builder().withSourceIds(Set.of("foo")).build();

		// THEN
		and.then(SecurityUtils.policyIsUnrestricted(policy)).as("Policy with source IDs is restricted")
				.isFalse();
	}

	@Test
	public void policyIsUnrestricted_userMetadataPaths() {
		// GIVEN
		final SecurityPolicy policy = BasicSecurityPolicy.builder().withUserMetadataPaths(Set.of("foo"))
				.build();

		// THEN
		and.then(SecurityUtils.policyIsUnrestricted(policy))
				.as("Policy with user metadata paths is restricted").isFalse();
	}

}
