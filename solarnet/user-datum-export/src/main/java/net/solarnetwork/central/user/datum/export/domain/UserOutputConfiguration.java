/* ==================================================================
 * UserOutputConfiguration.java - 21/03/2018 2:13:25 PM
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

package net.solarnetwork.central.user.datum.export.domain;

import static net.solarnetwork.util.ObjectUtils.nonnull;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import net.solarnetwork.central.datum.export.domain.OutputCompressionType;
import net.solarnetwork.central.datum.export.domain.OutputConfiguration;
import net.solarnetwork.central.domain.UserLongCompositePK;

/**
 * User related {@link OutputConfiguration} entity.
 *
 * @author matt
 * @version 1.1
 */
@JsonPropertyOrder({ "id", "created", "userId", "name", "serviceIdentifier", "compressionTypeKey",
		"serviceProperties" })
@JsonIgnoreProperties("enabled")
public class UserOutputConfiguration extends BaseExportConfigurationEntity<UserOutputConfiguration>
		implements OutputConfiguration, Serializable {

	@Serial
	private static final long serialVersionUID = -1581617729654201770L;

	private OutputCompressionType compressionType;

	/**
	 * Constructor.
	 * 
	 * @param id
	 *        the primary key
	 * @param created
	 *        the creation date
	 * @param name
	 *        the name
	 * @param serviceIdentifier
	 *        the service identifier
	 * @param compressionType
	 *        the compression type
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public UserOutputConfiguration(UserLongCompositePK id, Instant created, String name,
			String serviceIdentifier, OutputCompressionType compressionType) {
		super(id, created, name, serviceIdentifier);
		this.compressionType = requireNonNullArgument(compressionType, "compressionType");
	}

	/**
	 * Constructor.
	 * 
	 * @param userId
	 *        the user ID
	 * @param configId
	 *        the configuration ID
	 * @param created
	 *        the creation date
	 * @param name
	 *        the name
	 * @param serviceIdentifier
	 *        the service identifier
	 * @param compressionType
	 *        the compression type
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public UserOutputConfiguration(Long userId, Long configId, Instant created, String name,
			String serviceIdentifier, OutputCompressionType compressionType) {
		this(new UserLongCompositePK(userId, configId), created, name, serviceIdentifier,
				compressionType);
	}

	@Override
	public UserOutputConfiguration copyWithId(UserLongCompositePK id) {
		var copy = new UserOutputConfiguration(id, getCreated(), getName(), getServiceIdentifier(),
				compressionType);
		copyTo(copy);
		return copy;
	}

	@Override
	public void copyTo(UserOutputConfiguration entity) {
		super.copyTo(entity);
		entity.setCompressionType(compressionType);
	}

	@Override
	public boolean isSameAs(@Nullable UserOutputConfiguration other) {
		if ( !super.isSameAs(other) ) {
			return false;
		}
		final var o = nonnull(other, "other");
		return Objects.equals(compressionType, o.getCompressionType());
	}

	@JsonIgnore
	@Override
	public final OutputCompressionType getCompressionType() {
		return compressionType;
	}

	/**
	 * Set the compression type.
	 * 
	 * @param compressionType
	 *        the type to set
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public final void setCompressionType(OutputCompressionType compressionType) {
		this.compressionType = requireNonNullArgument(compressionType, "compressionType");
	}

	/**
	 * Get the compression type key value.
	 *
	 * @return the compression type; if {@link #getCompressionType()} is
	 *         {@code null} this will return the key value for
	 *         {@link OutputCompressionType#None}
	 */
	public final char getCompressionTypeKey() {
		return compressionType.getKey();
	}

	/**
	 * Set the output compression type via its key value.
	 *
	 * @param key
	 *        the key of the compression type to set; if {@code key} is
	 *        unsupported, the compression will be set to
	 *        {@link OutputCompressionType#None}
	 */
	public final void setCompressionTypeKey(char key) {
		OutputCompressionType type = OutputCompressionType.None;
		try {
			type = OutputCompressionType.forKey(key);
		} catch ( IllegalArgumentException e ) {
			// ignore, and force to None
		}
		setCompressionType(type);
	}

}
