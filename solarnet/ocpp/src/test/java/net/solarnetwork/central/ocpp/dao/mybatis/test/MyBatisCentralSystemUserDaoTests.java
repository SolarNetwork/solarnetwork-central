/* ==================================================================
 * MyBatisSystemUserDaoTests.java - 25/02/2020 10:02:23 am
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

package net.solarnetwork.central.ocpp.dao.mybatis.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.fail;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.dao.DuplicateKeyException;
import net.solarnetwork.central.ocpp.dao.mybatis.MyBatisCentralSystemUserDao;
import net.solarnetwork.central.ocpp.domain.CentralSystemUser;
import net.solarnetwork.dao.GenericDao;
import net.solarnetwork.ocpp.domain.SystemUser;

/**
 * Test cases for the {@link MyBatisCentralSystemUserDao} class.
 * 
 * @author matt
 * @version 2.0
 */
public class MyBatisCentralSystemUserDaoTests extends AbstractMyBatisDaoTestSupport {

	private MyBatisCentralSystemUserDao dao;

	private Long userId;
	private SystemUser last;

	@Before
	public void setUp() throws Exception {
		dao = new MyBatisCentralSystemUserDao();
		dao.setSqlSessionTemplate(getSqlSessionTemplate());
		last = null;
		userId = UUID.randomUUID().getMostSignificantBits();
		setupTestUser(userId);
	}

	private CentralSystemUser createTestSystemUser() {
		CentralSystemUser user = new CentralSystemUser(null, userId,
				Instant.ofEpochMilli(System.currentTimeMillis()));
		user.setUsername("foobar");
		user.setPassword("secret");
		return user;
	}

	@Test
	public void insert() {
		CentralSystemUser entity = createTestSystemUser();
		Long pk = dao.save(entity);
		assertThat("PK generated", pk, notNullValue());
		last = new CentralSystemUser(pk, entity.getUserId(), entity.getCreated());
		last.setUsername(entity.getUsername());
		last.setPassword(entity.getPassword());
		last.setAllowedChargePoints(entity.getAllowedChargePoints());
	}

	@Test(expected = DuplicateKeyException.class)
	public void insert_duplicate() {
		insert();
		SystemUser entity = createTestSystemUser();
		dao.save(entity);
		getSqlSessionTemplate().flushStatements();
		fail("Should not be able to create duplicate.");
	}

	@Test
	public void insert_withAllowedChargePoints() {
		SystemUser entity = createTestSystemUser();
		Set<String> allowedChargePoints = new LinkedHashSet<>();
		allowedChargePoints.add("one");
		allowedChargePoints.add("two");
		allowedChargePoints.add("three");
		entity.setAllowedChargePoints(allowedChargePoints);
		Long pk = dao.save(entity);
		assertThat("PK generated", pk, notNullValue());
		last = new SystemUser(pk, entity.getCreated());
		last.setUsername(entity.getUsername());
		last.setPassword(entity.getPassword());
		last.setAllowedChargePoints(entity.getAllowedChargePoints());
	}

	@Test
	public void getByPK() {
		insert();
		SystemUser entity = dao.get(last.getId());

		assertThat("ID", entity.getId(), equalTo(last.getId()));
		assertThat("Created", entity.getCreated(), equalTo(last.getCreated()));
		assertThat("Username", entity.getUsername(), equalTo(last.getUsername()));
		assertThat("Password is NOT returned", entity.getPassword(), nullValue());
	}

	@Test
	public void getByPK_withAllowedChargePoints() {
		insert_withAllowedChargePoints();
		SystemUser entity = dao.get(last.getId());

		assertThat("ID", entity.getId(), equalTo(last.getId()));
		assertThat("Allowed charge points", entity.getAllowedChargePoints(),
				contains("one", "two", "three"));
	}

	@Test
	public void update() {
		insert();
		SystemUser obj = dao.get(last.getId());
		obj.setUsername("new-username");
		obj.setPassword("new-password");
		Long pk = dao.save(obj);
		assertThat("PK unchanged", pk, equalTo(obj.getId()));

		SystemUser entity = dao.getForUsername(obj.getUsername());
		assertThat("Entity updated", entity.isSameAs(obj), equalTo(true));
	}

	@Test
	public void update_withAllowedChargePoints() {
		insert_withAllowedChargePoints();
		SystemUser obj = dao.get(last.getId());
		obj.setUsername("new-username");
		obj.setPassword("new-password");
		obj.getAllowedChargePoints().remove("two");
		obj.getAllowedChargePoints().add("four");
		obj.getAllowedChargePoints().add("five");
		Long pk = dao.save(obj);
		assertThat("PK unchanged", pk, equalTo(obj.getId()));

		SystemUser entity = dao.getForUsername(obj.getUsername());
		assertThat("Entity updated", entity.isSameAs(obj), equalTo(true));
		assertThat("Allowed charge points", entity.getAllowedChargePoints(),
				contains("one", "three", "four", "five"));
	}

	@Test
	public void findAll() {
		SystemUser obj1 = createTestSystemUser();
		obj1 = dao.get(dao.save(obj1));
		SystemUser obj2 = new CentralSystemUser(userId, obj1.getCreated().minusSeconds(60), "b", "bb");
		obj2 = dao.get(dao.save(obj2));
		SystemUser obj3 = new CentralSystemUser(userId, obj1.getCreated().plusSeconds(60), "c", "cc");
		obj3 = dao.get(dao.save(obj3));

		Collection<SystemUser> results = dao.getAll(null);
		assertThat("Results found in order", results, contains(obj2, obj1, obj3));
		assertThat("No password returned",
				results.stream().map(SystemUser::getPassword).allMatch(s -> s == null), equalTo(true));
	}

	@Test
	public void findAllForOwner() {
		SystemUser obj1 = createTestSystemUser();
		obj1 = dao.get(dao.save(obj1));
		SystemUser obj2 = new CentralSystemUser(userId, obj1.getCreated().minusSeconds(60), "b", "bb");
		obj2 = dao.get(dao.save(obj2));

		Long userId2 = this.userId - 1;
		setupTestUser(userId2);

		SystemUser obj3 = new CentralSystemUser(userId2, obj1.getCreated().plusSeconds(60), "c", "cc");
		obj3 = dao.get(dao.save(obj3));

		Collection<CentralSystemUser> results = dao.findAllForOwner(userId);
		assertThat("Results found in order", results, contains(obj2, obj1));
		assertThat("No password returned",
				results.stream().map(SystemUser::getPassword).allMatch(s -> s == null), equalTo(true));
	}

	@Test
	public void findAll_sortByCreatedDesc() {
		SystemUser obj1 = createTestSystemUser();
		obj1 = dao.get(dao.save(obj1));
		SystemUser obj2 = new CentralSystemUser(userId, obj1.getCreated().minusSeconds(60), "b", "bb");
		obj2 = dao.get(dao.save(obj2));
		SystemUser obj3 = new CentralSystemUser(userId, obj1.getCreated().plusSeconds(60), "c", "cc");
		obj3 = dao.get(dao.save(obj3));

		Collection<SystemUser> results = dao.getAll(GenericDao.SORT_BY_CREATED_DESCENDING);
		assertThat("Results found in order", results, contains(obj3, obj1, obj2));
	}

	@Test
	public void findAll_withAllowedChargePoints() {
		SystemUser obj1 = createTestSystemUser();
		obj1.setAllowedChargePoints(new LinkedHashSet<>(Arrays.asList("one", "two")));
		obj1 = dao.get(dao.save(obj1));
		SystemUser obj2 = new CentralSystemUser(userId, obj1.getCreated().minusSeconds(60), "b", "bb");
		obj2 = dao.get(dao.save(obj2));
		SystemUser obj3 = new CentralSystemUser(userId, obj1.getCreated().plusSeconds(60), "c", "cc");
		obj3.setAllowedChargePoints(new LinkedHashSet<>(Arrays.asList("three")));
		obj3 = dao.get(dao.save(obj3));

		List<SystemUser> results = dao.getAll(null).stream().collect(Collectors.toList());
		assertThat("Results found in order", results, contains(obj2, obj1, obj3));

		assertThat("Result 0 same", results.get(0).isSameAs(obj2), equalTo(true));
		assertThat("Result 1 same", results.get(1).isSameAs(obj1), equalTo(true));
		assertThat("Result 2 same", results.get(2).isSameAs(obj3), equalTo(true));
	}

	@Test
	public void findByUsername_none() {
		SystemUser entity = dao.getForUsername("foo");
		assertThat("No users", entity, nullValue());
	}

	@Test
	public void findByUsername_noMatch() {
		insert();
		SystemUser entity = dao.getForUsername("not a match");
		assertThat("No match", entity, nullValue());
	}

	@Test
	public void findByUsername() {
		findAll();
		SystemUser entity = dao.getForUsername("b");
		assertThat("Match", entity, notNullValue());
		assertThat("Username matches", entity.getUsername(), equalTo("b"));
		assertThat("Password IS returned", entity.getPassword(), equalTo("bb"));
	}

	@Test
	public void findByUsername_withAllowedChargePoints() {
		findAll_withAllowedChargePoints();
		SystemUser entity = dao.getForUsername("foobar");
		assertThat("Match", entity, notNullValue());
		assertThat("Username matches", entity.getUsername(), equalTo("foobar"));
		assertThat("Password IS returned", entity.getPassword(), equalTo("secret"));
		assertThat("Allowed charge points", entity.getAllowedChargePoints(), contains("one", "two"));
	}

	@Test
	public void findByUserAndUsername() {
		findAll();
		SystemUser entity = dao.getForUsername(userId, "b");
		assertThat("Match", entity, notNullValue());
		assertThat("Username matches", entity.getUsername(), equalTo("b"));
		assertThat("Password IS NOT returned", entity.getPassword(), nullValue());
	}

	@Test(expected = DataRetrievalFailureException.class)
	public void findByUserAndUsername_noMatch() {
		insert();
		dao.getForUsername(userId, "not a match");
	}

	@Test
	public void findByUserAndId() {
		insert();
		SystemUser entity = dao.get(userId, last.getId());
		assertThat("Match", entity, notNullValue());
		assertThat("Username matches", entity.getUsername(), equalTo("foobar"));
		assertThat("Password IS NOT returned", entity.getPassword(), nullValue());
	}

	@Test(expected = DataRetrievalFailureException.class)
	public void findByUserAndId_noMatch() {
		insert();
		dao.get(userId, last.getId() - 1);
	}

	@Test
	public void deleteByUserAndId() {
		insert();
		dao.delete(userId, last.getId());
		assertThat("No longer found", dao.get(last.getId()), nullValue());
	}

	@Test(expected = DataRetrievalFailureException.class)
	public void deleteByUserAndId_noMatch() {
		insert();
		dao.delete(userId, last.getId() - 1);
	}

}
