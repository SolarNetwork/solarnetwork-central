/* ==================================================================
 * GeneralNodeDatumComponents.java - 13/11/2018 7:25:54 AM
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

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.Serial;
import java.time.Instant;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import net.solarnetwork.domain.datum.DatumSamples;

/**
 * Extension of {@link GeneralNodeDatum} to facilitate serializing into sample
 * components rather than inline sample data.
 *
 * @author matt
 * @version 2.0
 * @since 1.30
 */
public class GeneralNodeDatumComponents extends GeneralNodeDatum {

	@Serial
	private static final long serialVersionUID = 5190123902690412934L;

	/**
	 * Constructor.
	 *
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public GeneralNodeDatumComponents(GeneralNodeDatumPK id) {
		super(id);
	}

	/**
	 * Constructor.
	 *
	 * @param nodeId
	 *        the node ID
	 * @param created
	 *        the creation date
	 * @param sourceId
	 *        the source ID
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	@JsonCreator
	public GeneralNodeDatumComponents(@JsonProperty("nodeId") Long nodeId,
			@JsonProperty("created") Instant created, @JsonProperty("sourceId") String sourceId) {
		super(nodeId, created, sourceId);
	}

	/**
	 * Copy constructor.
	 *
	 * @param other
	 *        the datum to copy
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public GeneralNodeDatumComponents(GeneralNodeDatum other) {
		this(requireNonNullArgument(other, "other").getId());
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
