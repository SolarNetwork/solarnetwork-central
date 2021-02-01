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

import static net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils.listDatum;
import static net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils.listLocationMetadata;
import static net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils.listNodeMetadata;
import static net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils.loadJsonDatumResource;
import static net.solarnetwork.central.datum.v2.domain.DatumProperties.propertiesOf;
import static net.solarnetwork.util.NumberUtils.decimalArray;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.central.datum.dao.jdbc.test.BaseDatumJdbcTestSupport;
import net.solarnetwork.central.datum.domain.GeneralLocationDatum;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.datum.v2.dao.DatumEntity;
import net.solarnetwork.central.datum.v2.dao.jdbc.JdbcDatumEntityDao;
import net.solarnetwork.central.datum.v2.domain.Datum;
import net.solarnetwork.central.datum.v2.domain.DatumPK;
import net.solarnetwork.central.datum.v2.domain.DatumProperties;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamMetadata;
import net.solarnetwork.central.datum.v2.support.DatumUtils;
import net.solarnetwork.dao.GenericDao;
import net.solarnetwork.domain.GeneralLocationDatumSamples;
import net.solarnetwork.domain.GeneralNodeDatumSamples;
import net.solarnetwork.util.JodaDateUtils;

/**
 * Test cases for the {@link JdbcDatumEntityDao} class' implementation of
 * {@link GenericDao}.
 * 
 * @author matt
 * @version 1.0
 */
public class JdbcDatumEntityDao_GenericDaoTests extends BaseDatumJdbcTestSupport {

	private JdbcDatumEntityDao dao;

	protected DatumEntity lastDatum;

	@Before
	public void setup() {
		dao = new JdbcDatumEntityDao(jdbcTemplate);
	}

	@Test
	public void saveNew() {
		DatumEntity datum = new DatumEntity(UUID.randomUUID(), Instant.now(), Instant.now(),
				DatumProperties.propertiesOf(
						new BigDecimal[] { new BigDecimal("1.23"), new BigDecimal("2.34") },
						new BigDecimal[] { new BigDecimal("3.45") }, new String[] { "On" }, null));
		DatumPK id = dao.save(datum);
		assertNotNull(id);
		lastDatum = datum;
	}

	@Test
	public void store_newStream() throws IOException {
		// GIVEN
		GeneralNodeDatum datum = loadJsonDatumResource("test-datum-01.txt", getClass()).get(0);

		// WHEN
		DatumPK id = dao.store(datum);

		// THEN
		assertThat("ID returned", id, notNullValue());
		assertThat("ID has stream ID", id.getStreamId(), notNullValue());
		assertThat("ID has expected timestamp", id.getTimestamp(),
				equalTo(JodaDateUtils.fromJodaToInstant(datum.getCreated())));

		List<Datum> rows = listDatum(jdbcTemplate);
		assertThat("Datum stored in DB", rows, hasSize(1));
		assertThat("Datum ID matches returned value", rows.get(0).getId(), equalTo(id));

		List<ObjectDatumStreamMetadata> metas = listNodeMetadata(jdbcTemplate);
		assertThat("Stream metadata created", metas, hasSize(1));
		assertThat("Metadata for stream ID", metas.get(0).getStreamId(), equalTo(id.getStreamId()));
		assertThat("Metadata for node ID", metas.get(0).getObjectId(), equalTo(1L));
		assertThat("Metadata for source ID", metas.get(0).getSourceId(), equalTo("a"));
		assertThat("Datum properties", rows.get(0).getProperties(),
				equalTo(propertiesOf(decimalArray("1.2", "2.1"), decimalArray("100"), null, null)));
	}

	@Test
	public void store_veryBigValues() {
		// GIVEN
		GeneralNodeDatum datum = new GeneralNodeDatum();
		datum.setNodeId(1L);
		datum.setSourceId("a");
		datum.setCreated(JodaDateUtils.toJoda(ZonedDateTime.now()));
		GeneralNodeDatumSamples s = new GeneralNodeDatumSamples();
		datum.setSamples(s);
		s.putInstantaneousSampleValue("watts", 498475890235787897L);
		s.putInstantaneousSampleValue("floating",
				new BigDecimal("293487590845639845728947589237.49087"));
		s.putAccumulatingSampleValue("watt_hours", 39309570293789380L);
		s.putAccumulatingSampleValue("very_big", new BigInteger("93475092039478209375027350293523957"));
		DatumPK id = dao.store(datum);

		DatumEntity entity = dao.get(id);
		ObjectDatumStreamMetadata meta = listNodeMetadata(jdbcTemplate).get(0);
		GeneralNodeDatum storedDatum = DatumUtils.toGeneralNodeDatum(entity, meta);
		// compare property by property because database stores as DECIMAL
		assertThat("Datum stored very big watts",
				storedDatum.getSamples().getInstantaneousSampleLong("watts"),
				equalTo(s.getInstantaneousSampleLong("watts")));
		assertThat("Datum stored very big watts",
				storedDatum.getSamples().getInstantaneousSampleBigDecimal("floating"),
				equalTo(s.getInstantaneousSampleBigDecimal("floating")));
		assertThat("Datum stored very big watt_hours",
				storedDatum.getSamples().getAccumulatingSampleLong("watt_hours"),
				equalTo(s.getAccumulatingSampleLong("watt_hours")));
		assertThat("Datum stored very big very_big",
				storedDatum.getSamples().getAccumulatingSampleBigDecimal("very_big"),
				equalTo(s.getAccumulatingSampleBigDecimal("very_big")));
	}

	@Test
	public void store_entireStream() throws IOException {
		// GIVEN
		List<GeneralNodeDatum> datums = loadJsonDatumResource("test-datum-01.txt", getClass());

		// WHEN
		List<DatumPK> ids = new ArrayList<>(datums.size());
		for ( GeneralNodeDatum datum : datums ) {
			DatumPK id = dao.store(datum);
			assertThat("ID returned", id, notNullValue());
			assertThat("ID has stream ID", id.getStreamId(), notNullValue());
			assertThat("ID has expected timestamp", id.getTimestamp(),
					equalTo(JodaDateUtils.fromJodaToInstant(datum.getCreated())));
			if ( !ids.isEmpty() ) {
				assertThat("Same stream ID returned for subsequent store", id.getStreamId(),
						equalTo(ids.get(0).getStreamId()));
			}
			ids.add(id);
		}

		// THEN
		List<Datum> rows = listDatum(jdbcTemplate);
		assertThat("Datum stored in DB", rows, hasSize(datums.size()));
		int i = 0;
		for ( Datum row : rows ) {
			assertThat("Datum ID matches returned value", row.getId(), equalTo(ids.get(i)));
			i++;
		}
		List<ObjectDatumStreamMetadata> metas = listNodeMetadata(jdbcTemplate);
		assertThat("Stream metadata created", metas, hasSize(1));
		assertThat("Metadata for stream ID", metas.get(0).getStreamId(),
				equalTo(ids.get(0).getStreamId()));
		assertThat("Metadata for node ID", metas.get(0).getObjectId(), equalTo(1L));
		assertThat("Metadata for source ID", metas.get(0).getSourceId(), equalTo("a"));
	}

	@Test
	public void store_newLocationStream() throws IOException {
		// GIVEN
		GeneralNodeDatum nodeDatum = loadJsonDatumResource("test-datum-01.txt", getClass()).get(0);
		GeneralLocationDatum datum = new GeneralLocationDatum();
		datum.setCreated(nodeDatum.getCreated());
		datum.setLocationId(TEST_LOC_ID);
		datum.setSourceId(nodeDatum.getSourceId());
		GeneralLocationDatumSamples s = new GeneralLocationDatumSamples();
		s.setI(nodeDatum.getSamples().getI());
		s.setA(nodeDatum.getSamples().getA());
		datum.setSamples(s);

		// WHEN
		DatumPK id = dao.store(datum);

		// THEN
		assertThat("ID returned", id, notNullValue());
		assertThat("ID has stream ID", id.getStreamId(), notNullValue());
		assertThat("ID has expected timestamp", id.getTimestamp(),
				equalTo(JodaDateUtils.fromJodaToInstant(datum.getCreated())));

		List<Datum> rows = listDatum(jdbcTemplate);
		assertThat("Datum stored in DB", rows, hasSize(1));
		assertThat("Datum ID matches returned value", rows.get(0).getId(), equalTo(id));
		assertThat("Datum properties", rows.get(0).getProperties(),
				equalTo(propertiesOf(decimalArray("1.2", "2.1"), decimalArray("100"), null, null)));

		List<ObjectDatumStreamMetadata> metas = listLocationMetadata(jdbcTemplate);
		assertThat("Stream metadata created", metas, hasSize(1));
		assertThat("Metadata for stream ID", metas.get(0).getStreamId(), equalTo(id.getStreamId()));
		assertThat("Metadata for node ID", metas.get(0).getObjectId(), equalTo(datum.getLocationId()));
		assertThat("Metadata for source ID", metas.get(0).getSourceId(), equalTo(datum.getSourceId()));
	}

	private void assertSame(DatumEntity expected, DatumEntity entity) {
		assertThat("DatumEntity should exist", entity, notNullValue());
		assertThat("Stream ID", expected.getStreamId(), equalTo(entity.getStreamId()));
		assertThat("Timestamp", expected.getTimestamp(), equalTo(entity.getTimestamp()));
		assertThat("Received", expected.getReceived(), equalTo(entity.getReceived()));
		assertThat("Properties", expected.getProperties(), equalTo(entity.getProperties()));
	}

	@Test
	public void getByPrimaryKey() {
		saveNew();
		DatumEntity datum = dao.get(lastDatum.getId());
		assertSame(lastDatum, datum);
	}

}
