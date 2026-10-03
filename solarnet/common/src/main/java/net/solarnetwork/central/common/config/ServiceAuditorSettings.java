/* ==================================================================
 * ServiceAuditorSettings.java - 29/10/2024 9:53:27 am
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

package net.solarnetwork.central.common.config;

import java.time.Duration;

/**
 * Settings for service auditors.
 * 
 * @author matt
 * @version 1.1
 */
public class ServiceAuditorSettings {

	private long updateDelay = 100;
	private long flushDelay = 10000;
	private long connectionRecoveryDelay = 15000;
	private int statLogUpdateCount = 1000;
	private Duration shutdownMaxWait = Duration.ofSeconds(10);

	/**
	 * Constructor.
	 */
	public ServiceAuditorSettings() {
		super();
	}

	/**
	 * Get the update delay.
	 * 
	 * @return the delay, in milliseconds
	 */
	public long getUpdateDelay() {
		return updateDelay;
	}

	/**
	 * Set the update delay.
	 * 
	 * @param updateDelay
	 *        the delay to set, in milliseconds
	 */
	public void setUpdateDelay(long updateDelay) {
		this.updateDelay = updateDelay;
	}

	/**
	 * Get the flush delay.
	 * 
	 * @return the delay, in milliseconds
	 */
	public long getFlushDelay() {
		return flushDelay;
	}

	/**
	 * Set the flush delay.
	 * 
	 * @param flushDelay
	 *        the delay to set, in milliseconds
	 */
	public void setFlushDelay(long flushDelay) {
		this.flushDelay = flushDelay;
	}

	/**
	 * Get the connection recovery delay.
	 * 
	 * @return the delay, in milliseconds
	 */
	public long getConnectionRecoveryDelay() {
		return connectionRecoveryDelay;
	}

	/**
	 * Set the connection recovery delay.
	 * 
	 * @param connectionRecoveryDelay
	 *        the delay to set, in milliseconds
	 */
	public void setConnectionRecoveryDelay(long connectionRecoveryDelay) {
		this.connectionRecoveryDelay = connectionRecoveryDelay;
	}

	/**
	 * Get the statistics log update count.
	 * 
	 * @return the log update count
	 */
	public int getStatLogUpdateCount() {
		return statLogUpdateCount;
	}

	/**
	 * Set the statistics log update count.
	 * 
	 * @param statLogUpdateCount
	 *        the update count to set
	 */
	public void setStatLogUpdateCount(int statLogUpdateCount) {
		this.statLogUpdateCount = statLogUpdateCount;
	}

	/**
	 * Get the maximum time to wait for the writer to write any remaining counts
	 * and stop at shutdown.
	 *
	 * @return the maximum wait
	 * @since 1.1
	 */
	public Duration getShutdownMaxWait() {
		return shutdownMaxWait;
	}

	/**
	 * Set the maximum time to wait for the writer to write any remaining counts
	 * and stop at shutdown.
	 *
	 * @param shutdownMaxWait
	 *        the maximum wait to set
	 * @since 1.1
	 */
	public void setShutdownMaxWait(Duration shutdownMaxWait) {
		this.shutdownMaxWait = shutdownMaxWait;
	}

}
