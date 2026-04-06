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

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;
import org.springframework.core.io.Resource;
import net.solarnetwork.central.mail.MessageDataSource;

/**
 * Simple implementation of {@link MessageDataSource}.
 * 
 * @author matt
 * @version 1.1
 */
public class SimpleMessageDataSource implements MessageDataSource {

	private final String subject;
	private final @Nullable Iterable<Resource> attachments;
	private @Nullable Supplier<String> body;

	/**
	 * Constructor.
	 * 
	 * @param subject
	 *        the message subject
	 * @throws IllegalArgumentException
	 *         if {@code subject} is {@code null}
	 */
	public SimpleMessageDataSource(String subject) {
		this(subject, null);
	}

	/**
	 * Constructor.
	 * 
	 * @param subject
	 *        the message subject
	 * @param body
	 *        the message body
	 * @throws IllegalArgumentException
	 *         if {@code subject} is {@code null}
	 */
	public SimpleMessageDataSource(String subject, @Nullable String body) {
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
	 * @throws IllegalArgumentException
	 *         if {@code subject} is {@code null}
	 */
	public SimpleMessageDataSource(String subject, @Nullable String body,
			@Nullable Iterable<Resource> attachments) {
		this(subject, stringSupplier(body), attachments);
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
	 * @throws IllegalArgumentException
	 *         if {@code subject} is {@code null}
	 * @since 1.1
	 */
	public SimpleMessageDataSource(String subject, @Nullable Supplier<String> body,
			@Nullable Iterable<Resource> attachments) {
		super();
		this.subject = requireNonNullArgument(subject, "subject");
		this.body = body;
		this.attachments = attachments;
	}

	private static @Nullable Supplier<String> stringSupplier(@Nullable String s) {
		if ( s == null ) {
			return null;
		}
		return s::toString;
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
	public final String getSubject() {
		return subject;
	}

	@Override
	public final @Nullable String getBody() {
		final Supplier<String> b = this.body;
		return (b != null ? b.get() : null);
	}

	/**
	 * Set the body supplier.
	 * 
	 * @param supplier
	 *        the supplier
	 */
	protected final void setBodySupplier(@Nullable Supplier<String> supplier) {
		this.body = supplier;
	}

	@Override
	public final @Nullable Iterable<Resource> getAttachments() {
		return attachments;
	}

}
