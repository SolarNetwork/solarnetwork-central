/* ==================================================================
 * IbatisUserDaoTest.java - Jan 29, 2010 9:33:23 AM
 * 
 * Copyright 2007-2010 SolarNetwork.net Dev Team
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
 * $Id$
 * ==================================================================
 */

package net.solarnetwork.central.user.dao.ibatis.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import net.solarnetwork.central.user.dao.ibatis.IbatisUserDao;
import net.solarnetwork.central.user.domain.User;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Unit test for the {@link IbatisUserDao} class.
 * 
 * @author matt
 * @version $Id$
 */
public class IbatisUserDaoTest extends AbstractIbatisUserDaoTestSupport {

	/** The tables to delete from at the start of the tests (within a transaction). */
	private static final String[] DELETE_TABLES = new String[] {"solaruser.user_user"};

	private static final String TEST_PASSWORD = "password";
	private static final String TEST_NAME = "Foo Bar";
	private static final String TEST_EMAIL = "foo@localhost.localdomain";
	private static final String TEST_ROLE_1 = "ROLE_TEST1";
	private static final String TEST_ROLE_2 = "ROLE_TEST2";

	@Autowired private IbatisUserDao userDao;

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
		logger.debug("Got new user PK: " +id);
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

}
