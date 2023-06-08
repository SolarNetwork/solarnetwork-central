/* ==================================================================
 * CapacityGroupMeasurementJobTests.java - 7/09/2022 12:52:38 pm
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

package net.solarnetwork.central.oscp.jobs.test;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;
import static java.util.UUID.randomUUID;
import static net.solarnetwork.central.oscp.domain.ExternalSystemServiceProperties.ASSET_MEAESUREMENT;
import static net.solarnetwork.central.oscp.web.OscpWebUtils.UrlPaths_20.V20;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.will;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.function.Function;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.springframework.http.HttpMethod;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.oscp.dao.AssetConfigurationDao;
import net.solarnetwork.central.oscp.dao.CapacityGroupConfigurationDao;
import net.solarnetwork.central.oscp.dao.CapacityProviderConfigurationDao;
import net.solarnetwork.central.oscp.dao.MeasurementDao;
import net.solarnetwork.central.oscp.dao.jdbc.test.OscpJdbcTestUtils;
import net.solarnetwork.central.oscp.domain.AssetConfiguration;
import net.solarnetwork.central.oscp.domain.CapacityGroupConfiguration;
import net.solarnetwork.central.oscp.domain.CapacityProviderConfiguration;
import net.solarnetwork.central.oscp.domain.EnergyDirection;
import net.solarnetwork.central.oscp.domain.EnergyType;
import net.solarnetwork.central.oscp.domain.Measurement;
import net.solarnetwork.central.oscp.domain.MeasurementStyle;
import net.solarnetwork.central.oscp.domain.MeasurementUnit;
import net.solarnetwork.central.oscp.domain.OscpRole;
import net.solarnetwork.central.oscp.domain.Phase;
import net.solarnetwork.central.oscp.domain.SystemSettings;
import net.solarnetwork.central.oscp.http.ExternalSystemClient;
import net.solarnetwork.central.oscp.jobs.CapacityGroupMeasurementJob;
import net.solarnetwork.central.oscp.util.CapacityGroupSystemTaskContext;
import net.solarnetwork.central.oscp.web.OscpWebUtils.UrlPaths_20;
import net.solarnetwork.dao.DateRangeCriteria;
import oscp.v20.AssetMeasurement;
import oscp.v20.EnergyMeasurement;
import oscp.v20.InstantaneousMeasurement;
import oscp.v20.UpdateAssetMeasurement;

/**
 * Test cases for the {@link CapacityGroupMeasurementJob} class.
 * 
 * @author matt
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
public class CapacityGroupMeasurementJob_CapacityProviderTests {

	@Mock
	private CapacityProviderConfigurationDao capacityProviderDao;

	@Mock
	private CapacityGroupConfigurationDao capacityGroupDao;

	@Mock
	private AssetConfigurationDao assetDao;

	@Mock
	private MeasurementDao measurementDao;

	@Mock
	private ExternalSystemClient client;

	@Captor
	private ArgumentCaptor<DateRangeCriteria> criteriaCaptor;

	@Captor
	private ArgumentCaptor<Supplier<String>> pathSupplierCaptor;

	@Captor
	private ArgumentCaptor<Object> postBodyCaptor;

	private CapacityGroupMeasurementJob job;

	@BeforeEach
	public void setup() {
		job = new CapacityGroupMeasurementJob(OscpRole.CapacityProvider, capacityProviderDao,
				capacityGroupDao, assetDao, measurementDao, client);
	}

	private CapacityProviderConfiguration systemConf() {
		CapacityProviderConfiguration systemConf = OscpJdbcTestUtils
				.newCapacityProviderConf(randomUUID().getMostSignificantBits(), 1L, Instant.now());
		systemConf.setOscpVersion(V20);
		systemConf.setBaseUrl("http://" + randomUUID().toString() + ".example.com/oscp/2.0");
		systemConf.setSettings(new SystemSettings(60, EnumSet.of(MeasurementStyle.Continuous)));
		systemConf.setServiceProps(singletonMap(ASSET_MEAESUREMENT, true));
		return systemConf.copyWithId(
				new UserLongCompositePK(systemConf.getUserId(), randomUUID().getMostSignificantBits()));
	}

	@Test
	public void runJob_provider_asset() {
		// GIVEN
		final Instant start = Instant.now().truncatedTo(ChronoUnit.HOURS);
		final Instant end = start.plus(10, ChronoUnit.MINUTES);
		final CapacityProviderConfiguration systemConf = systemConf();
		final CapacityGroupConfiguration group = OscpJdbcTestUtils
				.newCapacityGroupConfiguration(systemConf.getUserId(), systemConf.getEntityId(), null,
						start)
				.copyWithId(new UserLongCompositePK(systemConf.getUserId(),
						randomUUID().getMostSignificantBits()));

		final var results = new ArrayList<Instant>();

		// iterate over expired configurations
		final var ctx = new CapacityGroupSystemTaskContext<CapacityProviderConfiguration>(
				"Measurement Test", OscpRole.CapacityProvider, systemConf, group, start, null, null,
				capacityProviderDao, Collections.emptyMap());
		will((Answer<Void>) invocation -> {
			Function<CapacityGroupSystemTaskContext<CapacityProviderConfiguration>, Instant> handler = invocation
					.getArgument(0);
			if ( !results.isEmpty() ) {
				return null;
			}
			Instant result = handler.apply(ctx);
			results.add(result);

			return null;
		}).given(capacityProviderDao).processExternalSystemWithExpiredMeasurement(any());

		// get group configuration for expired system configuration
		given(capacityGroupDao.findForCapacityProvider(systemConf.getUserId(), systemConf.getEntityId(),
				group.getIdentifier())).willReturn(group);

		AssetConfiguration cpAsset = OscpJdbcTestUtils
				.newAssetConfiguration(systemConf.getUserId(), group.getEntityId(), Instant.now())
				.copyWithId(new UserLongCompositePK(systemConf.getUserId(),
						randomUUID().getMostSignificantBits()));
		AssetConfiguration coAsset = OscpJdbcTestUtils
				.newAssetConfiguration(systemConf.getUserId(), group.getEntityId(), Instant.now())
				.copyWithId(new UserLongCompositePK(systemConf.getUserId(),
						randomUUID().getMostSignificantBits()));
		coAsset.setAudience(OscpRole.CapacityOptimizer);

		// get all assets for group
		given(assetDao.findAllForCapacityGroup(systemConf.getUserId(), group.getEntityId(), null))
				.willReturn(asList(cpAsset, coAsset));

		// get measurements for system assets
		Measurement em = Measurement.energyMeasurement(new BigDecimal("123"), Phase.All,
				MeasurementUnit.kWh, end, EnergyType.Total, EnergyDirection.Import, start);
		Measurement im = Measurement.instantaneousMeasurement(new BigDecimal("345"), Phase.All,
				MeasurementUnit.kW, end);
		given(measurementDao.getMeasurements(same(cpAsset), any())).willReturn(asList(im, em));

		// WHEN
		job.run();

		// THEN
		assertThat("Result processed", results, hasSize(1));

		then(measurementDao).should().getMeasurements(same(cpAsset), criteriaCaptor.capture());
		assertThat("Measurement criteria start date from task date",
				criteriaCaptor.getValue().getStartDate(), is(equalTo(start)));
		assertThat("Measurement criteria end date from task date + measurement period",
				criteriaCaptor.getValue().getEndDate(), is(equalTo(end)));

		then(client).should().systemExchange(same(ctx), eq(HttpMethod.POST),
				pathSupplierCaptor.capture(), postBodyCaptor.capture());
		assertThat("POST path is for asset measurements", pathSupplierCaptor.getValue().get(),
				is(equalTo(UrlPaths_20.UPDATE_ASSET_MEASUREMENTS_URL_PATH)));
		assertThat("POST body is OSCP 2 Update Asset Measurements", postBodyCaptor.getValue(),
				is(instanceOf(UpdateAssetMeasurement.class)));
		UpdateAssetMeasurement post = (UpdateAssetMeasurement) postBodyCaptor.getValue();
		assertThat("POST group_id from group identifier", post.getGroupId(),
				is(equalTo(group.getIdentifier())));
		assertThat("Include one AssetMeasurement per AssetConfiguration", post.getMeasurements(),
				hasSize(1));
		AssetMeasurement m = post.getMeasurements().get(0);
		assertAssetMeasurement("1", group, cpAsset, m, im, em, start, end);
	}

	private void assertAssetMeasurement(String prefix, CapacityGroupConfiguration group,
			AssetConfiguration asset, AssetMeasurement m, Measurement im, Measurement em, Instant start,
			Instant end) {
		assertThat("Measurement asset %s category from asset configuration".formatted(prefix),
				m.getAssetCategory(), is(equalTo(asset.getCategory().toOscp20Value())));
		assertThat("Measurement asset %s ID from asset configuration".formatted(prefix), m.getAssetId(),
				is(equalTo(group.combinedAssetId() != null ? group.combinedAssetId()
						: asset.getIdentifier())));

		InstantaneousMeasurement aim = m.getInstantaneousMeasurement();
		assertInstantaneousMeasurement("1", im, aim, end);

		EnergyMeasurement aem = m.getEnergyMeasurement();
		assertEnergyMeasurement("1", em, aem, start, end);
	}

	private void assertInstantaneousMeasurement(String prefix, Measurement im,
			InstantaneousMeasurement aim, final Instant end) {
		assertThat("Instantaneous measurement %s populated".formatted(prefix), aim, is(notNullValue()));
		assertThat("Instantaneous measurement %s value from DAO IM".formatted(prefix), aim.getValue(),
				is(equalTo(im.value().doubleValue())));
		assertThat("Instantaneous measurement %s phase from DAO IM".formatted(prefix), aim.getPhase(),
				is(equalTo(im.phase().toOscp20Value())));
		assertThat("Instantaneous measurement %s unit from DAO IM".formatted(prefix), aim.getUnit(),
				is(equalTo(im.unit().toOscp20InstantaneousValue())));
		assertThat("Instantaneous measurement %s measure time from end criteria".formatted(prefix),
				aim.getMeasureTime(), is(equalTo(end)));
	}

	private void assertEnergyMeasurement(String prefix, Measurement em, EnergyMeasurement aem,
			final Instant start, final Instant end) {
		assertThat("Energy measurement %s populated".formatted(prefix), aem, is(notNullValue()));
		assertThat("Energy measurement %s value from DAO EM", aem.getValue(),
				is(equalTo(em.value().doubleValue())));
		assertThat("Energy measurement %s phase from DAO EM", aem.getPhase(),
				is(equalTo(em.phase().toOscp20Value())));
		assertThat("Energy measurement %s unit from DAO EM", aem.getUnit(),
				is(equalTo(em.unit().toOscp20EnergyValue())));
		assertThat("Energy measurement %s measure time from DAO EM", aem.getMeasureTime(),
				is(equalTo(end)));
		assertThat("Energy measurement %s energy type from DAO EM", aem.getEnergyType(),
				is(equalTo(em.energyType().toOscp20Value())));
		assertThat("Energy measurement %s energy direction from DAO EM", aem.getDirection(),
				is(equalTo(em.energyDirection().toOscp20Value())));
		assertThat("Energy measurement %s energy initial measurement time from start criteria",
				aem.getInitialMeasureTime(), is(equalTo(start)));
	}

	@Test
	public void runJob_provider_assets() {
		// GIVEN
		final Instant start = Instant.now().truncatedTo(ChronoUnit.HOURS);
		final Instant end = start.plus(10, ChronoUnit.MINUTES);
		final CapacityProviderConfiguration systemConf = systemConf();
		final CapacityGroupConfiguration group = OscpJdbcTestUtils
				.newCapacityGroupConfiguration(systemConf.getUserId(), systemConf.getEntityId(), null,
						start)
				.copyWithId(new UserLongCompositePK(systemConf.getUserId(),
						randomUUID().getMostSignificantBits()));

		final var results = new ArrayList<Instant>();

		// iterate over expired configurations
		final var ctx = new CapacityGroupSystemTaskContext<CapacityProviderConfiguration>(
				"Measurement Test", OscpRole.CapacityProvider, systemConf, group, start, null, null,
				capacityProviderDao, Collections.emptyMap());
		will((Answer<Void>) invocation -> {
			Function<CapacityGroupSystemTaskContext<CapacityProviderConfiguration>, Instant> handler = invocation
					.getArgument(0);
			if ( !results.isEmpty() ) {
				return null;
			}
			Instant result = handler.apply(ctx);
			results.add(result);

			return null;
		}).given(capacityProviderDao).processExternalSystemWithExpiredMeasurement(any());

		// get group configuration for expired system configuration
		given(capacityGroupDao.findForCapacityProvider(systemConf.getUserId(), systemConf.getEntityId(),
				group.getIdentifier())).willReturn(group);

		AssetConfiguration cpAsset1 = OscpJdbcTestUtils
				.newAssetConfiguration(systemConf.getUserId(), group.getEntityId(), Instant.now())
				.copyWithId(new UserLongCompositePK(systemConf.getUserId(),
						randomUUID().getMostSignificantBits()));
		AssetConfiguration cpAsset2 = OscpJdbcTestUtils
				.newAssetConfiguration(systemConf.getUserId(), group.getEntityId(), Instant.now())
				.copyWithId(new UserLongCompositePK(systemConf.getUserId(),
						randomUUID().getMostSignificantBits()));
		AssetConfiguration coAsset = OscpJdbcTestUtils
				.newAssetConfiguration(systemConf.getUserId(), group.getEntityId(), Instant.now())
				.copyWithId(new UserLongCompositePK(systemConf.getUserId(),
						randomUUID().getMostSignificantBits()));
		coAsset.setAudience(OscpRole.CapacityOptimizer);

		// get all assets for group
		given(assetDao.findAllForCapacityGroup(systemConf.getUserId(), group.getEntityId(), null))
				.willReturn(asList(cpAsset1, cpAsset2, coAsset));

		// get measurements for system assets
		Measurement em1 = Measurement.energyMeasurement(new BigDecimal("123"), Phase.All,
				MeasurementUnit.kWh, end, EnergyType.Total, EnergyDirection.Import, start);
		Measurement im1 = Measurement.instantaneousMeasurement(new BigDecimal("345"), Phase.All,
				MeasurementUnit.kW, end);
		given(measurementDao.getMeasurements(same(cpAsset1), any())).willReturn(asList(im1, em1));

		Measurement em2 = Measurement.energyMeasurement(new BigDecimal("234"), Phase.All,
				MeasurementUnit.kWh, end, EnergyType.Total, EnergyDirection.Import, start);
		Measurement im2 = Measurement.instantaneousMeasurement(new BigDecimal("456"), Phase.All,
				MeasurementUnit.kW, end);
		given(measurementDao.getMeasurements(same(cpAsset2), any())).willReturn(asList(im2, em2));

		// WHEN
		job.run();

		// THEN
		assertThat("Result processed", results, hasSize(1));

		then(measurementDao).should().getMeasurements(same(cpAsset1), criteriaCaptor.capture());
		assertThat("Measurement criteria 1 start date from task date",
				criteriaCaptor.getValue().getStartDate(), is(equalTo(start)));
		assertThat("Measurement criteria 1 end date from task date + measurement period",
				criteriaCaptor.getValue().getEndDate(), is(equalTo(end)));

		then(measurementDao).should().getMeasurements(same(cpAsset2), criteriaCaptor.capture());
		assertThat("Measurement criteria 2 start date from task date",
				criteriaCaptor.getValue().getStartDate(), is(equalTo(start)));
		assertThat("Measurement criteria 2 end date from task date + measurement period",
				criteriaCaptor.getValue().getEndDate(), is(equalTo(end)));

		then(client).should().systemExchange(same(ctx), eq(HttpMethod.POST),
				pathSupplierCaptor.capture(), postBodyCaptor.capture());
		assertThat("POST path is for asset measurements", pathSupplierCaptor.getValue().get(),
				is(equalTo(UrlPaths_20.UPDATE_ASSET_MEASUREMENTS_URL_PATH)));
		assertThat("POST body is OSCP 2 Update Asset Measurements", postBodyCaptor.getValue(),
				is(instanceOf(UpdateAssetMeasurement.class)));
		UpdateAssetMeasurement post = (UpdateAssetMeasurement) postBodyCaptor.getValue();
		assertThat("POST group_id from group identifier", post.getGroupId(),
				is(equalTo(group.getIdentifier())));
		assertThat("Include one AssetMeasurement per AssetConfiguration", post.getMeasurements(),
				hasSize(2));

		AssetMeasurement m1 = post.getMeasurements().get(0);
		assertAssetMeasurement("1", group, cpAsset1, m1, im1, em1, start, end);

		AssetMeasurement m2 = post.getMeasurements().get(1);
		assertAssetMeasurement("2", group, cpAsset2, m2, im2, em2, start, end);
	}

	@Test
	public void runJob_provider_assets_combined() {
		// GIVEN
		final Instant start = Instant.now().truncatedTo(ChronoUnit.HOURS);
		final Instant end = start.plus(10, ChronoUnit.MINUTES);
		final CapacityProviderConfiguration systemConf = systemConf();
		final CapacityGroupConfiguration group = OscpJdbcTestUtils
				.newCapacityGroupConfiguration(systemConf.getUserId(), systemConf.getEntityId(), null,
						start)
				.copyWithId(new UserLongCompositePK(systemConf.getUserId(),
						randomUUID().getMostSignificantBits()));

		final String combinedAssetId = "combined.asset";
		group.putServiceProp(CapacityGroupConfiguration.COMBINED_ASSET_ID_PROP, combinedAssetId);

		final var results = new ArrayList<Instant>();

		// iterate over expired configurations
		final var ctx = new CapacityGroupSystemTaskContext<CapacityProviderConfiguration>(
				"Measurement Test", OscpRole.CapacityProvider, systemConf, group, start, null, null,
				capacityProviderDao, Collections.emptyMap());
		will((Answer<Void>) invocation -> {
			Function<CapacityGroupSystemTaskContext<CapacityProviderConfiguration>, Instant> handler = invocation
					.getArgument(0);
			if ( !results.isEmpty() ) {
				return null;
			}
			Instant result = handler.apply(ctx);
			results.add(result);

			return null;
		}).given(capacityProviderDao).processExternalSystemWithExpiredMeasurement(any());

		// get group configuration for expired system configuration
		given(capacityGroupDao.findForCapacityProvider(systemConf.getUserId(), systemConf.getEntityId(),
				group.getIdentifier())).willReturn(group);

		AssetConfiguration cpAsset1 = OscpJdbcTestUtils
				.newAssetConfiguration(systemConf.getUserId(), group.getEntityId(), Instant.now())
				.copyWithId(new UserLongCompositePK(systemConf.getUserId(),
						randomUUID().getMostSignificantBits()));
		AssetConfiguration cpAsset2 = OscpJdbcTestUtils
				.newAssetConfiguration(systemConf.getUserId(), group.getEntityId(), Instant.now())
				.copyWithId(new UserLongCompositePK(systemConf.getUserId(),
						randomUUID().getMostSignificantBits()));
		AssetConfiguration coAsset = OscpJdbcTestUtils
				.newAssetConfiguration(systemConf.getUserId(), group.getEntityId(), Instant.now())
				.copyWithId(new UserLongCompositePK(systemConf.getUserId(),
						randomUUID().getMostSignificantBits()));
		coAsset.setAudience(OscpRole.CapacityOptimizer);

		// get all assets for group
		given(assetDao.findAllForCapacityGroup(systemConf.getUserId(), group.getEntityId(), null))
				.willReturn(asList(cpAsset1, cpAsset2, coAsset));

		// get measurements for system assets
		Measurement em1 = Measurement.energyMeasurement(new BigDecimal("123"), Phase.All,
				MeasurementUnit.kWh, end, EnergyType.Total, EnergyDirection.Import, start);
		Measurement im1 = Measurement.instantaneousMeasurement(new BigDecimal("345"), Phase.All,
				MeasurementUnit.kW, end);
		given(measurementDao.getMeasurements(same(cpAsset1), any())).willReturn(asList(im1, em1));

		Measurement em2 = Measurement.energyMeasurement(new BigDecimal("234"), Phase.All,
				MeasurementUnit.kWh, end, EnergyType.Total, EnergyDirection.Import, start);
		Measurement im2 = Measurement.instantaneousMeasurement(new BigDecimal("456"), Phase.All,
				MeasurementUnit.kW, end);
		given(measurementDao.getMeasurements(same(cpAsset2), any())).willReturn(asList(im2, em2));

		// WHEN
		job.run();

		// THEN
		assertThat("Result processed", results, hasSize(1));

		then(measurementDao).should().getMeasurements(same(cpAsset1), criteriaCaptor.capture());
		assertThat("Measurement criteria 1 start date from task date",
				criteriaCaptor.getValue().getStartDate(), is(equalTo(start)));
		assertThat("Measurement criteria 1 end date from task date + measurement period",
				criteriaCaptor.getValue().getEndDate(), is(equalTo(end)));

		then(measurementDao).should().getMeasurements(same(cpAsset2), criteriaCaptor.capture());
		assertThat("Measurement criteria 2 start date from task date",
				criteriaCaptor.getValue().getStartDate(), is(equalTo(start)));
		assertThat("Measurement criteria 2 end date from task date + measurement period",
				criteriaCaptor.getValue().getEndDate(), is(equalTo(end)));

		then(client).should().systemExchange(same(ctx), eq(HttpMethod.POST),
				pathSupplierCaptor.capture(), postBodyCaptor.capture());
		assertThat("POST path is for asset measurements", pathSupplierCaptor.getValue().get(),
				is(equalTo(UrlPaths_20.UPDATE_ASSET_MEASUREMENTS_URL_PATH)));
		assertThat("POST body is OSCP 2 Update Asset Measurements", postBodyCaptor.getValue(),
				is(instanceOf(UpdateAssetMeasurement.class)));
		UpdateAssetMeasurement post = (UpdateAssetMeasurement) postBodyCaptor.getValue();
		assertThat("POST group_id from group identifier", post.getGroupId(),
				is(equalTo(group.getIdentifier())));
		assertThat("Include one combined AssetMeasurement", post.getMeasurements(), hasSize(1));

		AssetMeasurement m1 = post.getMeasurements().get(0);

		Measurement combinedIm = Measurement.instantaneousMeasurement(im1.value().add(im2.value()),
				im1.phase(), im1.unit(), im1.measureTime());
		Measurement combinedEm = Measurement.energyMeasurement(em1.value().add(em2.value()), em1.phase(),
				em1.unit(), em1.measureTime(), em1.energyType(), em1.energyDirection(),
				em1.startMeasureTime());
		assertAssetMeasurement("Combined", group, cpAsset1, m1, combinedIm, combinedEm, start, end);
	}

}
