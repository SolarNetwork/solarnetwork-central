/* ==================================================================
 * XsltRequestTransformServiceTests.java - 29/03/2024 3:12:45 pm
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

package net.solarnetwork.central.inin.biz.impl.test;

import static java.util.Collections.singletonMap;
import static org.assertj.core.api.BDDAssertions.then;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Templates;
import org.apache.commons.codec.digest.DigestUtils;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import net.sf.saxon.TransformerFactoryImpl;
import net.solarnetwork.central.inin.biz.TransformConstants;
import net.solarnetwork.central.inin.biz.impl.DataUriResolver;
import net.solarnetwork.central.inin.biz.impl.XsltRequestTransformService;
import net.solarnetwork.central.instructor.domain.NodeInstruction;
import net.solarnetwork.central.support.BasicSharedValueCache;
import net.solarnetwork.central.support.SharedValueCache;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.domain.BasicIdentifiableConfiguration;
import net.solarnetwork.util.CachedResult;
import net.solarnetwork.util.ClassUtils;

/**
 * Test cases for the {@link XsltRequestTransformService} class.
 *
 * @author matt
 * @version 1.0
 */
public class XsltRequestTransformServiceTests {

	private ConcurrentMap<String, CachedResult<Templates>> primaryCache;
	private ConcurrentMap<String, Templates> sharedCache;
	private SharedValueCache<String, Templates, String> templatesCache;
	private XsltRequestTransformService service;

	@BeforeEach
	public void setup() throws ParserConfigurationException {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newNSInstance();
		dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
		TransformerFactoryImpl tf = new net.sf.saxon.TransformerFactoryImpl();
		tf.setURIResolver(new DataUriResolver());

		primaryCache = new ConcurrentHashMap<>();
		sharedCache = new ConcurrentHashMap<>();
		templatesCache = new BasicSharedValueCache<>(primaryCache, sharedCache);
		service = new XsltRequestTransformService(dbf, tf, JsonUtils.newDatumObjectMapper(),
				Duration.ZERO, templatesCache);
	}

	@Test
	public void xmlObject() throws IOException {
		// GIVEN
		final String xmlInput = """
				<data groupId="abc-123"/>
				""";

		final String xslt = ClassUtils.getResourceAsString("test-xform-01.xsl", getClass());

		final BasicIdentifiableConfiguration conf = new BasicIdentifiableConfiguration();
		conf.setServiceProps(singletonMap(XsltRequestTransformService.SETTING_XSLT, xslt));

		// WHEN
		Iterable<NodeInstruction> results = service.transformInput(xmlInput, TransformConstants.XML_TYPE,
				conf, null);

		// THEN
		// @formatter:off
		then(results)
				.as("Single instruction produced")
				.hasSize(1)
				.element(0)
				.as("Node ID not populated")
				.returns(null, NodeInstruction::getNodeId)
				.as("Topic populated")
				.returns("LatestDatum", NodeInstruction::getTopic)
				.as("Parameters populated")
				.extracting(NodeInstruction::getParams, InstanceOfAssertFactories.map(String.class, String.class))
				.as("One parameter populated")
				.hasSize(1)
				.as("Source ID parameter generated")
				.containsEntry("sourceIds", "abc-123")
				;
		// @formatter:on

		then(primaryCache).as("Templates not cached").isEmpty();
	}

	@Test
	public void xmlObject_xsltOutput() throws IOException {
		// GIVEN
		final String xmlInput = """
				<data groupId="abc-123"/>
				""";

		final String xslt = ClassUtils.getResourceAsString("test-xform-01.xsl", getClass());

		final BasicIdentifiableConfiguration conf = new BasicIdentifiableConfiguration();
		conf.setServiceProps(singletonMap(XsltRequestTransformService.SETTING_XSLT, xslt));

		// WHEN
		StringBuilder output = new StringBuilder();
		Map<String, Object> params = Map.of(TransformConstants.PARAM_DEBUG_OUTPUT, output);
		Iterable<NodeInstruction> results = service.transformInput(xmlInput, TransformConstants.XML_TYPE,
				conf, params);

		// THEN
		// @formatter:off
		then(results)
				.as("Single instruction produced")
				.hasSize(1)
				.element(0)
				.as("Node ID not populated")
				.returns(null, NodeInstruction::getNodeId)
				.as("Topic populated")
				.returns("LatestDatum", NodeInstruction::getTopic)
				.as("Parameters populated")
				.extracting(NodeInstruction::getParams, InstanceOfAssertFactories.map(String.class, String.class))
				.as("One parameter populated")
				.hasSize(1)
				.as("Source ID parameter generated")
				.containsEntry("sourceIds", "abc-123")
				;
		// @formatter:on

		then(primaryCache).as("Templates not cached").isEmpty();

		then(output.toString()).as("XSLT output appended").isEqualTo("""
				{"topic":"LatestDatum","params":{"sourceIds":"abc-123"}}""");
	}

	@Test
	public void xmlObject_stream() throws IOException {
		// GIVEN
		final String xmlInput = """
				<data groupId="abc-123"/>
				""";
		final var xmlInputStream = new ByteArrayInputStream(xmlInput.getBytes(StandardCharsets.UTF_8));

		final String xslt = ClassUtils.getResourceAsString("test-xform-01.xsl", getClass());

		final BasicIdentifiableConfiguration conf = new BasicIdentifiableConfiguration();
		conf.setServiceProps(singletonMap(XsltRequestTransformService.SETTING_XSLT, xslt));

		// WHEN
		Iterable<NodeInstruction> results = service.transformInput(xmlInputStream,
				TransformConstants.XML_TYPE, conf, null);

		// THEN
		// @formatter:off
		then(results)
				.as("Single instruction produced")
				.hasSize(1)
				.element(0)
				.as("Node ID not populated")
				.returns(null, NodeInstruction::getNodeId)
				.as("Topic populated")
				.returns("LatestDatum", NodeInstruction::getTopic)
				.as("Parameters populated")
				.extracting(NodeInstruction::getParams, InstanceOfAssertFactories.map(String.class, String.class))
				.as("One parameter populated")
				.hasSize(1)
				.as("Source ID parameter generated")
				.containsEntry("sourceIds", "abc-123")
				;
		// @formatter:on

		then(primaryCache).as("Templates not cached").isEmpty();
	}

	@Test
	public void xmlObject_inputParameters() throws IOException {
		// GIVEN
		final String xmlInput = "<data/>";

		final String xslt = ClassUtils.getResourceAsString("test-xform-02.xsl", getClass());

		final BasicIdentifiableConfiguration conf = new BasicIdentifiableConfiguration();
		conf.setServiceProps(singletonMap(XsltRequestTransformService.SETTING_XSLT, xslt));

		// WHEN
		Map<String, Object> params = Map.of("foo", "f", "bar", "b");
		Iterable<NodeInstruction> results = service.transformInput(xmlInput, TransformConstants.XML_TYPE,
				conf, params);

		// THEN
		// @formatter:off
		then(results)
				.as("Single instruction produced")
				.hasSize(1)
				.element(0)
				.as("Parameters populated")
				.extracting(NodeInstruction::getParams, InstanceOfAssertFactories.map(String.class, String.class))
				.as("One parameter populated")
				.hasSize(1)
				.as("Source ID parameter generated")
				.containsEntry("sourceIds", "f,b")
				;
		// @formatter:on

		then(primaryCache).as("Templates not cached").isEmpty();
	}

	@Test
	public void xmlObject_withCache() throws IOException {
		// GIVEN
		final String xmlInput = """
				<data groupId="abc-123"/>
				""";

		final String xmlInput2 = """
				<data groupId="def-234"/>
				""";

		final String xslt = ClassUtils.getResourceAsString("test-xform-01.xsl", getClass());

		final BasicIdentifiableConfiguration conf = new BasicIdentifiableConfiguration();
		conf.setServiceProps(Map.of(XsltRequestTransformService.SETTING_XSLT, xslt,
				XsltRequestTransformService.SETTING_XSLT_CACHE_DURATION, 600L));

		// WHEN
		Iterable<NodeInstruction> results = service.transformInput(xmlInput, TransformConstants.XML_TYPE,
				conf, null);
		Iterable<NodeInstruction> results2 = service.transformInput(xmlInput2,
				TransformConstants.XML_TYPE, conf, null);

		// THEN
		// @formatter:off
		then(results)
				.as("Single instruction produced")
				.hasSize(1)
				.element(0)
				.as("Node ID not populated")
				.returns(null, NodeInstruction::getNodeId)
				.as("Topic populated")
				.returns("LatestDatum", NodeInstruction::getTopic)
				.as("Parameters populated")
				.extracting(NodeInstruction::getParams, InstanceOfAssertFactories.map(String.class, String.class))
				.as("One parameter populated")
				.hasSize(1)
				.as("Source ID parameter generated")
				.containsEntry("sourceIds", "abc-123")
				;

		then(results2)
				.as("Single instruction produced")
				.hasSize(1)
				.element(0)
				.as("Node ID not populated")
				.returns(null, NodeInstruction::getNodeId)
				.as("Topic populated")
				.returns("LatestDatum", NodeInstruction::getTopic)
				.as("Parameters populated")
				.extracting(NodeInstruction::getParams, InstanceOfAssertFactories.map(String.class, String.class))
				.as("One parameter populated")
				.hasSize(1)
				.as("Source ID parameter generated")
				.containsEntry("sourceIds", "def-234")
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
				<data groupId="abc-123"/>
				""";

		final String xmlInput2 = """
				<data groupId="def-234"/>
				""";

		final String xslt = ClassUtils.getResourceAsString("test-xform-01.xsl", getClass());

		final BasicIdentifiableConfiguration conf = new BasicIdentifiableConfiguration();
		conf.setServiceProps(Map.of(XsltRequestTransformService.SETTING_XSLT, xslt,
				XsltRequestTransformService.SETTING_XSLT_CACHE_DURATION, 600L));

		// WHEN
		var params = Collections.singletonMap(TransformConstants.PARAM_CONFIGURATION_CACHE_KEY, "a");
		Iterable<NodeInstruction> results = service.transformInput(xmlInput, TransformConstants.XML_TYPE,
				conf, params);
		Iterable<NodeInstruction> results2 = service.transformInput(xmlInput2,
				TransformConstants.XML_TYPE, conf, params);

		// THEN
		// @formatter:off
		then(results)
				.as("Single instruction produced")
				.hasSize(1)
				.element(0)
				.as("Node ID not populated")
				.returns(null, NodeInstruction::getNodeId)
				.as("Topic populated")
				.returns("LatestDatum", NodeInstruction::getTopic)
				.as("Parameters populated")
				.extracting(NodeInstruction::getParams, InstanceOfAssertFactories.map(String.class, String.class))
				.as("One parameter populated")
				.hasSize(1)
				.as("Source ID parameter generated")
				.containsEntry("sourceIds", "abc-123")
				;

		then(results2)
				.as("Single instruction produced")
				.hasSize(1)
				.element(0)
				.as("Node ID not populated")
				.returns(null, NodeInstruction::getNodeId)
				.as("Topic populated")
				.returns("LatestDatum", NodeInstruction::getTopic)
				.as("Parameters populated")
				.extracting(NodeInstruction::getParams, InstanceOfAssertFactories.map(String.class, String.class))
				.as("One parameter populated")
				.hasSize(1)
				.as("Source ID parameter generated")
				.containsEntry("sourceIds", "def-234")
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
	public void multipleInstructions() throws IOException {
		// GIVEN
		final String xmlInput = """
				<array>
					<data node="123" source="test/1"/>
					<data node="124" source="test/2"/>
				</array>
				""";

		final String xslt = ClassUtils.getResourceAsString("test-xform-04.xsl", getClass());

		final BasicIdentifiableConfiguration conf = new BasicIdentifiableConfiguration();
		conf.setServiceProps(singletonMap(XsltRequestTransformService.SETTING_XSLT, xslt));

		// WHEN
		Iterable<NodeInstruction> results = service.transformInput(xmlInput, TransformConstants.XML_TYPE,
				conf, null);

		// THEN
		// @formatter:off
		then(results)
				.as("Two instruction produced")
				.hasSize(2)
				.element(0)
				.as("Node ID populated")
				.returns(123L, NodeInstruction::getNodeId)
				.as("Topic populated")
				.returns("LatestDatum", NodeInstruction::getTopic)
				.as("Parameters populated")
				.extracting(NodeInstruction::getParams, InstanceOfAssertFactories.map(String.class, String.class))
				.as("One parameter populated")
				.hasSize(1)
				.as("Source ID parameter generated")
				.containsEntry("sourceIds", "test/1")
				;

		then(results).element(1)
				.as("Node ID not populated")
				.returns(124L, NodeInstruction::getNodeId)
				.as("Topic populated")
				.returns("LatestDatum", NodeInstruction::getTopic)
				.as("Parameters populated")
				.extracting(NodeInstruction::getParams, InstanceOfAssertFactories.map(String.class, String.class))
				.as("One parameter populated")
				.hasSize(1)
				.as("Source ID parameter generated")
				.containsEntry("sourceIds", "test/2")
				;
		// @formatter:on

		then(primaryCache).as("Templates not cached").isEmpty();
	}

	@Test
	public void jsonObject() throws IOException {
		// GIVEN
		final String jsonInput = """
				{"groupId":"abc-123"}
				""";

		final String xslt = ClassUtils.getResourceAsString("test-xform-03.xsl", getClass());

		final BasicIdentifiableConfiguration conf = new BasicIdentifiableConfiguration();
		conf.setServiceProps(singletonMap(XsltRequestTransformService.SETTING_XSLT, xslt));

		// WHEN
		Iterable<NodeInstruction> results = service.transformInput(jsonInput,
				TransformConstants.JSON_TYPE, conf, null);

		// THEN
		// @formatter:off
		then(results)
				.as("Single instruction produced")
				.hasSize(1)
				.element(0)
				.as("Node ID not populated")
				.returns(null, NodeInstruction::getNodeId)
				.as("Topic populated")
				.returns("LatestDatum", NodeInstruction::getTopic)
				.as("Parameters populated")
				.extracting(NodeInstruction::getParams, InstanceOfAssertFactories.map(String.class, String.class))
				.as("One parameter populated")
				.hasSize(1)
				.as("Source ID parameter generated")
				.containsEntry("sourceIds", "abc-123")
				;
		// @formatter:on

		then(primaryCache).as("Templates not cached").isEmpty();
	}

}
