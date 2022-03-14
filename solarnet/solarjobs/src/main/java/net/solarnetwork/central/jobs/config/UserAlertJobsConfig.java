/* ==================================================================
 * UserAlertJobsConfig.java - 10/11/2021 4:15:07 PM
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

package net.solarnetwork.central.jobs.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.transaction.support.TransactionTemplate;
import net.solarnetwork.central.dao.AppSettingDao;
import net.solarnetwork.central.dao.SolarNodeDao;
import net.solarnetwork.central.datum.v2.dao.DatumEntityDao;
import net.solarnetwork.central.mail.MailService;
import net.solarnetwork.central.mail.support.DefaultMailService;
import net.solarnetwork.central.scheduler.ManagedJob;
import net.solarnetwork.central.user.alert.jobs.EmailNodeStaleDataAlertProcessor;
import net.solarnetwork.central.user.alert.jobs.UserAlertBatchJob;
import net.solarnetwork.central.user.alert.jobs.UserAlertBatchProcessor;
import net.solarnetwork.central.user.alert.jobs.UserAlertSituationCleanerJob;
import net.solarnetwork.central.user.dao.UserAlertDao;
import net.solarnetwork.central.user.dao.UserAlertSituationDao;
import net.solarnetwork.central.user.dao.UserDao;
import net.solarnetwork.central.user.dao.UserNodeDao;

/**
 * User alert jobs configuration.
 * 
 * @author matt
 * @version 1.0
 */
@Configuration
public class UserAlertJobsConfig {

	@Autowired
	private AsyncTaskExecutor taskExecutor;

	@Autowired
	private TransactionTemplate txTemplate;

	@Autowired
	private AppSettingDao appSettingDao;

	@Autowired
	private SolarNodeDao solarNodeDao;

	@Autowired
	private UserDao userDao;

	@Autowired
	private UserNodeDao userNodeDao;

	@Autowired
	private UserAlertDao userAlertDao;

	@Autowired
	private UserAlertSituationDao userAlertSituationDao;

	@Autowired
	private DatumEntityDao datumDao;

	@Autowired
	private MailSender mailSender;

	@ConfigurationProperties(prefix = "app.user-alert.stale-data.mail")
	@Bean
	public SimpleMailMessage emailNodeStaleDataAlertMailTemplate() {
		SimpleMailMessage msg = new SimpleMailMessage();
		msg.setFrom("alerts@solarnetwork.net");
		return msg;
	}

	@ConfigurationProperties(prefix = "app.user-alert.stale-data.mail")
	@Bean
	public MailService emailNodeStaleDataAlertMailService() {
		DefaultMailService service = new DefaultMailService(mailSender);
		service.setTemplateMessage(emailNodeStaleDataAlertMailTemplate());
		return service;
	}

	@ConfigurationProperties(prefix = "app.user-alert.stale-data.processor")
	@Bean
	public UserAlertBatchProcessor emailNodeStaleDataAlertProcessor() {
		ResourceBundleMessageSource msgSource = new ResourceBundleMessageSource();
		msgSource.setBasenames(EmailNodeStaleDataAlertProcessor.class.getName());

		return new EmailNodeStaleDataAlertProcessor(solarNodeDao, userDao, userNodeDao, userAlertDao,
				userAlertSituationDao, datumDao, emailNodeStaleDataAlertMailService(), msgSource);
	}

	@ConfigurationProperties(prefix = "app.job.user-alert.stale-data.emailer")
	@Bean(initMethod = "serviceDidStartup", destroyMethod = "serviceDidShutdown")
	public ManagedJob userAlertBatchJob() {
		UserAlertBatchJob job = new UserAlertBatchJob(emailNodeStaleDataAlertProcessor(), txTemplate,
				appSettingDao);
		job.setId("EmailNodeStaleDataAlertProcessor");
		job.setParallelTaskExecutor(taskExecutor);
		return job;
	}

	@ConfigurationProperties(prefix = "app.job.user-alert.stale-data.cleaner")
	@Bean
	public ManagedJob resolvedSituationCleanerJob() {
		UserAlertSituationCleanerJob job = new UserAlertSituationCleanerJob(userAlertSituationDao);
		job.setId("UserAlertSituationCleaner");
		job.setParallelTaskExecutor(taskExecutor);
		return job;
	}

}
