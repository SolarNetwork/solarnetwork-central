/* ==================================================================
 * StreamDatumFilteredResultsProcessor.java - 1/05/2022 8:23:35 pm
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

package net.solarnetwork.central.datum.v2.support;

import net.solarnetwork.central.support.FilteredResultsProcessor;
import net.solarnetwork.domain.datum.StreamDatum;

/**
 * Filtered results processor for {@link StreamDatum}.
 * 
 * <p>
 * The {@link #start(Long, Integer, Integer, java.util.Map)} method must provide
 * the {@link #METADATA_PROVIDER_ATTR} attribute value.
 * </p>
 * 
 * @author matt
 * @version 1.0
 * @since 1.3
 */
public interface StreamDatumFilteredResultsProcessor extends FilteredResultsProcessor<StreamDatum> {

	/**
	 * A starting attribute for a
	 * {@link net.solarnetwork.domain.datum.ObjectDatumStreamMetadataProvider}.
	 */
	String METADATA_PROVIDER_ATTR = "ObjectDatumStreamMetadataProvider";

}
