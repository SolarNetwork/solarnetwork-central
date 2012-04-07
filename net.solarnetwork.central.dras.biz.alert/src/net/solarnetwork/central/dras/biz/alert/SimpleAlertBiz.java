/* ==================================================================
 * SimpleAlertBiz.java - Jun 18, 2011 7:42:39 PM
 * 
 * Copyright 2007-2011 SolarNetwork.net Dev Team
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
 * $Id$
 * ==================================================================
 */

package net.solarnetwork.central.dras.biz.alert;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.central.domain.Identity;
import net.solarnetwork.central.dras.biz.AlertBiz;
import net.solarnetwork.central.dras.dao.EventDao;
import net.solarnetwork.central.dras.dao.OutboundMailDao;
import net.solarnetwork.central.dras.dao.ProgramDao;
import net.solarnetwork.central.dras.dao.UserDao;
import net.solarnetwork.central.dras.domain.Event;
import net.solarnetwork.central.dras.domain.Match;
import net.solarnetwork.central.dras.domain.Member;
import net.solarnetwork.central.dras.domain.OutboundMail;
import net.solarnetwork.central.dras.domain.User;
import net.solarnetwork.central.dras.domain.UserContact;
import net.solarnetwork.central.dras.support.SimpleAlertProcessingResult;
import net.solarnetwork.central.dras.support.SimpleUserFilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Default implementation of {@link AlertBiz}.
 * 
 * @author matt
 * @version $Revision$
 */
@Service
public class SimpleAlertBiz implements AlertBiz {

	@Autowired private EventDao eventDao;
	@Autowired private OutboundMailDao outboundMailDao;
	@Autowired private UserDao userDao;
	@Autowired private ProgramDao programDao;
	@Autowired private MailSender mailSender;
	
	@Autowired(required = false)
	private ExecutorService processor = Executors.newSingleThreadExecutor();
	
	@Autowired(required = false)
	private TransactionTemplate transactionTemplate;
	
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	@Override
	public Future<AlertProcessingResult> postAlert(Alert alert) {
		log.info("Alert received: {}", alert.getAlertType());
		AlertRunner runner = new AlertRunner(alert);
		return processor.submit(runner);
	}
	
	/**
	 * Shutdown the thread processing alerts.
	 * 
	 * @param secs the maximum number of seconds to wait
	 */
	public void shutdown(long secs) {
		processor.shutdown();
		try {
			processor.awaitTermination(secs, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			log.debug("Interrupted waiting for ExecutorService to shut down");
		}
	}
	
	// internal method called on background thread
	private void handleAlert(Alert alert, SimpleAlertProcessingResult result, Long creatorId) {
		Identity<Long> regarding = alert.getRegardingIdentity();
		Set<Identity<Long>> alerted = new LinkedHashSet<Identity<Long>>();
		if ( regarding instanceof Event ) {
			handleEventAlert(alert, (Event)regarding, alerted, creatorId);
		}
		result.setAlertedUsers(alerted);
	}
	
	// TODO: need localized message template support
	private static final String DEFAULT_EVENT_CREATED_MESSAGE_TEMPLATE
		= "Event ID %d (%s) created. Event date: %ta, %<te %<tb %<tY, %<tR %<tZ";
	
	private static final String DEFAULT_EVENT_MODIFIED_MESSAGE_TEMPLATE
		= "Event ID %d (%s) modified. Event date: %ta, %<te %<tb %<tY, %<tR %<tZ";
	
	private static final String DEFAULT_EVENT_UNKNOWN_MESSAGE_TEMPLATE
		= "Event ID %d (%s) alert: %s";

	private static final String DEFAULT_EVENT_CREATED_SUBJECT_TEMPLATE
		= "SolarADR event ID %d (%s) created";

	private static final String DEFAULT_EVENT_MODIFIED_SUBJECT_TEMPLATE
		= "SolarADR event ID %d (%s) modified";
	
	private static final String DEFAULT_EVENT_UNKNOWN_SUBJECT_TEMPLATE
		= "SolarADR event ID %d (%s) alert";
	
	private void handleEventAlert(final Alert alert, final Event event, 
			final Set<Identity<Long>> alerted, final Long creatorId) {
		Set<Member> users;
		if ( AlertBiz.ALERT_TYPE_ENTITY_CREATED.equals(alert.getAlertType()) ) {
			// when creating, send alert to all users in program
			users = programDao.resolveUserMembers(event.getProgramId(), null);
		} else {
			// get all users owning participants of given event, send alert to each one
			users = eventDao.resolveUserMembers(event.getId(), null);
		}
		for ( Member m : users ) {
			User user = userDao.get(m.getId());
			String message = null;
			String subject = null;
			String eventName = event.getName() == null ? "Unnamed" : event.getName();
			if ( AlertBiz.ALERT_TYPE_ENTITY_CREATED.equals(alert.getAlertType()) ) {
				subject = String.format(DEFAULT_EVENT_CREATED_SUBJECT_TEMPLATE, 
						event.getId(), eventName);
				message = String.format(DEFAULT_EVENT_CREATED_MESSAGE_TEMPLATE, 
						event.getId(), eventName, event.getEventDate().toDate());
			} else if ( AlertBiz.ALERT_TYPE_ENTITY_MODIFIED.equals(alert.getAlertType()) ) {
				subject = String.format(DEFAULT_EVENT_MODIFIED_SUBJECT_TEMPLATE, 
						event.getId(), eventName);
				message = String.format(DEFAULT_EVENT_MODIFIED_MESSAGE_TEMPLATE, 
						event.getId(), eventName, event.getEventDate().toDate());
			} else {
				// unknown type
				subject = String.format(DEFAULT_EVENT_UNKNOWN_SUBJECT_TEMPLATE, 
						event.getId(), eventName);
				message = String.format(DEFAULT_EVENT_UNKNOWN_MESSAGE_TEMPLATE, 
						event.getId(), event.getName(), alert.getAlertType());
			}
			if ( handleAlert(user, alert, subject, message, creatorId) ) {
				alerted.add(user);
			}
		}
	}
	
	private boolean handleAlert(final User user, final Alert alert, 
			final String subject, final String message, final Long creatorId) {
		List<UserContact> contacts = user.getContactInfo();
		if ( contacts == null ) {
			return false;
		}
		
		// get the user's preferred contact method
		UserContact contact = null;
		for ( UserContact aContact : contacts ) {
			if ( aContact.getPriority() == null ) {
				continue;
			}
			if ( contact == null || (aContact.getPriority() < contact.getPriority()) ) {
				contact = aContact;
			}
		}
		if ( contact == null ) {
			log.debug("User {} has no preferred contact method, not sending alert {}",
					user.getUsername(), alert.getAlertType());
			return false;
		}
		
		// send the user an alert... only email supported currently
		SimpleMailMessage msg = new SimpleMailMessage();
		msg.setTo(contact.getContact());
		msg.setText(message);
		switch ( contact.getKind() ) {
		case MOBILE:
			// treat as an email to their mobile number
			// TODO: extract out mobile SMS handling to configurable service
			msg.setTo(contact.getContact().replaceAll("\\D", "") + "@isms.net.nz");
			msg.setFrom("escalation@econz.co.nz");
			
			break;
			
		case EMAIL:
			msg.setSubject(subject);
			msg.setFrom("solar-adr@solarnetwork.net");
			break;
			
		default:
			log.debug("User {} contact type {} not supported in alerts",
					user.getUsername(), contact.getKind());
			return false;
		}
		sendMailMessage(msg, creatorId);
		return true;
	}

	private void sendMailMessage(final SimpleMailMessage msg, final Long creatorId) {
		OutboundMail out = new OutboundMail();
		out.setCreator(creatorId);
		out.setMessageBody(msg.getText());
		out.setTo(msg.getTo());
		sendMailMessage(msg, out);
	}

	private void sendMailMessage(SimpleMailMessage msg, OutboundMail out) {
		mailSender.send(msg);
		Long id = outboundMailDao.store(out);
		log.debug("Saved OutboundMail {} to {}", id, out.getToAddress());
	}
	
	private Long getCurrentUserId() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if ( auth == null ) {
			log.info("No Authentication available, cannot tell current user ID");
			return null;
		}
		String currentUserName = auth.getName();
		SimpleUserFilter filter = new SimpleUserFilter();
		filter.setUniqueId(currentUserName);
		FilterResults<Match> results = userDao.findFiltered(filter, null, null, null);
		if ( results.getReturnedResultCount() < 1 ) {
			return null;
		}
		return results.getResults().iterator().next().getId();
	}
	
	/**
	 * Simple Runnable to off load event processing to another thread.
	 */
	private class AlertRunner implements Callable<AlertProcessingResult> {
		
		private final Alert alert;
		private final Long userId;
		
		private AlertRunner(Alert alert) {
			this.alert = alert;
			userId = (alert.getActorId() != null ? alert.getActorId() : getCurrentUserId());
		}
		
		@Override
		public AlertProcessingResult call() {
			final SimpleAlertProcessingResult result = new SimpleAlertProcessingResult(alert);
			if ( transactionTemplate != null ) {
				transactionTemplate.execute(new TransactionCallbackWithoutResult() {
					@Override
					protected void doInTransactionWithoutResult(TransactionStatus status) {
						handleAlert(alert, result, userId);
					}
				});
			} else {
				handleAlert(alert, result, userId);
			}
			return result;
		}
		
	}

	public void setEventDao(EventDao eventDao) {
		this.eventDao = eventDao;
	}
	public void setOutboundMailDao(OutboundMailDao outboundMailDao) {
		this.outboundMailDao = outboundMailDao;
	}
	public void setProgramDao(ProgramDao programDao) {
		this.programDao = programDao;
	}
	public void setUserDao(UserDao userDao) {
		this.userDao = userDao;
	}
	public void setMailSender(MailSender mailSender) {
		this.mailSender = mailSender;
	}
	public void setTransactionTemplate(TransactionTemplate transactionTemplate) {
		this.transactionTemplate = transactionTemplate;
	}
	public void setProcessor(ExecutorService processor) {
		this.processor = processor;
	}
	
}
