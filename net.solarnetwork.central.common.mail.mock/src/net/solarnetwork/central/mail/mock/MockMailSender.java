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
 * $Id$
 * ==================================================================
 */

package net.solarnetwork.central.mail.mock;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;

/**
 * Mock implementation of Spring's {@link MailSender}.
 * 
 * <p>This implementation will log sending of messages only.</p>
 * 
 * @author matt
 * @version $Id$
 */
public class MockMailSender implements MailSender {
	
	private final Logger log = LoggerFactory.getLogger(MockMailSender.class);

	@Override
	public void send(SimpleMailMessage msg) throws MailException {
		if ( msg == null ) {
			return;
		}
		
		log.info(String.format("MOCK: sending mail to %s with text: %s\n", 
				Arrays.toString(msg.getTo()), msg.getText()));
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

}
