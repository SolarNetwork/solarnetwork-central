/* ==================================================================
 * JdbcDatumAuxiliaryEntityDao_GenericDaoTests.java - 28/11/2020 10:39:38 am
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

import static net.solarnetwork.central.datum.v2.dao.jdbc.test.DatumTestUtils.assertDatumAuxiliary;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.central.datum.dao.jdbc.test.BaseDatumJdbcTestSupport;
import net.solarnetwork.central.datum.domain.DatumAuxiliaryType;
import net.solarnetwork.central.datum.v2.dao.DatumAuxiliaryEntity;
import net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils;
import net.solarnetwork.central.datum.v2.dao.jdbc.JdbcDatumAuxiliaryEntityDao;
import net.solarnetwork.central.datum.v2.domain.DatumAuxiliary;
import net.solarnetwork.central.datum.v2.domain.DatumAuxiliaryPK;
import net.solarnetwork.domain.GeneralDatumMetadata;
import net.solarnetwork.domain.GeneralDatumSamples;

/**
 * Test cases for the {@link JdbcDatumAuxiliaryEntityDao} class.
 * 
 * @author matt
 * @version 1.0
 */
public class JdbcDatumAuxiliaryEntityDao_GenericDaoTests extends BaseDatumJdbcTestSupport {

	private JdbcDatumAuxiliaryEntityDao dao;

	protected DatumAuxiliaryEntity lastDatum;

	@Before
	public void setup() {
		dao = new JdbcDatumAuxiliaryEntityDao(jdbcTemplate);
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
	public void saveNew() {
		DatumAuxiliaryEntity datum = testAux();
		DatumAuxiliaryPK id = dao.save(datum);
		assertThat("Returned ID matches given ID", id, equalTo(datum.getId()));
		lastDatum = datum;
	}

	@Test
	public void getByPrimaryKey() {
		saveNew();
		DatumAuxiliaryEntity result = dao.get(lastDatum.getId());
		assertDatumAuxiliary("Get by PK", result, lastDatum);
	}

	@Test
	public void update() {
		// GIVEN
		saveNew();
		DatumAuxiliaryEntity before = dao.get(lastDatum.getId());

		GeneralDatumSamples f = lastDatum.getSamplesFinal();
		f.putAccumulatingSampleValue("ww", 11);
		GeneralDatumSamples s = lastDatum.getSamplesStart();
		s.putAccumulatingSampleValue("ww", 1010);
		GeneralDatumMetadata m = new GeneralDatumMetadata(lastDatum.getMetadata());
		m.putInfoValue("zim", "zam");

		// WHEN
		DatumAuxiliaryEntity changed = new DatumAuxiliaryEntity(before.getId(), null, f, s, "New note.",
				m);
		DatumAuxiliaryPK id = dao.save(changed);

		// THEN
		assertThat("Returned ID matches given ID", id, equalTo(changed.getId()));
		DatumAuxiliaryEntity after = dao.get(changed.getId());
		assertThat("Updated date not before original updated date",
				after.getUpdated().isBefore(before.getUpdated()), equalTo(false));

		assertDatumAuxiliary("Updated values saved", after, changed);
	}

	@Test
	public void delete() {
		// GIVEN
		saveNew();
		List<DatumAuxiliary> data = DatumDbUtils.listDatumAuxiliary(jdbcTemplate);
		assertThat("Row exists in DB", data, hasSize(1));
		assertDatumAuxiliary("Row matches saved entityt", data.get(0), lastDatum);

		// WHEN
		dao.delete(lastDatum);

		// THEN
		data = DatumDbUtils.listDatumAuxiliary(jdbcTemplate);
		assertThat("Row removed from DB", data, hasSize(0));
	}

}
