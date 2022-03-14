/* ==================================================================
 * SnfMatchers.java - 24/07/2020 11:07:00 AM
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

package net.solarnetwork.central.user.billing.snf.test;

import static net.solarnetwork.central.user.billing.snf.domain.TaxCodeFilter.filterFor;
import java.time.Instant;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import net.solarnetwork.central.user.billing.snf.domain.TaxCodeFilter;

/**
 * Helper matchers for unit tests.
 * 
 * @author matt
 * @version 1.0
 */
public final class SnfMatchers {

	private static class TaxCodeFilterMatcher extends TypeSafeMatcher<TaxCodeFilter> {

		private final TaxCodeFilter expected;

		public TaxCodeFilterMatcher(TaxCodeFilter expected) {
			this.expected = expected;
		}

		@Override
		public void describeTo(Description desc) {
			desc.appendText("matches filter");
			desc.appendValue(expected);

		}

		@Override
		protected boolean matchesSafely(TaxCodeFilter other) {
			return expected.isSameAs(other);
		}

	}

	/**
	 * Match a tax code filter.
	 * 
	 * @param date
	 *        the expected date
	 * @param zones
	 *        the expected zones
	 * @return the matcher
	 */
	public static Matcher<TaxCodeFilter> matchesFilter(Instant date, String... zones) {
		return new TaxCodeFilterMatcher(filterFor(date, zones));
	}

}
