/* ==================================================================
 * ChargeSessionFilter.java - 10/12/2022 10:31:46 am
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

import net.solarnetwork.central.common.dao.UserCriteria;
import net.solarnetwork.dao.DateRangeCriteria;
import net.solarnetwork.dao.PaginationCriteria;
import net.solarnetwork.dao.SortCriteria;

/**
 * Filter criteria for charge sessions.
 * 
 * @author matt
 * @version 1.0
 */
public interface ChargeSessionFilter extends ChargeSessionCriteria, ChargeSessionTransactionCriteria,
		ChargeSessionEndReasonCriteria, DateRangeCriteria, SortCriteria, PaginationCriteria,
		UserCriteria, ChargePointCriteria, ChargePointConnectorCriteria, IdentifierCriteria {

}
