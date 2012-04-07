/* ==================================================================
 * OutboundMail.java - Jun 18, 2011 7:58:54 PM
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

package net.solarnetwork.central.dras.domain;

import java.io.Serializable;

import net.solarnetwork.central.domain.BaseEntity;
import net.solarnetwork.util.SerializeIgnore;

/**
 * A record of some mail message.
 * 
 * @author matt
 * @version $Revision$
 */
public class OutboundMail extends BaseEntity implements Cloneable, Serializable, Match {

	private static final long serialVersionUID = 5660176310180276590L;

	private Long creator;
	private String messageId;
	private String subject;
	private String messageBody;
	private String[] to;
	
	/**
	 * Default constructor.
	 */
	public OutboundMail() {
		super();
	}
	
	/**
	 * Construct with ID.
	 * 
	 * @param id the ID
	 */
	public OutboundMail(Long id) {
		super();
		setId(id);
	}
	
	/**
	 * Get the first "to" address.
	 * 
	 * @return first to value, or <em>null</em> if not available
	 */
	@SerializeIgnore
	public String getToAddress() {
		if ( to == null || to.length < 1 ) {
			return null;
		}
		return to[0];
	}
	
	/**
	 * Set a single "to" address.
	 * 
	 * @param address the address to set
	 */
	public void setToAddress(String address) {
		to = new String[] {address};
	}
	
	public Long getCreator() {
		return creator;
	}
	public void setCreator(Long creator) {
		this.creator = creator;
	}
	public String getMessageId() {
		return messageId;
	}
	public void setMessageId(String messageId) {
		this.messageId = messageId;
	}
	public String getSubject() {
		return subject;
	}
	public void setSubject(String subject) {
		this.subject = subject;
	}
	public String getMessageBody() {
		return messageBody;
	}
	public void setMessageBody(String messageBody) {
		this.messageBody = messageBody;
	}
	public String[] getTo() {
		return to;
	}
	public void setTo(String[] to) {
		this.to = to;
	}
	
}
