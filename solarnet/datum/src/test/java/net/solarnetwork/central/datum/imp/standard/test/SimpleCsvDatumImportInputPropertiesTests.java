/* ==================================================================
 * BasicSimpleCsvDatumImportInputPropertiesTests.java - 8/11/2018 1:20:57 PM
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

import static net.solarnetwork.util.IntRange.rangeOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.Test;
import net.solarnetwork.central.datum.imp.standard.SimpleCsvDatumImportInputProperties;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.settings.KeyedSettingSpecifier;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.util.IntRangeSet;

/**
 * Test cases for the {@link SimpleCsvDatumImportInputProperties} class.
 * 
 * @author matt
 * @version 1.0
 */
public class SimpleCsvDatumImportInputPropertiesTests {

	@Test
	public void csvSettings() {
		List<SettingSpecifier> settings = SimpleCsvDatumImportInputProperties
				.getSimpleCsvSettingSpecifiers();
		assertThat("Settings", settings, hasSize(9));

		List<String> keys = settings.stream().filter(s -> s instanceof KeyedSettingSpecifier<?>)
				.map(s -> ((KeyedSettingSpecifier<?>) s).getKey()).collect(Collectors.toList());
		assertThat("Setting keys", keys,
				contains("headerRowCount", "dateFormat", "nodeIdColumn", "sourceIdColumn",
						"dateColumnsValue", "instantaneousDataColumns", "accumulatingDataColumns",
						"statusDataColumns", "tagDataColumns"));
	}

	@Test
	public void invalidByDefault() {
		SimpleCsvDatumImportInputProperties p = new SimpleCsvDatumImportInputProperties();
		assertThat("Invalid by default", p.isValid(), is(equalTo(false)));
	}

	@Test
	public void defaultServiceProperties() {
		Map<String, Object> props = new SimpleCsvDatumImportInputProperties().toServiceProperties();
		// @formatter:off
		assertThat(props,
				allOf(hasEntry("headerRowCount", (Object) 1),
						hasEntry("dateColumnsValue", "3"),
						hasEntry("dateFormat", "yyyy-MM-dd HH:mm:ss"),
						hasEntry("nodeIdColumn", (Object) 1),
						hasEntry("sourceIdColumn", (Object) 2)
						/*-
						hasEntry("instantaneousDataColumns", (Object) 4),
						hasEntry("accumulatingDataColumns", (Object) 5),
						hasEntry("statusDataColumns", (Object) 6),
						hasEntry("tagDataColumns", (Object) 7)
						*/
				));
		// @formatter:on
	}

	@Test
	public void invlalidWithoutDataColumns() {
		SimpleCsvDatumImportInputProperties p = new SimpleCsvDatumImportInputProperties();
		p.setTagDataColumns("1");
		assertThat("Invalid without data column", p.isValid(), is(equalTo(false)));
	}

	@Test
	public void columnsForType_empty() {
		// GIVEN
		SimpleCsvDatumImportInputProperties p = new SimpleCsvDatumImportInputProperties();

		// THEN
		assertThat("Empty property type returns null column set",
				p.columnsForType(DatumSamplesType.Instantaneous), is(nullValue()));
	}

	@Test
	public void columnsForType_number_single() {
		// GIVEN
		SimpleCsvDatumImportInputProperties p = new SimpleCsvDatumImportInputProperties();
		p.setInstantaneousDataColumns("4");

		// THEN
		IntRangeSet expected = new IntRangeSet(rangeOf(4));
		assertThat("Empty property type returns null column set",
				p.columnsForType(DatumSamplesType.Instantaneous), is(expected));
	}

	@Test
	public void columnsForType_number_multi() {
		// GIVEN
		SimpleCsvDatumImportInputProperties p = new SimpleCsvDatumImportInputProperties();
		p.setInstantaneousDataColumns("4,5,6,10");

		// THEN
		IntRangeSet expected = new IntRangeSet(rangeOf(4, 6), rangeOf(10));
		assertThat("Empty property type returns null column set",
				p.columnsForType(DatumSamplesType.Instantaneous), is(expected));
	}

	@Test
	public void columnsForType_number_range() {
		// GIVEN
		SimpleCsvDatumImportInputProperties p = new SimpleCsvDatumImportInputProperties();
		p.setInstantaneousDataColumns("4-6");

		// THEN
		IntRangeSet expected = new IntRangeSet(rangeOf(4, 6));
		assertThat("Empty property type returns null column set",
				p.columnsForType(DatumSamplesType.Instantaneous), is(expected));
	}

	@Test
	public void columnsForType_number_mixed() {
		// GIVEN
		SimpleCsvDatumImportInputProperties p = new SimpleCsvDatumImportInputProperties();
		p.setInstantaneousDataColumns("4-6,10,13-18");

		// THEN
		IntRangeSet expected = new IntRangeSet(rangeOf(4, 6), rangeOf(10), rangeOf(13, 18));
		assertThat("Empty property type returns null column set",
				p.columnsForType(DatumSamplesType.Instantaneous), is(expected));
	}

	@Test
	public void columnsForType_alpha_single() {
		// GIVEN
		SimpleCsvDatumImportInputProperties p = new SimpleCsvDatumImportInputProperties();
		p.setInstantaneousDataColumns("D");

		// THEN
		IntRangeSet expected = new IntRangeSet(rangeOf(4));
		assertThat("Single alpha column", p.columnsForType(DatumSamplesType.Instantaneous),
				is(expected));
	}

	@Test
	public void columnsForType_alpha_single_large() {
		// GIVEN
		SimpleCsvDatumImportInputProperties p = new SimpleCsvDatumImportInputProperties();
		p.setInstantaneousDataColumns("AA");

		// THEN
		IntRangeSet expected = new IntRangeSet(rangeOf(27));
		assertThat("Single large alpha column", p.columnsForType(DatumSamplesType.Instantaneous),
				is(expected));
	}

	@Test
	public void columnsForType_alpha_single_xlarge() {
		// GIVEN
		SimpleCsvDatumImportInputProperties p = new SimpleCsvDatumImportInputProperties();
		p.setInstantaneousDataColumns("BA");

		// THEN
		IntRangeSet expected = new IntRangeSet(rangeOf(53));
		assertThat("Single large alpha column", p.columnsForType(DatumSamplesType.Instantaneous),
				is(expected));
	}

	@Test
	public void columnsForType_alpha_single_xxlarge() {
		// GIVEN
		SimpleCsvDatumImportInputProperties p = new SimpleCsvDatumImportInputProperties();
		p.setInstantaneousDataColumns("ABC");

		// THEN
		IntRangeSet expected = new IntRangeSet(rangeOf(731));
		assertThat("Single large alpha column", p.columnsForType(DatumSamplesType.Instantaneous),
				is(expected));
	}

	@Test
	public void columnsForType_alpha_multi() {
		// GIVEN
		SimpleCsvDatumImportInputProperties p = new SimpleCsvDatumImportInputProperties();
		p.setInstantaneousDataColumns("D,E,F,J");

		// THEN
		IntRangeSet expected = new IntRangeSet(rangeOf(4, 6), rangeOf(10));
		assertThat("Alpha column list", p.columnsForType(DatumSamplesType.Instantaneous), is(expected));
	}

	@Test
	public void columnsForType_alpha_range() {
		// GIVEN
		SimpleCsvDatumImportInputProperties p = new SimpleCsvDatumImportInputProperties();
		p.setInstantaneousDataColumns("D-F");

		// THEN
		IntRangeSet expected = new IntRangeSet(rangeOf(4, 6));
		assertThat("Alpha range", p.columnsForType(DatumSamplesType.Instantaneous), is(expected));
	}

	@Test
	public void columnsForType_alpha_mixed() {
		// GIVEN
		SimpleCsvDatumImportInputProperties p = new SimpleCsvDatumImportInputProperties();
		p.setInstantaneousDataColumns("D-F,J,M-R");

		// THEN
		IntRangeSet expected = new IntRangeSet(rangeOf(4, 6), rangeOf(10), rangeOf(13, 18));
		assertThat("Alpha mix of ranges and columns", p.columnsForType(DatumSamplesType.Instantaneous),
				is(expected));
	}

	@Test
	public void columnsForType_alpha_large() {
		// GIVEN
		SimpleCsvDatumImportInputProperties p = new SimpleCsvDatumImportInputProperties();
		p.setInstantaneousDataColumns("M-AD");

		// THEN
		IntRangeSet expected = new IntRangeSet(rangeOf(13, 30));
		assertThat("Alpha range with large reference", p.columnsForType(DatumSamplesType.Instantaneous),
				is(expected));
	}

}
