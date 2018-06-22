/* ==================================================================
 * UsageRecordTests.java - 19/06/2018 6:38:12 AM
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
import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Test;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.central.user.billing.killbill.KillbillUtils;
import net.solarnetwork.central.user.billing.killbill.domain.UsageRecord;

/**
 * Test cases for the {@link UsageRecord} class.
 * 
 * @author matt
 * @version 1.0
 */
public class UsageRecordTests {

	private ObjectMapper objectMapper;

	@Before
	public void setup() {
		objectMapper = KillbillUtils.defaultObjectMapper();
	}

	@Test
	public void stringValueNullData() {
		UsageRecord r = new UsageRecord(null, null);
		String s = r.toString();
		assertThat("String value", s, equalTo("UsageRecord{recordDate=null,amount=null}"));
	}

	@Test
	public void stringValue() {
		LocalDate date = new LocalDate(2018, 06, 19);
		UsageRecord r = new UsageRecord(date, BigDecimal.ONE);
		String s = r.toString();
		assertThat("String value", s, equalTo("UsageRecord{recordDate=2018-06-19,amount=1}"));
	}

	@Test
	public void stringValueDecimalValues() {
		LocalDate date = new LocalDate(2018, 06, 19);
		UsageRecord r = new UsageRecord(date, new BigDecimal("1.234"));
		String s = r.toString();
		assertThat("String value", s, equalTo("UsageRecord{recordDate=2018-06-19,amount=1.234}"));
	}

	@Test
	public void jsonValue() throws Exception {
		LocalDate date = new LocalDate(2018, 06, 19);
		UsageRecord r = new UsageRecord(date, BigDecimal.ONE);
		String s = objectMapper.writeValueAsString(r);
		assertThat("JSON value", s, equalTo("{\"recordDate\":\"2018-06-19\",\"amount\":\"1\"}"));
	}

}
