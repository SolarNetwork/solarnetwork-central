/* ==================================================================
 * MessageTemplateDataSource.java - Jan 13, 2010 6:13:36 PM
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

import java.util.Locale;
import java.util.Map;
import org.springframework.core.io.Resource;

/**
 * API for data required to generate a template based mail message.
 * 
 * @author matt
 * @version 1.1
 */
public interface MessageTemplateDataSource extends MessageDataSource {

	/**
	 * A sensible standard column width to use for word-wrapping.
	 * 
	 * @since 1.1
	 */
	int STANDARD_WORD_WRAP_COLUMN = 80;

	/**
	 * Get a message template model to merge into the message.
	 * 
	 * @return Map of model objects
	 */
	Map<String, ?> getModel();

	/**
	 * Get a Locale for the message.
	 * 
	 * @return a Locale
	 */
	Locale getLocale();

	/**
	 * Get the message template.
	 * 
	 * @return message template
	 */
	Resource getMessageTemplate();

	/**
	 * Get the message subject.
	 * 
	 * @return message subject
	 */
	@Override
	String getSubject();

	/**
	 * Get a character column index at which to hard-wrap message text at.
	 * Return <code>0</code> to indicate no wrapping should occur.
	 * 
	 * @return The word wrap character column index, or <em>null</em> if
	 *         unspecified.
	 * @since 1.1
	 */
	Integer getWordWrapCharacterIndex();

}
