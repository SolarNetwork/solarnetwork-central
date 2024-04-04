/* ==================================================================
 * InstructionInputConfigurationEntity.java - 28/03/2024 11:11:20 am
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

package net.solarnetwork.central.inin.domain;

import java.io.Serializable;
import net.solarnetwork.central.dao.UserRelatedEntity;
import net.solarnetwork.central.domain.CompositeKey;
import net.solarnetwork.central.domain.UserIdRelated;
import net.solarnetwork.domain.CopyingIdentity;
import net.solarnetwork.domain.Differentiable;

/**
 * API for instruction input configuration entities.
 *
 * @author matt
 * @version 1.0
 */
public interface InstructionInputConfigurationEntity<C extends InstructionInputConfigurationEntity<C, K>, K extends CompositeKey & Comparable<K> & Serializable & UserIdRelated>
		extends UserRelatedEntity<K>, CopyingIdentity<K, C>, Differentiable<C>, Serializable, Cloneable {

	/**
	 * Erase any sensitive credentials.
	 */
	default void eraseCredentials() {
		// extending classes can implement as needed
	}

}
