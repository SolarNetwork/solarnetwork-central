/* ==================================================================
 * GeneralLocationDatumComponents.java - 13/11/2018 7:49:41 AM
 *
 * Copyright 2018 SolarNetwork.net Dev Team
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

import java.io.Serial;
import java.time.Instant;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.util.ObjectUtils;

/**
 * Extension of {@link GeneralLocationDatum} to facilitate serializing into
 * sample components rather than inline sample data.
 *
 * @author matt
 * @version 2.0
 * @since 1.30
 */
public class GeneralLocationDatumComponents extends GeneralLocationDatum {

	@Serial
	private static final long serialVersionUID = -4036964378594823693L;

	/**
	 * Constructor.
	 *
	 * @param id
	 *        the ID
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public GeneralLocationDatumComponents(GeneralLocationDatumPK id) {
		super(id);
	}

	/**
	 * Constructor.
	 *
	 * @param locationId
	 *        the location ID
	 * @param created
	 *        the creation date
	 * @param sourceId
	 *        the source ID
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	@JsonCreator
	public GeneralLocationDatumComponents(@JsonProperty("locationId") Long locationId,
			@JsonProperty("created") Instant created, @JsonProperty("sourceId") String sourceId) {
		super(locationId, created, sourceId);
	}

	/**
	 * Copy constructor.
	 *
	 * @param other
	 *        the datum to copy
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public GeneralLocationDatumComponents(GeneralLocationDatum other) {
		super(ObjectUtils.requireNonNullArgument(other, "other").getId());
		setPosted(other.getPosted());
		setSamples(other.getSamples());
	}

	@JsonUnwrapped
	public @Nullable DatumSamples getSampleComponents() {
		return getSamples();
	}

	/**
	 * This implementation returns {@code null} so the data is not unwrapped
	 * during serialization.
	 */
	@Override
	public @Nullable Map<String, ?> getSampleData() {
		return null;
	}

}
