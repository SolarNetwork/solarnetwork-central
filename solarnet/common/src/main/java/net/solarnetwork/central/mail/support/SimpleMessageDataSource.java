/* ==================================================================
 * SimpleMessageDataSource.java - 27/07/2020 8:56:24 AM
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

import org.springframework.core.io.Resource;
import net.solarnetwork.central.mail.MessageDataSource;

/**
 * Simple implementation of {@link MessageDataSource}.
 * 
 * @author matt
 * @version 1.0
 */
public class SimpleMessageDataSource implements MessageDataSource {

	private final String subject;
	private final String body;
	private final Iterable<Resource> attachments;

	/**
	 * Constructor.
	 * 
	 * @param subject
	 *        the message subject
	 */
	public SimpleMessageDataSource(String subject) {
		this(subject, null, null);
	}

	/**
	 * Constructor.
	 * 
	 * @param subject
	 *        the message subject
	 * @param body
	 *        the message body
	 */
	public SimpleMessageDataSource(String subject, String body) {
		this(subject, body, null);
	}

	/**
	 * Constructor.
	 * 
	 * @param subject
	 *        the message subject
	 * @param body
	 *        the message body
	 * @param attachments
	 *        the message attachments
	 */
	public SimpleMessageDataSource(String subject, String body, Iterable<Resource> attachments) {
		super();
		this.subject = subject;
		this.body = body;
		this.attachments = attachments;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("SimpleMessageDataSource{");
		builder.append(subject);
		builder.append("}");
		return builder.toString();
	}

	@Override
	public String getSubject() {
		return subject;
	}

	@Override
	public String getBody() {
		return body;
	}

	@Override
	public Iterable<Resource> getAttachments() {
		return attachments;
	}

}
