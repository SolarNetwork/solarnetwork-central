/* ==================================================================
 * DefaultMailService.java - Jan 13, 2010 6:43:19 PM
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

package net.solarnetwork.central.mail.support;

import static org.springframework.util.StringUtils.arrayToCommaDelimitedString;
import java.util.Arrays;
import java.util.Iterator;
import javax.mail.MessagingException;
import org.apache.commons.text.WordUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.mail.MailMessage;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMailMessage;
import org.springframework.mail.javamail.MimeMessageHelper;
import net.solarnetwork.central.mail.MailAddress;
import net.solarnetwork.central.mail.MailService;
import net.solarnetwork.central.mail.MessageDataSource;

/**
 * Default implementation of {@link MailService} that uses Spring's mail classes
 * for sending mail.
 * 
 * @author matt
 * @version 2.1
 */
public class DefaultMailService implements MailService {

	private final MailSender mailSender;
	private SimpleMailMessage templateMessage;
	private int hardWrapColumnIndex = 0;
	private boolean html = false;

	private final Logger log = LoggerFactory.getLogger(DefaultMailService.class);

	/**
	 * Constructor.
	 * 
	 * @param mailSender
	 *        the {@link MailSender} to use
	 */
	public DefaultMailService(MailSender mailSender) {
		this.mailSender = mailSender;
	}

	private void prepareMailMessage(MailMessage msg, MailAddress address,
			MessageDataSource messageDataSource) {
		if ( templateMessage != null ) {
			templateMessage.copyTo(msg);
		}
		msg.setTo(address.getTo());
		if ( address.getFrom() != null ) {
			msg.setFrom(address.getFrom());
		}
		if ( address.getCc() != null && address.getCc().length > 0 ) {
			msg.setCc(address.getCc());
		}
		if ( address.getBcc() != null && address.getBcc().length > 0 ) {
			msg.setBcc(address.getBcc());
		}
		if ( messageDataSource.getSubject() != null ) {
			msg.setSubject(messageDataSource.getSubject());
		}

		if ( !html ) {
			String msgText = messageDataSource.getBody();
			if ( msgText != null ) {
				int wrapColumn = hardWrapColumnIndex;
				if ( wrapColumn > 0 ) {
					// WordUtils doesn't preserve paragraphs, so first split text into paragraph strings and wrap each of those
					StringBuilder buf = new StringBuilder();
					String[] paragraphs = msgText.split("\n{2,}");
					for ( String para : paragraphs ) {
						if ( buf.length() > 0 ) {
							buf.append("\n\n");
						}
						// we also replace all single \n within the paragraph with spaces, in case the message was already hard-wrapped
						buf.append(WordUtils.wrap(para.replace("\n", " "), wrapColumn));
					}
					msgText = buf.toString();
				}
				msg.setText(msgText);
			}
		}
	}

	@Override
	public void sendMail(MailAddress address, MessageDataSource messageDataSource) {
		final Iterator<Resource> attachments = (messageDataSource.getAttachments() != null
				? messageDataSource.getAttachments().iterator()
				: null);
		if ( html || attachments != null && attachments.hasNext() ) {
			// need JavaMailSender to send attachments
			if ( !(mailSender instanceof JavaMailSender) ) {
				throw new RuntimeException("Cannot send mail attachments without a JavaMailSender.");
			}
			JavaMailSender sender = (JavaMailSender) mailSender;
			try {
				MimeMailMessage msg = new MimeMailMessage(new MimeMessageHelper(
						sender.createMimeMessage(), MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED));
				if ( html ) {
					msg.getMimeMessageHelper().setText(messageDataSource.getBody(), true);
				}
				prepareMailMessage(msg, address, messageDataSource);
				if ( attachments != null ) {
					while ( attachments.hasNext() ) {
						Resource att = attachments.next();
						msg.getMimeMessageHelper().addAttachment(att.getFilename(), att);
					}
				}
				if ( log.isInfoEnabled() ) {
					log.info("Sending MIME mail [{}] from [{}] to [{}]",
							msg.getMimeMessage().getSubject(),
							arrayToCommaDelimitedString(msg.getMimeMessage().getFrom()),
							arrayToCommaDelimitedString(msg.getMimeMessage().getAllRecipients()));
				}
				sender.send(msg.getMimeMessage());
			} catch ( MessagingException e ) {
				String err = String.format("Error preparing mail [%s] to %s: %s",
						messageDataSource.getSubject() != null ? messageDataSource.getSubject()
								: templateMessage.getSubject(),
						Arrays.toString(address.getTo()), e.getMessage());
				throw new RuntimeException(err, e);
			}
		} else {
			SimpleMailMessage msg = new SimpleMailMessage();
			prepareMailMessage(msg, address, messageDataSource);
			if ( log.isInfoEnabled() ) {
				log.info("Sending mail [{}] from [{}] to [{}]", msg.getSubject(), msg.getFrom(),
						arrayToCommaDelimitedString(msg.getTo()));
			}
			mailSender.send(msg);
		}
	}

	/**
	 * Get the template to use as a starting point for all messages.
	 * 
	 * @return the template message, or {@literal null}
	 */
	public SimpleMailMessage getTemplateMessage() {
		return templateMessage;
	}

	/**
	 * Set the template to use as a starting point for all messages.
	 * 
	 * <p>
	 * This can be used to configure a default "from" address, for example.
	 * </p>
	 * 
	 * @param templateMessage
	 *        the template to use
	 */
	public void setTemplateMessage(SimpleMailMessage templateMessage) {
		this.templateMessage = templateMessage;
	}

	/**
	 * Get the hard-wrap character column size setting.
	 * 
	 * @return The hard-wrap column.
	 * @since 1.1
	 */
	public int getHardWrapColumnIndex() {
		return hardWrapColumnIndex;
	}

	/**
	 * Set a character index to hard-wrap message text at. Hard-wrapping is
	 * disabled by setting this to zero.
	 * 
	 * @param hardWrapColumnIndex
	 *        The column index to hard-wrap message text at, or <code>0</code>
	 *        to disable hard wrapping.
	 * @since 1.1
	 */
	public void setHardWrapColumnIndex(int hardWrapColumnIndex) {
		this.hardWrapColumnIndex = hardWrapColumnIndex;
	}

	/**
	 * Get the HTML content flag.
	 * 
	 * @return {@literal true} if the message body is HTML, {@literal false} for
	 *         plain text; defaults to {@literal false}
	 * @since 1.3
	 */
	public boolean isHtml() {
		return html;
	}

	/**
	 * Set the HTML content flag.
	 * 
	 * @param html
	 *        {@literal true} if the message body is HTML, {@literal false} for
	 *        plain text
	 * @since 1.3
	 */
	public void setHtml(boolean html) {
		this.html = html;
	}

}
