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

package net.solarnetwork.central.din.biz.test;

import static java.util.Collections.singletonMap;
import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.BDDAssertions.then;
import java.io.IOException;
import java.time.Instant;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import net.sf.saxon.TransformerFactoryImpl;
import net.solarnetwork.central.din.biz.XsltTransformService;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.domain.BasicIdentifiableConfiguration;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.util.ClassUtils;

/**
 * Test cases for the {@link XsltTransformService} class.
 *
 * @author matt
 * @version 1.0
 */
public class XsltTransformServiceTests {

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

		service = new XsltTransformService(dbf, tf, JsonUtils.newDatumObjectMapper(), null);
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
	}

}
