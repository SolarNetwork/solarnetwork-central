/* ==================================================================
 * MessageDataSource.java - 27/07/2020 8:42:46 AM
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

package net.solarnetwork.central.mail;

import org.springframework.core.io.Resource;

/**
 * API for mail message content.
 * 
 * @author matt
 * @version 1.0
 * @since 1.5
 */
public interface MessageDataSource {

	/**
	 * Get the message subject.
	 * 
	 * @return the message subject
	 */
	String getSubject();

	/**
	 * Get the message body.
	 * 
	 * @return the message body, or {@literal null} if none
	 */
	String getBody();

	/**
	 * Get a collection of message attachments.
	 * 
	 * @return the attachments, or {@literal null} if none
	 */
	Iterable<Resource> getAttachments();

}
