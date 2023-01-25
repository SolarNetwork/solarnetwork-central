/* ==================================================================
 * JdbcDatumEntityDao_GenericDaoTests.java - 19/11/2020 5:24:58 pm
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

import static net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils.listAuditDatum;
import static net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils.listDatum;
import static net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils.listStaleAggregateDatum;
import static net.solarnetwork.domain.datum.DatumProperties.propertiesOf;
import static net.solarnetwork.util.NumberUtils.decimalArray;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.central.datum.dao.jdbc.test.BaseDatumJdbcTestSupport;
import net.solarnetwork.central.datum.v2.dao.DatumEntity;
import net.solarnetwork.central.datum.v2.dao.jdbc.JdbcDatumEntityDao;
import net.solarnetwork.central.datum.v2.domain.AuditDatum;
import net.solarnetwork.central.datum.v2.domain.Datum;
import net.solarnetwork.central.datum.v2.domain.DatumPK;
import net.solarnetwork.central.datum.v2.domain.StaleAggregateDatum;
import net.solarnetwork.dao.GenericDao;
import net.solarnetwork.domain.datum.Aggregation;
import net.solarnetwork.domain.datum.DatumProperties;

/**
 * Test cases for the {@link JdbcDatumEntityDao} class' implementation of
 * {@link GenericDao}.
 * 
 * @author matt
 * @version 1.0
 */
public class JdbcDatumEntityDao_StreamDatumTests extends BaseDatumJdbcTestSupport {

	private JdbcDatumEntityDao dao;

	protected DatumEntity lastDatum;

	@Before
	public void setup() {
		dao = new JdbcDatumEntityDao(jdbcTemplate);
	}

	@Test
	public void store_new() {
		// GIVEN
		DatumEntity datum = new DatumEntity(UUID.randomUUID(),
				Instant.now().truncatedTo(ChronoUnit.MILLIS),
				Instant.now().truncatedTo(ChronoUnit.MILLIS),
				DatumProperties.propertiesOf(decimalArray("1.23", "2.34"), decimalArray("3.45"),
						new String[] { "On" }, new String[] { "a" }));

		// WHEN
		DatumPK id = dao.store(datum);

		// THEN
		assertThat("ID returned", id, is(notNullValue()));
		assertThat("ID stream ID is preserved", id.getStreamId(), is(equalTo(datum.getStreamId())));
		assertThat("ID timestamp is created date", id.getTimestamp(), is(equalTo(datum.getCreated())));

		List<StaleAggregateDatum> staleAgg = listStaleAggregateDatum(jdbcTemplate, Aggregation.Hour);
		assertThat("Stale hourly aggregate datum stored in DB", staleAgg, hasSize(1));
		assertThat("Stale hourly aggregate stream ID from datum", staleAgg.get(0).getStreamId(),
				is(equalTo(id.getStreamId())));
		assertThat("Stale hourly aggregate timestamp truncated to hour from datum",
				staleAgg.get(0).getTimestamp(),
				is(equalTo(id.getTimestamp().truncatedTo(ChronoUnit.HOURS))));

		List<AuditDatum> audit = listAuditDatum(jdbcTemplate, Aggregation.Hour);
		assertThat("Hourly audit datum stored in DB", audit, hasSize(1));
		assertThat("Hourly audit stream ID from datum", audit.get(0).getStreamId(),
				is(equalTo(id.getStreamId())));
		assertThat("Hourly audit timestamp truncated to hour from datum", audit.get(0).getTimestamp(),
				is(equalTo(id.getTimestamp().truncatedTo(ChronoUnit.HOURS))));
		assertThat("Hourly audit datum count added", audit.get(0).getDatumCount(), is(equalTo(1L)));
		assertThat("Hourly audit prop count from datum", audit.get(0).getDatumPropertyCount(),
				is(equalTo(5L)));
		lastDatum = datum;
	}

	@Test
	public void store_new2() {
		// GIVEN
		DatumEntity datum = new DatumEntity(UUID.randomUUID(),
				Instant.now().truncatedTo(ChronoUnit.MILLIS),
				Instant.now().truncatedTo(ChronoUnit.MILLIS),
				DatumProperties.propertiesOf(decimalArray("3.21", "4.32", "5.43"), decimalArray("5.43"),
						null, new String[] { "b" }));

		// WHEN
		DatumPK id = dao.store(datum);

		// THEN
		assertThat("ID returned", id, is(notNullValue()));
		assertThat("ID stream ID is preserved", id.getStreamId(), is(equalTo(datum.getStreamId())));
		assertThat("ID timestamp is created date", id.getTimestamp(), is(equalTo(datum.getCreated())));

		List<StaleAggregateDatum> staleAgg = listStaleAggregateDatum(jdbcTemplate, Aggregation.Hour);
		assertThat("Stale hourly aggregate datum stored in DB", staleAgg, hasSize(1));
		assertThat("Stale hourly aggregate stream ID from datum", staleAgg.get(0).getStreamId(),
				is(equalTo(id.getStreamId())));
		assertThat("Stale hourly aggregate timestamp truncated to hour from datum",
				staleAgg.get(0).getTimestamp(),
				is(equalTo(id.getTimestamp().truncatedTo(ChronoUnit.HOURS))));

		List<AuditDatum> audit = listAuditDatum(jdbcTemplate, Aggregation.Hour);
		assertThat("Hourly audit datum stored in DB", audit, hasSize(1));
		assertThat("Hourly audit stream ID from datum", audit.get(0).getStreamId(),
				is(equalTo(id.getStreamId())));
		assertThat("Hourly audit timestamp truncated to hour from datum", audit.get(0).getTimestamp(),
				is(equalTo(id.getTimestamp().truncatedTo(ChronoUnit.HOURS))));
		assertThat("Hourly audit datum count added", audit.get(0).getDatumCount(), is(equalTo(1L)));
		assertThat("Hourly audit prop count from datum", audit.get(0).getDatumPropertyCount(),
				is(equalTo(5L)));
	}

	@Test
	public void store_update() throws IOException {
		// GIVEN
		store_new();
		DatumEntity datum = new DatumEntity(lastDatum.getStreamId(), lastDatum.getTimestamp(),
				Instant.now(), DatumProperties.propertiesOf(decimalArray("3.21", "4.32", "5.43"),
						decimalArray("5.43"), null, new String[] { "b" }));

		// WHEN
		DatumPK id = dao.store(datum);

		// THEN
		assertThat("ID returned", id, is(notNullValue()));
		assertThat("ID is unchanged", id, is(equalTo(lastDatum.getId())));

		List<Datum> rows = listDatum(jdbcTemplate);
		assertThat("Datum stored in DB", rows, hasSize(1));
		assertThat("Datum ID matches returned value", rows.get(0).getId(), equalTo(id));

		assertThat("Datum properties", rows.get(0).getProperties(),
				equalTo(propertiesOf(decimalArray("3.21", "4.32", "5.43"), decimalArray("5.43"), null,
						new String[] { "b" })));

		List<StaleAggregateDatum> staleAgg = listStaleAggregateDatum(jdbcTemplate, Aggregation.Hour);
		assertThat("Stale hourly aggregate datum stored in DB", staleAgg, hasSize(1));
		assertThat("Stale hourly aggregate stream ID from datum", staleAgg.get(0).getStreamId(),
				is(equalTo(id.getStreamId())));
		assertThat("Stale hourly aggregate timestamp truncated to hour from datum",
				staleAgg.get(0).getTimestamp(),
				is(equalTo(id.getTimestamp().truncatedTo(ChronoUnit.HOURS))));

		List<AuditDatum> audit = listAuditDatum(jdbcTemplate, Aggregation.Hour);
		assertThat("Hourly audit datum stored in DB", audit, hasSize(1));
		assertThat("Hourly audit stream ID from datum", audit.get(0).getStreamId(),
				is(equalTo(id.getStreamId())));
		assertThat("Hourly audit timestamp truncated to hour from datum", audit.get(0).getTimestamp(),
				is(equalTo(id.getTimestamp().truncatedTo(ChronoUnit.HOURS))));
		assertThat("Hourly audit datum count incremented", audit.get(0).getDatumCount(),
				is(equalTo(2L)));
		assertThat("Hourly audit prop count sum from datum insert and update",
				audit.get(0).getDatumPropertyCount(), is(equalTo(10L)));
	}

	private void assertSame(DatumEntity expected, DatumEntity entity) {
		assertThat("DatumEntity should exist", entity, notNullValue());
		assertThat("Stream ID", entity.getStreamId(), equalTo(expected.getStreamId()));
		assertThat("Timestamp", entity.getTimestamp(), equalTo(expected.getTimestamp()));
		assertThat("Received", entity.getReceived(), equalTo(expected.getReceived()));
		assertThat("Properties", entity.getProperties(), equalTo(expected.getProperties()));
	}

	@Test
	public void store_get() {
		store_new();
		DatumEntity datum = dao.get(lastDatum.getId());
		assertSame(lastDatum, datum);
	}

	@Test
	public void store_withNullPropertyValues() throws IOException {
		// GIVEN
		DatumEntity datum = new DatumEntity(UUID.randomUUID(),
				Instant.now().truncatedTo(ChronoUnit.MILLIS), Instant.now(),
				DatumProperties.propertiesOf(decimalArray(null, null, "1.23", null),
						decimalArray("2.34", null, null, "3.45"),
						new String[] { null, "On", null, "Off" }, new String[] { "a", "b" }));

		// WHEN
		dao.store(datum);

		// THEN
		List<Datum> rows = listDatum(jdbcTemplate);
		assertThat("Datum stored in table", rows, hasSize(1));
		DatumEntity d = (DatumEntity) rows.get(0);

		assertSame(datum, d);
	}

}
