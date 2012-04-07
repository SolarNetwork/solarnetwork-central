/* ==================================================================
 * MailAddress.java - Jan 13, 2010 6:21:28 PM
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

package net.solarnetwork.central.mail;

/**
 * API for mail address information.
 * 
 * @author matt
 * @version $Id$
 */
public interface MailAddress {

	/**
	 * Get list of addresses to send the mail to.
	 * 
	 * @return array of email addresses
	 */
	String[] getTo();
	
	/**
	 * Get list of addresses to carbon-copy the mail to.
	 * 
	 * @return array of email addresses
	 */
	String[] getCc();
	
	/**
	 * Get list of addresses to blind-carbon-copy the mail to.
	 * 
	 * @return array of email addresses
	 */
	String[] getBcc();
	
	/**
	 * Get the address to send the mail from.
	 * 
	 * @return email address
	 */
	String getFrom();
	
}
