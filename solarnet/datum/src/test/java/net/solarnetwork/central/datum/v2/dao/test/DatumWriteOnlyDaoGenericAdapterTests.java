/* ==================================================================
 * DatumWriteOnlyDaoGenericAdapterTests.java - 21/03/2026 1:09:52 pm
 *
 * Copyright 2026 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.v2.dao.test;

import static net.solarnetwork.central.datum.v2.domain.ObjectDatumPK.unassignedStream;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static org.assertj.core.api.BDDAssertions.and;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.datum.v2.dao.DatumWriteOnlyDao;
import net.solarnetwork.central.datum.v2.dao.DatumWriteOnlyDaoGenericAdapter;
import net.solarnetwork.central.datum.v2.domain.DatumPK;
import net.solarnetwork.domain.datum.BasicStreamDatum;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.domain.datum.DatumProperties;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.GeneralDatum;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.domain.datum.StreamDatum;
import net.solarnetwork.util.NumberUtils;

/**
 * Test cases for the {@link DatumWriteOnlyDaoGenericAdapter} class.
 *
 * @author matt
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
@SuppressWarnings("static-access")
public class DatumWriteOnlyDaoGenericAdapterTests {

	@Mock
	private DatumWriteOnlyDao delegateDao;

	private DatumWriteOnlyDaoGenericAdapter dao;

	@BeforeEach
	public void setup() {
		dao = new DatumWriteOnlyDaoGenericAdapter(delegateDao);
	}

	@Test
	public void persist_StreamDatum() {
		// GIVEN
		final UUID streamId = UUID.randomUUID();
		final Instant ts = Instant.now().truncatedTo(ChronoUnit.MILLIS);
		final DatumProperties properties = DatumProperties
				.propertiesOf(NumberUtils.decimalArray("1.0", "2.0"), null, null, null);
		final BasicStreamDatum entity = new BasicStreamDatum(streamId, ts, properties);

		final DatumPK delegateDaoResult = new DatumPK(entity.getStreamId(), entity.getTimestamp());
		given(delegateDao.store(any(StreamDatum.class))).willReturn(delegateDaoResult);

		// WHEN
		final DatumPK result = dao.persist(entity);

		// THEN
		and.then(result).as("Delegate DAO result returned").isSameAs(delegateDaoResult);
	}

	@Test
	public void persist_Datum() {
		// GIVEN
		final Long nodeId = randomLong();
		final String sourceId = randomString();
		final Instant ts = Instant.now().truncatedTo(ChronoUnit.MILLIS);
		final Map<String, Number> iData = Map.of("a", 1);
		final Map<String, Number> aData = Map.of("b", 2);
		final GeneralDatum entity = GeneralDatum.nodeDatum(nodeId, sourceId, ts,
				new DatumSamples(iData, aData, null));

		final DatumPK delegateDaoResult = unassignedStream(entity.getKind(), nodeId, sourceId, ts);
		given(delegateDao.store(any(Datum.class))).willReturn(delegateDaoResult);

		// WHEN
		final DatumPK result = dao.persist(entity);

		// THEN
		and.then(result).as("Delegate DAO result returned").isSameAs(delegateDaoResult);
	}

	@Test
	public void persist_GeneralObjectDatum() {
		// GIVEN
		final Long nodeId = randomLong();
		final String sourceId = randomString();
		final Instant ts = Instant.now().truncatedTo(ChronoUnit.MILLIS);
		final Map<String, Number> iData = Map.of("a", 1);
		final Map<String, Number> aData = Map.of("b", 2);
		final GeneralNodeDatum entity = new GeneralNodeDatum(nodeId, ts, sourceId);
		entity.setSamples(new DatumSamples(iData, aData, null));

		final DatumPK delegateDaoResult = unassignedStream(ObjectDatumKind.Node, nodeId, sourceId, ts);
		given(delegateDao.persist(any())).willReturn(delegateDaoResult);

		// WHEN
		final DatumPK result = dao.persist(entity);

		// THEN
		and.then(result).as("Delegate DAO result returned").isSameAs(delegateDaoResult);
	}

}
