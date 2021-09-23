/* ==================================================================
 * CombiningConfigTests.java - 4/12/2020 3:17:44 pm
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

package net.solarnetwork.central.datum.v2.dao.test;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import org.junit.Test;
import net.solarnetwork.central.datum.domain.CombiningType;
import net.solarnetwork.central.datum.v2.dao.BasicDatumCriteria;
import net.solarnetwork.central.datum.v2.dao.CombiningConfig;

/**
 * Test cases for the {@link CombiningConfig} class.
 * 
 * @author matt
 * @version 1.0
 */
public class CombiningConfigTests {

	@Test
	public void createFromCriteria_nodesAndSources() {
		// GIVEN
		BasicDatumCriteria c = new BasicDatumCriteria();
		c.setCombiningType(CombiningType.Sum);
		c.setObjectIdMaps(new String[] { "100:2,3,4", "200:5,6" });
		c.setSourceIdMaps(new String[] { "V1:A,B,C", "V2:D,E" });

		// WHEN
		CombiningConfig config = CombiningConfig.configFromCriteria(c);

		// THEN
		assertThat("Config created", config, notNullValue());
		assertThat("Type preserved", config.getType(), equalTo(c.getCombiningType()));
		assertThat("With obj", config.isWithObjectIds(), equalTo(true));
		assertThat("With src", config.isWithSourceIds(), equalTo(true));
		assertThat("Id config keys", config.getIdsConfigKeys(), containsInAnyOrder(
				CombiningConfig.OBJECT_IDS_CONFIG, CombiningConfig.SOURCE_IDS_CONFIG));
	}

}
