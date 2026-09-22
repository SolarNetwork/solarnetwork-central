/* ==================================================================
 * BaseDatumSecurityPolicyTestSupport.java - 22/09/2026 5:23:14 pm
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

package net.solarnetwork.central.datum.aop.test;

import static java.util.stream.Collectors.toSet;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import net.solarnetwork.central.common.dao.jdbc.JdbcSolarNodeOwnershipDao;
import net.solarnetwork.central.datum.v2.dao.jdbc.JdbcDatumEntityDao;
import net.solarnetwork.central.security.SecurityTokenType;
import net.solarnetwork.central.test.AbstractJUnit5JdbcDaoTestSupport;
import net.solarnetwork.central.test.tenant.SecurityContextExtension;
import net.solarnetwork.central.test.tenant.TestActor;
import net.solarnetwork.central.test.tenant.TestTenant;
import net.solarnetwork.central.test.tenant.TestTenants;
import net.solarnetwork.central.test.tenant.TestToken;
import net.solarnetwork.domain.SecurityPolicy;

/**
 * Base class for datum security policy tests using the database.
 *
 * <p>
 * The tenants, their tokens, and their node streams are inserted before each
 * test.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
@ExtendWith(SecurityContextExtension.class)
public abstract class BaseDatumSecurityPolicyTestSupport extends AbstractJUnit5JdbcDaoTestSupport {

	/** The tenants. */
	protected TestTenants tenants;

	/** Tenant A. */
	protected TestTenant a;

	/** Tenant B. */
	protected TestTenant b;

	/** The node ownership DAO. */
	protected JdbcSolarNodeOwnershipDao ownershipDao;

	/** The datum DAO. */
	protected JdbcDatumEntityDao datumDao;

	@BeforeEach
	public void setupTenants() {
		tenants = new TestTenants();
		a = tenants.a();
		b = tenants.b();
		tenants.insert(jdbcTemplate);
		tenants.insertStreams(jdbcTemplate);
		ownershipDao = new JdbcSolarNodeOwnershipDao(jdbcTemplate);
		datumDao = new JdbcDatumEntityDao(jdbcTemplate);
	}

	/**
	 * Create an actor for a new token, inserted into the database.
	 *
	 * @param tenant
	 *        the tenant that owns the token
	 * @param type
	 *        the token type
	 * @param policy
	 *        the token policy
	 * @return the actor
	 */
	protected TestActor tokenActor(TestTenant tenant, SecurityTokenType type, SecurityPolicy policy) {
		final TestToken token = tenant.newToken(type, policy);
		token.insert(jdbcTemplate);
		return TestActor.token(tenant.name() + " policy token", token);
	}

	/**
	 * Get the stream IDs of node sources of a tenant.
	 *
	 * @param tenant
	 *        the tenant
	 * @param nodeId
	 *        the node ID
	 * @param sourceIds
	 *        the source IDs, or none for all tenant sources
	 * @return the stream IDs
	 */
	protected static Set<UUID> streamIds(TestTenant tenant, Long nodeId, String... sourceIds) {
		final String[] sources = (sourceIds.length > 0 ? sourceIds
				: tenant.sourceIds().toArray(String[]::new));
		return Arrays.stream(sources).map(s -> tenant.stream(nodeId, s).getStreamId())
				.collect(toSet());
	}

}
