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

import static net.solarnetwork.central.common.dao.jdbc.sql.CommonJdbcUtils.timestampInstant;
import java.sql.ResultSet;
import java.sql.SQLException;
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

	/**
	 * Row mapper for {@link RequestTransformConfiguration} entities.
	 */
	public static final class RequestTransformConfigurationRowMapper
			extends TransformConfigurationRowMapper<RequestTransformConfiguration> {

		/** A default request instance. */
		public static final RowMapper<RequestTransformConfiguration> INSTANCE = new TransformConfigurationRowMapper.RequestTransformConfigurationRowMapper();

		/**
		 * Constructor.
		 */
		public RequestTransformConfigurationRowMapper() {
			super();
		}

		@Override
		protected RequestTransformConfiguration newConfiguration(Long userId, Long entityId, Instant ts,
				String name, String serviceIdentifier) {
			return new RequestTransformConfiguration(userId, entityId, ts, name, serviceIdentifier);
		}

	}

	/**
	 * Row mapper for {@link ResponseTransformConfiguration} entities.
	 */
	public static final class ResponseTransformConfigurationRowMapper
			extends TransformConfigurationRowMapper<ResponseTransformConfiguration> {

		/** A default request instance. */
		public static final RowMapper<ResponseTransformConfiguration> INSTANCE = new TransformConfigurationRowMapper.ResponseTransformConfigurationRowMapper();

		/**
		 * Constructor.
		 */
		public ResponseTransformConfigurationRowMapper() {
			super();
		}

		@Override
		protected ResponseTransformConfiguration newConfiguration(Long userId, Long entityId, Instant ts,
				String name, String serviceIdentifier) {
			return new ResponseTransformConfiguration(userId, entityId, ts, name, serviceIdentifier);
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
	 * @param name
	 *        the name
	 * @param serviceIdentifier
	 *        the service identifier
	 * @return the new instance
	 */
	protected abstract C newConfiguration(Long userId, Long entityId, Instant ts, String name,
			String serviceIdentifier);

	@Override
	public C mapRow(ResultSet rs, int rowNum) throws SQLException {
		int p = 0;
		Long userId = rs.getObject(++p, Long.class);
		Long entityId = rs.getObject(++p, Long.class);
		Instant ts = timestampInstant(rs, ++p);
		Instant mod = timestampInstant(rs, ++p);

		final C conf = newConfiguration(userId, entityId, ts, rs.getString(++p), rs.getString(++p));
		conf.setModified(mod);
		conf.setServicePropsJson(rs.getString(++p));
		return conf;
	}

}
