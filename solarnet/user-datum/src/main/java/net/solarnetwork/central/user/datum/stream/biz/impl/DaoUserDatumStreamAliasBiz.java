/* ==================================================================
 * DaoUserDatumStreamAliasBiz.java - 30/03/2026 10:05:30 am
 * 
 * Copyright 2026 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.datum.stream.biz.impl;

import static net.solarnetwork.central.security.AuthorizationException.requireNonNullObject;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.central.datum.v2.dao.BasicDatumCriteria;
import net.solarnetwork.central.datum.v2.dao.ObjectDatumStreamAliasEntityDao;
import net.solarnetwork.central.datum.v2.dao.ObjectDatumStreamAliasFilter;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamAliasEntity;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamAliasMatchType;
import net.solarnetwork.central.user.datum.stream.biz.UserDatumStreamAliasBiz;
import net.solarnetwork.central.user.datum.stream.domain.ObjectDatumStreamAliasEntityInput;
import net.solarnetwork.dao.FilterResults;

/**
 * DAO-based implementation of {@link UserDatumStreamAliasBiz}.
 * 
 * @author matt
 * @version 1.0
 */
public class DaoUserDatumStreamAliasBiz implements UserDatumStreamAliasBiz {

	private final ObjectDatumStreamAliasEntityDao aliasDao;

	/**
	 * Constructor.
	 * 
	 * @param aliasDao
	 *        the alias DAO
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public DaoUserDatumStreamAliasBiz(ObjectDatumStreamAliasEntityDao aliasDao) {
		super();
		this.aliasDao = requireNonNullArgument(aliasDao, "aliasDao");
	}

	@Override
	public ObjectDatumStreamAliasEntity aliasForUser(Long userId, UUID id) {
		final var f = new BasicDatumCriteria();
		f.setStreamAliasMatchType(ObjectDatumStreamAliasMatchType.AliasOnly);
		f.setUserId(userId);
		f.setStreamId(id);
		return requireNonNullObject(aliasDao.findFiltered(f).firstResult(), id);
	}

	@Override
	public FilterResults<ObjectDatumStreamAliasEntity, UUID> listAliases(Long userId,
			@Nullable ObjectDatumStreamAliasFilter filter) {
		final var f = new BasicDatumCriteria();
		f.copyFrom(filter);
		f.setUserId(userId);
		return aliasDao.findFiltered(f);
	}

	@Override
	public ObjectDatumStreamAliasEntity saveAlias(Long userId, UUID aliasId,
			ObjectDatumStreamAliasEntityInput input) {
		final ObjectDatumStreamAliasEntity alias = input.toEntity(aliasId, Instant.now());
		final var pk = aliasDao.save(alias);
		return requireNonNullObject(aliasDao.get(pk), pk);
	}

}
