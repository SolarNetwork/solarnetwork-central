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
import com.fasterxml.jackson.annotation.JsonProperty;
import net.solarnetwork.central.dao.CopyingIdentity;
import net.solarnetwork.dao.BasicUuidEntity;

/**
 * A system configuration entity.
 * 
 * @author matt
 * @version 1.0
 */
public class SystemConfiguration extends BasicUuidEntity
		implements Cloneable, CopyingIdentity<UUID, SystemConfiguration> {

	private static final long serialVersionUID = -9139184149203641821L;

	private String inToken;
	private String outToken;
	private String oscpVersion;
	private String baseUrl;

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

}
