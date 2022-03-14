/* ==================================================================
 * SolarNodeMatch.java - 28/05/2018 2:52:30 PM
 * 
 * Copyright 2018 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import net.solarnetwork.domain.SerializeIgnore;

/**
 * A "match" to a {@link SolarNode}.
 * 
 * <p>
 * Although this class extends {@link SolarNode} that is merely an
 * implementation detail.
 * </p>
 * 
 * @author matt
 * @version 2.0
 * @since 1.40
 */
public class SolarNodeMatch extends SolarNode implements SolarNodeFilterMatch {

	private static final long serialVersionUID = 7972204667914199552L;

	private String metaJson;

	@Override
	@JsonIgnore
	@SerializeIgnore
	public String getMetaJson() {
		return metaJson;
	}

	public void setMetaJson(String infoJson) {
		this.metaJson = infoJson;
	}

}
