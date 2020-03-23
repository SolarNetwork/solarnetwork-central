/* ==================================================================
 * MyBatisUserSettingsDaoTests.java - 27/02/2020 4:47:12 pm
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
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.springframework.dao.DataRetrievalFailureException;
import net.solarnetwork.central.ocpp.dao.mybatis.MyBatisUserSettingsDao;
import net.solarnetwork.central.ocpp.domain.UserSettings;

/**
 * Test cases for the {@link MyBatisUserSettingsDao}.
 * 
 * @author matt
 * @version 1.0
 */
public class MyBatisUserSettingsDaoTests extends AbstractMyBatisDaoTestSupport {

	private MyBatisUserSettingsDao dao;

	private Long userId;
	private Long nodeId;
	private UserSettings last;

	@Before
	public void setUp() throws Exception {
		dao = new MyBatisUserSettingsDao();
		dao.setSqlSessionTemplate(getSqlSessionTemplate());
		last = null;
		UUID uuid = UUID.randomUUID();
		userId = uuid.getMostSignificantBits();
		nodeId = uuid.getLeastSignificantBits();
		setupTestUser(userId);
		setupTestLocation();
		setupTestNode(nodeId);
		setupTestUserNode(userId, nodeId);
	}

	private UserSettings createTestUserSettings() {
		UserSettings s = new UserSettings(userId, Instant.ofEpochMilli(System.currentTimeMillis()));
		return s;
	}

	@Test
	public void insert() {
		UserSettings entity = createTestUserSettings();
		Long pk = dao.save(entity);
		assertThat("PK preserved", pk, equalTo(entity.getId()));
		last = entity;
	}

	@Test
	public void insert_duplicate() {
		insert();
		UserSettings entity = createTestUserSettings();
		dao.save(entity);
		getSqlSessionTemplate().flushStatements();
	}

	@Test
	public void getByPK() {
		insert();
		UserSettings entity = dao.get(last.getId());

		assertThat("ID", entity.getId(), equalTo(last.getId()));
		assertThat("Created", entity.getCreated(), equalTo(last.getCreated()));
		assertThat("Source ID template", entity.getSourceIdTemplate(),
				equalTo(last.getSourceIdTemplate()));
	}

	@Test
	public void update() {
		insert();
		UserSettings obj = dao.get(last.getId());
		obj.setSourceIdTemplate("new-template");
		Long pk = dao.save(obj);
		assertThat("PK unchanged", pk, equalTo(obj.getId()));

		UserSettings entity = dao.get(pk);
		assertThat("Entity updated", entity.isSameAs(obj), equalTo(true));
	}

	@Test
	public void findAll() {
		UUID uuid = UUID.randomUUID();
		Long userId2 = uuid.getMostSignificantBits();
		Long nodeId2 = uuid.getLeastSignificantBits();
		setupTestUser(userId2);
		setupTestNode(nodeId2);
		setupTestUserNode(userId2, nodeId2);

		UserSettings obj1 = createTestUserSettings();
		obj1 = dao.get(dao.save(obj1));
		UserSettings obj2 = new UserSettings(userId2, obj1.getCreated().minusSeconds(60));
		obj2 = dao.get(dao.save(obj2));

		Collection<UserSettings> results = dao.getAll(null);
		List<UserSettings> expected = Arrays.asList(obj1, obj2);
		expected.sort(new Comparator<UserSettings>() {

			@Override
			public int compare(UserSettings o1, UserSettings o2) {
				return o1.compareTo(o2.getId());
			}
		});
		assertThat("Results found in order", results, contains(expected.toArray()));
	}

	@Test
	public void deleteByUserId() {
		insert();
		dao.delete(userId);
		assertThat("No longer found", dao.get(last.getId()), nullValue());
	}

	@Test(expected = DataRetrievalFailureException.class)
	public void deleteByUserId_noMatch() {
		insert();
		dao.delete(userId - 1);
	}

}
