/* ==================================================================
 * StreamKind.java - 5/06/2021 10:51:50 AM
 * 
 * Copyright 2021 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.in.biz.dao;

import java.io.Serializable;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumKind;

/**
 * Association of object ID and object kind.
 * 
 * @author matt
 * @version 1.0
 */
public final class StreamKind implements Serializable {

	private static final long serialVersionUID = -8728780711232724835L;

	private final Long objectId;
	private final ObjectDatumKind kind;

	/**
	 * Constructor.
	 * 
	 * @param objectId
	 *        the object ID
	 * @param kind
	 *        the object kind
	 */
	public StreamKind(Long objectId, ObjectDatumKind kind) {
		super();
		this.objectId = objectId;
		this.kind = kind;
	}

	/**
	 * Get the object ID.
	 * 
	 * @return the objectId
	 */
	public Long getObjectId() {
		return objectId;
	}

	/**
	 * Get the object kind.
	 * 
	 * @return the kind
	 */
	public ObjectDatumKind getKind() {
		return kind;
	}

}
