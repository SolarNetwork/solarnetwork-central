/* ==================================================================
 * WeatherMappingTool.java - Nov 25, 2013 3:46:33 PM
 * 
 * Copyright 2007-2013 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.cassandra;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.regex.Pattern;
import net.solarnetwork.central.cassandra.config.WeatherMappingToolConfig;
import net.solarnetwork.central.datum.domain.SkyCondition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.jdbc.core.PreparedStatementCreator;

/**
 * Tool to test weather mapping.
 * 
 * @author matt
 * @version 1.0
 */
public class WeatherMappingTool {

	private final JdbcOperations jdbcOperations;
	private final Map<Pattern, SkyCondition> skyConditionMapping;

	private final Logger log = LoggerFactory.getLogger(getClass());

	public WeatherMappingTool(JdbcOperations ops, Map<Pattern, SkyCondition> skyConditionMapping) {
		super();
		this.jdbcOperations = ops;
		this.skyConditionMapping = skyConditionMapping;
	}

	public void go() {
		jdbcOperations.execute(new PreparedStatementCreator() {

			@Override
			public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
				return con
						.prepareCall("SELECT DISTINCT sky FROM solarnet.sn_weather_datum ORDER BY sky");
			}
		}, new PreparedStatementCallback<Object>() {

			@Override
			public Object doInPreparedStatement(PreparedStatement ps) throws SQLException,
					DataAccessException {
				ResultSet rs = ps.executeQuery();
				while ( rs.next() ) {
					String condition = rs.getString(1);
					SkyCondition sky = SkyCondition.mapStringValue(condition, skyConditionMapping);
					log.info("{}: {} => {}", (sky == null ? "FAIL" : "PASS"), condition, sky);
				}
				return null;
			}
		});
	}

	/**
	 * Execute the tool.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		ApplicationContext ctx = new AnnotationConfigApplicationContext(WeatherMappingToolConfig.class);
		WeatherMappingTool t = ctx.getBean(WeatherMappingTool.class);
		t.go();
	}

}
