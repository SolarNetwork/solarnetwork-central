/* ==================================================================
 * BaseMyBatisUserRelatedGenericDao.java - 17/04/2018 10:32:53 AM
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

package net.solarnetwork.central.user.dao.mybatis;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import net.solarnetwork.central.dao.mybatis.support.BaseMyBatisGenericDao;
import net.solarnetwork.central.user.dao.UserRelatedGenericDao;
import net.solarnetwork.central.user.domain.UserRelatedEntity;

/**
 * Extension of {@link BaseMyBatisGenericDao} that relies on
 * {@link UserRelatedEntity} domain objects so that additional security can be
 * enforced on domain objects belonging to specific users.
 * 
 * <p>
 * Note that this DAO works nearly the same as {@link BaseMyBatisGenericDao},
 * with these notable differences:
 * </p>
 * 
 * <ol>
 * <li>The {@link #get(Serializable)} method will throw an
 * {@link UnsupportedOperationException}. The {@link #get(Serializable, Long)}
 * method will use the same named SQL query defined by {@link #getQueryForId()}
 * but will pass it a map object with {@code id} and {@code userId} properties
 * (instead of just the primary key {@code PK}).</li>
 * <li>The {@link #delete(UserRelatedEntity)} method will use the same named SQL
 * query defined by {@link #getDelete()} but will pass it the actual domain
 * object {@code T} (instead of just the primary key {@code PK}).</li>
 * </ol>
 * 
 * @author matt
 * @version 1.0
 * @since 1.11
 */
@SuppressWarnings("deprecation")
public abstract class BaseMyBatisUserRelatedGenericDao<T extends UserRelatedEntity<PK>, PK extends Serializable>
		extends BaseMyBatisGenericDao<T, PK> implements UserRelatedGenericDao<T, PK> {

	/**
	 * Constructor.
	 * 
	 * @param domainClass
	 *        the domain class
	 * @param pkClass
	 *        the primary key class
	 */
	public BaseMyBatisUserRelatedGenericDao(Class<? extends T> domainClass,
			Class<? extends PK> pkClass) {
		super(domainClass, pkClass);
	}

	@Override
	public T get(PK id, Long userId) {
		Map<String, Object> params = new HashMap<String, Object>(2);
		params.put("id", id);
		params.put("userId", userId);
		return getSqlSession().selectOne(getQueryForId(), params);
	}

	@Override
	public T get(PK id) {
		throw new UnsupportedOperationException("The get(PK,Long) method must be used");
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void delete(T domainObject) {
		if ( domainObject.getId() == null || domainObject.getUserId() == null ) {
			return;
		}
		int result = getSqlSession().delete(getDelete(), domainObject);
		if ( result < 1 ) {
			log.warn("Delete [" + domainObject + "] did not affect any rows");
		} else if ( log.isInfoEnabled() ) {
			log.debug("Deleted [" + domainObject + ']');
		}
	}

}
