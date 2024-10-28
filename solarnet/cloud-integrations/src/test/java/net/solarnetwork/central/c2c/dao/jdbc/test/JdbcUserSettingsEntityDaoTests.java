/* ==================================================================
 * JdbcUserSettingsEntityDaoTests.java - 28/10/2024 10:23:41 am
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

package net.solarnetwork.central.c2c.dao.jdbc.test;

import static net.solarnetwork.central.c2c.dao.jdbc.test.CinJdbcTestUtils.allUserSettingsEntityData;
import static net.solarnetwork.central.c2c.dao.jdbc.test.CinJdbcTestUtils.newUserSettingsEntity;
import static net.solarnetwork.central.test.CommonTestUtils.randomBoolean;
import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.InstanceOfAssertFactories.list;
import static org.assertj.core.api.InstanceOfAssertFactories.map;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.c2c.dao.jdbc.JdbcUserSettingsEntityDao;
import net.solarnetwork.central.c2c.domain.UserSettingsEntity;
import net.solarnetwork.central.test.AbstractJUnit5JdbcDaoTestSupport;
import net.solarnetwork.central.test.CommonDbTestUtils;

/**
 * Test cases for the {@link JdbcUserSettingsEntityDao} class.
 *
 * @author matt
 * @version 1.0
 */
public class JdbcUserSettingsEntityDaoTests extends AbstractJUnit5JdbcDaoTestSupport {

	private JdbcUserSettingsEntityDao dao;
	private Long userId;

	private UserSettingsEntity last;

	@BeforeEach
	public void setup() {
		dao = new JdbcUserSettingsEntityDao(jdbcTemplate);
		userId = CommonDbTestUtils.insertUser(jdbcTemplate);
	}

	@Test
	public void insert() {
		// GIVEN
		UserSettingsEntity conf = newUserSettingsEntity(userId, randomBoolean(), randomBoolean());

		// WHEN
		Long result = dao.store(conf);

		// THEN

		// @formatter:off
		then(result).as("Primary key")
		.as("User ID as provided")
			.isEqualTo(userId)
			;

		List<Map<String, Object>> data = allUserSettingsEntityData(jdbcTemplate);
		then(data).as("Table has 1 row").hasSize(1).asInstanceOf(list(Map.class))
			.element(0, map(String.class, Object.class))
			.as("Row user ID")
			.containsEntry("user_id", userId)
			.as("Row creation date")
			.containsEntry("created", Timestamp.from(conf.getCreated()))
			.as("Row modification date")
			.containsEntry("modified", Timestamp.from(conf.getModified()))
			.as("Row publish SolarIn")
			.containsEntry("pub_in", conf.isPublishToSolarIn())
			.as("Row publish SolarFlux")
			.containsEntry("pub_flux", conf.isPublishToSolarFlux())
			;
		// @formatter:on
		last = conf;
	}

	@Test
	public void get() {
		// GIVEN
		insert();

		// WHEN
		UserSettingsEntity result = dao.get(last.getId());

		// THEN
		then(result).as("Retrieved entity matches source").isEqualTo(last);
	}

	@Test
	public void update() {
		// GIVEN
		insert();

		// WHEN
		UserSettingsEntity conf = last.copyWithId(last.getId());
		conf.setModified(Instant.now().plusMillis(474));
		conf.setPublishToSolarIn(!conf.isPublishToSolarIn());
		conf.setPublishToSolarFlux(!conf.isPublishToSolarFlux());

		Long result = dao.store(conf);
		UserSettingsEntity updated = dao.get(result);

		// THEN
		List<Map<String, Object>> data = allUserSettingsEntityData(jdbcTemplate);
		then(data).as("Table has 1 row").hasSize(1);
		// @formatter:off
		then(updated).as("Retrieved entity matches updated source")
			.isEqualTo(conf)
			.as("Entity saved updated values")
			.matches(c -> c.isSameAs(updated));
		// @formatter:on
	}

	@Test
	public void delete() {
		// GIVEN
		insert();

		// WHEN
		dao.delete(last);

		// THEN
		List<Map<String, Object>> data = allUserSettingsEntityData(jdbcTemplate);
		then(data).as("Row deleted from db").isEmpty();
	}

}
