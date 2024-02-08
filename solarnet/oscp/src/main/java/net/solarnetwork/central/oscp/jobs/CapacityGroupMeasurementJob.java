/* ==================================================================
 * CapacityGroupMeasurementJob.java - 1/09/2022 3:07:48 pm
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

package net.solarnetwork.central.oscp.jobs;

import static java.util.Collections.singleton;
import static net.solarnetwork.central.oscp.web.OscpWebUtils.UrlPaths_20.UPDATE_ASSET_MEASUREMENTS_URL_PATH;
import static net.solarnetwork.central.oscp.web.OscpWebUtils.UrlPaths_20.UPDATE_GROUP_MEASUREMENTS_URL_PATH;
import static net.solarnetwork.central.oscp.web.OscpWebUtils.UrlPaths_20.V20;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.http.HttpMethod;
import org.springframework.transaction.support.TransactionTemplate;
import net.solarnetwork.central.datum.v2.dao.BasicDatumCriteria;
import net.solarnetwork.central.oscp.dao.AssetConfigurationDao;
import net.solarnetwork.central.oscp.dao.CapacityGroupConfigurationDao;
import net.solarnetwork.central.oscp.dao.ExternalSystemConfigurationDao;
import net.solarnetwork.central.oscp.dao.MeasurementDao;
import net.solarnetwork.central.oscp.domain.AssetConfiguration;
import net.solarnetwork.central.oscp.domain.CapacityGroupConfiguration;
import net.solarnetwork.central.oscp.domain.Measurement;
import net.solarnetwork.central.oscp.domain.MeasurementPeriod;
import net.solarnetwork.central.oscp.domain.OscpRole;
import net.solarnetwork.central.oscp.http.ExternalSystemClient;
import net.solarnetwork.central.scheduler.JobSupport;
import net.solarnetwork.dao.DateRangeCriteria;
import oscp.v20.AssetMeasurement;
import oscp.v20.EnergyMeasurement;
import oscp.v20.InstantaneousMeasurement;
import oscp.v20.UpdateAssetMeasurement;
import oscp.v20.UpdateGroupMeasurements;

/**
 * Job to post OSCP measurement messages to external systems.
 * 
 * @author matt
 * @version 1.2
 */
public class CapacityGroupMeasurementJob extends JobSupport {

	private final OscpRole role;
	private final ExternalSystemConfigurationDao<?> dao;
	private final CapacityGroupConfigurationDao capacityGroupDao;
	private final AssetConfigurationDao assetDao;
	private final MeasurementDao measurementDao;
	private final ExternalSystemClient client;
	private TransactionTemplate txTemplate;

	/**
	 * Construct with properties.
	 * 
	 * @param role
	 *        the role
	 * @param dao
	 *        the DAO to use
	 * @param capacityGroupDao
	 *        the group DAO
	 * @param assetDao
	 *        the asset DAO to use
	 * @param measurementDao
	 *        the measurement DAO to use
	 * @param client
	 *        the client to use
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public CapacityGroupMeasurementJob(OscpRole role, ExternalSystemConfigurationDao<?> dao,
			CapacityGroupConfigurationDao capacityGroupDao, AssetConfigurationDao assetDao,
			MeasurementDao measurementDao, ExternalSystemClient client) {
		super();
		this.role = requireNonNullArgument(role, "role");
		this.dao = requireNonNullArgument(dao, "dao");
		this.capacityGroupDao = requireNonNullArgument(capacityGroupDao, "capacityGroupDao");
		this.assetDao = requireNonNullArgument(assetDao, "assetDao");
		this.measurementDao = requireNonNullArgument(measurementDao, "measurementDao");
		this.client = requireNonNullArgument(client, "client");
		setGroupId("OSCP");
		setId(this.role.toString() + "-CapacityGroupMeasurement");
		setMaximumWaitMs(1800000L);
	}

	/**
	 * Configure a transaction template.
	 * 
	 * @param txTemplate
	 *        the template
	 * @return this instance for method chaining
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public CapacityGroupMeasurementJob withTxTemplate(TransactionTemplate txTemplate) {
		this.txTemplate = requireNonNullArgument(txTemplate, "txTemplate");
		return this;
	}

	@Override
	public void run() {
		executeParallelJob(getId());
	}

	@Override
	protected int executeJobTask(AtomicInteger remainingIterataions) throws Exception {
		int totalProcessedCount = 0;
		Set<String> supportedOscpVersions = singleton(V20);
		final TransactionTemplate txTemplate = this.txTemplate;
		boolean processed = false;
		do {
			processed = false;
			if ( txTemplate != null ) {
				processed = txTemplate.execute((tx) -> {
					return exchange(supportedOscpVersions, remainingIterataions);
				});
			} else {
				processed = exchange(supportedOscpVersions, remainingIterataions);
			}
			if ( processed ) {
				totalProcessedCount += 1;
			}
		} while ( processed && remainingIterataions.get() > 0 );
		return totalProcessedCount;
	}

	private boolean exchange(Set<String> supportedOscpVersions, AtomicInteger remainingIterataions) {
		return dao.processExternalSystemWithExpiredMeasurement((ctx) -> {
			remainingIterataions.decrementAndGet();

			final CapacityGroupConfiguration group = switch (role) {
				case CapacityProvider -> capacityGroupDao.findForCapacityProvider(
						ctx.config().getUserId(), ctx.config().getEntityId(), ctx.groupIdentifier());
				case CapacityOptimizer -> capacityGroupDao.findForCapacityOptimizer(
						ctx.config().getUserId(), ctx.config().getEntityId(), ctx.groupIdentifier());
				default -> throw new IllegalArgumentException(
						"OSCP role [%s] not supported.".formatted(role));
			};
			final String combinedAssetId = group.combinedGroupAssetId() != null
					? group.combinedGroupAssetId()
					: ctx.config().combinedGroupAssetId();
			final MeasurementPeriod period = switch (role) {
				case CapacityProvider -> group.getCapacityProviderMeasurementPeriod();
				case CapacityOptimizer -> group.getCapacityOptimizerMeasurementPeriod();
				default -> throw new IllegalArgumentException(
						"OSCP role [%s] not supported.".formatted(role));
			};

			List<AssetConfiguration> assets = assetDao
					.findAllForCapacityGroup(group.getUserId(), group.getEntityId(), null).stream()
					.filter(e -> role == e.getAudience()).toList();

			Instant startDate = ctx.taskDate();
			Instant endDate = period.nextPeriodStart(startDate);
			BasicDatumCriteria dateCriteria = new BasicDatumCriteria();
			dateCriteria.setStartDate(startDate);
			dateCriteria.setEndDate(endDate);

			if ( !assets.isEmpty() ) {
				// get measurements
				final boolean useAssetMeasurement = ctx.config().useGroupAssetMeasurement();
				Object msg;
				if ( useAssetMeasurement ) {
					List<AssetMeasurement> measurements = assetMeasurements(assets, dateCriteria);
					if ( combinedAssetId != null ) {
						AssetMeasurement combined = combineAssetMeasurements(combinedAssetId,
								measurements);
						measurements = (combined == null ? Collections.emptyList()
								: Collections.singletonList(combined));
					}
					msg = new UpdateAssetMeasurement(group.getIdentifier(), measurements);
				} else {
					List<EnergyMeasurement> measurements = energyMeasurements(assets, dateCriteria);
					if ( combinedAssetId != null ) {
						EnergyMeasurement combined = combineEnergyMeasurements(measurements);
						measurements = (combined == null ? Collections.emptyList()
								: Collections.singletonList(combined));
					}
					msg = new UpdateGroupMeasurements(group.getIdentifier(), measurements);
				}
				try {
					client.systemExchange(ctx, HttpMethod.POST, () -> {
						ctx.verifySystemOscpVersion(supportedOscpVersions);
						return ctx.config().customUrlPath(
								useAssetMeasurement ? "UpdateAssetMeasurements"
										: "UpdateGroupMeasurements",
								useAssetMeasurement ? UPDATE_ASSET_MEASUREMENTS_URL_PATH
										: UPDATE_GROUP_MEASUREMENTS_URL_PATH);
					}, msg);
				} catch ( RuntimeException e ) {
					// ignore and continue; assume event logged in client.systemExchange()
					return null;
				}
			}

			return endDate;
		});
	}

	private EnergyMeasurement combineEnergyMeasurements(List<EnergyMeasurement> measurements) {
		if ( measurements == null || measurements.isEmpty() ) {
			return null;
		}
		if ( measurements.size() == 1 ) {
			return measurements.get(0);
		}
		/*-
		"value",
		"phase",
		"unit",
		"direction",
		"energy_type",
		"measure_time",
		"initial_measure_time"
		 */
		EnergyMeasurement result = null;
		for ( EnergyMeasurement m : measurements ) {
			if ( m.getValue() == null ) {
				continue;
			}
			if ( result == null ) {
				result = new EnergyMeasurement(m.getValue(), m.getPhase(), m.getUnit(), m.getDirection(),
						m.getMeasureTime());
				result.setInitialMeasureTime(m.getInitialMeasureTime());
				result.setEnergyType(m.getEnergyType());
			} else {
				result.setValue(result.getValue() + m.getValue());
			}
		}
		return result;
	}

	private InstantaneousMeasurement combineInstantaneousMeasurements(
			List<InstantaneousMeasurement> measurements) {
		if ( measurements == null || measurements.isEmpty() ) {
			return null;
		}
		if ( measurements.size() == 1 ) {
			return measurements.get(0);
		}
		/*-
		"value",
		"phase",
		"unit",
		"measure_time"
		 */
		InstantaneousMeasurement result = null;
		for ( InstantaneousMeasurement m : measurements ) {
			if ( m.getValue() == null ) {
				continue;
			}
			if ( result == null ) {
				result = new InstantaneousMeasurement(m.getValue(), m.getPhase(), m.getUnit(),
						m.getMeasureTime());
			} else {
				result.setValue(result.getValue() + m.getValue());
			}
		}
		return result;
	}

	private AssetMeasurement combineAssetMeasurements(String combinedAssetId,
			List<AssetMeasurement> measurements) {
		if ( measurements == null || measurements.isEmpty() ) {
			return null;
		}
		if ( measurements.size() == 1 ) {
			return measurements.get(0);
		}
		/*-
		"asset_id",
		"asset_category",
		"energy_measurement",
		"instantaneous_measurement"
		 */
		EnergyMeasurement energy = combineEnergyMeasurements(
				measurements.stream().map(AssetMeasurement::getEnergyMeasurement).toList());
		InstantaneousMeasurement inst = combineInstantaneousMeasurements(
				measurements.stream().map(AssetMeasurement::getInstantaneousMeasurement).toList());
		AssetMeasurement result = new AssetMeasurement(combinedAssetId,
				measurements.get(0).getAssetCategory());
		result.setEnergyMeasurement(energy);
		result.setInstantaneousMeasurement(inst);
		return result;
	}

	private List<AssetMeasurement> assetMeasurements(List<AssetConfiguration> assets,
			DateRangeCriteria criteria) {
		List<AssetMeasurement> result = new ArrayList<>(assets.size());
		for ( AssetConfiguration asset : assets ) {
			Collection<Measurement> measurements = measurementDao.getMeasurements(asset, criteria);
			AssetMeasurement measurement = new AssetMeasurement(asset.getIdentifier(),
					asset.getCategory().toOscp20Value());
			for ( Measurement meas : measurements ) {
				if ( meas.isEnergyMeasurement() ) {
					measurement.setEnergyMeasurement(meas.toOscp20EnergyValue());
				} else {
					measurement.setInstantaneousMeasurement(meas.toOscp20InstantaneousValue());
				}
			}
			result.add(measurement);
		}
		return result;
	}

	private List<EnergyMeasurement> energyMeasurements(List<AssetConfiguration> assets,
			DateRangeCriteria criteria) {
		List<EnergyMeasurement> result = new ArrayList<>(assets.size());
		for ( AssetConfiguration asset : assets ) {
			Collection<Measurement> measurements = measurementDao.getMeasurements(asset, criteria);
			for ( Measurement meas : measurements ) {
				if ( meas.isEnergyMeasurement() ) {
					result.add(meas.toOscp20EnergyValue());
				}
			}
		}
		return result;
	}

}
