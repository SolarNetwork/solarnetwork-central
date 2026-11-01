/* ==================================================================
 * CombinedAggregateDatumEntity.java - 2/11/2026 8:12:04 am
 *
 * Copyright 2026 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.v2.dao;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.Serial;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import net.solarnetwork.central.datum.domain.CombinedDatumContributors;
import net.solarnetwork.central.datum.domain.NodeSourcePK;
import net.solarnetwork.domain.datum.Aggregation;
import net.solarnetwork.domain.datum.DatumProperties;
import net.solarnetwork.domain.datum.DatumPropertiesStatistics;

/**
 * An aggregate datum on a virtual stream, combined from the listed real
 * streams.
 *
 * @author matt
 * @version 1.0
 */
public class CombinedAggregateDatumEntity extends AggregateDatumEntity
		implements CombinedDatumContributors {

	@Serial
	private static final long serialVersionUID = -2914416132904651378L;

	private final Set<NodeSourcePK> contributors;

	/**
	 * Constructor.
	 *
	 * @param streamId
	 *        the stream ID
	 * @param timestamp
	 *        the timestamp
	 * @param aggregation
	 *        the aggregation
	 * @param properties
	 *        the samples
	 * @param statistics
	 *        the statistics
	 * @param contributors
	 *        the contributing streams
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public CombinedAggregateDatumEntity(UUID streamId, Instant timestamp, Aggregation aggregation,
			DatumProperties properties, DatumPropertiesStatistics statistics,
			Set<NodeSourcePK> contributors) {
		super(streamId, timestamp, aggregation, properties, statistics);
		this.contributors = requireNonNullArgument(contributors, "contributors");
	}

	@Override
	public CombinedAggregateDatumEntity clone() {
		return (CombinedAggregateDatumEntity) super.clone();
	}

	@Override
	public Set<NodeSourcePK> contributors() {
		return contributors;
	}

}
