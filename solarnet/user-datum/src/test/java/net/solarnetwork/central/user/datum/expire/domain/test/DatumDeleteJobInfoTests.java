/* ==================================================================
 * DatumDeleteJobInfoTests.java - 26/02/2026 5:20:36 pm
 * 
 * Copyright 2026 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.datum.expire.domain.test;

import static java.util.Map.entry;
import static java.util.UUID.randomUUID;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.JSON;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static org.assertj.core.api.BDDAssertions.then;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.dao.UserUuidPK;
import net.solarnetwork.central.user.datum.expire.domain.DatumDeleteJobInfo;
import net.solarnetwork.codec.jackson.JsonUtils;

/**
 * Test cases for the {@link DatumDeleteJobInfo} class.
 * 
 * @author matt
 * @version 1.0
 */
public class DatumDeleteJobInfoTests {

	@Test
	public void toJson() {
		// GIVEN
		DatumDeleteJobInfo info = new DatumDeleteJobInfo();
		info.setId(new UserUuidPK(randomLong(), randomUUID()));

		// WHEN
		String json = JsonUtils.getJSONString(info);

		// THEN
		// @formatter:off
		then(json)
			.asInstanceOf(JSON)
			.isObject()
			.containsOnly(
				entry("id", info.getId().getId().toString()),
				entry("userId", info.getUserId()),
				entry("jobId", info.getJobId()),
				entry("jobDuration", info.getJobDuration().toString()),
				entry("jobStateKey", String.valueOf(info.getJobStateKey())),
				entry("submitDate", 0),
				entry("completionDate", 0),
				entry("modifiedDate", 0),
				entry("startedDate", 0),
				entry("resultCount", 0),
				entry("percentComplete", 0.0),
				entry("success", false)
			)
			;
		// @formatter:on
	}

}
