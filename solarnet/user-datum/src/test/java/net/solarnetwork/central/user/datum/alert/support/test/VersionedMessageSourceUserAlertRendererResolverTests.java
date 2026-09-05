/* ==================================================================
 * ST4DatumAlertTemplateRendererTests.java - 5 Sept 2026 7:22:12 pm
 * 
 * Copyright 2026 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.datum.alert.support.test;

import static net.solarnetwork.central.test.CommonTestUtils.randomEmail;
import static net.solarnetwork.central.test.CommonTestUtils.randomInt;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomSourceId;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static org.assertj.core.api.BDDAssertions.then;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.cache.Cache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import net.solarnetwork.central.dao.VersionedMessageDao.VersionedMessages;
import net.solarnetwork.central.support.CsvVersionedMessageDao;
import net.solarnetwork.central.support.SimpleCache;
import net.solarnetwork.central.support.VersionedMessageDaoMessageSource;
import net.solarnetwork.central.user.datum.alert.support.VersionedMessageSourceUserAlertRendererResolver;
import net.solarnetwork.central.user.domain.User;
import net.solarnetwork.central.user.domain.UserAlert;
import net.solarnetwork.central.user.domain.UserAlertSituation;
import net.solarnetwork.common.tmpl.st4.ST4TemplateRenderer;
import net.solarnetwork.service.TemplateRenderer;

/**
 * Test cases for the {@link VersionedMessageSourceUserAlertRendererResolver}
 * class.
 * 
 * @author matt
 * @version 1.0
 */
public class VersionedMessageSourceUserAlertRendererResolverTests {

	private CsvVersionedMessageDao messageDao;
	private Cache<String, VersionedMessages> messageCache;
	private Cache<String, ST4TemplateRenderer> templateCache;

	private VersionedMessageSourceUserAlertRendererResolver resolver;

	@BeforeEach
	public void setup() {
		messageDao = new CsvVersionedMessageDao(
				List.of(new ClassPathResource("messages.csv", getClass())));
		messageCache = new SimpleCache<>("TestMessageCache");
		templateCache = new SimpleCache<>("TestTemplateCache");
		resolver = new VersionedMessageSourceUserAlertRendererResolver(
				"/snf/text/html/stale-datum-alert", "alert", MimeTypeUtils.TEXT_HTML, messageDao,
				messageCache, templateCache);
	}

	@Test
	public void templateParams() {
		// GIVEN

		final User user = new User();
		final UserAlert alert = new UserAlert();
		final UserAlertSituation sit = new UserAlertSituation();
		sit.setAlert(alert);
		// @formatter:off
		final List<Map<String, Object>> staleInfo = List.of(
				Map.of()
		);
		sit.setInfo(Map.of("stale", staleInfo));
		// @formatter:on

		// WHEN
		Map<String, Object> result = resolver.templateParametersForAlert(user, sit);

		// THEN
		// @formatter:off
		then(result)
			.as("Template parameters created")
			.hasSize(3)
			;
		then(result.get("user")).as("User populated").isSameAs(user);
		then(result.get("alert")).as("Alert populated").isSameAs(alert);
		then(result.get("stale")).as("Stale info populated").isSameAs(staleInfo);
		// @formatter:on
	}

	@Test
	public void renderHtml_oneStale() throws IOException {
		// GIVEN
		final Locale locale = Locale.ENGLISH;
		final MimeType mimeType = MimeTypeUtils.TEXT_HTML;

		final Integer age = randomInt();
		final String sourceId = randomSourceId();
		final String localizedTimestamp = randomString();

		final User user = new User();
		user.setEmail(randomEmail());

		final UserAlert alert = new UserAlert();
		alert.setNodeId(randomLong());
		// @formatter:off
		alert.setOptions(Map.of(
				"age", age,
				"sourceIds", List.of(sourceId)
		));
		// @formatter:on

		final UserAlertSituation sit = new UserAlertSituation();
		sit.setAlert(alert);
		// @formatter:off
		sit.setInfo(Map.of(
				"stale", List.of(
						Map.of(
								"nodeId", alert.getNodeId(),
								"sourceId", sourceId,
								"localizedTimestamp", localizedTimestamp
						)
				)
		));
		// @formatter:on

		// WHEN
		TemplateRenderer renderer = resolver.rendererForAlert(sit, mimeType, locale);

		// THEN
		// @formatter:off
		then(renderer)
			.as("Renderer returned for HTML")
			.isNotNull()
			;
		
		final var messageSource =  new VersionedMessageDaoMessageSource(messageDao, new String[] {"snf.stale-datum-alert"}, Instant.now(),
				messageCache);
		final Map<String, Object> templateParameters = resolver.templateParametersForAlert(user, sit);
		templateParameters.put("messages", messageSource.propertiesForLocale(locale));
		
		final ByteArrayOutputStream byos = new ByteArrayOutputStream();
		renderer.render(locale, mimeType, templateParameters, byos);

		final String result = byos.toString(StandardCharsets.UTF_8);
		then(result)
			.as("HTML generated")
			.isNotNull()
			.isEqualToIgnoringWhitespace("""
				<html><head><meta charset="UTF-8">
				<title>SolarNetwork alert: %1$d</title>
				<style type="text/css">
					body { 
						font-family: sans-serif;
						background-color: #FFF;  
						margin: 2rem; }
					footer { border-top:2px solid #F7C819;color:#666;margin-top:2rem; }
					footer > p { margin-top:4px; margin-bottom: 4px; font-size:0.8rem; }
					footer > p + p { border-top: 1px solid #ccc; padding-top: 2px; }
					table { width: 100%%; }
					th.hr { border-top: 2px solid black; }
					td.hr { border-top: 1px solid #ccc; }
					th { text-align:left; }
					.items tbody td,
					.items tbody th,
					.items tfoot td,
					.items tfoot th {
						font-size:0.8rem;
					}
					.items td { padding-top: 2px; padding-bottom: 2px; vertical-align: text-bottom; }
					.items tr *:last-child { text-align: right; }
					.items th { width: auto; }
				</style></head><body><p>
				This is an automated alert email from SolarNetwork. The following sources are stale:
				</p>
				<table class="items">
					<thead>
						<tr>
							<th>Node</th>
							<th>Source</th>
							<th>Last datum date</th>
						</tr>
						<tr>
							<th colspan="3" class="hr"></th>
						</tr>
					</thead>
					<tbody>
						<tr>
							<th>%1$d</th>
							<th>%2$s</th>
							<td>%3$s</td>
						</tr>	
					</tbody>
				</table>
				<footer>
					<p>
					This automated email was sent to  because of an alert configured for SolarNode
					%1$d. You can manage your alert settings by logging in to your
					<a href="https://data.solarnetwork.net/solaruser/u/sec/alerts">SolarNetwork account</a>.
					</p>
				</footer></body></html>					
				""".formatted(alert.getNodeId(), sourceId, localizedTimestamp))
			;
		// @formatter:on
	}

}
