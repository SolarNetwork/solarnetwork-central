/* ==================================================================
 * ThreadPoolTaskSchedulerPingTest.java - 14/06/2024 1:25:28 pm
 *
 * Copyright 2024 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.scheduler;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.time.Instant;
import java.util.TreeMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import net.solarnetwork.domain.datum.AggregateDatumSamples;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.service.PingTest;
import net.solarnetwork.service.PingTestResult;

/**
 * {@link PingTest} for monitoring a thread pool executor.
 *
 * @author matt
 * @version 1.0
 */
public class ThreadPoolTaskSchedulerPingTest implements PingTest {

	private final String id;
	private final ThreadPoolTaskScheduler scheduler;

	private final AggregateDatumSamples stats = new AggregateDatumSamples(Instant.now());

	/**
	 * Constructor.
	 *
	 * <p>
	 * The ID will be set to this class' name, plus the executor's thread name
	 * prefix if available.
	 * </p>
	 *
	 * @param scheduler
	 *        the scheduler to monitor
	 */
	public ThreadPoolTaskSchedulerPingTest(ThreadPoolTaskScheduler scheduler) {
		this(defaultId(scheduler), scheduler);
	}

	private static String defaultId(ThreadPoolTaskScheduler executor) {
		requireNonNullArgument(executor, "executor");
		String id = ThreadPoolTaskSchedulerPingTest.class.getName();
		String prefix = executor.getThreadNamePrefix().trim();
		if ( prefix.endsWith("-") ) {
			id += "-" + prefix.substring(0, prefix.length() - 1);
		} else {
			id += prefix;
		}
		return id;
	}

	/**
	 * Constructor.
	 *
	 * @param id
	 *        the ping test ID
	 * @param scheduler
	 *        the scheduler to monitor
	 */
	public ThreadPoolTaskSchedulerPingTest(String id, ThreadPoolTaskScheduler scheduler) {
		super();
		this.id = requireNonNullArgument(id, "id");
		this.scheduler = requireNonNullArgument(scheduler, "scheduler");
	}

	@Override
	public String getPingTestId() {
		return id;
	}

	@Override
	public String getPingTestName() {
		return "Thread Pool Scheduler";
	}

	@Override
	public long getPingTestMaximumExecutionMilliseconds() {
		return 1000L;
	}

	private static final String ACTIVE_COUNT = "active";
	private static final String ACTIVE_COUNT_AVG = "active_avg";
	private static final String CORE_SIZE = "core-size";
	private static final String CORE_SIZE_AVG = "core-size_avg";
	private static final String POOL_SIZE = "size";
	private static final String POOL_SIZE_AVG = "size_avg";
	private static final String QUEUE_SIZE = "queue-size";
	private static final String QUEUE_SIZE_AVG = "queue-size_avg";

	@Override
	public Result performPingTest() throws Exception {
		final ScheduledExecutorService service = scheduler.getScheduledExecutor();
		final ScheduledThreadPoolExecutor executor = (service instanceof ScheduledThreadPoolExecutor e
				? e
				: null);
		final int activeCount = scheduler.getActiveCount();
		final int coreSize = (executor != null ? executor.getCorePoolSize() : 0);
		final int poolSize = scheduler.getPoolSize();
		final int queueSize = (executor != null ? executor.getQueue().size() : 0);

		DatumSamples avg;
		synchronized ( stats ) {
			stats.putInstantaneousSampleValue(ACTIVE_COUNT, activeCount);
			stats.putInstantaneousSampleValue(CORE_SIZE, coreSize);
			stats.putInstantaneousSampleValue(POOL_SIZE, poolSize);
			stats.putInstantaneousSampleValue(QUEUE_SIZE, queueSize);
			avg = stats.average(1, "%s_min", "%s_max");
		}

		// get averages, but then add "current" values
		final var props = new TreeMap<String, Object>(avg.getSampleData());
		props.put(ACTIVE_COUNT_AVG, props.get(ACTIVE_COUNT));
		props.put(ACTIVE_COUNT, activeCount);
		props.put(CORE_SIZE_AVG, props.get(CORE_SIZE));
		props.put(CORE_SIZE, coreSize);
		props.put(POOL_SIZE_AVG, props.get(POOL_SIZE));
		props.put(POOL_SIZE, poolSize);
		props.put(QUEUE_SIZE_AVG, props.get(QUEUE_SIZE));
		props.put(QUEUE_SIZE, queueSize);

		return new PingTestResult(true, null, props);
	}

}
