/* ==================================================================
 * HttpClientSettings.java - 13/03/2025 3:20:03 pm
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

package net.solarnetwork.central.common.http;

import java.time.Duration;
import org.jspecify.annotations.Nullable;

/**
 * Settings for the HTTP client.
 * 
 * @author matt
 * @version 1.0
 */
public class HttpClientSettings {

	/** The {@code connectTimeout} property default value. */
	public static final long DEFAULT_CONNECT_TIMEOUT_SECS = 10L;

	/** The {@code connectionKeepAlive} property default value. */
	public static final long DEFAULT_CONNECTION_KEEP_ALIVE_SECS = 60L;

	/** The {@code connectionRequestTimeout} property default value. */
	public static final long DEFAULT_CONNECTION_REQUEST_TIMEOUT_SECS = 15L;

	/** The {@code connectionTimeToLive} property default value. */
	public static final long DEFAULT_CONNECTION_TTL_SECS = 60L;

	/** The {@code connectionValidateAfterInactivity} property default value. */
	public static final long DEFAULT_CONNECTION_VALIDATE_AFTER_INACTIVITY_SECS = 20L;

	/** The {@code socketTimeout} property default value. */
	public static final long DEFAULT_SOCKET_TIMEOUT_SECONDS = 20L;

	private Duration connectTimeout = Duration.ofSeconds(DEFAULT_CONNECT_TIMEOUT_SECS);
	private Duration connectionKeepAlive = Duration.ofSeconds(DEFAULT_CONNECTION_KEEP_ALIVE_SECS);
	private Duration connectionRequestTimeout = Duration
			.ofSeconds(DEFAULT_CONNECTION_REQUEST_TIMEOUT_SECS);
	private Duration connectionTimeToLive = Duration.ofSeconds(DEFAULT_CONNECTION_TTL_SECS);
	private Duration connectionValidateAfterInactivity = Duration
			.ofSeconds(DEFAULT_CONNECTION_VALIDATE_AFTER_INACTIVITY_SECS);
	private Duration socketTimeout = Duration.ofSeconds(DEFAULT_SOCKET_TIMEOUT_SECONDS);

	/**
	 * Get the connection timeout.
	 *
	 * @return the timeout; defaults to {@link #DEFAULT_CONNECT_TIMEOUT_SECS}
	 */
	public final Duration getConnectTimeout() {
		return connectTimeout;
	}

	/**
	 * Set the connection timeout.
	 *
	 * @param connectTimeout
	 *        the timeout to set; if {@code null} then
	 *        {@link #DEFAULT_CONNECT_TIMEOUT_SECS} will be used
	 */
	public final void setConnectTimeout(Duration connectTimeout) {
		this.connectTimeout = (connectTimeout != null ? connectTimeout
				: Duration.ofSeconds(DEFAULT_CONNECT_TIMEOUT_SECS));
	}

	/**
	 * Get the connection keep-alive default timeout.
	 * 
	 * @return the timeout; defaults to
	 *         {@link #DEFAULT_CONNECTION_KEEP_ALIVE_SECS}
	 */
	public final Duration getConnectionKeepAlive() {
		return connectionKeepAlive;
	}

	/**
	 * Set the connection keep-alive default timeout.
	 * 
	 * @param connectionKeepAlive
	 *        the timeout to set; if {@code null} then
	 *        {@link #DEFAULT_CONNECTION_KEEP_ALIVE_SECS} will be used
	 */
	public final void setConnectionKeepAlive(@Nullable Duration connectionKeepAlive) {
		this.connectionKeepAlive = (connectionKeepAlive != null ? connectionKeepAlive
				: Duration.ofSeconds(DEFAULT_CONNECTION_KEEP_ALIVE_SECS));
	}

	/**
	 * Get the connection pool borrow timeout.
	 *
	 * @return the timeout; defaults to
	 *         {@link #DEFAULT_CONNECTION_REQUEST_TIMEOUT_SECS}
	 */
	public final Duration getConnectionRequestTimeout() {
		return connectionRequestTimeout;
	}

	/**
	 * Set the connection pool borrow timeout.
	 *
	 * @param connectionRequestTimeout
	 *        the connectionRequestTimeout to set; if {@code null} then
	 *        {@link #DEFAULT_CONNECTION_REQUEST_TIMEOUT_SECS} will be used
	 */
	public final void setConnectionRequestTimeout(@Nullable Duration connectionRequestTimeout) {
		this.connectionRequestTimeout = (connectionRequestTimeout != null ? connectionRequestTimeout
				: Duration.ofSeconds(DEFAULT_CONNECTION_REQUEST_TIMEOUT_SECS));
	}

	/**
	 * Get the connection time to live timeout.
	 * 
	 * @return the timeout; defaults to {@link #DEFAULT_CONNECTION_TTL_SECS}
	 */
	public final Duration getConnectionTimeToLive() {
		return connectionTimeToLive;
	}

	/**
	 * Set the connection time to live timeout.
	 * 
	 * @param connectionTimeToLive
	 *        the timeout to set; if {@code null} then
	 *        {@link #DEFAULT_CONNECTION_TTL_SECS will be used
	 */
	public final void setConnectionTimeToLive(Duration connectionTimeToLive) {
		this.connectionTimeToLive = (connectionTimeToLive != null ? connectionTimeToLive
				: Duration.ofSeconds(DEFAULT_CONNECTION_TTL_SECS));
	}

	/**
	 * Get the connection validate-after-inactivity timeout.
	 * 
	 * @return the timeout; deafults to
	 *         {@link #DEFAULT_CONNECTION_VALIDATE_AFTER_INACTIVITY_SECS}
	 */
	public final Duration getConnectionValidateAfterInactivity() {
		return connectionValidateAfterInactivity;
	}

	/**
	 * Set the connection validate-after-inactivity timeout.
	 * 
	 * @param connectionValidateAfterInactivity
	 *        the timeout to set; if {@code null} then
	 *        {@link #DEFAULT_CONNECTION_VALIDATE_AFTER_INACTIVITY_SECS} will be
	 *        used
	 */
	public final void setConnectionValidateAfterInactivity(
			@Nullable Duration connectionValidateAfterInactivity) {
		this.connectionValidateAfterInactivity = (connectionValidateAfterInactivity != null
				? connectionValidateAfterInactivity
				: Duration.ofSeconds(DEFAULT_CONNECTION_VALIDATE_AFTER_INACTIVITY_SECS));
	}

	/**
	 * Get the socket timeout.
	 *
	 * @return the timeout; defaults to {@link #DEFAULT_SOCKET_TIMEOUT_SECONDS}
	 */
	public final Duration getSocketTimeout() {
		return socketTimeout;
	}

	/**
	 * Set the socket timeout.
	 *
	 * @param socketTimeout
	 *        the timeout to set; if {@code null} then
	 *        {@link #DEFAULT_SOCKET_TIMEOUT_SECONDS} will be used
	 */
	public final void setSocketTimeout(@Nullable Duration socketTimeout) {
		this.socketTimeout = (socketTimeout != null ? socketTimeout
				: Duration.ofSeconds(DEFAULT_SOCKET_TIMEOUT_SECONDS));
	}

}
