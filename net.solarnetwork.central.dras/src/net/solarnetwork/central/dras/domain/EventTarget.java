/* ==================================================================
 * EventTarget.java - May 9, 2011 2:59:30 PM
 * 
 * Copyright 2007-2011 SolarNetwork.net Dev Team
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
 * $Id$
 * ==================================================================
 */

package net.solarnetwork.central.dras.domain;

import java.io.Serializable;

import org.joda.time.Duration;

/**
 * A target value for a given event at a specific point in time during the event.
 * 
 * @author matt
 * @version $Revision$
 */
public class EventTarget implements Cloneable, Serializable, Comparable<EventTarget> {

	private static final long serialVersionUID = -101510547324099994L;

	private Duration eventDateOffset;
	private Double value;
	
	/**
	 * Default constructor.
	 */
	public EventTarget() {
		super();
	}
	
	/**
	 * Construct with values.
	 * 
	 * @param offset the event date offset
	 * @param type the target type
	 * @param value the target value
	 */
	public EventTarget(Duration offset, Double value) {
		super();
		setEventDateOffset(offset);
		setValue(value);
	}
	
	@Override
	public Object clone() {
		try {
			return super.clone();
		} catch (CloneNotSupportedException e) {
			// should never get here
			throw new RuntimeException(e);
		}
	}

	/**
	 * Compare based on the eventDateOffset, with <em>null<em> values ordered before non-null values.
	 */
	@Override
	public int compareTo(EventTarget other) {
		if ( eventDateOffset == null && other.eventDateOffset == null ) {
			return 0;
		}
		if ( eventDateOffset == null ) {
			return -1;
		}
		if ( other.eventDateOffset == null ) {
			return 1;
		}
		return eventDateOffset.compareTo(other.eventDateOffset);
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((eventDateOffset == null) ? 0 : eventDateOffset.hashCode());
		return result;
	}

	/**
	 * Compare for equality, based on {@code eventId} and {@code eventDateOffset}.
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		EventTarget other = (EventTarget) obj;
		if (eventDateOffset == null) {
			if (other.eventDateOffset != null)
				return false;
		} else if (!eventDateOffset.equals(other.eventDateOffset))
			return false;
		return true;
	}

	/**
	 * @return the eventDateOffset
	 */
	public Duration getEventDateOffset() {
		return eventDateOffset;
	}
	
	/**
	 * @param eventDateOffset the eventDateOffset to set
	 */
	public void setEventDateOffset(Duration eventDateOffset) {
		this.eventDateOffset = eventDateOffset;
	}
	
	/**
	 * @return the value
	 */
	public Double getValue() {
		return value;
	}

	/**
	 * @param value the value to set
	 */
	public void setValue(Double value) {
		this.value = value;
	}

}
