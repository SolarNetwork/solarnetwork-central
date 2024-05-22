/* ==================================================================
 * JdbcNodeServiceAuditorCount.java - 21/01/2023 6:15:01 pm
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

package net.solarnetwork.central.common.dao.jdbc;

/**
 * Statistics for JDBC node service audit processing.
 * 
 * @author matt
 * @version 1.1
 */
public enum JdbcNodeServiceAuditorCount {

	/** Results added. */
	ResultsAdded,

	/** Write threads started. */
	WriterThreadsStarted,

	/** Write threads ended. */
	WriterThreadsEnded,

	/** JDBC connections created. */
	ConnectionsCreated,

	/** Flushed counts to DB. */
	CountsFlushed,

	/** Zero-valued counts cleared. */
	ZeroCountsCleared,

	/** SQL updates executed. */
	UpdatesExecuted,

	/** SQL updates failed. */
	UpdatesFailed,

	/** Results re-added (from errors). */
	ResultsReadded,

	;

}
