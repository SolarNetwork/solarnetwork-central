/* ==================================================================
 * BaseCloudIntegrationsConfigurationInput.java - 4/10/2024 1:18:25 pm
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

package net.solarnetwork.central.dao;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import net.solarnetwork.central.domain.UserRelatedCompositeKey;

/**
 * Base DTO for user related entities.
 *
 * @param <C>
 *        the configuration type
 * @param <K>
 *        the key type
 * @author matt
 * @version 1.0
 */
public abstract class BaseUserRelatedStdInput<C extends BaseUserModifiableEntity<C, K> & UserRelatedStdEntity<C, K>, K extends UserRelatedCompositeKey<K>>
		implements UserRelatedStdInput<C, K> {

	/**
	 * Constructor.
	 */
	public BaseUserRelatedStdInput() {
		super();
	}

	private boolean enabled;

	/**
	 * Populate input properties onto a configuration instance.
	 *
	 * @param conf
	 *        the configuration to populate
	 */
	protected void populateConfiguration(C conf) {
		requireNonNullArgument(conf, "conf");
		conf.setModified(conf.getCreated());
		conf.setEnabled(enabled);
	}

	/**
	 * Get the enabled flag.
	 *
	 * @return the enabled
	 */
	public boolean isEnabled() {
		return enabled;
	}

	/**
	 * Set the enabled flag.
	 *
	 * @param enabled
	 *        the enabled to set
	 */
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

}