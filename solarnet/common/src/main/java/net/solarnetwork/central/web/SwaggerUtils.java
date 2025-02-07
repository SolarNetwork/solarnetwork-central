/* ==================================================================
 * SwaggerUtils.java - 31/01/2025 11:18:55 am
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

package net.solarnetwork.central.web;

import static org.apache.commons.lang3.StringUtils.splitByCharacterTypeCamelCase;
import java.util.Comparator;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ClassInfoList;
import io.github.classgraph.ScanResult;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.converter.ResolvedSchema;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.tags.Tag;
import net.solarnetwork.dao.FilterResults;

/**
 * Swagger documentation utilities.
 * 
 * @author matt
 * @version 1.0
 */
public class SwaggerUtils {

	private static final Logger log = LoggerFactory.getLogger(SwaggerUtils.class);

	/**
	 * Sort API tags by name.
	 * 
	 * <p>
	 * Names are split on the {@code -} character, and each component compared
	 * in order.
	 * </p>
	 */
	public static class ApiTagSorter implements Comparator<Tag> {

		@Override
		public int compare(Tag a, Tag b) {
			var ac = a.getName().split("-");
			var bc = b.getName().split("-");
			var acLen = ac.length;
			var bcLen = bc.length;
			for ( int i = 0, len = Math.min(acLen, bcLen); i < len; i += 1 ) {
				int res = ac[i].compareToIgnoreCase(bc[i]);
				if ( res != 0 ) {
					return res;
				} else if ( i + 1 == acLen ) {
					return -1;
				} else if ( i + 1 == bcLen ) {
					return 1;
				}
			}
			return 0;
		}

	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void fixupFilterResultsSchemas(OpenAPI api) {
		Map<String, Schema> schemas = api.getComponents().getSchemas();
		ScanResult cpScanResult = null;
		ClassInfoList cpInfoList = null;
		try {
			// we might mutate the schemas map, so iterate over array copy of current names
			for ( String schemaName : schemas.keySet().toArray(String[]::new) ) {
				Schema s = schemas.get(schemaName);
				if ( !s.getName().startsWith(FilterResults.class.getSimpleName()) ) {
					continue;
				}
				Object resProp = s.getProperties().get("results");
				if ( resProp instanceof Schema ) {
					// The schema name will be along the lines of FilterResultsMyType, but for types that are
					// themselves generic then FilterResultsMyTypeGeneric. We take the part of the name after
					// FilterResults and split that by CamelCase into potential component schema names,
					// starting with the full name and dropping the last CamelCase component until we find a
					// match. For example FilterResultsUserEntityLong would look for UserEntityLong,
					// UserEntity, and User.
					boolean fixed = false;
					String[] nameComponents = splitByCharacterTypeCamelCase(s.getName().substring(13));
					for ( int i = nameComponents.length; i > 0; i-- ) {
						String targetSchemaName = StringUtils.join(nameComponents, "", 0, i);
						if ( !schemas.containsKey(targetSchemaName) ) {
							continue;
						}
						log.info(
								"Fixing FilterResults component schema {} results property as array of {}",
								s.getName(), targetSchemaName);
						ArraySchema targetArraySchema = new ArraySchema();
						Schema targetArrayItemSchema = new Schema();
						targetArrayItemSchema.set$ref("#/components/schemas/" + targetSchemaName);
						targetArraySchema.items(targetArrayItemSchema);
						s.getProperties().put("results", targetArraySchema);
						fixed = true;
						break;
					}
					if ( !fixed ) {
						// search classpath for class with simple name that matches schema name,
						// and dynamically add that schema if one is found, and then associate
						// the schema with the FilterResults
						for ( int i = nameComponents.length; i > 0; i-- ) {
							String targetSchemaName = StringUtils.join(nameComponents, "", 0, i);
							if ( cpInfoList == null ) {
								cpScanResult = new ClassGraph().enableClassInfo()
										.acceptPackages("net.solarnetwork").scan();
								cpInfoList = cpScanResult.getAllClasses();
							}
							ClassInfo classInfo = cpInfoList.stream()
									.filter(c -> targetSchemaName.equals(c.getSimpleName())).findAny()
									.orElse(null);
							if ( classInfo != null ) {
								// create schema for target
								ResolvedSchema resolvedSchema = ModelConverters.getInstance()
										.resolveAsResolvedSchema(
												new AnnotatedType(classInfo.loadClass()));
								api.schema(targetSchemaName, resolvedSchema.schema);
								log.info(
										"Fixing FilterResults component schema {} results property as array of {}",
										s.getName(), targetSchemaName);
								ArraySchema targetArraySchema = new ArraySchema();
								Schema targetArrayItemSchema = new Schema();
								targetArrayItemSchema
										.set$ref("#/components/schemas/" + targetSchemaName);
								targetArraySchema.items(targetArrayItemSchema);
								s.getProperties().put("results", targetArraySchema);
								fixed = true;
								break;
							}
						}
					}
				}
			}
		} finally {
			if ( cpScanResult != null ) {
				cpScanResult.close();
			}
		}
	}

}
