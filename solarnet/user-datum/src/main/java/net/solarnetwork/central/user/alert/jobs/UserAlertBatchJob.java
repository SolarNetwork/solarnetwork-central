/* ==================================================================
 * UserAlertBatchJob.java - 15/05/2015 2:24:52 pm
 * 
 * Copyright 2007-2015 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.alert.jobs;

import static net.solarnetwork.central.domain.AppSetting.appSetting;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.time.Instant;
import java.util.Collection;
import java.util.function.Consumer;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import net.solarnetwork.central.dao.AppSettingDao;
import net.solarnetwork.central.domain.AppSetting;
import net.solarnetwork.central.scheduler.JobSupport;
import net.solarnetwork.central.user.domain.UserAlertSituation;
import net.solarnetwork.central.user.domain.UserAlertType;
import net.solarnetwork.service.ServiceLifecycleObserver;

/**
 * Job to look for {@link UserAlertType#NodeStaleData} needing of creating /
 * updating a {@link UserAlertSituation} for.
 * 
 * @author matt
 * @version 2.0
 */
public class UserAlertBatchJob extends JobSupport implements ServiceLifecycleObserver {

	/** The setting key used for application settings. */
	public static final String SETTING_KEY = "UserAlertBatchJob";

	/**
	 * The job property for a row to lock.
	 */
	public static final String JOB_PROP_LOCK = "Lock";

	/**
	 * The job property for the starting alert ID to use. If not specified,
	 * start with the smallest alert ID available.
	 */
	public static final String JOB_PROP_STARTING_ID = "AlertIdStart";

	/**
	 * The job property for the valid date to use, as milliseconds since the
	 * epoch. If not specified, use the current date.
	 */
	public static final String JOB_PROP_VALID_DATE = "AlertValidDate";

	private final UserAlertBatchProcessor processor;
	private final TransactionTemplate txTemplate;
	private final AppSettingDao appSettingDao;

	/**
	 * Construct with properties.
	 * 
	 * @param processor
	 *        the batch processor to use
	 * @param txTemplate
	 *        the transaction template to use
	 * @param appSettingDao
	 *        the setting DAO to use
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public UserAlertBatchJob(UserAlertBatchProcessor processor, TransactionTemplate txTemplate,
			AppSettingDao appSettingDao) {
		super();
		this.processor = requireNonNullArgument(processor, "processor");
		this.txTemplate = requireNonNullArgument(txTemplate, "txTemplate");
		this.appSettingDao = requireNonNullArgument(appSettingDao, "appSettingDao");
		setGroupId("UserAlert");
		setMaximumWaitMs(1800000L);
	}

	@Override
	public void serviceDidStartup() {
		// make sure lock row exists
		appSettingDao.save(appSetting(SETTING_KEY, JOB_PROP_LOCK, JOB_PROP_LOCK));
	}

	@Override
	public void serviceDidShutdown() {
		// nothing
	}

	@Override
	public void run() {
		txTemplate.executeWithoutResult(new Consumer<TransactionStatus>() {

			@Override
			public void accept(TransactionStatus t) {
				Collection<AppSetting> settings = appSettingDao.lockForUpdate(SETTING_KEY);
				Long startingId = null;
				Long validDateMs = null;
				if ( settings != null ) {
					for ( AppSetting setting : settings ) {
						if ( JOB_PROP_STARTING_ID.equals(setting.getType()) ) {
							try {
								startingId = Long.valueOf(setting.getValue());
							} catch ( NumberFormatException e ) {
								// ignore this and continue
							}
						} else if ( JOB_PROP_VALID_DATE.equals(setting.getType()) ) {
							try {
								validDateMs = Long.valueOf(setting.getValue());
							} catch ( NumberFormatException e ) {
								// ignore this and continue
							}
						}
					}
				}

				final Instant validDate = (validDateMs == null ? Instant.now()
						: Instant.ofEpochMilli(validDateMs));
				startingId = processor.processAlerts(startingId, validDate);
				if ( startingId != null ) {
					Instant now = Instant.now();
					appSettingDao.save(new AppSetting(SETTING_KEY, JOB_PROP_STARTING_ID, null, now,
							startingId.toString()));
					appSettingDao.save(new AppSetting(SETTING_KEY, JOB_PROP_VALID_DATE, null, now,
							String.valueOf(validDate.toEpochMilli())));
				} else if ( validDateMs != null ) {

				}
			}
		});
	}

	/**
	 * Get the processor.
	 * 
	 * @return the processor
	 */
	public UserAlertBatchProcessor getUserAlertDao() {
		return processor;
	}

}
