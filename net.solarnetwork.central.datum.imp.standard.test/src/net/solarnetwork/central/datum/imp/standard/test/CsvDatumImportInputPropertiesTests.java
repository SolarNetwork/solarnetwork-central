/* ==================================================================
 * CsvDatumImportInputPropertiesTests.java - 8/11/2018 1:11:28 PM
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

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Test;
import net.solarnetwork.central.datum.imp.standard.CsvDatumImportInputProperties;
import net.solarnetwork.settings.KeyedSettingSpecifier;
import net.solarnetwork.settings.SettingSpecifier;

/**
 * Test cases for the {@link CsvDatumImportInputProperties} class.
 * 
 * @author matt
 * @version 1.0
 */
public class CsvDatumImportInputPropertiesTests {

	@Test
	public void csvSettings() {
		List<SettingSpecifier> settings = CsvDatumImportInputProperties.getCsvSettingSpecifiers();
		assertThat("Settings", settings, hasSize(5));

		List<String> keys = settings.stream().filter(s -> s instanceof KeyedSettingSpecifier<?>)
				.map(s -> ((KeyedSettingSpecifier<?>) s).getKey()).collect(Collectors.toList());
		assertThat("Setting keys", keys, contains("headerRowCount", "dateFormat", "nodeIdColumn",
				"sourceIdColumn", "dateColumnsValue"));
	}

	@Test
	public void setDateColumnsValue() {
		CsvDatumImportInputProperties p = new CsvDatumImportInputProperties();
		p.setDateColumnsValue("1,2,3");
		assertThat("Date columns parsed", p.getDateColumns(), contains(1, 2, 3));
	}

	@Test
	public void dateColumnsValue() {
		CsvDatumImportInputProperties p = new CsvDatumImportInputProperties();
		p.setDateColumns(Arrays.asList(3, 2, 1));
		assertThat("Date columns value", p.getDateColumnsValue(), equalTo("3,2,1"));
	}

}
