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

import net.solarnetwork.central.mail.MailAddress;

/**
 * Basic implementation of {@link MailAddress}.
 * 
 * @author matt
 * @version 1.1
 */
public class BasicMailAddress implements MailAddress {

	private String[] to;
	private String[] cc;
	private String[] bcc;
	private String from;

	/**
	 * Constructor.
	 * 
	 * @param to
	 *        the to email address list
	 * @since 1.1
	 */
	public BasicMailAddress(String[] to) {
		super();
		this.to = to;
	}

	/**
	 * Construct with a single "to" address.
	 * 
	 * @param toName
	 *        the address display name
	 * @param toAddress
	 *        the email address
	 */
	public BasicMailAddress(String toName, String toAddress) {
		this.to = new String[] { formatMailAddress(toName, toAddress) };
	}

	@Override
	public String[] getBcc() {
		return bcc == null ? null : bcc.clone();
	}

	@Override
	public String[] getCc() {
		return cc == null ? null : cc.clone();
	}

	@Override
	public String getFrom() {
		return from;
	}

	@Override
	public String[] getTo() {
		return to == null ? null : to.clone();
	}

	private String formatMailAddress(String name, String email) {
		if ( name == null || name.length() < 1 ) {
			return email;
		}
		return "\"" + name + "\" <" + email + ">";
	}

	/**
	 * @param to
	 *        the to to set
	 */
	public void setTo(String[] to) {
		this.to = to;
	}

	/**
	 * @param cc
	 *        the cc to set
	 */
	public void setCc(String[] cc) {
		this.cc = cc;
	}

	/**
	 * @param bcc
	 *        the bcc to set
	 */
	public void setBcc(String[] bcc) {
		this.bcc = bcc;
	}

	/**
	 * @param from
	 *        the from to set
	 */
	public void setFrom(String from) {
		this.from = from;
	}

}
