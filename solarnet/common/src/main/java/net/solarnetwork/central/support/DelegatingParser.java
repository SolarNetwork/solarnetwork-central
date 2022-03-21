/* ==================================================================
 * TemporalAccessorParser.java - 22/03/2022 10:30:51 AM
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

import java.text.ParseException;
import java.util.Locale;
import org.springframework.format.Parser;
import net.solarnetwork.util.ObjectUtils;

/**
 * A parser that delegates to a set of other parsers, returning the first
 * successful result.
 * 
 * @author matt
 * @version 1.0
 */
public class DelegatingParser<T> implements Parser<T> {

	final Parser<T>[] delegates;

	/**
	 * Constructor.
	 * 
	 * @param parser
	 *        main parser
	 * @param delegates
	 *        the delegates to parse with
	 * @throws IllegalArgumentException
	 *         if {@code delegates} is {@literal null} or empty
	 */
	@SafeVarargs
	public DelegatingParser(Parser<T>... delegates) {
		super();

		this.delegates = ObjectUtils.requireNonNullArgument(delegates, "delegates");
		if ( delegates.length < 1 ) {
			throw new IllegalArgumentException("At least one delegate parser must be provided.");
		}
	}

	@Override
	public T parse(String text, Locale locale) throws ParseException {
		ParseException exception = null;
		for ( Parser<T> p : delegates ) {
			try {
				return p.parse(text, locale);
			} catch ( ParseException e ) {
				if ( exception == null ) {
					exception = e;
				}
			}
		}
		if ( exception != null ) {
			throw exception;
		}
		return null;
	}

}
