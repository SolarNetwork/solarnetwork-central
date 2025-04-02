/* ==================================================================
 * ServerDataPointFilter.java - 14/08/2023 4:21:32 pm
 *
 * Copyright 2023 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.dnp3.dao;

import net.solarnetwork.central.common.dao.EnabledCriteria;
import net.solarnetwork.central.common.dao.IdentifierCriteria;
import net.solarnetwork.central.common.dao.IndexCriteria;
import net.solarnetwork.central.common.dao.NodeCriteria;
import net.solarnetwork.central.common.dao.NodeOwnershipCriteria;
import net.solarnetwork.central.common.dao.SourceCriteria;
import net.solarnetwork.central.common.dao.UserCriteria;
import net.solarnetwork.dao.PaginationCriteria;

/**
 * A filter for DNP3-related server measurement and control entities.
 *
 * @author matt
 * @version 1.0
 */
public interface ServerDataPointFilter
		extends UserCriteria, ServerCriteria, IdentifierCriteria, IndexCriteria, EnabledCriteria,
		NodeOwnershipCriteria, NodeCriteria, SourceCriteria, PaginationCriteria {

}
