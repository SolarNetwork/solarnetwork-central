/* ==================================================================
 * DaoUserFluxBiz.java - 25/06/2024 10:42:05 am
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

package net.solarnetwork.central.user.flux.biz.impl;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.time.Clock;
import java.time.Instant;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.user.flux.biz.UserFluxBiz;
import net.solarnetwork.central.user.flux.dao.BasicFluxConfigurationFilter;
import net.solarnetwork.central.user.flux.dao.UserFluxAggregatePublishConfigurationDao;
import net.solarnetwork.central.user.flux.dao.UserFluxAggregatePublishConfigurationFilter;
import net.solarnetwork.central.user.flux.dao.UserFluxDefaultAggregatePublishConfigurationDao;
import net.solarnetwork.central.user.flux.domain.UserFluxAggregatePublishConfiguration;
import net.solarnetwork.central.user.flux.domain.UserFluxAggregatePublishConfigurationInput;
import net.solarnetwork.central.user.flux.domain.UserFluxDefaultAggregatePublishConfiguration;
import net.solarnetwork.central.user.flux.domain.UserFluxDefaultAggregatePublishConfigurationInput;
import net.solarnetwork.dao.FilterResults;

/**
 * DAO implementation of {@link UserFluxBiz}.
 * 
 * @author matt
 * @version 1.0
 */
public class DaoUserFluxBiz implements UserFluxBiz {

	private final Clock clock;
	private final UserFluxDefaultAggregatePublishConfigurationDao defaultAggPublishConfDao;
	private final UserFluxAggregatePublishConfigurationDao aggPublishConfDao;

	/**
	 * Constructor.
	 * 
	 * @param defaultAggPublishConfDao
	 *        the default aggregate publish configuration DAO to use
	 * @param aggPublishConfDao
	 *        the aggregate publish configuration DAO to use
	 */
	public DaoUserFluxBiz(UserFluxDefaultAggregatePublishConfigurationDao defaultAggPublishConfDao,
			UserFluxAggregatePublishConfigurationDao aggPublishConfDao) {
		this(Clock.systemUTC(), defaultAggPublishConfDao, aggPublishConfDao);
	}

	/**
	 * Constructor.
	 * 
	 * @param clock
	 *        the clock to use
	 * @param defaultAggPublishConfDao
	 *        the default aggregate publish configuration DAO to use
	 * @param aggPublishConfDao
	 *        the aggregate publish configuration DAO to use
	 */
	public DaoUserFluxBiz(Clock clock,
			UserFluxDefaultAggregatePublishConfigurationDao defaultAggPublishConfDao,
			UserFluxAggregatePublishConfigurationDao aggPublishConfDao) {
		super();
		this.clock = requireNonNullArgument(clock, "clock");
		this.defaultAggPublishConfDao = requireNonNullArgument(defaultAggPublishConfDao,
				"defaultAggPublishConfDao");
		this.aggPublishConfDao = requireNonNullArgument(aggPublishConfDao, "aggPublishConfDao");
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public UserFluxDefaultAggregatePublishConfiguration saveDefaultAggregatePublishConfiguration(
			Long userId, UserFluxDefaultAggregatePublishConfigurationInput input) {
		requireNonNullArgument(userId, "userId");
		requireNonNullArgument(input, "input");
		var conf = input.toEntity(userId, clock.instant());
		defaultAggPublishConfDao.save(conf);
		return conf;
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void deleteDefaultAggregatePublishConfiguration(Long userId) {
		requireNonNullArgument(userId, "userId");
		var filter = new UserFluxDefaultAggregatePublishConfiguration(userId, clock.instant());
		defaultAggPublishConfDao.delete(filter);
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public UserFluxDefaultAggregatePublishConfiguration defaultAggregatePublishConfigurationForUser(
			Long userId) {
		requireNonNullArgument(userId, "userId");
		UserFluxDefaultAggregatePublishConfiguration result = defaultAggPublishConfDao.get(userId);
		return (result != null ? result
				: new UserFluxDefaultAggregatePublishConfiguration(userId, Instant.EPOCH));
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public UserFluxAggregatePublishConfiguration saveAggregatePublishConfiguration(
			UserLongCompositePK id, UserFluxAggregatePublishConfigurationInput input) {
		requireNonNullArgument(requireNonNullArgument(id, "id").getUserId(), "id.userId");
		UserFluxAggregatePublishConfiguration conf = requireNonNullArgument(input, "input").toEntity(id,
				clock.instant());
		UserLongCompositePK pk = aggPublishConfDao.create(id.getUserId(), conf);
		return conf.copyWithId(pk);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void deleteAggregatePublishConfiguration(UserLongCompositePK id) {
		requireNonNullArgument(id, "id");
		var filter = new UserFluxAggregatePublishConfiguration(id, clock.instant());
		aggPublishConfDao.delete(filter);
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public UserFluxAggregatePublishConfiguration aggregatePublishConfigurationForUser(Long userId,
			Long configurationId) {
		return aggPublishConfDao.get(new UserLongCompositePK(requireNonNullArgument(userId, "userId"),
				requireNonNullArgument(configurationId, "configurationId")));
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public FilterResults<UserFluxAggregatePublishConfiguration, UserLongCompositePK> aggregatePublishConfigurationsForUser(
			Long userId, UserFluxAggregatePublishConfigurationFilter filter) {
		requireNonNullArgument(userId, "userId");
		var daoFilter = new BasicFluxConfigurationFilter();
		if ( filter != null ) {
			daoFilter.copyFrom(filter);
		}
		daoFilter.setUserId(userId);
		return aggPublishConfDao.findFiltered(daoFilter);
	}

}
