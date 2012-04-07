/* ==================================================================
 * IbatisUserDaoTest.java - Feb 2, 2010 2:20:41 PM
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

package net.solarnetwork.central.dras.dao.ibatis.test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.central.dras.dao.ConstraintDao;
import net.solarnetwork.central.dras.dao.FeeDao;
import net.solarnetwork.central.dras.dao.LocationDao;
import net.solarnetwork.central.dras.dao.UserDao;
import net.solarnetwork.central.dras.dao.ibatis.IbatisUserDao;
import net.solarnetwork.central.dras.domain.Constraint;
import net.solarnetwork.central.dras.domain.Fee;
import net.solarnetwork.central.dras.domain.Location;
import net.solarnetwork.central.dras.domain.Match;
import net.solarnetwork.central.dras.domain.User;
import net.solarnetwork.central.dras.domain.UserContact;
import net.solarnetwork.central.dras.domain.UserRole;
import net.solarnetwork.central.dras.support.SimpleUserFilter;

import org.apache.commons.codec.digest.DigestUtils;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Test case for the {@link IbatisUserDao} class.
 * 
 * @author matt
 * @version $Id$
 */
public class IbatisUserDaoTest extends AbstractIbatisDaoTestSupport {

	private static final String TEST_ROLENAME = "UnitTest";

	@Autowired private ConstraintDao constraintDao;
	@Autowired private UserDao userDao;
	@Autowired private LocationDao locationDao;
	@Autowired private FeeDao feeDao;
	
	private Long lastUserId;
	
	@Before
	public void setup() {
		lastUserId = null;
	}
	
	@Test
	public void getUserById() {
		// test getting unit test user
		User user = userDao.get(TEST_USER_ID);
		assertNotNull(user);
		assertNotNull(user.getId());
		assertEquals(TEST_USER_ID, user.getId());
		assertEquals(TEST_USERNAME, user.getUsername());
		assertEquals(Boolean.TRUE, user.getEnabled());
	}
	
	@Test
	public void getNonExistingUserById() {
		User user = userDao.get(-99999L);
		assertNull(user);
	}
	
	private void validateUser(User user, User entity) {
		assertNotNull("User should exist", entity);
		assertNotNull("Created date should be set", user.getCreated());
		assertArrayEquals(user.getAddress(), entity.getAddress());
		assertEquals(user.getContactInfo(), entity.getContactInfo());
		assertEquals(user.getDisplayName(), entity.getDisplayName());
		assertEquals(user.getEnabled(), entity.getEnabled());
		assertEquals(user.getPassword(), entity.getPassword());
		assertEquals(user.getUsername(), entity.getUsername());
		assertEquals(user.getVendor(), entity.getVendor());
	}
	
	@Test
	public void insertUser() {
		User user = new User();
		user.setAddress(new String[]{"One", "Two"});
		user.setDisplayName("Test User");
		user.setEnabled(Boolean.TRUE);
		user.setPassword(DigestUtils.sha256Hex("password"));
		user.setUsername("foouser");
		user.setVendor("vendor");
		
		logger.debug("Inserting new User: " +user);
		
		Long id = userDao.store(user);
		assertNotNull(id);
		
		User entity = userDao.get(id);
		validateUser(user, entity);
		
		lastUserId = id;
	}
	
	@Test
	public void insertUserWithSingleContactInfo() {
		UserContact ct1 = new UserContact(UserContact.ContactKind.VOICE, "555-555-5555", 1);
		List<UserContact> contacts = new ArrayList<UserContact>(1);
		contacts.add(ct1);
		
		User user = new User();
		user.setAddress(new String[]{"One", "Two"});
		user.setContactInfo(contacts);
		user.setDisplayName("Test Uesr");
		user.setEnabled(Boolean.TRUE);
		user.setPassword(DigestUtils.sha256Hex("password"));
		user.setUsername("foouser");
		user.setVendor("vendor");
		
		logger.debug("Inserting new User: " +user);
		
		Long id = userDao.store(user);
		assertNotNull(id);
		
		User entity = userDao.get(id);
		validateUser(user, entity);
		
		lastUserId = id;
	}
	
	@Test
	public void insertUserWithMultipleContactInfo() {
		UserContact ct1 = new UserContact(UserContact.ContactKind.VOICE, "555-555-5555", 1);
		UserContact ct2 = new UserContact(UserContact.ContactKind.FAX, "666-666-6666", 2);
		UserContact ct3 = new UserContact(UserContact.ContactKind.PAGER, "777-777-7777", null);
		List<UserContact> contacts = new ArrayList<UserContact>(1);
		contacts.add(ct1);
		contacts.add(ct2);
		contacts.add(ct3);
		
		User user = new User();
		user.setAddress(new String[]{"One", "Two"});
		user.setContactInfo(contacts);
		user.setDisplayName("Test Uesr");
		user.setEnabled(Boolean.TRUE);
		user.setPassword(DigestUtils.sha256Hex("password"));
		user.setUsername("foouser");
		user.setVendor("vendor");
		
		logger.debug("Inserting new User: " +user);
		
		Long id = userDao.store(user);
		assertNotNull(id);
		
		User entity = userDao.get(id);
		validateUser(user, entity);
		
		lastUserId = id;
	}
	
	@Test
	public void updateUser() {
		insertUser();
		assertNotNull(lastUserId);
		
		User user = userDao.get(lastUserId);
		assertEquals("foouser", user.getUsername());
		user.setUsername("foouser.update");
		
		Long id = userDao.store(user);
		assertEquals(lastUserId, id);
		
		User entity = userDao.get(id);
		validateUser(user, entity);
	}

	@Test
	public void cantUpdateSystemUser() {
		
		User user = userDao.get(0L);
		assertNotNull(user);
		assertEquals("system", user.getUsername());
		user.setUsername("haha");
		
		Long id = userDao.store(user);
		assertEquals(Long.valueOf(0), id);
		
		// verify username has NOT changed: not allowed to update system user
		User entity = userDao.get(id);
		assertEquals("system", entity.getUsername());
	}

	@Test
	public void updateUserWithSingleContact() {
		insertUserWithSingleContactInfo();
		assertNotNull(lastUserId);
		
		User user = userDao.get(lastUserId);
		assertEquals("foouser", user.getUsername());
		user.setUsername("foouser.update");
		
		Long id = userDao.store(user);
		assertEquals(lastUserId, id);
		
		User entity = userDao.get(id);
		validateUser(user, entity);
		
		// now change contact info and save again
		assertEquals(1, user.getContactInfo().size());
		UserContact ct1 = user.getContactInfo().get(0);
		assertEquals("555-555-5555", ct1.getContact());
		ct1.setContact("123-123-1234");

		id = userDao.store(user);
		assertEquals(lastUserId, id);
		entity = userDao.get(id);
		validateUser(user, entity);
	}

	@Test
	public void updateUserWithMultipleContacts() {
		insertUserWithMultipleContactInfo();
		assertNotNull(lastUserId);
		
		User user = userDao.get(lastUserId);
		assertEquals("foouser", user.getUsername());
		user.setUsername("foouser.update");
		
		Long id = userDao.store(user);
		assertEquals(lastUserId, id);
		
		User entity = userDao.get(id);
		validateUser(user, entity);
		
		// now change contact info and save again
		assertEquals(3, user.getContactInfo().size());
		UserContact ct2 = user.getContactInfo().get(1);
		assertEquals("666-666-6666", ct2.getContact());
		ct2.setContact("123-123-1234");

		user.getContactInfo().remove(0);
		
		UserContact ct4 = new UserContact(UserContact.ContactKind.EMAIL, "foo@localhost", 1);
		user.getContactInfo().add(ct4);
		
		id = userDao.store(user);
		assertEquals(lastUserId, id);
		entity = userDao.get(id);
		validateUser(user, entity);
	}
	
	@Test
	public void emptyUserRoleSet() {
		insertUser();
		Set<UserRole> roles = userDao.getUserRoles(lastUserId);
		assertNotNull(roles);
		assertEquals(0, roles.size());
	}
	
	private void validateRoles(Set<UserRole> src, Set<UserRole> found) {
		assertNotNull(found);
		assertEquals("number of roles", src.size(), found.size());
		for ( UserRole role : src ) {
			assertTrue("contains role " +role.getId(), found.contains(role));
		}
	}
	
	@Before
	public void setupTestRole() {
		setupTestRole(TEST_ROLENAME, "Unit test role");
	}
	
	@Test
	public void getUserRole() {
		insertUser();
		simpleJdbcTemplate.update(
				"insert into solardras.dras_user_role (usr_id,rolename) values (?,?)", 
				lastUserId, TEST_ROLENAME);
		
		Set<UserRole> roles = userDao.getUserRoles(lastUserId);
		Set<UserRole> expected = new LinkedHashSet<UserRole>(1);
		expected.add(new UserRole(TEST_ROLENAME));
		validateRoles(expected, roles);
	}
	
	@Test
	public void setUserRole() {
		insertUser();
		Set<String> roles = new LinkedHashSet<String>(1);
		roles.add(TEST_ROLENAME);
		userDao.assignUserRoles(lastUserId, roles);
		
		Set<UserRole> expected = new LinkedHashSet<UserRole>(1);
		expected.add(new UserRole(TEST_ROLENAME));
		
		Set<UserRole> found = userDao.getUserRoles(lastUserId);
		validateRoles(expected, found);
	}

	@Test
	public void getUserRoles() {
		insertUser();
		
		// add a second test role and assign to user
		final String testRolename2 = TEST_ROLENAME+"2";
		simpleJdbcTemplate.update(
				"insert into solardras.dras_role (rolename,description) values (?,?)", 
				testRolename2, "Unit test role 2");
		simpleJdbcTemplate.update(
				"insert into solardras.dras_user_role (usr_id,rolename) values (?,?)", 
				lastUserId, TEST_ROLENAME);
		simpleJdbcTemplate.update(
				"insert into solardras.dras_user_role (usr_id,rolename) values (?,?)", 
				lastUserId, testRolename2);
		
		Set<UserRole> roles = userDao.getUserRoles(lastUserId);
		Set<UserRole> expected = new LinkedHashSet<UserRole>(2);
		expected.add(new UserRole(TEST_ROLENAME));
		expected.add(new UserRole(testRolename2));
		validateRoles(expected, roles);
	}
	
	@Test
	public void setUserRoles() {
		insertUser();

		// add a second test role
		final String testRolename2 = TEST_ROLENAME+"2";
		simpleJdbcTemplate.update(
				"insert into solardras.dras_role (rolename,description) values (?,?)", 
				testRolename2, "Unit test role 2");

		Set<String> members = new HashSet<String>(2);
		members.add(TEST_ROLENAME);
		members.add(testRolename2);
		userDao.assignUserRoles(lastUserId, members);

		Set<UserRole> roles = new LinkedHashSet<UserRole>(2);
		for ( String role : members ) {
			roles.add(new UserRole(role));
		}

		Set<UserRole> found = userDao.getUserRoles(lastUserId);
		validateRoles(roles, found);
	}

	@Test
	public void deleteUserRoles() {
		setUserRoles();

		Set<String> members = new LinkedHashSet<String>(2);
		userDao.assignUserRoles(lastUserId, members);
		
		Set<UserRole> expected = new HashSet<UserRole>(2);
		
		Set<UserRole> found = userDao.getUserRoles(lastUserId);
		validateRoles(expected, found);
	}


	@Test
	public void updateUserRoles() {
		setUserRoles();
		Set<UserRole> roles = userDao.getUserRoles(lastUserId);
		roles.remove(roles.iterator().next());
		Set<String> members = new HashSet<String>();
		for ( UserRole role : roles ) {
			members.add(role.getId());
		}
		userDao.assignUserRoles(lastUserId, members);
		
		Set<UserRole> found = userDao.getUserRoles(lastUserId);
		validateRoles(roles, found);
	}
	
	@Test
	public void findFilteredEmptySet() {
		SimpleUserFilter filter = new SimpleUserFilter();
		filter.setUsername("foo");
		FilterResults<Match> results = userDao.findFiltered(filter, null, null, null);
		assertNotNull(results);
		assertEquals(0, results.getReturnedResultCount().intValue());
	}
	
	@Test
	public void cantFindSystemUser() {
		// search with empty filter, i.e. all users
		SimpleUserFilter filter = new SimpleUserFilter();
		FilterResults<Match> results = userDao.findFiltered(filter, null, null, null);
		assertNotNull(results);
		assertEquals(1, results.getReturnedResultCount().intValue());
		assertEquals(TEST_USER_ID, results.getResults().iterator().next().getId());
		
		// now explicitly search for system user
		filter.setUsername("system");
		results = userDao.findFiltered(filter, null, null, null);
		assertNotNull(results);
		assertEquals(0, results.getReturnedResultCount().intValue());
	}
	
	@Test
	public void findFilteredUsername() {
		SimpleUserFilter filter = new SimpleUserFilter();
		filter.setUsername(TEST_USERNAME);
		FilterResults<Match> results = userDao.findFiltered(filter, null, null, null);
		assertNotNull(results);
		assertEquals(1, results.getReturnedResultCount().intValue());
		assertEquals(TEST_USER_ID, results.getResults().iterator().next().getId());
	}
	
	@Test
	public void findFilteredUniqueId() {
		SimpleUserFilter filter = new SimpleUserFilter();
		filter.setUniqueId(TEST_USERNAME);
		FilterResults<Match> results = userDao.findFiltered(filter, null, null, null);
		assertNotNull(results);
		assertEquals(1, results.getReturnedResultCount().intValue());
		assertEquals(TEST_USER_ID, results.getResults().iterator().next().getId());
		
		filter.setUniqueId("foo");
		results = userDao.findFiltered(filter, null, null, null);
		assertNotNull(results);
		assertEquals(0, results.getReturnedResultCount().intValue());
	}
	
	@Test
	public void findFilteredRoleEmptySet() {
		List<String> roles = new ArrayList<String>(1);
		roles.add(TEST_ROLENAME);
		SimpleUserFilter filter = new SimpleUserFilter();
		filter.setRoles(roles);
		FilterResults<Match> results = userDao.findFiltered(filter, null, null, null);
		assertNotNull(results);
		assertEquals(0, results.getReturnedResultCount().intValue());
	}
	
	@Test
	public void findFilteredRole() {
		setUserRoles();
		List<String> roles = new ArrayList<String>(1);
		roles.add(TEST_ROLENAME);
		SimpleUserFilter filter = new SimpleUserFilter();
		filter.setRoles(roles);
		FilterResults<Match> results = userDao.findFiltered(filter, null, null, null);
		assertNotNull(results);
		assertEquals(1, results.getReturnedResultCount().intValue());
		assertEquals(lastUserId, results.getResults().iterator().next().getId());
	}
	
	@Test
	public void findFilteredRoleMultipleResults() {
		insertUserWithMultipleContactInfo();

		Set<String> members = new LinkedHashSet<String>(1);
		members.add(TEST_ROLENAME);
		userDao.assignUserRoles(lastUserId, members);
		
		final String secondRole = "Another Role";
		setupTestRole(secondRole, "Unit test role");
		
		Set<String> roleSet = new HashSet<String>(1);
		roleSet.add(TEST_ROLENAME);
		roleSet.add(secondRole);
		setupTestUser(-8889L, "afoo");
		userDao.assignUserRoles(-8889L, roleSet);

		List<String> roles = new ArrayList<String>(1);
		roles.add(TEST_ROLENAME);
		roles.add(secondRole);
		SimpleUserFilter filter = new SimpleUserFilter();
		filter.setRoles(roles);
		FilterResults<Match> results = userDao.findFiltered(filter, null, null, null);
		assertNotNull(results);
		assertEquals(2, results.getReturnedResultCount().intValue());
		Iterator<Match> itr = results.getResults().iterator();
		assertEquals(Long.valueOf(-8889L), itr.next().getId());
		assertEquals(lastUserId, itr.next().getId());
		
		// now remove TEST_ROLENAME, should just find afoo user
		roles.remove(0);
		filter.setRoles(roles);
		results = userDao.findFiltered(filter, null, null, null);
		assertNotNull(results);
		assertEquals(1, results.getReturnedResultCount().intValue());
		assertEquals(Long.valueOf(-8889L), results.getResults().iterator().next().getId());
	}
	
	@Test
	public void findFilteredUserGroupEmptySet() {
		List<Long> groups = new ArrayList<Long>(1);
		groups.add(TEST_GROUP_ID);
		SimpleUserFilter filter = new SimpleUserFilter();
		filter.setUserGroups(groups);
		FilterResults<Match> results = userDao.findFiltered(filter, null, null, null);
		assertNotNull(results);
		assertEquals(0, results.getReturnedResultCount().intValue());
	}
	
	@Test
	public void findFilteredUserGroup() {
		insertUser();
		setupTestLocation();
		setupTestUserGroup(TEST_GROUP_ID, TEST_GROUPNAME, TEST_LOCATION_ID);
		assignUserToGroup(TEST_GROUP_ID, lastUserId, TEST_EFFECTIVE_ID);
		List<Long> groups = new ArrayList<Long>(1);
		groups.add(TEST_GROUP_ID);
		SimpleUserFilter filter = new SimpleUserFilter();
		filter.setUserGroups(groups);
		FilterResults<Match> results = userDao.findFiltered(filter, null, null, null);
		assertNotNull(results);
		assertEquals(1, results.getReturnedResultCount().intValue());
		assertEquals(lastUserId, results.getResults().iterator().next().getId());
	}
	
	@Test
	public void findFilteredUserGroupAndRole() {
		setUserRole();
		setupTestLocation();
		setupTestUserGroup(TEST_GROUP_ID, TEST_GROUPNAME, TEST_LOCATION_ID);
		assignUserToGroup(TEST_GROUP_ID, lastUserId, TEST_EFFECTIVE_ID);
		
		// create second member of group, but not member of role
		setupTestUser(-8889L, "atwo");
		assignUserToGroup(TEST_GROUP_ID, -8889L, TEST_EFFECTIVE_ID);
		
		List<Long> groups = new ArrayList<Long>(1);
		groups.add(TEST_GROUP_ID);
		SimpleUserFilter filter = new SimpleUserFilter();
		filter.setUserGroups(groups);
		
		FilterResults<Match> results = userDao.findFiltered(filter, null, null, null);
		assertNotNull(results);
		assertEquals(2, results.getReturnedResultCount().intValue());
		Iterator<Match> itr = results.getResults().iterator();
		assertEquals(Long.valueOf(-8889L), itr.next().getId());
		assertEquals(lastUserId, itr.next().getId());
		
		// now add role to search criteria, should only find one
		List<String> roles = new ArrayList<String>(1);
		roles.add(TEST_ROLENAME);
		filter.setRoles(roles);
		
		results = userDao.findFiltered(filter, null, null, null);
		assertNotNull(results);
		assertEquals(1, results.getReturnedResultCount().intValue());
		assertEquals(lastUserId, results.getResults().iterator().next().getId());
	}
	
	@Test
	public void findFilteredBoxEmptySet() {
		insertUser();
		
		// now search for box coordinates
		SimpleUserFilter filter = new SimpleUserFilter();
		filter.setLatitude(-3.3);
		filter.setLongitude(-3.5);
		filter.setBoxLatitude(3.3);
		filter.setBoxLongitude(3.5);

		FilterResults<Match> results = userDao.findFiltered(filter, null, null, null);
		assertNotNull(results);
		assertEquals(0, results.getReturnedResultCount().intValue());
	}
	
	@Test
	public void findFilteredBox() {
		insertUser();

		// create a location with lat/long
		setupTestLocation();
		Location l = locationDao.get(TEST_LOCATION_ID);
		l.setLatitude(1.1234);
		l.setLongitude(2.3456);
		locationDao.store(l);
		
		// create group at this location, assign user to it
		setupTestUserGroup(TEST_GROUP_ID, TEST_GROUPNAME, TEST_LOCATION_ID);
		assignUserToGroup(TEST_GROUP_ID, lastUserId, TEST_EFFECTIVE_ID);
		
		// now search for box coordinates
		SimpleUserFilter filter = new SimpleUserFilter();
		filter.setLatitude(-3.3);
		filter.setLongitude(-3.5);
		filter.setBoxLatitude(3.3);
		filter.setBoxLongitude(3.5);

		FilterResults<Match> results = userDao.findFiltered(filter, null, null, null);
		assertNotNull(results);
		assertEquals(1, results.getReturnedResultCount().intValue());
		assertEquals(lastUserId, results.getResults().iterator().next().getId());
	}
	
	@Test
	public void findFilteredEnabled() {
		// we expect the "system" and "unittest" users to exist at
		// this point, the latter created for the test Effective
		// set up before every test
		
		// first search with no filter
		SimpleUserFilter filter = new SimpleUserFilter();

		FilterResults<Match> results = userDao.findFiltered(filter, null, null, null);
		assertNotNull(results);
		assertEquals(1, results.getReturnedResultCount().intValue());
		assertEquals(TEST_USER_ID, results.getResults().iterator().next().getId());

		// now search for enabled only
		filter.setEnabled(Boolean.TRUE);
		results = userDao.findFiltered(filter, null, null, null);
		assertNotNull(results);
		assertEquals(1, results.getReturnedResultCount().intValue());
		assertEquals(TEST_USER_ID, results.getResults().iterator().next().getId());
		
		// now search for disabled only
		filter.setEnabled(Boolean.FALSE);
		results = userDao.findFiltered(filter, null, null, null);
		assertNotNull(results);
		assertEquals(0, results.getReturnedResultCount().intValue());
	}
	
	@Test
	public void assignConstraints() {
		Constraint c = createConstraint();
		c.setId(constraintDao.store(c));
		Constraint c2 = createConstraint();
		c2.setId(constraintDao.store(c2));
		insertUser();
		
		Set<Constraint> members = new LinkedHashSet<Constraint>(2);
		members.add(c);
		members.add(c2);
		
		Set<Long> memberIds = new LinkedHashSet<Long>(2);
		memberIds.add(c.getId());
		memberIds.add(c2.getId());
		
		userDao.assignConstraints(lastUserId, memberIds, TEST_EFFECTIVE_ID);

		Set<Constraint> found = userDao.getConstraints(lastUserId, new DateTime());
		validateMembers(members, found);
	}

	@Test
	public void assignProgramConstraints() {
		Constraint c = createConstraint();
		c.setId(constraintDao.store(c));
		Constraint c2 = createConstraint();
		c2.setId(constraintDao.store(c2));
		insertUser();
		setupTestProgram(TEST_PROGRAM_ID, TEST_PROGRAM_NAME);
		
		Set<Constraint> members = new LinkedHashSet<Constraint>(2);
		members.add(c);
		members.add(c2);
		
		Set<Long> memberIds = new LinkedHashSet<Long>(2);
		memberIds.add(c.getId());
		memberIds.add(c2.getId());
		
		userDao.assignUserProgramConstraints(lastUserId, TEST_PROGRAM_ID, memberIds, TEST_EFFECTIVE_ID);

		Set<Constraint> found = userDao.getUserProgramConstraints(lastUserId, TEST_PROGRAM_ID, new DateTime());
		validateMembers(members, found);
	}
	
	@Test
	public void setFee() {
		Fee f = createFee();
		f.setId(feeDao.store(f));
		insertUser();
		
		userDao.setFee(lastUserId, f.getId(), TEST_EFFECTIVE_ID);
	}
	
	@Test 
	public void getFee() {
		Fee f = createFee();
		f.setId(feeDao.store(f));
		
		
		Fee found = userDao.getFee(lastUserId, null);
		assertNull(found);
		
		insertUser();
		userDao.setFee(lastUserId, f.getId(), TEST_EFFECTIVE_ID);
		
		found = userDao.getFee(lastUserId, null);
		assertNotNull(found);
		assertEquals(f.getId(), found.getId());
	}
}
