/* ==================================================================
 * GeneralNodeDatumAuxiliaryPK.java - 1/02/2019 4:30:40 pm
 * 
 * Copyright 2019 SolarNetwork.net Dev Team
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

import java.util.Objects;
import org.joda.time.DateTime;

/**
 * Primary key for a general node datum auxiliary entity.
 * 
 * @author matt
 * @version 1.0
 * @since 1.35
 */
public class GeneralNodeDatumAuxiliaryPK extends GeneralNodeDatumPK {

	private static final long serialVersionUID = 2851055344175402929L;

	private DatumAuxiliaryType type;

	/**
	 * Default constructor.
	 */
	public GeneralNodeDatumAuxiliaryPK() {
		super();
	}

	/**
	 * Constructor.
	 * 
	 * <p>
	 * The {@link DatumAuxiliaryType#Reset} type will be set.
	 * </p>
	 * 
	 * @param nodeId
	 *        the node ID
	 * @param created
	 *        the creation date
	 * @param sourceId
	 *        the source ID
	 */
	public GeneralNodeDatumAuxiliaryPK(Long nodeId, DateTime created, String sourceId) {
		this(nodeId, created, sourceId, DatumAuxiliaryType.Reset);
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
	 * @param type
	 *        the type
	 */
	public GeneralNodeDatumAuxiliaryPK(Long nodeId, DateTime created, String sourceId,
			DatumAuxiliaryType type) {
		super(nodeId, created, sourceId);
		this.type = type;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(type);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( !super.equals(obj) ) {
			return false;
		}
		if ( !(obj instanceof GeneralNodeDatumAuxiliaryPK) ) {
			return false;
		}
		GeneralNodeDatumAuxiliaryPK other = (GeneralNodeDatumAuxiliaryPK) obj;
		return type == other.type;
	}

	@Override
	protected void populateIdValue(StringBuilder buf) {
		super.populateIdValue(buf);
		buf.append(";t=");
		if ( type != null ) {
			buf.append(type);
		}
	}

	@Override
	protected void populateStringValue(StringBuilder buf) {
		super.populateStringValue(buf);
		if ( type != null ) {
			if ( buf.length() > 0 ) {
				buf.append(", ");
			}
			buf.append("type=").append(type);
		}
	}

	@Override
	public int compareTo(GeneralNodeDatumPK o) {
		int result = super.compareTo(o);
		if ( result != 0 || !(o instanceof GeneralNodeDatumAuxiliaryPK) ) {
			return result;
		}
		GeneralNodeDatumAuxiliaryPK pk = (GeneralNodeDatumAuxiliaryPK) o;
		if ( pk.type == null ) {
			return 1;
		} else if ( type == null ) {
			return -1;
		}
		return type.compareTo(pk.type);
	}

	public DatumAuxiliaryType getType() {
		return type;
	}

	public void setType(DatumAuxiliaryType type) {
		this.type = type;
	}

}
