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
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.http.HttpMethod;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClientException;
import net.solarnetwork.central.datum.v2.dao.BasicDatumCriteria;
import net.solarnetwork.central.oscp.dao.AssetConfigurationDao;
import net.solarnetwork.central.oscp.dao.CapacityGroupConfigurationDao;
import net.solarnetwork.central.oscp.dao.ExternalSystemConfigurationDao;
import net.solarnetwork.central.oscp.dao.MeasurementDao;
import net.solarnetwork.central.oscp.domain.AssetConfiguration;
import net.solarnetwork.central.oscp.domain.CapacityGroupConfiguration;
import net.solarnetwork.central.oscp.domain.ExternalSystemConfigurationException;
import net.solarnetwork.central.oscp.domain.Measurement;
import net.solarnetwork.central.oscp.domain.MeasurementPeriod;
import net.solarnetwork.central.oscp.domain.OscpRole;
import net.solarnetwork.central.oscp.http.ExternalSystemClient;
import net.solarnetwork.central.scheduler.JobSupport;
import net.solarnetwork.dao.DateRangeCriteria;
import oscp.v20.AssetMeasurement;
import oscp.v20.EnergyMeasurement;
import oscp.v20.UpdateAssetMeasurement;
import oscp.v20.UpdateGroupMeasurements;

/**
 * Job to post OSCP measurement messages to external systems.
 * 
 * @author matt
 * @version 1.0
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
			Instant endDate = startDate.plusSeconds(period.getCode());
			BasicDatumCriteria dateCriteria = new BasicDatumCriteria();
			dateCriteria.setStartDate(startDate);
			dateCriteria.setEndDate(endDate);

			if ( !assets.isEmpty() ) {
				// get measurements
				final boolean useAssetMeasurement = ctx.config().useGroupAssetMeasurement();
				Object msg;
				if ( useAssetMeasurement ) {
					List<AssetMeasurement> measurements = assetMeasurements(assets, dateCriteria);
					msg = new UpdateAssetMeasurement(group.getIdentifier(), measurements);
				} else {
					List<EnergyMeasurement> measurements = energyMeasurements(assets, dateCriteria);
					msg = new UpdateGroupMeasurements(group.getIdentifier(), measurements);
				}
				try {
					client.systemExchange(ctx, HttpMethod.POST, () -> {
						ctx.verifySystemOscpVersion(supportedOscpVersions);
						if ( useAssetMeasurement ) {
							return UPDATE_ASSET_MEASUREMENTS_URL_PATH;
						}
						return UPDATE_GROUP_MEASUREMENTS_URL_PATH;
					}, msg);
				} catch ( RestClientException | ExternalSystemConfigurationException e ) {
					// ignore and continue; assume event logged in client.systemExchange()
				}
			}

			return endDate;
		});
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
