/* ==================================================================
 * XsltTransformServiceTests.java - 22/02/2024 7:52:30 am
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

package net.solarnetwork.central.din.biz.impl.test;

import static java.util.Collections.singletonMap;
import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.BDDAssertions.then;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import net.sf.saxon.TransformerFactoryImpl;
import net.solarnetwork.central.din.biz.impl.XsltTransformService;
import net.solarnetwork.central.support.BasicSharedValueCache;
import net.solarnetwork.central.support.SharedValueCache;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.domain.BasicIdentifiableConfiguration;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.util.CachedResult;
import net.solarnetwork.util.ClassUtils;

/**
 * Test cases for the {@link XsltTransformService} class.
 *
 * @author matt
 * @version 1.0
 */
public class XsltTransformServiceTests {

	private ConcurrentMap<String, CachedResult<Templates>> primaryCache;
	private ConcurrentMap<String, Templates> sharedCache;
	private SharedValueCache<String, Templates, String> templatesCache;
	private XsltTransformService service;

	@BeforeEach
	public void setup() throws ParserConfigurationException {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newNSInstance();
		dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
		TransformerFactoryImpl tf = new net.sf.saxon.TransformerFactoryImpl();
		tf.setURIResolver(new URIResolver() {

			@Override
			public Source resolve(String href, String base) throws TransformerException {
				throw new UnsupportedOperationException(
						"External resources are not allowed (" + href + ") from (" + base + ")");
			}
		});

		primaryCache = new ConcurrentHashMap<>();
		sharedCache = new ConcurrentHashMap<>();
		templatesCache = new BasicSharedValueCache<>(primaryCache, sharedCache);
		service = new XsltTransformService(dbf, tf, JsonUtils.newDatumObjectMapper(), Duration.ZERO,
				templatesCache);
	}

	@Test
	public void xmlObject() throws IOException {
		// GIVEN
		final String xmlInput = """
				<data ts="2024-02-22T12:00:00Z">
					<prop name="foo">123</prop>
					<prop name="bim">234</prop>
					<prop name="msg">Hello</prop>
				</data>
				""";

		final String xslt = ClassUtils.getResourceAsString("test-xform-01.xsl", getClass());

		final BasicIdentifiableConfiguration conf = new BasicIdentifiableConfiguration();
		conf.setServiceProps(singletonMap(XsltTransformService.SETTING_XSLT, xslt));

		// WHEN
		Iterable<Datum> results = service.transform(xmlInput, XsltTransformService.XML_TYPE, conf, null);

		// THEN
		DatumSamples expectedSamples = new DatumSamples();
		expectedSamples.putInstantaneousSampleValue("foo", 123);
		expectedSamples.putInstantaneousSampleValue("bim", 234);
		expectedSamples.putStatusSampleValue("msg", "Hello");

		// @formatter:off
		then(results)
				.as("Single datum produced")
				.hasSize(1)
				.element(0)
				.as("Created date parsed")
				.returns(Instant.parse("2024-02-22T12:00:00Z"), from(Datum::getTimestamp))
				.as("Kind unknown")
				.returns(null, Datum::getKind)
				.as("Node ID not populated")
				.returns(null, Datum::getObjectId)
				.as("Source ID not populated")
				.returns(null, Datum::getSourceId)
				.extracting(Datum::asSampleOperations)
				.as("Samples populated")
				.isEqualTo(expectedSamples)
				;
		// @formatter:on

		then(primaryCache).as("Templates not cached").isEmpty();
	}

	@Test
	public void xmlObject_stream() throws IOException {
		// GIVEN
		final String xmlInput = """
				<data ts="2024-02-22T12:00:00Z">
					<prop name="foo">123</prop>
					<prop name="bim">234</prop>
					<prop name="msg">Hello</prop>
				</data>
				""";
		final var xmlInputStream = new ByteArrayInputStream(xmlInput.getBytes(StandardCharsets.UTF_8));

		final String xslt = ClassUtils.getResourceAsString("test-xform-01.xsl", getClass());

		final BasicIdentifiableConfiguration conf = new BasicIdentifiableConfiguration();
		conf.setServiceProps(singletonMap(XsltTransformService.SETTING_XSLT, xslt));

		// WHEN
		Iterable<Datum> results = service.transform(xmlInputStream, XsltTransformService.XML_TYPE, conf,
				null);

		// THEN
		DatumSamples expectedSamples = new DatumSamples();
		expectedSamples.putInstantaneousSampleValue("foo", 123);
		expectedSamples.putInstantaneousSampleValue("bim", 234);
		expectedSamples.putStatusSampleValue("msg", "Hello");

		// @formatter:off
		then(results)
				.as("Single datum produced")
				.hasSize(1)
				.element(0)
				.as("Created date parsed")
				.returns(Instant.parse("2024-02-22T12:00:00Z"), from(Datum::getTimestamp))
				.as("Kind unknown")
				.returns(null, Datum::getKind)
				.as("Node ID not populated")
				.returns(null, Datum::getObjectId)
				.as("Source ID not populated")
				.returns(null, Datum::getSourceId)
				.extracting(Datum::asSampleOperations)
				.as("Samples populated")
				.isEqualTo(expectedSamples)
				;
		// @formatter:on

		then(primaryCache).as("Templates not cached").isEmpty();
	}

	@Test
	public void xmlObject_inputParameters() throws IOException {
		// GIVEN
		final String xmlInput = "<data/>";

		final String xslt = ClassUtils.getResourceAsString("test-xform-03.xsl", getClass());

		final BasicIdentifiableConfiguration conf = new BasicIdentifiableConfiguration();
		conf.setServiceProps(singletonMap(XsltTransformService.SETTING_XSLT, xslt));

		// WHEN
		Map<String, Object> params = Map.of("foo", "f", "bar", "b");
		Iterable<Datum> results = service.transform(xmlInput, XsltTransformService.XML_TYPE, conf,
				params);

		// THEN
		DatumSamples expectedSamples = new DatumSamples();
		expectedSamples.putStatusSampleValue("foo", "f");
		expectedSamples.putStatusSampleValue("bar", "b");

		// @formatter:off
		then(results)
				.as("Single datum produced")
				.hasSize(1)
				.element(0)
				.extracting(Datum::asSampleOperations)
				.as("Samples populated")
				.isEqualTo(expectedSamples)
				;
		// @formatter:on

		then(primaryCache).as("Templates not cached").isEmpty();
	}

	@Test
	public void xmlObject_withCache() throws IOException {
		// GIVEN
		final String xmlInput = """
				<data ts="2024-02-22T12:00:00Z">
					<prop name="foo">123</prop>
					<prop name="bim">234</prop>
					<prop name="msg">Hello</prop>
				</data>
				""";

		final String xmlInput2 = """
				<data ts="2024-02-22T12:01:00Z">
					<prop name="foo">1231</prop>
					<prop name="bim">2341</prop>
					<prop name="msg">World</prop>
				</data>
				""";

		final String xslt = ClassUtils.getResourceAsString("test-xform-01.xsl", getClass());

		final BasicIdentifiableConfiguration conf = new BasicIdentifiableConfiguration();
		conf.setServiceProps(Map.of(XsltTransformService.SETTING_XSLT, xslt,
				XsltTransformService.SETTING_XSLT_CACHE_DURATION, 600L));

		// WHEN
		Iterable<Datum> results = service.transform(xmlInput, XsltTransformService.XML_TYPE, conf, null);
		Iterable<Datum> results2 = service.transform(xmlInput2, XsltTransformService.XML_TYPE, conf,
				null);

		// THEN
		DatumSamples expectedSamples = new DatumSamples();
		expectedSamples.putInstantaneousSampleValue("foo", 123);
		expectedSamples.putInstantaneousSampleValue("bim", 234);
		expectedSamples.putStatusSampleValue("msg", "Hello");

		// @formatter:off
		then(results)
				.as("Single datum produced")
				.hasSize(1)
				.element(0)
				.as("Created date parsed")
				.returns(Instant.parse("2024-02-22T12:00:00Z"), from(Datum::getTimestamp))
				.as("Kind unknown")
				.returns(null, Datum::getKind)
				.as("Node ID not populated")
				.returns(null, Datum::getObjectId)
				.as("Source ID not populated")
				.returns(null, Datum::getSourceId)
				.extracting(Datum::asSampleOperations)
				.as("Samples populated")
				.isEqualTo(expectedSamples)
				;

		DatumSamples expectedSamples2 = new DatumSamples();
		expectedSamples2.putInstantaneousSampleValue("foo", 1231);
		expectedSamples2.putInstantaneousSampleValue("bim", 2341);
		expectedSamples2.putStatusSampleValue("msg", "World");

		then(results2)
				.as("Single datum produced")
				.hasSize(1)
				.element(0)
				.as("Created date parsed")
				.returns(Instant.parse("2024-02-22T12:01:00Z"), from(Datum::getTimestamp))
				.as("Kind unknown")
				.returns(null, Datum::getKind)
				.as("Node ID not populated")
				.returns(null, Datum::getObjectId)
				.as("Source ID not populated")
				.returns(null, Datum::getSourceId)
				.extracting(Datum::asSampleOperations)
				.as("Samples populated")
				.isEqualTo(expectedSamples2)
				;

		then(primaryCache)
			.as("Primary templates cached")
			.hasSize(1)
			.as("Primary templates cache key SHA256 of XSLT.")
			.containsKey(DigestUtils.sha256Hex(xslt))
			;

		then(sharedCache)
				.as("Shared templates cached")
				.hasSize(1)
				.as("Shared templates cache key SHA256 of XSLT.")
				.containsKey(DigestUtils.sha256Hex(xslt))
				;
		// @formatter:on
	}

	@Test
	public void xmlObject_withCache_providedCacheKey() throws IOException {
		// GIVEN
		final String xmlInput = """
				<data ts="2024-02-22T12:00:00Z">
					<prop name="foo">123</prop>
					<prop name="bim">234</prop>
					<prop name="msg">Hello</prop>
				</data>
				""";

		final String xmlInput2 = """
				<data ts="2024-02-22T12:01:00Z">
					<prop name="foo">1231</prop>
					<prop name="bim">2341</prop>
					<prop name="msg">World</prop>
				</data>
				""";

		final String xslt = ClassUtils.getResourceAsString("test-xform-01.xsl", getClass());

		final BasicIdentifiableConfiguration conf = new BasicIdentifiableConfiguration();
		conf.setServiceProps(Map.of(XsltTransformService.SETTING_XSLT, xslt,
				XsltTransformService.SETTING_XSLT_CACHE_DURATION, 600L));

		// WHEN
		var params = Collections.singletonMap(XsltTransformService.PARAM_CONFIGURATION_CACHE_KEY, "a");
		Iterable<Datum> results = service.transform(xmlInput, XsltTransformService.XML_TYPE, conf,
				params);
		Iterable<Datum> results2 = service.transform(xmlInput2, XsltTransformService.XML_TYPE, conf,
				params);

		// THEN
		DatumSamples expectedSamples = new DatumSamples();
		expectedSamples.putInstantaneousSampleValue("foo", 123);
		expectedSamples.putInstantaneousSampleValue("bim", 234);
		expectedSamples.putStatusSampleValue("msg", "Hello");

		// @formatter:off
		then(results)
				.as("Single datum produced")
				.hasSize(1)
				.element(0)
				.as("Created date parsed")
				.returns(Instant.parse("2024-02-22T12:00:00Z"), from(Datum::getTimestamp))
				.as("Kind unknown")
				.returns(null, Datum::getKind)
				.as("Node ID not populated")
				.returns(null, Datum::getObjectId)
				.as("Source ID not populated")
				.returns(null, Datum::getSourceId)
				.extracting(Datum::asSampleOperations)
				.as("Samples populated")
				.isEqualTo(expectedSamples)
				;

		DatumSamples expectedSamples2 = new DatumSamples();
		expectedSamples2.putInstantaneousSampleValue("foo", 1231);
		expectedSamples2.putInstantaneousSampleValue("bim", 2341);
		expectedSamples2.putStatusSampleValue("msg", "World");

		then(results2)
				.as("Single datum produced")
				.hasSize(1)
				.element(0)
				.as("Created date parsed")
				.returns(Instant.parse("2024-02-22T12:01:00Z"), from(Datum::getTimestamp))
				.as("Kind unknown")
				.returns(null, Datum::getKind)
				.as("Node ID not populated")
				.returns(null, Datum::getObjectId)
				.as("Source ID not populated")
				.returns(null, Datum::getSourceId)
				.extracting(Datum::asSampleOperations)
				.as("Samples populated")
				.isEqualTo(expectedSamples2)
				;

		then(primaryCache)
			.as("Primary templates cached")
			.hasSize(1)
			.as("Primary templates cache key provided value.")
			.containsKey("a")
			;

		then(sharedCache)
				.as("Shared templates cached")
				.hasSize(1)
				.as("Shared templates cache key SHA256 of XSLT.")
				.containsKey(DigestUtils.sha256Hex(xslt))
				;
		// @formatter:on
	}

	@Test
	public void xmlObject_nodeAndSource() throws IOException {
		// GIVEN
		final String xmlInput = """
				<data ts="2024-02-22T12:00:00Z" node="123" source="test/1">
					<prop name="foo">123</prop>
				</data>
				""";

		final String xslt = ClassUtils.getResourceAsString("test-xform-01.xsl", getClass());

		final BasicIdentifiableConfiguration conf = new BasicIdentifiableConfiguration();
		conf.setServiceProps(singletonMap(XsltTransformService.SETTING_XSLT, xslt));

		// WHEN
		Iterable<Datum> results = service.transform(xmlInput, XsltTransformService.XML_TYPE, conf, null);

		// THEN
		DatumSamples expectedSamples = new DatumSamples();
		expectedSamples.putInstantaneousSampleValue("foo", 123);

		// @formatter:off
		then(results)
				.as("Single datum produced")
				.hasSize(1)
				.element(0)
				.as("Created date parsed")
				.returns(Instant.parse("2024-02-22T12:00:00Z"), from(Datum::getTimestamp))
				.as("Kind is node")
				.returns(ObjectDatumKind.Node, Datum::getKind)
				.as("Node ID populated")
				.returns(123L, Datum::getObjectId)
				.as("Source ID populated")
				.returns("test/1", Datum::getSourceId)
				.extracting(Datum::asSampleOperations)
				.as("Samples populated")
				.isEqualTo(expectedSamples)
				;
		// @formatter:on

		then(primaryCache).as("Templates not cached").isEmpty();
	}

	@Test
	public void xmlObject_locationAndSource() throws IOException {
		// GIVEN
		final String xmlInput = """
				<data ts="2024-02-22T12:00:00Z" location="123" source="test/1">
					<prop name="foo">123</prop>
				</data>
				""";

		final String xslt = ClassUtils.getResourceAsString("test-xform-01.xsl", getClass());

		final BasicIdentifiableConfiguration conf = new BasicIdentifiableConfiguration();
		conf.setServiceProps(singletonMap(XsltTransformService.SETTING_XSLT, xslt));

		// WHEN
		Iterable<Datum> results = service.transform(xmlInput, XsltTransformService.XML_TYPE, conf, null);

		// THEN
		DatumSamples expectedSamples = new DatumSamples();
		expectedSamples.putInstantaneousSampleValue("foo", 123);

		// @formatter:off
		then(results)
				.as("Single datum produced")
				.hasSize(1)
				.element(0)
				.as("Created date parsed")
				.returns(Instant.parse("2024-02-22T12:00:00Z"), from(Datum::getTimestamp))
				.as("Kind is location")
				.returns(ObjectDatumKind.Location, Datum::getKind)
				.as("Location ID populated")
				.returns(123L, Datum::getObjectId)
				.as("Source ID populated")
				.returns("test/1", Datum::getSourceId)
				.extracting(Datum::asSampleOperations)
				.as("Samples populated")
				.isEqualTo(expectedSamples)
				;
		// @formatter:on

		then(primaryCache).as("Templates not cached").isEmpty();
	}

	@Test
	public void xmlList_nodeAndSource() throws IOException {
		// GIVEN
		final String xmlInput = """
				<array>
					<data ts="2024-02-22T12:00:00Z" node="123" source="test/1">
						<prop name="foo">123</prop>
					</data>
					<data ts="2024-02-22T12:01:00Z" node="124" source="test/2">
						<prop name="bim">234</prop>
					</data>
				</array>
				""";

		final String xslt = ClassUtils.getResourceAsString("test-xform-01.xsl", getClass());

		final BasicIdentifiableConfiguration conf = new BasicIdentifiableConfiguration();
		conf.setServiceProps(singletonMap(XsltTransformService.SETTING_XSLT, xslt));

		// WHEN
		Iterable<Datum> results = service.transform(xmlInput, XsltTransformService.XML_TYPE, conf, null);

		// THEN
		DatumSamples expectedSamples1 = new DatumSamples();
		expectedSamples1.putInstantaneousSampleValue("foo", 123);

		DatumSamples expectedSamples2 = new DatumSamples();
		expectedSamples2.putInstantaneousSampleValue("bim", 234);

		// @formatter:off
		then(results)
				.as("Two datum produced")
				.hasSize(2)
				.element(0)
				.as("Created date parsed")
				.returns(Instant.parse("2024-02-22T12:00:00Z"), from(Datum::getTimestamp))
				.as("Kind is node")
				.returns(ObjectDatumKind.Node, Datum::getKind)
				.as("Node ID populated")
				.returns(123L, Datum::getObjectId)
				.as("Source ID populated")
				.returns("test/1", Datum::getSourceId)
				.extracting(Datum::asSampleOperations)
				.as("Samples populated")
				.isEqualTo(expectedSamples1)
				;

		then(results).element(1)
				.as("Created date parsed")
				.returns(Instant.parse("2024-02-22T12:01:00Z"), from(Datum::getTimestamp))
				.as("Kind is node")
				.returns(ObjectDatumKind.Node, Datum::getKind)
				.as("Node ID populated")
				.returns(124L, Datum::getObjectId)
				.as("Source ID populated")
				.returns("test/2", Datum::getSourceId)
				.extracting(Datum::asSampleOperations)
				.as("Samples populated")
				.isEqualTo(expectedSamples2)
				;
		// @formatter:on

		then(primaryCache).as("Templates not cached").isEmpty();
	}

	@Test
	public void jsonObject() throws IOException {
		// GIVEN
		final String jsonInput = """
				{"ts":"2024-02-22T12:00:00Z","props":{
					"foo":123,
					"bim":234,
					"msg":"Hello"
				}}
				""";

		final String xslt = ClassUtils.getResourceAsString("test-xform-02.xsl", getClass());

		final BasicIdentifiableConfiguration conf = new BasicIdentifiableConfiguration();
		conf.setServiceProps(singletonMap(XsltTransformService.SETTING_XSLT, xslt));

		// WHEN
		Iterable<Datum> results = service.transform(jsonInput, XsltTransformService.JSON_TYPE, conf,
				null);

		// THEN
		DatumSamples expectedSamples = new DatumSamples();
		expectedSamples.putInstantaneousSampleValue("foo", 123);
		expectedSamples.putInstantaneousSampleValue("bim", 234);
		expectedSamples.putStatusSampleValue("msg", "Hello");

		// @formatter:off
		then(results)
				.as("Single datum produced")
				.hasSize(1)
				.element(0)
				.as("Created date parsed")
				.returns(Instant.parse("2024-02-22T12:00:00Z"), from(Datum::getTimestamp))
				.as("Kind unknown")
				.returns(null, Datum::getKind)
				.as("Node ID not populated")
				.returns(null, Datum::getObjectId)
				.as("Source ID not populated")
				.returns(null, Datum::getSourceId)
				.extracting(Datum::asSampleOperations)
				.as("Samples populated")
				.isEqualTo(expectedSamples)
				;
		// @formatter:on

		then(primaryCache).as("Templates not cached").isEmpty();
	}

}
