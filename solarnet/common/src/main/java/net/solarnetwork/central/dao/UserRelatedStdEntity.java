/* ==================================================================
 * UserRelatedStdEntity.java - 28/09/2024 5:12:42 pm
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

import java.io.Serializable;
import net.solarnetwork.central.domain.UserRelatedCompositeKey;
import net.solarnetwork.domain.CopyingIdentity;
import net.solarnetwork.domain.Differentiable;

/**
 * Extension of {@link UserRelatedEntity} that supports {@link CopyingIdentity}
 * and {@link Differentiable}.
 *
 * @param <C>
 *        the entity type
 * @param <K>
 *        the key type
 * @author matt
 * @version 2.0
 */
public interface UserRelatedStdEntity<C extends UserRelatedStdEntity<C, K>, K extends UserRelatedCompositeKey<K>>
		extends UserRelatedEntity<K>, CopyingIdentity<C, K>, Differentiable<C>, Serializable, Cloneable {

}
