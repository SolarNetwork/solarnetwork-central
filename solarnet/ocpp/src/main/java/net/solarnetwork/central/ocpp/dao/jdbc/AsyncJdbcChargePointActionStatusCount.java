/* ==================================================================
 * AsyncJdbcChargePointActionStatusCount.java - 11/06/2018 7:43:25 PM
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

package net.solarnetwork.central.ocpp.dao.jdbc;

import net.solarnetwork.util.StatCounter.Stat;

/**
 * Statistics for asynchronous JDBC charge point action status processing.
 * 
 * @author matt
 * @version 1.0
 */
public enum AsyncJdbcChargePointActionStatusCount implements Stat {

	ResultsAdded(0, "results added"),

	WriterThreadsStarted(1, "write threads started"),

	WriterThreadsEnded(2, "write threads ended"),

	ConnectionsCreated(3, "JDBC connections created"),

	UpdatesExecuted(4, "SQL updates executed"),

	UpdatesFailed(5, "SQL updates failed"),

	;

	private final int index;
	private final String description;

	private AsyncJdbcChargePointActionStatusCount(int index, String description) {
		this.index = index;
		this.description = description;
	}

	@Override
	public int getIndex() {
		return index;
	}

	@Override
	public String getDescription() {
		return description;
	}

}
