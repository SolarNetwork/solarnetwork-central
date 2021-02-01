/* ==================================================================
 * BasicDatumStreamMetadataTests.java - 22/10/2020 3:18:01 pm
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

package net.solarnetwork.central.datum.v2.domain.test;

import static java.util.UUID.randomUUID;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import org.junit.Test;
import net.solarnetwork.central.datum.v2.domain.BasicDatumStreamMetadata;
import net.solarnetwork.domain.GeneralDatumSamplesType;

/**
 * Test cases for the {@link BasicDatumStreamMetadata} class.
 * 
 * @author matt
 * @version 1.0
 */
public class BasicDatumStreamMetadataTests {

	@Test
	public void lengths() {
		BasicDatumStreamMetadata m = new BasicDatumStreamMetadata(randomUUID(), "UTC",
				new String[] { "one", "two", "three" }, new String[] { "four", "five" },
				new String[] { "six" });
		assertThat("Total length", m.getPropertyNamesLength(), equalTo(6));
		assertThat("Instantaneous length", m.getInstantaneousLength(), equalTo(3));
		assertThat("Accumulating length", m.getAccumulatingLength(), equalTo(2));
		assertThat("Status length", m.getStatusLength(), equalTo(1));
	}

	@Test
	public void lengths_null() {
		BasicDatumStreamMetadata m = new BasicDatumStreamMetadata(randomUUID(), "UTC", null, null, null);
		assertThat("Total length", m.getPropertyNamesLength(), equalTo(0));
		assertThat("Instantaneous length", m.getInstantaneousLength(), equalTo(0));
		assertThat("Accumulating length", m.getAccumulatingLength(), equalTo(0));
		assertThat("Status length", m.getStatusLength(), equalTo(0));
	}

	@Test
	public void lengths_empty() {
		BasicDatumStreamMetadata m = new BasicDatumStreamMetadata(randomUUID(), "UTC", new String[0],
				new String[0], new String[0]);
		assertThat("Total length", m.getPropertyNamesLength(), equalTo(0));
		assertThat("Instantaneous length", m.getInstantaneousLength(), equalTo(0));
		assertThat("Accumulating length", m.getAccumulatingLength(), equalTo(0));
		assertThat("Status length", m.getStatusLength(), equalTo(0));
	}

	@Test
	public void typeArrays() {
		BasicDatumStreamMetadata m = new BasicDatumStreamMetadata(randomUUID(), "UTC",
				new String[] { "one", "two", "three" }, new String[] { "four", "five" },
				new String[] { "six" });
		assertThat("All props", m.getPropertyNames(),
				arrayContaining("one", "two", "three", "four", "five", "six"));
		assertThat("Instantaneous props", m.propertyNamesForType(GeneralDatumSamplesType.Instantaneous),
				arrayContaining("one", "two", "three"));
		assertThat("Accumulating props", m.propertyNamesForType(GeneralDatumSamplesType.Accumulating),
				arrayContaining("four", "five"));
		assertThat("Status props", m.propertyNamesForType(GeneralDatumSamplesType.Status),
				arrayContaining("six"));
		assertThat("Tag props", m.propertyNamesForType(GeneralDatumSamplesType.Tag), nullValue());
	}

	@Test
	public void typeArrays_null() {
		BasicDatumStreamMetadata m = new BasicDatumStreamMetadata(randomUUID(), "UTC", null, null, null);
		assertThat("All props", m.getPropertyNames(), nullValue());
		assertThat("Instantaneous props", m.propertyNamesForType(GeneralDatumSamplesType.Instantaneous),
				nullValue());
		assertThat("Accumulating props", m.propertyNamesForType(GeneralDatumSamplesType.Accumulating),
				nullValue());
		assertThat("Status props", m.propertyNamesForType(GeneralDatumSamplesType.Status), nullValue());
		assertThat("Tag props", m.propertyNamesForType(GeneralDatumSamplesType.Tag), nullValue());
	}

	@Test
	public void typeArrays_empty() {
		BasicDatumStreamMetadata m = new BasicDatumStreamMetadata(randomUUID(), "UTC", new String[0],
				new String[0], new String[0]);
		assertThat("All props", m.getPropertyNames(), nullValue());
		assertThat("Instantaneous props", m.propertyNamesForType(GeneralDatumSamplesType.Instantaneous),
				nullValue());
		assertThat("Accumulating props", m.propertyNamesForType(GeneralDatumSamplesType.Accumulating),
				nullValue());
		assertThat("Status props", m.propertyNamesForType(GeneralDatumSamplesType.Status), nullValue());
		assertThat("Tag props", m.propertyNamesForType(GeneralDatumSamplesType.Tag), nullValue());
	}
}
