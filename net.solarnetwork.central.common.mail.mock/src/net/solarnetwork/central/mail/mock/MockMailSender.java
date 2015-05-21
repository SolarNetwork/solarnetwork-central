/* ==================================================================
 * MockMailSender.java - Jan 14, 2010 5:13:15 PM
 * 
 * Copyright 2007-2010 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.mail.mock;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;

/**
 * Mock implementation of Spring's {@link MailSender}.
 * 
 * <p>
 * This implementation will log sending of messages only.
 * </p>
 * 
 * @author matt
 * @version 1.1
 */
public class MockMailSender implements MailSender {

	private final Logger log = LoggerFactory.getLogger(MockMailSender.class);

	private final Queue<SimpleMailMessage> sent = new ConcurrentLinkedQueue<SimpleMailMessage>();

	@Override
	public void send(SimpleMailMessage msg) throws MailException {
		if ( msg == null ) {
			return;
		}

		log.info("MOCK: sending mail from {} to {} with text:\n{}\n", msg.getFrom(), msg.getTo(),
				msg.getText());
		sent.add(msg);
	}

	@Override
	public void send(SimpleMailMessage[] msgs) throws MailException {
		if ( msgs == null ) {
			return;
		}
		for ( SimpleMailMessage msg : msgs ) {
			send(msg);
		}
	}

	public Logger getLog() {
		return log;
	}

	/**
	 * Get a list of all sent messages. This list can be cleared during unit
	 * tests to keep track of the messages sent during the test.
	 * 
	 * @return List of messages, never <em>null</em>.
	 * @since 1.1
	 */
	public Queue<SimpleMailMessage> getSent() {
		return sent;
	}

}
