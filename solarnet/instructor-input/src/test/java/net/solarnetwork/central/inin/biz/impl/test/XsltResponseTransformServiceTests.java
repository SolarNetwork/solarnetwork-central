/* ==================================================================
 * XsltResponseTransformServiceTests.java - 30/03/2024 6:48:51 am
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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Templates;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import net.sf.saxon.TransformerFactoryImpl;
import net.solarnetwork.central.inin.biz.impl.DataUriResolver;
import net.solarnetwork.central.inin.biz.impl.XsltRequestTransformService;
import net.solarnetwork.central.inin.biz.impl.XsltResponseTransformService;
import net.solarnetwork.central.instructor.domain.NodeInstruction;
import net.solarnetwork.central.support.BasicSharedValueCache;
import net.solarnetwork.central.support.SharedValueCache;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.domain.BasicIdentifiableConfiguration;
import net.solarnetwork.domain.InstructionStatus.InstructionState;
import net.solarnetwork.util.CachedResult;
import net.solarnetwork.util.ClassUtils;

/**
 * Test cases for the {@link XsltResponseTransformService} class.
 *
 * @author matt
 * @version 1.0
 */
public class XsltResponseTransformServiceTests {

	private ConcurrentMap<String, CachedResult<Templates>> primaryCache;
	private ConcurrentMap<String, Templates> sharedCache;
	private SharedValueCache<String, Templates, String> templatesCache;
	private XsltResponseTransformService service;

	@BeforeEach
	public void setup() throws ParserConfigurationException {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newNSInstance();
		dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
		TransformerFactoryImpl tf = new net.sf.saxon.TransformerFactoryImpl();
		tf.setURIResolver(new DataUriResolver());

		primaryCache = new ConcurrentHashMap<>();
		sharedCache = new ConcurrentHashMap<>();
		templatesCache = new BasicSharedValueCache<>(primaryCache, sharedCache);
		service = new XsltResponseTransformService(dbf, tf, JsonUtils.newDatumObjectMapper(),
				Duration.ZERO, templatesCache);
	}

	@Test
	public void empty() throws IOException {
		// GIVEN
		List<NodeInstruction> instructions = Collections.emptyList();

		final String xslt = ClassUtils.getResourceAsString("test-xform-res-01.xsl", getClass());

		final BasicIdentifiableConfiguration conf = new BasicIdentifiableConfiguration();
		conf.setServiceProps(singletonMap(XsltRequestTransformService.SETTING_XSLT, xslt));

		// WHEN
		String result = null;
		try (ByteArrayOutputStream out = new ByteArrayOutputStream(1024)) {
			service.transformOutput(instructions, null, conf, null, out);
			result = out.toString(StandardCharsets.UTF_8);
		}

		// THEN
		// @formatter:off
		then(result)
				.as("Empty input produces result")
				.isEqualTo("""
						{"total-power":0}""")
				;
		// @formatter:on
	}

	@Test
	public void oneInstruction() throws IOException {
		// GIVEN
		List<NodeInstruction> instructions = new ArrayList<>();
		NodeInstruction instr1 = new NodeInstruction("LatestDatum", Instant.now().minusSeconds(1), 123L);
		instr1.setParams(Map.of("foo", "bar", "bim", "bam"));
		instr1.setState(InstructionState.Completed);
		instr1.setStatusDate(Instant.now());
		instr1.setResultParametersJson("""
				{"datum": [
					{
						"sourceId": "test/1",
						"watts": 123,
						"voltage": 240
					},
					{
						"sourceId": "test/2",
						"watts": 234,
						"voltage": 241
					}
				]}
				""");
		instructions.add(instr1);

		final String xslt = ClassUtils.getResourceAsString("test-xform-res-01.xsl", getClass());

		final BasicIdentifiableConfiguration conf = new BasicIdentifiableConfiguration();
		conf.setServiceProps(singletonMap(XsltRequestTransformService.SETTING_XSLT, xslt));

		// WHEN
		String result = null;
		try (ByteArrayOutputStream out = new ByteArrayOutputStream(1024)) {
			service.transformOutput(instructions, null, conf, null, out);
			result = out.toString(StandardCharsets.UTF_8);
		}

		// THEN
		// @formatter:off
		then(result)
				.as("Input produces result")
				.isEqualTo("""
						{"total-power":357}""")
				;
		// @formatter:on
	}

}
