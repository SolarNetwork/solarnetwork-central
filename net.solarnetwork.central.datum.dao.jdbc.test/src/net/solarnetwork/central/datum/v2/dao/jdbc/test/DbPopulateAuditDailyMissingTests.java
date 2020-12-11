/* ==================================================================
 * DbPopulateAuditDailyMissingTests.java - 12/12/2020 9:27:30 am
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

package net.solarnetwork.central.datum.v2.dao.jdbc.test;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;
import net.solarnetwork.central.datum.dao.jdbc.test.BaseDatumJdbcTestSupport;

/**
 * Test cases for the {@code solardatm.populate_audit_datm_daily_missing}
 * database procedure.
 * 
 * @author matt
 * @version 1.0
 */
public class DbPopulateAuditDailyMissingTests extends BaseDatumJdbcTestSupport {

	private Long populateAuditDatumDailyMissing(LocalDate date) {
		return jdbcTemplate.execute(new ConnectionCallback<Long>() {

			@Override
			public Long doInConnection(Connection con) throws SQLException, DataAccessException {
				log.debug("Populating audit datum daily missing for {}", date);
				try (CallableStatement stmt = con
						.prepareCall("{? = call solardatm.populate_audit_datm_daily_missing(?)}")) {
					stmt.registerOutParameter(1, Types.BIGINT);
					stmt.setObject(2, date, Types.DATE);
					stmt.execute();
					long result = stmt.getLong(1);
					log.debug("Populated {} audit datum:\n{}", result);
					return result;
				}
			}
		});
	}

	@Test
	public void todo() {
		// GIVEN
		LocalDate date = LocalDate.now();

		// WHEN
		Long result = populateAuditDatumDailyMissing(date);

		// THEN
		assertThat("Count returned", result, notNullValue());
		Assert.fail("TODO");
	}

}
