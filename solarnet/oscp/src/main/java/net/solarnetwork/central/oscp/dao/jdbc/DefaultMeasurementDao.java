/* ==================================================================
 * DefaultMeasurementDao.java - 6/09/2022 12:11:58 pm
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

package net.solarnetwork.central.oscp.dao.jdbc;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Period;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.springframework.dao.TransientDataAccessResourceException;
import net.solarnetwork.central.datum.domain.DatumReadingType;
import net.solarnetwork.central.datum.v2.dao.BasicDatumCriteria;
import net.solarnetwork.central.datum.v2.dao.ReadingDatumDao;
import net.solarnetwork.central.datum.v2.domain.AggregateDatum;
import net.solarnetwork.central.datum.v2.domain.ReadingDatum;
import net.solarnetwork.central.datum.v2.support.BasicStreamDatumFilteredResultsProcessor;
import net.solarnetwork.central.oscp.dao.MeasurementDao;
import net.solarnetwork.central.oscp.domain.AssetConfiguration;
import net.solarnetwork.central.oscp.domain.AssetEnergyDatumConfiguration;
import net.solarnetwork.central.oscp.domain.AssetInstantaneousDatumConfiguration;
import net.solarnetwork.central.oscp.domain.Measurement;
import net.solarnetwork.dao.DateRangeCriteria;
import net.solarnetwork.domain.datum.Aggregation;
import net.solarnetwork.domain.datum.DatumProperties;
import net.solarnetwork.domain.datum.DatumPropertiesStatistics;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadata;
import net.solarnetwork.domain.datum.StreamDatum;

/**
 * JDBC implementation of {@link MeasurementDao}.
 *
 * @author matt
 * @version 1.1
 */
public class DefaultMeasurementDao implements MeasurementDao {

	/** The maximum allowed query duration. */
	public static final Duration MAX_QUERY_DURATION = Duration.ofHours(1);

	private final ReadingDatumDao readingDatumDao;
	private boolean createZeroValueMeasurementsOnMissingData;

	/**
	 * Constructor.
	 *
	 * @param readingDatumDao
	 *        the DAO to query readings from
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public DefaultMeasurementDao(ReadingDatumDao readingDatumDao) {
		super();
		this.readingDatumDao = requireNonNullArgument(readingDatumDao, "readingDatumDao");
	}

	@SuppressWarnings("JavaDurationGetSecondsToToSeconds")
	@Override
	public Collection<Measurement> getMeasurements(AssetConfiguration asset,
			DateRangeCriteria criteria) {
		// use CalculatedAtDifference reading for accumulating diffs, and list aggregate for time range
		final Duration queryDuration = Duration.between(criteria.getStartDate(), criteria.getEndDate());
		if ( queryDuration.compareTo(MAX_QUERY_DURATION) > 0 ) {
			throw new IllegalArgumentException(
					"Date range in query is more than maximum of 1 hour: " + queryDuration);
		}

		final BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setNodeId(asset.getNodeId());
		filter.setSourceId(asset.getSourceId());
		filter.setStartDate(criteria.getStartDate());
		filter.setEndDate(criteria.getEndDate());

		final List<Measurement> result = new ArrayList<>(2);

		if ( asset.getInstantaneous() != null && asset.getInstantaneous().getPropertyNames() != null ) {
			Aggregation agg = switch ((int) queryDuration.getSeconds()) {
				case 300 -> Aggregation.FiveMinute;
				case 600 -> Aggregation.TenMinute;
				case 900 -> Aggregation.FifteenMinute;
				case 1800 -> Aggregation.ThirtyMinute;
				case 3600 -> Aggregation.Hour;
				default -> throw new IllegalArgumentException(
						"Query duration must be 5, 10, 15, 30, or 60 minutes, but got: "
								+ queryDuration);
			};
			BasicDatumCriteria f = filter.clone();
			f.setAggregation(agg);
			final BasicStreamDatumFilteredResultsProcessor processor = new BasicStreamDatumFilteredResultsProcessor();
			try {
				readingDatumDao.findFilteredStream(f, processor, null, null, null);
			} catch ( IOException e ) {
				throw new TransientDataAccessResourceException("IO error querying datum.", e);
			}
			if ( processor.getData().isEmpty() ) {
				if ( !createZeroValueMeasurementsOnMissingData ) {
					return List.of();
				}
				// generate a "zero" measurement value
				AssetInstantaneousDatumConfiguration inst = asset.getInstantaneous();
				for ( @SuppressWarnings("unused")
				String propName : inst.getPropertyNames() ) {
					Measurement m = Measurement.instantaneousMeasurement(BigDecimal.ZERO,
							asset.getPhase(), inst.getUnit(), criteria.getEndDate());
					result.add(m);
				}
			} else {
				StreamDatum sd = processor.getData().getFirst();
				if ( sd instanceof AggregateDatum d ) {
					ObjectDatumStreamMetadata meta = processor.getMetadataProvider()
							.metadataForStreamId(d.getStreamId());
					AssetInstantaneousDatumConfiguration inst = asset.getInstantaneous();
					DatumProperties props = d.getProperties();
					DatumPropertiesStatistics stats = d.getStatistics();
					for ( String propName : inst.getPropertyNames() ) {
						int idx = meta.propertyIndex(DatumSamplesType.Instantaneous, propName);
						if ( idx < 0 ) {
							continue;
						}

						BigDecimal v = switch (inst.statisticType()) {
							case Maximum -> stats.getInstantaneousMaximum(idx);
							case Minimum -> stats.getInstantaneousMinimum(idx);
							case Count -> stats.getInstantaneousCount(idx);
							default -> props.instantaneousValue(idx);
						};
						if ( v == null ) {
							continue;
						}
						if ( inst.getMultiplier() != null ) {
							v = v.multiply(inst.getMultiplier());
						}
						Measurement m = Measurement.instantaneousMeasurement(v, asset.getPhase(),
								inst.getUnit(), criteria.getEndDate());
						result.add(m);
					}
				}
			}
		}

		if ( asset.getEnergy() != null && asset.getEnergy().getPropertyNames() != null ) {
			BasicDatumCriteria f = filter.clone();
			f.setReadingType(DatumReadingType.CalculatedAtDifference);
			f.setTimeTolerance(Period.ofDays(1));
			final BasicStreamDatumFilteredResultsProcessor processor = new BasicStreamDatumFilteredResultsProcessor();
			try {
				readingDatumDao.findFilteredStream(f, processor, null, null, null);
			} catch ( IOException e ) {
				throw new TransientDataAccessResourceException("IO error querying datum.", e);
			}

			StreamDatum sd = !processor.getData().isEmpty() ? processor.getData().getFirst() : null;

			if ( sd == null || !(sd instanceof ReadingDatum d)
					|| d.getTimestamp().equals(d.getEndTimestamp()) ) {
				// generate a "zero" measurement value if no data available, or the reading start/end on same date,
				// which can happen with reading queries where tolerance falls outside of measurement period
				AssetEnergyDatumConfiguration energy = asset.getEnergy();
				var m = Measurement.energyMeasurement(BigDecimal.ZERO, asset.getPhase(),
						energy.getUnit(), criteria.getEndDate(), energy.getType(), energy.getDirection(),
						criteria.getStartDate());
				result.add(m);
			} else {
				ObjectDatumStreamMetadata meta = processor.getMetadataProvider()
						.metadataForStreamId(d.getStreamId());
				AssetEnergyDatumConfiguration energy = asset.getEnergy();
				DatumProperties props = d.getProperties();
				for ( String propName : energy.getPropertyNames() ) {
					int idx = meta.propertyIndex(DatumSamplesType.Accumulating, propName);
					if ( idx < 0 ) {
						continue;
					}
					BigDecimal v = props.accumulatingValue(idx);
					if ( v == null ) {
						continue;
					}
					if ( energy.getMultiplier() != null ) {
						v = v.multiply(energy.getMultiplier());
					}
					var m = Measurement.energyMeasurement(v, asset.getPhase(), energy.getUnit(),
							d.getEndTimestamp(), energy.getType(), energy.getDirection(),
							d.getTimestamp());
					result.add(m);
				}
			}
		}

		return result;
	}

	/**
	 * Get the missing data zero measurement mode.
	 *
	 * @return {@code true} to create zero-valued measurement instances if no
	 *         datum data is available for a given measurement period, or
	 *         {@code false} to return an empty result set
	 * @since 1.1
	 */
	public boolean isCreateZeroValueMeasurementsOnMissingData() {
		return createZeroValueMeasurementsOnMissingData;
	}

	/**
	 * Set the missing data zero measurement mode.
	 *
	 * @param createZeroValueMeasurementsOnMissingData
	 *        {@code true} to create zero-valued measurement instances if no
	 *        datum data is available for a given measurement period, or
	 *        {@code false} to return an empty result set
	 * @since 1.1
	 */
	public void setCreateZeroValueMeasurementsOnMissingData(
			boolean createZeroValueMeasurementsOnMissingData) {
		this.createZeroValueMeasurementsOnMissingData = createZeroValueMeasurementsOnMissingData;
	}

}
