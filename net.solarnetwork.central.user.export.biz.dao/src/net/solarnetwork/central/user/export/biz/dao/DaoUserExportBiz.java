/* ==================================================================
 * DaoUserExportBiz.java - 25/03/2018 1:01:05 PM
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

package net.solarnetwork.central.user.export.biz.dao;

import java.util.Collections;
import java.util.List;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import net.solarnetwork.central.datum.biz.DatumExportDestinationService;
import net.solarnetwork.central.datum.biz.DatumExportOutputFormatService;
import net.solarnetwork.central.user.export.biz.UserExportBiz;
import net.solarnetwork.central.user.export.dao.UserDataConfigurationDao;
import net.solarnetwork.central.user.export.dao.UserDatumExportConfigurationDao;
import net.solarnetwork.central.user.export.dao.UserDestinationConfigurationDao;
import net.solarnetwork.central.user.export.dao.UserOutputConfigurationDao;
import net.solarnetwork.central.user.export.domain.UserDataConfiguration;
import net.solarnetwork.central.user.export.domain.UserDatumExportConfiguration;
import net.solarnetwork.central.user.export.domain.UserDestinationConfiguration;
import net.solarnetwork.central.user.export.domain.UserIdentifiableConfiguration;
import net.solarnetwork.central.user.export.domain.UserOutputConfiguration;
import net.solarnetwork.util.OptionalServiceCollection;

/**
 * DAO implementation of {@link UserExportBiz}.
 * 
 * @author matt
 * @version 1.0
 */
public class DaoUserExportBiz implements UserExportBiz {

	private final UserDatumExportConfigurationDao datumExportConfigDao;
	private final UserDataConfigurationDao dataConfigDao;
	private final UserDestinationConfigurationDao destinationConfigDao;
	private final UserOutputConfigurationDao outputConfigDao;

	private OptionalServiceCollection<DatumExportOutputFormatService> outputFormatServices;
	private OptionalServiceCollection<DatumExportDestinationService> destinationServices;

	/**
	 * Constructor.
	 * 
	 * @param datumExportConfigDao
	 *        the datum export configuration DAO to use
	 * @param dataConfigDao
	 *        the data configuration DAO to use
	 * @param destinationConfigDao
	 *        the destination configuration DAO to use
	 * @param outputConfigDao
	 *        the output configuration DAO to use
	 */
	public DaoUserExportBiz(UserDatumExportConfigurationDao datumExportConfigDao,
			UserDataConfigurationDao dataConfigDao, UserDestinationConfigurationDao destinationConfigDao,
			UserOutputConfigurationDao outputConfigDao) {
		super();
		this.datumExportConfigDao = datumExportConfigDao;
		this.dataConfigDao = dataConfigDao;
		this.destinationConfigDao = destinationConfigDao;
		this.outputConfigDao = outputConfigDao;
	}

	@Override
	public Iterable<DatumExportOutputFormatService> availableOutputFormatServices() {
		OptionalServiceCollection<DatumExportOutputFormatService> svcs = this.outputFormatServices;
		return (svcs != null ? svcs.services() : Collections.emptyList());
	}

	@Override
	public Iterable<DatumExportDestinationService> availableDestinationServices() {
		OptionalServiceCollection<DatumExportDestinationService> svcs = this.destinationServices;
		return (svcs != null ? svcs.services() : Collections.emptyList());
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public UserDatumExportConfiguration datumExportConfiguration(Long id) {
		return datumExportConfigDao.get(id);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public Long saveDatumExportConfiguration(UserDatumExportConfiguration configuration) {
		return datumExportConfigDao.store(configuration);
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public List<UserDatumExportConfiguration> datumExportsForUser(Long userId) {
		return datumExportConfigDao.findConfigurationsForUser(userId);
	}

	@SuppressWarnings("unchecked")
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public <T extends UserIdentifiableConfiguration> T configurationForUser(Long userId,
			Class<T> configurationClass, Long id) {
		if ( UserDataConfiguration.class.isAssignableFrom(configurationClass) ) {
			return (T) dataConfigDao.get(id);
		} else if ( UserDestinationConfiguration.class.isAssignableFrom(configurationClass) ) {
			return (T) destinationConfigDao.get(id);
		} else if ( UserOutputConfiguration.class.isAssignableFrom(configurationClass) ) {
			return (T) outputConfigDao.get(id);
		}
		throw new IllegalArgumentException("Unsupported configurationClass: " + configurationClass);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public Long saveConfiguration(UserIdentifiableConfiguration configuration) {
		if ( configuration instanceof UserDataConfiguration ) {
			return dataConfigDao.store((UserDataConfiguration) configuration);
		} else if ( configuration instanceof UserDestinationConfiguration ) {
			return destinationConfigDao.store((UserDestinationConfiguration) configuration);
		} else if ( configuration instanceof UserOutputConfiguration ) {
			return outputConfigDao.store((UserOutputConfiguration) configuration);
		}
		throw new IllegalArgumentException("Unsupported configuration: " + configuration);
	}

	@SuppressWarnings("unchecked")
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public <T extends UserIdentifiableConfiguration> List<T> configurationsForUser(Long userId,
			Class<T> configurationClass) {
		if ( UserDataConfiguration.class.isAssignableFrom(configurationClass) ) {
			return (List<T>) dataConfigDao.findConfigurationsForUser(userId);
		} else if ( UserDestinationConfiguration.class.isAssignableFrom(configurationClass) ) {
			return (List<T>) destinationConfigDao.findConfigurationsForUser(userId);
		} else if ( UserOutputConfiguration.class.isAssignableFrom(configurationClass) ) {
			return (List<T>) outputConfigDao.findConfigurationsForUser(userId);
		}
		throw new IllegalArgumentException("Unsupported configurationClass: " + configurationClass);
	}

	/**
	 * Set the optional output format services.
	 * 
	 * @param outputFormatServices
	 *        the optional services
	 */
	public void setOutputFormatServices(
			OptionalServiceCollection<DatumExportOutputFormatService> outputFormatServices) {
		this.outputFormatServices = outputFormatServices;
	}

	/**
	 * Set the optional destination services.
	 * 
	 * @param destinationServices
	 *        the optional services
	 */
	public void setDestinationServices(
			OptionalServiceCollection<DatumExportDestinationService> destinationServices) {
		this.destinationServices = destinationServices;
	}

}
