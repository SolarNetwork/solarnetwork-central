/* ==================================================================
 * HealthCheckResult.java - 29/09/2017 4:15:22 PM
 * 
 * Copyright 2017 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.billing.killbill.domain;

/**
 * An individual health check result.
 * 
 * @author matt
 * @version 1.0
 */
public class HealthCheckResult {

	private final String name;
	private final boolean healthy;
	private final String message;

	/**
	 * Constructor.
	 * 
	 * @param name
	 *        the check name
	 * @param healthy
	 *        {@literal true} if the check passed
	 */
	public HealthCheckResult(String name, boolean healthy) {
		this(name, healthy, null);
	}

	/**
	 * Constructor.
	 * 
	 * @param name
	 *        the check name
	 * @param healthy
	 *        {@literal true} if the check passed
	 * @param message
	 *        an optional message
	 */
	public HealthCheckResult(String name, boolean healthy, String message) {
		super();
		this.name = name;
		this.healthy = healthy;
		this.message = message;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (healthy ? 1231 : 1237);
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>
	 * Compares the {@code name} and {@code healthy} flags of another
	 * {@link HealthCheckResult} for equality with this instance.
	 * </p>
	 */
	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( obj == null ) {
			return false;
		}
		if ( !(obj instanceof HealthCheckResult) ) {
			return false;
		}
		HealthCheckResult other = (HealthCheckResult) obj;
		if ( healthy != other.healthy ) {
			return false;
		}
		if ( name == null ) {
			if ( other.name != null ) {
				return false;
			}
		} else if ( !name.equals(other.name) ) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "HealthCheckResult{name=" + name + ", healthy=" + healthy + ", message=" + message + "}";
	}

	/**
	 * Get the name of the health check.
	 * 
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Get the health check status.
	 * 
	 * @return {@literal true} if the check passed and is considered "healthy"
	 */
	public boolean isHealthy() {
		return healthy;
	}

	/**
	 * Get an optional message.
	 * 
	 * @return the message
	 */
	public String getMessage() {
		return message;
	}

}
