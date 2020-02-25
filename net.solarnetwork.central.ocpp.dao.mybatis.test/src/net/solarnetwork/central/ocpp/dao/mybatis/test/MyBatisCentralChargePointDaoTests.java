/* ==================================================================
 * MyBatisChargePointDaoTests.java - 25/02/2020 10:02:23 am
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
import net.solarnetwork.central.ocpp.dao.mybatis.MyBatisCentralChargePointDao;
import net.solarnetwork.central.ocpp.domain.CentralChargePoint;
import net.solarnetwork.dao.GenericDao;
import net.solarnetwork.ocpp.domain.ChargePoint;
import net.solarnetwork.ocpp.domain.ChargePointInfo;
import net.solarnetwork.ocpp.domain.RegistrationStatus;

/**
 * Test cases for the {@link MyBatisCentralChargePointDao} class.
 * 
 * @author matt
 * @version 1.0
 */
public class MyBatisCentralChargePointDaoTests extends AbstractMyBatisDaoTestSupport {

	private MyBatisCentralChargePointDao dao;

	private Long userId;
	private ChargePoint last;

	@Before
	public void setUp() throws Exception {
		dao = new MyBatisCentralChargePointDao();
		dao.setSqlSessionFactory(getSqlSessionFactory());
		last = null;
		userId = UUID.randomUUID().getMostSignificantBits();
		setupTestUser(userId);
	}

	private CentralChargePoint createTestChargePoint() {
		CentralChargePoint entity = new CentralChargePoint(null, userId,
				Instant.ofEpochMilli(System.currentTimeMillis()),
				new ChargePointInfo("foobar", "foo", "bar"));
		entity.setEnabled(true);
		entity.setRegistrationStatus(RegistrationStatus.Accepted);
		entity.setConnectorCount(1);
		return entity;
	}

	@Test
	public void insert() {
		CentralChargePoint entity = createTestChargePoint();
		Long pk = dao.save(entity);
		assertThat("PK generated", pk, notNullValue());
		last = new CentralChargePoint(pk, entity.getUserId(), entity.getCreated(), entity.getInfo());
		last.setEnabled(entity.isEnabled());
		last.setRegistrationStatus(entity.getRegistrationStatus());
		last.setConnectorCount(entity.getConnectorCount());
	}

	@Test(expected = DuplicateKeyException.class)
	public void insert_duplicate() {
		insert();
		ChargePoint entity = createTestChargePoint();
		dao.save(entity);
		fail("Should not be able to create duplicate.");
	}

	@Test
	public void getByPK() {
		insert();
		ChargePoint entity = dao.get(last.getId());

		assertThat("ID", entity.getId(), equalTo(last.getId()));
		assertThat("Created", entity.getCreated(), equalTo(last.getCreated()));
		assertThat("Enabled", entity.isEnabled(), equalTo(last.isEnabled()));
		assertThat("Status", entity.getRegistrationStatus(), equalTo(last.getRegistrationStatus()));
		assertThat("Connector count", entity.getConnectorCount(), equalTo(last.getConnectorCount()));
		assertThat("Info", entity.getInfo().isSameAs(last.getInfo()), equalTo(true));
	}

	@Test
	public void update() {
		insert();
		ChargePoint obj = dao.get(last.getId());
		obj.setEnabled(false);
		obj.setRegistrationStatus(RegistrationStatus.Rejected);
		obj.getInfo().setId("new id");
		obj.getInfo().setIccid("icky");
		Long pk = dao.save(obj);
		assertThat("PK unchanged", pk, equalTo(obj.getId()));

		ChargePoint entity = dao.get(pk);
		assertThat("Entity updated", entity.isSameAs(obj), equalTo(true));
	}

	@Test
	public void findAll() {
		ChargePoint obj1 = createTestChargePoint();
		obj1 = dao.get(dao.save(obj1));
		ChargePoint obj2 = new CentralChargePoint(userId, obj1.getCreated().minusSeconds(60), "b", "foo",
				"bar");
		obj2 = dao.get(dao.save(obj2));
		ChargePoint obj3 = new CentralChargePoint(userId, obj1.getCreated().plusSeconds(60), "c", "foo",
				"bar");
		obj3 = dao.get(dao.save(obj3));

		Collection<ChargePoint> results = dao.getAll(null);
		assertThat("Results found in order", results, contains(obj2, obj1, obj3));
	}

	@Test
	public void findAll_sortByCreatedDesc() {
		ChargePoint obj1 = createTestChargePoint();
		obj1 = dao.get(dao.save(obj1));
		ChargePoint obj2 = new CentralChargePoint(userId, obj1.getCreated().minusSeconds(60), "b", "foo",
				"bar");
		obj2 = dao.get(dao.save(obj2));
		ChargePoint obj3 = new CentralChargePoint(userId, obj1.getCreated().plusSeconds(60), "c", "foo",
				"bar");
		obj3 = dao.get(dao.save(obj3));

		Collection<ChargePoint> results = dao.getAll(GenericDao.SORT_BY_CREATED_DESCENDING);
		assertThat("Results found in order", results, contains(obj3, obj1, obj2));
	}

	@Test
	public void findByToken_none() {
		ChargePoint entity = dao.getForIdentifier(userId, "foo");
		assertThat("No users", entity, nullValue());
	}

	@Test
	public void findByToken_noMatch() {
		insert();
		ChargePoint entity = dao.getForIdentifier(userId, "not a match");
		assertThat("No match", entity, nullValue());
	}

	@Test
	public void findByToken() {
		findAll();
		ChargePoint entity = dao.getForIdentifier(userId, "b");
		assertThat("Match", entity, notNullValue());
		assertThat("Username matches", entity.getInfo().getId(), equalTo("b"));
	}
}
