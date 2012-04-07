/* ===================================================================
 * NodeDatum.java
 * 
 * Created Jul 29, 2009 12:20:31 PM
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
 * $Id$
 * ===================================================================
 */

package net.solarnetwork.central.datum.domain;

/**
 * Basic node-level NodeDatum API.
 *
 * @author matt
 * @version $Revision$ $Date$
 */
public interface NodeDatum extends Datum {

	/**
	 * Get the ID of the node this datam originates from.
	 * 
	 * @return the node ID
	 */
	public Long getNodeId();
	
}
