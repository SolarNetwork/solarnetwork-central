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

import static java.time.Instant.now;
import static net.solarnetwork.central.test.CommonTestUtils.randomEmail;
import static net.solarnetwork.central.test.CommonTestUtils.randomInt;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomSourceId;
import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.InstanceOfAssertFactories.list;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
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
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamPK.NodeDatumStreamPK;
import net.solarnetwork.central.domain.SolarLocation;
import net.solarnetwork.central.support.CsvVersionedMessageDao;
import net.solarnetwork.central.support.SimpleCache;
import net.solarnetwork.central.support.VersionedMessageDaoMessageSource;
import net.solarnetwork.central.user.biz.UserAlertRendererResolver;
import net.solarnetwork.central.user.datum.alert.support.VersionedMessageSourceUserAlertRendererResolver;
import net.solarnetwork.central.user.domain.User;
import net.solarnetwork.central.user.domain.UserAlert;
import net.solarnetwork.central.user.domain.UserAlertOptions;
import net.solarnetwork.central.user.domain.UserAlertSituation;
import net.solarnetwork.central.user.domain.UserAlertSituationStatus;
import net.solarnetwork.common.tmpl.st4.ST4TemplateRenderer;
import net.solarnetwork.service.TemplateRenderer;
import net.solarnetwork.util.DateUtils;

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
		final SolarLocation loc = new SolarLocation();
		loc.setTimeZoneId("America/Los_Angeles");
		user.setLocation(loc);

		final UserAlert alert = new UserAlert();

		final UserAlertSituation sit = new UserAlertSituation();
		sit.setAlert(alert);

		final List<NodeDatumStreamPK> datum = List
				.of(new NodeDatumStreamPK(randomLong(), randomSourceId(), now()));
		final Locale locale = Locale.US;

		// WHEN
		Map<String, Object> result = resolver.templateParametersForAlert(user, sit, datum, locale);

		// THEN
		// @formatter:off
		then(result)
			.as("Template parameters created")
			.hasSize(4)
			;
		then(result.get(UserAlertRendererResolver.USER_PARAM)).as("User populated").isSameAs(user);
		then(result.get(UserAlertRendererResolver.SITUATION_PARAM)).as("Alert populated").isSameAs(sit);
		then(result.get(UserAlertRendererResolver.DATUM_IDENTIFIER_LIST_PARAM))
			.as("Datum info populated")
			.asInstanceOf(list(UserAlertRendererResolver.DatumStreamInfo.class))
			.hasSameSizeAs(datum)
			.element(0)
			.isEqualTo(new UserAlertRendererResolver.DatumStreamInfo(
					datum.getFirst().getNodeId(),
					datum.getFirst().getSourceId(),
					datum.getFirst().getTimestamp(),
					DateUtils.DISPLAY_DATE_LONG_TIME_SHORT.withLocale(locale).format(
							datum.getFirst().getTimestamp().atZone(ZoneId.of(loc.getTimeZoneId())))))
			;
		then(result.get(UserAlertRendererResolver.DESTINATION_EMAILS_PARAM))
			.as("Localized email list populated")
			.isEqualTo(user.getEmail())
			;
		// @formatter:on
	}

	@Test
	public void templateParams_withAgeAnd2DestList() {
		// GIVEN
		final User user = new User();
		final SolarLocation loc = new SolarLocation();
		loc.setTimeZoneId("America/Los_Angeles");
		user.setLocation(loc);

		final Integer age = 3600;
		final List<String> destEmails = List.of(randomEmail(), randomEmail());

		final UserAlert alert = new UserAlert();
		alert.setOptions(Map.of(
		// @formatter:off
				UserAlertOptions.AGE_THRESHOLD, age,
				UserAlertOptions.EMAIL_TOS, destEmails
				// @formatter:on
		));

		final UserAlertSituation sit = new UserAlertSituation();
		sit.setAlert(alert);

		final List<NodeDatumStreamPK> datum = List
				.of(new NodeDatumStreamPK(randomLong(), randomSourceId(), now()));
		final Locale locale = Locale.US;

		// WHEN
		Map<String, Object> result = resolver.templateParametersForAlert(user, sit, datum, locale);

		// THEN
		// @formatter:off
		then(result)
			.as("Template parameters created")
			.hasSize(5)
			;
		then(result.get(UserAlertRendererResolver.USER_PARAM)).as("User populated").isSameAs(user);
		then(result.get(UserAlertRendererResolver.SITUATION_PARAM)).as("Alert populated").isSameAs(sit);
		then(result.get(UserAlertRendererResolver.DATUM_IDENTIFIER_LIST_PARAM))
			.as("Datum info populated")
			.asInstanceOf(list(UserAlertRendererResolver.DatumStreamInfo.class))
			.hasSameSizeAs(datum)
			.element(0)
			.isEqualTo(new UserAlertRendererResolver.DatumStreamInfo(
					datum.getFirst().getNodeId(),
					datum.getFirst().getSourceId(),
					datum.getFirst().getTimestamp(),
					DateUtils.DISPLAY_DATE_LONG_TIME_SHORT.withLocale(locale).format(
							datum.getFirst().getTimestamp().atZone(ZoneId.of(loc.getTimeZoneId())))))
			;
		then(result.get(UserAlertRendererResolver.LOCALIZED_ALERT_AGE_PARAM))
			.as("Localized age populated")
			.isEqualTo("%s minutes".formatted(NumberFormat.getIntegerInstance(locale).format(age / 60)))
			;
		then(result.get(UserAlertRendererResolver.DESTINATION_EMAILS_PARAM))
			.as("Localized email list populated")
			.isEqualTo("%s and %s".formatted(destEmails.get(0), destEmails.get(1)))
			;
		// @formatter:on
	}

	@Test
	public void templateParams_with1DestList() {
		// GIVEN
		final User user = new User();
		final SolarLocation loc = new SolarLocation();
		loc.setTimeZoneId("America/Los_Angeles");
		user.setLocation(loc);

		final List<String> destEmails = List.of(randomEmail());

		final UserAlert alert = new UserAlert();
		alert.setOptions(Map.of(
		// @formatter:off
				UserAlertOptions.EMAIL_TOS, destEmails
				// @formatter:on
		));

		final UserAlertSituation sit = new UserAlertSituation();
		sit.setAlert(alert);

		final List<NodeDatumStreamPK> datum = List
				.of(new NodeDatumStreamPK(randomLong(), randomSourceId(), now()));
		final Locale locale = Locale.US;

		// WHEN
		Map<String, Object> result = resolver.templateParametersForAlert(user, sit, datum, locale);

		// THEN
		// @formatter:off
		then(result)
			.as("Template parameters created")
			.hasSize(4)
			;
		then(result.get(UserAlertRendererResolver.DESTINATION_EMAILS_PARAM))
			.as("Localized email list populated")
			.isEqualTo("%s".formatted(destEmails.get(0)))
			;
		// @formatter:on
	}

	@Test
	public void templateParams_with3DestList() {
		// GIVEN
		final User user = new User();
		final SolarLocation loc = new SolarLocation();
		loc.setTimeZoneId("America/Los_Angeles");
		user.setLocation(loc);

		final List<String> destEmails = List.of(randomEmail(), randomEmail(), randomEmail());

		final UserAlert alert = new UserAlert();
		alert.setOptions(Map.of(
		// @formatter:off
				UserAlertOptions.EMAIL_TOS, destEmails
				// @formatter:on
		));

		final UserAlertSituation sit = new UserAlertSituation();
		sit.setAlert(alert);

		final List<NodeDatumStreamPK> datum = List
				.of(new NodeDatumStreamPK(randomLong(), randomSourceId(), now()));
		final Locale locale = Locale.US;

		// WHEN
		Map<String, Object> result = resolver.templateParametersForAlert(user, sit, datum, locale);

		// THEN
		// @formatter:off
		then(result)
			.as("Template parameters created")
			.hasSize(4)
			;
		then(result.get(UserAlertRendererResolver.DESTINATION_EMAILS_PARAM))
			.as("Localized email list populated")
			.isEqualTo("%s, %s, and %s".formatted(destEmails.get(0), destEmails.get(1), destEmails.get(2)))
			;
		// @formatter:on
	}

	@Test
	public void renderHtml_oneStale() throws IOException {
		// GIVEN
		final Locale locale = Locale.ENGLISH;
		final MimeType mimeType = MimeTypeUtils.TEXT_HTML;

		final Integer age = randomInt();
		final String sourceId = randomSourceId();

		final User user = new User();
		user.setEmail(randomEmail());
		final SolarLocation loc = new SolarLocation();
		loc.setTimeZoneId("Europe/London");
		user.setLocation(loc);

		final UserAlert alert = new UserAlert();
		alert.setNodeId(randomLong());
		// @formatter:off
		alert.setOptions(Map.of(
				UserAlertOptions.AGE_THRESHOLD, age,
				UserAlertOptions.SOURCE_IDS, List.of(sourceId)
		));
		// @formatter:on

		final UserAlertSituation sit = new UserAlertSituation();
		sit.setAlert(alert);

		final List<NodeDatumStreamPK> datum = List
				.of(new NodeDatumStreamPK(alert.getNodeId(), sourceId, now()));

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
		final Map<String, Object> templateParameters = resolver.templateParametersForAlert(user, sit, datum, locale);
		templateParameters.put("messages", messageSource.propertiesForLocale(locale));
		
		final ByteArrayOutputStream byos = new ByteArrayOutputStream();
		renderer.render(locale, mimeType, templateParameters, byos);

		final String result = byos.toString(StandardCharsets.UTF_8);
		then(result)
			.as("HTML generated")
			.isNotNull()
			.isEqualToIgnoringWhitespace("""
				<html><head><meta charset="UTF-8">
				<title>SolarNetwork stale datum alert</title>
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
				This is an automated <b>stale datum alert</b> email from SolarNetwork.
				The following sources have not posted data in more than <b>%4$s minutes</b>:
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
					This automated email was sent to %5$s because of a stale datum alert configured in
					the <a href="https://data.solarnetwork.net/solaruser/u/sec/alerts">SolarNetwork
					account</a> of %6$s.
					</p>
				</footer></body></html>					
				""".formatted(
						  alert.getNodeId()
						, sourceId
						, DateUtils.DISPLAY_DATE_LONG_TIME_SHORT.withLocale(locale).format(
							datum.getFirst().getTimestamp().atZone(ZoneId.of(loc.getTimeZoneId())))
						, NumberFormat.getIntegerInstance(locale).format(age / 60)
						, user.getEmail()
						, user.getEmail()
					)
			)
			;
		// @formatter:on
	}

	@Test
	public void renderHtml_oneStale_resolved() throws IOException {
		// GIVEN
		final Locale locale = Locale.ENGLISH;
		final MimeType mimeType = MimeTypeUtils.TEXT_HTML;

		final Integer age = randomInt();
		final String sourceId = randomSourceId();

		final User user = new User();
		user.setEmail(randomEmail());
		final SolarLocation loc = new SolarLocation();
		loc.setTimeZoneId("Europe/London");
		user.setLocation(loc);

		final UserAlert alert = new UserAlert();
		alert.setNodeId(randomLong());
		// @formatter:off
		alert.setOptions(Map.of(
				UserAlertOptions.AGE_THRESHOLD, age,
				UserAlertOptions.SOURCE_IDS, List.of(sourceId)
		));
		// @formatter:on

		final UserAlertSituation sit = new UserAlertSituation();
		sit.setAlert(alert);
		sit.setStatus(UserAlertSituationStatus.Resolved);

		final List<NodeDatumStreamPK> datum = List
				.of(new NodeDatumStreamPK(alert.getNodeId(), sourceId, now()));

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
		final Map<String, Object> templateParameters = resolver.templateParametersForAlert(user, sit, datum, locale);
		templateParameters.put("messages", messageSource.propertiesForLocale(locale));
		
		final ByteArrayOutputStream byos = new ByteArrayOutputStream();
		renderer.render(locale, mimeType, templateParameters, byos);

		final String result = byos.toString(StandardCharsets.UTF_8);
		then(result)
			.as("HTML generated")
			.isNotNull()
			.isEqualToIgnoringWhitespace("""
				<html><head><meta charset="UTF-8">
				<title>SolarNetwork stale datum alert resolved</title>
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
				This is an automated stale datum <b>resolved</b> email from SolarNetwork.
				The following sources <b>are no longer considered stale</b> because they have have posted
				data within the past <b>%4$s minutes</b>:
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
					This automated email was sent to %5$s because of a stale datum alert configured in
					the <a href="https://data.solarnetwork.net/solaruser/u/sec/alerts">SolarNetwork
					account</a> of %6$s.
					</p>
				</footer></body></html>					
				""".formatted(
						  alert.getNodeId()
						, sourceId
						, DateUtils.DISPLAY_DATE_LONG_TIME_SHORT.withLocale(locale).format(
							datum.getFirst().getTimestamp().atZone(ZoneId.of(loc.getTimeZoneId())))
						, NumberFormat.getIntegerInstance(locale).format(age / 60)
						, user.getEmail()
						, user.getEmail()
					)
			)
			;
		// @formatter:on
	}

	@Test
	public void renderHtml_multiStale_multiDest() throws IOException {
		// GIVEN
		final Locale locale = Locale.ENGLISH;
		final MimeType mimeType = MimeTypeUtils.TEXT_HTML;

		final Integer age = randomInt();
		final String sourceId1 = randomSourceId();
		final String sourceId2 = randomSourceId();
		final List<String> destEmails = List.of(randomEmail(), randomEmail());

		final User user = new User();
		user.setEmail(randomEmail());
		final SolarLocation loc = new SolarLocation();
		loc.setTimeZoneId("Europe/London");
		user.setLocation(loc);

		final UserAlert alert = new UserAlert();
		alert.setNodeId(randomLong());
		// @formatter:off
		alert.setOptions(Map.of(
				UserAlertOptions.AGE_THRESHOLD, age,
				UserAlertOptions.SOURCE_IDS, List.of(sourceId1),
				UserAlertOptions.EMAIL_TOS, destEmails
		));
		// @formatter:on

		final UserAlertSituation sit = new UserAlertSituation();
		sit.setAlert(alert);

		final List<NodeDatumStreamPK> datum = List.of(
				new NodeDatumStreamPK(alert.getNodeId(), sourceId1, now()),
				new NodeDatumStreamPK(alert.getNodeId(), sourceId2, now().minusSeconds(age)));

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
		final Map<String, Object> templateParameters = resolver.templateParametersForAlert(user, sit, datum, locale);
		templateParameters.put("messages", messageSource.propertiesForLocale(locale));
		
		final ByteArrayOutputStream byos = new ByteArrayOutputStream();
		renderer.render(locale, mimeType, templateParameters, byos);

		final String result = byos.toString(StandardCharsets.UTF_8);
		then(result)
			.as("HTML generated")
			.isNotNull()
			.isEqualToIgnoringWhitespace("""
				<html><head><meta charset="UTF-8">
				<title>SolarNetwork stale datum alert</title>
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
				This is an automated <b>stale datum alert</b> email from SolarNetwork.
				The following sources have not posted data in more than <b>%7$s minutes</b>:
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
						<tr>
							<th>%4$d</th>
							<th>%5$s</th>
							<td>%6$s</td>
						</tr>	
					</tbody>
				</table>
				<footer>
					<p>
					This automated email was sent to %8$s because of a stale datum alert configured in
					the <a href="https://data.solarnetwork.net/solaruser/u/sec/alerts">SolarNetwork
					account</a> of %9$s.
					</p>
				</footer></body></html>					
				""".formatted(
						  alert.getNodeId()
						, sourceId1
						, DateUtils.DISPLAY_DATE_LONG_TIME_SHORT.withLocale(locale).format(
							datum.getFirst().getTimestamp().atZone(ZoneId.of(loc.getTimeZoneId())))
						, alert.getNodeId()
						, sourceId2
						, DateUtils.DISPLAY_DATE_LONG_TIME_SHORT.withLocale(locale).format(
							datum.getLast().getTimestamp().atZone(ZoneId.of(loc.getTimeZoneId())))
						, NumberFormat.getIntegerInstance(locale).format(age / 60)
						, "%s and %s".formatted(destEmails.get(0), destEmails.get(1))
						, user.getEmail()
					)
			)
			;
		// @formatter:on
	}

}
