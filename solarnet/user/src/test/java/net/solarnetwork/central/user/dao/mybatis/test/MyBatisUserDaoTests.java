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

import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.BDDAssertions.then;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.jdbc.JdbcTestUtils;
import net.solarnetwork.central.domain.SolarLocation;
import net.solarnetwork.central.domain.UserFilterCommand;
import net.solarnetwork.central.test.CommonDbTestUtils;
import net.solarnetwork.central.user.dao.mybatis.MyBatisUserDao;
import net.solarnetwork.central.user.domain.User;
import net.solarnetwork.central.user.domain.UserFilterMatch;
import net.solarnetwork.dao.FilterResults;

/**
 * Test cases for the {@link MyBatisUserDao} class.
 * 
 * @author matt
 * @version 2.2
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

	@BeforeEach
	public void setUp() throws Exception {
		JdbcTestUtils.deleteFromTables(jdbcTemplate, DELETE_TABLES);
		userId = null;
	}

	/**
	 * Test able to persist a new user.
	 */
	@Test
	public void storeNewUser() {
		User newUser = new User(TEST_EMAIL);
		newUser.setCreated(Instant.now());
		newUser.setName(TEST_NAME);
		newUser.setPassword(TEST_PASSWORD);
		newUser.setEnabled(Boolean.TRUE);
		Long id = userDao.save(newUser);
		log.debug("Got new user PK: " + id);
		then(id).isNotNull();
		userId = id;
	}

	/**
	 * Test able to get a user.
	 */
	@Test
	public void getByPrimaryKey() {
		storeNewUser();
		User user = userDao.get(this.userId);
		// @formatter:off
		then(user)
			.isNotNull()
			.returns(userId, from(User::getId))
			.returns(TEST_NAME, from(User::getName))
			.returns(TEST_PASSWORD, from(User::getPassword))
			.returns(TEST_EMAIL, from(User::getEmail))
			.returns(true, from(User::getEnabled))
			.as("English language defaulted by database")
			.returns(Locale.ENGLISH.getLanguage(), from(User::getLang))
			.extracting(User::getCreated)
			.isNotNull()
			;
		// @formatter:on
	}

	@Test
	public void getWithLocation() {
		// GIVEN
		final Locale locale = Locale.of("es", "MX");
		final String timeZoneId = "America/Mexico_City";
		final Long locId = CommonDbTestUtils.insertLocation(jdbcTemplate, locale.getCountry(),
				timeZoneId);

		final User newUser = new User(TEST_EMAIL);
		newUser.setCreated(Instant.now());
		newUser.setName(TEST_NAME);
		newUser.setPassword(TEST_PASSWORD);
		newUser.setEnabled(Boolean.TRUE);
		newUser.setLang(locale.getLanguage());
		newUser.setLocationId(locId);

		// WHEN
		final User result = userDao.getUserWithLocation(userDao.save(newUser));

		// @formatter:off
		then(result)
			.as("User is persisted")
			.isNotNull()
			.as("Explicit language preserved")
			.returns(locale.getLanguage(), from(User::getLang))
			.as("Locale resolved from user lang and SolarLocation country")
			.returns(locale, from(User::locale))
			.extracting(User::getLocation)
			.as("Location instance provided")
			.isNotNull()
			.as("Matches user's location ID")
			.returns(locId, from(SolarLocation::getId))
			.as("Location country populated")
			.returns(locale.getCountry(), from(SolarLocation::getCountry))
			.as("Location time zone populated")
			.returns(timeZoneId, from(SolarLocation::getTimeZoneId))
			;
		// @formatter:on

		userId = result.getId();
	}

	@Test
	public void getWithLocation_noLocationId() {
		// GIVEN
		final Locale locale = Locale.of("es", "MX");

		final User newUser = new User(TEST_EMAIL);
		newUser.setCreated(Instant.now());
		newUser.setName(TEST_NAME);
		newUser.setPassword(TEST_PASSWORD);
		newUser.setEnabled(Boolean.TRUE);
		newUser.setLang(locale.getLanguage());

		// WHEN
		final User result = userDao.getUserWithLocation(userDao.save(newUser));

		// @formatter:off
		then(result)
			.as("User is returned")
			.isNotNull()
			.as("No location instance available")
			.returns(null, from(User::getLocation))
			.extracting(User::getLocation)
			;
		// @formatter:on

		userId = result.getId();
	}

	@Test
	public void storeNewUser_explicitLang() {
		// GIVEN
		User newUser = new User(TEST_EMAIL);
		newUser.setCreated(Instant.now());
		newUser.setName(TEST_NAME);
		newUser.setPassword(TEST_PASSWORD);
		newUser.setEnabled(Boolean.TRUE);
		newUser.setLang(Locale.JAPANESE.getLanguage());

		// WHEN
		final User result = userDao.get(userDao.save(newUser));

		// @formatter:off
		then(result)
			.as("User is persisted")
			.isNotNull()
			.as("Explicit language preserved")
			.returns(Locale.JAPANESE.getLanguage(), from(User::getLang))
			;
		// @formatter:on
		userId = result.getId();
	}

	@Test
	public void storeNewUser_langAndCountry() {
		// GIVEN
		final Locale locale = Locale.of("es", "MX");
		final Long locId = CommonDbTestUtils.insertLocation(jdbcTemplate, "MX", "America/Mexico_City");

		final User newUser = new User(TEST_EMAIL);
		newUser.setCreated(Instant.now());
		newUser.setName(TEST_NAME);
		newUser.setPassword(TEST_PASSWORD);
		newUser.setEnabled(Boolean.TRUE);
		newUser.setLang(locale.getLanguage());
		newUser.setLocationId(locId);

		// WHEN
		final User result = userDao.get(userDao.save(newUser));

		// @formatter:off
		then(result)
			.as("User is persisted")
			.isNotNull()
			.as("Explicit language preserved")
			.returns(locale.getLanguage(), from(User::getLang))
			.as("Locale resolved from user lang but default country without SolarLocation instance")
			.returns(Locale.of(locale.getLanguage(), Locale.US.getCountry()), from(User::locale))
			;
		// @formatter:on

		userId = result.getId();
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
		Long id = userDao.save(user);
		then(id).isNotNull().isEqualTo(userId);
		User updatedUser = userDao.get(id);
		// @formatter:off
		then(updatedUser)
			.isNotNull()
			.returns(userId, from(User::getId))
			.returns(user.getName(), from(User::getName))
			.returns(user.getPassword(), from(User::getPassword))
			.returns(user.getEmail(), from(User::getEmail))
			.returns(user.getEnabled(), from(User::getEnabled))
			.returns(created, from(User::getCreated))
			;
		// @formatter:on
	}

	@Test
	public void updateInternalDataUnchanged() {
		storeInternalPropertyNullColumn();
		User user = userDao.get(this.userId);
		user.putInternalDataValue("bim", "bam");
		Long userId = userDao.save(user);
		then(userId).isEqualTo(user.getId());
		User updated = userDao.get(user.getId());
		then(updated.getInternalData()).as("Intenral data not changed by update")
				.isEqualTo(Map.of("foo", "bar"));
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
		then(roles).isNotNull().containsExactly(TEST_ROLE_1);
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
		then(roles).isNotNull().containsExactlyInAnyOrder(TEST_ROLE_1, TEST_ROLE_2);
	}

	@Test
	public void addInternalAccountId() {
		storeNewUser();
		setupTestLocation();
		User user = userDao.get(this.userId);
		user.setLocationId(TEST_LOC_ID);
		Long id = userDao.save(user);
		then(id).isEqualTo(this.userId);

		Map<String, Object> data = Map.of("bim", (Object) "bam");
		userDao.storeInternalData(id, data);

		User updatedUser = userDao.get(id);
		// @formatter:off
		then(updatedUser)
			.isNotNull()
			.returns(userId, from(User::getId))
			.returns(user.getName(), from(User::getName))
			.returns(user.getPassword(), from(User::getPassword))
			.returns(user.getEmail(), from(User::getEmail))
			.returns(user.getEnabled(), from(User::getEnabled))
			.returns(user.getCreated(), from(User::getCreated))
			.returns(TEST_LOC_ID, from(User::getLocationId))
			.returns(data, from(User::getInternalData))
			;
		// @formatter:on
	}

	private Long storeTestUser(String email) {
		User newUser = new User(email);
		newUser.setCreated(Instant.now());
		newUser.setName(TEST_NAME);
		newUser.setPassword(TEST_PASSWORD);
		newUser.setEnabled(Boolean.TRUE);
		return userDao.save(newUser);
	}

	@Test
	public void findFilteredForEmail() {
		storeNewUser();
		storeTestUser("bar@example.com");

		UserFilterCommand criteria = new UserFilterCommand();
		criteria.setEmail(TEST_EMAIL);

		FilterResults<UserFilterMatch, Long> results = userDao.findFiltered(criteria, null, null, null);
		then(results.getTotalResults()).isEqualTo(1L);
		then(results.getReturnedResultCount()).isEqualTo(1);
		then(results).first().returns(userId, from(UserFilterMatch::getId));
	}

	@Test
	public void findFilteredForInternalProperty() {
		storeNewUser();
		User user1 = userDao.get(userId);
		Map<String, Object> billingData1 = Map.of("bim", (Object) "bam");
		user1.setInternalData(billingData1);
		userDao.storeInternalData(user1.getId(), billingData1);

		Long userId2 = storeTestUser("bar@example.com");
		User user2 = userDao.get(userId2);
		Map<String, Object> billingData2 = Map.of("bim", (Object) "baz");
		user2.setInternalData(billingData2);
		userDao.storeInternalData(user2.getId(), billingData2);

		UserFilterCommand criteria = new UserFilterCommand();
		criteria.setInternalData(billingData1);

		FilterResults<UserFilterMatch, Long> results = userDao.findFiltered(criteria, null, null, null);
		then(results.getTotalResults()).isEqualTo(1L);
		then(results.getReturnedResultCount()).isEqualTo(1);
		then(results).first().returns(userId, from(UserFilterMatch::getId));

		criteria.setInternalData(billingData2);
		results = userDao.findFiltered(criteria, null, null, null);
		then(results.getTotalResults()).isEqualTo(1L);
		then(results.getReturnedResultCount()).isEqualTo(1);
		then(results).first().returns(userId2, from(UserFilterMatch::getId));
	}

	@Test
	public void storeInternalPropertyNullColumn() {
		storeNewUser();

		Map<String, Object> data = Map.of("foo", (Object) "bar");
		userDao.storeInternalData(userId, data);

		User user = userDao.get(userId);
		then(user.getInternalData()).as("Internal data").isEqualTo(data);
	}

	@Test
	public void storeInternalPropertyAdd() {
		storeInternalPropertyNullColumn();
		User user = userDao.get(userId);

		Map<String, Object> billingData = Map.of("bim", (Object) "bam");
		userDao.storeInternalData(user.getId(), billingData);

		User updated = userDao.get(userId);
		Map<String, Object> expected = new HashMap<String, Object>(user.getInternalData());
		expected.put("bim", "bam");
		then(updated.getInternalData()).as("Internal data updated").isEqualTo(expected);
	}

	@Test
	public void storeInternalPropertyReplace() {
		storeInternalPropertyNullColumn();
		User user = userDao.get(userId);

		Map<String, Object> billingData = Map.of("foo", (Object) "bam");
		userDao.storeInternalData(user.getId(), billingData);

		User updated = userDao.get(userId);
		then(updated.getInternalData()).as("Internal data updated").isEqualTo(billingData);
	}

	@Test
	public void storeInternalPropertyNullValue() {
		storeNewUser();

		// note can not use Map.of() here because that does not support null values
		Map<String, Object> billingData = Collections.singletonMap("foo", null);
		userDao.storeInternalData(userId, billingData);

		User updated = userDao.get(userId);
		then(updated.getInternalData()).as("Internal data updated").isEqualTo(Map.of());
	}

	@Test
	public void getInternalPropertiesNull() {
		storeNewUser();
		Map<String, Object> result = userDao.getInternalData(userId);
		then(result).isNull();
	}

	@Test
	public void getInternalProperties() {
		storeInternalPropertyNullColumn();
		Map<String, Object> result = userDao.getInternalData(userId);
		then(result).isEqualTo(Map.of("foo", "bar"));
	}

}
