/* ==================================================================
 * MyBatisAuthorizationDaoTests.java - 25/02/2020 10:02:23 am
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

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import java.time.Instant;
import java.util.Collection;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.springframework.dao.DuplicateKeyException;
import net.solarnetwork.central.ocpp.dao.mybatis.MyBatisCentralAuthorizationDao;
import net.solarnetwork.central.ocpp.domain.CentralAuthorization;
import net.solarnetwork.dao.GenericDao;
import net.solarnetwork.ocpp.domain.Authorization;

/**
 * Test cases for the {@link MyBatisCentralAuthorizationDao} class.
 * 
 * @author matt
 * @version 1.0
 */
public class MyBatisCentralAuthorizationDaoTests extends AbstractMyBatisDaoTestSupport {

	private MyBatisCentralAuthorizationDao dao;

	private Long userId;
	private Authorization last;

	@Before
	public void setUp() throws Exception {
		dao = new MyBatisCentralAuthorizationDao();
		dao.setSqlSessionTemplate(getSqlSessionTemplate());
		last = null;
		userId = UUID.randomUUID().getMostSignificantBits();
		setupTestUser(userId);
	}

	private CentralAuthorization createTestAuthorization() {
		CentralAuthorization entity = new CentralAuthorization(null, userId,
				Instant.ofEpochMilli(System.currentTimeMillis()));
		entity.setToken("foobar");
		entity.setEnabled(true);
		entity.setExpiryDate(entity.getCreated().plusSeconds(600));
		entity.setParentId(UUID.randomUUID().toString().substring(0, 20));
		return entity;
	}

	@Test
	public void insert() {
		CentralAuthorization entity = createTestAuthorization();
		Long pk = dao.save(entity);
		assertThat("PK generated", pk, notNullValue());
		last = new CentralAuthorization(pk, entity.getUserId(), entity.getCreated());
		last.setToken(entity.getToken());
		last.setEnabled(entity.isEnabled());
		last.setExpiryDate(entity.getExpiryDate());
		last.setParentId(entity.getParentId());
	}

	@Test(expected = DuplicateKeyException.class)
	public void insert_duplicate() {
		insert();
		Authorization entity = createTestAuthorization();
		dao.save(entity);
		getSqlSessionTemplate().flushStatements();
		fail("Should not be able to create duplicate.");
	}

	@Test
	public void getByPK() {
		insert();
		Authorization entity = dao.get(last.getId());

		assertThat("ID", entity.getId(), equalTo(last.getId()));
		assertThat("Created", entity.getCreated(), equalTo(last.getCreated()));
		assertThat("Username", entity.getToken(), equalTo(last.getToken()));
		assertThat("Password", entity.isEnabled(), equalTo(last.isEnabled()));
		assertThat("Username", entity.getExpiryDate(), equalTo(last.getExpiryDate()));
		assertThat("Username", entity.getParentId(), equalTo(last.getParentId()));
	}

	@Test
	public void update() {
		insert();
		Authorization obj = dao.get(last.getId());
		obj.setToken("new-username");
		obj.setEnabled(false);
		obj.setExpiryDate(obj.getExpiryDate().plusSeconds(60));
		obj.setParentId(null);
		Long pk = dao.save(obj);
		assertThat("PK unchanged", pk, equalTo(obj.getId()));

		Authorization entity = dao.get(pk);
		assertThat("Entity updated", entity.isSameAs(obj), equalTo(true));
	}

	@Test
	public void findAll() {
		Authorization obj1 = createTestAuthorization();
		obj1 = dao.get(dao.save(obj1));
		Authorization obj2 = new CentralAuthorization(userId, obj1.getCreated().minusSeconds(60), "b");
		obj2 = dao.get(dao.save(obj2));
		Authorization obj3 = new CentralAuthorization(userId, obj1.getCreated().plusSeconds(60), "c");
		obj3 = dao.get(dao.save(obj3));

		Collection<Authorization> results = dao.getAll(null);
		assertThat("Results found in order", results, contains(obj2, obj1, obj3));
	}

	@Test
	public void findAll_sortByCreatedDesc() {
		Authorization obj1 = createTestAuthorization();
		obj1 = dao.get(dao.save(obj1));
		Authorization obj2 = new CentralAuthorization(userId, obj1.getCreated().minusSeconds(60), "b");
		obj2 = dao.get(dao.save(obj2));
		Authorization obj3 = new CentralAuthorization(userId, obj1.getCreated().plusSeconds(60), "c");
		obj3 = dao.get(dao.save(obj3));

		Collection<Authorization> results = dao.getAll(GenericDao.SORT_BY_CREATED_DESCENDING);
		assertThat("Results found in order", results, contains(obj3, obj1, obj2));
	}

	@Test
	public void findByToken_none() {
		Authorization entity = dao.getForToken(userId, "foo");
		assertThat("No users", entity, nullValue());
	}

	@Test
	public void findByToken_noMatch() {
		insert();
		Authorization entity = dao.getForToken(userId, "not a match");
		assertThat("No match", entity, nullValue());
	}

	@Test
	public void findByToken() {
		findAll();
		Authorization entity = dao.getForToken(userId, "b");
		assertThat("Match", entity, notNullValue());
		assertThat("Username matches", entity.getToken(), equalTo("b"));
	}

}
