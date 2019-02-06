/* ==================================================================
 * CommonSupportingProceduresTests.java - 5/02/2019 12:42:01 pm
 * 
 * Copyright 2019 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.common.dao.jdbc.test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import java.sql.Timestamp;
import javax.sql.DataSource;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import net.solarnetwork.central.test.AbstractCentralTransactionalTest;

/**
 * Test cases for common supporting database functions.
 * 
 * @author matt
 * @version 1.0
 */
public class CommonSupportingProceduresTests extends AbstractCentralTransactionalTest {

	private JdbcTemplate jdbcTemplate;

	@Override
	@Autowired
	public void setDataSource(DataSource dataSource) {
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}

	@Test
	public void formatRfc1123DateSmallDay() {
		DateTime date = new DateTime(2019, 2, 4, 22, 30, DateTimeZone.UTC);
		String val = jdbcTemplate.queryForObject("SELECT solarcommon.to_rfc1123_utc(?)", String.class,
				new Timestamp(date.getMillis()));
		assertThat("Formatted date", val, equalTo("Mon, 04 Feb 2019 22:30:00 GMT"));
	}

	@Test
	public void formatRfc1123DateBigDay() {
		DateTime date = new DateTime(2019, 2, 11, 22, 30, DateTimeZone.UTC);
		String val = jdbcTemplate.queryForObject("SELECT solarcommon.to_rfc1123_utc(?)", String.class,
				new Timestamp(date.getMillis()));
		assertThat("Formatted date", val, equalTo("Mon, 11 Feb 2019 22:30:00 GMT"));
	}

	@Test
	public void formatRfc1123DateSmallHourAndMinutesAndSeconds() {
		DateTime date = new DateTime(2019, 2, 4, 2, 3, 4, DateTimeZone.UTC);
		String val = jdbcTemplate.queryForObject("SELECT solarcommon.to_rfc1123_utc(?)", String.class,
				new Timestamp(date.getMillis()));
		assertThat("Formatted date", val, equalTo("Mon, 04 Feb 2019 02:03:04 GMT"));
	}

	@Test
	public void formatRfc1123DateBigHourAndMinutesAndSeconds() {
		DateTime date = new DateTime(2019, 2, 4, 22, 33, 44, DateTimeZone.UTC);
		String val = jdbcTemplate.queryForObject("SELECT solarcommon.to_rfc1123_utc(?)", String.class,
				new Timestamp(date.getMillis()));
		assertThat("Formatted date", val, equalTo("Mon, 04 Feb 2019 22:33:44 GMT"));
	}
}
