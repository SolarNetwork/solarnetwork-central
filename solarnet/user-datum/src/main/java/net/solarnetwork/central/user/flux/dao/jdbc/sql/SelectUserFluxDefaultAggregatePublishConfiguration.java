/* ==================================================================
 * SelectUserFluxDefaultAggregatePublishConfiguration.java - 25/06/2024 10:50:13 am
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

package net.solarnetwork.central.user.flux.dao.jdbc.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.central.user.flux.domain.UserFluxDefaultAggregatePublishConfiguration;
import net.solarnetwork.util.ObjectUtils;

/**
 * Select for {@link UserFluxDefaultAggregatePublishConfiguration} entities.
 *
 * @author matt
 * @version 1.0
 */
public final class SelectUserFluxDefaultAggregatePublishConfiguration
		implements PreparedStatementCreator, SqlProvider {

	private final Long userId;

	/**
	 * Constructor.
	 *
	 * @param userId
	 *        the user ID to get
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public SelectUserFluxDefaultAggregatePublishConfiguration(Long userId) {
		super();
		this.userId = ObjectUtils.requireNonNullArgument(userId, "userId");
	}

	@Override
	public String getSql() {
		StringBuilder buf = new StringBuilder();
		buf.append("""
				SELECT user_id, created, modified, publish, retain
				FROM solaruser.user_flux_default_agg_pub_settings
				WHERE user_id = ?
				""");
		return buf.toString();
	}

	@Override
	public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
		PreparedStatement stmt = con.prepareStatement(getSql(), ResultSet.TYPE_FORWARD_ONLY,
				ResultSet.CONCUR_READ_ONLY, ResultSet.CLOSE_CURSORS_AT_COMMIT);
		stmt.setObject(1, userId);
		return stmt;
	}

}
