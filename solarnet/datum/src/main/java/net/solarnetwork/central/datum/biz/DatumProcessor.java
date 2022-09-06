/* ==================================================================
 * DatumProcessor.java - 28/02/2020 2:27:01 pm
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

package net.solarnetwork.central.datum.biz;

import java.util.Collections;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumPK;
import net.solarnetwork.domain.Identity;
import net.solarnetwork.domain.datum.Aggregation;

/**
 * A general-purpose API for processing {@link GeneralNodeDatum} instances.
 * 
 * @author matt
 * @version 1.1
 */
public interface DatumProcessor {

	/**
	 * Test if the processor is configured and ready for processing.
	 * 
	 * @return {@literal true} if configured and ready for processing, or
	 *         {@literal false} if calls to
	 *         {@link #processDatumCollection(Iterable, Aggregation)} will not
	 *         be handled
	 */
	boolean isConfigured();

	/**
	 * Process a collection of datum.
	 * 
	 * @param datum
	 *        the datum to process
	 * @param aggregation
	 *        an optional aggregation level the {@code datum} represent
	 * @return {@literal true} if the processing was handled successfully
	 */
	boolean processDatumCollection(Iterable<? extends Identity<GeneralNodeDatumPK>> datum,
			Aggregation aggregation);

	/**
	 * Process a single datum.
	 * 
	 * @param datum
	 *        the datum to process
	 * @param aggregation
	 *        an optional aggregation level the {@code datum} represent
	 * @return {@literal true} if the processing was handled successfully
	 */
	default boolean processDatum(Identity<GeneralNodeDatumPK> datum, Aggregation aggregation) {
		return processDatumCollection(Collections.singleton(datum), aggregation);
	}

	/**
	 * Process a single datum.
	 * 
	 * <p>
	 * This passes {@link Aggregation#None} to
	 * {@link #processDatumCollection(Iterable, Aggregation)}.
	 * </p>
	 * 
	 * @param datum
	 *        the datum to process
	 * @return {@literal true} if the processing was handled successfully
	 */
	default boolean processDatum(Identity<GeneralNodeDatumPK> datum) {
		return processDatumCollection(Collections.singleton(datum), Aggregation.None);
	}
}
