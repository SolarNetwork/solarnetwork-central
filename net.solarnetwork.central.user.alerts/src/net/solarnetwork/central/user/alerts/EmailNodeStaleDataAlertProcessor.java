/* ==================================================================
 * EmailNodeStaleDataAlertProcessor.java - 15/05/2015 7:23:12 pm
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

package net.solarnetwork.central.user.alerts;

import java.util.List;
import net.solarnetwork.central.RepeatableTaskException;
import net.solarnetwork.central.datum.dao.GeneralNodeDatumDao;
import net.solarnetwork.central.mail.MailService;
import net.solarnetwork.central.mail.MessageTemplateDataSource;
import net.solarnetwork.central.mail.support.BasicMailAddress;
import net.solarnetwork.central.user.dao.UserAlertDao;
import net.solarnetwork.central.user.dao.UserDao;
import net.solarnetwork.central.user.domain.User;
import net.solarnetwork.central.user.domain.UserAlert;
import net.solarnetwork.central.user.domain.UserAlertType;

/**
 * Process stale data alerts for nodes.
 * 
 * @author matt
 * @version 1.0
 */
public class EmailNodeStaleDataAlertProcessor implements UserAlertBatchProcessor {

	public static final Integer DEFAULT_BATCH_SIZE = 50;

	private final UserDao userDao;
	private final UserAlertDao userAlertDao;
	private final GeneralNodeDatumDao generalNodeDatumDao;
	private final MailService mailService;
	private Integer batchSize = DEFAULT_BATCH_SIZE;

	/**
	 * Construct with properties.
	 * 
	 * @param userDao
	 *        The {@link UserDao} to use.
	 * @param userAlertDao
	 *        The {@link UserAlertDao} to use.
	 * @param generalNodeDatumDao
	 *        The {@link GeneralNodeDatumDao} to use.
	 * @param mailService
	 *        The {@link MailService} to use.
	 */
	public EmailNodeStaleDataAlertProcessor(UserDao userDao, UserAlertDao userAlertDao,
			GeneralNodeDatumDao generalNodeDatumDao, MailService mailService) {
		super();
		this.userDao = userDao;
		this.userAlertDao = userAlertDao;
		this.generalNodeDatumDao = generalNodeDatumDao;
		this.mailService = mailService;
	}

	@Override
	public Long processAlerts(Long lastProcessedAlertId) {
		List<UserAlert> alerts = userAlertDao.findAlertsToProcess(UserAlertType.NodeStaleData,
				lastProcessedAlertId, batchSize);
		Long lastAlertId = null;
		try {
			for ( UserAlert alert : alerts ) {
				boolean sendEmail = false;
				// TODO: process alert, send email, etc
				if ( sendEmail ) {
					User user = userDao.get(alert.getUserId());
					if ( user != null ) {
						// TODO: construct mail data source
						BasicMailAddress addr = new BasicMailAddress(user.getName(), user.getEmail());
						MessageTemplateDataSource msg = null; // TODO
						mailService.sendMail(addr, msg);
					}
				}
				lastAlertId = alert.getId();
			}
		} catch ( RuntimeException e ) {
			throw new RepeatableTaskException("Error processing user alerts", e, lastAlertId);
		}
		return lastAlertId;
	}

	public Integer getBatchSize() {
		return batchSize;
	}

	public void setBatchSize(Integer batchSize) {
		this.batchSize = batchSize;
	}

}
