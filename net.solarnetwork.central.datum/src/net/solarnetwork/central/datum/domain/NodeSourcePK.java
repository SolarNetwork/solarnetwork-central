/* ==================================================================
 * NodeSourcePK.java - Oct 3, 2014 6:47:25 AM
 * 
 * Copyright 2007-2014 SolarNetwork.net Dev Team
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

import java.io.Serializable;

/**
 * Primary key based on a node ID and source ID.
 * 
 * @author matt
 * @version 1.1
 */
public class NodeSourcePK extends BasicNodeSourcePK
		implements Serializable, Cloneable, Comparable<NodeSourcePK> {

	private static final long serialVersionUID = 959344239925688873L;

	/**
	 * Default constructor.
	 */
	public NodeSourcePK() {
		super();
	}

	/**
	 * Construct with values.
	 * 
	 * @param nodeId
	 *        the node ID
	 * @param sourceId
	 *        the source ID
	 */
	public NodeSourcePK(Long nodeId, String sourceId) {
		super(nodeId, sourceId);
	}

	/**
	 * Compare two {@code NodeSourcePK} objects. Keys are ordered based on:
	 * 
	 * <ol>
	 * <li>nodeId</li>
	 * <li>sourceId</li>
	 * </ol>
	 * 
	 * <em>Null</em> values will be sorted before non-<em>null</em> values.
	 */
	@Override
	public int compareTo(NodeSourcePK o) {
		return super.compareTo(o);
	}

}
