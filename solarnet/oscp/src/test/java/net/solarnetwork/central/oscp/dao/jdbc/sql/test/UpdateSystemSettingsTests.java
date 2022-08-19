/* ==================================================================
 * UpdateSystemSettingsTests.java - 19/08/2022 3:51:51 pm
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.oscp.dao.jdbc.sql.test;

import static java.util.UUID.randomUUID;
import static net.solarnetwork.central.common.dao.jdbc.sql.CommonSqlUtils.SQL_COMMENT;
import static net.solarnetwork.central.test.CommonTestUtils.equalToTextResource;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.oscp.dao.jdbc.sql.UpdateSystemSettings;
import net.solarnetwork.central.oscp.domain.MeasurementStyle;
import net.solarnetwork.central.oscp.domain.OscpRole;
import net.solarnetwork.central.oscp.domain.SystemSettings;
import net.solarnetwork.domain.CodedValue;

/**
 * Test cases for the {@link UpdateSystemSettings} class.
 * 
 * @author matt
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
public class UpdateSystemSettingsTests {

	private final Logger log = LoggerFactory.getLogger(getClass());

	@Mock
	private Connection con;

	@Mock
	private PreparedStatement stmt;

	@Mock
	private Array measurementStylesArray;

	@Captor
	private ArgumentCaptor<String> sqlCaptor;

	private void givenPrepStatement() throws SQLException {
		given(con.prepareStatement(any())).willReturn(stmt);
	}

	private void givenSetMeasurementStylesArrayParameter(Set<MeasurementStyle> styles)
			throws SQLException {
		Integer[] codes = styles.stream().map(CodedValue::getCode).toArray(Integer[]::new);
		given(con.createArrayOf(eq("integer"), aryEq(codes))).willReturn(measurementStylesArray);
	}

	private void thenPrepStatement(PreparedStatement stmt, OscpRole role, UserLongCompositePK id,
			SystemSettings settings) throws SQLException {
		then(stmt).should().setTimestamp(eq(1), any());
		then(stmt).should().setObject(2, settings.heartbeatSeconds(), Types.SMALLINT);
		then(stmt).should().setArray(eq(3), same(measurementStylesArray));
		then(stmt).should().setObject(4, id.getUserId(), Types.BIGINT);
		then(stmt).should().setObject(5, id.getEntityId(), Types.BIGINT);
	}

	private SystemSettings newSettings() {
		return new SystemSettings(123,
				EnumSet.of(MeasurementStyle.Continuous, MeasurementStyle.Intermittent));
	}

	@Test
	public void unassignedId() {
		// GIVEN
		UserLongCompositePK id = UserLongCompositePK
				.unassignedEntityIdKey(randomUUID().getMostSignificantBits());
		SystemSettings settings = newSettings();

		// WHEN
		assertThrows(IllegalArgumentException.class, () -> {
			new UpdateSystemSettings(OscpRole.CapacityProvider, id, settings).getSql();
		});
	}

	@Test
	public void capacityProvider_sql() {
		// GIVEN
		UserLongCompositePK id = new UserLongCompositePK(randomUUID().getMostSignificantBits(),
				randomUUID().getMostSignificantBits());
		SystemSettings settings = newSettings();

		// WHEN
		String sql = new UpdateSystemSettings(OscpRole.CapacityProvider, id, settings).getSql();

		// THEN
		log.debug("Generated SQL:\n{}", sql);
		assertThat("SQL matches", sql, equalToTextResource("update-capacity-provider-settings.sql",
				TestSqlResources.class, SQL_COMMENT));
	}

	@Test
	public void capacityProvider_prep() throws SQLException {
		// GIVEN
		UserLongCompositePK id = new UserLongCompositePK(randomUUID().getMostSignificantBits(),
				randomUUID().getMostSignificantBits());
		SystemSettings settings = newSettings();

		givenPrepStatement();
		givenSetMeasurementStylesArrayParameter(settings.measurementStyles());

		// WHEN
		PreparedStatement result = new UpdateSystemSettings(OscpRole.CapacityProvider, id, settings)
				.createPreparedStatement(con);

		// THEN
		then(con).should().prepareStatement(sqlCaptor.capture());
		log.debug("Generated SQL:\n{}", sqlCaptor.getValue());
		assertThat("Generated SQL", sqlCaptor.getValue(), equalToTextResource(
				"update-capacity-provider-settings.sql", TestSqlResources.class, SQL_COMMENT));
		assertThat("Statement returned", result, sameInstance(stmt));
		thenPrepStatement(result, OscpRole.CapacityProvider, id, settings);
	}

}
