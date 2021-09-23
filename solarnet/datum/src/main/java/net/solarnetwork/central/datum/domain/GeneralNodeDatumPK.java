/* ==================================================================
 * GeneralNodeDatumPK.java - Aug 22, 2014 5:51:19 AM
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
import java.time.Instant;

/**
 * Primary key for a general node datum.
 * 
 * @author matt
 * @version 2.0
 */
public class GeneralNodeDatumPK extends BasicNodeSourceDatePK
		implements Serializable, Cloneable, Comparable<GeneralNodeDatumPK> {

	private static final long serialVersionUID = 2663897681819661032L;

	/**
	 * Default constructor.
	 */
	public GeneralNodeDatumPK() {
		super();
	}

	/**
	 * Constructor.
	 * 
	 * @param nodeId
	 *        the node ID
	 * @param created
	 *        the creation date
	 * @param sourceId
	 *        the source ID
	 * @since 1.3
	 */
	public GeneralNodeDatumPK(Long nodeId, Instant created, String sourceId) {
		super(nodeId, sourceId, created);
	}

	/**
	 * Compare two {@code GeneralNodeDautumPK} objects.
	 * 
	 * <p>
	 * Keys are ordered based on:
	 * </p>
	 * 
	 * <ol>
	 * <li>nodeId</li>
	 * <li>sourceId</li>
	 * <li>created</li>
	 * </ol>
	 * 
	 * {@literal null} values will be sorted before non-{@literal null} values.
	 */
	@Override
	public int compareTo(GeneralNodeDatumPK o) {
		return super.compareTo(o);
	}

}
