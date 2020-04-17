/* ==================================================================
 * PartialRangesTests.java - 17/04/2020 1:47:19 pm
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

package net.solarnetwork.central.datum.dao.mybatis.test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import java.util.List;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;
import org.junit.Test;
import net.solarnetwork.central.datum.dao.mybatis.MyBatisGeneralNodeDatumDao;
import net.solarnetwork.central.datum.dao.mybatis.NodeSourceRange;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.domain.Aggregation;

/**
 * Test cases for computing partial ranges.
 * 
 * @author matt
 * @version 1.0
 */
public class PartialRangesTests {

	@Test
	public void localRange_Month_Day_exactFull() {
		// GIVEN
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setLocalStartDate(new LocalDateTime(2020, 1, 1, 0, 0));
		filter.setLocalEndDate(new LocalDateTime(2020, 3, 1, 0, 0));
		filter.setAggregate(Aggregation.Month);
		filter.setPartialAggregation(Aggregation.Day);

		// WHEN
		List<NodeSourceRange> ranges = MyBatisGeneralNodeDatumDao.partialAggregationRanges(filter);

		// THEN
		assertThat("Full range returned", ranges, hasSize(1));
		assertThat("Start start date", ranges.get(0).getLocalStartDate(),
				equalTo(filter.getLocalStartDate()));
		assertThat("End partial end date", ranges.get(0).getLocalEndDate(),
				equalTo(filter.getLocalEndDate()));
	}

	@Test
	public void localRange_Month_Day_triple() {
		// GIVEN
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setLocalStartDate(new LocalDateTime(2020, 1, 15, 0, 0));
		filter.setLocalEndDate(new LocalDateTime(2020, 3, 15, 0, 0));
		filter.setAggregate(Aggregation.Month);
		filter.setPartialAggregation(Aggregation.Day);

		// WHEN
		List<NodeSourceRange> ranges = MyBatisGeneralNodeDatumDao.partialAggregationRanges(filter);

		// THEN
		assertThat("Partial, full, partial ranges returned", ranges, hasSize(3));
		assertThat("Start start date", ranges.get(0).getLocalStartDate(),
				equalTo(filter.getLocalStartDate()));
		assertThat("Start end date", ranges.get(0).getLocalEndDate(),
				equalTo(new LocalDateTime(2020, 2, 1, 0, 0)));
		assertThat("Middle start date", ranges.get(1).getLocalStartDate(),
				equalTo(new LocalDateTime(2020, 2, 1, 0, 0)));
		assertThat("Middle end date", ranges.get(1).getLocalEndDate(),
				equalTo(new LocalDateTime(2020, 3, 1, 0, 0)));
		assertThat("End start date", ranges.get(2).getLocalStartDate(),
				equalTo(new LocalDateTime(2020, 3, 1, 0, 0)));
		assertThat("End end date", ranges.get(2).getLocalEndDate(), equalTo(filter.getLocalEndDate()));
	}

	@Test
	public void localRange_Month_Day_leading() {
		// GIVEN
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setLocalStartDate(new LocalDateTime(2020, 1, 15, 0, 0));
		filter.setLocalEndDate(new LocalDateTime(2020, 3, 1, 0, 0));
		filter.setAggregate(Aggregation.Month);
		filter.setPartialAggregation(Aggregation.Day);

		// WHEN
		List<NodeSourceRange> ranges = MyBatisGeneralNodeDatumDao.partialAggregationRanges(filter);

		// THEN
		assertThat("Partial, full ranges returned", ranges, hasSize(2));
		assertThat("Start start date", ranges.get(0).getLocalStartDate(),
				equalTo(filter.getLocalStartDate()));
		assertThat("Start end date", ranges.get(0).getLocalEndDate(),
				equalTo(new LocalDateTime(2020, 2, 1, 0, 0)));
		assertThat("End start date", ranges.get(1).getLocalStartDate(),
				equalTo(new LocalDateTime(2020, 2, 1, 0, 0)));
		assertThat("End end date", ranges.get(1).getLocalEndDate(), equalTo(filter.getLocalEndDate()));
	}

	@Test
	public void localRange_Month_Day_trailing() {
		// GIVEN
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setLocalStartDate(new LocalDateTime(2020, 1, 1, 0, 0));
		filter.setLocalEndDate(new LocalDateTime(2020, 3, 15, 0, 0));
		filter.setAggregate(Aggregation.Month);
		filter.setPartialAggregation(Aggregation.Day);

		// WHEN
		List<NodeSourceRange> ranges = MyBatisGeneralNodeDatumDao.partialAggregationRanges(filter);

		// THEN
		assertThat("Full, partial ranges returned", ranges, hasSize(2));
		assertThat("Start start date", ranges.get(0).getLocalStartDate(),
				equalTo(filter.getLocalStartDate()));
		assertThat("Start end date", ranges.get(0).getLocalEndDate(),
				equalTo(new LocalDateTime(2020, 3, 1, 0, 0)));
		assertThat("End start date", ranges.get(1).getLocalStartDate(),
				equalTo(new LocalDateTime(2020, 3, 1, 0, 0)));
		assertThat("End end date", ranges.get(1).getLocalEndDate(), equalTo(filter.getLocalEndDate()));
	}

	@Test
	public void localRange_Month_Day_sub() {
		// GIVEN
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setLocalStartDate(new LocalDateTime(2020, 1, 10, 0, 0));
		filter.setLocalEndDate(new LocalDateTime(2020, 1, 20, 0, 0));
		filter.setAggregate(Aggregation.Month);
		filter.setPartialAggregation(Aggregation.Day);

		// WHEN
		List<NodeSourceRange> ranges = MyBatisGeneralNodeDatumDao.partialAggregationRanges(filter);

		// THEN
		assertThat("Partial ranges returned", ranges, hasSize(1));
		assertThat("Start start date", ranges.get(0).getLocalStartDate(),
				equalTo(filter.getLocalStartDate()));
		assertThat("End end date", ranges.get(0).getLocalEndDate(), equalTo(filter.getLocalEndDate()));
	}

	@Test
	public void range_Month_Day_exactFull() {
		// GIVEN
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setStartDate(new DateTime(2020, 1, 1, 0, 0, DateTimeZone.UTC));
		filter.setEndDate(new DateTime(2020, 3, 1, 0, 0, DateTimeZone.UTC));
		filter.setAggregate(Aggregation.Month);
		filter.setPartialAggregation(Aggregation.Day);

		// WHEN
		List<NodeSourceRange> ranges = MyBatisGeneralNodeDatumDao.partialAggregationRanges(filter);

		// THEN
		assertThat("Full range returned", ranges, hasSize(1));
		assertThat("Start start date", ranges.get(0).getStartDate(), equalTo(filter.getStartDate()));
		assertThat("End partial end date", ranges.get(0).getEndDate(), equalTo(filter.getEndDate()));
	}

	@Test
	public void range_Month_Day_triple() {
		// GIVEN
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setStartDate(new DateTime(2020, 1, 15, 0, 0, DateTimeZone.UTC));
		filter.setEndDate(new DateTime(2020, 3, 15, 0, 0, DateTimeZone.UTC));
		filter.setAggregate(Aggregation.Month);
		filter.setPartialAggregation(Aggregation.Day);

		// WHEN
		List<NodeSourceRange> ranges = MyBatisGeneralNodeDatumDao.partialAggregationRanges(filter);

		// THEN
		assertThat("Partial, full, partial ranges returned", ranges, hasSize(3));
		assertThat("Start start date", ranges.get(0).getStartDate(), equalTo(filter.getStartDate()));
		assertThat("Start end date", ranges.get(0).getEndDate(),
				equalTo(new DateTime(2020, 2, 1, 0, 0, DateTimeZone.UTC)));
		assertThat("Middle start date", ranges.get(1).getStartDate(),
				equalTo(new DateTime(2020, 2, 1, 0, 0, DateTimeZone.UTC)));
		assertThat("Middle end date", ranges.get(1).getEndDate(),
				equalTo(new DateTime(2020, 3, 1, 0, 0, DateTimeZone.UTC)));
		assertThat("End start date", ranges.get(2).getStartDate(),
				equalTo(new DateTime(2020, 3, 1, 0, 0, DateTimeZone.UTC)));
		assertThat("End end date", ranges.get(2).getEndDate(), equalTo(filter.getEndDate()));
	}

	@Test
	public void range_Month_Day_leading() {
		// GIVEN
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setStartDate(new DateTime(2020, 1, 15, 0, 0, DateTimeZone.UTC));
		filter.setEndDate(new DateTime(2020, 3, 1, 0, 0, DateTimeZone.UTC));
		filter.setAggregate(Aggregation.Month);
		filter.setPartialAggregation(Aggregation.Day);

		// WHEN
		List<NodeSourceRange> ranges = MyBatisGeneralNodeDatumDao.partialAggregationRanges(filter);

		// THEN
		assertThat("Partial, full ranges returned", ranges, hasSize(2));
		assertThat("Start start date", ranges.get(0).getStartDate(), equalTo(filter.getStartDate()));
		assertThat("Start end date", ranges.get(0).getEndDate(),
				equalTo(new DateTime(2020, 2, 1, 0, 0, DateTimeZone.UTC)));
		assertThat("End start date", ranges.get(1).getStartDate(),
				equalTo(new DateTime(2020, 2, 1, 0, 0, DateTimeZone.UTC)));
		assertThat("End end date", ranges.get(1).getEndDate(), equalTo(filter.getEndDate()));
	}

	@Test
	public void range_Month_Day_trailing() {
		// GIVEN
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setStartDate(new DateTime(2020, 1, 1, 0, 0, DateTimeZone.UTC));
		filter.setEndDate(new DateTime(2020, 3, 15, 0, 0, DateTimeZone.UTC));
		filter.setAggregate(Aggregation.Month);
		filter.setPartialAggregation(Aggregation.Day);

		// WHEN
		List<NodeSourceRange> ranges = MyBatisGeneralNodeDatumDao.partialAggregationRanges(filter);

		// THEN
		assertThat("Full, partial ranges returned", ranges, hasSize(2));
		assertThat("Start start date", ranges.get(0).getStartDate(), equalTo(filter.getStartDate()));
		assertThat("Start end date", ranges.get(0).getEndDate(),
				equalTo(new DateTime(2020, 3, 1, 0, 0, DateTimeZone.UTC)));
		assertThat("End start date", ranges.get(1).getStartDate(),
				equalTo(new DateTime(2020, 3, 1, 0, 0, DateTimeZone.UTC)));
		assertThat("End end date", ranges.get(1).getEndDate(), equalTo(filter.getEndDate()));
	}

	@Test
	public void range_Month_Day_sub() {
		// GIVEN
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setStartDate(new DateTime(2020, 1, 10, 0, 0, DateTimeZone.UTC));
		filter.setEndDate(new DateTime(2020, 1, 20, 0, 0, DateTimeZone.UTC));
		filter.setAggregate(Aggregation.Month);
		filter.setPartialAggregation(Aggregation.Day);

		// WHEN
		List<NodeSourceRange> ranges = MyBatisGeneralNodeDatumDao.partialAggregationRanges(filter);

		// THEN
		assertThat("Partial ranges returned", ranges, hasSize(1));
		assertThat("Start start date", ranges.get(0).getStartDate(), equalTo(filter.getStartDate()));
		assertThat("End end date", ranges.get(0).getEndDate(), equalTo(filter.getEndDate()));
	}

}
