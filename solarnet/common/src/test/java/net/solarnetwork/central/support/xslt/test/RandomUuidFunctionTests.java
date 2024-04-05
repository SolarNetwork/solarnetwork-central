/* ==================================================================
 * RandomUuidFunctionTests.java - 5/04/2024 5:22:45 pm
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
import java.io.ByteArrayOutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.function.Predicate;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Result;
import javax.xml.transform.Templates;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import net.sf.saxon.TransformerFactoryImpl;
import net.solarnetwork.central.support.xslt.RandomUuidFunction;

/**
 * Test cases for the {@link RandomUuidFunction} class.
 * 
 * @author matt
 * @version 1.0
 */
public class RandomUuidFunctionTests {

	private static final String TEST_XSLT = """
			<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
			    xmlns:xs="http://www.w3.org/2001/XMLSchema"
			    xpath-default-namespace="http://www.w3.org/2005/xpath-functions" version="3.0">

			    <xsl:output method="text"/>

			    <xsl:variable name="uuid-fn" select="function-lookup(QName('http://solarnetwork.net/xslt','random-uuid'), 0)"/>

			    <xsl:template match="/">
			        <xsl:value-of select="if (exists($uuid-fn)) then $uuid-fn() else 'NO UUID'"/>
			    </xsl:template>

			 </xsl:stylesheet>
			""";

	@Test
	public void transform_noFunction() throws Exception {
		// GIVEN
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newNSInstance();
		dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
		TransformerFactoryImpl tf = new net.sf.saxon.TransformerFactoryImpl();

		//WHEN
		String result = null;
		try (Reader xsltInput = new StringReader(TEST_XSLT);
				ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			Document xsltDoc = dbf.newDocumentBuilder().parse(new InputSource(xsltInput));
			Templates t = tf.newTemplates(new DOMSource(xsltDoc));
			Result jsonResult = new StreamResult(out);
			t.newTransformer().transform(new DOMSource(), jsonResult);

			result = out.toString(StandardCharsets.UTF_8);
		}

		// THEN
		then(result).as("Transform without function registered").isEqualTo("NO UUID");
	}

	@Test
	public void transform_withFunction() throws Exception {
		// GIVEN
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newNSInstance();
		dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
		TransformerFactoryImpl tf = new net.sf.saxon.TransformerFactoryImpl();
		tf.getConfiguration().registerExtensionFunction(RandomUuidFunction.INSTANCE);

		//WHEN
		String result = null;
		try (Reader xsltInput = new StringReader(TEST_XSLT);
				ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			Document xsltDoc = dbf.newDocumentBuilder().parse(new InputSource(xsltInput));
			Templates t = tf.newTemplates(new DOMSource(xsltDoc));
			Result jsonResult = new StreamResult(out);
			t.newTransformer().transform(new DOMSource(), jsonResult);

			result = out.toString(StandardCharsets.UTF_8);
		}

		// THEN
		then(result).as("Transform with function registered").isNotBlank().as("Is a UUID string value")
				.matches(new Predicate<String>() {

					@Override
					public boolean test(String t) {
						try {
							UUID.fromString(t);
							return true;
						} catch ( IllegalArgumentException e ) {
							// sad
						}
						return false;
					}

				});
	}

}
