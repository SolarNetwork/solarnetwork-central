/* ==================================================================
 * UserDatumDeleteBizSecurityPolicyTests.java - 22/09/2026 5:23:14 pm
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

package net.solarnetwork.central.user.datum.expire.aop.test;

import static net.solarnetwork.central.datum.test.tenant.TenantDatumFixtures.DEFAULT_START;
import static net.solarnetwork.central.test.aop.SecuredProxies.securedProxy;
import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenExceptionOfType;
import static org.mockito.Mockito.mock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.core.task.AsyncTaskExecutor;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.common.dao.jdbc.JdbcSolarNodeOwnershipDao;
import net.solarnetwork.central.datum.test.dao.jdbc.DatumDbUtils;
import net.solarnetwork.central.datum.test.tenant.TenantDatumFixtures;
import net.solarnetwork.central.datum.v2.dao.jdbc.JdbcDatumEntityDao;
import net.solarnetwork.central.datum.v2.domain.Datum;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumId;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.test.AbstractJUnit5JdbcDaoTestSupport;
import net.solarnetwork.central.test.tenant.SecurityContextExtension;
import net.solarnetwork.central.test.tenant.TestTenant;
import net.solarnetwork.central.test.tenant.TestTenants;
import net.solarnetwork.central.user.dao.UserNodeDao;
import net.solarnetwork.central.user.datum.expire.aop.UserExpireSecurityAspect;
import net.solarnetwork.central.user.datum.expire.biz.UserDatumDeleteBiz;
import net.solarnetwork.central.user.datum.expire.biz.dao.DaoUserDatumDeleteBiz;
import net.solarnetwork.central.user.datum.expire.dao.UserDatumDeleteJobInfoDao;
import net.solarnetwork.domain.datum.Aggregation;
import net.solarnetwork.domain.datum.ObjectDatumKind;

/**
 * Verify {@link UserDatumDeleteBiz} only deletes datum of the actor's user with
 * the {@code UserExpireSecurityAspect} aspect applied, using the database.
 *
 * @author matt
 * @version 1.1
 */
@ExtendWith(SecurityContextExtension.class)
public class UserDatumDeleteBizSecurityPolicyTests extends AbstractJUnit5JdbcDaoTestSupport {

	private static final int DATUM_COUNT = 3;

	private TestTenants tenants;
	private TestTenant a;
	private TestTenant b;
	private UserDatumDeleteBiz biz;

	@BeforeEach
	public void setup() {
		tenants = new TestTenants();
		a = tenants.a();
		b = tenants.b();
		tenants.insert(jdbcTemplate);
		tenants.insertStreams(jdbcTemplate);
		for ( TestTenant t : List.of(a, b) ) {
			TenantDatumFixtures.insertDatum(jdbcTemplate, t, DEFAULT_START, Duration.ofMinutes(5),
					DATUM_COUNT);
		}
		final DaoUserDatumDeleteBiz dao = new DaoUserDatumDeleteBiz(Instant::now,
				mock(UserEventAppenderBiz.class), mock(AsyncTaskExecutor.class),
				mock(UserNodeDao.class), new JdbcDatumEntityDao(jdbcTemplate),
				mock(UserDatumDeleteJobInfoDao.class));
		biz = securedProxy((UserDatumDeleteBiz) dao,
				new UserExpireSecurityAspect(new JdbcSolarNodeOwnershipDao(jdbcTemplate))).proxy();
	}

	private ObjectDatumId firstDatumId(TestTenant tenant, Long nodeId) {
		final String sourceId = tenant.sourceIds().getFirst();
		final UUID streamId = tenant.stream(nodeId, sourceId).getStreamId();
		final Datum datum = DatumDbUtils.listDatum(jdbcTemplate).stream()
				.filter(d -> streamId.equals(d.getStreamId())).findFirst().orElseThrow();
		return ObjectDatumId.datumId(ObjectDatumKind.Node, streamId, nodeId, sourceId,
				datum.getTimestamp(), Aggregation.None);
	}

	private long datumCount(TestTenant tenant, Long nodeId) {
		final UUID streamId = tenant.stream(nodeId, tenant.sourceIds().getFirst()).getStreamId();
		return DatumDbUtils.listDatum(jdbcTemplate).stream()
				.filter(d -> streamId.equals(d.getStreamId())).count();
	}

	@Test
	public void deleteDatum_otherUserDatumNotDeleted() {
		// GIVEN
		final ObjectDatumId ownId = firstDatumId(a, a.privateNodeId());
		final ObjectDatumId otherId = firstDatumId(b, b.privateNodeId());
		a.userActor().become();

		// WHEN
		final Set<ObjectDatumId> result = biz.deleteDatum(a.userId(), Set.of(ownId, otherId));

		// THEN
		// @formatter:off
		then(result)
			.as("Only the user's datum deleted")
			.containsExactly(ownId)
			;
		then(datumCount(a, a.privateNodeId()))
			.as("User's datum deleted")
			.isEqualTo(DATUM_COUNT - 1)
			;
		then(datumCount(b, b.privateNodeId()))
			.as("Other user's datum not deleted")
			.isEqualTo(DATUM_COUNT)
			;
		// @formatter:on
	}

	@Test
	public void deleteDatum_otherUser_denied() {
		// GIVEN
		final ObjectDatumId otherId = firstDatumId(b, b.privateNodeId());
		a.userActor().become();

		// THEN
		// @formatter:off
		thenExceptionOfType(AuthorizationException.class)
			.as("Deleting datum as another user denied")
			.isThrownBy(() -> biz.deleteDatum(b.userId(), Set.of(otherId)))
			;
		then(datumCount(b, b.privateNodeId()))
			.as("Other user's datum not deleted")
			.isEqualTo(DATUM_COUNT)
			;
		// @formatter:on
	}

	@Test
	public void deleteDatum_restrictedToken_nonPolicyNode_denied() {
		// GIVEN
		final ObjectDatumId id = firstDatumId(a, a.privateNodeId());
		a.restrictedTokenActor().become();

		// THEN
		// @formatter:off
		thenExceptionOfType(AuthorizationException.class)
			.as("Deleting datum of a node outside the token policy denied")
			.isThrownBy(() -> biz.deleteDatum(a.userId(), Set.of(id)))
			;
		// @formatter:on
	}

}
