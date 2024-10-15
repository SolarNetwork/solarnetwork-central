/* ==================================================================
 * BasicCloudDatumStreamQueryResult.java - 15/10/2024 1:56:45 pm
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

import java.util.Collections;
import java.util.Iterator;
import java.util.SequencedCollection;
import java.util.Spliterator;
import java.util.function.Consumer;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import net.solarnetwork.domain.datum.Datum;

/**
 * Basic implementation of {@link CloudDatumStreamQueryResult}.
 *
 * @author matt
 * @version 1.0
 */
@JsonIgnoreProperties({ "empty" })
@JsonPropertyOrder({ "returnedResultCount", "nextQueryFilter", "results" })
public class BasicCloudDatumStreamQueryResult implements CloudDatumStreamQueryResult {

	private final CloudDatumStreamQueryFilter nextQueryFilter;
	private final SequencedCollection<Datum> results;

	/**
	 * Constructor.
	 *
	 * @param results
	 *        the results, or {@literal null}
	 */
	public BasicCloudDatumStreamQueryResult(SequencedCollection<Datum> results) {
		this(null, results);
	}

	/**
	 * Constructor.
	 *
	 * @param nextQueryFilter
	 *        the next query filter, or {@literal null}
	 * @param results
	 *        the results, or {@literal null}
	 */
	public BasicCloudDatumStreamQueryResult(CloudDatumStreamQueryFilter nextQueryFilter,
			SequencedCollection<Datum> results) {
		super();
		this.nextQueryFilter = nextQueryFilter;
		this.results = (results != null ? results : Collections.emptyList());
	}

	@Override
	public Iterator<Datum> iterator() {
		return results.iterator();
	}

	@Override
	public void forEach(Consumer<? super Datum> action) {
		results.forEach(action);
	}

	@Override
	public Spliterator<Datum> spliterator() {
		return results.spliterator();
	}

	@Override
	public final SequencedCollection<Datum> getResults() {
		return results;
	}

	@Override
	public final CloudDatumStreamQueryFilter getNextQueryFilter() {
		return nextQueryFilter;
	}

}
