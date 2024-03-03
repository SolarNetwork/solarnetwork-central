/* ==================================================================
 * GeneralLocationDatumPK.java - Oct 17, 2014 12:07:05 PM
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
import net.solarnetwork.domain.datum.ObjectDatumKind;

/**
 * Primary key for a general location datum.
 *
 * @author matt
 * @version 2.1
 */
public class GeneralLocationDatumPK extends BasicLocationSourceDatePK
		implements Serializable, Cloneable, Comparable<GeneralLocationDatumPK>, GeneralObjectDatumKey {

	private static final long serialVersionUID = 8981870788775613402L;

	/**
	 * Default constructor.
	 */
	public GeneralLocationDatumPK() {
		super();
	}

	/**
	 * Constructor.
	 *
	 * @param locationId
	 *        the location ID
	 * @param created
	 *        the creation date
	 * @param sourceId
	 *        the source ID
	 * @since 1.3
	 */
	public GeneralLocationDatumPK(Long locationId, Instant created, String sourceId) {
		super(locationId, sourceId, created);
	}

	/**
	 * Compare two {@code GeneralLocationDautumPK} objects.
	 *
	 * <p>
	 * Keys are ordered based on:
	 * </p>
	 *
	 * <ol>
	 * <li>locationId</li>
	 * <li>sourceId</li>
	 * <li>created</li>
	 * </ol>
	 *
	 * {@literal null} values will be sorted before non-{@literal null} values.
	 */
	@Override
	public int compareTo(GeneralLocationDatumPK o) {
		return super.compareTo(o);
	}

	@Override
	public ObjectDatumKind getKind() {
		return ObjectDatumKind.Location;
	}

	@Override
	public Long getObjectId() {
		return getLocationId();
	}

	@Override
	public Instant getTimestamp() {
		return getCreated();
	}

}
