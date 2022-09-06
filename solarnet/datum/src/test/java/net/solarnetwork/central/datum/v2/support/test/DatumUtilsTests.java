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

import static net.solarnetwork.domain.BasicLocation.locationOf;
import static net.solarnetwork.util.NumberUtils.decimalArray;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.Assert;
import org.junit.Test;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.datum.domain.ReportingGeneralNodeDatum;
import net.solarnetwork.central.datum.domain.StreamDatumFilterCommand;
import net.solarnetwork.central.datum.v2.dao.AggregateDatumEntity;
import net.solarnetwork.central.datum.v2.dao.BasicDatumCriteria;
import net.solarnetwork.central.datum.v2.dao.DatumEntity;
import net.solarnetwork.central.datum.v2.dao.ReadingDatumEntity;
import net.solarnetwork.central.datum.v2.domain.BasicObjectDatumStreamMetadata;
import net.solarnetwork.domain.datum.DatumPropertiesStatistics;
import net.solarnetwork.central.datum.v2.support.DatumUtils;
import net.solarnetwork.domain.datum.Aggregation;
import net.solarnetwork.central.domain.SolarLocation;
import net.solarnetwork.domain.SortDescriptor;
import net.solarnetwork.domain.datum.DatumProperties;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadata;
import net.solarnetwork.util.DateUtils;

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

	private void assertGeneralDatumSamples(DatumSamples s) {
		assertThat("Instantaneous keys copied", s.getInstantaneous().keySet(),
				containsInAnyOrder("a", "b", "c", "d"));

		assertThat("Accumulating keys copied", s.getAccumulating().keySet(),
				containsInAnyOrder("e", "f", "g"));

		assertThat("Status keys copied", s.getStatus().keySet(), containsInAnyOrder("h", "i"));

		assertThat("Tags copied", s.getTags(), containsInAnyOrder("t"));
	}

	private void assertAggregateGeneralDatumSamples(DatumSamples s) {
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

	private void assertReadingGeneralDatumSamples(DatumSamples s) {
		assertReadingGeneralDatumSamples(s, null);
	}

	private void assertReadingGeneralDatumSamples(DatumSamples s, ZonedDateTime endDate) {
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

		if ( endDate != null ) {
			assertThat("Status keys copied", s.getStatus().keySet(),
					containsInAnyOrder("h", "i", "timeZone", "endDate", "localEndDate"));
			assertThat("Time zone", s.getStatusSampleString("timeZone"),
					equalTo(endDate.getZone().getId()));
			assertThat("End date as formatted string", s.getStatusSampleString("endDate"),
					equalTo(DateUtils.ISO_DATE_TIME_ALT_UTC.format(endDate)));
			assertThat("Local end date as formatted string", s.getStatusSampleString("localEndDate"),
					equalTo(DateUtils.ISO_DATE_TIME_ALT_UTC.format(endDate.toLocalDateTime())));
		} else {
			assertThat("Status keys copied", s.getStatus().keySet(), containsInAnyOrder("h", "i"));
		}

		assertThat("Tags copied", s.getTags(), containsInAnyOrder("t"));
	}

	@Test
	public void populateGeneralDatumSamples_typical() {
		// GIVEN
		DatumProperties props = newProps();

		// WHEN
		DatumSamples s = new DatumSamples();
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
				sameInstance(datum.getTimestamp()));
		assertThat("Local date copied from datum and meta time zone", d.getLocalDateTime(),
				equalTo(datum.getTimestamp().atZone(ZoneId.of(meta.getTimeZoneId())).toLocalDateTime()));
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
				sameInstance(datum.getTimestamp()));
		assertThat("Local date copied from datum and meta time zone", d.getLocalDateTime(),
				equalTo(datum.getTimestamp().atZone(ZoneId.of(meta.getTimeZoneId())).toLocalDateTime()));
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
				sameInstance(datum.getTimestamp()));
		assertThat("Local date copied from datum and meta time zone", d.getLocalDateTime(),
				equalTo(datum.getTimestamp().atZone(ZoneId.of(meta.getTimeZoneId())).toLocalDateTime()));
		assertThat("Received date NOT copied from datum (not part of Datum API)", d.getPosted(),
				nullValue());
		assertReadingGeneralDatumSamples(d.getSamples());
	}

	@Test
	public void toGeneralNodeDatum_readingWithEndTimestamp() {
		// GIVEN
		DatumProperties props = newProps();
		DatumPropertiesStatistics stats = newStats();
		ReadingDatumEntity datum = new ReadingDatumEntity(UUID.randomUUID(),
				Instant.now().truncatedTo(ChronoUnit.HOURS).plusMillis(123L), Aggregation.Hour,
				Instant.now().truncatedTo(ChronoUnit.HOURS).plusSeconds(3600).plusMillis(234L), props,
				stats);
		ObjectDatumStreamMetadata meta = newNodeMeta();

		// WHEN
		ReportingGeneralNodeDatum d = DatumUtils.toGeneralNodeDatum(datum, meta);

		// THEN
		assertThat("Node ID copied from meta", d.getNodeId(), equalTo(meta.getObjectId()));
		assertThat("Source ID copied from meta", d.getSourceId(), equalTo(meta.getSourceId()));
		assertThat("Timestamp copied from datum and meta time zone", d.getCreated(),
				sameInstance(datum.getTimestamp()));
		assertThat("Local date copied from datum and meta time zone", d.getLocalDateTime(),
				equalTo(datum.getTimestamp().atZone(ZoneId.of(meta.getTimeZoneId())).toLocalDateTime()));
		assertThat("Received date NOT copied from datum (not part of Datum API)", d.getPosted(),
				nullValue());
		assertReadingGeneralDatumSamples(d.getSamples(),
				datum.getEndTimestamp().atZone(ZoneId.of(meta.getTimeZoneId())));
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
				sameInstance(datum.getTimestamp()));
		assertThat("Local date copied from datum and meta time zone", d.getLocalDateTime(),
				equalTo(datum.getTimestamp().atZone(ZoneId.of(meta.getTimeZoneId())).toLocalDateTime()));
		assertThat("Received date NOT copied from datum (not part of Datum API)", d.getPosted(),
				nullValue());

		// without stats, we treat like a Datum, not ReadingDatum; used with CalculatedAt query
		assertGeneralDatumSamples(d.getSamples());
	}

	@Test
	public void toGeneralNodeDatum_moreAccumulatingPropNamesThanStatValues() {
		// GIVEN
		DatumProperties props = newProps();
		DatumPropertiesStatistics stats = newStats();
		ReadingDatumEntity datum = new ReadingDatumEntity(UUID.randomUUID(),
				Instant.now().truncatedTo(ChronoUnit.HOURS).plusMillis(123L), Aggregation.Hour,
				Instant.now().truncatedTo(ChronoUnit.HOURS).plusSeconds(3600).plusMillis(234L), props,
				stats);
		ObjectDatumStreamMetadata meta = new BasicObjectDatumStreamMetadata(UUID.randomUUID(),
				"Pacific/Auckland", ObjectDatumKind.Node, 1L, "a", new String[] { "a", "b", "c", "d" },
				new String[] { "e", "f", "g", "gg", "ggg" }, new String[] { "h", "i" });

		// WHEN
		ReportingGeneralNodeDatum d = DatumUtils.toGeneralNodeDatum(datum, meta);

		// THEN
		assertThat("Node ID copied from meta", d.getNodeId(), equalTo(meta.getObjectId()));
		assertThat("Source ID copied from meta", d.getSourceId(), equalTo(meta.getSourceId()));
		assertThat("Timestamp is creation date", d.getCreated(), sameInstance(datum.getTimestamp()));
		assertThat("Local date copied from datum and meta time zone", d.getLocalDateTime(),
				equalTo(datum.getTimestamp().atZone(ZoneId.of(meta.getTimeZoneId())).toLocalDateTime()));
		assertThat("Received date NOT copied from datum (not part of Datum API)", d.getPosted(),
				nullValue());
		assertReadingGeneralDatumSamples(d.getSamples(),
				datum.getEndTimestamp().atZone(ZoneId.of(meta.getTimeZoneId())));
	}

	@Test
	public void criteriaFromFilter_datumFilterCommand_aggNodeSourceDateRange() {
		// GIVEN
		DatumFilterCommand f = new DatumFilterCommand();
		f.setAggregate(Aggregation.Hour);
		f.setNodeIds(new Long[] { 1L, 2L });
		f.setSourceIds(new String[] { "a", "b" });
		f.setStartDate(Instant.now().truncatedTo(ChronoUnit.DAYS));
		f.setEndDate(Instant.now());

		// WHEN
		BasicDatumCriteria c = DatumUtils.criteriaFromFilter(f);

		// THEN
		assertThat("Agg copied", c.getAggregation(), equalTo(f.getAggregation()));
		assertThat("Node IDs copied", c.getNodeIds(), arrayContaining(f.getNodeIds()));
		assertThat("Source IDs copied", c.getSourceIds(), arrayContaining(f.getSourceIds()));
		assertThat("Start date copied", c.getStartDate(), equalTo(f.getStartDate()));
		assertThat("End date copied", c.getEndDate(), equalTo(f.getEndDate()));
	}

	@Test
	public void criteriaFromFilter_datumFilterCommand_localDateRange() {
		// GIVEN
		DatumFilterCommand f = new DatumFilterCommand();
		f.setLocalStartDate(
				LocalDateTime.of(2020, 1, 2, 12, 34, 56, (int) TimeUnit.MILLISECONDS.toNanos(789)));
		f.setLocalEndDate(f.getLocalStartDate().plusDays(1));

		// WHEN
		BasicDatumCriteria c = DatumUtils.criteriaFromFilter(f);

		// THEN
		assertThat("Local start date copied", c.getLocalStartDate(), equalTo(
				LocalDateTime.of(2020, 1, 2, 12, 34, 56, (int) TimeUnit.MILLISECONDS.toNanos(789))));
		assertThat("Local end date copied", c.getLocalEndDate(), equalTo(
				LocalDateTime.of(2020, 1, 3, 12, 34, 56, (int) TimeUnit.MILLISECONDS.toNanos(789))));
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
		List<net.solarnetwork.domain.MutableSortDescriptor> msds = new ArrayList<>();
		net.solarnetwork.domain.MutableSortDescriptor msd = new net.solarnetwork.domain.MutableSortDescriptor();
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
		List<net.solarnetwork.domain.MutableSortDescriptor> msds = new ArrayList<>();
		net.solarnetwork.domain.MutableSortDescriptor msd = new net.solarnetwork.domain.MutableSortDescriptor();
		msd.setSortKey("a");
		msd.setDescending(true);
		msds.add(msd);
		f.setSorts(msds);

		List<net.solarnetwork.domain.SortDescriptor> msds2 = new ArrayList<>();
		net.solarnetwork.domain.MutableSortDescriptor msd2 = new net.solarnetwork.domain.MutableSortDescriptor();
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

	@Test
	public void criteriaFromFilter_datumFilterCommand_location() {
		// GIVEN
		SolarLocation sl = new SolarLocation();
		sl.setCountry("NZ");
		sl.setRegion("Wellington");
		sl.setTimeZoneId("Pacific/Auckland");
		DatumFilterCommand f = new DatumFilterCommand(sl);

		// WHEN
		BasicDatumCriteria c = DatumUtils.criteriaFromFilter(f);

		// THEN
		assertThat("Location converted from filter", c.getLocation(),
				equalTo(locationOf(sl.getCountry(), sl.getRegion(), sl.getTimeZoneId())));
	}

	@Test
	public void criteriaFromFilter_datumFilterCommand_location_emptyRemoved() {
		// GIVEN
		SolarLocation sl = new SolarLocation();
		sl.setCountry("");
		sl.setRegion("     ");
		sl.setTimeZoneId("");
		DatumFilterCommand f = new DatumFilterCommand(sl);

		// WHEN
		BasicDatumCriteria c = DatumUtils.criteriaFromFilter(f);

		// THEN
		assertThat("Location not converted from filter because has only empty values", c.getLocation(),
				nullValue());
	}

	@Test
	public void criteriaFromFilter_datumFilterCommand_tag() {
		// GIVEN
		DatumFilterCommand f = new DatumFilterCommand();
		f.setTag("foo");

		// WHEN
		BasicDatumCriteria c = DatumUtils.criteriaFromFilter(f);

		// THEN
		assertThat("Tag converted to search filter", c.getSearchFilter(), equalTo("(/t=foo)"));
	}

	@Test
	public void criteriaFromFilter_datumFilterCommand_tags() {
		// GIVEN
		DatumFilterCommand f = new DatumFilterCommand();
		f.setTags(new String[] { "foo", "bar" });

		// WHEN
		BasicDatumCriteria c = DatumUtils.criteriaFromFilter(f);

		// THEN
		assertThat("Tags converted to search filter", c.getSearchFilter(),
				equalTo("(|(/t=foo)(/t=bar))"));
	}

	@Test
	public void criteriaFromFilter_streamDatumFilterCommand_nodesAndSources() {
		// GIVEN
		StreamDatumFilterCommand f = new StreamDatumFilterCommand();
		f.setNodeIds(new Long[] { 1L, 2L });
		f.setSourceIds(new String[] { "a", "b" });

		// WHEN
		BasicDatumCriteria c = DatumUtils.criteriaFromFilter(f);

		// THEN
		assertThat("Node IDs copied", c.getNodeIds(), is(arrayContaining(1L, 2L)));
		assertThat("Source IDs copied", c.getSourceIds(), is(arrayContaining("a", "b")));
	}

	@Test
	public void truncateDate_local_hour() {
		// WHEN
		LocalDateTime result = DatumUtils.truncateDate(LocalDateTime.of(2020, 2, 3, 4, 5, 6),
				Aggregation.Hour);

		// THEN
		assertThat("Truncated to hour", result, equalTo(LocalDateTime.of(2020, 2, 3, 4, 0, 0)));
	}

	@Test
	public void truncateDate_local_day() {
		// WHEN
		LocalDateTime result = DatumUtils.truncateDate(LocalDateTime.of(2020, 2, 3, 4, 5, 6),
				Aggregation.Day);

		// THEN
		assertThat("Truncated to hour", result, equalTo(LocalDateTime.of(2020, 2, 3, 0, 0, 0)));
	}

	@Test
	public void truncateDate_local_month() {
		// WHEN
		LocalDateTime result = DatumUtils.truncateDate(LocalDateTime.of(2020, 2, 3, 4, 5, 6),
				Aggregation.Month);

		// THEN
		assertThat("Truncated to hour", result, equalTo(LocalDateTime.of(2020, 2, 1, 0, 0, 0)));
	}

	@Test
	public void truncateDate_local_year() {
		// WHEN
		LocalDateTime result = DatumUtils.truncateDate(LocalDateTime.of(2020, 2, 3, 4, 5, 6),
				Aggregation.Year);

		// THEN
		assertThat("Truncated to hour", result, equalTo(LocalDateTime.of(2020, 1, 1, 0, 0, 0)));
	}

	@Test
	public void truncateDate_local_other() {
		for ( Aggregation agg : EnumSet.complementOf(
				EnumSet.of(Aggregation.Hour, Aggregation.Day, Aggregation.Month, Aggregation.Year)) ) {
			try {
				DatumUtils.truncateDate(LocalDateTime.of(2020, 2, 3, 4, 5, 6), agg);
				Assert.fail("Should have thrown IllegalArgumentException for aggregation " + agg);
			} catch ( IllegalArgumentException e ) {
				// good
			}
		}
	}

	@Test
	public void virtualStreamId_1() {
		// WHEN
		UUID id = DatumUtils.virtualStreamId(-1L, "V");

		// THEN
		assertThat("Virtual ID created", id,
				equalTo(UUID.fromString("edeee74b-6334-5692-b280-869bab52d02e")));
	}

	@Test
	public void virtualStreamId_2() {
		// WHEN
		UUID id = DatumUtils.virtualStreamId(123456789L, "A");

		// THEN
		assertThat("Virtual ID created", id,
				equalTo(UUID.fromString("175f1f02-53c5-5984-b5ba-003a90b4ccd0")));
	}

}
