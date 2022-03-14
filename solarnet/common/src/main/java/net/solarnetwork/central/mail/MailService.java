/* ==================================================================
 * MailService.java - Jan 13, 2010 6:11:31 PM
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

package net.solarnetwork.central.mail;

/**
 * API for a mail sending service.
 * 
 * @author matt
 * @version 1.1
 */
public interface MailService {

	/**
	 * Send a template-based mail message.
	 * 
	 * @param address
	 *        where to send the mail to
	 * @param messageDataSource
	 *        the message data source
	 */
	void sendMail(MailAddress address, MessageDataSource messageDataSource);

}
