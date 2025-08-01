/* ==================================================================
 * SpringdocConfig.java - 31/01/2025 11:05:18 am
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

package net.solarnetwork.central.query.config;

import static java.util.stream.Collectors.toMap;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.info.Info;
import net.solarnetwork.central.web.SwaggerUtils;
import net.solarnetwork.dao.FilterResults;

/**
 * Configuration for Springdoc OpenAPI v3 generation.
 * 
 * @author matt
 * @version 1.1
 */
@Configuration
public class SpringdocConfig {

	@Bean
	public OpenAPI customOpenAPI(@Value("${app.meta.api-version}") String appApiVersion) {
		// @formatter:off
	    return new OpenAPI()
	        .info(new Info()
	            .title("SolarQuery Application")
	            .version(appApiVersion)
	            .termsOfService("https://solarnetwork.net/legal.html"));
	    // @formatter:on
	}

	@Bean
	public OpenApiCustomizer sortTagsAndPaths() {
		return (api) -> {
			if ( api.getTags() != null ) {
				api.setTags(api.getTags().stream().sorted(new SwaggerUtils.ApiTagSorter()).toList());
			}
			if ( api.getPaths() != null ) {
				api.setPaths(api.getPaths().entrySet().stream().sorted(new SwaggerUtils.PathsSorter())
						.collect(toMap(e -> e.getKey(), e -> e.getValue(), (l, r) -> l, Paths::new)));
			}

		};
	}

	/**
	 * Customize OpenAPI {@link FilterResults} component {@code results}
	 * properties into array schemas of the appropriate item type.
	 * 
	 * @return the customizer
	 */
	@Bean
	public OpenApiCustomizer filterResultsComponentCustomizer() {
		return (api) -> {
			SwaggerUtils.fixupFilterResultsSchemas(api);
		};
	}

}
