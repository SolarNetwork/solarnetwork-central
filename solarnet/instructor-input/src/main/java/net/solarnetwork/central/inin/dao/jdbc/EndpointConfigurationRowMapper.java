/* ==================================================================
 * EndpointConfigurationRowMapper.java - 21/02/2024 2:58:02 pm
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

import static net.solarnetwork.central.common.dao.jdbc.sql.CommonJdbcUtils.getTimestampInstant;
import static net.solarnetwork.util.ObjectUtils.nonnull;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.UUID;
import org.springframework.jdbc.core.RowMapper;
import net.solarnetwork.central.common.dao.jdbc.sql.CommonJdbcUtils;
import net.solarnetwork.central.inin.domain.EndpointConfiguration;

/**
 * Row mapper for {@link EndpointConfiguration} entities.
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
 * <li>enabled (BOOLEAN)</li>
 * <li>cname (TEXT)</li>
 * <li>node_ids (BIGINT[])</li>
 * <li>req_xform_id (BIGINT)</li>
 * <li>res_xform_id (BIGINT)</li>
 * <li>max_exec_secs (INTEGER)</li>
 * <li>user_meta_path (TEXT)</li>
 * <li>req_type (TEXT)</li>
 * <li>res_type (TEXT)</li>
 * </ol>
 *
 * @author matt
 * @version 1.2
 */
public class EndpointConfigurationRowMapper implements RowMapper<EndpointConfiguration> {

	/** A default instance. */
	public static final RowMapper<EndpointConfiguration> INSTANCE = new EndpointConfigurationRowMapper();

	private final int columnOffset;

	/**
	 * Default constructor.
	 */
	public EndpointConfigurationRowMapper() {
		this(0);
	}

	/**
	 * Constructor.
	 *
	 * @param columnOffset
	 *        a column offset to apply
	 */
	public EndpointConfigurationRowMapper(int columnOffset) {
		this.columnOffset = columnOffset;
	}

	@Override
	public EndpointConfiguration mapRow(ResultSet rs, int rowNum) throws SQLException {
		int p = columnOffset;
		Long userId = rs.getLong(++p);
		UUID entityId = nonnull(CommonJdbcUtils.getUuid(rs, ++p), "entityId");
		Instant ts = nonnull(getTimestampInstant(rs, ++p), "created");
		Instant mod = getTimestampInstant(rs, ++p);
		boolean enabled = rs.getBoolean(++p);

		final EndpointConfiguration conf = new EndpointConfiguration(userId, entityId, ts,
				rs.getString(++p));
		conf.setModified(mod);
		conf.setEnabled(enabled);

		Long[] nodeIds = CommonJdbcUtils.getArray(rs, ++p);
		if ( nodeIds != null ) {
			conf.setNodeIds(new LinkedHashSet<>(Arrays.asList(nodeIds)));
		}

		conf.setRequestTransformId(rs.getObject(++p, Long.class));
		conf.setResponseTransformId(rs.getObject(++p, Long.class));
		conf.setMaxExecutionSeconds(rs.getInt(++p));
		conf.setUserMetadataPath(rs.getString(++p));
		conf.setRequestContentType(rs.getString(++p));
		conf.setResponseContentType(rs.getString(++p));
		return conf;
	}

}
