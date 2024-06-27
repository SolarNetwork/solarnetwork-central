/* ==================================================================
 * JdbcUserFluxDefaultAggregatePublishConfigurationDaoTests.java - 25/06/2024 12:50:11 pm
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

package net.solarnetwork.central.user.flux.dao.jdbc.test;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;
import static org.assertj.core.api.InstanceOfAssertFactories.list;
import static org.assertj.core.api.InstanceOfAssertFactories.map;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.test.AbstractJUnit5JdbcDaoTestSupport;
import net.solarnetwork.central.test.CommonDbTestUtils;
import net.solarnetwork.central.user.flux.dao.jdbc.JdbcUserFluxDefaultAggregatePublishConfigurationDao;
import net.solarnetwork.central.user.flux.domain.UserFluxDefaultAggregatePublishConfiguration;

/**
 * Test cases for the
 * {@link JdbcUserFluxDefaultAggregatePublishConfigurationDao} class.
 * 
 * @author matt
 * @version 1.0
 */
public class JdbcUserFluxDefaultAggregatePublishConfigurationDaoTests
		extends AbstractJUnit5JdbcDaoTestSupport {

	private JdbcUserFluxDefaultAggregatePublishConfigurationDao dao;
	private Long userId;

	private UserFluxDefaultAggregatePublishConfiguration last;

	@BeforeEach
	public void setup() {
		dao = new JdbcUserFluxDefaultAggregatePublishConfigurationDao(jdbcTemplate);
		userId = CommonDbTestUtils.insertUser(jdbcTemplate);
	}

	private List<Map<String, Object>> allUserFluxDefaultAggregatePublishConfigurationTable() {
		return CommonDbTestUtils.allTableData(log, jdbcTemplate,
				"solaruser.user_flux_default_agg_pub_settings", "user_id");
	}

	private static UserFluxDefaultAggregatePublishConfiguration newUserFluxDefaultAggregatePublishConfiguration(
			Long userId, boolean publish, boolean retain) {
		UserFluxDefaultAggregatePublishConfiguration conf = new UserFluxDefaultAggregatePublishConfiguration(
				userId, Instant.now());
		conf.setModified(conf.getCreated());
		conf.setPublish(publish);
		conf.setRetain(retain);
		return conf;
	}

	@Test
	public void insert() {
		// GIVEN
		UserFluxDefaultAggregatePublishConfiguration conf = newUserFluxDefaultAggregatePublishConfiguration(
				userId, true, true);

		// WHEN
		Long result = dao.save(conf);

		// THEN

		// @formatter:off
		then(result).as("Primary key")
			.isEqualTo(userId)
			;

		List<Map<String, Object>> data = allUserFluxDefaultAggregatePublishConfigurationTable();
		then(data).as("Table has 1 row").hasSize(1).asInstanceOf(list(Map.class))
			.element(0, map(String.class, Object.class))
			.as("Row user ID")
			.containsEntry("user_id", userId)
			.as("Row creation date")
			.containsEntry("created", Timestamp.from(conf.getCreated()))
			.as("Row modification date")
			.containsEntry("modified", Timestamp.from(conf.getModified()))
			.as("No row enabled column")
			.doesNotContainKey("enabled")
			.as("Publish flag")
			.containsEntry("publish", true)
			.as("Retain flag")
			.containsEntry("retain", true)
			;
		// @formatter:on
		last = conf;
	}

	@Test
	public void get() {
		// GIVEN
		insert();

		allUserFluxDefaultAggregatePublishConfigurationTable();

		// WHEN
		UserFluxDefaultAggregatePublishConfiguration result = dao.get(last.getId());

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
	public void delete() {
		// GIVEN
		insert();

		// WHEN
		dao.delete(last);

		// THEN
		var data = allUserFluxDefaultAggregatePublishConfigurationTable();
		then(data).as("Row deleted").isEmpty();
	}

	@Test
	public void getAll() {
		thenThrownBy(() -> {
			dao.getAll(null);
		}).as("Not supported").isInstanceOf(UnsupportedOperationException.class);
	}

}
