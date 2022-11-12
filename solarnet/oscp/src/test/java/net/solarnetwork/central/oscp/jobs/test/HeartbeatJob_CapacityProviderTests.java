/* ==================================================================
 * HeartbeatJob_CapacityProviderTests.java - 22/08/2022 11:10:02 am
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

import static java.util.UUID.randomUUID;
import static net.solarnetwork.central.oscp.web.OscpWebUtils.UrlPaths_20.HEARTBEAT_URL_PATH;
import static net.solarnetwork.central.oscp.web.OscpWebUtils.UrlPaths_20.V20;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.will;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
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
import net.solarnetwork.central.oscp.dao.CapacityProviderConfigurationDao;
import net.solarnetwork.central.oscp.dao.jdbc.test.OscpJdbcTestUtils;
import net.solarnetwork.central.oscp.domain.CapacityProviderConfiguration;
import net.solarnetwork.central.oscp.domain.ExternalSystemServiceProperties;
import net.solarnetwork.central.oscp.domain.MeasurementStyle;
import net.solarnetwork.central.oscp.domain.OscpRole;
import net.solarnetwork.central.oscp.domain.SystemSettings;
import net.solarnetwork.central.oscp.http.ExternalSystemClient;
import net.solarnetwork.central.oscp.jobs.HeartbeatJob;
import net.solarnetwork.central.oscp.util.SystemTaskContext;
import oscp.v20.Heartbeat;

/**
 * Test cases for the {@link HeartbeatJob} class for capacity provider systems.
 * 
 * @author matt
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
public class HeartbeatJob_CapacityProviderTests {

	@Mock
	private CapacityProviderConfigurationDao capacityProviderDao;

	@Mock
	private ExternalSystemClient client;

	@Captor
	private ArgumentCaptor<Supplier<String>> pathSupplierCaptor;

	@Captor
	ArgumentCaptor<Supplier<Object>> httpBodyCaptor;

	private HeartbeatJob job;

	@BeforeEach
	public void setup() {
		job = new HeartbeatJob(OscpRole.CapacityProvider, capacityProviderDao, client);
	}

	@Test
	public void runJob() {
		// GIVEN
		final int rows = 2;
		final Instant start = Instant.now().truncatedTo(ChronoUnit.SECONDS);

		final var confs = new ArrayList<CapacityProviderConfiguration>(rows);
		CapacityProviderConfiguration c1 = OscpJdbcTestUtils
				.newCapacityProviderConf(randomUUID().getMostSignificantBits(), 1L, start);
		c1.setOscpVersion(V20);
		c1.setBaseUrl("http://" + randomUUID().toString() + ".example.com/oscp/2.0");
		c1.setSettings(new SystemSettings(60, EnumSet.of(MeasurementStyle.Continuous)));
		CapacityProviderConfiguration c3 = OscpJdbcTestUtils.newCapacityProviderConf(
				randomUUID().getMostSignificantBits(), 1L, start.plusSeconds(2));
		c3.setOscpVersion(V20);
		c3.setBaseUrl("http://" + randomUUID().toString() + ".example.com/oscp/2.0");
		c3.setSettings(new SystemSettings(62, EnumSet.of(MeasurementStyle.Continuous)));
		confs.add(c1);
		confs.add(c3);

		var results = new ArrayList<Instant>(rows);

		will((Answer<Void>) invocation -> {
			Function<SystemTaskContext<CapacityProviderConfiguration>, Instant> handler = invocation
					.getArgument(0);
			if ( results.size() >= rows ) {
				return null;
			}
			for ( int i = 0; i < rows; i++ ) {
				CapacityProviderConfiguration conf = confs.get(i);
				var ctx = new SystemTaskContext<CapacityProviderConfiguration>("Heartbeat Test",
						OscpRole.CapacityProvider, conf, null, null, capacityProviderDao,
						Collections.emptyMap());
				Instant result = handler.apply(ctx);
				results.add(result);
			}
			return null;
		}).given(capacityProviderDao).processExternalSystemWithExpiredHeartbeat(any());

		// WHEN
		job.run();

		// THEN
		assertThat("Result count matches row count", results, hasSize(rows));
	}

	@Test
	public void runJob_customUrl() {
		// GIVEN
		final int rows = 2;
		final Instant start = Instant.now().truncatedTo(ChronoUnit.SECONDS);
		final String customUrlPath = "/hb";

		final var confs = new ArrayList<CapacityProviderConfiguration>(rows);
		CapacityProviderConfiguration c1 = OscpJdbcTestUtils
				.newCapacityProviderConf(randomUUID().getMostSignificantBits(), 1L, start);
		c1.setOscpVersion(V20);
		c1.setBaseUrl("http://" + randomUUID().toString() + ".example.com/oscp/2.0");
		c1.setSettings(new SystemSettings(60, EnumSet.of(MeasurementStyle.Continuous)));
		CapacityProviderConfiguration c3 = OscpJdbcTestUtils.newCapacityProviderConf(
				randomUUID().getMostSignificantBits(), 1L, start.plusSeconds(2));
		c3.setOscpVersion(V20);
		c3.setBaseUrl("http://" + randomUUID().toString() + ".example.com");
		c3.setServiceProps(
				Map.of(ExternalSystemServiceProperties.URL_PATHS, Map.of("Heartbeat", customUrlPath)));
		c3.setSettings(new SystemSettings(62, EnumSet.of(MeasurementStyle.Continuous)));
		confs.add(c1);
		confs.add(c3);

		var results = new ArrayList<Instant>(rows);

		will((Answer<Void>) invocation -> {
			Function<SystemTaskContext<CapacityProviderConfiguration>, Instant> handler = invocation
					.getArgument(0);
			if ( results.size() >= rows ) {
				return null;
			}
			for ( int i = 0; i < rows; i++ ) {
				CapacityProviderConfiguration conf = confs.get(i);
				var ctx = new SystemTaskContext<CapacityProviderConfiguration>("Heartbeat Test",
						OscpRole.CapacityProvider, conf, null, null, capacityProviderDao,
						Collections.emptyMap());
				Instant result = handler.apply(ctx);
				results.add(result);
			}
			return null;
		}).given(capacityProviderDao).processExternalSystemWithExpiredHeartbeat(any());

		// WHEN
		job.run();

		// THEN
		assertThat("Result count matches row count", results, hasSize(rows));
		then(client).should(times(2)).systemExchange(any(), eq(HttpMethod.POST),
				pathSupplierCaptor.capture(), httpBodyCaptor.capture());
		for ( int i = 0; i < 2; i++ ) {
			String url = pathSupplierCaptor.getAllValues().get(i).get();
			assertThat("Custom URL path used for job %d".formatted(i), url,
					is(equalTo(i == 0 ? HEARTBEAT_URL_PATH : customUrlPath)));
			Object body = httpBodyCaptor.getAllValues().get(i);
			assertThat("Heartbeat body provided", body, is(instanceOf(Heartbeat.class)));
		}
	}

}
