/* ==================================================================
 * KillbillPingTest.java - 29/09/2017 3:50:16 PM
 * 
 * Copyright 2017 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.billing.killbill;

import java.util.Collection;
import java.util.concurrent.TimeUnit;
import net.solarnetwork.central.user.billing.killbill.domain.HealthCheckResult;
import net.solarnetwork.domain.PingTest;
import net.solarnetwork.domain.PingTestResult;
import net.solarnetwork.util.CachedResult;

/**
 * Health check for Kill Bill.
 * 
 * @author matt
 * @version 1.2
 */
public class KillbillPingTest implements PingTest {

	private final KillbillClient client;
	private int pingResultsCacheSeconds = 300;

	private CachedResult<PingTestResult> cachedResult;

	/**
	 * Constructor.
	 * 
	 * @param client
	 *        the Kill Bill client to use
	 */
	public KillbillPingTest(KillbillClient client) {
		super();
		this.client = client;
	}

	@Override
	public String getPingTestId() {
		return getClass().getName() + "-" + client.getUniqueId();
	}

	@Override
	public String getPingTestName() {
		return "Kill Bill Billing";
	}

	@Override
	public long getPingTestMaximumExecutionMilliseconds() {
		return 10000;
	}

	@Override
	public PingTest.Result performPingTest() throws Exception {
		CachedResult<PingTestResult> cached = cachedResult;
		if ( cached != null && cached.isValid() ) {
			return cached.getResult();
		}

		Collection<HealthCheckResult> results = client.healthCheck();
		boolean healthy = true;
		String msg = "All health checks passed";
		if ( results.isEmpty() ) {
			healthy = false;
			msg = "Health check results not available";
		} else {
			HealthCheckResult firstFailed = results.stream().filter(c -> !c.isHealthy()).findAny()
					.orElse(null);
			if ( firstFailed != null ) {
				healthy = false;
				msg = "Check [" + firstFailed.getName() + "] not healthy";
				if ( firstFailed.getMessage() != null ) {
					msg += ": " + firstFailed.getMessage();
				}
			}
		}

		PingTestResult result = new PingTestResult(healthy, msg);
		cached = new CachedResult<PingTestResult>(result, pingResultsCacheSeconds, TimeUnit.SECONDS);
		cachedResult = cached;
		return result;
	}

	public int getPingResultsCacheSeconds() {
		return pingResultsCacheSeconds;
	}

	public void setPingResultsCacheSeconds(int pingResultsCacheSeconds) {
		this.pingResultsCacheSeconds = pingResultsCacheSeconds;
	}
}
