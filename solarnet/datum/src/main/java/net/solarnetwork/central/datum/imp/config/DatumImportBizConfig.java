/* ==================================================================
 * DatumImportBizConfig.java - 5/11/2021 1:59:08 PM
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

package net.solarnetwork.central.datum.imp.config;

import static net.solarnetwork.central.datum.imp.config.SolarNetDatumImportConfiguration.DATUM_IMPORT;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.scheduling.TaskScheduler;
import net.solarnetwork.central.dao.SecurityTokenDao;
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;
import net.solarnetwork.central.datum.imp.biz.DatumImportInputFormatService;
import net.solarnetwork.central.datum.imp.biz.dao.DaoDatumImportBiz;
import net.solarnetwork.central.datum.imp.dao.DatumImportJobInfoDao;
import net.solarnetwork.central.datum.v2.dao.DatumEntityDao;
import net.solarnetwork.event.AppEventPublisher;
import net.solarnetwork.service.ResourceStorageService;

/**
 * Configuration for datum import services.
 * 
 * @author matt
 * @version 1.0
 */
@Configuration
public class DatumImportBizConfig {

	@Autowired
	private TaskScheduler taskScheduler;

	@Autowired
	private SolarNodeOwnershipDao userNodeDao;

	@Autowired
	private DatumImportJobInfoDao jobInfoDao;

	@Autowired
	private DatumEntityDao datumDao;

	@Autowired(required = false)
	@Qualifier(DATUM_IMPORT)
	private ResourceStorageService resourceStorageService;

	@Autowired
	private AppEventPublisher eventPublisher;

	@Autowired
	private List<DatumImportInputFormatService> datumImportInputFormatServices;

	@Autowired
	private SecurityTokenDao securityTokenDao;

	public static class DatumImportSettings {

		private int concurrentTasks = 2;
		private int concurrentPreviewTasks = 4;
		private int previewMaxCount = 100;
		private int progressLogCount = 25_000;
		private long completedTaskMinimumCacheTime = 14_400_000L;
		private String workPath;
		private long resourceStorageWaitMs = 60_000L;
		private String resourceStorageUid = "Datum-Import";

		public int getConcurrentTasks() {
			return concurrentTasks;
		}

		public void setConcurrentTasks(int concurrentTasks) {
			this.concurrentTasks = concurrentTasks;
		}

		public int getConcurrentPreviewTasks() {
			return concurrentPreviewTasks;
		}

		public void setConcurrentPreviewTasks(int concurrentPreviewTasks) {
			this.concurrentPreviewTasks = concurrentPreviewTasks;
		}

		public int getPreviewMaxCount() {
			return previewMaxCount;
		}

		public void setPreviewMaxCount(int previewMaxCount) {
			this.previewMaxCount = previewMaxCount;
		}

		public int getProgressLogCount() {
			return progressLogCount;
		}

		public void setProgressLogCount(int progressLogCount) {
			this.progressLogCount = progressLogCount;
		}

		public long getCompletedTaskMinimumCacheTime() {
			return completedTaskMinimumCacheTime;
		}

		public void setCompletedTaskMinimumCacheTime(long completedTaskMinimumCacheTime) {
			this.completedTaskMinimumCacheTime = completedTaskMinimumCacheTime;
		}

		public String getWorkPath() {
			return workPath;
		}

		public void setWorkPath(String workPath) {
			this.workPath = workPath;
		}

		public long getResourceStorageWaitMs() {
			return resourceStorageWaitMs;
		}

		public void setResourceStorageWaitMs(long resourceStorageWaitMs) {
			this.resourceStorageWaitMs = resourceStorageWaitMs;
		}

		public String getResourceStorageUid() {
			return resourceStorageUid;
		}

		public void setResourceStorageUid(String resourceStorageUid) {
			this.resourceStorageUid = resourceStorageUid;
		}

	}

	@ConfigurationProperties(prefix = "app.datum.import")
	@Bean
	public DatumImportSettings datumImportSettings() {
		return new DatumImportSettings();
	}

	@Bean(initMethod = "serviceDidStartup", destroyMethod = "serviceDidShutdown")
	public DaoDatumImportBiz datumImportBiz() {
		DatumImportSettings settings = datumImportSettings();

		SimpleAsyncTaskExecutor taskExecutor = new SimpleAsyncTaskExecutor("Datum-Import-");
		taskExecutor.setConcurrencyLimit(settings.concurrentTasks);

		DaoDatumImportBiz biz = new DaoDatumImportBiz(taskScheduler, taskExecutor, userNodeDao,
				securityTokenDao, jobInfoDao, datumDao);
		biz.setMaxPreviewCount(settings.previewMaxCount);
		biz.setProgressLogCount(settings.progressLogCount);
		biz.setCompletedTaskMinimumCacheTime(settings.completedTaskMinimumCacheTime);
		biz.setWorkPath(settings.workPath);
		biz.setResourceStorageWaitMs(settings.resourceStorageWaitMs);
		biz.setResourceStorageService(resourceStorageService);
		biz.setInputServices(datumImportInputFormatServices);

		SimpleAsyncTaskExecutor previewTaskExecutor = new SimpleAsyncTaskExecutor(
				"Datum-Import-Preview-");
		previewTaskExecutor.setConcurrencyLimit(settings.concurrentPreviewTasks);
		biz.setPreviewExecutor(previewTaskExecutor);

		biz.setEventPublisher(eventPublisher);

		return biz;
	}

}
