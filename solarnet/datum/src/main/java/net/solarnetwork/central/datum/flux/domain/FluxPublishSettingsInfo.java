/* ==================================================================
 * FluxPublishSettingsInfo.java - 24/06/2024 8:34:27 am
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

package net.solarnetwork.central.datum.flux.domain;

import java.io.Serializable;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Basic implementation of {@link FluxPublishSettings}.
 *
 * @author matt
 * @version 1.0
 */
public record FluxPublishSettingsInfo(boolean publish, boolean retain)
		implements FluxPublishSettings, Serializable {

	/** A common "publish retained" setting instance. */
	public static final FluxPublishSettings PUBLISH_RETAINED = new FluxPublishSettingsInfo(true, true);

	/** A common "publish, not retained" setting instance. */
	public static final FluxPublishSettings PUBLISH_NOT_RETAINED = new FluxPublishSettingsInfo(true,
			false);

	/** A common "do not publish, not retained" setting instance. */
	public static final FluxPublishSettings NOT_PUBLISHED = new FluxPublishSettingsInfo(false, false);

	/** A common "do not publish, retained" setting instance. */
	public static final FluxPublishSettings RETAINED = new FluxPublishSettingsInfo(false, true);

	@JsonIgnore
	@Override
	public boolean isPublish() {
		return publish;
	}

	@JsonIgnore
	@Override
	public boolean isRetain() {
		return retain;
	}

}
