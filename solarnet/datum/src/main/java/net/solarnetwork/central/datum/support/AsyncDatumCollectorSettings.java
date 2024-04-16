/* ==================================================================
 * AsyncDatumCollectorSettings.java - 4/10/2021 4:50:12 PM
 *
 * Copyright 2021 SolarNetwork.net Dev Team
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

/**
 * Settings for the AsyncDatumCollector.
 *
 * @author matt
 * @version 1.1
 */
public class AsyncDatumCollectorSettings {

	private int threads = AsyncDatumCollector.DEFAULT_CONCURRENCY;
	private int shutdownWaitSecs = AsyncDatumCollector.DEFAULT_SHUTDOWN_WAIT_SECS;
	private int queueSize = AsyncDatumCollector.DEFAULT_QUEUE_SIZE;
	private int datumCacheRemovalAlertThreshold = AsyncDatumCollector.DEFAULT_DATUM_CACHE_REMOVAL_ALERT_THRESHOLD;
	private double queueRefillThreshold = AsyncDatumCollector.DEFAULT_QUEUE_REFILL_THRESHOLD;
	private int statFrequency = 200;

	/**
	 * Get the thread count.
	 *
	 * @return the thread count
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
	 * Get the shutdown wait time, in seconds.
	 *
	 * @return the seconds
	 */
	public int getShutdownWaitSecs() {
		return shutdownWaitSecs;
	}

	/**
	 * Set the shutdown wait time, in seconds.
	 *
	 * @param shutdownWaitSecs
	 *        the seconds to set
	 */
	public void setShutdownWaitSecs(int shutdownWaitSecs) {
		this.shutdownWaitSecs = shutdownWaitSecs;
	}

	/**
	 * Get the queue size.
	 *
	 * @return the queueSize
	 */
	public int getQueueSize() {
		return queueSize;
	}

	/**
	 * Set the queue size.
	 *
	 * @param queueSize
	 *        the queueSize to set
	 */
	public void setQueueSize(int queueSize) {
		this.queueSize = queueSize;
	}

	/**
	 * Get the statistic frequency.
	 *
	 * @return the frequency
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
	 * @return the threshold
	 */
	public int getDatumCacheRemovalAlertThreshold() {
		return datumCacheRemovalAlertThreshold;
	}

	/**
	 * Set the cache removal alert threshold.
	 *
	 * @param datumCacheRemovalAlertThreshold
	 *        the threshold to set
	 */
	public void setDatumCacheRemovalAlertThreshold(int datumCacheRemovalAlertThreshold) {
		this.datumCacheRemovalAlertThreshold = datumCacheRemovalAlertThreshold;
	}

	/**
	 * Get the percentage full threshold that triggers a "refill" from the
	 * cache.
	 *
	 * @return the threshold
	 * @since 1.1
	 */
	public double getQueueRefillThreshold() {
		return queueRefillThreshold;
	}

	/**
	 * Set the percentage full threshold that triggers a "refill" from the
	 * cache.
	 *
	 * @param queueRefillThreshold
	 *        the threshold to set
	 * @since 1.1
	 */
	public void setQueueRefillThreshold(double queueRefillThreshold) {
		this.queueRefillThreshold = queueRefillThreshold;
	}

}
