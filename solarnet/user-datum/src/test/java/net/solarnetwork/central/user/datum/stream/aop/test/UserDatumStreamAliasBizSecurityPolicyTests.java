/* ==================================================================
 * UserDatumStreamAliasBizSecurityPolicyTests.java - 22/09/2026 5:38:12 pm
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

package net.solarnetwork.central.user.datum.stream.aop.test;

import static java.util.stream.Collectors.toSet;
import static net.solarnetwork.central.test.aop.SecuredProxies.securedProxy;
import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenExceptionOfType;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import net.solarnetwork.central.common.dao.jdbc.JdbcSolarNodeOwnershipDao;
import net.solarnetwork.central.datum.v2.dao.BasicDatumCriteria;
import net.solarnetwork.central.datum.v2.dao.jdbc.JdbcObjectDatumStreamAliasEntityDao;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamAliasEntity;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.test.AbstractJUnit5JdbcDaoTestSupport;
import net.solarnetwork.central.test.tenant.SecurityContextExtension;
import net.solarnetwork.central.test.tenant.TestTenant;
import net.solarnetwork.central.test.tenant.TestTenants;
import net.solarnetwork.central.user.datum.stream.aop.UserDatumStreamAliasSecurityAspect;
import net.solarnetwork.central.user.datum.stream.biz.UserDatumStreamAliasBiz;
import net.solarnetwork.central.user.datum.stream.biz.impl.DaoUserDatumStreamAliasBiz;
import net.solarnetwork.central.user.datum.stream.domain.ObjectDatumStreamAliasEntityInput;
import net.solarnetwork.domain.datum.ObjectDatumKind;

/**
 * Verify security policies are enforced on {@link UserDatumStreamAliasBiz}
 * with the {@code UserDatumStreamAliasSecurityAspect} aspect applied, using
 * the database.
 *
 * @author matt
 * @version 1.0
 */
@ExtendWith(SecurityContextExtension.class)
public class UserDatumStreamAliasBizSecurityPolicyTests extends AbstractJUnit5JdbcDaoTestSupport {

	private TestTenants tenants;
	private TestTenant a;
	private TestTenant b;
	private JdbcObjectDatumStreamAliasEntityDao aliasDao;
	private UserDatumStreamAliasBiz biz;
	private ObjectDatumStreamAliasEntity aAlias;
	private ObjectDatumStreamAliasEntity aPolicyAlias;
	private ObjectDatumStreamAliasEntity bAlias;

	@BeforeEach
	public void setup() {
		tenants = new TestTenants();
		a = tenants.a();
		b = tenants.b();
		tenants.insert(jdbcTemplate);
		tenants.insertStreams(jdbcTemplate);
		aliasDao = new JdbcObjectDatumStreamAliasEntityDao(jdbcTemplate);
		aAlias = saveAlias(a, a.privateNodeId());
		aPolicyAlias = saveAlias(a, a.otherPrivateNodeId());
		bAlias = saveAlias(b, b.privateNodeId());
		final JdbcSolarNodeOwnershipDao ownershipDao = new JdbcSolarNodeOwnershipDao(jdbcTemplate);
		biz = securedProxy((UserDatumStreamAliasBiz) new DaoUserDatumStreamAliasBiz(aliasDao),
				new UserDatumStreamAliasSecurityAspect(ownershipDao, aliasDao)).proxy();
	}

	private ObjectDatumStreamAliasEntity saveAlias(TestTenant tenant, Long nodeId) {
		final Instant now = Instant.now();
		final String sourceId = tenant.sourceIds().getFirst();
		final ObjectDatumStreamAliasEntity alias = new ObjectDatumStreamAliasEntity(
				UUID.randomUUID(), now, now, ObjectDatumKind.Node, nodeId, sourceId + "/alias", nodeId,
				sourceId);
		aliasDao.save(alias);
		return alias;
	}

	private static Set<UUID> ids(Iterable<ObjectDatumStreamAliasEntity> results) {
		return StreamSupport.stream(results.spliterator(), false)
				.map(ObjectDatumStreamAliasEntity::getStreamId).collect(toSet());
	}

	@Test
	public void listAliases_user_ownAliasesOnly() {
		// GIVEN
		a.userActor().become();

		// WHEN
		var results = biz.listAliases(a.userId(), null);

		// THEN
		// @formatter:off
		then(ids(results))
			.as("Only the user's aliases returned")
			.containsExactlyInAnyOrder(aAlias.getStreamId(), aPolicyAlias.getStreamId())
			;
		// @formatter:on
	}

	@Test
	public void listAliases_restrictedToken_policyNodeAliasesOnly() {
		// GIVEN
		a.restrictedTokenActor().become();

		// WHEN
		var results = biz.listAliases(a.userId(), new BasicDatumCriteria());

		// THEN
		// @formatter:off
		then(ids(results))
			.as("Only aliases of the policy node returned")
			.containsExactly(aPolicyAlias.getStreamId())
			;
		// @formatter:on
	}

	@Test
	public void aliasForUser_otherUserAlias_notFound() {
		// GIVEN
		a.userActor().become();

		// THEN
		// @formatter:off
		thenExceptionOfType(AuthorizationException.class)
			.as("Another user's alias is not found")
			.isThrownBy(() -> biz.aliasForUser(a.userId(), bAlias.getStreamId()))
			;
		// @formatter:on
	}

	@Test
	public void saveAlias_otherUserAlias_denied() {
		// GIVEN
		a.userActor().become();
		final ObjectDatumStreamAliasEntityInput input = new ObjectDatumStreamAliasEntityInput();
		input.setOriginalObjectId(a.privateNodeId());
		input.setOriginalSourceId(a.sourceIds().getFirst());
		input.setObjectId(a.privateNodeId());
		input.setSourceId("/taken");

		// THEN
		// @formatter:off
		thenExceptionOfType(AuthorizationException.class)
			.as("Replacing another user's alias denied")
			.isThrownBy(() -> biz.saveAlias(a.userId(), bAlias.getStreamId(), input))
			;
		then(aliasDao.get(bAlias.getStreamId()))
			.as("Other user's alias unchanged")
			.returns(bAlias.getOriginalObjectId(), from(ObjectDatumStreamAliasEntity::getOriginalObjectId))
			.returns(bAlias.getSourceId(), from(ObjectDatumStreamAliasEntity::getSourceId))
			;
		// @formatter:on
	}

	@Test
	public void deleteAliases_user_otherUserAliasesKept() {
		// GIVEN
		a.userActor().become();

		// WHEN
		biz.deleteAliases(a.userId(), null);

		// THEN
		// @formatter:off
		then(aliasDao.get(aAlias.getStreamId()))
			.as("User's alias deleted")
			.isNull()
			;
		then(aliasDao.get(bAlias.getStreamId()))
			.as("Other user's alias not deleted")
			.isNotNull()
			;
		// @formatter:on
	}

}
