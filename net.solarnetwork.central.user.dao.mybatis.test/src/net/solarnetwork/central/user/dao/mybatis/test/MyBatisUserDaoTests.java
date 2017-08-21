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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.central.user.dao.mybatis.MyBatisUserDao;
import net.solarnetwork.central.user.domain.User;
import net.solarnetwork.central.user.domain.UserFilterCommand;
import net.solarnetwork.central.user.domain.UserFilterMatch;

/**
 * Test cases for the {@link MyBatisUserDao} class.
 * 
 * @author matt
 * @version 1.1
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
	private static final String BILLING_ACCOUNT_ID_KEY = "accountId";
	private static final String TEST_BILLING_ACCOUNT_ID = "test.account";

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
		newUser.setCreated(new DateTime());
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
		DateTime created = user.getCreated();
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
	public void addBillingAccountId() {
		storeNewUser();
		setupTestLocation();
		User user = userDao.get(this.userId);
		user.setLocationId(TEST_LOC_ID);

		Map<String, Object> billingData = new HashMap<String, Object>();
		billingData.put(BILLING_ACCOUNT_ID_KEY, TEST_BILLING_ACCOUNT_ID);
		user.setBillingData(billingData);

		Long id = userDao.store(user);
		assertEquals(this.userId, id);
		User updatedUser = userDao.get(id);
		assertNotNull(updatedUser);
		assertEquals(this.userId, updatedUser.getId());
		assertEquals(user.getEmail(), updatedUser.getEmail());
		assertEquals(user.getName(), updatedUser.getName());
		assertEquals(user.getPassword(), updatedUser.getPassword());
		assertEquals(user.getEnabled(), updatedUser.getEnabled());
		assertEquals(TEST_LOC_ID, updatedUser.getLocationId());
		assertEquals(billingData, updatedUser.getBillingData());
	}

	private Long storeTestUser(String email) {
		User newUser = new User();
		newUser.setCreated(new DateTime());
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
	public void findFilteredForBillingProperty() {
		storeNewUser();
		User user1 = userDao.get(userId);
		Map<String, Object> billingData1 = new HashMap<String, Object>();
		billingData1.put(BILLING_ACCOUNT_ID_KEY, TEST_BILLING_ACCOUNT_ID);
		user1.setBillingData(billingData1);
		user1 = userDao.get(userDao.store(user1));

		Long userId2 = storeTestUser("bar@example.com");
		User user2 = userDao.get(userId2);
		Map<String, Object> billingData2 = new HashMap<String, Object>();
		billingData2.put(BILLING_ACCOUNT_ID_KEY, "foo-bar");
		user2.setBillingData(billingData2);
		user2 = userDao.get(userDao.store(user2));

		UserFilterCommand criteria = new UserFilterCommand();
		criteria.setBillingData(billingData1);

		FilterResults<UserFilterMatch> results = userDao.findFiltered(criteria, null, null, null);
		assertNotNull(results);
		assertEquals(1L, (long) results.getTotalResults());
		assertEquals(1, (int) results.getReturnedResultCount());
		UserFilterMatch match = results.getResults().iterator().next();
		assertEquals("Match ID", userId, match.getId());

		criteria.setBillingData(billingData2);
		results = userDao.findFiltered(criteria, null, null, null);
		assertNotNull(results);
		assertEquals(1L, (long) results.getTotalResults());
		assertEquals(1, (int) results.getReturnedResultCount());
		match = results.getResults().iterator().next();
		assertEquals("Match ID", userId2, match.getId());
	}

	@Test
	public void storeBillingPropertyNullColumn() {
		storeNewUser();

		int count = userDao.storeBillingDataProperty(userId, "foo", "bar");
		assertEquals(1, count);

		User user = userDao.get(userId);
		assertNull("Can't update null billing data", user.getBillingData());
	}

	@Test
	public void storeBillingPropertyAdd() {
		storeNewUser();
		User user = userDao.get(userId);

		Map<String, Object> billingData = new HashMap<String, Object>();
		billingData.put(BILLING_ACCOUNT_ID_KEY, TEST_BILLING_ACCOUNT_ID);
		user.setBillingData(billingData);
		userDao.store(user);

		int count = userDao.storeBillingDataProperty(userId, "foo", "bar");
		assertEquals(1, count);

		user = userDao.get(userId);
		Map<String, Object> expected = new HashMap<String, Object>(billingData);
		expected.put("foo", "bar");
		assertEquals("Billing data updated", expected, user.getBillingData());
	}

	@Test
	public void storeBillingPropertyReplace() {
		storeNewUser();
		User user = userDao.get(userId);

		Map<String, Object> billingData = new HashMap<String, Object>();
		billingData.put(BILLING_ACCOUNT_ID_KEY, TEST_BILLING_ACCOUNT_ID);
		user.setBillingData(billingData);
		userDao.store(user);

		int count = userDao.storeBillingDataProperty(userId, BILLING_ACCOUNT_ID_KEY, "bar");
		assertEquals(1, count);

		user = userDao.get(userId);
		Map<String, Object> expected = new HashMap<String, Object>();
		expected.put(BILLING_ACCOUNT_ID_KEY, "bar");
		assertEquals("Billing data updated", expected, user.getBillingData());
	}

	@Test
	public void storeBillingPropertyNullValue() {
		storeNewUser();
		User user = userDao.get(userId);

		Map<String, Object> billingData = new HashMap<String, Object>();
		billingData.put(BILLING_ACCOUNT_ID_KEY, TEST_BILLING_ACCOUNT_ID);
		user.setBillingData(billingData);
		userDao.store(user);

		int count = userDao.storeBillingDataProperty(userId, BILLING_ACCOUNT_ID_KEY, null);
		assertEquals(1, count);

		user = userDao.get(userId);
		Map<String, Object> expected = new HashMap<String, Object>();
		expected.put(BILLING_ACCOUNT_ID_KEY, null);
		assertEquals("Billing data updated", expected, user.getBillingData());
	}

}
