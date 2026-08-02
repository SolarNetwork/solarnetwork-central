/* ==================================================================
 * BasicDatumStreamMetadata.java - 22/10/2020 3:07:55 pm
 *
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.v2.domain;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.domain.datum.DatumStreamMetadata;

/**
 * Implementation of {@link DatumStreamMetadata}.
 *
 * @author matt
 * @version 2.2
 * @since 2.8
 */
public class BasicDatumStreamMetadata implements DatumStreamMetadata, Serializable {

	@Serial
	private static final long serialVersionUID = 2292730487865098801L;

	private final UUID streamId;
	private final @Nullable String timeZoneId;
	private final String @Nullable [] instantaneousProperties;
	private final String @Nullable [] accumulatingProperties;
	private final String @Nullable [] statusProperties;

	/**
	 * Constructor.
	 *
	 * <p>
	 * All arguments except {@code streamId} are allowed to be {@code null}. If
	 * any array is empty, it will be treated as if it were {@code null}.
	 * </p>
	 *
	 * @param streamId
	 *        the stream ID
	 * @param timeZoneId
	 *        the time zone ID
	 * @param instantaneousProperties
	 *        the instantaneous property names
	 * @param accumulatingProperties
	 *        the accumulating property names
	 * @param statusProperties
	 *        the status property names
	 * @throws IllegalArgumentException
	 *         if {@code streamId} is {@code null}
	 */
	public BasicDatumStreamMetadata(UUID streamId, @Nullable String timeZoneId,
			String @Nullable [] instantaneousProperties, String @Nullable [] accumulatingProperties,
			String @Nullable [] statusProperties) {
		super();
		this.streamId = requireNonNullArgument(streamId, "streamId");
		this.timeZoneId = timeZoneId;
		this.instantaneousProperties = instantaneousProperties != null
				&& instantaneousProperties.length > 0 ? instantaneousProperties : null;
		this.accumulatingProperties = accumulatingProperties != null && accumulatingProperties.length > 0
				? accumulatingProperties
				: null;
		this.statusProperties = statusProperties != null && statusProperties.length > 0
				? statusProperties
				: null;
	}

	/**
	 * Constructor.
	 *
	 * <p>
	 * All arguments except {@code streamId} are allowed to be {@code null}. The
	 * other arguments are {@code Object} to work around MyBatis mapping issues.
	 * If any array is empty, it will be treated as if it were {@code null}.
	 * </p>
	 *
	 * @param streamId
	 *        the stream ID
	 * @param timeZoneId
	 *        the time zone ID
	 * @param instantaneousProperties
	 *        the instantaneous property names; must be a {@code String[]}
	 * @param accumulatingProperties
	 *        the accumulating property names; must be a {@code String[]}
	 * @param statusProperties
	 *        the status property names; must be a {@code String[]}
	 * @throws IllegalArgumentException
	 *         if {@code streamId} is {@code null}
	 */
	public BasicDatumStreamMetadata(UUID streamId, @Nullable String timeZoneId,
			@Nullable Object instantaneousProperties, @Nullable Object accumulatingProperties,
			@Nullable Object statusProperties) {
		this(streamId, timeZoneId, (String @Nullable []) instantaneousProperties,
				(String @Nullable []) accumulatingProperties, (String @Nullable []) statusProperties);
	}

	@Override
	public int hashCode() {
		return Objects.hash(streamId);
	}

	/**
	 * Compare for equality.
	 *
	 * <p>
	 * Only the {@code streamId} is considered.
	 * </p>
	 *
	 * {@inheritDoc}
	 */
	@Override
	public boolean equals(@Nullable Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( !(obj instanceof BasicDatumStreamMetadata other) ) {
			return false;
		}
		return Objects.equals(streamId, other.streamId);
	}

	@Override
	public final UUID getStreamId() {
		return streamId;
	}

	@Override
	public final @Nullable String getTimeZoneId() {
		return timeZoneId;
	}

	@Override
	public String @Nullable [] propertyNamesForType(DatumSamplesType type) {
		if ( type == null ) {
			return null;
		}
		return switch (type) {
			case Instantaneous -> instantaneousProperties;
			case Accumulating -> accumulatingProperties;
			case Status -> statusProperties;
			default -> null;
		};
	}

}
