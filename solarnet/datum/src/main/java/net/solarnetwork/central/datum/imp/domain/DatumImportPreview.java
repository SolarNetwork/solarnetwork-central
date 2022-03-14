/* ==================================================================
 * DatumImportPreview.java - 10/11/2018 9:52:52 AM
 * 
 * Copyright 2018 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.imp.domain;

import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.support.BasicFilterResults;

/**
 * Preview of datum import result datum.
 * 
 * @author matt
 * @version 1.0
 */
public class DatumImportPreview extends BasicFilterResults<GeneralNodeDatum> {

	/**
	 * Constructor.
	 * 
	 * @param results
	 *        the parsed preview datum
	 * @param totalResults
	 *        the total results (if known)
	 * @param startingOffset
	 *        the starting offset
	 * @param returnedResultCount
	 *        the number of items in {@code results}
	 */
	public DatumImportPreview(Iterable<GeneralNodeDatum> results, Long totalResults,
			Integer startingOffset, Integer returnedResultCount) {
		super(results, totalResults, startingOffset, returnedResultCount);
	}

}
