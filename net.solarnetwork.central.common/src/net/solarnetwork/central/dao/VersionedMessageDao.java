/* ==================================================================
 * VersionedMessageDao.java - 25/07/2020 9:48:13 AM
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

package net.solarnetwork.central.dao;

import java.io.Serializable;
import java.time.Instant;
import java.util.Properties;

/**
 * DAO API for a resource bundle-like set of messages.
 * 
 * @author matt
 * @version 1.0
 * @since 2.6
 */
public interface VersionedMessageDao {

	/**
	 * Messages for a set of bundle names and locale, at a specific version.
	 */
	class VersionedMessages implements Serializable {

		private static final long serialVersionUID = -6102062987223847054L;

		private final Instant version;
		private final String[] bundleNames;
		private final String locale;
		private final Properties properties;

		/**
		 * Constructor.
		 * 
		 * @param version
		 *        the version
		 * @param bundleNames
		 *        the bundle names
		 * @param locale
		 *        the locale
		 * @param properties
		 *        the properties
		 * @throws IllegalArgumentException
		 *         if any argument is {@literal null}
		 */
		public VersionedMessages(Instant version, String[] bundleNames, String locale,
				Properties properties) {
			super();
			if ( version == null ) {
				throw new IllegalArgumentException("The version argument must be provided.");
			}
			this.version = version;
			if ( bundleNames == null ) {
				throw new IllegalArgumentException("The bundleNames argument must be provided.");
			}
			this.bundleNames = bundleNames;
			if ( locale == null ) {
				throw new IllegalArgumentException("The locale argument must be provided.");
			}
			this.locale = locale;
			if ( properties == null ) {
				throw new IllegalArgumentException("The properties argument must be provided.");
			}
			this.properties = properties;
		}

		/**
		 * Get the version of these messages.
		 * 
		 * @return the version, never {@literal null}
		 */
		public Instant getVersion() {
			return version;
		}

		/**
		 * Get the bundle names associated with these messages.
		 * 
		 * @return the bundle names, never {@literal null}
		 */
		public String[] getBundleNames() {
			return bundleNames;
		}

		/**
		 * Get the locale associated with these messages.
		 * 
		 * @return the locale, never {@literal null}
		 */
		public String getLocale() {
			return locale;
		}

		/**
		 * Get the messages.
		 * 
		 * @return the messages, never {@literal null}
		 */
		public Properties getProperties() {
			return properties;
		}

	}

	/**
	 * Get all available messages for a specific version.
	 * 
	 * @param version
	 * @param bundleNames
	 * @param locale
	 * @return
	 */
	Properties findMessages(Instant version, String[] bundleNames, String locale);

}
