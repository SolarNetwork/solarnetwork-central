/* ==================================================================
 * DbMoveDatumAuxiliaryTests.java - 28/11/2020 6:10:09 pm
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

import static java.util.Collections.singleton;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import java.sql.CallableStatement;
import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.CallableStatementCallback;
import net.solarnetwork.central.datum.dao.jdbc.test.BaseDatumJdbcTestSupport;
import net.solarnetwork.central.datum.domain.DatumAuxiliaryType;
import net.solarnetwork.central.datum.v2.dao.DatumAuxiliaryEntity;
import net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils;
import net.solarnetwork.central.datum.v2.dao.jdbc.sql.MoveDatumAuxiliary;
import net.solarnetwork.central.datum.v2.domain.DatumAuxiliary;
import net.solarnetwork.central.datum.v2.domain.DatumAuxiliaryPK;
import net.solarnetwork.domain.GeneralDatumMetadata;
import net.solarnetwork.domain.GeneralDatumSamples;

/**
 * Test cases for the {@code solardatm.move_datm_aux} database procedure.
 * 
 * @author matt
 * @version 1.0
 */
public class DbMoveDatumAuxiliaryTests extends BaseDatumJdbcTestSupport {

	private boolean callMoveDatumAuxiliary(DatumAuxiliaryPK from, DatumAuxiliary to) {
		return jdbcTemplate.execute(new MoveDatumAuxiliary(from, to),
				new CallableStatementCallback<Boolean>() {

					@Override
					public Boolean doInCallableStatement(CallableStatement cs)
							throws SQLException, DataAccessException {
						cs.execute();
						return cs.getBoolean(1);
					}
				});
	}

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
	public void move_id() {
		// GIVEN
		DatumAuxiliaryEntity aux = testAux();
		DatumDbUtils.insertDatumAuxiliary(log, jdbcTemplate, singleton(aux));

		// WHEN
		DatumAuxiliaryEntity newAux = new DatumAuxiliaryEntity(UUID.randomUUID(), Instant.now(),
				DatumAuxiliaryType.Reset, null, aux.getSamplesFinal(), aux.getSamplesStart(),
				aux.getNotes(), aux.getMetadata());
		boolean result = callMoveDatumAuxiliary(aux.getId(), newAux);

		// THEN
		assertThat("Moved", result, Matchers.equalTo(true));
		List<DatumAuxiliary> rows = DatumDbUtils.listDatumAuxiliary(jdbcTemplate);
		assertThat("One row in DB", rows, hasSize(1));
		DatumTestUtils.assertDatumAuxiliary("Moved", rows.get(0), newAux);
	}

}
