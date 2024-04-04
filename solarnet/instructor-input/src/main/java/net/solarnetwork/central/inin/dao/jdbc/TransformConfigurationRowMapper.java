/* ==================================================================
 * TransformConfigurationRowMapper.java - 21/02/2024 1:35:54 pm
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

package net.solarnetwork.central.inin.dao.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import org.springframework.jdbc.core.RowMapper;
import net.solarnetwork.central.inin.domain.TransformConfiguration;
import net.solarnetwork.central.inin.domain.TransformConfiguration.RequestTransformConfiguration;
import net.solarnetwork.central.inin.domain.TransformConfiguration.ResponseTransformConfiguration;

/**
 * Row mapper for {@link TransformConfiguration} entities.
 *
 * <p>
 * The expected column order in the SQL results is:
 * </p>
 *
 * <ol>
 * <li>user_id (BIGINT)</li>
 * <li>id (LONG)</li>
 * <li>created (TIMESTAMP)</li>
 * <li>modified (TIMESTAMP)</li>
 * <li>cname (TEXT)</li>
 * <li>sident (TEXT)</li>
 * <li>sprops (JSON TEXT)</li>
 * </ol>
 *
 * @author matt
 * @version 1.0
 */
public abstract sealed class TransformConfigurationRowMapper<C extends TransformConfiguration<C>>
		implements RowMapper<C> {

	/** A default request instance. */
	public static final RowMapper<RequestTransformConfiguration> REQ_INSTANCE = new RequestTransformConfigurationRowMapper();

	/** A default request instance. */
	public static final RowMapper<ResponseTransformConfiguration> RES_INSTANCE = new ResponseTransformConfigurationRowMapper();

	/**
	 * Row mapper for {@link RequestTransformConfiguration} entities.
	 */
	public static final class RequestTransformConfigurationRowMapper
			extends TransformConfigurationRowMapper<RequestTransformConfiguration> {

		/**
		 * Constructor.
		 */
		public RequestTransformConfigurationRowMapper() {
			super();
		}

		@Override
		protected RequestTransformConfiguration newConfiguration(Long userId, Long entityId,
				Instant ts) {
			return new RequestTransformConfiguration(userId, entityId, ts);
		}

	}

	/**
	 * Row mapper for {@link ResponseTransformConfiguration} entities.
	 */
	public static final class ResponseTransformConfigurationRowMapper
			extends TransformConfigurationRowMapper<ResponseTransformConfiguration> {

		/**
		 * Constructor.
		 */
		public ResponseTransformConfigurationRowMapper() {
			super();
		}

		@Override
		protected ResponseTransformConfiguration newConfiguration(Long userId, Long entityId,
				Instant ts) {
			return new ResponseTransformConfiguration(userId, entityId, ts);
		}

	}

	/**
	 * Constructor.
	 */
	public TransformConfigurationRowMapper() {
		super();
	}

	/**
	 * Construct a new entity instance.
	 *
	 * @param userId
	 *        the user ID
	 * @param entityId
	 *        the entity ID
	 * @param ts
	 *        the timestamp
	 * @return the new instance
	 */
	protected abstract C newConfiguration(Long userId, Long entityId, Instant ts);

	@Override
	public C mapRow(ResultSet rs, int rowNum) throws SQLException {
		int p = 0;
		Long userId = rs.getObject(++p, Long.class);
		Long entityId = rs.getObject(++p, Long.class);
		Timestamp ts = rs.getTimestamp(++p);
		C conf = newConfiguration(userId, entityId, ts.toInstant());
		conf.setModified(rs.getTimestamp(++p).toInstant());
		conf.setName(rs.getString(++p));
		conf.setServiceIdentifier(rs.getString(++p));
		conf.setServicePropsJson(rs.getString(++p));
		return conf;
	}

}
