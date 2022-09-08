/* ==================================================================
 * DaoUserExpireBiz.java - 10/07/2018 11:30:28 AM
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

package net.solarnetwork.central.user.expire.biz.dao;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import net.solarnetwork.central.datum.domain.DatumRecordCounts;
import net.solarnetwork.central.user.domain.UserIdentifiableConfiguration;
import net.solarnetwork.central.user.expire.biz.UserExpireBiz;
import net.solarnetwork.central.user.expire.dao.ExpireUserDataConfigurationDao;
import net.solarnetwork.central.user.expire.domain.ExpireUserDataConfiguration;
import net.solarnetwork.domain.BasicLocalizedServiceInfo;
import net.solarnetwork.domain.LocalizedServiceInfo;
import net.solarnetwork.domain.datum.Aggregation;

/**
 * DAO implementation of {@link UserExpireBiz}.
 * 
 * @author matt
 * @version 1.1
 */
public class DaoUserExpireBiz implements UserExpireBiz {

	private final ExpireUserDataConfigurationDao dataConfigDao;

	private MessageSource messageSource;

	/**
	 * Constructor.
	 * 
	 * @param dataConfigDao
	 *        the data configuration DAO to use
	 */
	public DaoUserExpireBiz(ExpireUserDataConfigurationDao dataConfigDao) {
		super();
		this.dataConfigDao = dataConfigDao;
	}

	@Override
	public Iterable<LocalizedServiceInfo> availableAggregationTypes(Locale locale) {
		List<LocalizedServiceInfo> results = new ArrayList<>(4);
		List<Aggregation> aggs = Arrays.asList(Aggregation.None, Aggregation.Hour, Aggregation.Day,
				Aggregation.Month);
		for ( Aggregation type : aggs ) {
			String name = type.toString();
			String desc = null;
			if ( messageSource != null ) {
				name = messageSource.getMessage("aggregation." + type.name() + ".key", null, name,
						locale);
				desc = messageSource.getMessage("aggregation." + type.name() + ".desc", null, desc,
						locale);
			}
			results.add(new BasicLocalizedServiceInfo(String.valueOf(type.getKey()), locale, name, desc,
					null));
		}
		return results;
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public DatumRecordCounts countExpiredDataForConfiguration(ExpireUserDataConfiguration config) {
		return dataConfigDao.countExpiredDataForConfiguration(config);
	}

	@SuppressWarnings("unchecked")
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public <T extends UserIdentifiableConfiguration> T configurationForUser(Long userId,
			Class<T> configurationClass, Long id) {
		if ( ExpireUserDataConfiguration.class.isAssignableFrom(configurationClass) ) {
			return (T) dataConfigDao.get(id, userId);
		}
		throw new IllegalArgumentException("Unsupported configurationClass: " + configurationClass);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void deleteConfiguration(UserIdentifiableConfiguration configuration) {
		if ( configuration instanceof ExpireUserDataConfiguration ) {
			dataConfigDao.delete((ExpireUserDataConfiguration) configuration);
		} else {
			throw new IllegalArgumentException("Unsupported configuration type: " + configuration);
		}
	}

	@SuppressWarnings("unchecked")
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public <T extends UserIdentifiableConfiguration> List<T> configurationsForUser(Long userId,
			Class<T> configurationClass) {
		if ( ExpireUserDataConfiguration.class.isAssignableFrom(configurationClass) ) {
			return (List<T>) dataConfigDao.findConfigurationsForUser(userId);
		}
		throw new IllegalArgumentException("Unsupported configurationClass: " + configurationClass);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public Long saveConfiguration(UserIdentifiableConfiguration configuration) {
		if ( configuration instanceof ExpireUserDataConfiguration ) {
			return dataConfigDao.store((ExpireUserDataConfiguration) configuration);
		}
		throw new IllegalArgumentException("Unsupported configuration: " + configuration);
	}

	/**
	 * A message source for resolving messages with.
	 * 
	 * @param messageSource
	 *        the message source
	 */
	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

}
