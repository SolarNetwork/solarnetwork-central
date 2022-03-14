/* ==================================================================
 * MyBatisUserDaoTests.java - Nov 11, 2014 6:39:45 AM
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.central.domain.UserFilterCommand;
import net.solarnetwork.central.user.dao.mybatis.MyBatisUserDao;
import net.solarnetwork.central.user.domain.User;
import net.solarnetwork.central.user.domain.UserFilterMatch;

/**
 * Test cases for the {@link MyBatisUserDao} class.
 * 
 * @author matt
 * @version 2.0
 */
public class MyBatisUserDaoTests extends AbstractMyBatisUserDaoTestSupport {

	/**
	 * The tables to delete from at the start of the tests (within a
	 * transaction).
	 */
	private static final String[] DELETE_TABLES = new String[] { "solaruser.user_user" };

	private static final String TEST_PASSWORD = "password";
	private static final String TEST_NAME = "Foo Bar";
	private static final String TEST_EMAIL = "foo@localhost.localdomain";
	private static final String TEST_ROLE_1 = "ROLE_TEST1";
	private static final String TEST_ROLE_2 = "ROLE_TEST2";

	private Long userId = null;

	@Before
	public void setUp() throws Exception {
		deleteFromTables(DELETE_TABLES);
		userId = null;
	}

	/**
	 * Test able to persist a new user.
	 */
	@Test
	public void storeNewUser() {
		User newUser = new User();
		newUser.setCreated(Instant.now());
		newUser.setEmail(TEST_EMAIL);
		newUser.setName(TEST_NAME);
		newUser.setPassword(TEST_PASSWORD);
		newUser.setEnabled(Boolean.TRUE);
		Long id = userDao.store(newUser);
		logger.debug("Got new user PK: " + id);
		assertNotNull(id);
		userId = id;
	}

	/**
	 * Test able to get a user.
	 */
	@Test
	public void getByPrimaryKey() {
		storeNewUser();
		User user = userDao.get(this.userId);
		assertNotNull(user);
		assertEquals(this.userId, user.getId());
		assertEquals(TEST_NAME, user.getName());
		assertEquals(TEST_PASSWORD, user.getPassword());
		assertEquals(TEST_EMAIL, user.getEmail());
		assertNotNull(user.getEnabled());
		assertTrue(user.getEnabled());
		assertNotNull(user.getCreated());
	}

	/**
	 * Test able to update an existing user.
	 */
	@Test
	public void update() {
		storeNewUser();
		User user = userDao.get(this.userId);
		Instant created = user.getCreated();
		user.setName("New Name");
		user.setPassword("New Password");
		user.setEmail("New Email");
		user.setEnabled(Boolean.FALSE);
		Long id = userDao.store(user);
		assertNotNull(id);
		assertEquals(this.userId, id);
		User updatedUser = userDao.get(id);
		assertNotNull(updatedUser);
		assertEquals(this.userId, updatedUser.getId());
		assertEquals(user.getEmail(), updatedUser.getEmail());
		assertEquals(user.getName(), updatedUser.getName());
		assertEquals(user.getPassword(), updatedUser.getPassword());
		assertEquals(user.getEnabled(), updatedUser.getEnabled());
		assertEquals(created, updatedUser.getCreated());
	}

	@Test
	public void updateInternalDataUnchanged() {
		storeInternalPropertyNullColumn();
		User user = userDao.get(this.userId);
		user.putInternalDataValue("bim", "bam");
		Long userId = userDao.store(user);
		assertEquals(user.getId(), userId);
		User updated = userDao.get(user.getId());
		assertEquals("Intenral data not changed by update", Collections.singletonMap("foo", "bar"),
				updated.getInternalData());
	}

	/**
	 * Test able to persist a single role.
	 */
	@Test
	public void storeRole() {
		storeNewUser();
		User user = userDao.get(this.userId);
		Set<String> roles = new HashSet<String>();
		roles.add(TEST_ROLE_1);
		userDao.storeUserRoles(user, roles);
	}

	/**
	 * Test able to get a single role.
	 */
	@Test
	public void getRole() {
		storeRole();
		User user = userDao.get(this.userId);
		Set<String> roles = userDao.getUserRoles(user);
		assertNotNull(roles);
		assertEquals(1, roles.size());
		assertEquals(TEST_ROLE_1, roles.iterator().next());
	}

	/**
	 * Test able to persist a set of roles.
	 */
	@Test
	public void storeRoles() {
		storeNewUser();
		User user = userDao.get(this.userId);
		Set<String> roles = new HashSet<String>();
		roles.add(TEST_ROLE_1);
		roles.add(TEST_ROLE_2);
		userDao.storeUserRoles(user, roles);
	}

	/**
	 * Test able to get a set of roles.
	 */
	@Test
	public void getRoles() {
		storeRoles();
		User user = userDao.get(this.userId);
		Set<String> roles = userDao.getUserRoles(user);
		assertNotNull(roles);
		assertEquals(2, roles.size());
		assertTrue(roles.contains(TEST_ROLE_1));
		assertTrue(roles.contains(TEST_ROLE_2));
	}

	@Test
	public void addInternalAccountId() {
		storeNewUser();
		setupTestLocation();
		User user = userDao.get(this.userId);
		user.setLocationId(TEST_LOC_ID);
		Long id = userDao.store(user);
		assertEquals(this.userId, id);

		Map<String, Object> data = Collections.singletonMap("bim", (Object) "bam");
		userDao.storeInternalData(id, data);

		User updatedUser = userDao.get(id);
		assertNotNull(updatedUser);
		assertEquals(this.userId, updatedUser.getId());
		assertEquals(user.getEmail(), updatedUser.getEmail());
		assertEquals(user.getName(), updatedUser.getName());
		assertEquals(user.getPassword(), updatedUser.getPassword());
		assertEquals(user.getEnabled(), updatedUser.getEnabled());
		assertEquals(TEST_LOC_ID, updatedUser.getLocationId());

		assertEquals(data, updatedUser.getInternalData());
	}

	private Long storeTestUser(String email) {
		User newUser = new User();
		newUser.setCreated(Instant.now());
		newUser.setEmail(email);
		newUser.setName(TEST_NAME);
		newUser.setPassword(TEST_PASSWORD);
		newUser.setEnabled(Boolean.TRUE);
		return userDao.store(newUser);
	}

	@Test
	public void findFilteredForEmail() {
		storeNewUser();
		storeTestUser("bar@example.com");

		UserFilterCommand criteria = new UserFilterCommand();
		criteria.setEmail(TEST_EMAIL);

		FilterResults<UserFilterMatch> results = userDao.findFiltered(criteria, null, null, null);
		assertNotNull(results);
		assertEquals(1L, (long) results.getTotalResults());
		assertEquals(1, (int) results.getReturnedResultCount());
		UserFilterMatch match = results.getResults().iterator().next();
		assertEquals("Match ID", userId, match.getId());
	}

	@Test
	public void findFilteredForInternalProperty() {
		storeNewUser();
		User user1 = userDao.get(userId);
		Map<String, Object> billingData1 = Collections.singletonMap("bim", (Object) "bam");
		user1.setInternalData(billingData1);
		userDao.storeInternalData(user1.getId(), billingData1);

		Long userId2 = storeTestUser("bar@example.com");
		User user2 = userDao.get(userId2);
		Map<String, Object> billingData2 = Collections.singletonMap("bim", (Object) "baz");
		user2.setInternalData(billingData2);
		userDao.storeInternalData(user2.getId(), billingData2);

		UserFilterCommand criteria = new UserFilterCommand();
		criteria.setInternalData(billingData1);

		FilterResults<UserFilterMatch> results = userDao.findFiltered(criteria, null, null, null);
		assertNotNull(results);
		assertEquals(1L, (long) results.getTotalResults());
		assertEquals(1, (int) results.getReturnedResultCount());
		UserFilterMatch match = results.getResults().iterator().next();
		assertEquals("Match ID", userId, match.getId());

		criteria.setInternalData(billingData2);
		results = userDao.findFiltered(criteria, null, null, null);
		assertNotNull(results);
		assertEquals(1L, (long) results.getTotalResults());
		assertEquals(1, (int) results.getReturnedResultCount());
		match = results.getResults().iterator().next();
		assertEquals("Match ID", userId2, match.getId());
	}

	@Test
	public void storeInternalPropertyNullColumn() {
		storeNewUser();

		Map<String, Object> data = Collections.singletonMap("foo", (Object) "bar");
		userDao.storeInternalData(userId, data);

		User user = userDao.get(userId);
		assertEquals("Internal data", data, user.getInternalData());
	}

	@Test
	public void storeInternalPropertyAdd() {
		storeInternalPropertyNullColumn();
		User user = userDao.get(userId);

		Map<String, Object> billingData = Collections.singletonMap("bim", (Object) "bam");
		userDao.storeInternalData(user.getId(), billingData);

		User updated = userDao.get(userId);
		Map<String, Object> expected = new HashMap<String, Object>(user.getInternalData());
		expected.put("bim", "bam");
		assertEquals("Internal data updated", expected, updated.getInternalData());
	}

	@Test
	public void storeInternalPropertyReplace() {
		storeInternalPropertyNullColumn();
		User user = userDao.get(userId);

		Map<String, Object> billingData = Collections.singletonMap("foo", (Object) "bam");
		userDao.storeInternalData(user.getId(), billingData);

		User updated = userDao.get(userId);
		assertEquals("Internal data updated", billingData, updated.getInternalData());
	}

	@Test
	public void storeInternalPropertyNullValue() {
		storeNewUser();

		Map<String, Object> billingData = Collections.singletonMap("foo", null);
		userDao.storeInternalData(userId, billingData);

		User updated = userDao.get(userId);
		assertEquals("Internal data updated", Collections.emptyMap(), updated.getInternalData());
	}

	@Test
	public void getInternalPropertiesNull() {
		storeNewUser();
		Map<String, Object> result = userDao.getInternalData(userId);
		assertNull(result);
	}

	@Test
	public void getInternalProperties() {
		storeInternalPropertyNullColumn();
		Map<String, Object> result = userDao.getInternalData(userId);
		assertEquals(Collections.singletonMap("foo", (Object) "bar"), result);
	}

}
