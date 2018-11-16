/* ==================================================================
 * BasicBasicCsvDatumImportInputPropertiesTests.java - 8/11/2018 1:20:57 PM
 * 
 * Copyright 2018 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.imp.standard.test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.Test;
import net.solarnetwork.central.datum.imp.standard.BasicCsvDatumImportInputProperties;
import net.solarnetwork.settings.KeyedSettingSpecifier;
import net.solarnetwork.settings.SettingSpecifier;

/**
 * Test cases for the {@link BasicCsvDatumImportInputProperties} class.
 * 
 * @author matt
 * @version 1.0
 */
public class BasicCsvDatumImportInputPropertiesTests {

	@Test
	public void csvSettings() {
		List<SettingSpecifier> settings = BasicCsvDatumImportInputProperties
				.getBasicCsvSettingSpecifiers();
		assertThat("Settings", settings, hasSize(9));

		List<String> keys = settings.stream().filter(s -> s instanceof KeyedSettingSpecifier<?>)
				.map(s -> ((KeyedSettingSpecifier<?>) s).getKey()).collect(Collectors.toList());
		assertThat("Setting keys", keys,
				contains("headerRowCount", "dateFormat", "nodeIdColumn", "sourceIdColumn",
						"dateColumnsValue", "instantaneousDataColumn", "accumulatingDataColumn",
						"statusDataColumn", "tagDataColumn"));
	}

	@Test
	public void validByDefault() {
		BasicCsvDatumImportInputProperties p = new BasicCsvDatumImportInputProperties();
		assertThat("Valid by default", p.isValid(), equalTo(true));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void defaultServiceProperties() {
		Map<String, Object> props = new BasicCsvDatumImportInputProperties().toServiceProperties();
		assertThat(props,
				allOf(hasEntry("headerRowCount", (Object) 1), hasEntry("dateColumnsValue", "3"),
						hasEntry("dateFormat", "yyyy-MM-dd HH:mm:ss"),
						hasEntry("nodeIdColumn", (Object) 1), hasEntry("sourceIdColumn", (Object) 2),
						hasEntry("instantaneousDataColumn", (Object) 4),
						hasEntry("accumulatingDataColumn", (Object) 5),
						hasEntry("statusDataColumn", (Object) 6), hasEntry("tagDataColumn", (Object) 7)

				));
	}

	@Test
	public void invlalidWithoutDataColumns() {
		BasicCsvDatumImportInputProperties p = new BasicCsvDatumImportInputProperties();
		p.setInstantaneousDataColumn(null);
		p.setAccumulatingDataColumn(null);
		p.setStatusDataColumn(null);
		assertThat("Invalid without data column", p.isValid(), equalTo(false));
	}
}
