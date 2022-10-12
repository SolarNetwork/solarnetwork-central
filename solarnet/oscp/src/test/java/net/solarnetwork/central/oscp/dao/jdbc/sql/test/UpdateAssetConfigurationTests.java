/* ==================================================================
 * UpdateAssetConfigurationTests.java - 5/09/2022 12:53:10 pm
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
import static net.solarnetwork.central.oscp.dao.jdbc.test.OscpJdbcTestUtils.newAssetConfiguration;
import static net.solarnetwork.central.test.CommonTestUtils.equalToTextResource;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.oscp.dao.jdbc.sql.UpdateAssetConfiguration;
import net.solarnetwork.central.oscp.domain.AssetConfiguration;
import net.solarnetwork.codec.JsonUtils;

/**
 * Test cases for the {@link UpdateAssetConfiguration} class.
 * 
 * @author matt
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
public class UpdateAssetConfigurationTests {

	private final Logger log = LoggerFactory.getLogger(getClass());

	@Mock
	private Connection con;

	@Mock
	private PreparedStatement stmt;

	@Mock
	private Array iPropNamesArray;

	@Mock
	private Array ePropNamesArray;

	@Captor
	private ArgumentCaptor<String> sqlCaptor;

	private void givenPrepStatement() throws SQLException {
		given(con.prepareStatement(any())).willReturn(stmt);
	}

	private void givenSetInstantaneousPropNamesArrayParameter(String[] value) throws SQLException {
		given(con.createArrayOf(eq("text"), aryEq(value))).willReturn(iPropNamesArray);
	}

	private void givenSetEnergyPropNamesArrayParameter(String[] value) throws SQLException {
		given(con.createArrayOf(eq("text"), aryEq(value))).willReturn(ePropNamesArray);
	}

	private void thenPrepStatement(PreparedStatement result, UserLongCompositePK id,
			AssetConfiguration conf) throws SQLException {
		Timestamp ts = Timestamp.from(conf.getModified());
		int p = 0;
		then(result).should().setTimestamp(++p, ts);
		then(result).should().setBoolean(++p, conf.isEnabled());
		then(result).should().setString(++p, conf.getName());
		then(result).should().setObject(++p, conf.getCapacityGroupId());
		then(result).should().setString(++p, conf.getIdentifier());
		then(result).should().setInt(++p, conf.getAudience().getCode());
		then(result).should().setObject(++p, conf.getNodeId());
		then(result).should().setString(++p, conf.getSourceId());
		then(result).should().setInt(++p, conf.getCategory().getCode());
		then(result).should().setInt(++p, conf.getPhase().getCode());
		then(result).should().setArray(++p, iPropNamesArray);
		then(result).should().setInt(++p, conf.getInstantaneous().getStatisticType().getCode());
		then(result).should().setInt(++p, conf.getInstantaneous().getUnit().getCode());
		then(result).should().setBigDecimal(++p, conf.getInstantaneous().getMultiplier());
		then(result).should().setArray(++p, ePropNamesArray);
		then(result).should().setInt(++p, conf.getEnergy().getStatisticType().getCode());
		then(result).should().setInt(++p, conf.getEnergy().getUnit().getCode());
		then(result).should().setBigDecimal(++p, conf.getEnergy().getMultiplier());
		then(result).should().setInt(++p, conf.getEnergy().getType().getCode());
		then(result).should().setInt(++p, conf.getEnergy().getDirection().getCode());
		if ( conf.getServiceProps() != null ) {
			then(result).should().setString(++p, JsonUtils.getJSONString(conf.getServiceProps(), "{}"));
		} else {
			then(result).should().setNull(++p, Types.VARCHAR);
		}
		then(result).should().setObject(++p, id.getUserId());
		then(result).should().setObject(++p, id.getEntityId());
	}

	@Test
	public void sql() {
		// GIVEN
		Long userId = randomUUID().getMostSignificantBits();
		UserLongCompositePK id = new UserLongCompositePK(userId, randomUUID().getMostSignificantBits());
		AssetConfiguration conf = newAssetConfiguration(userId, randomUUID().getMostSignificantBits(),
				Instant.now());
		conf.setModified(Instant.now());

		// WHEN
		String sql = new UpdateAssetConfiguration(id, conf).getSql();

		// THEN
		log.debug("Generated SQL:\n{}", sql);
		assertThat("SQL matches", sql,
				equalToTextResource("update-asset-conf.sql", TestSqlResources.class, SQL_COMMENT));
	}

	@Test
	public void prep() throws SQLException {
		// GIVEN
		Long userId = randomUUID().getMostSignificantBits();
		UserLongCompositePK id = new UserLongCompositePK(userId, randomUUID().getMostSignificantBits());
		AssetConfiguration conf = newAssetConfiguration(userId, randomUUID().getMostSignificantBits(),
				Instant.now()).copyWithId(id);
		conf.setModified(Instant.now());

		// GIVEN
		givenPrepStatement();
		givenSetInstantaneousPropNamesArrayParameter(conf.getInstantaneous().getPropertyNames());
		givenSetEnergyPropNamesArrayParameter(conf.getEnergy().getPropertyNames());

		// WHEN
		PreparedStatement result = new UpdateAssetConfiguration(id, conf).createPreparedStatement(con);

		// THEN
		then(con).should().prepareStatement(sqlCaptor.capture());
		log.debug("Generated SQL:\n{}", sqlCaptor.getValue());
		assertThat("Generated SQL", sqlCaptor.getValue(),
				equalToTextResource("update-asset-conf.sql", TestSqlResources.class, SQL_COMMENT));
		assertThat("Statement returned", result, sameInstance(stmt));
		thenPrepStatement(result, conf.getId(), conf);
	}

}
