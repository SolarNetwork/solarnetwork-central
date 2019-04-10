/* ==================================================================
 * GeneralNodeDatumKindPK.java - 11/04/2019 9:12:16 am
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
import java.util.Objects;
import org.joda.time.DateTime;

/**
 * A primary key based on a node, source, date, and "kind" flag.
 * 
 * @author matt
 * @version 1.0
 * @since 1.39
 */
public class GeneralNodeDatumKindPK extends BasicNodeSourceDatePK
		implements Serializable, Cloneable, Comparable<GeneralNodeDatumKindPK> {

	private static final long serialVersionUID = -3510872010302073368L;

	private String kind;

	/**
	 * Default constructor.
	 */
	public GeneralNodeDatumKindPK() {
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
	 * @param kind
	 *        the kind
	 */
	public GeneralNodeDatumKindPK(Long nodeId, DateTime created, String sourceId, String kind) {
		super(nodeId, sourceId, created);
		setKind(kind);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(kind);
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
		if ( !(obj instanceof GeneralNodeDatumKindPK) ) {
			return false;
		}
		GeneralNodeDatumKindPK other = (GeneralNodeDatumKindPK) obj;
		return kind == other.kind;
	}

	@Override
	protected void populateIdValue(StringBuilder buf) {
		super.populateIdValue(buf);
		buf.append(";k=");
		if ( kind != null ) {
			buf.append(kind);
		}
	}

	@Override
	protected void populateStringValue(StringBuilder buf) {
		super.populateStringValue(buf);
		if ( kind != null ) {
			if ( buf.length() > 0 ) {
				buf.append(", ");
			}
			buf.append("kind=").append(kind);
		}
	}

	@Override
	public int compareTo(GeneralNodeDatumKindPK o) {
		int result = super.compareTo(o);
		if ( result != 0 ) {
			return result;
		}
		if ( o.kind == null ) {
			return 1;
		} else if ( kind == null ) {
			return -1;
		}
		return kind.compareTo(o.kind);
	}

	public String getKind() {
		return kind;
	}

	public void setKind(String kind) {
		this.kind = kind;
	}

}
