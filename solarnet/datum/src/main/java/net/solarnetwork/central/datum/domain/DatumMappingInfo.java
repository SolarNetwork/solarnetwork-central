/* ==================================================================
 * DatumMappingInfo.java - Apr 14, 2015 6:30:29 AM
 *
 * Copyright 2007-2015 SolarNetwork.net Dev Team
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

import org.jspecify.annotations.Nullable;

/**
 * Helper object when mapping legacy {link Datum} objects into
 * {@link GeneralNodeDatum} or {@link GeneralLocationDatum} objects.
 *
 * @author matt
 * @version 1.0
 */
public class DatumMappingInfo {

	private @Nullable Long id;
	private @Nullable String sourceId;
	private @Nullable String timeZoneId;

	public final @Nullable Long getId() {
		return id;
	}

	public final void setId(@Nullable Long id) {
		this.id = id;
	}

	public final @Nullable String getSourceId() {
		return sourceId;
	}

	public final void setSourceId(@Nullable String sourceId) {
		this.sourceId = sourceId;
	}

	public final @Nullable String getTimeZoneId() {
		return timeZoneId;
	}

	public final void setTimeZoneId(@Nullable String timeZoneId) {
		this.timeZoneId = timeZoneId;
	}

}
