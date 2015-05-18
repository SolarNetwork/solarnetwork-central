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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.solarnetwork.central.RepeatableTaskException;
import net.solarnetwork.central.dao.SolarNodeDao;
import net.solarnetwork.central.datum.dao.GeneralNodeDatumDao;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumFilterMatch;
import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.central.domain.SolarNode;
import net.solarnetwork.central.mail.MailService;
import net.solarnetwork.central.mail.support.BasicMailAddress;
import net.solarnetwork.central.mail.support.ClasspathResourceMessageTemplateDataSource;
import net.solarnetwork.central.user.dao.UserAlertDao;
import net.solarnetwork.central.user.dao.UserAlertSituationDao;
import net.solarnetwork.central.user.dao.UserDao;
import net.solarnetwork.central.user.domain.User;
import net.solarnetwork.central.user.domain.UserAlert;
import net.solarnetwork.central.user.domain.UserAlertOptions;
import net.solarnetwork.central.user.domain.UserAlertSituation;
import net.solarnetwork.central.user.domain.UserAlertSituationStatus;
import net.solarnetwork.central.user.domain.UserAlertType;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;

/**
 * Process stale data alerts for nodes.
 * 
 * @author matt
 * @version 1.0
 */
public class EmailNodeStaleDataAlertProcessor implements UserAlertBatchProcessor {

	/** The minimum number of ms before an alert reminder will be processed. */
	private static final int MIN_ALERT_VALID_DELAY = 60000;

	/** The default value for {@link #getBatchSize()}. */
	public static final Integer DEFAULT_BATCH_SIZE = 50;

	/** The default value for {@link #get}. */
	public static final String DEFAULT_MAIL_TEMPLATE_RESOURCE = "/net/solarnetwork/central/user/alerts/user-alert-NodeStaleData.txt";

	private final SolarNodeDao solarNodeDao;
	private final UserDao userDao;
	private final UserAlertDao userAlertDao;
	private final UserAlertSituationDao userAlertSituationDao;
	private final GeneralNodeDatumDao generalNodeDatumDao;
	private final MailService mailService;
	private Integer batchSize = DEFAULT_BATCH_SIZE;
	private final MessageSource messageSource;
	private String mailTemplateResource = DEFAULT_MAIL_TEMPLATE_RESOURCE;
	private DateTimeFormatter timestampFormat = DateTimeFormat.forPattern("d MMM yyyy HH:mm z");

	private final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * Construct with properties.
	 * 
	 * @param solarNodeDao
	 *        The {@link SolarNodeDao} to use.
	 * @param userDao
	 *        The {@link UserDao} to use.
	 * @param userAlertDao
	 *        The {@link UserAlertDao} to use.
	 * @param userAlertSituationDao
	 *        The {@link UserAlertSituationDao} to use.
	 * @param generalNodeDatumDao
	 *        The {@link GeneralNodeDatumDao} to use.
	 * @param mailService
	 *        The {@link MailService} to use.
	 * @param messageSource
	 *        The {@link MessageSource} to use.
	 */
	public EmailNodeStaleDataAlertProcessor(SolarNodeDao solarNodeDao, UserDao userDao,
			UserAlertDao userAlertDao, UserAlertSituationDao userAlertSituationDao,
			GeneralNodeDatumDao generalNodeDatumDao, MailService mailService, MessageSource messageSource) {
		super();
		this.solarNodeDao = solarNodeDao;
		this.userDao = userDao;
		this.userAlertDao = userAlertDao;
		this.userAlertSituationDao = userAlertSituationDao;
		this.generalNodeDatumDao = generalNodeDatumDao;
		this.mailService = mailService;
		this.messageSource = messageSource;
	}

	@Override
	public Long processAlerts(Long lastProcessedAlertId, DateTime validDate) {
		if ( validDate == null ) {
			validDate = new DateTime();
		}
		List<UserAlert> alerts = userAlertDao.findAlertsToProcess(UserAlertType.NodeStaleData,
				lastProcessedAlertId, validDate, batchSize);
		Long lastAlertId = null;
		try {
			// get our set of unique node IDs, to query for the latest data; 
			// maintain result listing order via LinkedHashMap, so our startingId logic works later on
			Map<Long, List<UserAlert>> alertNodeMapping = getAlertNodeMapping(alerts);
			if ( alerts.size() > 0 ) {
				Map<Long, List<GeneralNodeDatumFilterMatch>> latestNodeDataMapping = getLatestNodeData(alertNodeMapping);

				final long now = System.currentTimeMillis();

				// now we can re-iterate over alerts, processing those with stale data accordingly
				for ( Map.Entry<Long, List<UserAlert>> me : alertNodeMapping.entrySet() ) {
					List<GeneralNodeDatumFilterMatch> latestNodeData = latestNodeDataMapping.get(me
							.getKey());
					for ( UserAlert alert : me.getValue() ) {
						Map<String, Object> alertOptions = alert.getOptions();
						if ( alertOptions == null ) {
							continue;
						}

						// extract options
						Number age;
						String[] sourceIds;
						try {
							age = (Number) alertOptions.get(UserAlertOptions.AGE_THRESHOLD);
							sourceIds = (String[]) alertOptions.get(UserAlertOptions.SOURCE_IDS);
						} catch ( ClassCastException e ) {
							log.warn("Unexpected option data type in alert {}: {}", alert,
									e.getMessage());
							continue;
						}

						if ( age == null ) {
							log.debug("Skipping alert {} that does not include {} option", alert,
									UserAlertOptions.AGE_THRESHOLD);
							continue;
						}

						if ( sourceIds != null ) {
							// sort so we can to binarySearch later
							Arrays.sort(sourceIds);
						}

						// look for first stale data matching age + source criteria
						GeneralNodeDatumFilterMatch stale = null;
						for ( GeneralNodeDatumFilterMatch datum : latestNodeData ) {
							if ( datum.getId().getCreated().getMillis()
									+ (long) (age.doubleValue() * 1000) < now
									&& (sourceIds == null || Arrays.binarySearch(sourceIds, datum
											.getId().getSourceId()) >= 0) ) {
								stale = datum;
								break;
							}
						}

						if ( stale != null ) {
							// get UserAlertSitutation for this alert
							UserAlertSituation sit = userAlertSituationDao
									.getActiveAlertSituationForAlert(alert.getId());
							if ( sit == null ) {
								sit = new UserAlertSituation();
								sit.setCreated(new DateTime(now));
								sit.setAlert(alert);
								sit.setStatus(UserAlertSituationStatus.Active);
								sit.setNotified(new DateTime(now));
							}

							// taper off the alerts so the become less frequent over time
							if ( sit.getCreated().getMillis()
									+ (sit.getNotified().getMillis() - sit.getCreated().getMillis())
									* 1.5 >= now ) {
								sendAlertMail(alert, stale);
								sit.setNotified(new DateTime(now));
							}
							if ( sit.getNotified().getMillis() == now ) {
								userAlertSituationDao.store(sit);
							}
							alert.setValidTo(validDate.plus(MIN_ALERT_VALID_DELAY));
						} else {
							// not stale, so mark valid for age span
							alert.setValidTo(validDate.plusSeconds(age.intValue()));
						}
						userAlertDao.store(alert);
						lastAlertId = alert.getId();
					}
				}
			}
		} catch ( RuntimeException e ) {
			throw new RepeatableTaskException("Error processing user alerts", e, lastAlertId);
		}

		// short-circuit performing batch for no results if obvious
		if ( alerts.size() < batchSize && lastAlertId != null
				&& lastAlertId.equals(alerts.get(alerts.size() - 1).getId()) ) {
			// we've finished our batch
			lastAlertId = null;
		}

		return lastAlertId;
	}

	private void sendAlertMail(UserAlert alert, GeneralNodeDatumFilterMatch stale) {
		User user = userDao.get(alert.getUserId());
		SolarNode node = solarNodeDao.get(alert.getNodeId());
		if ( user != null ) {
			BasicMailAddress addr = new BasicMailAddress(user.getName(), user.getEmail());
			Locale locale = Locale.US; // TODO: get Locale from User entity
			Map<String, Object> model = new HashMap<String, Object>(4);
			model.put("alert", alert);
			model.put("user", user);
			model.put("datum", stale);

			// add a formatted datum date to model
			DateTimeFormatter dateFormat = timestampFormat.withLocale(locale);
			if ( node != null && node.getTimeZone() != null ) {
				dateFormat = dateFormat.withZone(DateTimeZone.forTimeZone(node.getTimeZone()));
			}
			model.put("datumDate", dateFormat.print(stale.getId().getCreated()));

			log.debug("Sending NodeStaleData alert to {} with model {}", user.getEmail(), model);

			String subject = messageSource.getMessage("user.alert.NodeStaleData.mail.subject",
					new Object[] { alert.getNodeId() }, locale);
			ClasspathResourceMessageTemplateDataSource msg = new ClasspathResourceMessageTemplateDataSource(
					locale, subject, mailTemplateResource, model);
			msg.setClassLoader(getClass().getClassLoader());
			mailService.sendMail(addr, msg);
		}
	}

	private Map<Long, List<UserAlert>> getAlertNodeMapping(List<UserAlert> alerts) {
		Map<Long, List<UserAlert>> alertNodeMapping = new LinkedHashMap<Long, List<UserAlert>>(
				alerts.size());
		for ( UserAlert alert : alerts ) {
			if ( alert.getNodeId() == null ) {
				log.debug("Skipping NodeStaleData alert with null nodeId: {}", alert);
				continue;
			}
			List<UserAlert> nodeList = alertNodeMapping.get(alert.getNodeId());
			if ( nodeList == null ) {
				nodeList = new ArrayList<UserAlert>(4);
				alertNodeMapping.put(alert.getNodeId(), nodeList);
			}
			nodeList.add(alert);
		}
		return alertNodeMapping;
	}

	private Map<Long, List<GeneralNodeDatumFilterMatch>> getLatestNodeData(
			Map<Long, List<UserAlert>> alertNodeMapping) {
		Long[] nodeIds = alertNodeMapping.keySet().toArray(new Long[alertNodeMapping.keySet().size()]);
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeIds(nodeIds);
		filter.setMostRecent(true);
		FilterResults<GeneralNodeDatumFilterMatch> latestNodeData = generalNodeDatumDao.findFiltered(
				filter, null, null, null);
		Map<Long, List<GeneralNodeDatumFilterMatch>> latestNodeDataMapping = new HashMap<Long, List<GeneralNodeDatumFilterMatch>>(
				latestNodeData.getReturnedResultCount());
		for ( GeneralNodeDatumFilterMatch match : latestNodeData.getResults() ) {
			List<GeneralNodeDatumFilterMatch> nodeData = latestNodeDataMapping.get(match.getId()
					.getNodeId());
			if ( nodeData == null ) {
				nodeData = new ArrayList<GeneralNodeDatumFilterMatch>(4);
				latestNodeDataMapping.put(match.getId().getNodeId(), nodeData);
			}
			nodeData.add(match);
		}
		return latestNodeDataMapping;
	}

	public Integer getBatchSize() {
		return batchSize;
	}

	public void setBatchSize(Integer batchSize) {
		this.batchSize = batchSize;
	}

	public String getMailTemplateResource() {
		return mailTemplateResource;
	}

	public void setMailTemplateResource(String mailTemplateResource) {
		this.mailTemplateResource = mailTemplateResource;
	}

	public DateTimeFormatter getTimestampFormat() {
		return timestampFormat;
	}

	public void setTimestampFormat(DateTimeFormatter timestampFormat) {
		this.timestampFormat = timestampFormat;
	}

}
