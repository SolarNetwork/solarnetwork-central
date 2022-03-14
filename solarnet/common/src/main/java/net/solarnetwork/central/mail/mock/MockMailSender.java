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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.MailMessage;
import org.springframework.mail.MailParseException;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMailMessage;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.util.FileCopyUtils;

/**
 * Mock implementation of Spring's {@link MailSender}.
 * 
 * <p>
 * This implementation will log sending of messages only.
 * </p>
 * 
 * @author matt
 * @version 1.2
 */
public class MockMailSender implements MailSender, JavaMailSender {

	private final Logger log = LoggerFactory.getLogger(MockMailSender.class);

	private final Session session = Session.getInstance(new Properties());
	private final Queue<MailMessage> sent = new ConcurrentLinkedQueue<MailMessage>();

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
	public void send(SimpleMailMessage... msgs) throws MailException {
		if ( msgs == null ) {
			return;
		}
		for ( SimpleMailMessage msg : msgs ) {
			send(msg);
		}
	}

	private Session getSession() {
		return session;
	}

	@Override
	public MimeMessage createMimeMessage() {
		return new MimeMessage(getSession());
	}

	@Override
	public MimeMessage createMimeMessage(InputStream contentStream) throws MailException {
		try {
			return new MimeMessage(getSession(), contentStream);
		} catch ( Exception ex ) {
			throw new MailParseException("Could not parse raw MIME content", ex);
		}
	}

	private void extractContent(Object content, StringBuilder buf)
			throws MessagingException, IOException {
		if ( content instanceof String ) {
			buf.append(content.toString());
		} else if ( content instanceof InputStream ) {
			buf.append(
					FileCopyUtils.copyToString(new InputStreamReader((InputStream) content, "UTF-8")));
		} else if ( content instanceof MimeMultipart ) {
			MimeMultipart multi = (MimeMultipart) content;
			for ( int i = 0; i < multi.getCount(); i++ ) {
				BodyPart part = multi.getBodyPart(i);
				extractContent(part, buf);
			}
		} else if ( content instanceof BodyPart ) {
			BodyPart part = (BodyPart) content;
			extractContent(part.getContent(), buf);
		}
	}

	@Override
	public void send(MimeMessage mimeMessage) throws MailException {
		if ( mimeMessage == null ) {
			return;
		}
		try {
			StringBuilder buf = new StringBuilder();
			extractContent(mimeMessage.getContent(), buf);
			log.info("MOCK: sending MIME mail from {} to {} with content:\n{}\n", mimeMessage.getFrom(),
					mimeMessage.getAllRecipients(), buf.toString());
		} catch ( IOException | MessagingException e ) {
			// ignore
		}
		sent.add(new MimeMailMessage(mimeMessage));
	}

	@Override
	public void send(MimeMessage... mimeMessages) throws MailException {
		if ( mimeMessages == null ) {
			return;
		}
		for ( MimeMessage msg : mimeMessages ) {
			send(msg);
		}

	}

	@Override
	public void send(MimeMessagePreparator mimeMessagePreparator) throws MailException {
		if ( mimeMessagePreparator == null ) {
			return;
		}
		MimeMessage mimeMessage = createMimeMessage();
		try {
			mimeMessagePreparator.prepare(mimeMessage);
		} catch ( Exception e ) {
			throw new RuntimeException(e);
		}
		send(mimeMessage);
	}

	@Override
	public void send(MimeMessagePreparator... mimeMessagePreparators) throws MailException {
		if ( mimeMessagePreparators == null ) {
			return;
		}
		for ( MimeMessagePreparator mimeMessagePreparator : mimeMessagePreparators ) {
			send(mimeMessagePreparator);
		}
	}

	public Logger getLog() {
		return log;
	}

	/**
	 * Get a list of all sent messages. This list can be cleared during unit
	 * tests to keep track of the messages sent during the test.
	 * 
	 * @return List of messages, never {@literal null}.
	 * @since 1.1
	 */
	public Queue<MailMessage> getSent() {
		return sent;
	}

}
