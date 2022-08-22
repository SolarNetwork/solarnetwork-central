/* ==================================================================
 * HeartbeatJobTests.java - 22/08/2022 11:10:02 am
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
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import net.solarnetwork.central.oscp.dao.CapacityOptimizerConfigurationDao;
import net.solarnetwork.central.oscp.dao.CapacityProviderConfigurationDao;
import net.solarnetwork.central.oscp.dao.ExternalSystemSupportDao;
import net.solarnetwork.central.oscp.dao.jdbc.test.JdbcCapacityOptimizerConfigurationDaoTests;
import net.solarnetwork.central.oscp.dao.jdbc.test.JdbcCapacityProviderConfigurationDaoTests;
import net.solarnetwork.central.oscp.domain.BaseOscpExternalSystemConfiguration;
import net.solarnetwork.central.oscp.domain.CapacityOptimizerConfiguration;
import net.solarnetwork.central.oscp.domain.CapacityProviderConfiguration;
import net.solarnetwork.central.oscp.domain.OscpRole;
import net.solarnetwork.central.oscp.http.ExternalSystemClient;
import net.solarnetwork.central.oscp.jobs.HeartbeatJob;
import net.solarnetwork.central.oscp.util.SystemTaskContext;

/**
 * Test cases for the {@link HeartbeatJob} class.
 * 
 * @author matt
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
public class HeartbeatJobTests {

	@Mock
	private ExternalSystemSupportDao systemSupportDao;

	@Mock
	private CapacityProviderConfigurationDao capacityProviderDao;

	@Mock
	private CapacityOptimizerConfigurationDao capacityOptimizerDao;

	@Mock
	private ExternalSystemClient client;

	private HeartbeatJob job;

	@BeforeEach
	public void setup() {
		job = new HeartbeatJob(systemSupportDao, client);
	}

	@Test
	public void runJob() {
		// GIVEN
		final int rows = 3;
		final Instant start = Instant.now().truncatedTo(ChronoUnit.SECONDS);

		final var confs = new ArrayList<BaseOscpExternalSystemConfiguration<?>>(rows);
		CapacityProviderConfiguration c1 = JdbcCapacityProviderConfigurationDaoTests
				.newConf(randomUUID().getMostSignificantBits(), 1L, start);
		c1.setOscpVersion(V20);
		c1.setBaseUrl("http://" + randomUUID().toString() + ".example.com/oscp/2.0");
		CapacityOptimizerConfiguration c2 = JdbcCapacityOptimizerConfigurationDaoTests
				.newConf(randomUUID().getMostSignificantBits(), 1L, start.plusSeconds(1));
		c2.setOscpVersion(V20);
		c2.setBaseUrl("http://" + randomUUID().toString() + ".example.com/oscp/2.0");
		CapacityProviderConfiguration c3 = JdbcCapacityProviderConfigurationDaoTests
				.newConf(randomUUID().getMostSignificantBits(), 1L, start.plusSeconds(2));
		c3.setOscpVersion(V20);
		c3.setBaseUrl("http://" + randomUUID().toString() + ".example.com/oscp/2.0");
		confs.add(c1);
		confs.add(c2);
		confs.add(c3);

		var results = new ArrayList<Instant>(rows);

		will((Answer<Void>) invocation -> {
			Function<SystemTaskContext<?>, Instant> handler = invocation.getArgument(0);
			if ( results.size() >= rows ) {
				return null;
			}
			for ( int i = 0; i < rows; i++ ) {
				BaseOscpExternalSystemConfiguration<?> conf = confs.get(i);
				SystemTaskContext<?> ctx;
				if ( conf instanceof CapacityProviderConfiguration c ) {
					ctx = new SystemTaskContext<>("Heartbeat Test", OscpRole.CapacityProvider, c, null,
							null, capacityProviderDao);
				} else if ( conf instanceof CapacityOptimizerConfiguration c ) {
					ctx = new SystemTaskContext<>("Heartbeat Test", OscpRole.CapacityOptimizer, c, null,
							null, capacityOptimizerDao);
				} else {
					throw new RuntimeException("Unsupported configuration?");
				}
				Instant result = handler.apply(ctx);
				results.add(result);
			}
			return null;
		}).given(systemSupportDao).processExternalSystemWithExpiredHeartbeat(any());

		// WHEN
		job.run();

		// THEN
		assertThat("Result count matches row count", results, hasSize(rows));
	}

}
