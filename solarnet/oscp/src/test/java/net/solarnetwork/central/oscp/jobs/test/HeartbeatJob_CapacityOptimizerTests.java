/* ==================================================================
 * HeartbeatJob_CapacityOptimizerTests.java - 22/08/2022 11:10:02 am
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
import static net.solarnetwork.central.oscp.web.OscpWebUtils.UrlPaths_20.V20;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.will;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import net.solarnetwork.central.oscp.dao.CapacityOptimizerConfigurationDao;
import net.solarnetwork.central.oscp.dao.jdbc.test.OscpJdbcTestUtils;
import net.solarnetwork.central.oscp.domain.CapacityOptimizerConfiguration;
import net.solarnetwork.central.oscp.domain.MeasurementStyle;
import net.solarnetwork.central.oscp.domain.OscpRole;
import net.solarnetwork.central.oscp.domain.SystemSettings;
import net.solarnetwork.central.oscp.http.ExternalSystemClient;
import net.solarnetwork.central.oscp.jobs.HeartbeatJob;
import net.solarnetwork.central.oscp.util.SystemTaskContext;

/**
 * Test cases for the {@link HeartbeatJob} class for capacity optimizer systems.
 * 
 * @author matt
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
public class HeartbeatJob_CapacityOptimizerTests {

	@Mock
	private CapacityOptimizerConfigurationDao capacityOptimizerDao;

	@Mock
	private ExternalSystemClient client;

	private HeartbeatJob job;

	@BeforeEach
	public void setup() {
		job = new HeartbeatJob(OscpRole.CapacityOptimizer, capacityOptimizerDao, client);
	}

	@Test
	public void runJob() {
		// GIVEN
		final int rows = 2;
		final Instant start = Instant.now().truncatedTo(ChronoUnit.SECONDS);

		final var confs = new ArrayList<CapacityOptimizerConfiguration>(rows);
		CapacityOptimizerConfiguration c1 = OscpJdbcTestUtils
				.newCapacityOptimizerConf(randomUUID().getMostSignificantBits(), 1L, start);
		c1.setOscpVersion(V20);
		c1.setBaseUrl("http://" + randomUUID().toString() + ".example.com/oscp/2.0");
		c1.setSettings(new SystemSettings(60, EnumSet.of(MeasurementStyle.Continuous)));
		CapacityOptimizerConfiguration c3 = OscpJdbcTestUtils.newCapacityOptimizerConf(
				randomUUID().getMostSignificantBits(), 1L, start.plusSeconds(2));
		c3.setOscpVersion(V20);
		c3.setBaseUrl("http://" + randomUUID().toString() + ".example.com/oscp/2.0");
		c3.setSettings(new SystemSettings(62, EnumSet.of(MeasurementStyle.Continuous)));
		confs.add(c1);
		confs.add(c3);

		var results = new ArrayList<Instant>(rows);

		will((Answer<Void>) invocation -> {
			Function<SystemTaskContext<CapacityOptimizerConfiguration>, Instant> handler = invocation
					.getArgument(0);
			if ( results.size() >= rows ) {
				return null;
			}
			for ( int i = 0; i < rows; i++ ) {
				CapacityOptimizerConfiguration conf = confs.get(i);
				var ctx = new SystemTaskContext<CapacityOptimizerConfiguration>("Heartbeat Test",
						OscpRole.CapacityOptimizer, conf, null, null, capacityOptimizerDao,
						Collections.emptyMap());
				Instant result = handler.apply(ctx);
				results.add(result);
			}
			return null;
		}).given(capacityOptimizerDao).processExternalSystemWithExpiredHeartbeat(any());

		// WHEN
		job.run();

		// THEN
		assertThat("Result count matches row count", results, hasSize(rows));
	}

}
