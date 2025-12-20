/* ==================================================================
 * CloudDatumStreamRakeTaskEntityInput.java - 22/09/2025 9:44:28 am
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

package net.solarnetwork.central.user.c2c.domain;

import jakarta.validation.constraints.NotNull;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamIdRelated;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamRakeTaskEntity;
import net.solarnetwork.central.domain.UserLongCompositePK;

/**
 * DTO for cloud datum stream rake task entity.
 *
 * @author matt
 * @version 1.2
 */
public class CloudDatumStreamRakeTaskEntityInput extends CloudDatumStreamRakeTaskEntityBaseInput
		implements CloudDatumStreamIdRelated {

	@NotNull
	private Long datumStreamId;

	/**
	 * Constructor.
	 */
	public CloudDatumStreamRakeTaskEntityInput() {
		super();
	}

	@Override
	public CloudDatumStreamRakeTaskEntity toEntity(UserLongCompositePK id) {
		CloudDatumStreamRakeTaskEntity conf = super.toEntity(id);
		conf.setDatumStreamId(datumStreamId);
		return conf;
	}

	/**
	 * Get the datum stream ID.
	 *
	 * @return the datum stream ID
	 */
	@Override
	public Long getDatumStreamId() {
		return datumStreamId;
	}

	/**
	 * Set the datum stream ID.
	 *
	 * @param datumStreamId
	 *        the datum stream ID to set
	 */
	public void setDatumStreamId(Long datumStreamId) {
		this.datumStreamId = datumStreamId;
	}

}
