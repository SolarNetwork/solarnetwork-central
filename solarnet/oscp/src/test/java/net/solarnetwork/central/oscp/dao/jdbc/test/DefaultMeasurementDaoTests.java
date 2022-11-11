/* ==================================================================
 * DefaultMeasurementDaoTests.java - 7/09/2022 3:04:37 pm
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

package net.solarnetwork.central.oscp.dao.jdbc.test;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;
import static java.util.UUID.randomUUID;
import static net.solarnetwork.central.datum.v2.support.StreamDatumFilteredResultsProcessor.METADATA_PROVIDER_ATTR;
import static net.solarnetwork.central.oscp.domain.Measurement.instantaneousMeasurement;
import static net.solarnetwork.domain.datum.BasicObjectDatumStreamDataSet.dataSet;
import static net.solarnetwork.domain.datum.DatumProperties.propertiesOf;
import static net.solarnetwork.domain.datum.DatumPropertiesStatistics.statisticsOf;
import static net.solarnetwork.util.NumberUtils.decimalArray;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.will;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import net.solarnetwork.central.datum.domain.DatumReadingType;
import net.solarnetwork.central.datum.v2.dao.BasicDatumCriteria;
import net.solarnetwork.central.datum.v2.dao.DatumCriteria;
import net.solarnetwork.central.datum.v2.dao.ReadingDatumDao;
import net.solarnetwork.central.datum.v2.dao.ReadingDatumEntity;
import net.solarnetwork.central.datum.v2.support.StreamDatumFilteredResultsProcessor;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.oscp.dao.jdbc.DefaultMeasurementDao;
import net.solarnetwork.central.oscp.domain.AssetConfiguration;
import net.solarnetwork.central.oscp.domain.Measurement;
import net.solarnetwork.domain.datum.Aggregation;
import net.solarnetwork.domain.datum.BasicObjectDatumStreamMetadata;
import net.solarnetwork.domain.datum.DatumProperties;
import net.solarnetwork.domain.datum.DatumPropertiesStatistics;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.domain.datum.ObjectDatumStreamDataSet;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadata;
import net.solarnetwork.domain.datum.StreamDatum;

/**
 * Test cases for the {@link DefaultMeasurementDao} class.
 * 
 * @author matt
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
public class DefaultMeasurementDaoTests {

	@Mock
	private ReadingDatumDao readingDatumDao;

	@Captor
	private ArgumentCaptor<DatumCriteria> criteriaCaptor;

	private DefaultMeasurementDao dao;

	@BeforeEach
	public void setup() {
		dao = new DefaultMeasurementDao(readingDatumDao);
	}

	private ObjectDatumStreamMetadata nodeMeta(Long nodeId, String sourceId, String[] i, String[] a,
			String[] s) {
		return new BasicObjectDatumStreamMetadata(UUID.randomUUID(), "Pacific/Auckland",
				ObjectDatumKind.Node, nodeId, sourceId, i, a, s);
	}

	@Test
	public void getMeasurements() throws IOException {
		// GIVEN
		final Long cgId = randomUUID().getMostSignificantBits();

		final Long userId = randomUUID().getMostSignificantBits();
		AssetConfiguration asset = OscpJdbcTestUtils.newAssetConfiguration(userId, cgId, Instant.now())
				.copyWithId(new UserLongCompositePK(userId, randomUUID().getMostSignificantBits()));

		final Instant start = Instant.now().truncatedTo(ChronoUnit.HOURS);
		final Instant end = start.plus(15, ChronoUnit.MINUTES);

		ObjectDatumStreamMetadata meta = nodeMeta(asset.getNodeId(), asset.getSourceId(),
				new String[] { "watts" }, new String[] { "wattHours" }, null);
		DatumProperties p = propertiesOf(decimalArray("1.23"), decimalArray("3.45"), null, null);
		DatumPropertiesStatistics s = statisticsOf(
				new BigDecimal[][] { decimalArray("60", "1.0", "2.0") },
				new BigDecimal[][] { decimalArray("10", "0", "10") });
		var reading = new ReadingDatumEntity(meta.getStreamId(), start, Aggregation.None, end, p, s);

		ObjectDatumStreamDataSet<StreamDatum> data = dataSet(asList(meta), asList(reading));

		will((Answer<Void>) invocation -> {
			StreamDatumFilteredResultsProcessor processor = invocation.getArgument(1);
			processor.start(null, null, null, singletonMap(METADATA_PROVIDER_ATTR, data));
			processor.handleResultItem(reading);
			return null;
		}).given(readingDatumDao).findFilteredStream(any(), any(), isNull(), isNull(), isNull());

		// WHEN
		BasicDatumCriteria criteria = new BasicDatumCriteria();
		criteria.setStartDate(start);
		criteria.setEndDate(end);

		Collection<Measurement> results = dao.getMeasurements(asset, criteria);

		// THEN
		then(readingDatumDao).should(times(2)).findFilteredStream(criteriaCaptor.capture(), any(),
				isNull(), isNull(), isNull());

		// query 1 for instantaneous averages
		DatumCriteria daoCriteria = criteriaCaptor.getAllValues().get(0);
		assertThat("Datum criteria reading type not set", daoCriteria.getReadingType(), is(nullValue()));
		assertThat("Datum criteria time tolerance not set", daoCriteria.getTimeTolerance(),
				is(nullValue()));
		assertThat("Datum criteria for asset node ID", daoCriteria.getNodeId(),
				is(equalTo(asset.getNodeId())));
		assertThat("Datum criteria for asset source ID", daoCriteria.getSourceId(),
				is(equalTo(asset.getSourceId())));
		assertThat("Datum criteria start date from input criteria", daoCriteria.getStartDate(),
				is(equalTo(start)));
		assertThat("Datum criteria end date from input criteria", daoCriteria.getEndDate(),
				is(equalTo(end)));
		assertThat("Datum criteria aggregation as duration of input criteria date range",
				daoCriteria.getAggregation(), is(equalTo(Aggregation.FifteenMinute)));

		// query 2 for accumulating reading
		daoCriteria = criteriaCaptor.getAllValues().get(1);
		assertThat("Datum criteria reading type", daoCriteria.getReadingType(),
				is(equalTo(DatumReadingType.CalculatedAtDifference)));
		assertThat("Datum criteria time tolerance", daoCriteria.getTimeTolerance(),
				is(equalTo(Period.ofDays(1))));
		assertThat("Datum criteria for asset node ID", daoCriteria.getNodeId(),
				is(equalTo(asset.getNodeId())));
		assertThat("Datum criteria for asset source ID", daoCriteria.getSourceId(),
				is(equalTo(asset.getSourceId())));
		assertThat("Datum criteria start date from input criteria", daoCriteria.getStartDate(),
				is(equalTo(start)));
		assertThat("Datum criteria end date from input criteria", daoCriteria.getEndDate(),
				is(equalTo(end)));
		assertThat("Datum criteria aggregation not set", daoCriteria.getAggregation(), is(nullValue()));

		List<Measurement> resultList = new ArrayList<>(results);
		assertThat("Results returned for instantaneous and energy measurements", results, hasSize(2));
		assertThat("Instantaneous measurement", resultList.get(0),
				is(equalTo(instantaneousMeasurement(
						s.getInstantaneousMaximum(0).multiply(asset.getInstantaneous().getMultiplier()),
						asset.getPhase(), asset.getInstantaneous().getUnit(), end))));
		assertThat("Energy measurement", resultList.get(1),
				is(equalTo(Measurement.energyMeasurement(
						p.accumulatingValue(0).multiply(asset.getEnergy().getMultiplier()),
						asset.getPhase(), asset.getEnergy().getUnit(), end, asset.getEnergy().getType(),
						asset.getEnergy().getDirection(), start))));
	}

}
