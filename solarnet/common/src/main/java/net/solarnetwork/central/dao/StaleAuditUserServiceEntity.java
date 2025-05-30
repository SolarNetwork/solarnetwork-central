/* ==================================================================
 * StaleAuditUserServiceEntity.java - 29/05/2024 4:02:11 pm
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

package net.solarnetwork.central.dao;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import net.solarnetwork.central.domain.AggregateDatumId;
import net.solarnetwork.central.domain.StaleAuditUserServiceValue;
import net.solarnetwork.dao.BasicEntity;
import net.solarnetwork.domain.Differentiable;
import net.solarnetwork.domain.datum.DatumId;

/**
 * Stale audit user service entity.
 *
 * <p>
 * Although {@link AggregateDatumId} is used as a primary key for this entity,
 * and {@link AggregateDatumId#getKind()} will be set to
 * {@link net.solarnetwork.domain.datum.ObjectDatumKind#Node}, this entity is
 * <b>not</b> node related, and the {@link DatumId#getObjectId()} actually
 * refers to a <b>user</b> entity.
 *
 * @author matt
 * @version 2.0
 */
public class StaleAuditUserServiceEntity
		extends BasicEntity<StaleAuditUserServiceEntity, AggregateDatumId> implements
		StaleAuditUserServiceValue, Cloneable, Serializable, Differentiable<StaleAuditUserServiceValue> {

	@Serial
	private static final long serialVersionUID = -3150400939940353987L;

	/**
	 * Constructor.
	 *
	 * @param id
	 *        the ID
	 * @param created
	 *        the creation date
	 */
	public StaleAuditUserServiceEntity(AggregateDatumId id, Instant created) {
		super(id, created);
	}

	/**
	 * Test if the properties of another object are the same as in this
	 * instance.
	 *
	 * @param other
	 *        the other entity to compare to
	 * @return {@literal true} if the properties of this instance are equal to
	 *         the other
	 */
	public boolean isSameAs(StaleAuditUserServiceValue other) {
		if ( other == null ) {
			return false;
		}
		return Objects.equals(getId(), other.getId());
	}

	@Override
	public boolean differsFrom(StaleAuditUserServiceValue other) {
		return !isSameAs(other);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("StaleAuditUserServiceEntity{");
		final AggregateDatumId id = getId();
		if ( id != null ) {
			builder.append("userId=").append(id.getObjectId());
			builder.append(",service=").append(id.getSourceId());
			builder.append(",agg=").append(id.getAggregation());
			builder.append(",ts=").append(id.getTimestamp());
		}
		builder.append("}");
		return builder.toString();
	}

}
