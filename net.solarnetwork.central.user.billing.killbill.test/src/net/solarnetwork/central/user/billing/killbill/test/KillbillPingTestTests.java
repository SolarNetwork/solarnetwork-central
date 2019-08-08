/* ==================================================================
 * KillbillPingTestTests.java - 29/09/2017 4:52:48 PM
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

package net.solarnetwork.central.user.billing.killbill.test;

import static org.easymock.EasyMock.expect;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.central.user.billing.killbill.KillbillClient;
import net.solarnetwork.central.user.billing.killbill.KillbillPingTest;
import net.solarnetwork.central.user.billing.killbill.domain.HealthCheckResult;
import net.solarnetwork.domain.PingTest;

/**
 * Test cases for the {@link KillbillPingTest} class.
 * 
 * @author matt
 * @version 1.0
 */
public class KillbillPingTestTests {

	private KillbillClient client;
	private KillbillPingTest pingTest;

	@Before
	public void setup() {
		client = EasyMock.createMock(KillbillClient.class);
		pingTest = new KillbillPingTest(client);
	}

	@After
	public void teardown() {
		EasyMock.verify(client);
	}

	private void replayAll() {
		EasyMock.replay(client);
	}

	@Test
	public void pass() throws Exception {
		// given
		List<HealthCheckResult> results = Collections.singletonList(new HealthCheckResult("foo", true));
		expect(client.healthCheck()).andReturn(results);

		replayAll();

		// when
		PingTest.Result result = pingTest.performPingTest();

		// then
		assertThat(result, notNullValue());
		assertThat(result.isSuccess(), equalTo(true));
		assertThat(result.getMessage(), equalTo("All health checks passed"));
	}

	@Test
	public void passCached() throws Exception {
		// given
		List<HealthCheckResult> results = Collections.singletonList(new HealthCheckResult("foo", true));
		expect(client.healthCheck()).andReturn(results);

		replayAll();

		// when
		PingTest.Result result = pingTest.performPingTest();
		PingTest.Result cached = pingTest.performPingTest();

		// then
		assertThat(result, notNullValue());
		assertThat("Cached result returned", cached, sameInstance(result));
	}

	@Test
	public void passCachedExpired() throws Exception {
		// given
		pingTest.setPingResultsCacheSeconds(1);
		List<HealthCheckResult> results = Collections.singletonList(new HealthCheckResult("foo", true));
		expect(client.healthCheck()).andReturn(results).times(2);

		replayAll();

		// when
		PingTest.Result result = pingTest.performPingTest();
		Thread.sleep(1100); // wait for cache to expire
		PingTest.Result cached = pingTest.performPingTest();

		// then
		assertThat(result, notNullValue());
		assertThat(cached, notNullValue());
		assertThat("Expired cached result not returned", cached, not(sameInstance(result)));
	}

	@Test
	public void fail() throws Exception {
		// given
		List<HealthCheckResult> results = Collections.singletonList(new HealthCheckResult("foo", false));
		expect(client.healthCheck()).andReturn(results);

		replayAll();

		// when
		PingTest.Result result = pingTest.performPingTest();

		// then
		assertThat(result, notNullValue());
		assertThat(result.isSuccess(), equalTo(false));
		assertThat(result.getMessage(), equalTo("Check [foo] not healthy"));
	}

	@Test
	public void failWithMessage() throws Exception {
		// given
		List<HealthCheckResult> results = Collections
				.singletonList(new HealthCheckResult("foo", false, "bar"));
		expect(client.healthCheck()).andReturn(results);

		replayAll();

		// when
		PingTest.Result result = pingTest.performPingTest();

		// then
		assertThat(result, notNullValue());
		assertThat(result.isSuccess(), equalTo(false));
		assertThat(result.getMessage(), equalTo("Check [foo] not healthy: bar"));
	}

	@Test
	public void failWithinHealthy() throws Exception {
		// given
		List<HealthCheckResult> results = Arrays.asList(new HealthCheckResult("foo", true),
				new HealthCheckResult("bar", false), new HealthCheckResult("bam", true));
		expect(client.healthCheck()).andReturn(results);

		replayAll();

		// when
		PingTest.Result result = pingTest.performPingTest();

		// then
		assertThat(result, notNullValue());
		assertThat(result.isSuccess(), equalTo(false));
		assertThat(result.getMessage(), equalTo("Check [bar] not healthy"));
	}

}
