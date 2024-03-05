/* ==================================================================
 * JdbcInputDataEntityDaoTests.java - 5/03/2024 11:17:25 am
 *
 * Copyright 2024 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.din.dao.jdbc.test;

import static net.solarnetwork.central.din.dao.jdbc.test.DinJdbcTestUtils.allInputDataEntityData;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.InstanceOfAssertFactories.map;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.din.dao.jdbc.JdbcInputDataEntityDao;
import net.solarnetwork.central.din.domain.InputDataEntity;
import net.solarnetwork.central.domain.UserLongStringCompositePK;
import net.solarnetwork.central.test.AbstractJUnit5JdbcDaoTestSupport;
import net.solarnetwork.central.test.CommonDbTestUtils;

/**
 * Test cases for the {@link JdbcInputDataDao} class.
 *
 * @author matt
 * @version 1.0
 */
public class JdbcInputDataEntityDaoTests extends AbstractJUnit5JdbcDaoTestSupport {

	private JdbcInputDataEntityDao dao;
	private Long userId;

	private InputDataEntity last;

	@BeforeEach
	public void setup() {
		dao = new JdbcInputDataEntityDao(jdbcTemplate);
		userId = CommonDbTestUtils.insertUser(jdbcTemplate);
	}

	@Test
	public void insert() {
		// GIVEN
		InputDataEntity entity = new InputDataEntity(userId, randomLong(), randomString(), Instant.now(),
				new byte[] { 1, 2, 3 });

		// WHEN
		UserLongStringCompositePK result = dao.save(entity);

		// THEN

		// @formatter:off
		then(result).as("Primary key")
			.isNotNull()
			.as("User ID as provided")
			.returns(userId, from(UserLongStringCompositePK::getUserId))
			.as("Node ID as provided")
			.returns(entity.getNodeId(), UserLongStringCompositePK::getGroupId)
			.as("Source ID as provided")
			.returns(entity.getSourceId(), UserLongStringCompositePK::getEntityId)
			;

		List<Map<String, Object>> data = allInputDataEntityData(jdbcTemplate);
		then(data).as("Table has 1 row").hasSize(1).asList().element(0, map(String.class, Object.class))
			.as("Row user ID")
			.containsEntry("user_id", userId)
			.as("Row node ID")
			.containsEntry("node_id", entity.getNodeId())
			.as("Row source ID")
			.containsEntry("source_id", entity.getSourceId())
			.as("Row creation date")
			.containsEntry("created", Timestamp.from(entity.getCreated()))
			.as("Row data")
			.hasEntrySatisfying("input_data", rowData -> {
				then(rowData).isInstanceOf(byte[].class);
				then((byte[])rowData)
					.as("Row byte data")
					.containsExactly(1,2,3)
					;
			})
			;
		// @formatter:on
		last = entity;
	}

	@Test
	public void get() {
		// GIVEN
		insert();

		// WHEN
		InputDataEntity result = dao.get(last.getId());

		// THEN
		// @formatter:off
		then(result)
			.as("Retrieved entity matches source")
			.isEqualTo(last)
			.as("Entity values retrieved")
			.matches(c -> c.isSameAs(last))
			;
		// @formatter:on
	}

	@Test
	public void update() {
		// GIVEN
		insert();

		// WHEN
		InputDataEntity conf = new InputDataEntity(last.getId(), Instant.now().plusSeconds(1),
				new byte[] { 2, 3, 4 });

		UserLongStringCompositePK result = dao.save(conf);
		InputDataEntity updated = dao.get(result);

		// THEN
		List<Map<String, Object>> data = allInputDataEntityData(jdbcTemplate);
		then(data).as("Table has 1 row").hasSize(1);
		// @formatter:off
		then(updated).as("Retrieved entity matches updated source")
			.isEqualTo(conf)
			.as("Entity saved updated values")
			.matches(c -> c.isSameAs(updated))
			.as("Creation date updated")
			.returns(conf.getCreated(), from(InputDataEntity::getCreated))
			;
		// @formatter:on
	}

	@Test
	public void delete() {
		// GIVEN
		insert();

		// WHEN
		dao.delete(last);

		// THEN
		List<Map<String, Object>> data = allInputDataEntityData(jdbcTemplate);
		then(data).as("Row deleted from db").isEmpty();
	}

	@Test
	public void update_data() {
		// GIVEN
		insert();

		// WHEN
		byte[] newData = new byte[] { 2, 3, 4 };

		byte[] prevData = dao.getAndPut(last.getId(), newData);

		// THEN
		// @formatter:off
		then(prevData)
			.as("Previous data returned")
			.containsExactly(1, 2, 3)
			;

		List<Map<String, Object>> data = allInputDataEntityData(jdbcTemplate);
		then(data).as("Table has 1 row").hasSize(1).asList().element(0, map(String.class, Object.class))
			.as("Row user ID")
			.containsEntry("user_id", userId)
			.as("Row node ID")
			.containsEntry("node_id", last.getNodeId())
			.as("Row source ID")
			.containsEntry("source_id", last.getSourceId())
			.as("Row data")
			.hasEntrySatisfying("input_data", rowData -> {
				then(rowData).isInstanceOf(byte[].class);
				then((byte[])rowData)
					.as("Row byte data")
					.containsExactly(2, 3, 4)
					;
			})
			;
		// @formatter:on

	}

	@Test
	public void update_data_noPrevious() {
		// GIVEN
		InputDataEntity entity = new InputDataEntity(userId, randomLong(), randomString(), Instant.now(),
				new byte[] { 1, 2, 3 });

		// WHEN
		byte[] prevData = dao.getAndPut(entity.getId(), entity.getData());

		// THEN
		// @formatter:off
		then(prevData)
			.as("Previous data not avaialble")
			.isNull();
			;

		List<Map<String, Object>> data = allInputDataEntityData(jdbcTemplate);
		then(data).as("Table has 1 row").hasSize(1).asList().element(0, map(String.class, Object.class))
			.as("Row user ID")
			.containsEntry("user_id", userId)
			.as("Row node ID")
			.containsEntry("node_id", entity.getNodeId())
			.as("Row source ID")
			.containsEntry("source_id", entity.getSourceId())
			.as("Row data")
			.hasEntrySatisfying("input_data", rowData -> {
				then(rowData).isInstanceOf(byte[].class);
				then((byte[])rowData)
					.as("Row byte data")
					.containsExactly(1, 2, 3)
					;
			})
			;
		// @formatter:on

	}

}
