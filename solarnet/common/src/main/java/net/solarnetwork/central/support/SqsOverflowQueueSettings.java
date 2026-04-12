/* ==================================================================
 * SqsOverflowQueueSettings.java - 30/04/2025 5:32:23 pm
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

package net.solarnetwork.central.support;

import java.time.Duration;
import net.solarnetwork.central.common.biz.impl.SqsOverflowQueue;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.SqsAsyncClientBuilder;

/**
 * Settings for the {@link SqsOverflowQueue} class.
 *
 * @author matt
 * @version 1.1
 */
public class SqsOverflowQueueSettings extends SqsProperties {

	/** The {@code statFrequency} property default value. */
	public static final int DEFAULT_STAT_FREQUENCY = 200;

	/** The {@code workQueueSize} property default value. */
	public static final int DEFUALT_WORK_QUEUE_SIZE = 100;

	private int statFrequency = DEFAULT_STAT_FREQUENCY;
	private int workQueueSize = DEFUALT_WORK_QUEUE_SIZE;
	private Duration workItemMaxWait = Duration.ofMillis(SqsOverflowQueue.DEFAULT_WORK_ITEM_MAX_WAIT_MS);
	private int readConcurrency = SqsOverflowQueue.DEFAULT_READ_CONCURRENCY;
	private int writeConcurrency = SqsOverflowQueue.DEFAULT_WRITE_CONCURRENCY;
	private int readMaxMessageCount = SqsOverflowQueue.DEFAULT_READ_MAX_MESSAGE_COUNT;
	private Duration readMaxWaitTime = Duration
			.ofSeconds(SqsOverflowQueue.DEFAULT_READ_MAX_WAIT_TIME_SECS);
	private Duration readSleepMin = Duration.ofMillis(SqsOverflowQueue.DEFAULT_READ_SLEEP_MIN_MS);
	private Duration readSleepMax = Duration.ofMillis(SqsOverflowQueue.DEFAULT_READ_SLEEP_MAX_MS);
	private Duration readSleepThrottleStep = Duration
			.ofMillis(SqsOverflowQueue.DEFAULT_READ_SLEEP_THROTTLE_STEP_MS);
	private Duration pingTestTimeout = Duration.ofMillis(SqsOverflowQueue.DEFAULT_PING_TEST_TIMEOUT_MS);

	private Duration shutdownWait = Duration.ZERO;

	/**
	 * Create an asynchronous client from the settings of this instance.
	 *
	 * @return the client
	 */
	public SqsAsyncClient newAsyncClient() {
		SqsAsyncClientBuilder builder = SqsAsyncClient.builder().region(Region.of(getRegion()));
		String accessKey = getAccessKey();
		String secretKey = getSecretKey();
		if ( accessKey != null && !accessKey.isBlank() && secretKey != null && !secretKey.isBlank() ) {
			builder.credentialsProvider(
					StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)));
		}
		return builder.build();
	}

	/**
	 * Configure a {@link SqsOverflowQueue} from these settings.
	 * 
	 * @param queue
	 *        the queue to configure
	 * @since 1.1
	 */
	public void configure(SqsOverflowQueue<?, ?> queue) {
		queue.setReadConcurrency(readConcurrency);
		queue.setWriteConcurrency(writeConcurrency);
		if ( pingTestTimeout != null ) {
			queue.setPingTestTimeoutMs(pingTestTimeout.toMillis());
		}
		if ( workItemMaxWait != null ) {
			queue.setWorkItemMaxWaitMs(workItemMaxWait.toMillis());
		}
		queue.setReadMaxMessageCount(readMaxMessageCount);
		if ( readMaxWaitTime != null ) {
			queue.setReadMaxWaitTimeSecs((int) readMaxWaitTime.toSeconds());
		}
		if ( readSleepMin != null ) {
			queue.setReadSleepMinMs(readSleepMin.toMillis());
		}
		if ( readSleepMax != null ) {
			queue.setReadSleepMaxMs(readSleepMax.toMillis());
		}
		if ( readSleepThrottleStep != null ) {
			queue.setReadSleepThrottleStepMs(readSleepThrottleStep.toMillis());
		}
		if ( shutdownWait != null ) {
			queue.setShutdownWaitSecs((int) shutdownWait.toSeconds());
		}
	}

	/**
	 * Get the statistic frequency.
	 *
	 * @return the frequency; defaults to {@link #DEFAULT_STAT_FREQUENCY}
	 */
	public final int getStatFrequency() {
		return statFrequency;
	}

	/**
	 * Set the statistic frequency.
	 *
	 * @param statFrequency
	 *        the frequency to set
	 */
	public final void setStatFrequency(int statFrequency) {
		this.statFrequency = statFrequency;
	}

	/**
	 * Get the in-memory work queue size.
	 *
	 * @return the size; defaults to {@link #DEFUALT_WORK_QUEUE_SIZE}
	 */
	public final int getWorkQueueSize() {
		return workQueueSize;
	}

	/**
	 * Set the in-memory work queue size.
	 *
	 * @param workQueueSize
	 *        the size to set
	 */
	public final void setWorkQueueSize(int workQueueSize) {
		this.workQueueSize = workQueueSize;
	}

	/**
	 * Get the number of reader threads to use.
	 *
	 * @return the number of reader threads; defaults to
	 *         {@link SqsOverflowQueue#DEFAULT_READ_CONCURRENCY}
	 */
	public final int getReadConcurrency() {
		return readConcurrency;
	}

	/**
	 * Set the number of reader threads to use.
	 *
	 * @param readConcurrency
	 *        the number of reader threads, or {@code 0} to disable reading;
	 *        anything less than {@literal 0} will be treated as {@literal 0}
	 */
	public final void setReadConcurrency(int readConcurrency) {
		if ( readConcurrency < 0 ) {
			readConcurrency = 0;
		}
		this.readConcurrency = readConcurrency;
	}

	/**
	 * Get the number of writer threads to use.
	 *
	 * @return the number of writer threads; defaults to
	 *         {@link SqsOverflowQueue#DEFAULT_WRITE_CONCURRENCY}
	 */
	public final int getWriteConcurrency() {
		return writeConcurrency;
	}

	/**
	 * Set the number of writer threads to use.
	 *
	 * @param writeConcurrency
	 *        the number of writer threads; anything less than {@literal 1} will
	 *        be treated as {@literal 1}
	 */
	public final void setWriteConcurrency(int writeConcurrency) {
		if ( writeConcurrency < 1 ) {
			writeConcurrency = 1;
		}
		this.writeConcurrency = writeConcurrency;
	}

	/**
	 * Get the maximum duration to wait for threads to finish during shutdown.
	 *
	 * @return the wait
	 */
	public final Duration getShutdownWait() {
		return shutdownWait;
	}

	/**
	 * Set the maximum duration to wait for threads to finish during shutdown.
	 *
	 * @param shutdownWait
	 *        the wait duration; anything less than {@literal 0} will be treated
	 *        as {@literal 0}
	 */
	public final void setShutdownWait(Duration shutdownWait) {
		if ( shutdownWait != null && shutdownWait.isNegative() ) {
			shutdownWait = Duration.ZERO;
		}
		this.shutdownWait = shutdownWait;
	}

	/**
	 * Get the maximum amount of time to wait for a work item to be processed.
	 *
	 * @return the maximum time
	 */
	public final Duration getWorkItemMaxWait() {
		return workItemMaxWait;
	}

	/**
	 * Set the maximum amount of time to wait for a work item to be processed.
	 *
	 * @param workItemMaxWait
	 *        the maximum time to set; if {@code null} then
	 *        {@link SqsOverflowQueue#DEFAULT_WORK_ITEM_MAX_WAIT_MS}
	 *        milliseconds will be used
	 */
	public final void setWorkItemMaxWait(Duration workItemMaxWait) {
		this.workItemMaxWait = (workItemMaxWait != null ? workItemMaxWait
				: Duration.ofMillis(SqsOverflowQueue.DEFAULT_WORK_ITEM_MAX_WAIT_MS));
	}

	/**
	 * Get the maximum number of SQS messages to read per request.
	 *
	 * @return the count; defaults to
	 *         {@link SqsOverflowQueue#DEFAULT_READ_MAX_MESSAGE_COUNT}
	 */
	public final int getReadMaxMessageCount() {
		return readMaxMessageCount;
	}

	/**
	 * Set the maximum number of SQS messages to read per request.
	 *
	 * @param readMaxMessageCount
	 *        the count to set; see AWS documentation for valid range (e.g.
	 *        1-10)
	 */
	public final void setReadMaxMessageCount(int readMaxMessageCount) {
		this.readMaxMessageCount = readMaxMessageCount;
	}

	/**
	 * Get the maximum SQS receive wait time.
	 *
	 * @return the seconds; defaults to
	 *         {@link SqsOverflowQueue#DEFAULT_READ_MAX_WAIT_TIME_SECS}
	 */
	public final Duration getReadMaxWaitTime() {
		return readMaxWaitTime;
	}

	/**
	 * Set the maximum SQS receive wait time.
	 *
	 * @param readMaxWaitTime
	 *        the seconds to set; see AWS documentation for valid range (e.g.
	 *        1-20s); if {@code null} then
	 *        {@link SqsOverflowQueue#DEFAULT_READ_MAX_WAIT_TIME_SECS} seconds
	 *        will be used
	 */
	public final void setReadMaxWaitTime(Duration readMaxWaitTime) {
		this.readMaxWaitTime = (readMaxWaitTime != null ? readMaxWaitTime
				: Duration.ofSeconds(SqsOverflowQueue.DEFAULT_READ_MAX_WAIT_TIME_SECS));
	}

	/**
	 * Get the minimum amount of time to pause after receiving messages from
	 * SQS.
	 *
	 * @return the minimum sleep amount; defaults to
	 *         {@link SqsOverflowQueue#DEFAULT_READ_SLEEP_MIN_MS}
	 */
	public final Duration getReadSleepMin() {
		return readSleepMin;
	}

	/**
	 * Set the minimum amount of time to pause after receiving messages from
	 * SQS.
	 *
	 * @param readSleepMin
	 *        the minimum sleep amount to set; if {@code null} then
	 *        {@link SqsOverflowQueue#DEFAULT_READ_SLEEP_MIN_MS} milliseconds
	 *        will be used
	 */
	public final void setReadSleepMin(Duration readSleepMin) {
		this.readSleepMin = (readSleepMin != null ? readSleepMin
				: Duration.ofMillis(SqsOverflowQueue.DEFAULT_READ_SLEEP_MIN_MS));
	}

	/**
	 * Get the maximum amount of time to pause after receiving messages from
	 * SQS.
	 *
	 * @return the minimum sleep amount; defaults to
	 *         {@link SqsOverflowQueue#DEFAULT_READ_SLEEP_MAX_MS}
	 */
	public final Duration getReadSleepMax() {
		return readSleepMax;
	}

	/**
	 * Set the maximum amount of time to pause after receiving messages from
	 * SQS.
	 *
	 * @param readSleepMax
	 *        the maximum sleep amount to set; if {@code null} then
	 *        {@link SqsOverflowQueue#DEFAULT_READ_SLEEP_MAX_MS} milliseconds
	 *        will be used
	 */
	public final void setReadSleepMax(Duration readSleepMax) {
		this.readSleepMax = (readSleepMax != null ? readSleepMax
				: Duration.ofMillis(SqsOverflowQueue.DEFAULT_READ_SLEEP_MAX_MS));
	}

	/**
	 * Get the amount of time to increase pausing after SQS receive requests for
	 * each received message that is rejected from the work queue, or to
	 * decrease after successfully offering all messages to the work queue.
	 *
	 * @return the step amount; defaults to
	 *         {@link SqsOverflowQueue#DEFAULT_READ_SLEEP_THROTTLE_STEP_MS}
	 *         milliseconds
	 */
	public final Duration getReadSleepThrottleStep() {
		return readSleepThrottleStep;
	}

	/**
	 * Set the amount of time to increase pausing after SQS receive requests for
	 * each received message that is rejected from the work queue, or to
	 * decrease after successfully offering all messages to the work queue.
	 *
	 * @param readSleepThrottleStep
	 *        the step amount to set; if {@code null} then
	 *        {@link SqsOverflowQueue#DEFAULT_READ_SLEEP_THROTTLE_STEP_MS}
	 *        milliseconds will be used
	 */
	public final void setReadSleepThrottleStep(Duration readSleepThrottleStep) {
		this.readSleepThrottleStep = (readSleepThrottleStep != null ? readSleepThrottleStep
				: Duration.ofMillis(SqsOverflowQueue.DEFAULT_READ_SLEEP_THROTTLE_STEP_MS));
	}

	/**
	 * Get the maximum amount of time to wait for the ping test to complete.
	 * 
	 * @return the timeout; defaults to
	 *         {@link SqsOverflowQueue#DEFAULT_PING_TEST_TIMEOUT_MS}
	 *         milliseconds
	 * @since 1.1
	 */
	public final Duration getPingTestTimeout() {
		return pingTestTimeout;
	}

	/**
	 * Set the maximum amount of time to wait for the ping test to complete.
	 * 
	 * @param pingTestTimeout
	 *        the timeout to set; if {@code null} then
	 *        {@link SqsOverflowQueue#DEFAULT_PING_TEST_TIMEOUT_MS} milliseconds
	 *        will be used
	 * @since 1.1
	 */
	public final void setPingTestTimeout(Duration pingTestTimeout) {
		this.pingTestTimeout = (pingTestTimeout != null ? pingTestTimeout
				: Duration.ofMillis(SqsOverflowQueue.DEFAULT_PING_TEST_TIMEOUT_MS));
	}

}
