/* ==================================================================
 * DatumSamplesMetadataExpressionRoot.java - 12/11/2024 5:26:33 pm
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

package net.solarnetwork.central.datum.domain;

import java.util.Map;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.domain.datum.DatumMetadataOperations;
import net.solarnetwork.domain.datum.DatumSamplesExpressionRoot;
import net.solarnetwork.domain.datum.DatumSamplesOperations;

/**
 * Extension of {@link DatumSamplesMetadataExpressionRoot} that adds support for
 * {@link DatumMetadataOperations}.
 *
 * @author matt
 * @version 1.0
 */
public class DatumSamplesMetadataExpressionRoot extends DatumSamplesExpressionRoot {

	private final DatumMetadataOperations metadata;

	/**
	 * Constructor.
	 *
	 * @param datum
	 *        the datum currently being populated
	 * @param sample
	 *        the samples
	 * @param parameters
	 *        the parameters
	 * @param metadata
	 *        the metadata
	 */
	public DatumSamplesMetadataExpressionRoot(Datum datum, DatumSamplesOperations sample,
			Map<String, ?> parameters, DatumMetadataOperations metadata) {
		super(datum, sample, parameters);
		this.metadata = metadata;
	}

	/**
	 * Get the general metadata.
	 *
	 * @return the general metadata, or {@literal null} if none available
	 */
	public DatumMetadataOperations metadata() {
		return metadata;
	}

}
