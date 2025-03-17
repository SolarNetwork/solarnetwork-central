/* ==================================================================
 * UserDestinationConfigurationTests.java - 17/03/2025 3:55:08 pm
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

import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static org.assertj.core.api.BDDAssertions.then;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.user.export.domain.UserDestinationConfiguration;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.util.DateUtils;

/**
 * Test cases for the {@link UserDestinationConfiguration} class.
 * 
 * @author matt
 * @version 1.0
 */
public class UserDestinationConfigurationTests {

	@Test
	public void toJson() {
		// GIVEN
		UserDestinationConfiguration conf = UserDatumExportConfigurationTests.newDestConf(randomLong(),
				randomLong(), Instant.now());

		// WHEN
		String json = JsonUtils.getJSONString(conf);

		// THEN
		then(json).isEqualTo("""
				{"id":%d,"created":"%s","userId":%d,"name":"%s",\
				"serviceIdentifier":"%s","serviceProperties":{"string":"foo",\
				"list":["first","second"],"number":42}}""".formatted(conf.getId(),
				DateUtils.ISO_DATE_TIME_ALT_UTC.format(conf.getCreated()), conf.getUserId(),
				conf.getName(), conf.getServiceIdentifier()));
	}

}
