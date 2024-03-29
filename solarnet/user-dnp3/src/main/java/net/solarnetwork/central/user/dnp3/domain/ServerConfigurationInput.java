/* ==================================================================
 * ServerConfigurationInput.java - 7/08/2023 10:31:14 am
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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import net.solarnetwork.central.dnp3.domain.ServerConfiguration;
import net.solarnetwork.central.domain.UserLongCompositePK;

/**
 * DTO for DNP3 server configuration.
 * 
 * @author matt
 * @version 1.0
 */
public class ServerConfigurationInput
		extends BaseDnp3ConfigurationInput<ServerConfiguration, UserLongCompositePK> {

	@NotNull
	@NotBlank
	@Size(max = 64)
	private String name;

	@Override
	public ServerConfiguration toEntity(UserLongCompositePK id, Instant date) {
		ServerConfiguration conf = new ServerConfiguration(requireNonNullArgument(id, "id"), date);
		populateConfiguration(conf);
		return conf;
	}

	@Override
	protected void populateConfiguration(ServerConfiguration conf) {
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
