/* ==================================================================
 * RetrySettings.java - 17/05/2026 6:46:04 pm
 * 
 * Copyright 2026 SolarNetwork.net Dev Team
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
import java.util.function.Consumer;
import org.jspecify.annotations.Nullable;
import org.springframework.core.retry.RetryPolicy;

/**
 * General task retry settings.
 * 
 * @author matt
 * @version 1.0
 */
public class RetrySettings {

	private @Nullable Long maxRetries;
	private @Nullable Duration delay;
	private @Nullable Double multiplier;

	/**
	 * Constructor.
	 */
	public RetrySettings() {
		super();
	}

	/**
	 * Get a policy from these settings.
	 * 
	 * @return the policy
	 */
	public RetryPolicy toPolicy() {
		return toPolicy(null);
	}

	/**
	 * Get a policy from these settings.
	 * 
	 * @param customizer
	 *        an optional customize hook
	 * @return the policy
	 */
	public RetryPolicy toPolicy(@Nullable Consumer<RetryPolicy.Builder> customizer) {
		final var builder = RetryPolicy.builder();
		if ( maxRetries != null ) {
			builder.maxRetries(maxRetries);
		}
		if ( delay != null ) {
			builder.delay(delay);
		}
		if ( multiplier != null ) {
			builder.multiplier(multiplier);
		}
		if ( customizer != null ) {
			customizer.accept(builder);
		}
		return builder.build();
	}

	/**
	 * Get the maximum number of retries allowed.
	 * 
	 * @return the max number
	 */
	public final @Nullable Long getMaxRetries() {
		return maxRetries;
	}

	/**
	 * Set the maximum number of retries allowed.
	 * 
	 * @param maxRetries
	 *        the ax number to set
	 */
	public final void setMaxRetries(@Nullable Long maxRetries) {
		this.maxRetries = maxRetries;
	}

	/**
	 * Get the delay between retry attempts.
	 * 
	 * <p>
	 * If {@link #getMultiplier()} is configured, this represents the initial
	 * delay.
	 * </p>
	 * 
	 * @return the delay
	 */
	public final @Nullable Duration getDelay() {
		return delay;
	}

	/**
	 * Set the delay between retry attempts.
	 * 
	 * @param delay
	 *        the delay to set
	 */
	public final void setDelay(@Nullable Duration delay) {
		this.delay = delay;
	}

	/**
	 * Get a retry delay multiplier.
	 * 
	 * @return the multiplier to apply to the configured {@link #getDelay()}
	 *         after each retry attempt
	 */
	public final @Nullable Double getMultiplier() {
		return multiplier;
	}

	/**
	 * Set a retry delay multiplier.
	 * 
	 * @param multiplier
	 *        the delay to set
	 */
	public final void setMultiplier(@Nullable Double multiplier) {
		this.multiplier = multiplier;
	}

}
