/* ==================================================================
 * TestTenants.java - 22/09/2026 8:39:25 am
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

package net.solarnetwork.central.test.tenant;

import java.util.List;
import org.springframework.jdbc.core.JdbcOperations;
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;

/**
 * Two isolated test tenants, "A" and "B", and the actors that access them.
 *
 * <p>
 * Security tests target tenant A's data and verify which actors are allowed
 * access. The {@link #actors()} list is the standard actor matrix:
 * </p>
 *
 * <ol>
 * <li>anonymous</li>
 * <li>A user, A user token, A restricted user token, A data token, A
 * node</li>
 * <li>B user, B user token, B data token, B node</li>
 * </ol>
 *
 * @author matt
 * @version 1.0
 */
public final class TestTenants {

	private final TestTenant a;
	private final TestTenant b;
	private final List<TestActor> actors;
	private final TenantOwnershipDao ownershipDao;

	/**
	 * Constructor.
	 */
	public TestTenants() {
		super();
		this.a = new TestTenant("A");
		this.b = new TestTenant("B");
		// @formatter:off
		this.actors = List.of(
				TestActor.ANONYMOUS,
				a.userActor(),
				a.tokenActor(),
				a.restrictedTokenActor(),
				a.dataTokenActor(),
				a.nodeActor(),
				b.userActor(),
				b.tokenActor(),
				b.dataTokenActor(),
				b.nodeActor());
		// @formatter:on
		this.ownershipDao = new TenantOwnershipDao(List.of(a, b));
	}

	/**
	 * Insert both tenants into the database.
	 *
	 * @param jdbcOps
	 *        the JDBC operations
	 * @see TestTenant#insert(JdbcOperations)
	 */
	public void insert(JdbcOperations jdbcOps) {
		a.insert(jdbcOps);
		b.insert(jdbcOps);
	}

	/**
	 * Insert the datum stream metadata of both tenants into the database.
	 *
	 * @param jdbcOps
	 *        the JDBC operations
	 * @see TestTenant#insertStreams(JdbcOperations)
	 */
	public void insertStreams(JdbcOperations jdbcOps) {
		a.insertStreams(jdbcOps);
		b.insertStreams(jdbcOps);
	}

	/**
	 * Get tenant A, whose data security tests target.
	 *
	 * @return tenant A
	 */
	public TestTenant a() {
		return a;
	}

	/**
	 * Get tenant B.
	 *
	 * @return tenant B
	 */
	public TestTenant b() {
		return b;
	}

	/**
	 * Get the anonymous actor.
	 *
	 * @return the actor
	 */
	public TestActor anonymous() {
		return TestActor.ANONYMOUS;
	}

	/**
	 * Get the standard actor matrix.
	 *
	 * @return the actors
	 */
	public List<TestActor> actors() {
		return actors;
	}

	/**
	 * Get a node ownership DAO backed by the tenant data.
	 *
	 * @return the DAO
	 */
	public SolarNodeOwnershipDao ownershipDao() {
		return ownershipDao;
	}

}
