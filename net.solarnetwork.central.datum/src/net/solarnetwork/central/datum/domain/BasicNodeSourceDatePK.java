/* ==================================================================
 * BasicNodeSourceDatePK.java - 11/04/2019 9:47:22 am
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

import java.io.Serializable;
import org.joda.time.DateTime;

/**
 * Basic primary key composed of a node ID and source ID and date.
 * 
 * @author matt
 * @version 1.0
 * @since 1.39
 */
public class BasicNodeSourceDatePK extends BasicNodeSourcePK implements Serializable, Cloneable {

	private static final long serialVersionUID = -772542534219722588L;

	private DateTime created;

	/**
	 * Default constructor.
	 */
	public BasicNodeSourceDatePK() {
		super();
	}

	/**
	 * Constructor.
	 * 
	 * @param nodeId
	 *        the node ID
	 * @param sourceId
	 *        the source ID
	 * @param created
	 *        the creation date
	 */
	public BasicNodeSourceDatePK(Long nodeId, String sourceId, DateTime created) {
		super();
		setNodeId(nodeId);
		setSourceId(sourceId);
		setCreated(created);
	}

	@Override
	protected void populateIdValue(StringBuilder buf) {
		super.populateIdValue(buf);
		buf.append(";c=");
		if ( created != null ) {
			buf.append(created);
		}
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
	public int compareTo(BasicNodeSourceDatePK o) {
		int comparison = super.compareTo(o);
		if ( comparison != 0 ) {
			return comparison;
		}
		if ( o.created == null ) {
			return 1;
		} else if ( created == null ) {
			return -1;
		}
		return created.compareTo(o.created);
	}

	/**
	 * Populate a string builder with a friendly string value.
	 * 
	 * <p>
	 * This method is called from {@link #toString()}. Extending classes can add
	 * more data as necessary. The buffer will be initially empty when invoked.
	 * </p>
	 * 
	 * @param buf
	 *        the buffer to populate
	 */
	@Override
	protected void populateStringValue(StringBuilder buf) {
		super.populateStringValue(buf);
		if ( created != null ) {
			if ( buf.length() > 0 ) {
				buf.append(", ");
			}
			buf.append("created=");
			buf.append(created);
		}
	}

	public DateTime getCreated() {
		return created;
	}

	public void setCreated(DateTime created) {
		this.created = created;
	}

}
