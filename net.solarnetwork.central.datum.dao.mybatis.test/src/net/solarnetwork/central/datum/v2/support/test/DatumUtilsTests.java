/* ==================================================================
 * DatumUtilsTests.java - 24/11/2020 3:04:02 pm
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

package net.solarnetwork.central.datum.v2.support.test;

import static net.solarnetwork.util.NumberUtils.decimalArray;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.Test;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.datum.domain.ReportingGeneralNodeDatum;
import net.solarnetwork.central.datum.v2.dao.AggregateDatumEntity;
import net.solarnetwork.central.datum.v2.dao.BasicDatumCriteria;
import net.solarnetwork.central.datum.v2.dao.DatumEntity;
import net.solarnetwork.central.datum.v2.dao.ReadingDatumEntity;
import net.solarnetwork.central.datum.v2.domain.BasicObjectDatumStreamMetadata;
import net.solarnetwork.central.datum.v2.domain.DatumProperties;
import net.solarnetwork.central.datum.v2.domain.DatumPropertiesStatistics;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumKind;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamMetadata;
import net.solarnetwork.central.datum.v2.support.DatumUtils;
import net.solarnetwork.central.domain.Aggregation;
import net.solarnetwork.domain.GeneralDatumSamples;
import net.solarnetwork.domain.SortDescriptor;

/**
 * Test cases for the {@link DatumUtils} class.
 * 
 * @author matt
 * @version 1.0
 */
public class DatumUtilsTests {

	private ObjectDatumStreamMetadata newNodeMeta() {
		return new BasicObjectDatumStreamMetadata(UUID.randomUUID(), "Pacific/Auckland",
				ObjectDatumKind.Node, 1L, "a", new String[] { "a", "b", "c", "d" },
				new String[] { "e", "f", "g" }, new String[] { "h", "i" });
	}

	private DatumProperties newProps() {
		return DatumProperties.propertiesOf(decimalArray("1.1", "1.2", "1.3", "1.4"),
				decimalArray("2.1", "2.2", "2.3"), new String[] { "a", "b" }, new String[] { "t" });
	}

	private DatumPropertiesStatistics newStats() {
		return DatumPropertiesStatistics.statisticsOf(
		// @formatter:off
				new BigDecimal[][] { 
						decimalArray("60", "1.0", "2.0"), 
						decimalArray("61", "2.0", "3.0"),
						decimalArray("62", "3.0", "4.0"), 
						decimalArray("63", "4.0", "5.0") },
				new BigDecimal[][] { 
						decimalArray("10", "0", "10"), 
						decimalArray("20", "10", "30"),
						decimalArray("30", "30", "60") }
				// @formatter:on
		);
	}

	private void assertGeneralDatumSamples(GeneralDatumSamples s) {
		assertThat("Instantaneous keys copied", s.getInstantaneous().keySet(),
				containsInAnyOrder("a", "b", "c", "d"));

		assertThat("Accumulating keys copied", s.getAccumulating().keySet(),
				containsInAnyOrder("e", "f", "g"));

		assertThat("Status keys copied", s.getStatus().keySet(), containsInAnyOrder("h", "i"));

		assertThat("Tags copied", s.getTags(), containsInAnyOrder("t"));
	}

	private void assertAggregateGeneralDatumSamples(GeneralDatumSamples s) {
		assertThat("Instantaneous keys copied with stats", s.getInstantaneous().keySet(),
				containsInAnyOrder("a", "a_min", "a_max", "b", "b_min", "b_max", "c", "c_min", "c_max",
						"d", "d_min", "d_max"));

		// @formatter:off
		assertThat("Instantaneous prop 'a' with stats", s.getI(), allOf(
				hasEntry("a", new BigDecimal("1.1")), 
				hasEntry("a_min", new BigDecimal("1.0")),
				hasEntry("a_max", new BigDecimal("2.0"))));
		assertThat("Instantaneous prop 'd' with stats", s.getI(), allOf(
				hasEntry("d", new BigDecimal("1.4")), 
				hasEntry("d_min", new BigDecimal("4.0")),
				hasEntry("d_max", new BigDecimal("5.0"))));
		// @formatter:on

		assertThat("Accumulating keys copied", s.getAccumulating().keySet(),
				containsInAnyOrder("e", "f", "g"));

		// @formatter:off
		assertThat("Accumluating values from props", s.getA(), allOf(
				hasEntry("e", new BigDecimal("2.1")),
				hasEntry("f", new BigDecimal("2.2")),
				hasEntry("g", new BigDecimal("2.3"))));
		// @formatter:on

		assertThat("Status keys copied", s.getStatus().keySet(), containsInAnyOrder("h", "i"));

		assertThat("Tags copied", s.getTags(), containsInAnyOrder("t"));
	}

	private void assertReadingGeneralDatumSamples(GeneralDatumSamples s) {
		assertThat("Instantaneous keys copied with stats along with accumualting stat keys",
				s.getInstantaneous().keySet(),
				containsInAnyOrder("a", "a_min", "a_max", "b", "b_min", "b_max", "c", "c_min", "c_max",
						"d", "d_min", "d_max", "e_start", "e_end", "f_start", "f_end", "g_start",
						"g_end"));

		// @formatter:off
		assertThat("Instantaneous prop 'a' with stats", s.getI(), allOf(
				hasEntry("a", new BigDecimal("1.1")), 
				hasEntry("a_min", new BigDecimal("1.0")),
				hasEntry("a_max", new BigDecimal("2.0"))));
		assertThat("Instantaneous prop 'd' with stats", s.getI(), allOf(
				hasEntry("d", new BigDecimal("1.4")), 
				hasEntry("d_min", new BigDecimal("4.0")),
				hasEntry("d_max", new BigDecimal("5.0"))));
		assertThat("Accumluating prop 'e' stats", s.getI(), allOf(
				hasEntry("e_start", new BigDecimal("0")),
				hasEntry("e_end", new BigDecimal("10"))));
		assertThat("Accumluating prop 'g' stats", s.getI(), allOf(
				hasEntry("g_start", new BigDecimal("30")),
				hasEntry("g_end", new BigDecimal("60"))));
		// @formatter:on

		assertThat("Accumulating keys copied", s.getAccumulating().keySet(),
				containsInAnyOrder("e", "f", "g"));

		// @formatter:off
		assertThat("Accumluating values from stats", s.getA(), allOf(
				hasEntry("e", new BigDecimal("10")),
				hasEntry("f", new BigDecimal("20")),
				hasEntry("g", new BigDecimal("30"))));
		// @formatter:on

		assertThat("Status keys copied", s.getStatus().keySet(), containsInAnyOrder("h", "i"));

		assertThat("Tags copied", s.getTags(), containsInAnyOrder("t"));
	}

	@Test
	public void populateGeneralDatumSamples_typical() {
		// GIVEN
		DatumProperties props = newProps();

		// WHEN
		GeneralDatumSamples s = new GeneralDatumSamples();
		DatumUtils.populateGeneralDatumSamples(s, props, newNodeMeta());

		// THEN
		assertGeneralDatumSamples(s);
	}

	@Test
	public void toGeneralNodeDatum_typical() {
		// GIVEN
		DatumProperties props = newProps();
		DatumEntity datum = new DatumEntity(UUID.randomUUID(),
				Instant.now().truncatedTo(ChronoUnit.SECONDS), Instant.now(), props);
		ObjectDatumStreamMetadata meta = newNodeMeta();

		// WHEN
		ReportingGeneralNodeDatum d = DatumUtils.toGeneralNodeDatum(datum, meta);

		// THEN
		assertThat("Node ID copied from meta", d.getNodeId(), equalTo(meta.getObjectId()));
		assertThat("Source ID copied from meta", d.getSourceId(), equalTo(meta.getSourceId()));
		assertThat("Timestamp copied from datum and meta time zone", d.getCreated(),
				equalTo(new org.joda.time.DateTime(datum.getTimestamp().toEpochMilli(),
						org.joda.time.DateTimeZone.forID(meta.getTimeZoneId()))));
		assertThat("Local date copied from datum and meta time zone", d.getLocalDateTime(),
				equalTo(new org.joda.time.DateTime(datum.getTimestamp().toEpochMilli(),
						org.joda.time.DateTimeZone.forID(meta.getTimeZoneId())).toLocalDateTime()));
		assertThat("Received date NOT copied from datum (not part of Datum API)", d.getPosted(),
				nullValue());
		assertGeneralDatumSamples(d.getSamples());
	}

	@Test
	public void toGeneralNodeDatum_aggregate() {
		// GIVEN
		DatumProperties props = newProps();
		DatumPropertiesStatistics stats = newStats();
		AggregateDatumEntity datum = new AggregateDatumEntity(UUID.randomUUID(),
				Instant.now().truncatedTo(ChronoUnit.HOURS), Aggregation.Hour, props, stats);
		ObjectDatumStreamMetadata meta = newNodeMeta();

		// WHEN
		ReportingGeneralNodeDatum d = DatumUtils.toGeneralNodeDatum(datum, meta);

		// THEN
		assertThat("Node ID copied from meta", d.getNodeId(), equalTo(meta.getObjectId()));
		assertThat("Source ID copied from meta", d.getSourceId(), equalTo(meta.getSourceId()));
		assertThat("Timestamp copied from datum and meta time zone", d.getCreated(),
				equalTo(new org.joda.time.DateTime(datum.getTimestamp().toEpochMilli(),
						org.joda.time.DateTimeZone.forID(meta.getTimeZoneId()))));
		assertThat("Local date copied from datum and meta time zone", d.getLocalDateTime(),
				equalTo(new org.joda.time.DateTime(datum.getTimestamp().toEpochMilli(),
						org.joda.time.DateTimeZone.forID(meta.getTimeZoneId())).toLocalDateTime()));
		assertThat("Received date NOT copied from datum (not part of Datum API)", d.getPosted(),
				nullValue());
		assertAggregateGeneralDatumSamples(d.getSamples());
	}

	@Test
	public void toGeneralNodeDatum_reading() {
		// GIVEN
		DatumProperties props = newProps();
		DatumPropertiesStatistics stats = newStats();
		ReadingDatumEntity datum = new ReadingDatumEntity(UUID.randomUUID(),
				Instant.now().truncatedTo(ChronoUnit.HOURS), Aggregation.Hour, null, props, stats);
		ObjectDatumStreamMetadata meta = newNodeMeta();

		// WHEN
		ReportingGeneralNodeDatum d = DatumUtils.toGeneralNodeDatum(datum, meta);

		// THEN
		assertThat("Node ID copied from meta", d.getNodeId(), equalTo(meta.getObjectId()));
		assertThat("Source ID copied from meta", d.getSourceId(), equalTo(meta.getSourceId()));
		assertThat("Timestamp copied from datum and meta time zone", d.getCreated(),
				equalTo(new org.joda.time.DateTime(datum.getTimestamp().toEpochMilli(),
						org.joda.time.DateTimeZone.forID(meta.getTimeZoneId()))));
		assertThat("Local date copied from datum and meta time zone", d.getLocalDateTime(),
				equalTo(new org.joda.time.DateTime(datum.getTimestamp().toEpochMilli(),
						org.joda.time.DateTimeZone.forID(meta.getTimeZoneId())).toLocalDateTime()));
		assertThat("Received date NOT copied from datum (not part of Datum API)", d.getPosted(),
				nullValue());
		assertReadingGeneralDatumSamples(d.getSamples());
	}

	@Test
	public void toGeneralNodeDatum_readingWithoutStats() {
		// GIVEN
		DatumProperties props = newProps();
		ReadingDatumEntity datum = new ReadingDatumEntity(UUID.randomUUID(),
				Instant.now().truncatedTo(ChronoUnit.HOURS), Aggregation.Hour, null, props, null);
		ObjectDatumStreamMetadata meta = newNodeMeta();

		// WHEN
		ReportingGeneralNodeDatum d = DatumUtils.toGeneralNodeDatum(datum, meta);

		// THEN
		assertThat("Node ID copied from meta", d.getNodeId(), equalTo(meta.getObjectId()));
		assertThat("Source ID copied from meta", d.getSourceId(), equalTo(meta.getSourceId()));
		assertThat("Timestamp copied from datum and meta time zone", d.getCreated(),
				equalTo(new org.joda.time.DateTime(datum.getTimestamp().toEpochMilli(),
						org.joda.time.DateTimeZone.forID(meta.getTimeZoneId()))));
		assertThat("Local date copied from datum and meta time zone", d.getLocalDateTime(),
				equalTo(new org.joda.time.DateTime(datum.getTimestamp().toEpochMilli(),
						org.joda.time.DateTimeZone.forID(meta.getTimeZoneId())).toLocalDateTime()));
		assertThat("Received date NOT copied from datum (not part of Datum API)", d.getPosted(),
				nullValue());

		// without stats, we treat like a Datum, not ReadingDatum; used with CalculatedAt query
		assertGeneralDatumSamples(d.getSamples());
	}

	@Test
	public void criteriaFromFilter_datumFilterCommand_aggNodeSourceDateRange() {
		// GIVEN
		DatumFilterCommand f = new DatumFilterCommand();
		f.setAggregate(Aggregation.Hour);
		f.setNodeIds(new Long[] { 1L, 2L });
		f.setSourceIds(new String[] { "a", "b" });
		f.setStartDate(new org.joda.time.DateTime().hourOfDay().roundFloorCopy());
		f.setEndDate(new org.joda.time.DateTime());

		// WHEN
		BasicDatumCriteria c = DatumUtils.criteriaFromFilter(f);

		// THEN
		assertThat("Agg copied", c.getAggregation(), equalTo(f.getAggregation()));
		assertThat("Node IDs copied", c.getNodeIds(), arrayContaining(f.getNodeIds()));
		assertThat("Source IDs copied", c.getSourceIds(), arrayContaining(f.getSourceIds()));
		assertThat("Start date copied", c.getStartDate(),
				equalTo(Instant.ofEpochMilli(f.getStartDate().getMillis())));
		assertThat("End date copied", c.getEndDate(),
				equalTo(Instant.ofEpochMilli(f.getEndDate().getMillis())));
	}

	@Test
	public void criteriaFromFilter_datumFilterCommand_localDateRange() {
		// GIVEN
		DatumFilterCommand f = new DatumFilterCommand();
		f.setLocalStartDate(new org.joda.time.LocalDateTime(2020, 1, 2, 12, 34, 56, 789));
		f.setLocalEndDate(f.getLocalStartDate().plusDays(1));

		// WHEN
		BasicDatumCriteria c = DatumUtils.criteriaFromFilter(f);

		// THEN
		assertThat("Local start date copied", c.getLocalStartDate(),
				equalTo(LocalDateTime.of(2020, 1, 2, 12, 34, 56, 789 * 1000000)));
		assertThat("Local end date copied", c.getLocalEndDate(),
				equalTo(LocalDateTime.of(2020, 1, 3, 12, 34, 56, 789 * 1000000)));
	}

	@Test
	public void criteriaFromFilter_datumFilterCommand_paginationFromFilter() {
		// GIVEN
		DatumFilterCommand f = new DatumFilterCommand();
		f.setMax(1);
		f.setOffset(2);

		// WHEN
		BasicDatumCriteria c = DatumUtils.criteriaFromFilter(f);

		// THEN
		assertThat("Max copied from filter", c.getMax(), equalTo(1));
		assertThat("Offset copied from filter", c.getOffset(), equalTo(2));
	}

	@Test
	public void criteriaFromFilter_datumFilterCommand_paginationFromArgs() {
		// GIVEN
		DatumFilterCommand f = new DatumFilterCommand();
		f.setMax(1);
		f.setOffset(2);

		// WHEN
		BasicDatumCriteria c = DatumUtils.criteriaFromFilter(f, null, 4, 3);

		// THEN
		assertThat("Max copied from args", c.getMax(), equalTo(3));
		assertThat("Offset copied from args", c.getOffset(), equalTo(4));
	}

	@Test
	public void criteriaFromFilter_datumFilterCommand_sortsFromFilter() {
		// GIVEN
		DatumFilterCommand f = new DatumFilterCommand();
		List<net.solarnetwork.central.support.MutableSortDescriptor> msds = new ArrayList<>();
		net.solarnetwork.central.support.MutableSortDescriptor msd = new net.solarnetwork.central.support.MutableSortDescriptor();
		msd.setSortKey("a");
		msd.setDescending(true);
		msds.add(msd);
		f.setSorts(msds);

		// WHEN
		BasicDatumCriteria c = DatumUtils.criteriaFromFilter(f);

		// THEN
		assertThat("Sorts copied from filter", c.getSorts(), hasSize(1));
		SortDescriptor sort = c.getSorts().get(0);
		assertThat("Sort key copied", sort.getSortKey(), equalTo(msd.getSortKey()));
		assertThat("Sort order copied", sort.isDescending(), equalTo(msd.isDescending()));
	}

	@Test
	public void criteriaFromFilter_datumFilterCommand_sortsFromArgs() {
		// GIVEN
		DatumFilterCommand f = new DatumFilterCommand();
		List<net.solarnetwork.central.support.MutableSortDescriptor> msds = new ArrayList<>();
		net.solarnetwork.central.support.MutableSortDescriptor msd = new net.solarnetwork.central.support.MutableSortDescriptor();
		msd.setSortKey("a");
		msd.setDescending(true);
		msds.add(msd);
		f.setSorts(msds);

		List<net.solarnetwork.central.domain.SortDescriptor> msds2 = new ArrayList<>();
		net.solarnetwork.central.support.MutableSortDescriptor msd2 = new net.solarnetwork.central.support.MutableSortDescriptor();
		msd2.setSortKey("a");
		msd2.setDescending(true);
		msds2.add(msd);

		// WHEN
		BasicDatumCriteria c = DatumUtils.criteriaFromFilter(f, msds2, null, null);

		// THEN
		assertThat("Sorts copied from filter", c.getSorts(), hasSize(1));
		SortDescriptor sort = c.getSorts().get(0);
		assertThat("Sort key copied", sort.getSortKey(), equalTo(msd2.getSortKey()));
		assertThat("Sort order copied", sort.isDescending(), equalTo(msd2.isDescending()));
	}
}
