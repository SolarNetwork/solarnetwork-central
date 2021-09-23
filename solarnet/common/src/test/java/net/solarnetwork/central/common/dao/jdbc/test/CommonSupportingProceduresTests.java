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
import static org.hamcrest.MatcherAssert.assertThat;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import javax.sql.DataSource;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import net.solarnetwork.central.test.AbstractJdbcDaoTestSupport;

/**
 * Test cases for common supporting database functions.
 * 
 * @author matt
 * @version 2.0
 */
public class CommonSupportingProceduresTests extends AbstractJdbcDaoTestSupport {

	private JdbcTemplate jdbcTemplate;

	@Override
	@Autowired
	public void setDataSource(DataSource dataSource) {
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}

	@Test
	public void formatRfc1123DateSmallDay() {
		Instant date = LocalDateTime.of(2019, 2, 4, 22, 30).toInstant(ZoneOffset.UTC);
		String val = jdbcTemplate.queryForObject("SELECT solarcommon.to_rfc1123_utc(?)", String.class,
				Timestamp.from(date));
		assertThat("Formatted date", val, equalTo("Mon, 04 Feb 2019 22:30:00 GMT"));
	}

	@Test
	public void formatRfc1123DateBigDay() {
		Instant date = LocalDateTime.of(2019, 2, 11, 22, 30).toInstant(ZoneOffset.UTC);
		String val = jdbcTemplate.queryForObject("SELECT solarcommon.to_rfc1123_utc(?)", String.class,
				Timestamp.from(date));
		assertThat("Formatted date", val, equalTo("Mon, 11 Feb 2019 22:30:00 GMT"));
	}

	@Test
	public void formatRfc1123DateSmallHourAndMinutesAndSeconds() {
		Instant date = LocalDateTime.of(2019, 2, 4, 2, 3, 4).toInstant(ZoneOffset.UTC);
		String val = jdbcTemplate.queryForObject("SELECT solarcommon.to_rfc1123_utc(?)", String.class,
				Timestamp.from(date));
		assertThat("Formatted date", val, equalTo("Mon, 04 Feb 2019 02:03:04 GMT"));
	}

	@Test
	public void formatRfc1123DateBigHourAndMinutesAndSeconds() {
		Instant date = LocalDateTime.of(2019, 2, 4, 22, 33, 44).toInstant(ZoneOffset.UTC);
		String val = jdbcTemplate.queryForObject("SELECT solarcommon.to_rfc1123_utc(?)", String.class,
				Timestamp.from(date));
		assertThat("Formatted date", val, equalTo("Mon, 04 Feb 2019 22:33:44 GMT"));
	}
}
