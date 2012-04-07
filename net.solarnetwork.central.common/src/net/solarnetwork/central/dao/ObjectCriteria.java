/* ==================================================================
 * ObjectCriteria.java - Aug 8, 2010 8:52:25 PM
 * 
 * Copyright 2007-2010 SolarNetwork.net Dev Team
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
 * $Revision$
 * ==================================================================
 */

package net.solarnetwork.central.dao;

import net.solarnetwork.central.domain.Filter;

/**
 * Generic object search criteria API.
 * 
 * @author matt
 * @version $Revision$
 */
public interface ObjectCriteria<T extends Filter> {

	/**
	 * Search filter boolean join types.
	 */
	enum JoinType {
		
		/** Join all enclosed AttributeSearchFilter objects with a logical AND (default join mode). */
		AND,
		
		/** Join all enclosed AttributeSearchFilter objects with a logical OR. */
		OR,
		
		/** Join all enclosed AttributeSearchFilter objects with a logical NOT. */
		NOT;
		
		@Override
		public String toString() {
			switch (this) {
				case AND: return "&";
				case OR: return "|";
				case NOT: return "!";
				default: throw new AssertionError(this);
			}
		}
	}
	
	/**
	 * Search match types.
	 */
	enum MatchType {

		/** Match exactly this attribute value. */
		EQUAL,

		/** Match anything but exactly this attribute value. */
		NOT_EQUAL,

		/** Match attribute values less than this attribute value. */
		LESS_THAN,

		/** Match attribute values less than or equal to this attribute value. */
		LESS_THAN_EQUAL,

		/** Match attribute values greater than this attribute value. */
		GREATER_THAN,

		/** Match attribute values greater than or equal to this attribute value. */
		GREATER_THAN_EQUAL,

		/** Match a substring (this attribute value) within attribute values. */
		SUBSTRING,

		/** Match a substring (this attribute value) at the start of an attribute value. */
		SUBSTRING_AT_START,

		/** Match if the attribute name is present, regardless of its value. */
		PRESENT,

		/** Approximately match the attribute value to this attribute value. */
		APPROX,
		
		/** For array comparison, an overlap operator. */
		OVERLAP;

		@Override
		public String toString() {
			switch (this) {
				case EQUAL: return "=";
				case NOT_EQUAL: return "<>";
				case LESS_THAN: return "<";
				case LESS_THAN_EQUAL: return "<=";
				case GREATER_THAN: return ">";
				case GREATER_THAN_EQUAL: return ">=";
				case SUBSTRING: return "**";
				case SUBSTRING_AT_START: return "*";
				case PRESENT: return "?";
				case APPROX: return "~";
				case OVERLAP: return "&&";
				default: throw new AssertionError(this);
			}
		}
	}
	
	/**
	 * Get a simple filter object.
	 * 
	 * @return simple filter objecct
	 */
	T getSimpleFilter();
	
	/**
	 * Get the simple filter join type.
	 * 
	 * @return join type
	 */
	JoinType getSimpleJoinType();
	
	/**
	 * Get the simple filter match type.
	 * 
	 * @return match type
	 */
	MatchType getSimpleMatchType();
	
	/**
	 * Get a result offset.
	 * 
	 * @return result offset
	 */
	Integer getResultOffset();
	
	/**
	 * Get the maximum number of results.
	 * 
	 * @return result max
	 */
	Integer getResultMax();
	
}
