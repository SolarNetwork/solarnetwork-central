/* ==================================================================
 * DatumImportJobInfoTests.java - 26/02/2026 5:09:35 pm
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

package net.solarnetwork.central.datum.imp.domain.test;

import static java.time.Instant.now;
import static java.util.Map.entry;
import static java.util.UUID.randomUUID;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.JSON;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.util.DateUtils.ISO_DATE_TIME_ALT_UTC;
import static org.assertj.core.api.BDDAssertions.then;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.dao.UserUuidPK;
import net.solarnetwork.central.datum.imp.domain.DatumImportJobInfo;
import net.solarnetwork.codec.jackson.JsonUtils;

/**
 * Test cases for the {@link DatuMImportJobInfo} class.
 *
 * @author matt
 * @version 1.0
 */
public class DatumImportJobInfoTests {

	@Test
	public void toJson() {
		// GIVEN
		DatumImportJobInfo info = new DatumImportJobInfo(new UserUuidPK(randomLong(), randomUUID()),
				now());

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
				entry("importDate", ISO_DATE_TIME_ALT_UTC.format(info.getImportDate())),
				entry("importStateKey", String.valueOf(info.getImportStateKey())),
				entry("jobDuration", info.getJobDuration().toString()),
				entry("jobStateKey", String.valueOf(info.getJobStateKey())),
				entry("loadedCount", 0),
				entry("percentComplete", 0.0),
				entry("success", false)
			)
			;
		// @formatter:on
	}

}
