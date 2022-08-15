/* ==================================================================
 * InsertAssetConfigurationTests.java - 12/08/2022 3:29:58 pm
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
import static net.solarnetwork.central.oscp.domain.MeasurementUnit.KILO_MULTIPLIER;
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
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.oscp.dao.jdbc.sql.InsertAssetConfiguration;
import net.solarnetwork.central.oscp.domain.AssetCategory;
import net.solarnetwork.central.oscp.domain.AssetConfiguration;
import net.solarnetwork.central.oscp.domain.EnergyType;
import net.solarnetwork.central.oscp.domain.MeasurementUnit;
import net.solarnetwork.central.oscp.domain.Phase;
import net.solarnetwork.codec.JsonUtils;

/**
 * Test cases for the {@link InsertAssetConfiguration} class.
 * 
 * @author matt
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
public class InsertAssetConfigurationTests {

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
		given(con.prepareStatement(any(), eq(Statement.RETURN_GENERATED_KEYS))).willReturn(stmt);
	}

	private void givenSetInstantaneousPropNamesArrayParameter(String[] value) throws SQLException {
		given(con.createArrayOf(eq("text"), aryEq(value))).willReturn(iPropNamesArray);
	}

	private void givenSetEnergyPropNamesArrayParameter(String[] value) throws SQLException {
		given(con.createArrayOf(eq("text"), aryEq(value))).willReturn(ePropNamesArray);
	}

	private AssetConfiguration createAssetConfiguration(Long userId, Long providerId, Long optimizerId) {
		AssetConfiguration conf = new AssetConfiguration(UserLongCompositePK.unassignedEntityIdKey(userId),
				Instant.now());
		conf.setEnabled(true);
		conf.setName(randomUUID().toString());
		conf.setCapacityGroupId(randomUUID().getMostSignificantBits());
		conf.setNodeId(randomUUID().getMostSignificantBits());
		conf.setSourceId(randomUUID().toString());
		conf.setCategory(AssetCategory.Charging);
		conf.setInstantaneousPropertyNames(new String[] { "watts" });
		conf.setInstantaneousUnit(MeasurementUnit.kW);
		conf.setInstantaneousMultiplier(KILO_MULTIPLIER);
		conf.setInstantaneousPhase(Phase.All);
		conf.setEnergyPropertyNames(new String[] { "wattHours" });
		conf.setEnergyUnit(MeasurementUnit.kWh);
		conf.setEnergyMultiplier(KILO_MULTIPLIER);
		conf.setEnergyType(EnergyType.Total);
		conf.setServiceProps(Collections.singletonMap("foo", randomUUID().toString()));
		return conf;
	}

	private void thenPrepStatement(PreparedStatement result, Long userId, AssetConfiguration conf)
			throws SQLException {
		Timestamp ts = Timestamp.from(conf.getCreated());
		then(result).should().setTimestamp(1, ts);
		then(result).should().setTimestamp(2, ts);
		then(result).should().setObject(3, userId);
		then(result).should().setBoolean(4, conf.isEnabled());
		then(result).should().setString(5, conf.getName());
		then(result).should().setObject(6, conf.getCapacityGroupId());
		then(result).should().setObject(7, conf.getNodeId());
		then(result).should().setString(8, conf.getSourceId());
		then(result).should().setInt(9, conf.getCategory().getCode());
		then(result).should().setArray(10, iPropNamesArray);
		then(result).should().setInt(11, conf.getInstantaneousUnit().getCode());
		then(result).should().setBigDecimal(12, conf.getInstantaneousMultiplier());
		then(result).should().setInt(13, conf.getInstantaneousPhase().getCode());
		then(result).should().setArray(14, ePropNamesArray);
		then(result).should().setInt(15, conf.getEnergyUnit().getCode());
		then(result).should().setBigDecimal(16, conf.getEnergyMultiplier());
		then(result).should().setInt(17, conf.getEnergyType().getCode());
		if ( conf.getServiceProps() != null ) {
			then(result).should().setString(18, JsonUtils.getJSONString(conf.getServiceProps(), "{}"));
		} else {
			then(result).should().setNull(18, Types.VARCHAR);
		}
	}

	@Test
	public void sql() {
		// GIVEN
		AssetConfiguration conf = createAssetConfiguration(randomUUID().getMostSignificantBits(),
				randomUUID().getMostSignificantBits(), randomUUID().getMostSignificantBits());

		// WHEN
		Long userId = randomUUID().getMostSignificantBits();
		String sql = new InsertAssetConfiguration(userId, conf).getSql();

		// THEN
		log.debug("Generated SQL:\n{}", sql);
		assertThat("SQL matches", sql,
				equalToTextResource("insert-asset-conf.sql", TestSqlResources.class, SQL_COMMENT));
	}

	@Test
	public void prep() throws SQLException {
		// GIVEN
		AssetConfiguration conf = createAssetConfiguration(randomUUID().getMostSignificantBits(),
				randomUUID().getMostSignificantBits(), randomUUID().getMostSignificantBits());

		// GIVEN
		givenPrepStatement();
		givenSetInstantaneousPropNamesArrayParameter(conf.getInstantaneousPropertyNames());
		givenSetEnergyPropNamesArrayParameter(conf.getEnergyPropertyNames());

		// WHEN
		Long userId = randomUUID().getMostSignificantBits();
		PreparedStatement result = new InsertAssetConfiguration(userId, conf)
				.createPreparedStatement(con);

		// THEN
		then(con).should().prepareStatement(sqlCaptor.capture(), eq(Statement.RETURN_GENERATED_KEYS));
		log.debug("Generated SQL:\n{}", sqlCaptor.getValue());
		assertThat("Generated SQL", sqlCaptor.getValue(),
				equalToTextResource("insert-asset-conf.sql", TestSqlResources.class, SQL_COMMENT));
		assertThat("Statement returned", result, sameInstance(stmt));
		thenPrepStatement(result, userId, conf);
	}

}
