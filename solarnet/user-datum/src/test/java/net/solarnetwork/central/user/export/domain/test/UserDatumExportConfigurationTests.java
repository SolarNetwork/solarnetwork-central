/* ==================================================================
 * UserDatumExportConfigurationTests.java - 17/03/2025 1:28:48 pm
 * 
 * Copyright 2025 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.export.domain.test;

import static java.time.Instant.now;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static org.assertj.core.api.BDDAssertions.then;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.datum.export.domain.OutputCompressionType;
import net.solarnetwork.central.datum.export.domain.ScheduleType;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.user.export.domain.UserDataConfiguration;
import net.solarnetwork.central.user.export.domain.UserDatumExportConfiguration;
import net.solarnetwork.central.user.export.domain.UserDestinationConfiguration;
import net.solarnetwork.central.user.export.domain.UserOutputConfiguration;
import net.solarnetwork.codec.jackson.JsonUtils;
import net.solarnetwork.domain.datum.Aggregation;
import net.solarnetwork.util.DateUtils;

/**
 * Test cases for the {@ink UserDatumExportConfiguration} class.
 * 
 * @author matt
 * @version 1.0
 */
public class UserDatumExportConfigurationTests {

	public static UserDataConfiguration newDataConf(Long id, Long userId, Instant now) {
		UserDataConfiguration conf = new UserDataConfiguration(new UserLongCompositePK(userId, id), now);
		conf.setName(randomString());
		conf.setServiceIdentifier(randomString());

		Map<String, Object> sprops = new HashMap<String, Object>(4);
		sprops.put("string", "foo");
		sprops.put("number", 42);

		List<String> optionList = new ArrayList<String>(4);
		optionList.add("first");
		optionList.add("second");
		sprops.put("list", optionList);

		conf.setServiceProps(sprops);

		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setAggregate(Aggregation.Day);
		filter.setNodeId(randomLong());
		conf.setFilter(filter);

		return conf;
	}

	public static UserDestinationConfiguration newDestConf(Long id, Long userId, Instant now) {
		UserDestinationConfiguration conf = new UserDestinationConfiguration(
				new UserLongCompositePK(userId, id), now);
		conf.setName(UUID.randomUUID().toString());
		conf.setServiceIdentifier(UUID.randomUUID().toString());

		Map<String, Object> sprops = new HashMap<String, Object>(4);
		sprops.put("string", "foo");
		sprops.put("number", 42);

		List<String> optionList = new ArrayList<String>(4);
		optionList.add("first");
		optionList.add("second");
		sprops.put("list", optionList);

		conf.setServiceProps(sprops);

		return conf;
	}

	public static UserOutputConfiguration newOutpConf(Long id, Long userId, Instant now) {
		UserOutputConfiguration conf = new UserOutputConfiguration(new UserLongCompositePK(userId, id),
				now);
		conf.setName(UUID.randomUUID().toString());
		conf.setServiceIdentifier(UUID.randomUUID().toString());
		conf.setCompressionType(OutputCompressionType.None);

		Map<String, Object> sprops = new HashMap<String, Object>(4);
		sprops.put("string", "foo");
		sprops.put("number", 42);

		List<String> optionList = new ArrayList<String>(4);
		optionList.add("first");
		optionList.add("second");
		sprops.put("list", optionList);

		conf.setServiceProps(sprops);

		return conf;
	}

	@Test
	public void toJson() {
		// GIVEN
		UserDatumExportConfiguration conf = new UserDatumExportConfiguration(
				new UserLongCompositePK(randomLong(), randomLong()), now());
		conf.setName(randomString());
		conf.setHourDelayOffset(2);
		conf.setSchedule(ScheduleType.Weekly);

		// WHEN
		String json = JsonUtils.getJSONString(conf);

		// THEN
		then(json).isEqualTo("""
				{"id":%d,"created":"%s","userId":%d,"name":"%s","hourDelayOffset":2,"scheduleKey":"w"}"""
				.formatted(conf.getConfigId(), DateUtils.ISO_DATE_TIME_ALT_UTC.format(conf.getCreated()),
						conf.getUserId(), conf.getName()));
	}

}
