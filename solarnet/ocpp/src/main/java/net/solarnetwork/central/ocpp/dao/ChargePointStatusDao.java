/* ==================================================================
 * ChargePointStatusDao.java - 17/11/2022 6:27:13 am
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

package net.solarnetwork.central.ocpp.dao;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.ocpp.domain.ChargePointStatus;
import net.solarnetwork.central.support.FilteredResultsProcessor;
import net.solarnetwork.dao.FilterableDao;
import net.solarnetwork.domain.SortDescriptor;

/**
 * DAO API for {@link ChargePointStatus} entities.
 * 
 * @author matt
 * @version 1.0
 */
public interface ChargePointStatusDao
		extends FilterableDao<ChargePointStatus, UserLongCompositePK, ChargePointStatusFilter> {

	/**
	 * Update a specific charge point connection status.
	 * 
	 * <p>
	 * This method will create a new entity if one does not already exist.
	 * </p>
	 * 
	 * @param userId
	 *        the user ID
	 * @param chargePointIdentifier
	 *        the charge point identifier
	 * @param connectedTo
	 *        the name of the SolarIn instance the charger is connected to, or
	 *        {@literal null} if disconnected
	 * @param connectionDate
	 *        if {@code connectedTo} is not {@literal null} the connected
	 *        timestamp to save
	 */
	void updateConnectionStatus(Long userId, String chargePointIdentifier, String connectedTo,
			Instant connectionDate);

	/**
	 * Update a specific charge point connection status.
	 * 
	 * <p>
	 * This method will create a new entity if one does not already exist.
	 * </p>
	 * 
	 * @param id
	 *        the charge point ID
	 * @param connectedTo
	 *        the name of the SolarIn instance the charger is connected to, or
	 *        {@literal null} if disconnected
	 * @param connectionDate
	 *        if {@code connectedTo} is not {@literal null} the connected
	 *        timestamp to save
	 */
	void updateConnectionStatus(UserLongCompositePK id, String connectedTo, Instant connectionDate);

	/**
	 * API for querying for a stream of {@link ChargePointStatus}.
	 * 
	 * @param filter
	 *        the filter
	 * @param processor
	 *        the stream processor
	 * @param sortDescriptors
	 *        the optional sort descriptors
	 * @param offset
	 *        the optional starting offset
	 * @param max
	 *        the optional maximum result count
	 * @throws IOException
	 *         if any IO error occurs
	 */
	void findFilteredStream(ChargePointStatusFilter filter,
			FilteredResultsProcessor<ChargePointStatus> processor, List<SortDescriptor> sortDescriptors,
			Integer offset, Integer max) throws IOException;

}
