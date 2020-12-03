/* ==================================================================
 * PartialAggregationIntervalTests.java - 3/12/2020 3:46:46 pm
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

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.Test;
import net.solarnetwork.central.datum.v2.domain.LocalDateInterval;
import net.solarnetwork.central.datum.v2.domain.PartialAggregationInterval;
import net.solarnetwork.central.domain.Aggregation;

/**
 * Test cases for the {@link PartialAggregationInterval} class.
 * 
 * @author matt
 * @version 1.0
 */
public class PartialAggregationIntervalTests {

	@Test
	public void localRange_Year_Month_exactFull() {
		// GIVEN
		LocalDateTime start = LocalDateTime.of(2020, 1, 1, 0, 0);
		LocalDateTime end = LocalDateTime.of(2023, 1, 1, 0, 0);

		// WHEN
		List<LocalDateInterval> ranges = new PartialAggregationInterval(Aggregation.Year,
				Aggregation.Month, start, end).getIntervals();

		// THEN
		assertThat("Full range returned", ranges, hasSize(1));
		assertThat("Start date", ranges.get(0).getStart(), equalTo(start));
		assertThat("End date", ranges.get(0).getEnd(), equalTo(end));
		assertThat("Agg", ranges.get(0).getAggregation(), equalTo(Aggregation.Year));
	}

	@Test
	public void localRange_Year_Month_triple() {
		// GIVEN
		LocalDateTime start = LocalDateTime.of(2020, 3, 1, 0, 0);
		LocalDateTime end = LocalDateTime.of(2023, 3, 1, 0, 0);

		// WHEN
		List<LocalDateInterval> ranges = new PartialAggregationInterval(Aggregation.Year,
				Aggregation.Month, start, end).getIntervals();

		// THEN
		assertThat("Partial, full, partial ranges returned", ranges, hasSize(3));
		assertThat("Start start date", ranges.get(0).getStart(), equalTo(start));
		assertThat("Start end date", ranges.get(0).getEnd(),
				equalTo(LocalDateTime.of(2021, 1, 1, 0, 0)));
		assertThat("Start agg", ranges.get(0).getAggregation(), equalTo(Aggregation.Month));

		assertThat("Middle start date", ranges.get(1).getStart(),
				equalTo(LocalDateTime.of(2021, 1, 1, 0, 0)));
		assertThat("Middle end date", ranges.get(1).getEnd(),
				equalTo(LocalDateTime.of(2023, 1, 1, 0, 0)));
		assertThat("Middle agg", ranges.get(1).getAggregation(), equalTo(Aggregation.Year));

		assertThat("End start date", ranges.get(2).getStart(),
				equalTo(LocalDateTime.of(2023, 1, 1, 0, 0)));
		assertThat("End end date", ranges.get(2).getEnd(), equalTo(end));
		assertThat("End agg", ranges.get(2).getAggregation(), equalTo(Aggregation.Month));
	}

	@Test
	public void localRange_Month_Day_exactFull() {
		// GIVEN
		LocalDateTime start = LocalDateTime.of(2020, 1, 1, 0, 0);
		LocalDateTime end = LocalDateTime.of(2020, 3, 1, 0, 0);

		// WHEN
		List<LocalDateInterval> ranges = new PartialAggregationInterval(Aggregation.Month,
				Aggregation.Day, start, end).getIntervals();

		// THEN
		assertThat("Full range returned", ranges, hasSize(1));
		assertThat("Start date", ranges.get(0).getStart(), equalTo(start));
		assertThat("End date", ranges.get(0).getEnd(), equalTo(end));
		assertThat("Agg", ranges.get(0).getAggregation(), equalTo(Aggregation.Month));
	}

	@Test
	public void localRange_Month_Day_triple() {
		// GIVEN
		LocalDateTime start = LocalDateTime.of(2020, 1, 15, 0, 0);
		LocalDateTime end = LocalDateTime.of(2020, 3, 15, 0, 0);

		// WHEN
		List<LocalDateInterval> ranges = new PartialAggregationInterval(Aggregation.Month,
				Aggregation.Day, start, end).getIntervals();

		// THEN
		assertThat("Partial, full, partial ranges returned", ranges, hasSize(3));
		assertThat("Start start date", ranges.get(0).getStart(), equalTo(start));
		assertThat("Start end date", ranges.get(0).getEnd(),
				equalTo(LocalDateTime.of(2020, 2, 1, 0, 0)));
		assertThat("Start agg", ranges.get(0).getAggregation(), equalTo(Aggregation.Day));

		assertThat("Middle start date", ranges.get(1).getStart(),
				equalTo(LocalDateTime.of(2020, 2, 1, 0, 0)));
		assertThat("Middle end date", ranges.get(1).getEnd(),
				equalTo(LocalDateTime.of(2020, 3, 1, 0, 0)));
		assertThat("Middle agg", ranges.get(1).getAggregation(), equalTo(Aggregation.Month));

		assertThat("End start date", ranges.get(2).getStart(),
				equalTo(LocalDateTime.of(2020, 3, 1, 0, 0)));
		assertThat("End end date", ranges.get(2).getEnd(), equalTo(end));
		assertThat("End agg", ranges.get(2).getAggregation(), equalTo(Aggregation.Day));
	}

	@Test
	public void localRange_Month_Day_leading() {
		// GIVEN
		LocalDateTime start = LocalDateTime.of(2020, 1, 15, 0, 0);
		LocalDateTime end = LocalDateTime.of(2020, 3, 1, 0, 0);

		// WHEN
		List<LocalDateInterval> ranges = new PartialAggregationInterval(Aggregation.Month,
				Aggregation.Day, start, end).getIntervals();

		// THEN
		assertThat("Partial, full ranges returned", ranges, hasSize(2));
		assertThat("Start start date", ranges.get(0).getStart(), equalTo(start));
		assertThat("Start end date", ranges.get(0).getEnd(),
				equalTo(LocalDateTime.of(2020, 2, 1, 0, 0)));
		assertThat("Start agg", ranges.get(0).getAggregation(), equalTo(Aggregation.Day));

		assertThat("End start date", ranges.get(1).getStart(),
				equalTo(LocalDateTime.of(2020, 2, 1, 0, 0)));
		assertThat("End end date", ranges.get(1).getEnd(), equalTo(end));
		assertThat("End agg", ranges.get(1).getAggregation(), equalTo(Aggregation.Month));
	}

	@Test
	public void localRange_Month_Day_trailing() {
		// GIVEN
		LocalDateTime start = LocalDateTime.of(2020, 1, 1, 0, 0);
		LocalDateTime end = LocalDateTime.of(2020, 3, 15, 0, 0);

		// WHEN
		List<LocalDateInterval> ranges = new PartialAggregationInterval(Aggregation.Month,
				Aggregation.Day, start, end).getIntervals();

		// THEN
		assertThat("Full, partial ranges returned", ranges, hasSize(2));
		assertThat("Start start date", ranges.get(0).getStart(), equalTo(start));
		assertThat("Start end date", ranges.get(0).getEnd(),
				equalTo(LocalDateTime.of(2020, 3, 1, 0, 0)));
		assertThat("Start agg", ranges.get(0).getAggregation(), equalTo(Aggregation.Month));

		assertThat("End start date", ranges.get(1).getStart(),
				equalTo(LocalDateTime.of(2020, 3, 1, 0, 0)));
		assertThat("End end date", ranges.get(1).getEnd(), equalTo(end));
		assertThat("Start agg", ranges.get(1).getAggregation(), equalTo(Aggregation.Day));
	}

	@Test
	public void localRange_Month_Day_sub() {
		// GIVEN
		LocalDateTime start = LocalDateTime.of(2020, 1, 10, 0, 0);
		LocalDateTime end = LocalDateTime.of(2020, 1, 20, 0, 0);

		// WHEN
		List<LocalDateInterval> ranges = new PartialAggregationInterval(Aggregation.Month,
				Aggregation.Day, start, end).getIntervals();

		// THEN
		assertThat("Partial ranges returned", ranges, hasSize(1));
		assertThat("Start date", ranges.get(0).getStart(), equalTo(start));
		assertThat("End date", ranges.get(0).getEnd(), equalTo(end));
		assertThat("Agg", ranges.get(0).getAggregation(), equalTo(Aggregation.Day));
	}

	@Test
	public void localRange_Day_Hour_exactFull() {
		// GIVEN
		LocalDateTime start = LocalDateTime.of(2020, 1, 1, 0, 0);
		LocalDateTime end = LocalDateTime.of(2020, 1, 9, 0, 0);

		// WHEN
		List<LocalDateInterval> ranges = new PartialAggregationInterval(Aggregation.Day,
				Aggregation.Hour, start, end).getIntervals();

		// THEN
		assertThat("Full range returned", ranges, hasSize(1));
		assertThat("Start date", ranges.get(0).getStart(), equalTo(start));
		assertThat("End date", ranges.get(0).getEnd(), equalTo(end));
		assertThat("Agg", ranges.get(0).getAggregation(), equalTo(Aggregation.Day));
	}

	@Test
	public void localRange_Day_Hour_triple() {
		// GIVEN
		LocalDateTime start = LocalDateTime.of(2020, 1, 1, 10, 0);
		LocalDateTime end = LocalDateTime.of(2020, 1, 9, 12, 0);

		// WHEN
		List<LocalDateInterval> ranges = new PartialAggregationInterval(Aggregation.Day,
				Aggregation.Hour, start, end).getIntervals();

		// THEN
		assertThat("Partial, full, partial ranges returned", ranges, hasSize(3));
		assertThat("Start start date", ranges.get(0).getStart(), equalTo(start));
		assertThat("Start end date", ranges.get(0).getEnd(),
				equalTo(LocalDateTime.of(2020, 1, 2, 0, 0)));
		assertThat("Start agg", ranges.get(0).getAggregation(), equalTo(Aggregation.Hour));

		assertThat("Middle start date", ranges.get(1).getStart(),
				equalTo(LocalDateTime.of(2020, 1, 2, 0, 0)));
		assertThat("Middle end date", ranges.get(1).getEnd(),
				equalTo(LocalDateTime.of(2020, 1, 9, 0, 0)));
		assertThat("Middle agg", ranges.get(1).getAggregation(), equalTo(Aggregation.Day));

		assertThat("End start date", ranges.get(2).getStart(),
				equalTo(LocalDateTime.of(2020, 1, 9, 0, 0)));
		assertThat("End end date", ranges.get(2).getEnd(), equalTo(end));
		assertThat("End agg", ranges.get(2).getAggregation(), equalTo(Aggregation.Hour));
	}

	@Test
	public void localRange_Day_Hour_leading() {
		// GIVEN
		LocalDateTime start = LocalDateTime.of(2020, 1, 1, 10, 0);
		LocalDateTime end = LocalDateTime.of(2020, 1, 9, 0, 0);

		// WHEN
		List<LocalDateInterval> ranges = new PartialAggregationInterval(Aggregation.Day,
				Aggregation.Hour, start, end).getIntervals();

		// THEN
		assertThat("Partial, full ranges returned", ranges, hasSize(2));
		assertThat("Start start date", ranges.get(0).getStart(), equalTo(start));
		assertThat("Start end date", ranges.get(0).getEnd(),
				equalTo(LocalDateTime.of(2020, 1, 2, 0, 0)));
		assertThat("Start agg", ranges.get(0).getAggregation(), equalTo(Aggregation.Hour));

		assertThat("End start date", ranges.get(1).getStart(),
				equalTo(LocalDateTime.of(2020, 1, 2, 0, 0)));
		assertThat("End end date", ranges.get(1).getEnd(), equalTo(end));
		assertThat("End agg", ranges.get(1).getAggregation(), equalTo(Aggregation.Day));
	}

	@Test
	public void localRange_Day_Hour_trailing() {
		// GIVEN
		LocalDateTime start = LocalDateTime.of(2020, 1, 1, 0, 0);
		LocalDateTime end = LocalDateTime.of(2020, 1, 9, 12, 0);

		// WHEN
		List<LocalDateInterval> ranges = new PartialAggregationInterval(Aggregation.Day,
				Aggregation.Hour, start, end).getIntervals();

		// THEN
		assertThat("Full, partial ranges returned", ranges, hasSize(2));
		assertThat("Start start date", ranges.get(0).getStart(), equalTo(start));
		assertThat("Start end date", ranges.get(0).getEnd(),
				equalTo(LocalDateTime.of(2020, 1, 9, 0, 0)));
		assertThat("Start agg", ranges.get(0).getAggregation(), equalTo(Aggregation.Day));

		assertThat("End start date", ranges.get(1).getStart(),
				equalTo(LocalDateTime.of(2020, 1, 9, 0, 0)));
		assertThat("End end date", ranges.get(1).getEnd(), equalTo(end));
		assertThat("End agg", ranges.get(1).getAggregation(), equalTo(Aggregation.Hour));
	}

	@Test
	public void localRange_Day_Hour_sub() {
		// GIVEN
		LocalDateTime start = LocalDateTime.of(2020, 1, 1, 10, 0);
		LocalDateTime end = LocalDateTime.of(2020, 1, 1, 12, 0);

		// WHEN
		List<LocalDateInterval> ranges = new PartialAggregationInterval(Aggregation.Day,
				Aggregation.Hour, start, end).getIntervals();

		// THEN
		assertThat("Partial ranges returned", ranges, hasSize(1));
		assertThat("Start date", ranges.get(0).getStart(), equalTo(start));
		assertThat("End date", ranges.get(0).getEnd(), equalTo(end));
		assertThat("Agg", ranges.get(0).getAggregation(), equalTo(Aggregation.Hour));
	}

}
