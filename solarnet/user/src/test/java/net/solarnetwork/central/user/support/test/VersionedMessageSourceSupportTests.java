/* ==================================================================
 * VersionedMessageSourceSupportTests.java - 15/10/2026 12:41:05 pm
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

package net.solarnetwork.central.user.support.test;

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
import org.springframework.util.MimeTypeUtils;
import net.solarnetwork.central.dao.VersionedMessageDao;
import net.solarnetwork.central.dao.VersionedMessageDao.VersionedMessages;
import net.solarnetwork.central.support.CsvVersionedMessageDao;
import net.solarnetwork.central.support.SimpleCache;
import net.solarnetwork.central.user.support.VersionedMessageSourceSupport;
import net.solarnetwork.common.tmpl.st4.ST4TemplateRenderer;
import net.solarnetwork.service.TemplateRenderer;

/**
 * Test cases for the {@link VersionedMessageSourceSupport} class.
 *
 * @author matt
 * @version 1.0
 */
public class VersionedMessageSourceSupportTests {

	private static final Instant VERSION_1 = Instant.parse("2001-06-01T00:00:00Z");
	private static final Instant VERSION_2 = Instant.parse("2002-06-01T00:00:00Z");

	private VersionedMessageDao messageDao;
	private Cache<String, VersionedMessages> messageCache;
	private Cache<String, ST4TemplateRenderer> templateCache;

	private static final class TestRendererResolver extends VersionedMessageSourceSupport {

		private TestRendererResolver(String bundleName, List<String> importedBundleNames,
				VersionedMessageDao messageDao, Cache<String, VersionedMessages> messageCache,
				Cache<String, ST4TemplateRenderer> templateCache) {
			super(bundleName, importedBundleNames, "main", MimeTypeUtils.TEXT_HTML, messageDao,
					messageCache, templateCache);
		}

		private TemplateRenderer renderer(Instant version) {
			return rendererForVersion(version, MimeTypeUtils.TEXT_HTML, Locale.ENGLISH);
		}

	}

	@BeforeEach
	public void setup() {
		messageDao = new CsvVersionedMessageDao(
				List.of(new ClassPathResource("imports-messages.csv", getClass())));
		messageCache = new SimpleCache<>("TestMessageCache");
		templateCache = new SimpleCache<>("TestTemplateCache");
	}

	private static String render(TemplateRenderer renderer) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		renderer.render(Locale.ENGLISH, MimeTypeUtils.TEXT_HTML, Map.of("name", "World"), out);
		return out.toString(StandardCharsets.UTF_8);
	}

	@Test
	public void importedTemplates() throws IOException {
		// GIVEN
		TestRendererResolver resolver = new TestRendererResolver("/test/main",
				List.of("/test/parts"), messageDao, messageCache, templateCache);

		// WHEN
		String result = render(resolver.renderer(VERSION_1));

		// THEN
		// @formatter:off
		then(result)
			.as("Templates of the imported bundle used")
			.isEqualTo("<p>Hello World</p><footer>parts</footer>")
			;
		// @formatter:on
	}

	@Test
	public void importedTemplates_newVersion() throws IOException {
		// GIVEN
		TestRendererResolver resolver = new TestRendererResolver("/test/main",
				List.of("/test/parts"), messageDao, messageCache, templateCache);
		TemplateRenderer renderer1 = resolver.renderer(VERSION_1);

		// WHEN
		TemplateRenderer renderer2 = resolver.renderer(VERSION_2);
		String result = render(renderer2);

		// THEN
		// @formatter:off
		then(renderer2)
			.as("New renderer for a new version of only the imported bundle")
			.isNotSameAs(renderer1)
			;
		then(result)
			.as("New version of imported template used")
			.isEqualTo("<p>Hi World</p><footer>parts</footer>")
			;
		then(resolver.renderer(VERSION_1))
			.as("Renderer cached by version")
			.isSameAs(renderer1)
			;
		// @formatter:on
	}

	@Test
	public void importedTemplates_overridden() throws IOException {
		// GIVEN
		TestRendererResolver resolver = new TestRendererResolver("/test/override",
				List.of("/test/parts"), messageDao, messageCache, templateCache);

		// WHEN
		String result = render(resolver.renderer(VERSION_1));

		// THEN
		// @formatter:off
		then(result)
			.as("Template of the main bundle used instead of the imported one of the same name")
			.isEqualTo("<p>Hello World</p><footer>override</footer>")
			;
		// @formatter:on
	}

	@Test
	public void noImports() throws IOException {
		// GIVEN
		TestRendererResolver resolver = new TestRendererResolver("/test/main", List.of(), messageDao,
				messageCache, templateCache);

		// WHEN
		String result = render(resolver.renderer(VERSION_1));

		// THEN
		// @formatter:off
		then(result)
			.as("Missing templates render as empty")
			.isEqualTo("<p> World</p>")
			;
		// @formatter:on
	}

}
