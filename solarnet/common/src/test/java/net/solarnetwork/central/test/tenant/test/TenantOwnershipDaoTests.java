/* ==================================================================
 * TenantOwnershipDaoTests.java - 22/09/2026 8:39:25 am
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

package net.solarnetwork.central.test.tenant.test;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.groups.Tuple.tuple;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.assertj.core.groups.Tuple;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.common.dao.jdbc.JdbcSolarNodeOwnershipDao;
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;
import net.solarnetwork.central.domain.SolarNodeOwnership;
import net.solarnetwork.central.test.AbstractJUnit5JdbcDaoTestSupport;
import net.solarnetwork.central.test.tenant.TenantOwnershipDao;
import net.solarnetwork.central.test.tenant.TestTenant;
import net.solarnetwork.central.test.tenant.TestTenants;
import net.solarnetwork.central.test.tenant.TestToken;
import net.solarnetwork.domain.datum.ObjectDatumStreamIdentity;

/**
 * Test cases for the {@link TenantOwnershipDao} class, verifying it returns
 * the same results as {@link JdbcSolarNodeOwnershipDao} for tenants inserted
 * into the database.
 *
 * @author matt
 * @version 1.0
 */
public class TenantOwnershipDaoTests extends AbstractJUnit5JdbcDaoTestSupport {

	private TestTenants tenants;
	private List<TestTenant> tenantList;
	private SolarNodeOwnershipDao stubDao;
	private SolarNodeOwnershipDao jdbcDao;

	@BeforeEach
	public void setup() {
		tenants = new TestTenants();
		tenantList = List.of(tenants.a(), tenants.b());
		tenants.insert(jdbcTemplate);
		tenants.insertStreams(jdbcTemplate);
		stubDao = tenants.ownershipDao();
		jdbcDao = new JdbcSolarNodeOwnershipDao(jdbcTemplate);
	}

	private static @Nullable Tuple ownership(@Nullable SolarNodeOwnership o) {
		return (o != null
				? tuple(o.getNodeId(), o.getUserId(), o.getCountry(), o.getZone(),
						o.isRequiresAuthorization(), o.isArchived())
				: null);
	}

	private static List<@Nullable Tuple> ownerships(SolarNodeOwnership @Nullable [] os) {
		return (os != null ? Arrays.stream(os).map(TenantOwnershipDaoTests::ownership)
				.sorted((l, r) -> ((Long) l.toArray()[0]).compareTo((Long) r.toArray()[0])).toList()
				: List.of());
	}

	@Test
	public void ownershipForNodeId() {
		for ( TestTenant tenant : tenantList ) {
			for ( Long nodeId : tenant.nodeIds() ) {
				// WHEN
				final Tuple stub = ownership(stubDao.ownershipForNodeId(nodeId));
				final Tuple jdbc = ownership(jdbcDao.ownershipForNodeId(nodeId));

				// THEN
				// @formatter:off
				then(stub)
					.as("Stub ownership of %s node %d same as database", tenant.name(), nodeId)
					.isNotNull()
					.isEqualTo(jdbc)
					;
				// @formatter:on
			}
		}
	}

	@Test
	public void ownershipForNodeId_unknown() {
		// WHEN
		final SolarNodeOwnership result = stubDao.ownershipForNodeId(-1L);

		// THEN
		then(result).as("Unknown node has no ownership").isNull();
	}

	@Test
	public void ownershipsForUserId() {
		for ( TestTenant tenant : tenantList ) {
			// WHEN
			final List<Tuple> stub = ownerships(stubDao.ownershipsForUserId(tenant.userId()));
			final List<Tuple> jdbc = ownerships(jdbcDao.ownershipsForUserId(tenant.userId()));

			// THEN
			// @formatter:off
			then(stub)
				.as("Stub ownerships of %s same as database", tenant.name())
				.hasSize(tenant.nodeIds().size())
				.isEqualTo(jdbc)
				;
			// @formatter:on
		}
	}

	@Test
	public void nonArchivedNodeIdsForToken() {
		for ( TestTenant tenant : tenantList ) {
			for ( TestToken token : tenant.tokens() ) {
				// WHEN
				final Long[] stub = stubDao.nonArchivedNodeIdsForToken(token.tokenId());
				final Long[] jdbc = jdbcDao.nonArchivedNodeIdsForToken(token.tokenId());

				// THEN
				// @formatter:off
				then(stub)
					.as("Stub token %s nodes same as database", token.tokenId())
					.isNotEmpty()
					.containsExactly(jdbc)
					;
				// @formatter:on
			}
		}
	}

	@Test
	public void nonArchivedNodeIdsForToken_restricted() {
		// GIVEN
		final TestTenant a = tenants.a();

		// WHEN
		final Long[] result = stubDao.nonArchivedNodeIdsForToken(a.restrictedUserToken().tokenId());

		// THEN
		// @formatter:off
		then(result)
			.as("Restricted token allows only the policy node")
			.containsExactly(a.otherPrivateNodeId())
			;
		// @formatter:on
	}

	@Test
	public void getDatumStreamMetadataIds() {
		// GIVEN
		final UUID[] streamIds = tenantList.stream().flatMap(t -> t.streams().stream())
				.map(ObjectDatumStreamIdentity::getStreamId).toArray(UUID[]::new);

		// WHEN
		final var stub = stubDao.getDatumStreamMetadataIds(streamIds);
		final var jdbc = jdbcDao.getDatumStreamMetadataIds(streamIds);

		// THEN
		// @formatter:off
		then(stub)
			.as("Stub stream IDs same as database")
			.hasSize(streamIds.length)
			.isEqualTo(jdbc)
			;
		// @formatter:on
	}

}
