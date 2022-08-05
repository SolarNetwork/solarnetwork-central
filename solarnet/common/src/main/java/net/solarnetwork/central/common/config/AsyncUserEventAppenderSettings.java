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

import net.solarnetwork.central.biz.dao.AsyncDaoUserEventAppenderBiz;

/**
 * Settings for the {@link AsyncDaoUserEventAppenderBiz} class.
 * 
 * @author matt
 */
public class AsyncUserEventAppenderSettings {

	private int threads = 1;
	private int statFrequency = 200;
	private int queueLagAlertThreshold = 500;

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

}
