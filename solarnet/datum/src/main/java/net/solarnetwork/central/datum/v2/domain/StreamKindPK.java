/* ==================================================================
 * StreamKindPK.java - 7/11/2020 7:09:54 am
 *
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.v2.domain;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Primary key for a stream associated with a string qualifier "kind".
 *
 * @author matt
 * @version 1.0
 * @since 2.8
 */
public class StreamKindPK extends StreamPK implements Serializable, Cloneable, Comparable<StreamKindPK> {

	@Serial
	private static final long serialVersionUID = 9078757219353810241L;

	private final String kind;

	/**
	 * Constructor.
	 *
	 * @param streamId
	 *        the stream ID
	 * @param timestamp
	 *        the time stamp
	 * @param kind
	 *        the kind
	 */
	public StreamKindPK(UUID streamId, Instant timestamp, String kind) {
		super(streamId, timestamp);
		this.kind = kind;
	}

	@Override
	public StreamKindPK clone() {
		return (StreamKindPK) super.clone();
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
			if ( !buf.isEmpty() ) {
				buf.append(", ");
			}
			buf.append("kind=");
			buf.append(kind);
		}
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
		if ( !super.equals(obj) || !(obj instanceof StreamKindPK other) ) {
			return false;
		}
		return Objects.equals(kind, other.kind);
	}

	@SuppressWarnings("ReferenceEquality")
	@Override
	public int compareTo(StreamKindPK o) {
		int result = super.compareWith(o);
		if ( result != 0 ) {
			return result;
		}
		if ( kind == o.kind ) {
			return 0;
		} else if ( kind == null ) {
			return 1;
		} else if ( o.kind == null ) {
			return -1;
		}
		return kind.compareTo(o.kind);
	}

	/**
	 * Get the kind qualifier.
	 *
	 * @return the kind
	 */
	public String getKind() {
		return kind;
	}
}
