/* ==================================================================
 * EnvironmentConfig.java - Nov 25, 2013 4:05:06 PM
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

package net.solarnetwork.central.cassandra.config;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import javax.annotation.Resource;
import net.solarnetwork.central.datum.domain.SkyCondition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

/**
 * Bean configuration for the environment.
 * 
 * @author matt
 * @version 1.0
 */
@Configuration
@ImportResource("classpath:env.xml")
public class EnvironmentConfig {

	@Resource(name = "skyConditionMap")
	private Map<String, SkyCondition> skyConditionMap;

	@Bean
	public Map<Pattern, SkyCondition> skyConditionMapping() {
		Map<Pattern, SkyCondition> map = new LinkedHashMap<Pattern, SkyCondition>();
		for ( Map.Entry<String, SkyCondition> me : skyConditionMap.entrySet() ) {
			Pattern p = Pattern.compile(me.getKey(), Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
			map.put(p, me.getValue());
		}
		return map;
	}

}
