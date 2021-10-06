/* ==================================================================
 * JdbcSolarNodeOwnershipDaoTests.java - 28/02/2020 3:12:46 pm
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.common.dao.jdbc.test;

import static net.solarnetwork.central.domain.BasicSolarNodeOwnership.ownershipFor;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.central.common.dao.jdbc.JdbcSolarNodeOwnershipDao;
import net.solarnetwork.central.domain.BasicSolarNodeOwnership;
import net.solarnetwork.central.domain.SolarNodeOwnership;
import net.solarnetwork.central.test.AbstractJdbcDaoTestSupport;

/**
 * Test cases for the {@link JdbcSolarNodeOwnershipDao} class.
 * 
 * @author matt
 * @version 1.0
 */
public class JdbcSolarNodeOwnershipDaoTests extends AbstractJdbcDaoTestSupport {

	private static final Long TEST_USER_ID = Long.valueOf(-9999);
	private static final String TEST_USERNAME = "unittest@localhost";
	private static final Long TEST_USER_ID_2 = Long.valueOf(-9998);
	private static final String TEST_USERNAME_2 = "unittest2@localhost";

	private JdbcSolarNodeOwnershipDao dao;

	private void setupTestUser(Long id, String username) {
		jdbcTemplate.update(
				"insert into solaruser.user_user (id,email,password,disp_name,enabled) values (?,?,?,?,?)",
				id, username, DigestUtils.sha256Hex("password"), "Unit Test", Boolean.TRUE);
	}

	private void setupTestUserNode(Long userId, Long nodeId, String name) {
		jdbcTemplate.update("insert into solaruser.user_node (user_id,node_id,disp_name) values (?,?,?)",
				userId, nodeId, name);
	}

	@Before
	public void setup() {
		dao = new JdbcSolarNodeOwnershipDao(jdbcTemplate);
	}

	@Test
	public void ownershipForNodeId_noMatch() {
		// GIVEN

		// WHEN
		SolarNodeOwnership ownership = dao.ownershipForNodeId(TEST_NODE_ID);

		// THEN
		assertThat("Null returned when no match", ownership, nullValue());
	}

	@Test
	public void ownershipForNodeId_match() {
		// GIVEN
		setupTestLocation(TEST_LOC_ID, "Pacific/Auckland");
		setupTestNode(TEST_NODE_ID, TEST_LOC_ID);
		setupTestUser(TEST_USER_ID, TEST_USERNAME);
		setupTestUserNode(TEST_USER_ID, TEST_NODE_ID, "Test Node");

		// WHEN
		SolarNodeOwnership ownership = dao.ownershipForNodeId(TEST_NODE_ID);

		// THEN
		BasicSolarNodeOwnership expected = ownershipFor(TEST_NODE_ID, TEST_USER_ID);
		assertThat("Ownership found", expected.isSameAs(ownership), equalTo(true));
	}

	@Test
	public void ownershipsForUserId_noMatch() {
		// GIVEN

		// WHEN
		SolarNodeOwnership[] ownerships = dao.ownershipsForUserId(TEST_USER_ID);

		// THEN
		assertThat("Null returned when no match", ownerships, nullValue());
	}

	@Test
	public void ownershipsForUserId_match() {
		// GIVEN
		setupTestLocation(TEST_LOC_ID, "Pacific/Auckland");
		setupTestNode(TEST_NODE_ID, TEST_LOC_ID);
		setupTestUser(TEST_USER_ID, TEST_USERNAME);
		setupTestUserNode(TEST_USER_ID, TEST_NODE_ID, "Test Node");

		// WHEN
		SolarNodeOwnership[] ownerships = dao.ownershipsForUserId(TEST_USER_ID);

		// THEN
		assertThat("One match returned", ownerships, is(arrayWithSize(1)));
		BasicSolarNodeOwnership expected = ownershipFor(TEST_NODE_ID, TEST_USER_ID);
		assertThat("Ownership returned when single match", expected.isSameAs(ownerships[0]), is(true));
	}

	@Test
	public void ownershipsForUserId_matchMulti() {
		// GIVEN
		setupTestLocation(TEST_LOC_ID, "Pacific/Auckland");
		setupTestUser(TEST_USER_ID, TEST_USERNAME);
		List<BasicSolarNodeOwnership> expected = new ArrayList<>(5);
		SecureRandom r = new SecureRandom();
		for ( int i = 0; i < 5; i++ ) {
			Long nodeId = r.nextLong();
			setupTestNode(nodeId, TEST_LOC_ID);
			setupTestUserNode(TEST_USER_ID, nodeId, "Test Node " + i);
			expected.add(BasicSolarNodeOwnership.ownershipFor(nodeId, TEST_USER_ID));
		}
		// toss in some nodes for a different user, to verify they are NOT returned
		setupTestUser(TEST_USER_ID_2, TEST_USERNAME_2);
		for ( int i = 0; i < 5; i++ ) {
			Long nodeId = r.nextLong();
			setupTestNode(nodeId, TEST_LOC_ID);
			setupTestUserNode(TEST_USER_ID_2, nodeId, "User 2 Test Node " + i);
		}
		Collections.sort(expected, Comparator.comparing(BasicSolarNodeOwnership::getNodeId));

		// WHEN
		SolarNodeOwnership[] ownerships = dao.ownershipsForUserId(TEST_USER_ID);

		// THEN
		assertThat("One match returned", ownerships, is(arrayWithSize(expected.size())));
		for ( int i = 0; i < expected.size(); i++ ) {
			assertThat("Ownership returned in node order for multi match",
					expected.get(i).isSameAs(ownerships[i]), is(true));
		}
	}

}
