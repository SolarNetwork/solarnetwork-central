/* ==================================================================
 * BasicMailAddress.java - Jan 13, 2010 6:27:54 PM
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

import static net.solarnetwork.util.ObjectUtils.requireNonEmptyArgument;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.central.mail.MailAddress;

/**
 * Basic implementation of {@link MailAddress}.
 *
 * @author matt
 * @version 1.1
 */
public class BasicMailAddress implements MailAddress {

	private String[] to;
	private String @Nullable [] cc;
	private String @Nullable [] bcc;
	private @Nullable String from;

	/**
	 * Constructor.
	 *
	 * @param to
	 *        the to email address list
	 * @throws IllegalArgumentException
	 *         if {@code to} is empty
	 * @since 1.1
	 */
	public BasicMailAddress(String[] to) {
		super();
		this.to = requireNonEmptyArgument(to, "to");
	}

	/**
	 * Construct with a single "to" address.
	 *
	 * @param toName
	 *        the address display name
	 * @param toAddress
	 *        the email address
	 */
	public BasicMailAddress(@Nullable String toName, String toAddress) {
		this(new String[] { formatMailAddress(toName, toAddress) });
	}

	/**
	 * Format an email address.
	 * 
	 * @param name
	 *        the optional name
	 * @param email
	 *        the email
	 * @return the formatted address
	 */
	public static String formatMailAddress(@Nullable String name, String email) {
		if ( name == null || name.isEmpty() ) {
			return email;
		}
		return "\"" + name + "\" <" + email + ">";
	}

	@Override
	public final String @Nullable [] getBcc() {
		return bcc == null ? null : bcc.clone();
	}

	@Override
	public final String @Nullable [] getCc() {
		return cc == null ? null : cc.clone();
	}

	@Override
	public final @Nullable String getFrom() {
		return from;
	}

	@Override
	public final String[] getTo() {
		return to.clone();
	}

	/**
	 * Set the recipients.
	 *
	 * @param to
	 *        the recipients to set
	 * @throws IllegalArgumentException
	 *         if {@code to} is empty
	 */
	public final void setTo(String[] to) {
		this.to = requireNonEmptyArgument(to, "to");
	}

	/**
	 * Set the CC recipients.
	 *
	 * @param cc
	 *        the cc recipients to set
	 */
	public final void setCc(String @Nullable [] cc) {
		this.cc = cc;
	}

	/**
	 * Set the BCC recipients.
	 *
	 * @param bcc
	 *        the bcc recipients to set
	 */
	public final void setBcc(String @Nullable [] bcc) {
		this.bcc = bcc;
	}

	/**
	 * Set the sender.
	 *
	 * @param from
	 *        the sender to set
	 */
	public final void setFrom(@Nullable String from) {
		this.from = from;
	}

}
