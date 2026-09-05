/* ==================================================================
 * UserAlertRendererResolver.java - 26/07/2020 3:13:19 PM
 *
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.biz;

import static net.solarnetwork.util.CollectionUtils.mapProperty;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.util.MimeType;
import net.solarnetwork.central.user.domain.User;
import net.solarnetwork.central.user.domain.UserAlertSituation;
import net.solarnetwork.service.TemplateRenderer;

/**
 * API for resolving a {@link TemplateRenderer} for rendering an alert.
 *
 * @author matt
 * @version 1.0
 */
public interface UserAlertRendererResolver {

	/**
	 * Resolve a renderer for a given alert situation and output
	 * characteristics.
	 *
	 * @param invoice
	 *        the invoice to be rendered
	 * @param mimeType
	 *        the desired output MIME type
	 * @param locale
	 *        the output locale
	 * @return the renderer, or {@code null} if none can be resolved
	 */
	@Nullable
	TemplateRenderer rendererForAlert(UserAlertSituation alert, MimeType mimeType, Locale locale);

	/**
	 * Get default template parameters for a given alert.
	 * 
	 * @param user
	 *        the alert user
	 * @param alert
	 *        the alert
	 * @return the parameters
	 */
	default Map<String, Object> templateParametersForAlert(final User user,
			final UserAlertSituation alert) {
		Map<String, Object> result = new LinkedHashMap<>(8);
		result.put("user", user);
		result.put("alert", alert.getAlert());
		result.put("stale", mapProperty("stale", List.class, List.of(), alert.getInfo()));
		return result;
	}

}
