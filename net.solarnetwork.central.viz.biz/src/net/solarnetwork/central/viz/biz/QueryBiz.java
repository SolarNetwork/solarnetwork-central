/* ===================================================================
 * QueryBiz.java
 * 
 * Created Aug 5, 2009 11:39:52 AM
 * 
 * Copyright (c) 2009 Solarnetwork.net Dev Team.
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
 * ===================================================================
 * $Id: QueryBiz.java 415 2009-11-03 22:59:25Z msqr $
 * ===================================================================
 */

package net.solarnetwork.central.viz.biz;

import net.solarnetwork.central.datum.domain.NodeDatum;

import org.joda.time.ReadableInterval;

/**
 * API for querying business logic.
 *
 * @author matt
 * @version $Revision: 415 $ $Date: 2009-11-04 11:59:25 +1300 (Wed, 04 Nov 2009) $
 */
public interface QueryBiz {

	/**
	 * Get a date interval of available data for a type of NodeDatum.
	 * 
	 * <p>This method can be used to find the earliest and latest dates
	 * data is available for a set of given {@link NodeDatum} types. When
	 * more than one NodeDatum are provided, the returned interval will be the
	 * union of all ranges, that is the earliest date of all specified 
	 * NodeDatum values and the maximum date of all specified NodeDatum values. This
	 * could be useful for reporting UIs that want to display a view of
	 * the complete range of data available.</p>
	 * 
	 * @param nodeId the ID of the node to look for
	 * @param types the set of NodeDatum types to look for
	 * @return ReadableInterval instance, or <em>null</em> if no data available
	 */
	ReadableInterval getReportableInterval(Long nodeId, Class<? extends NodeDatum>[] types);
	
	/**
	 * Get a date interval of available data for a type of NodeDatum, across
	 * all nodes in the system.
	 * 
	 * <p>This method can be used to find the earliest and latest dates
	 * data is available for a set of given {@link NodeDatum} types. When
	 * more than one NodeDatum are provided, the returned interval will be the
	 * union of all ranges, that is the earliest date of all specified 
	 * NodeDatum values and the maximum date of all specified NodeDatum values. This
	 * could be useful for reporting UIs that want to display a view of
	 * the complete range of data available.</p>
	 * 
	 * @param types the set of NodeDatum types to look for
	 * @return ReadableInterval instance, or <em>null</em> if no data available
	 */
	ReadableInterval getNetworkReportableInterval(Class<? extends NodeDatum>[] types);
	
}
