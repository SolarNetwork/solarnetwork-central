/* ==================================================================
 * KillbillCatalogMessageSource.java - 31/08/2017 6:20:30 AM
 * 
 * Copyright 2017 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.billing.killbill;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.Properties;
import javax.cache.Cache;
import org.springframework.context.support.AbstractMessageSource;

/**
 * Resolve Killbill catalog messages.
 * 
 * @author matt
 * @version 1.0
 */
public class KillbillCatalogMessageSource extends AbstractMessageSource {

	private final KillbillClient client;
	private final Cache<String, Properties> cache;

	public KillbillCatalogMessageSource(KillbillClient client, Cache<String, Properties> cache) {
		super();
		this.client = client;
		this.cache = cache;
	}

	@Override
	protected MessageFormat resolveCode(String code, Locale locale) {
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

		if ( props == null ) {
			return null;
		}

		String msg = props.getProperty(code);
		if ( msg == null ) {
			return null;
		}
		return new MessageFormat(msg, locale);
	}

	private Properties getPropsForLocale(String locale) {
		// check cache first
		Properties props = cache.get(locale);
		if ( props == null ) {
			// try server
			props = client.invoiceCatalogTranslation(locale);

			// if not available, still cache the results
			if ( props == null ) {
				props = new Properties();
			}
			cache.putIfAbsent(locale, props);
		}
		return (props.isEmpty() ? null : props);
	}

}
