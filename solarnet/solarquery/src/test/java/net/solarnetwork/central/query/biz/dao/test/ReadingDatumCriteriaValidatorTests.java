/* ==================================================================
 * ReadingDatumCriteriaValidatorTests.java - 30/04/2021 10:12:06 AM
 * 
 * Copyright 2021 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.query.biz.dao.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.EnumSet;
import org.junit.Test;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import net.solarnetwork.central.datum.domain.DatumReadingType;
import net.solarnetwork.central.datum.v2.dao.BasicDatumCriteria;
import net.solarnetwork.central.domain.Aggregation;
import net.solarnetwork.central.query.biz.dao.ReadingDatumCriteriaValidator;

/**
 * Test cases for the {@link ReadingDatumCriteriaValidator} class.
 * 
 * @author matt
 * @version 2.0
 */
public class ReadingDatumCriteriaValidatorTests {

	@Test
	public void queryDiff_localDateRange() {
		// GIVEN
		BasicDatumCriteria c = new BasicDatumCriteria();
		c.setReadingType(DatumReadingType.Difference);
		c.setLocalStartDate(LocalDateTime.now().minusDays(1));
		c.setLocalEndDate(LocalDateTime.now());
		c.setNodeId(1L);
		c.setSourceId("s");

		// WHEN
		Errors errors = new BindException(c, "filter");
		new ReadingDatumCriteriaValidator().validate(c, errors);

		// THEN
		assertThat("No validation errors", errors.hasErrors(), equalTo(false));
	}

	@Test
	public void queryDiff_dateRange() {
		// GIVEN
		BasicDatumCriteria c = new BasicDatumCriteria();
		c.setReadingType(DatumReadingType.Difference);
		c.setStartDate(Instant.now().minusSeconds(600));
		c.setEndDate(Instant.now());
		c.setNodeId(1L);
		c.setSourceId("s");

		// WHEN
		Errors errors = new BindException(c, "filter");
		new ReadingDatumCriteriaValidator().validate(c, errors);

		// THEN
		assertThat("No validation errors", errors.hasErrors(), equalTo(false));
	}

	@Test
	public void queryDiff_missingDateRange() {
		// GIVEN
		BasicDatumCriteria c = new BasicDatumCriteria();
		c.setReadingType(DatumReadingType.Difference);
		c.setNodeId(1L);
		c.setSourceId("s");

		// WHEN
		Errors errors = new BindException(c, "filter");
		new ReadingDatumCriteriaValidator().validate(c, errors);

		// THEN
		assertThat("Validation errors", errors.hasErrors(), equalTo(true));
		ObjectError oe = errors.getGlobalError();
		assertThat("Date range error", oe.getCode(), equalTo("error.filter.dateRange.required"));
	}

	private void assertField(String msg, Errors errors, String name, String errorCode) {
		FieldError fe = errors.getFieldError(name);
		assertThat(msg + " field", fe, notNullValue());
		assertThat(msg + " code", fe.getCode(), equalTo(errorCode));
	}

	@Test
	public void queryDiff_mostRecent() {
		// GIVEN
		BasicDatumCriteria c = new BasicDatumCriteria();
		c.setReadingType(DatumReadingType.Difference);
		c.setStartDate(Instant.now().minusSeconds(600));
		c.setEndDate(Instant.now());
		c.setNodeId(1L);
		c.setSourceId("s");
		c.setMostRecent(true);

		// WHEN
		Errors errors = new BindException(c, "filter");
		new ReadingDatumCriteriaValidator().validate(c, errors);

		// THEN
		assertField("Most recent", errors, "mostRecent", "error.filter.reading.mostRecent.invalid");
	}

	@Test
	public void queryDiff_agg_invalid() {
		for ( Aggregation agg : EnumSet.complementOf(
				EnumSet.of(Aggregation.None, Aggregation.Hour, Aggregation.Day, Aggregation.Month)) ) {
			// GIVEN
			BasicDatumCriteria c = new BasicDatumCriteria();
			c.setReadingType(DatumReadingType.Difference);
			c.setStartDate(Instant.now().minusSeconds(600));
			c.setEndDate(Instant.now());
			c.setNodeId(1L);
			c.setSourceId("s");
			c.setAggregation(agg);

			// WHEN
			Errors errors = new BindException(c, "filter");
			new ReadingDatumCriteriaValidator().validate(c, errors);

			// THEN
			assertField(agg + " aggregation not allowed", errors, "aggregation",
					"error.filter.reading.aggregation.invalid");
		}
	}

	@Test
	public void queryDiff_agg_mostRecent_ok() {
		for ( Aggregation agg : EnumSet.of(Aggregation.Hour, Aggregation.Day, Aggregation.Month) ) {
			// GIVEN
			BasicDatumCriteria c = new BasicDatumCriteria();
			c.setReadingType(DatumReadingType.Difference);
			c.setStartDate(Instant.now().minusSeconds(600));
			c.setEndDate(Instant.now());
			c.setNodeId(1L);
			c.setSourceId("s");
			c.setMostRecent(true);
			c.setAggregation(agg);

			// WHEN
			Errors errors = new BindException(c, "filter");
			new ReadingDatumCriteriaValidator().validate(c, errors);

			// THEN
			assertThat(agg + " aggregation allowed with mostRecent", errors.hasErrors(), equalTo(false));
		}
	}

	@Test
	public void queryDiff_agg_invalidReadingType() {
		for ( Aggregation agg : EnumSet.of(Aggregation.Hour, Aggregation.Day, Aggregation.Month) ) {
			for ( DatumReadingType readingType : EnumSet
					.complementOf(EnumSet.of(DatumReadingType.Difference)) ) {
				// GIVEN
				BasicDatumCriteria c = new BasicDatumCriteria();
				c.setReadingType(readingType);
				c.setStartDate(Instant.now().minusSeconds(600));
				c.setEndDate(Instant.now());
				c.setNodeId(1L);
				c.setSourceId("s");
				c.setAggregation(agg);

				// WHEN
				Errors errors = new BindException(c, "filter");
				new ReadingDatumCriteriaValidator().validate(c, errors);

				// THEN
				assertField(readingType + " reading type with " + agg + " aggregation not allowed",
						errors, "readingType", "error.filter.reading.aggregation.readingType");
			}
		}
	}

}
