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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
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
import net.solarnetwork.central.datum.v2.domain.StaleAggregateDatum;
import net.solarnetwork.central.datum.v2.domain.StreamKindPK;
import net.solarnetwork.domain.datum.Aggregation;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.GeneralDatumMetadata;

/**
 * Test cases for the {@link JdbcDatumAuxiliaryEntityDao} class.
 * 
 * @author matt
 * @version 1.1
 */
public class JdbcDatumAuxiliaryEntityDao_GenericDaoTests extends BaseDatumJdbcTestSupport {

	private JdbcDatumAuxiliaryEntityDao dao;

	protected DatumAuxiliaryEntity lastDatum;

	@Before
	public void setup() {
		dao = new JdbcDatumAuxiliaryEntityDao(jdbcTemplate);
	}

	private static DatumAuxiliaryEntity testAux() {
		DatumSamples sf = new DatumSamples();
		sf.putAccumulatingSampleValue("foo", 1);

		DatumSamples ss = new DatumSamples();
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
		lastDatum = datum;

		assertThat("Returned ID matches given ID", id, is(equalTo(datum.getId())));
		List<StaleAggregateDatum> stale = DatumDbUtils.listStaleAggregateDatum(jdbcTemplate);
		assertThat("One stale row created", stale, hasSize(1));
		assertThat("Stale row for deleted datum key hour", stale.get(0).getId(),
				is(equalTo(new StreamKindPK(datum.getStreamId(),
						datum.getTimestamp().truncatedTo(ChronoUnit.HOURS),
						Aggregation.Hour.getKey()))));
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
		jdbcTemplate.update("delete from solardatm.agg_stale_datm"); // clear out to verify re-insert
		DatumAuxiliaryEntity before = dao.get(lastDatum.getId());

		DatumSamples f = lastDatum.getSamplesFinal();
		f.putAccumulatingSampleValue("ww", 11);
		DatumSamples s = lastDatum.getSamplesStart();
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
		List<StaleAggregateDatum> stale = DatumDbUtils.listStaleAggregateDatum(jdbcTemplate);
		assertThat("One stale row created", stale, hasSize(1));
		assertThat("Stale row for deleted datum key hour", stale.get(0).getId(),
				is(equalTo(new StreamKindPK(changed.getStreamId(),
						changed.getTimestamp().truncatedTo(ChronoUnit.HOURS),
						Aggregation.Hour.getKey()))));
	}

	@Test
	public void delete() {
		// GIVEN
		saveNew();
		List<DatumAuxiliary> data = DatumDbUtils.listDatumAuxiliary(jdbcTemplate);
		assertThat("Row exists in DB", data, hasSize(1));
		assertDatumAuxiliary("Row matches saved entity", data.get(0), lastDatum);

		// WHEN
		dao.delete(lastDatum);

		// THEN
		data = DatumDbUtils.listDatumAuxiliary(jdbcTemplate);
		assertThat("Row removed from DB", data, hasSize(0));

		List<StaleAggregateDatum> stale = DatumDbUtils.listStaleAggregateDatum(jdbcTemplate);
		assertThat("One stale row created", stale, hasSize(1));
		assertThat("Stale row for deleted datum key hour", stale.get(0).getId(),
				is(equalTo(new StreamKindPK(lastDatum.getStreamId(),
						lastDatum.getTimestamp().truncatedTo(ChronoUnit.HOURS),
						Aggregation.Hour.getKey()))));
	}

}
