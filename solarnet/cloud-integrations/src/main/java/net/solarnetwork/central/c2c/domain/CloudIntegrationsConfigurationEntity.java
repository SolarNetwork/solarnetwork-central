/* ==================================================================
 * CloudIntegrationsConfigurationEntity.java - 4/10/2024 11:04:50 am
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

package net.solarnetwork.central.c2c.domain;

import java.util.Map;
import net.solarnetwork.central.dao.UserRelatedStdEntity;
import net.solarnetwork.central.domain.UserRelatedCompositeKey;
import net.solarnetwork.service.ServiceConfiguration;
import net.solarnetwork.util.StringUtils;

/**
 * API for cloud integration configuration entities.
 *
 * @param <C>
 *        the entity type
 * @param <K>
 *        the key type
 * @author matt
 * @version 1.1
 */
public interface CloudIntegrationsConfigurationEntity<C extends CloudIntegrationsConfigurationEntity<C, K>, K extends UserRelatedCompositeKey<K>>
		extends UserRelatedStdEntity<C, K> {

	/**
	 * A service property key for a map of named placeholder values, that can be
	 * used to resolve placeholder names, for example in source IDs or value
	 * references.
	 *
	 * @since 1.1
	 */
	String PLACEHOLDERS_SERVICE_PROPERTY = "placeholders";

	/**
	 * Test if this configuration is "fully" configured.
	 *
	 * <p>
	 * This can be used to filter out configurations that are only partially
	 * configured, and thus should not be used.
	 * </p>
	 *
	 * @return {@literal true} if this configuration contains all required
	 *         settings to function as intended
	 */
	boolean isFullyConfigured();

	/**
	 * Resolve placeholder values on a template string.
	 *
	 * <p>
	 * The template uses placeholder value in the form {@code {X}} where
	 * {@code X} is a placeholder name.
	 * </p>
	 *
	 * @param template
	 *        the template string to resolve placeholder values on
	 * @param configuration
	 *        the service configuration to obtain placeholder values from, on a
	 *        {@link #PLACEHOLDERS_SERVICE_PROPERTY} service property
	 * @return the {@code template} with all placeholder values resolved, or
	 *         {@literal null} if {@code template} is {@code null}
	 * @see StringUtils#expandTemplateString(String, Map)
	 * @since 1.1
	 */
	static String resolvePlaceholders(String template, ServiceConfiguration configuration) {
		if ( template == null || template.isEmpty() ) {
			return template;
		}
		@SuppressWarnings("unchecked")
		Map<String, ?> placeholders = configuration.serviceProperty(PLACEHOLDERS_SERVICE_PROPERTY,
				Map.class);
		return StringUtils.expandTemplateString(template, placeholders);
	}

}
