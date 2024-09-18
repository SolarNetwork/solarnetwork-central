/* ==================================================================
 * BaseXsltServiceTests.java - 19/09/2024 6:58:17 am
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

package net.solarnetwork.central.support.xslt.test;

import static org.assertj.core.api.BDDAssertions.then;
import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.URIResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.sf.saxon.TransformerFactoryImpl;
import net.solarnetwork.central.support.SharedValueCache;
import net.solarnetwork.central.support.xslt.BaseXsltService;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.domain.BasicIdentifiableConfiguration;
import net.solarnetwork.service.IdentifiableConfiguration;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.util.ClassUtils;

/**
 * Test cases for the {@link BaseXsltService} class.
 * 
 * @author matt
 * @version 1.0
 */
public class BaseXsltServiceTests {

	private static class TestXsltService extends BaseXsltService {

		private TestXsltService(DocumentBuilderFactory documentBuilderFactory,
				TransformerFactory transformerFactory, ObjectMapper objectMapper,
				Duration templatesCacheTtl) {
			super("test", documentBuilderFactory, transformerFactory, objectMapper, templatesCacheTtl);
		}

		@Override
		public String getDisplayName() {
			return "";
		}

		@Override
		public List<SettingSpecifier> getSettingSpecifiers() {
			return Collections.emptyList();
		}

		public static String inputTextValue(Object object) throws IOException {
			return inputText(object);
		}

		@Override
		public long templatesCacheTtlSeconds(IdentifiableConfiguration config) {
			return super.templatesCacheTtlSeconds(config);
		}

		public Duration getTemplatesCacheTtl() {
			return templatesCacheTtl;
		}

		public SharedValueCache<String, Templates, String> getTemplatesCache() {
			return templatesCache;
		}

		@Override
		public Templates templates(String xslt, IdentifiableConfiguration config, Object cacheKey)
				throws IOException {
			return super.templates(xslt, config, cacheKey);
		}

	}

	private static class TestUriResolver implements URIResolver {

		@Override
		public Source resolve(String href, String base) throws TransformerException {
			throw new UnsupportedOperationException();
		}

	}

	private DocumentBuilderFactory dbf;
	private TransformerFactoryImpl tf;
	private TestXsltService service;

	@BeforeEach
	public void setup() throws Exception {
		dbf = DocumentBuilderFactory.newNSInstance();
		dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
		tf = new net.sf.saxon.TransformerFactoryImpl();
		tf.setURIResolver(new TestUriResolver());

		service = new TestXsltService(dbf, tf, JsonUtils.newDatumObjectMapper(), Duration.ofSeconds(60));
	}

	@Test
	public void stripDoctype() throws IOException {
		// GIVEN
		final String xmlInput = """
				<!DOCTYPE data PUBLIC "-//example.com//DTD Test 1.0//EN"
					"http://example.com/dtd/test.dtd">
				<data ts="2024-02-22T12:00:00Z">
					<prop name="foo">123</prop>
				</data>
				""";

		// WHEN
		String result = TestXsltService.inputTextValue(xmlInput);

		// THEN
		// @formatter:off
		then(result)
			.as("DOCTYPE declaration removed")
			.isEqualTo(
					"""
					<data ts="2024-02-22T12:00:00Z">
						<prop name="foo">123</prop>
					</data>
					""")
			;
		// @formatter:on
	}

	@Test
	public void stripDoctype_none() throws IOException {
		// GIVEN
		final String xmlInput = """
				<data ts="2024-02-22T12:00:00Z">
					<prop name="foo">123</prop>
				</data>
				""";

		// WHEN
		String result = TestXsltService.inputTextValue(xmlInput);

		// THEN
		// @formatter:off
		then(result)
			.as("Absence of DOCTYPE returns unchanged input")
			.isEqualTo(
					"""
					<data ts="2024-02-22T12:00:00Z">
						<prop name="foo">123</prop>
					</data>
					""")
			;
		// @formatter:on
	}

	@Test
	public void stripDoctype_empty() throws IOException {
		// GIVEN
		final String xmlInput = """
				<!DOCTYPE>
				<data ts="2024-02-22T12:00:00Z">
					<prop name="foo">123</prop>
				</data>
				""";

		// WHEN
		String result = TestXsltService.inputTextValue(xmlInput);

		// THEN
		// @formatter:off
		then(result)
			.as("DOCTYPE declaration removed")
			.isEqualTo(
					"""
					<data ts="2024-02-22T12:00:00Z">
						<prop name="foo">123</prop>
					</data>
					""")
			;
		// @formatter:on
	}

	@Test
	public void stripDoctype_notAtStart() throws IOException {
		// GIVEN
		final String xmlInput = """
				<?xml version="1.0" encoding="UTF-8"?>
				<!-- Comment here. -->
				<!DOCTYPE>
				<data ts="2024-02-22T12:00:00Z">
					<prop name="foo">123</prop>
				</data>
				""";

		// WHEN
		String result = TestXsltService.inputTextValue(xmlInput);

		// THEN
		// @formatter:off
		then(result)
			.as("DOCTYPE declaration removed")
			.isEqualTo(
					"""
					<?xml version="1.0" encoding="UTF-8"?>
					<!-- Comment here. -->
					<data ts="2024-02-22T12:00:00Z">
						<prop name="foo">123</prop>
					</data>
					""")
			;
		// @formatter:on
	}

	@Test
	public void stripDoctype_multiple() throws IOException {
		// GIVEN
		final String xmlInput = """
				<!DOCTYPE>
				<?xml version="1.0" encoding="UTF-8"?>
				<!-- Comment here. -->
				<!DOCTYPE>
				<!DOCTYPE>
				<data ts="2024-02-22T12:00:00Z">
					<prop name="foo">123</prop>
				</data>
				<!DOCTYPE>
				""";

		// WHEN
		String result = TestXsltService.inputTextValue(xmlInput);

		// THEN
		// @formatter:off
		then(result)
			.as("DOCTYPE declaration removed")
			.isEqualTo(
					"""
					<?xml version="1.0" encoding="UTF-8"?>
					<!-- Comment here. -->
					<data ts="2024-02-22T12:00:00Z">
						<prop name="foo">123</prop>
					</data>
					""")
			;
		// @formatter:on
	}

	@Test
	public void extractTemplatesCacheTtlSecondsFromConfig() {
		// GIVEN
		final BasicIdentifiableConfiguration config = new BasicIdentifiableConfiguration();
		config.setServiceProps(Map.of(BaseXsltService.SETTING_XSLT_CACHE_DURATION, "1234"));

		// WHEN
		long result = service.templatesCacheTtlSeconds(config);

		// THEN
		then(result).as("Seconds extracted from duration service property string").isEqualTo(1234L);
	}

	@Test
	public void extractTemplatesCacheTtlSecondsFromConfig_default() {
		// GIVEN
		final BasicIdentifiableConfiguration config = new BasicIdentifiableConfiguration();

		// WHEN
		long result = service.templatesCacheTtlSeconds(config);

		// THEN
		then(result).as("Cache duration uses default when no service property available")
				.isEqualTo(service.getTemplatesCacheTtl().toSeconds());
	}

	@Test
	public void parseXslt() throws IOException {
		// GIVEN
		final BasicIdentifiableConfiguration config = new BasicIdentifiableConfiguration();
		final String xslt = ClassUtils.getResourceAsString("test-xform-01.xsl", getClass(), null);
		final String cacheKey = UUID.randomUUID().toString();

		// WHEN
		Templates result = service.templates(xslt, config, cacheKey);

		// THEN
		// @formatter:off
		then(result)
			.as("XSLT parsed")
			.isNotNull()
			;
		
		then(service.getTemplatesCache().get(cacheKey))
			.as("XSLT saved to cache based on given key")
			.isNotNull()
			;
		// @formatter:on
	}

}
