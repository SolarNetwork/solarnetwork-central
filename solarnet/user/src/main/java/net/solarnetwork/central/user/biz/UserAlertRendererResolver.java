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

import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import org.jspecify.annotations.Nullable;
import org.springframework.util.MimeType;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamPK.NodeDatumStreamPK;
import net.solarnetwork.central.user.domain.User;
import net.solarnetwork.central.user.domain.UserAlertSituation;
import net.solarnetwork.service.TemplateRenderer;
import net.solarnetwork.util.DateUtils;

/**
 * API for resolving a {@link TemplateRenderer} for rendering an alert.
 *
 * @author matt
 * @version 1.0
 */
public interface UserAlertRendererResolver {

	/** A template parameter name for a {@code User} instance. */
	String USER_PARAM = "user";

	/** A template parameter name for an {@code UserAlert} instance. */
	String ALERT_PARAM = "alert";

	/**
	 * A template parameter name for a {@code List<DatumStreamInfo>} datum
	 * stream information list.
	 */
	String DATUM_IDENTIFIER_LIST_PARAM = "datum";

	/** A template parameter name for a localized alert display age. */
	String LOCALIZED_ALERT_AGE_PARAM = "localizedAlertAge";

	/** A template parameter name for the destination email. */
	String DESTINATION_EMAILS_PARAM = "destinationEmails";

	/**
	 * A node stream template information record.
	 * 
	 * @param nodeId
	 *        the node ID
	 * @param sourceId
	 *        the source ID
	 * @param timestamp
	 *        the datum timestamp
	 * @param localizedTimestamp
	 *        the localized formatted timestamp
	 */
	record DatumStreamInfo(Long nodeId, String sourceId, Instant timestamp, String localizedTimestamp) {

	}

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
	 * @param situation
	 *        the alert situation
	 * @param datum
	 *        the datum identifier list
	 * @param locale
	 *        the locale to use
	 * @return the parameters
	 */
	default Map<String, Object> templateParametersForAlert(final User user,
			final UserAlertSituation situation, List<NodeDatumStreamPK> datum, Locale locale) {
		final ZoneId tz = user.timeZone();
		final DateTimeFormatter formatter = DateUtils.DISPLAY_DATE_LONG_TIME_SHORT.withLocale(locale)
				.withZone(tz);
		final Map<String, Object> result = new LinkedHashMap<>(8);
		result.put(USER_PARAM, user);
		result.put(ALERT_PARAM, situation.getAlert());
		result.put(DATUM_IDENTIFIER_LIST_PARAM,
				datum.stream().map(id -> new DatumStreamInfo(id.getNodeId(), id.getSourceId(),
						id.getTimestamp(), formatter.format(id.getTimestamp()))).toList());

		final ResourceBundle bundle = ResourceBundle.getBundle(UserAlertRendererResolver.class.getName(),
				locale);

		String[] destEmails = situation.getAlert().optionEmailTos();
		if ( destEmails == null || destEmails.length < 1 ) {
			destEmails = new String[] { user.getEmail() };
		}
		final StringBuilder locDestEmails = new StringBuilder();
		for ( int i = 0, max = destEmails.length - 1; i <= max; i++ ) {
			if ( i > 0 ) {
				if ( max > 1 ) {
					locDestEmails.append(',');
				}
				locDestEmails.append(' ');
				if ( max > 0 && i == max ) {
					locDestEmails.append(bundle.getString("and")).append(' ');
				}
			}
			locDestEmails.append(destEmails[i]);
		}
		result.put(DESTINATION_EMAILS_PARAM, locDestEmails.toString());

		final Integer ageThreshold = situation.getAlert().optionAgeThreshold();
		if ( ageThreshold != null ) {
			NumberFormat numFmt = NumberFormat.getIntegerInstance(locale);
			result.put(LOCALIZED_ALERT_AGE_PARAM,
					"%s %s".formatted(numFmt.format(ageThreshold / 60), bundle.getString("minutes")));
		}

		return result;
	}

}
