/* ==================================================================
 * DatumPropertiesTests.java - 6/11/2020 6:46:57 am
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
import static org.junit.Assert.assertThat;
import java.math.BigDecimal;
import org.junit.Test;
import net.solarnetwork.central.datum.v2.domain.DatumProperties;

/**
 * Test cases for the {@link DatumProperties} class.
 * 
 * @author matt
 * @version 1.0
 */
public class DatumPropertiesTests {

	@Test
	public void length_null() {
		DatumProperties p = DatumProperties.propertiesOf(null, null, null, null);

		assertThat("Null instantaneous length is 0", p.getInstantaneousLength(), equalTo(0));
		assertThat("Null accumulating length is 0", p.getAccumulatingLength(), equalTo(0));
		assertThat("Null status length is 0", p.getStatusLength(), equalTo(0));
		assertThat("Null tags length is 0", p.getTagsLength(), equalTo(0));
	}

	@Test
	public void length_empty() {
		DatumProperties p = DatumProperties.propertiesOf(new BigDecimal[0], new BigDecimal[0],
				new String[0], new String[0]);

		assertThat("Empty instantaneous length is 0", p.getInstantaneousLength(), equalTo(0));
		assertThat("Empty accumulating length is 0", p.getAccumulatingLength(), equalTo(0));
		assertThat("Empty status length is 0", p.getStatusLength(), equalTo(0));
		assertThat("Empty tags length is 0", p.getTagsLength(), equalTo(0));
	}

	@Test
	public void length() {
		DatumProperties p = DatumProperties.propertiesOf(new BigDecimal[] { new BigDecimal("1") },
				new BigDecimal[] { new BigDecimal("2.1"), new BigDecimal("2.2") },
				new String[] { "3.1", "3.2", "3.3" }, new String[] { "4.1", "4.2", "4.3", "4.4" });

		assertThat("Instantaneous length", p.getInstantaneousLength(), equalTo(1));
		assertThat("Accumulating length", p.getAccumulatingLength(), equalTo(2));
		assertThat("Status length", p.getStatusLength(), equalTo(3));
		assertThat("Tags length", p.getTagsLength(), equalTo(4));
	}

}
