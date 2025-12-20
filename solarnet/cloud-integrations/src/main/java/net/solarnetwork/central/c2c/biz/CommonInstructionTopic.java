/* ==================================================================
 * CommonInstructionTopic.java - 12/11/2025 7:08:56 am
 *
 * Copyright 2025 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.c2c.biz;

/**
 * Common instruction topic enumeration.
 *
 * @author matt
 * @version 1.0
 */
public enum CommonInstructionTopic {

	/** Set a control parameter value. */
	SetControlParameter,

	;

	/**
	 * Find an enumeration for a topic string.
	 *
	 * <p>
	 * This method supports case-insensitive topic name matching.
	 * </p>
	 *
	 * @param topic
	 *        the topic string
	 * @return the matching enum instance, or {@code null} if not found
	 */
	public static CommonInstructionTopic findForTopic(String topic) {
		try {
			return CommonInstructionTopic.valueOf(topic);
		} catch ( IllegalArgumentException e ) {
			// try case-insensitive match
			for ( CommonInstructionTopic t : CommonInstructionTopic.values() ) {
				if ( topic.equalsIgnoreCase(t.toString()) ) {
					return t;
				}
			}
		}
		return null;
	}

}
