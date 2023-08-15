/* ==================================================================
 * ServerAuthConfigurationInput.java - 8/08/2023 5:48:28 am
 * 
 * Copyright 2023 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.dnp3.domain;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.time.Instant;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import net.solarnetwork.central.dnp3.domain.ServerAuthConfiguration;
import net.solarnetwork.central.domain.UserLongStringCompositePK;

/**
 * DTO for DNP3 server auth configuration.
 * 
 * @author matt
 * @version 1.0
 */
public class ServerAuthConfigurationInput
		extends BaseDnp3ConfigurationInput<ServerAuthConfiguration, UserLongStringCompositePK> {

	@NotNull
	@NotBlank
	@Size(max = 64)
	private String name;

	@Override
	public ServerAuthConfiguration toEntity(UserLongStringCompositePK id, Instant date) {
		ServerAuthConfiguration conf = new ServerAuthConfiguration(requireNonNullArgument(id, "id"),
				date);
		populateConfiguration(conf);
		return conf;
	}

	@Override
	protected void populateConfiguration(ServerAuthConfiguration conf) {
		super.populateConfiguration(conf);
		conf.setName(name);
	}

	/**
	 * Get the name.
	 * 
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set the name
	 * 
	 * @param name
	 *        the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

}
