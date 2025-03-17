/* ==================================================================
 * UserOutputConfigurationInput.java - 17/03/2025 4:53:37 pm
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

package net.solarnetwork.central.reg.web.domain;

import java.time.Instant;
import com.fasterxml.jackson.annotation.JsonIgnore;
import net.solarnetwork.central.dao.BaseUserRelatedStdIdentifiableConfigurationInput;
import net.solarnetwork.central.datum.export.domain.OutputCompressionType;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.user.export.domain.UserOutputConfiguration;

/**
 * Input DTO for {@link UserOutputConfiguration} entities.
 *
 * @author matt
 * @version 1.0
 */
public final class UserOutputConfigurationInput extends
		BaseUserRelatedStdIdentifiableConfigurationInput<UserOutputConfiguration, UserLongCompositePK> {

	private Long id;

	private OutputCompressionType compressionType;

	/**
	 * Constructor.
	 */
	public UserOutputConfigurationInput() {
		super();
	}

	@Override
	public UserOutputConfiguration toEntity(UserLongCompositePK id, Instant date) {
		UserOutputConfiguration entity = new UserOutputConfiguration(id, date);
		populateConfiguration(entity);
		return entity;
	}

	@Override
	protected void populateConfiguration(UserOutputConfiguration conf) {
		super.populateConfiguration(conf);
		conf.setCompressionType(compressionType);
	}

	/**
	 * Get the configuration ID.
	 *
	 * @return the ID
	 */
	public Long getId() {
		return id;
	}

	/**
	 * Set the configuration ID.
	 *
	 * @param id
	 *        the ID to set
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Get the compression type.
	 *
	 * @return the type
	 */
	@JsonIgnore
	public OutputCompressionType getCompressionType() {
		return compressionType;
	}

	/**
	 * Set the compression type.
	 *
	 * @param compressionType
	 *        the type to set
	 */
	public void setCompressionType(OutputCompressionType compressionType) {
		this.compressionType = compressionType;
	}

	/**
	 * Get the compression type key value.
	 *
	 * @return the compression type; if {@link #getCompressionType()} is
	 *         {@literal null} this will return the key value for
	 *         {@link OutputCompressionType#None}
	 */
	public char getCompressionTypeKey() {
		OutputCompressionType type = getCompressionType();
		return (type != null ? type.getKey() : OutputCompressionType.None.getKey());
	}

	/**
	 * Set the output compression type via its key value.
	 *
	 * @param key
	 *        the key of the compression type to set; if {@code key} is
	 *        unsupported, the compression will be set to
	 *        {@link OutputCompressionType#None}
	 */
	public void setCompressionTypeKey(char key) {
		OutputCompressionType type = OutputCompressionType.None;
		try {
			type = OutputCompressionType.forKey(key);
		} catch ( IllegalArgumentException e ) {
			// ignore, and force to None
		}
		setCompressionType(type);
	}

}
