/* ==================================================================
 * DatumExportBizConfig.java - 9/11/2021 9:26:38 AM
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

package net.solarnetwork.central.datum.export.config;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.transaction.support.TransactionTemplate;
import net.solarnetwork.central.datum.biz.QueryAuditor;
import net.solarnetwork.central.datum.export.biz.DatumExportDestinationService;
import net.solarnetwork.central.datum.export.biz.DatumExportOutputFormatService;
import net.solarnetwork.central.datum.export.biz.dao.DaoDatumExportBiz;
import net.solarnetwork.central.datum.export.dao.DatumExportTaskInfoDao;
import net.solarnetwork.central.datum.v2.dao.DatumEntityDao;
import net.solarnetwork.event.AppEventPublisher;

/**
 * Datum export service configuration.
 * 
 * @author matt
 * @version 1.0
 */
@Configuration
public class DatumExportBizConfig {

	@Value("${app.datum.export.completed-task-minimum-cache-time:14400000}")
	private int completedTaskMinimumCacheTime = 14400000;

	@Autowired
	private TransactionTemplate transactionTemplate;

	@Autowired
	private TaskScheduler taskScheduler;

	@Autowired
	private AsyncTaskExecutor taskExecutor;

	@Autowired
	private AppEventPublisher eventPublisher;

	@Autowired
	private DatumExportTaskInfoDao datumExportTaskInfoDao;

	@Autowired
	private DatumEntityDao datumEntityDao;

	@Autowired
	private QueryAuditor queryAuditor;

	@Autowired
	private List<DatumExportDestinationService> datumExportDestinationServices;

	@Autowired
	private List<DatumExportOutputFormatService> datumExportOutputFormatServices;

	@Bean(initMethod = "init", destroyMethod = "shutdown")
	public DaoDatumExportBiz datumExportBiz() {
		DaoDatumExportBiz biz = new DaoDatumExportBiz(datumExportTaskInfoDao, datumEntityDao,
				taskScheduler, taskExecutor, transactionTemplate);
		biz.setQueryAuditor(queryAuditor);
		biz.setCompletedTaskMinimumCacheTime(completedTaskMinimumCacheTime);
		biz.setDestinationServices(datumExportDestinationServices);
		biz.setOutputFormatServices(datumExportOutputFormatServices);
		biz.setEventPublisher(eventPublisher);
		return biz;
	}

}
