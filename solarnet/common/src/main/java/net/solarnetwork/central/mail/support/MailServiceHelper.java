/* ==================================================================
 * MailServiceHelper.java - Jan 14, 2010 11:56:12 AM
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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import net.solarnetwork.central.mail.MailAddress;
import net.solarnetwork.central.mail.MessageTemplateDataSource;

/**
 * Factory helper class for creating mail objects.
 * 
 * @author matt
 * @version 1.0
 */
public class MailServiceHelper implements Serializable {

	private static final long serialVersionUID = 534833315331249860L;

	/**
	 * Create a new {@link MailAddress} from a display name and an email
	 * address.
	 * 
	 * @param toName
	 *        the display name
	 * @param toAddress
	 *        the email address
	 * @return new MailAddress
	 */
	public MailAddress createAddress(String toName, String toAddress) {
		return new BasicMailAddress(toName, toAddress);
	}

	/**
	 * Create a new {@link MessageTemplateDataSource} from necessary components.
	 * 
	 * @param subject
	 *        the mail subject
	 * @param resourcePath
	 *        the resource path
	 * @param locale
	 *        the message locale
	 * @param params
	 *        the message template parameters
	 * @return new MessageTemplateDataSource
	 */
	public MessageTemplateDataSource createResourceDataSource(String subject, String resourcePath,
			Locale locale, Object... params) {
		Map<String, Object> model = new HashMap<String, Object>();
		for ( Object o : params ) {
			Class<?> clazz = o.getClass();
			// prefer interfaces to class names if possible
			if ( !clazz.isInterface() && clazz.getInterfaces().length > 0 ) {
				clazz = clazz.getInterfaces()[0];
			}
			model.put(clazz.getSimpleName(), o);
		}
		return new ClasspathResourceMessageTemplateDataSource(locale, subject, resourcePath, model);
	}

}
