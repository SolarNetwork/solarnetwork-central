/* ==================================================================
 * UsersUserEvents.java - 2/10/2024 11:03:48 am
 *
 * Copyright 2024 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.domain;

import java.util.List;
import net.solarnetwork.central.common.http.HttpUserEvents;
import net.solarnetwork.central.domain.CommonUserEvents;
import net.solarnetwork.central.instructor.domain.InstructorUserEvents;

/**
 * Constants and helpers for user user event handling.
 *
 * @author matt
 * @version 1.0
 */
public interface UsersUserEvents extends CommonUserEvents, InstructorUserEvents, HttpUserEvents {

	/**
	 * Tags for a user instruction error event.
	 */
	List<String> INSTRUCTION_ERROR_TAGS = List.of(ERROR_TAG, NODE_TAG, INSTRUCTION_TAG);

	/**
	 * Tags for a non-error user instruction events.
	 */
	List<String> INSTRUCTION_TAGS = INSTRUCTION_ERROR_TAGS.stream().filter(t -> !ERROR_TAG.equals(t))
			.toList();

	/**
	 * Tags for a user instruction HTTP error event.
	 */
	List<String> INSTRUCTION_HTTP_ERROR_TAGS = List.of(ERROR_TAG, NODE_TAG, INSTRUCTION_TAG, HTTP_TAG);

	/**
	 * Tags for a non-error user instruction HTTP events.
	 */
	List<String> INSTRUCTION_HTTP_TAGS = INSTRUCTION_ERROR_TAGS.stream()
			.filter(t -> !ERROR_TAG.equals(t)).toList();

}
