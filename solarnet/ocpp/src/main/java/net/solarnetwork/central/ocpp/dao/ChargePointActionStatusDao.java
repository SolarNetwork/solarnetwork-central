/* ==================================================================
 * ChargePointActionStatusDao.java - 16/11/2022 5:27:37 pm
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
import net.solarnetwork.central.ocpp.domain.ChargePointActionStatus;
import net.solarnetwork.central.ocpp.domain.ChargePointActionStatusKey;
import net.solarnetwork.central.support.FilteredResultsProcessor;
import net.solarnetwork.dao.FilterableDao;
import net.solarnetwork.domain.SortDescriptor;

/**
 * DAO API for {@link ChargePointActionStatus} entities.
 * 
 * @author matt
 * @version 1.0
 */
public interface ChargePointActionStatusDao extends
		FilterableDao<ChargePointActionStatus, ChargePointActionStatusKey, ChargePointActionStatusFilter> {

	/**
	 * Update the timestamp for a specific charge point action.
	 * 
	 * <p>
	 * This method will create a new entity if one does not already exist.
	 * </p>
	 * 
	 * @param userId
	 *        the user ID
	 * @param chargePointIdentifier
	 *        the charge point identifier
	 * @param connectorId
	 *        the connector ID the message is related to, or {@literal null} or
	 *        {@literal 0} for charger-wide actions
	 * @param action
	 *        the action name
	 * @param date
	 *        the date
	 * @throws IllegalArgumentException
	 *         if any argument other than {@code connectorId} is {@literal null}
	 */
	void updateActionTimestamp(Long userId, String chargePointIdentifier, Integer connectorId,
			String action, Instant date);

	/**
	 * API for querying for a stream of {@link ChargePointActionStatus}.
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
	void findFilteredStream(ChargePointActionStatusFilter filter,
			FilteredResultsProcessor<ChargePointActionStatus> processor,
			List<SortDescriptor> sortDescriptors, Integer offset, Integer max) throws IOException;

}
