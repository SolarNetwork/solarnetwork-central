/* ==================================================================
 * PartialAggregationInterval.java - 3/12/2020 2:56:00 pm
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

package net.solarnetwork.central.datum.v2.domain;

import static java.util.Collections.unmodifiableList;
import static net.solarnetwork.central.datum.v2.support.DatumUtils.truncateDate;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import net.solarnetwork.central.domain.Aggregation;

/**
 * A date interval for partial aggregation support.
 * 
 * @author matt
 * @version 1.0
 * @since 2.8
 */
public class PartialAggregationInterval {

	private final Aggregation main;
	private final Aggregation partial;
	private final LocalDateTime start;
	private final LocalDateTime end;
	private final List<LocalDateInterval> intervals;

	/**
	 * Constructor.
	 * 
	 * @param main
	 *        the main aggregation
	 * @param partial
	 *        the partial aggregation (must be a smaller level than
	 *        {@code main})
	 * @param start
	 *        the start date
	 * @param end
	 *        the end date
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null} or {@code partial} does not
	 *         have a smaller level than {@code main}
	 */
	public PartialAggregationInterval(Aggregation main, Aggregation partial, LocalDateTime start,
			LocalDateTime end) {
		super();
		this.main = requireNonNullArgument(main, "main");
		this.partial = requireNonNullArgument(partial, "partial");
		this.start = requireNonNullArgument(start, "start");
		this.end = requireNonNullArgument(end, "end");
		if ( !(partial.compareLevel(main) < 0) ) {
			throw new IllegalArgumentException(String.format(
					"The partial aggregation %s is not smaller than the main aggregation %s.", partial,
					main));
		}
		this.intervals = unmodifiableList(buildIntervals());
	}

	private static ChronoUnit unit(Aggregation agg) {
		final ChronoUnit field;
		switch (agg) {
			case Year:
				field = ChronoUnit.YEARS;
				break;

			case Month:
				field = ChronoUnit.MONTHS;
				break;

			case Day:
				field = ChronoUnit.DAYS;
				break;

			case Hour:
				field = ChronoUnit.HOURS;
				break;

			default:
				field = null;
				break;
		}
		return field;
	}

	private List<LocalDateInterval> buildIntervals() {
		List<LocalDateInterval> result = new ArrayList<>(3);
		LocalDateTime curr = truncateDate(start, main);
		if ( curr.isBefore(start) ) {
			curr = curr.plus(1, unit(main));
			if ( curr.isAfter(end) ) {
				curr = truncateDate(end, partial);
			}
			result.add(new LocalDateInterval(truncateDate(start, partial), curr, partial));
		}
		LocalDateTime next = truncateDate(end, main);
		if ( curr.isBefore(end) ) {
			if ( curr.isBefore(next) ) {
				result.add(new LocalDateInterval(curr, next, main));
				curr = next;
			}
			next = truncateDate(end, partial);
			if ( curr.isBefore(next) ) {
				result.add(new LocalDateInterval(curr, next, partial));
			}
		}
		return result;
	}

	/**
	 * Get a list of intervals representing the appropriate partial ranges
	 * defined by the properties of this instance.
	 * 
	 * <p>
	 * This will return up to 3 intervals, representing leading, middle, and
	 * trailing aggregate ranges with the {@code partial} and {@code main}
	 * aggregation specified appropriately.
	 * </p>
	 * 
	 * @return the list of intervals, never {@literal null}
	 */
	public List<LocalDateInterval> getIntervals() {
		return intervals;
	}

}
