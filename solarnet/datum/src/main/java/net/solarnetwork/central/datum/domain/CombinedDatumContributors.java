/* ==================================================================
 * CombinedDatumContributors.java - 2/11/2026 8:12:04 am
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

package net.solarnetwork.central.datum.domain;

import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * API for a datum that can list the real node streams that contributed to it,
 * when it is the result of combining streams into a virtual stream.
 *
 * @author matt
 * @version 1.0
 */
public interface CombinedDatumContributors {

	/**
	 * Get the streams that contributed to this datum.
	 *
	 * @return the contributing streams, which must not be modified; empty if no
	 *         stream contributed, or {@code null} if the datum is not combined
	 */
	@Nullable
	Set<NodeSourcePK> contributors();

}
