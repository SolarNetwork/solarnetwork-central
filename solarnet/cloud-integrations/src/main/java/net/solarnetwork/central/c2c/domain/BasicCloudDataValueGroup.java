/* ==================================================================
 * BasicCloudDataValueGroup.java - 30/09/2024 8:07:27 am
 *
 * Copyright 2024 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.c2c.domain;

import java.util.Locale;
import java.util.Map;
import net.solarnetwork.domain.BasicLocalizedServiceInfo;

/**
 * Basic implementation of {@link CloudDataValueGroup}.
 *
 * @author matt
 * @version 1.0
 */
public class BasicCloudDataValueGroup extends BasicLocalizedServiceInfo implements CloudDataValueGroup {

	private final Iterable<CloudDataValue> values;
	private final Iterable<CloudDataValueGroup> groups;

	/**
	 * Constructor.
	 *
	 * @param id
	 *        the unique service identifier
	 * @param locale
	 *        the locale
	 * @param name
	 *        the localized name
	 * @param description
	 *        the localized description
	 * @param infoMessages
	 *        the localized info messages
	 * @param values
	 *        the values
	 * @param groups
	 *        the groups
	 */
	public BasicCloudDataValueGroup(String id, Locale locale, String name, String description,
			Map<String, String> infoMessages, Iterable<CloudDataValue> values,
			Iterable<CloudDataValueGroup> groups) {
		super(id, locale, name, description, infoMessages);
		this.values = values;
		this.groups = groups;
	}

	@Override
	public Iterable<CloudDataValue> values() {
		return values;
	}

	@Override
	public Iterable<CloudDataValueGroup> groups() {
		return groups;
	}

}
