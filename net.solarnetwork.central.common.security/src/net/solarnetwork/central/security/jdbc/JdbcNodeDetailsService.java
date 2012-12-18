/* ==================================================================
 * JdbcNodeDetailsService.java - Dec 18, 2012 7:00:14 AM
 * 
 * Copyright 2007-2012 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.security.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import javax.sql.DataSource;
import net.solarnetwork.central.security.AuthenticatedNode;
import net.solarnetwork.central.security.NodeUserDetailsService;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * JDBC based extension of {@link NodeUserDetailsService}.
 * 
 * <p>
 * The configurable properties of this class are:
 * </p>
 * 
 * <dl class="class-properties">
 * <dt>nodesByUsernameQuery</dt>
 * <dd>The SQL query to execute. It must accept a single string "username"
 * parameter and it must return in the first row a Long node ID value.</dd>
 * 
 * <dt>jdbcTemplate</dt>
 * <dd>The {@link JdbcTemplate} to use.</dd>
 * </dl>
 * 
 * @author matt
 * @version 1.0
 */
public class JdbcNodeDetailsService extends NodeUserDetailsService {

	private JdbcTemplate jdbcTemplate;
	private String nodesByUsernameQuery;

	@Override
	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException,
			DataAccessException {
		List<UserDetails> results = jdbcTemplate.query(nodesByUsernameQuery, new String[] { username,
				username }, new RowMapper<UserDetails>() {

			@Override
			public UserDetails mapRow(ResultSet rs, int rowNum) throws SQLException {
				Long id = rs.getLong(1);
				String authToken = rs.getString(2);
				String authSecret = rs.getString(3);
				return new AuthenticatedNode(id, authToken, authSecret, AUTHORITIES);
			}
		});
		if ( results.size() > 0 ) {
			return results.get(0);
		}
		return null;
	}

	/**
	 * Configure a {@link JdbcTemplate} by providing the {@link DataSource}.
	 * 
	 * <p>
	 * This will create a new {@link JdbcTemplate} from the given
	 * {@link DataSource}.
	 * </p>
	 * 
	 * @param dataSource
	 *        the DataSource to use
	 */
	public void setDataSource(DataSource dataSource) {
		setJdbcTemplate(new JdbcTemplate(dataSource));
	}

	public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public void setNodesByUsernameQuery(String nodesByUsernameQuery) {
		this.nodesByUsernameQuery = nodesByUsernameQuery;
	}

}
