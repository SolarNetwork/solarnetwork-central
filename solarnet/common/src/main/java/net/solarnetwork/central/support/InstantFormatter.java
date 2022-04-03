/* ==================================================================
 * InstantFormatter.java - 22/11/2021 11:26:18 AM
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

package net.solarnetwork.central.support;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.text.ParseException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import org.springframework.format.Formatter;

/**
 * {@link Formatter} for {@link Instant} with a configurable
 * {@link DateTimeFormatter}.
 * 
 * @author matt
 * @version 1.1
 */
public class InstantFormatter implements Formatter<Instant> {

	private final DateTimeFormatter formatter;
	private final DateTimeFormatter[] fallbackFormatters;

	/**
	 * Constructor.
	 * 
	 * @param formatter
	 *        the formatter to use
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public InstantFormatter(DateTimeFormatter formatter) {
		this(formatter, (DateTimeFormatter[]) null);
	}

	/**
	 * Constructor.
	 * 
	 * @param formatter
	 *        the formatter to use
	 * @param fallbackFormatters
	 *        optional fallback formatters to use during parsing
	 * @throws IllegalArgumentException
	 *         if {@code formatter} is {@literal null}
	 */
	public InstantFormatter(DateTimeFormatter formatter, DateTimeFormatter... fallbackFormatters) {
		super();
		this.formatter = requireNonNullArgument(formatter, "formatter");
		this.fallbackFormatters = fallbackFormatters;
	}

	@Override
	public String print(Instant object, Locale locale) {
		return formatter.format(object);
	}

	@Override
	public Instant parse(String text, Locale locale) throws ParseException {
		try {
			return formatter.parse(text, Instant::from);
		} catch ( DateTimeParseException e ) {
			if ( fallbackFormatters != null ) {
				for ( DateTimeFormatter fmt : fallbackFormatters ) {
					try {
						return fmt.parse(text, Instant::from);
					} catch ( DateTimeParseException e2 ) {
						// ignore and continue
					}
				}
			}
			throw e;
		}
	}

}
