/* ==================================================================
 * SystemConfiguration.java - 23/08/2022 11:58:07 am
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

package net.solarnetwork.oscp.sim.cp.domain;

import java.time.Instant;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import net.solarnetwork.central.oscp.domain.SystemSettings;
import net.solarnetwork.dao.BasicUuidEntity;
import net.solarnetwork.domain.CopyingIdentity;
import net.solarnetwork.util.ObjectUtils;

/**
 * A system configuration entity.
 * 
 * @author matt
 * @version 1.0
 */
public class SystemConfiguration extends BasicUuidEntity
		implements Cloneable, CopyingIdentity<UUID, SystemConfiguration> {

	private static final long serialVersionUID = 7911753153368818260L;

	private String inToken;
	private String outToken;
	private String oscpVersion;
	private String baseUrl;
	private SystemSettings settings;
	private Instant heartbeatDate;
	private Instant offlineDate;

	/**
	 * Constructor.
	 * 
	 * @param id
	 *        the ID
	 * @param created
	 *        the creation date
	 */
	@JsonCreator
	public SystemConfiguration(@JsonProperty("id") UUID id, @JsonProperty("created") Instant created) {
		super(id, created);
	}

	/**
	 * Test if the heartbeat date is expired now.
	 * 
	 * @return {@literal true} if {@code heartbeatSecs} is configured and
	 *         {@code heartbeatDate} is {@literal null} or expired right now
	 */
	@JsonIgnore
	public boolean isHeartbeatExpired() {
		return isHeartbeatExpired(Instant.now());
	}

	/**
	 * Test if the heartbeat date is expired.
	 * 
	 * @param at
	 *        the date at which to test if the heartbeat has expired
	 * @return {@literal true} if {@code heartbeatSecs} is configured and
	 *         {@code heartbeatDate} is {@literal null} or expired at the given
	 *         date
	 */
	public boolean isHeartbeatExpired(Instant at) {
		ObjectUtils.requireNonNullArgument(at, "at");

		final long secs = (settings != null && settings.heartbeatSeconds() != null
				? settings.heartbeatSeconds().longValue()
				: 0L);
		if ( secs < 1 ) {
			return false;
		}
		final Instant lastHeartbeat = (heartbeatDate != null ? heartbeatDate : null);
		return (lastHeartbeat == null || lastHeartbeat.plusSeconds(secs).isBefore(at));
	}

	/**
	 * Test if the offline date is expired now.
	 * 
	 * @return {@literal true} if {@code offlineDate} is not {@literal null} and
	 *         expired right now
	 */
	@JsonIgnore
	public boolean isOffline() {
		return isOffline(Instant.now());
	}

	/**
	 * Test if the offline date is expired.
	 * 
	 * @param at
	 *        the date at which to test if the offline date has expired
	 * @return {@literal true} if {@code offlineDate} is not {@literal null} and
	 *         expired right now
	 */
	public boolean isOffline(Instant at) {
		ObjectUtils.requireNonNullArgument(at, "at");
		final Instant offline = getOfflineDate();
		return (offline != null && offline.isBefore(at));
	}

	@Override
	public SystemConfiguration clone() {
		return (SystemConfiguration) super.clone();
	}

	@Override
	public SystemConfiguration copyWithId(UUID id) {
		var copy = new SystemConfiguration(id, getCreated() != null ? getCreated() : Instant.now());
		copyTo(copy);
		return copy;
	}

	@Override
	public void copyTo(SystemConfiguration entity) {
		entity.setInToken(inToken);
		entity.setOutToken(outToken);
		entity.setOscpVersion(oscpVersion);
		entity.setBaseUrl(baseUrl);
	}

	public String getInToken() {
		return inToken;
	}

	public void setInToken(String inToken) {
		this.inToken = inToken;
	}

	public String getOutToken() {
		return outToken;
	}

	public void setOutToken(String outToken) {
		this.outToken = outToken;
	}

	public String getOscpVersion() {
		return oscpVersion;
	}

	public void setOscpVersion(String oscpVersion) {
		this.oscpVersion = oscpVersion;
	}

	public String getBaseUrl() {
		return baseUrl;
	}

	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}

	public SystemSettings getSettings() {
		return settings;
	}

	public void setSettings(SystemSettings settings) {
		this.settings = settings;
	}

	public Instant getHeartbeatDate() {
		return heartbeatDate;
	}

	public void setHeartbeatDate(Instant heartbeatDate) {
		this.heartbeatDate = heartbeatDate;
	}

	public Instant getOfflineDate() {
		return offlineDate;
	}

	public void setOfflineDate(Instant offlineDate) {
		this.offlineDate = offlineDate;
	}

}
