/* ==================================================================
 * CloudDatumStreamPollTaskProcessorTests.java - 23/09/2025 6:58:41 am
 *
 * Copyright 2025 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.c2c.job.test;

import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static org.assertj.core.api.BDDAssertions.and;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import net.solarnetwork.central.c2c.biz.CloudDatumStreamPollService;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamPollTaskEntity;
import net.solarnetwork.central.c2c.job.CloudDatumStreamPollTaskProcessor;

/**
 * Test cases for the {@link CloudDatumStreamPollTaskProcessor} class.
 *
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class CloudDatumStreamPollTaskProcessorTests {

	private static final int MAX_THREADS = 2;

	@Mock
	private CloudDatumStreamPollService service;

	private final AtomicInteger serviceThreadCount = new AtomicInteger(0);
	private ThreadPoolTaskExecutor serviceExecutor;
	private TestProcessor job;

	private static final class TestProcessor extends CloudDatumStreamPollTaskProcessor {

		private final ThreadPoolTaskExecutor executor;

		private TestProcessor(CloudDatumStreamPollService service) {
			super(service);
			executor = new ThreadPoolTaskExecutor();
			executor.setCorePoolSize(4);
			executor.setMaxPoolSize(4);
			executor.setAllowCoreThreadTimeOut(false);
			executor.initialize();
			setParallelTaskExecutor(executor);
		}

	}

	@BeforeEach
	public void setup() {
		serviceExecutor = new ThreadPoolTaskExecutor();
		serviceExecutor.setCorePoolSize(MAX_THREADS);
		serviceExecutor.setMaxPoolSize(MAX_THREADS);
		serviceExecutor.setAllowCoreThreadTimeOut(true);
		serviceExecutor.setQueueCapacity(0);
		serviceExecutor.setThreadFactory(new ThreadFactory() {

			@Override
			public Thread newThread(Runnable r) {
				return new Thread(r,
						"CloudDatumStreamPollService-" + serviceThreadCount.incrementAndGet());
			}
		});
		serviceExecutor.initialize();
		job = new TestProcessor(service);
		job.setParallelism(1);
	}

	@AfterEach
	public void teardown() {
		job.executor.shutdown();
		serviceExecutor.shutdown();
	}

	@Test
	public void executeParallelTasks() {
		// GIVEN
		final Long userId = randomLong();

		// configure more tasks than threads to process them, so one is rejected
		final int taskCount = 2;
		final List<CloudDatumStreamPollTaskEntity> tasks = new ArrayList<>();
		for ( int i = 0; i < taskCount; i++ ) {
			var task = new CloudDatumStreamPollTaskEntity(userId, randomLong());
			tasks.add(task);
		}

		// claim 3 tasks
		given(service.claimQueuedTask()).willReturn(tasks.get(0),
				new CloudDatumStreamPollTaskEntity[] { tasks.get(1), null });

		// execute submitted tasks
		final List<CloudDatumStreamPollTaskEntity> processedTasks = Collections
				.synchronizedList(new ArrayList<>(taskCount));
		final CountDownLatch latch = new CountDownLatch(2);

		given(service.executeTask(any())).willAnswer(invocation -> {
			final var arg = (CloudDatumStreamPollTaskEntity) invocation.getArgument(0);
			return serviceExecutor.submit(() -> {
				processedTasks.add(arg);
				try {
					Thread.sleep(250L);
				} catch ( InterruptedException e ) {
					// ignore
				} finally {
					latch.countDown();
				}
				return arg;
			});
		});

		// WHEN
		job.setMaximumIterations(MAX_THREADS);
		job.run();
		try {
			latch.await(2, TimeUnit.SECONDS);
		} catch ( InterruptedException e ) {
			// continue
		}

		// THEN

		// @formatter:off
		and.then(serviceThreadCount.get())
			.as("Maximum task threads created")
			.isEqualTo(MAX_THREADS)
			;

		and.then(processedTasks)
			.as("All tasks processed")
			.containsOnly(tasks.toArray(CloudDatumStreamPollTaskEntity[]::new))
			;
		// @formatter:on
	}

	@Test
	public void executeParallelTasks_rejected() {
		// GIVEN
		final Long userId = randomLong();

		// configure more tasks than threads to process them, so one is rejected
		final int taskCount = 3;
		final List<CloudDatumStreamPollTaskEntity> tasks = new ArrayList<>();
		for ( int i = 0; i < taskCount; i++ ) {
			var task = new CloudDatumStreamPollTaskEntity(userId, randomLong());
			tasks.add(task);
		}

		// claim 3 tasks
		given(service.claimQueuedTask()).willReturn(tasks.get(0),
				new CloudDatumStreamPollTaskEntity[] { tasks.get(1), tasks.get(2), null });

		// execute submitted tasks
		final List<CloudDatumStreamPollTaskEntity> processedTasks = Collections
				.synchronizedList(new ArrayList<>(taskCount));
		final CountDownLatch latch = new CountDownLatch(2);

		given(service.executeTask(any())).willAnswer(invocation -> {
			final var arg = (CloudDatumStreamPollTaskEntity) invocation.getArgument(0);
			return serviceExecutor.submit(() -> {
				processedTasks.add(arg);
				try {
					Thread.sleep(250L);
				} catch ( InterruptedException e ) {
					// ignore
				} finally {
					latch.countDown();
				}
				return arg;
			});
		});

		// WHEN
		job.run();
		try {
			latch.await(2, TimeUnit.SECONDS);
		} catch ( InterruptedException e ) {
			// continue
		}

		// THEN

		// @formatter:off
		and.then(serviceThreadCount.get())
			.as("Maximum task threads created")
			.isEqualTo(MAX_THREADS)
			;

		and.then(processedTasks)
			.as("First two tasks processed (3rd rejected)")
			.containsOnly(tasks.subList(0, MAX_THREADS).toArray(CloudDatumStreamPollTaskEntity[]::new))
			;
		// @formatter:on
	}

}
