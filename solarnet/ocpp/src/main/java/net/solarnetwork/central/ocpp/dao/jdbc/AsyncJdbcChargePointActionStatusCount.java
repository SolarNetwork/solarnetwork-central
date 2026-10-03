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

/**
 * Statistics for asynchronous JDBC charge point action status processing.
 * 
 * @author matt
 * @version 1.2
 */
public enum AsyncJdbcChargePointActionStatusCount {

	/** Results added. */
	ResultsAdded,

	/** Results removed. */
	ResultsRemoved,

	/** Writer threads started. */
	WriterThreadsStarted,

	/** Writer threads ended. */
	WriterThreadsEnded,

	/** JDBC connections created. */
	ConnectionsCreated,

	/** SQL updates executed. */
	UpdatesExecuted,

	/** SQL updates failed. */
	UpdatesFailed,

	/**
	 * Results replaced by a later result for the same charge point action,
	 * before being written.
	 *
	 * @since 1.2
	 */
	ResultsReplaced,

	/**
	 * Results added back to the queue after failing to be written, to try
	 * again after reconnecting.
	 *
	 * @since 1.2
	 */
	ResultsReadded,

	/**
	 * Results discarded after failing to be written, for a reason that trying
	 * again would not fix.
	 *
	 * @since 1.2
	 */
	ResultsDiscarded,

	;

}
