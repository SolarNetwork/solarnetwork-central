/* ==================================================================
 * StoreDatumAuxiliaryTests.java - 28/11/2020 9:14:35 am
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

package net.solarnetwork.central.datum.v2.dao.jdbc.sql.test;

import static net.solarnetwork.central.datum.v2.dao.jdbc.test.DatumTestUtils.equalToTextResource;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.central.datum.domain.DatumAuxiliaryType;
import net.solarnetwork.central.datum.v2.dao.DatumAuxiliaryEntity;
import net.solarnetwork.central.datum.v2.dao.jdbc.sql.StoreDatumAuxiliary;
import net.solarnetwork.domain.GeneralDatumMetadata;
import net.solarnetwork.domain.GeneralDatumSamples;
import net.solarnetwork.util.JsonUtils;

/**
 * Test cases for the {@link StoreDatumAuxiliary} class.
 * 
 * @author matt
 * @version 1.0
 */
public class StoreDatumAuxiliaryTests {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private static DatumAuxiliaryEntity testAux() {
		GeneralDatumSamples sf = new GeneralDatumSamples();
		sf.putAccumulatingSampleValue("foo", 1);

		GeneralDatumSamples ss = new GeneralDatumSamples();
		ss.putAccumulatingSampleValue("foo", 10);

		GeneralDatumMetadata meta = new GeneralDatumMetadata();
		meta.putInfoValue("bim", "pow");
		return new DatumAuxiliaryEntity(UUID.randomUUID(), Instant.now().truncatedTo(ChronoUnit.HOURS),
				DatumAuxiliaryType.Reset, null, sf, ss, "Note.", meta);
	}

	@Test
	public void sql_store() {
		// GIVEN
		DatumAuxiliaryEntity d = testAux();

		// WHEN
		String sql = new StoreDatumAuxiliary(d).getSql();

		// THEN
		assertThat("SQL matches", sql,
				equalToTextResource("store-datum-aux.sql", TestSqlResources.class));
	}

	@Test
	public void prep_store() throws SQLException {
		// GIVEN
		DatumAuxiliaryEntity d = testAux();

		Connection con = EasyMock.createMock(Connection.class);
		CallableStatement stmt = EasyMock.createMock(CallableStatement.class);

		// stream_id, ts, atype, notes, jdata_af, jdata_as, jmeta

		Capture<String> sqlCaptor = new Capture<>();
		expect(con.prepareCall(capture(sqlCaptor))).andReturn(stmt);

		stmt.setObject(1, d.getStreamId(), Types.OTHER);

		Capture<Timestamp> tsCaptor = new Capture<>();
		stmt.setTimestamp(eq(2), capture(tsCaptor));

		stmt.setString(3, DatumAuxiliaryType.Reset.name());
		stmt.setString(4, d.getNotes());
		stmt.setString(5, JsonUtils.getJSONString(d.getSamplesFinal(), null));
		stmt.setString(6, JsonUtils.getJSONString(d.getSamplesStart(), null));
		stmt.setString(7, JsonUtils.getJSONString(d.getMetadata(), null));

		// WHEN
		replay(con, stmt);
		CallableStatement result = new StoreDatumAuxiliary(d).createCallableStatement(con);

		// THEN
		log.debug("Generated SQL:\n{}", sqlCaptor.getValue());
		assertThat("SQL matches", sqlCaptor.getValue(),
				equalToTextResource("store-datum-aux.sql", TestSqlResources.class));
		assertThat("Connection statement returned", result, sameInstance(stmt));
		assertThat("Timestamp prarameter", tsCaptor.getValue(),
				equalTo(Timestamp.from(d.getTimestamp())));
		verify(con, stmt);
	}

}
