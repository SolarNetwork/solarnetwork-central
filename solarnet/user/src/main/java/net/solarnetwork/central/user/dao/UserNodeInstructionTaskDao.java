/* ==================================================================
 * UserNodeInstructionTaskDao.java - 10/11/2025 3:44:57 pm
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

package net.solarnetwork.central.user.dao;

import static net.solarnetwork.central.user.domain.UserNodeInstructionTaskEntity.EXPRESSION_SECRETS_PROP;
import static net.solarnetwork.central.user.domain.UserNodeInstructionTaskEntity.EXPRESSION_SETTINGS_PROP;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.BiFunction;
import net.solarnetwork.central.common.dao.ClaimableTaskDao;
import net.solarnetwork.central.common.dao.FilterableDeleteDao;
import net.solarnetwork.central.common.dao.GenericCompositeKey2Dao;
import net.solarnetwork.central.common.dao.UserServiceConfigurationDao;
import net.solarnetwork.central.dao.UserModifiableEnabledStatusDao;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.security.SecurityUtils;
import net.solarnetwork.central.user.domain.UserNodeInstructionTaskEntity;
import net.solarnetwork.dao.FilterableDao;

/**
 * DAO API for {@link UserNodeInstructionTaskEntity} entities.
 *
 * @author matt
 * @version 1.0
 */
public interface UserNodeInstructionTaskDao
		extends GenericCompositeKey2Dao<UserNodeInstructionTaskEntity, UserLongCompositePK, Long, Long>,
		FilterableDao<UserNodeInstructionTaskEntity, UserLongCompositePK, UserNodeInstructionTaskFilter>,
		FilterableDeleteDao<UserNodeInstructionTaskFilter>,
		UserModifiableEnabledStatusDao<UserNodeInstructionTaskFilter>,
		ClaimableTaskDao<UserNodeInstructionTaskEntity, UserLongCompositePK>,
		UserServiceConfigurationDao<UserLongCompositePK> {

	@Override
	default Map<String, Object> serviceConfiguration(UserLongCompositePK id,
			BiFunction<UserLongCompositePK, String, String> secretResolver) {
		final UserNodeInstructionTaskEntity task = get(id);
		if ( task == null ) {
			return null;
		}
		final Map<String, ?> props = task.getServiceProps();
		final Map<String, Object> result = new LinkedHashMap<>(4);
		if ( props != null && props.get(EXPRESSION_SETTINGS_PROP) instanceof Map<?, ?> s ) {
			for ( Entry<?, ?> e : s.entrySet() ) {
				if ( e.getKey() == null || e.getValue() == null ) {
					continue;
				}
				result.put(e.getKey().toString(), e.getValue());
			}
		}
		if ( props != null && props.get(EXPRESSION_SECRETS_PROP) instanceof Map<?, ?> s ) {
			Set<String> secureKeys = new HashSet<>(s.size());
			for ( Entry<?, ?> e : s.entrySet() ) {
				if ( e.getKey() == null || e.getValue() == null ) {
					continue;
				}
				String key = e.getKey().toString();
				secureKeys.add(key);
				result.put(key, e.getValue());
			}
			return SecurityUtils.decryptedMap(result, secureKeys,
					(key) -> secretResolver.apply(id, key));
		}
		return result;
	}

}
