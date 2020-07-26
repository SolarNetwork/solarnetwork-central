/* ==================================================================
 * VersionedMessageDaoMessageSource.java - 25/07/2020 10:06:54 AM
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

package net.solarnetwork.central.support;

import java.text.MessageFormat;
import java.time.Instant;
import java.util.Locale;
import java.util.Properties;
import javax.cache.Cache;
import org.springframework.context.MessageSource;
import org.springframework.context.support.AbstractMessageSource;
import net.solarnetwork.central.dao.VersionedMessageDao;
import net.solarnetwork.central.dao.VersionedMessageDao.VersionedMessages;

/**
 * {@link MessageSource} implementation that uses a {@link VersionedMessageDao}
 * to load messages.
 * 
 * @author matt
 * @version 1.0
 * @since 2.6
 */
public class VersionedMessageDaoMessageSource extends AbstractMessageSource {

	private final VersionedMessageDao dao;
	private final String[] bundleNames;
	private final Instant version;
	private final Cache<String, VersionedMessages> cache;

	/**
	 * Constructor.
	 * 
	 * @param dao
	 *        the DAO to use
	 * @param bundleNames
	 *        the bundle names
	 * @param version
	 *        the desired version; if {@literal null} the system date will be
	 *        used
	 * @param cache
	 *        the optional cache
	 * @throws IllegalArgumentException
	 *         if {@code dao} or {@code bundleNames} are {@literal null}
	 */
	public VersionedMessageDaoMessageSource(VersionedMessageDao dao, String[] bundleNames,
			Instant version, Cache<String, VersionedMessages> cache) {
		super();
		if ( dao == null ) {
			throw new IllegalArgumentException("The dao argument must be provided.");
		}
		this.dao = dao;
		if ( bundleNames == null ) {
			throw new IllegalArgumentException("The bundleNames argument must be provided.");
		}
		this.bundleNames = bundleNames;

		this.version = (version != null ? version : Instant.now());
		this.cache = cache;
	}

	@Override
	protected MessageFormat resolveCode(String code, Locale locale) {
		Properties props = propertiesForLocale(locale);
		if ( props == null ) {
			return null;
		}

		String msg = props.getProperty(code);
		if ( msg == null ) {
			return null;
		}
		return new MessageFormat(msg, locale);
	}

	private String cacheKey(String locale) {
		StringBuilder buf = new StringBuilder();
		for ( String bundleName : bundleNames ) {
			buf.append(bundleName).append('.');
		}
		buf.append(locale).append('.').append(version.toEpochMilli());
		return buf.toString();
	}

	/**
	 * Get a {@link Properties} object of all available messages for a given
	 * locale.
	 * 
	 * @param locale
	 *        the locale of the messages to get
	 * @return the properties or {@literal null} if none available
	 */
	public Properties propertiesForLocale(Locale locale) {
		final String origLocaleCode = locale.toString();

		// try full version first
		Properties props = getPropsForLocale(origLocaleCode);

		// try lang/country
		if ( props == null && locale.getLanguage() != null && locale.getCountry() != null ) {
			String localeCode = locale.getLanguage() + "_" + locale.getCountry();
			if ( !localeCode.equals(origLocaleCode) ) {
				props = getPropsForLocale(localeCode);
			}
		}

		// try lang
		if ( props == null && locale.getLanguage() != null ) {
			String localeCode = locale.getLanguage();
			if ( !localeCode.equals(origLocaleCode) ) {
				props = getPropsForLocale(localeCode);
			}
		}

		return props;
	}

	private Properties getPropsForLocale(String locale) {
		// check cache first
		VersionedMessages msgs = null;
		String cacheKey = null;
		if ( cache != null ) {
			cacheKey = cacheKey(locale);
			msgs = cache.get(cacheKey);
		}
		if ( msgs == null ) {
			// try DAO
			Properties props = dao.findMessages(version, bundleNames, locale);

			// if not available, still cache the results
			if ( props == null ) {
				props = new Properties();
			}

			msgs = new VersionedMessages(version, bundleNames, locale, props);

			if ( cache != null ) {
				cache.putIfAbsent(cacheKey, msgs);
			}
		}
		return (msgs.getProperties().isEmpty() ? null : msgs.getProperties());
	}

}
