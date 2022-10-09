/* ==================================================================
 * JdbcUserSettingsDaoTests.java - 10/10/2022 10:34:48 am
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.oscp.dao.jdbc.test;

import static net.solarnetwork.central.oscp.dao.jdbc.test.OscpJdbcTestUtils.allUserSettingsData;
import static net.solarnetwork.central.oscp.dao.jdbc.test.OscpJdbcTestUtils.newUserSettings;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.oscp.dao.jdbc.JdbcUserSettingsDao;
import net.solarnetwork.central.oscp.domain.UserSettings;
import net.solarnetwork.central.test.AbstractJUnit5JdbcDaoTestSupport;
import net.solarnetwork.central.test.CommonDbTestUtils;

/**
 * Test cases for the {@link JdbcUserSettingsDao} class.
 * 
 * @author matt
 * @version 1.0
 */
public class JdbcUserSettingsDaoTests extends AbstractJUnit5JdbcDaoTestSupport {

	private JdbcUserSettingsDao dao;
	private Long userId;

	private UserSettings last;

	@BeforeEach
	public void setup() {
		dao = new JdbcUserSettingsDao(jdbcTemplate);
		userId = CommonDbTestUtils.insertUser(jdbcTemplate);
	}

	@Test
	public void insert() {
		// GIVEN
		UserSettings settings = new UserSettings(userId, Instant.now());
		settings.setModified(settings.getCreated());
		settings.setPublishToSolarIn(true);
		settings.setPublishToSolarFlux(true);
		settings.setSourceIdTemplate("foo/bar");

		// WHEN
		Long result = dao.save(settings);

		// THEN
		List<Map<String, Object>> data = allUserSettingsData(jdbcTemplate);
		assertThat("Table has 1 row", data, hasSize(1));
		Map<String, Object> row = data.get(0);
		assertThat("Row creation date", row, hasEntry("created", Timestamp.from(settings.getCreated())));
		assertThat("Row modification date is creation date", row,
				hasEntry("modified", Timestamp.from(settings.getCreated())));
		assertThat("Row user ID matches", row, hasEntry("user_id", settings.getUserId()));
		assertThat("Row pub_in matches", row, hasEntry("pub_in", settings.isPublishToSolarIn()));
		assertThat("Row pub_flux matches", row, hasEntry("pub_flux", settings.isPublishToSolarFlux()));
		assertThat("Row source_id_tmpl matches", row,
				hasEntry("source_id_tmpl", settings.getSourceIdTemplate()));

		last = settings.copyWithId(result);
	}

	@Test
	public void get() {
		// GIVEN
		insert();

		// WHEN
		UserSettings result = dao.get(last.getId());

		// THEN
		assertThat("Retrieved entity matches source", result, is(equalTo(last)));
	}

	@Test
	public void update() {
		// GIVEN
		insert();

		// WHEN
		UserSettings settings = last.copyWithId(last.getId());
		settings.setModified(Instant.now().plusMillis(474));
		settings.setPublishToSolarIn(false);
		settings.setPublishToSolarIn(false);
		settings.setSourceIdTemplate("bim/bam");

		Long result = dao.save(settings);

		// THEN
		assertThat("Result is user ID", result, is(equalTo(last.getUserId())));
		List<Map<String, Object>> data = allUserSettingsData(jdbcTemplate);
		assertThat("Table has 1 row", data, hasSize(1));
		Map<String, Object> row = data.get(0);
		assertThat("Row creation date", row, hasEntry("created", Timestamp.from(settings.getCreated())));
		assertThat("Row modification date is creation date", row,
				hasEntry("modified", Timestamp.from(settings.getCreated())));
		assertThat("Row user ID matches", row, hasEntry("user_id", settings.getUserId()));
		assertThat("Row pub_in matches", row, hasEntry("pub_in", settings.isPublishToSolarIn()));
		assertThat("Row pub_flux matches", row, hasEntry("pub_flux", settings.isPublishToSolarFlux()));
		assertThat("Row source_id_tmpl matches", row,
				hasEntry("source_id_tmpl", settings.getSourceIdTemplate()));
	}

	@Test
	public void delete() {
		// GIVEN
		insert();

		// WHEN
		dao.delete(last);

		// THEN
		List<Map<String, Object>> data = allUserSettingsData(jdbcTemplate);
		assertThat("Row deleted from db", data, hasSize(0));
	}

	@Test
	public void findForUser() {
		// GIVEN
		final int count = 3;
		final int userCount = 3;
		final List<Long> userIds = new ArrayList<>(userCount);
		Map<Long, UserSettings> userSettings = new LinkedHashMap<>(userCount);
		final List<UserSettings> confs = new ArrayList<>(count * userCount);
		final Instant start = Instant.now().truncatedTo(ChronoUnit.MINUTES);
		for ( int i = 0; i < count; i++ ) {
			Instant t = start.plusSeconds(i);
			for ( int u = 0; u < userCount; u++ ) {
				Long userId;
				if ( i == 0 ) {
					userId = CommonDbTestUtils.insertUser(jdbcTemplate);
					userIds.add(userId);
				} else {
					userId = userIds.get(u);
				}
				UserSettings conf = newUserSettings(userId, t);
				Long id = dao.save(conf);
				conf = conf.copyWithId(id);
				confs.add(conf);
				userSettings.put(id, conf);
			}
		}

		// WHEN
		final Long userId = userIds.get(1);
		UserSettings result = dao.get(userId);

		// THEN
		assertThat("Results for single user returned", result, is(equalTo(userSettings.get(userId))));
	}

}
