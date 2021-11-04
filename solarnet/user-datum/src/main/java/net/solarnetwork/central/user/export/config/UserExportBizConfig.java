/* ==================================================================
 * UserExportBizConfig.java - 5/11/2021 9:07:41 AM
 * 
 * Copyright 2021 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.export.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.util.AntPathMatcher;
import net.solarnetwork.central.datum.export.biz.DatumExportDestinationService;
import net.solarnetwork.central.datum.export.biz.DatumExportOutputFormatService;
import net.solarnetwork.central.datum.export.domain.OutputCompressionType;
import net.solarnetwork.central.datum.export.domain.ScheduleType;
import net.solarnetwork.central.datum.v2.dao.DatumStreamMetadataDao;
import net.solarnetwork.central.domain.Aggregation;
import net.solarnetwork.central.user.dao.UserNodeDao;
import net.solarnetwork.central.user.export.biz.UserExportBiz;
import net.solarnetwork.central.user.export.biz.UserExportTaskBiz;
import net.solarnetwork.central.user.export.biz.dao.DaoUserExportBiz;
import net.solarnetwork.central.user.export.biz.dao.DaoUserExportTaskBiz;
import net.solarnetwork.central.user.export.dao.UserAdhocDatumExportTaskInfoDao;
import net.solarnetwork.central.user.export.dao.UserDataConfigurationDao;
import net.solarnetwork.central.user.export.dao.UserDatumExportConfigurationDao;
import net.solarnetwork.central.user.export.dao.UserDatumExportTaskInfoDao;
import net.solarnetwork.central.user.export.dao.UserDestinationConfigurationDao;
import net.solarnetwork.central.user.export.dao.UserOutputConfigurationDao;
import net.solarnetwork.support.PrefixedMessageSource;

/**
 * User export service configuration.
 * 
 * @author matt
 * @version 1.0
 */
@Configuration
public class UserExportBizConfig {

	@Autowired
	private UserDatumExportConfigurationDao datumExportConfigDao;

	@Autowired
	private UserDataConfigurationDao dataConfigDao;

	@Autowired
	private UserDestinationConfigurationDao destinationConfigDao;

	@Autowired
	private UserOutputConfigurationDao outputConfigDao;

	@Autowired
	private UserDatumExportTaskInfoDao taskDao;

	@Autowired
	private UserAdhocDatumExportTaskInfoDao adhocTaskDao;

	@Autowired
	private UserNodeDao userNodeDao;

	@Autowired
	private DatumStreamMetadataDao metaDao;

	@Autowired
	private List<DatumExportOutputFormatService> datumExportOutputFormatServices;

	@Autowired
	private List<DatumExportDestinationService> datumExportDestinationServices;

	@Bean
	public UserExportTaskBiz userExportTaskBiz() {
		DaoUserExportTaskBiz biz = new DaoUserExportTaskBiz(taskDao, adhocTaskDao, userNodeDao, metaDao);

		AntPathMatcher pathMatcher = new AntPathMatcher();
		pathMatcher.setCachePatterns(false);
		pathMatcher.setCaseSensitive(false);
		biz.setPathMatcher(pathMatcher);

		return biz;
	}

	@Bean
	public UserExportBiz userExportBiz() {
		DaoUserExportBiz biz = new DaoUserExportBiz(datumExportConfigDao, dataConfigDao,
				destinationConfigDao, outputConfigDao, taskDao, adhocTaskDao, userExportTaskBiz());

		biz.setOutputFormatServices(datumExportOutputFormatServices);
		biz.setDestinationServices(datumExportDestinationServices);

		ResourceBundleMessageSource compressionMsgSrc = new ResourceBundleMessageSource();
		compressionMsgSrc.setBasename(OutputCompressionType.class.getName());

		ResourceBundleMessageSource scheduleMsgSrc = new ResourceBundleMessageSource();
		scheduleMsgSrc.setBasename(ScheduleType.class.getName());

		ResourceBundleMessageSource aggMsgSrc = new ResourceBundleMessageSource();
		aggMsgSrc.setBasename(Aggregation.class.getName());

		Map<String, MessageSource> msgSources = new HashMap<>(3);
		msgSources.put("compressionType.", compressionMsgSrc);
		msgSources.put("scheduleType.", scheduleMsgSrc);
		msgSources.put("aggregation.", aggMsgSrc);

		PrefixedMessageSource msgSrc = new PrefixedMessageSource();
		msgSrc.setDelegates(msgSources);
		biz.setMessageSource(msgSrc);

		return biz;
	}

}
