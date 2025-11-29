/* ==================================================================
 * CloudIntegrationsUserEvents.java - 2/10/2024 11:03:48 am
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

package net.solarnetwork.central.c2c.domain;

import java.util.List;
import net.solarnetwork.central.common.http.HttpUserEvents;
import net.solarnetwork.central.domain.CommonUserEvents;
import net.solarnetwork.central.instructor.domain.InstructorUserEvents;

/**
 * Constants and helpers for cloud integration user event handling.
 *
 * @author matt
 * @version 1.5
 */
public interface CloudIntegrationsUserEvents
		extends CommonUserEvents, HttpUserEvents, InstructorUserEvents {

	/** A user event tag for cloud integrations. */
	String CLOUD_INTEGRATIONS_TAG = "c2c";

	/**
	 * A user event tag for {@code CloudIntegrationService} related events.
	 *
	 * @since 1.2
	 */
	String CLOUD_INTEGRATION_TAG = "i9n";

	/**
	 * A user event tag for {@code CloudDatumStreamService} related events.
	 *
	 * @since 1.2
	 */
	String CLOUD_DATUM_STREAM_TAG = "ds";

	/**
	 * A user event tag for {@code CloudControlService} related events.
	 *
	 * @since 1.5
	 */
	String CLOUD_CONTROL_TAG = "ctrl";

	/** A user event tag for an "error". */
	String ERROR_TAG = "error";

	/** A user event tag for a datum stream poll event. */
	String POLL_TAG = "poll";

	/**
	 * A user event tag for a datum stream rake event.
	 *
	 * @since 1.4
	 */
	String RAKE_TAG = "rake";

	/**
	 * User event data key for an integration ID.
	 *
	 * @since 1.5
	 */
	String INTEGRATION_ID_DATA_KEY = "integrationId";

	/** Tags for an authorization error event. */
	List<String> INTEGRATION_AUTH_ERROR_TAGS = List.of(CLOUD_INTEGRATIONS_TAG, ERROR_TAG,
			CLOUD_INTEGRATION_TAG, AUTHORIZATION_TAG);

	/** Tags for an HTTP error event. */
	List<String> INTEGRATION_HTTP_ERROR_TAGS = List.of(CLOUD_INTEGRATIONS_TAG, ERROR_TAG,
			CLOUD_INTEGRATION_TAG, HTTP_TAG);

	/** Tags for an expression error event. */
	List<String> DATUM_STREAM_EXPRESSION_ERROR_TAGS = List.of(CLOUD_INTEGRATIONS_TAG, ERROR_TAG,
			CLOUD_DATUM_STREAM_TAG, EXPRESSION_TAG);

	/** Tags for a poll error event. */
	List<String> INTEGRATION_POLL_ERROR_TAGS = List.of(CLOUD_INTEGRATIONS_TAG, ERROR_TAG,
			CLOUD_DATUM_STREAM_TAG, POLL_TAG);

	/**
	 * Tags for a non-error poll events.
	 *
	 * @since 1.1
	 */
	List<String> INTEGRATION_POLL_TAGS = INTEGRATION_POLL_ERROR_TAGS.stream()
			.filter(t -> !ERROR_TAG.equals(t)).toList();

	/**
	 * Tags for a rake error event.
	 *
	 * @since 1.4
	 */
	List<String> INTEGRATION_RAKE_ERROR_TAGS = List.of(CLOUD_INTEGRATIONS_TAG, ERROR_TAG,
			CLOUD_DATUM_STREAM_TAG, RAKE_TAG);

	/**
	 * Tags for a non-error rake events.
	 *
	 * @since 1.4
	 */
	List<String> INTEGRATION_RAKE_TAGS = INTEGRATION_RAKE_ERROR_TAGS.stream()
			.filter(t -> !ERROR_TAG.equals(t)).toList();

	/**
	 * Tags for a control instruction error event.
	 *
	 * @since 1.5
	 */
	List<String> INTEGRATION_CONTROL_INSTRUCTION_ERROR_TAGS = List.of(CLOUD_INTEGRATIONS_TAG, ERROR_TAG,
			CLOUD_CONTROL_TAG, INSTRUCTION_TAG);

	/**
	 * Tags for a non-error control instruction events.
	 *
	 * @since 1.5
	 */
	List<String> INTEGRATION_CONTROL_INSTRUCTION_TAGS = INTEGRATION_CONTROL_INSTRUCTION_ERROR_TAGS
			.stream().filter(t -> !ERROR_TAG.equals(t)).toList();

}
