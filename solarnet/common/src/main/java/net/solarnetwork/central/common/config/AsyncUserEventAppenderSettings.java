/* ==================================================================
 * AsyncUserEventAppenderSettings.java - 1/08/2022 4:42:46 pm
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

package net.solarnetwork.central.common.config;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import net.solarnetwork.central.biz.dao.AsyncDaoUserEventAppenderBiz;

/**
 * Settings for the {@link AsyncDaoUserEventAppenderBiz} class.
 * 
 * @author matt
 * @version 1.1
 */
public class AsyncUserEventAppenderSettings {

	/** The {@code threads} property default value. */
	public static final int DEFAULT_THREADS = 1;

	/** The {@code statFrequency} property default value. */
	public static final int DEFAULT_STAT_FREQUENCY = 200;

	/** The {@code queueLagAlertThreshold} property default value. */
	public static final int DEFAULT_QUEUE_LAG_ALERT_THRESHOLD = 500;

	/** The {@code queueCapacity} property default value. */
	public static final int DEFAULT_QUEUE_CAPACITY = 500_000;

	private int threads = DEFAULT_THREADS;
	private int statFrequency = DEFAULT_STAT_FREQUENCY;
	private int queueLagAlertThreshold = DEFAULT_QUEUE_LAG_ALERT_THRESHOLD;
	private int queueCapacity = DEFAULT_QUEUE_CAPACITY;

	/**
	 * Constructor.
	 */
	public AsyncUserEventAppenderSettings() {
		super();
	}

	/**
	 * Create a thread pool based on these settings.
	 * 
	 * @return the pool
	 */
	public ThreadPoolExecutor createThreadPool() {
		BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>(
				queueCapacity < 1 ? Integer.MAX_VALUE : queueCapacity);
		ThreadPoolExecutor executor = new ThreadPoolExecutor(threads, threads, 5L, TimeUnit.MINUTES,
				queue, new CustomizableThreadFactory("UserEventAppender-"));
		executor.allowCoreThreadTimeOut(true);
		return executor;
	}

	/**
	 * Get the thread count.
	 * 
	 * @return the thread count; defaults to {@link #DEFAULT_THREADS}
	 */
	public int getThreads() {
		return threads;
	}

	/**
	 * Set the thread count.
	 * 
	 * @param threads
	 *        the thread count to set
	 */
	public void setThreads(int threads) {
		this.threads = threads;
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
	 * Get the cache removal alert threshold.
	 * 
	 * @return the threshold; defaults to
	 *         {@link #DEFAULT_QUEUE_LAG_ALERT_THRESHOLD}
	 */
	public int getQueueLagAlertThreshold() {
		return queueLagAlertThreshold;
	}

	/**
	 * Set the cache removal alert threshold.
	 * 
	 * @param queueLagAlertThreshold
	 *        the threshold to set
	 */
	public void setQueueLagAlertThreshold(int queueLagAlertThreshold) {
		this.queueLagAlertThreshold = queueLagAlertThreshold;
	}

	/**
	 * Get the maximum queue capacity.
	 * 
	 * @return the capacity, or {@code 0} for no limit; defaults to
	 *         {@link #DEFAULT_QUEUE_CAPACITY}
	 * @since 1.1
	 */
	public final int getQueueCapacity() {
		return queueCapacity;
	}

	/**
	 * Set the maximum queue capacity.
	 * 
	 * @param queueCapacity
	 *        the capacity to set, or {@code 0} for no limit
	 * @since 1.1
	 */
	public final void setQueueCapacity(int queueCapacity) {
		this.queueCapacity = queueCapacity;
	}

}
