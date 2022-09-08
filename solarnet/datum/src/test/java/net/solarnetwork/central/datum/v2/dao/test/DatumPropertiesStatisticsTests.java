/* ==================================================================
 * DatumPropertiesStatisticsTests.java - 6/11/2020 6:54:04 am
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

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import java.math.BigDecimal;
import org.junit.Test;
import net.solarnetwork.domain.datum.DatumPropertiesStatistics;

/**
 * Test cases for the {@link DatumPropertiesStatistics} class.
 * 
 * @author matt
 * @version 1.0
 */
public class DatumPropertiesStatisticsTests {

	@Test
	public void length_null() {
		DatumPropertiesStatistics s = DatumPropertiesStatistics.statisticsOf(null, null);

		assertThat("Null instantaneous length is 0", s.getInstantaneousLength(), equalTo(0));
		assertThat("Null accumulating length is 0", s.getAccumulatingLength(), equalTo(0));
	}

	@Test
	public void length_empty() {
		DatumPropertiesStatistics s = DatumPropertiesStatistics.statisticsOf(new BigDecimal[0][],
				new BigDecimal[0][]);

		assertThat("Empty instantaneous length is 0", s.getInstantaneousLength(), equalTo(0));
		assertThat("Empty accumulating length is 0", s.getAccumulatingLength(), equalTo(0));
	}

	@Test
	public void length() {
		DatumPropertiesStatistics s = DatumPropertiesStatistics.statisticsOf(
				new BigDecimal[][] { new BigDecimal[] { new BigDecimal("1.1"), new BigDecimal("1.2"),
						new BigDecimal("1.3") } },
				new BigDecimal[][] { new BigDecimal[] { new BigDecimal("2.11"), new BigDecimal("2.12") },
						new BigDecimal[] { new BigDecimal("2.21"), new BigDecimal("2.22") } });

		assertThat("Instantaneous length", s.getInstantaneousLength(), equalTo(1));
		assertThat("Accumulating length", s.getAccumulatingLength(), equalTo(2));
	}

}
