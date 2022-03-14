/* ==================================================================
 * CsvVersionedMessageDao.java - 31/05/2021 12:03:54 PM
 * 
 * Copyright 2021 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.billing.snf.util;

import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.springframework.core.io.Resource;
import org.supercsv.io.CsvListReader;
import org.supercsv.io.ICsvListReader;
import org.supercsv.prefs.CsvPreference;
import net.solarnetwork.central.dao.VersionedMessageDao;

/**
 * Implementation of {@link VersionedMessageDao} that reads from CSV resources.
 * 
 * <p>
 * Load CSV with the following columns:
 * </p>
 * 
 * <ol>
 * <li>timestamp</li>
 * <li>bundle name</li>
 * <li>lang</li>
 * <li>template name</li>
 * <li>template body</li>
 * </ol>
 * 
 * @author matt
 * @version 1.0
 */
public class CsvVersionedMessageDao implements VersionedMessageDao {

	/**
	 * The timestamp formatter to use.
	 */
	public static final DateTimeFormatter TIMESTAMP_FORMATTER;
	static {
		// @formatter:off
		TIMESTAMP_FORMATTER = new DateTimeFormatterBuilder()
				.append(DateTimeFormatter.ISO_DATE)
				.optionalStart()
				.appendLiteral(' ')
				.append(DateTimeFormatter.ISO_LOCAL_TIME)
				.appendOffset("+HH", "+00")
				.toFormatter();
		// @formatter:on
	}

	private final Iterable<Resource> resources;

	/**
	 * Constructor.
	 * 
	 * @param resources
	 *        the set of CSV resources to load
	 * @throws IllegalArgumentException
	 *         if {@code resources} is {@literal null}
	 */
	public CsvVersionedMessageDao(Iterable<Resource> resources) {
		super();
		if ( resources == null ) {
			throw new IllegalArgumentException("The resources argument must not be null.");
		}
		this.resources = resources;
	}

	private static final class Row {

		private final Instant version;
		private final String name;
		private final String template;

		public Row(Instant version, String name, String template) {
			super();
			this.version = version;
			this.name = name;
			this.template = template;
		}
	}

	@Override
	public Properties findMessages(Instant version, String[] bundleNames, String locale) {
		Map<String, Row> rows = new LinkedHashMap<>(64);
		for ( Resource r : resources ) {
			try (ICsvListReader reader = new CsvListReader(
					new InputStreamReader(r.getInputStream(), "UTF-8"),
					CsvPreference.STANDARD_PREFERENCE)) {
				List<String> list = null;
				while ( (list = reader.read()) != null ) {
					Instant ts = TIMESTAMP_FORMATTER.parse(list.get(0), Instant::from);
					if ( ts.isAfter(version) ) {
						continue;
					}
					String lang = list.get(2);
					if ( !lang.equals(locale) ) {
						continue;
					}
					String bundleName = list.get(1);
					boolean bundleMatches = false;
					if ( bundleNames != null ) {
						for ( String b : bundleNames ) {
							if ( b.equals(bundleName) ) {
								bundleMatches = true;
								break;
							}
						}
					}
					if ( !bundleMatches ) {
						continue;
					}
					String name = list.get(3);
					String template = list.get(4);
					rows.compute(name, (k, curr) -> {
						if ( curr == null || curr.version.isBefore(ts) ) {
							return new Row(ts, name, template);
						}
						return curr;
					});
				}
			} catch ( IOException e ) {
				throw new RuntimeException(e);
			}
		}
		Properties p = new Properties();
		for ( Row r : rows.values() ) {
			p.put(r.name, r.template);
		}
		return p;
	}

}
