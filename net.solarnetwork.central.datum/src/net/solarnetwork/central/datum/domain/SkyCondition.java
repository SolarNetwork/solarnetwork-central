/* ===================================================================
 * SkyCondition.java
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
 * A standardized enumeration of weather sky conditions.
 * 
 * @author matt.magoffin
 * @version $Revision$ $Date$
 */
public enum SkyCondition {
	
	/** Clear night. */
	ClearNight,
	
	/** Clear day. */
	Clear,
	
	/** Few clouds night. */
	FewCloudsNight,
	
	/** Few couds day. */
	FewClouds,
	
	/** Fog. */
	Fog,
	
	/** Overcast. */
	Overcast,
	
	/** Severe alert. */
	SevereAlert,
	
	/** Showers scattered. */
	ShowersScattered,
	
	/** Showers. */
	Showers,
	
	/** Snow. */
	Snow,
	
	/** Storm. */
	Storm;
	
	/**
	 * Get a night-time equivalent value for a given SkyCondition.
	 * 
	 * <p>Some conditions have day and night counterparts. If this object
	 * is a day condition and has a night equivalent, the night equivalent
	 * will be returned. Otherwise {@code this} will be returned.</p>
	 * 
	 * @return the night time equivalent SkyCondition
	 */
	public SkyCondition getNightEquivalent() {
		switch ( this ) {
			case Clear:
				return ClearNight;
				
			case FewClouds:
				return FewCloudsNight;
				
			default:
				return this;
		}
	}
	
	/**
	 * Get a day-time equivalent value for a given SkyCondition.
	 * 
	 * <p>Some conditions have day and night counterparts. If this object
	 * is a night condition and has a day equivalent, the day equivalent
	 * will be returned. Otherwise {@code this} will be returned.</p>
	 * 
	 * @return the day time equivalent SkyCondition
	 */
	public SkyCondition getDayEquivalent() {
		switch ( this ) {
			case ClearNight:
				return Clear;
				
			case FewCloudsNight:
				return FewClouds;
				
			default:
				return this;
		}
	}
	
}