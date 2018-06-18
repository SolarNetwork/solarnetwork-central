/* ==================================================================
 * UsageUnitRecordTests.java - 19/06/2018 6:26:14 AM
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

package net.solarnetwork.central.user.billing.killbill.domain.test;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Test;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.central.user.billing.killbill.KillbillUtils;
import net.solarnetwork.central.user.billing.killbill.domain.UsageRecord;
import net.solarnetwork.central.user.billing.killbill.domain.UsageUnitRecord;

/**
 * Test cases for the {@link UsageUnitRecord} class.
 * 
 * @author matt
 * @version 1.0
 */
public class UsageUnitRecordTests {

	private ObjectMapper objectMapper;

	@Before
	public void setup() {
		objectMapper = KillbillUtils.defaultObjectMapper();
	}

	@Test
	public void stringValueNullData() {
		UsageUnitRecord r = new UsageUnitRecord(null, null);
		String s = r.toString();
		assertThat("String value", s, equalTo("UsageUnitRecord{type=null,amount=0}"));
	}

	@Test
	public void stringValueEmptyList() {
		UsageUnitRecord r = new UsageUnitRecord("foo", Collections.emptyList());
		String s = r.toString();
		assertThat("String value", s, equalTo("UsageUnitRecord{type=foo,amount=0}"));
	}

	@Test
	public void stringValueSingleton() {
		LocalDate date = new LocalDate(2018, 06, 19);
		UsageUnitRecord r = new UsageUnitRecord("foo",
				Collections.singletonList(new UsageRecord(date, BigDecimal.ONE)));
		String s = r.toString();
		assertThat("String value", s, equalTo("UsageUnitRecord{type=foo,amount=1}"));
	}

	@Test
	public void stringValueList() {
		LocalDate date = new LocalDate(2018, 06, 19);
		UsageUnitRecord r = new UsageUnitRecord("foo",
				Arrays.asList(new UsageRecord(date, BigDecimal.ONE),
						new UsageRecord(date.minusDays(1), BigDecimal.ONE),
						new UsageRecord(date.minusDays(2), BigDecimal.ONE)));
		String s = r.toString();
		assertThat("String value", s, equalTo("UsageUnitRecord{type=foo,amount=3}"));
	}

	@Test
	public void stringValueDecimalValues() {
		LocalDate date = new LocalDate(2018, 06, 19);
		UsageUnitRecord r = new UsageUnitRecord("foo",
				Arrays.asList(new UsageRecord(date, new BigDecimal("1.234")),
						new UsageRecord(date.minusDays(1), new BigDecimal("2.345")),
						new UsageRecord(date.minusDays(2), new BigDecimal("3.456"))));
		String s = r.toString();
		assertThat("String value", s, equalTo("UsageUnitRecord{type=foo,amount=6}"));
	}

	@Test
	public void jsonValue() throws Exception {
		LocalDate date = new LocalDate(2018, 06, 19);
		UsageUnitRecord r = new UsageUnitRecord("foo",
				Arrays.asList(new UsageRecord(date, BigDecimal.ONE),
						new UsageRecord(date.minusDays(1), BigDecimal.TEN)));
		String s = objectMapper.writeValueAsString(r);
		assertThat("JSON value", s,
				equalTo("{\"unitType\":\"foo\",\"usageRecords\":["
						+ "{\"recordDate\":\"2018-06-19\",\"amount\":\"1\"}"
						+ ",{\"recordDate\":\"2018-06-18\",\"amount\":\"10\"}" + "]}"));
	}

}
