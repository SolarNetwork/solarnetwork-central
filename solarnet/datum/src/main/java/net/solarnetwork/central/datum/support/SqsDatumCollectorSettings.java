/* ==================================================================
 * SqsDatumCollectorSettings.java - 30/04/2025 5:32:23 pm
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

package net.solarnetwork.central.datum.support;

import java.time.Duration;
import net.solarnetwork.central.support.SqsProperties;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.SqsAsyncClientBuilder;

/**
 * Settings for the {@link SqsDatumCollector} class.
 *
 * @author matt
 * @version 1.1
 */
public class SqsDatumCollectorSettings extends SqsProperties {

	/** The {@code statFrequency} property default value. */
	public static final int DEFAULT_STAT_FREQUENCY = 200;

	/** The {@code workQueueSize} property default value. */
	public static final int DEFUALT_WORK_QUEUE_SIZE = 100;

	private int statFrequency = DEFAULT_STAT_FREQUENCY;
	private int workQueueSize = DEFUALT_WORK_QUEUE_SIZE;
	private Duration workItemMaxWait = Duration
			.ofMillis(SqsDatumCollector.DEFAULT_WORK_ITEM_MAX_WAIT_MS);
	private int readConcurrency = SqsDatumCollector.DEFAULT_READ_CONCURRENCY;
	private int writeConcurrency = SqsDatumCollector.DEFAULT_WRITE_CONCURRENCY;
	private int readMaxMessageCount = SqsDatumCollector.DEFAULT_READ_MAX_MESSAGE_COUNT;
	private Duration readMaxWaitTime = Duration
			.ofSeconds(SqsDatumCollector.DEFAULT_READ_MAX_WAIT_TIME_SECS);
	private Duration readSleepMin = Duration.ofMillis(SqsDatumCollector.DEFAULT_READ_SLEEP_MIN_MS);
	private Duration readSleepMax = Duration.ofMillis(SqsDatumCollector.DEFAULT_READ_SLEEP_MAX_MS);
	private Duration readSleepThrottleStep = Duration
			.ofMillis(SqsDatumCollector.DEFAULT_READ_SLEEP_THROTTLE_STEP_MS);

	private Duration shutdownWait;

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
	 * Get the statistic frequency.
	 *
	 * @return the frequency; defaults to {@link #DEFAULT_STAT_FREQUENCY}
	 */
	public int getStatFrequency() {
		return statFrequency;
	}

	/**
	 * Set the statistic frequency.
	 *
	 * @param statFrequency
	 *        the frequency to set
	 */
	public void setStatFrequency(int statFrequency) {
		this.statFrequency = statFrequency;
	}

	/**
	 * Get the in-memory work queue size.
	 *
	 * @return the size; defaults to {@link #DEFUALT_WORK_QUEUE_SIZE}
	 */
	public int getWorkQueueSize() {
		return workQueueSize;
	}

	/**
	 * Set the in-memory work queue size.
	 *
	 * @param workQueueSize
	 *        the size to set
	 */
	public void setWorkQueueSize(int workQueueSize) {
		this.workQueueSize = workQueueSize;
	}

	/**
	 * Get the number of reader threads to use.
	 *
	 * @return the number of reader threads; defaults to
	 *         {@link SqsDatumCollector#DEFAULT_READ_CONCURRENCY}
	 */
	public int getReadConcurrency() {
		return readConcurrency;
	}

	/**
	 * Set the number of reader threads to use.
	 *
	 * @param readConcurrency
	 *        the number of reader threads, or {@code 0} to disable reading;
	 *        anything less than {@literal 0} will be treated as {@literal 0}
	 */
	public void setReadConcurrency(int readConcurrency) {
		if ( readConcurrency < 0 ) {
			readConcurrency = 0;
		}
		this.readConcurrency = readConcurrency;
	}

	/**
	 * Get the number of writer threads to use.
	 *
	 * @return the number of writer threads; defaults to
	 *         {@link SqsDatumCollector#DEFAULT_WRITE_CONCURRENCY}
	 */
	public int getWriteConcurrency() {
		return writeConcurrency;
	}

	/**
	 * Set the number of writer threads to use.
	 *
	 * @param writeConcurrency
	 *        the number of writer threads; anything less than {@literal 1} will
	 *        be treated as {@literal 1}
	 */
	public void setWriteConcurrency(int writeConcurrency) {
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
	public Duration getShutdownWait() {
		return shutdownWait;
	}

	/**
	 * Set the maximum duration to wait for threads to finish during shutdown.
	 *
	 * @param shutdownWait
	 *        the wait duration; anything less than {@literal 0} will be treated
	 *        as {@literal 0}
	 */
	public void setShutdownWait(Duration shutdownWait) {
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
	public Duration getWorkItemMaxWait() {
		return workItemMaxWait;
	}

	/**
	 * Set the maximum amount of time to wait for a work item to be processed.
	 *
	 * @param workItemMaxWait
	 *        the maximum time to set
	 */
	public void setWorkItemMaxWait(Duration workItemMaxWait) {
		this.workItemMaxWait = workItemMaxWait;
	}

	/**
	 * Get the maximum number of SQS messages to read per request.
	 *
	 * @return the count; defaults to
	 *         {@link SqsDatumCollector#DEFAULT_READ_MAX_MESSAGE_COUNT}
	 */
	public int getReadMaxMessageCount() {
		return readMaxMessageCount;
	}

	/**
	 * Set the maximum number of SQS messages to read per request.
	 *
	 * @param readMaxMessageCount
	 *        the count to set; see AWS documentation for valid range (e.g.
	 *        1-10)
	 */
	public void setReadMaxMessageCount(int readMaxMessageCount) {
		this.readMaxMessageCount = readMaxMessageCount;
	}

	/**
	 * Get the maximum SQS receive wait time.
	 *
	 * @return the seconds; defaults to
	 *         {@link SqsDatumCollector#DEFAULT_READ_MAX_WAIT_TIME_SECS}
	 */
	public Duration getReadMaxWaitTime() {
		return readMaxWaitTime;
	}

	/**
	 * Set the maximum SQS receive wait time.
	 *
	 * @param readMaxWaitTime
	 *        the seconds to set; see AWS documentation for valid range (e.g.
	 *        1-20s)
	 */
	public void setReadMaxWaitTime(Duration readMaxWaitTime) {
		this.readMaxWaitTime = readMaxWaitTime;
	}

	/**
	 * Get the minimum amount of time to pause after receiving messages from
	 * SQS.
	 *
	 * @return the minimum sleep amount; defaults to
	 *         {@link SqsDatumCollector#DEFAULT_READ_SLEEP_MIN_MS}
	 */
	public Duration getReadSleepMin() {
		return readSleepMin;
	}

	/**
	 * Set the minimum amount of time to pause after receiving messages from
	 * SQS.
	 *
	 * @param readSleepMin
	 *        the minimum sleep amount to set
	 */
	public void setReadSleepMin(Duration readSleepMin) {
		this.readSleepMin = readSleepMin;
	}

	/**
	 * Get the maximum amount of time to pause after receiving messages from
	 * SQS.
	 *
	 * @return the minimum sleep amount; defaults to
	 *         {@link SqsDatumCollector#DEFAULT_READ_SLEEP_MAX_MS}
	 */
	public Duration getReadSleepMax() {
		return readSleepMax;
	}

	/**
	 * Set the maximum amount of time to pause after receiving messages from
	 * SQS.
	 *
	 * @param readSleepMax
	 *        the maximum sleep amount to set
	 */
	public void setReadSseepMax(Duration readSleepMax) {
		this.readSleepMax = readSleepMax;
	}

	/**
	 * Get the amount of time to increase pausing after SQS receive requests for
	 * each received message that is rejected from the work queue, or to
	 * decrease after successfully offering all messages to the work queue.
	 *
	 * @return the step amount, in milliseconds; defaults to
	 *         {@link SqsDatumCollector#DEFAULT_READ_SLEEP_THROTTLE_STEP_MS}
	 */
	public Duration getReadSleepThrottleStep() {
		return readSleepThrottleStep;
	}

	/**
	 * Set the amount of time to increase pausing after SQS receive requests for
	 * each received message that is rejected from the work queue, or to
	 * decrease after successfully offering all messages to the work queue.
	 *
	 * @param readSleepThrottleStep
	 *        the step amount to set
	 */
	public void setReadRejectionSleepStep(Duration readSleepThrottleStep) {
		this.readSleepThrottleStep = readSleepThrottleStep;
	}

}
