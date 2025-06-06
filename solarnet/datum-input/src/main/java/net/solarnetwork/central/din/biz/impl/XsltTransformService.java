/* ==================================================================
 * XsltTransformService.java - 21/02/2024 8:37:38 pm
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

package net.solarnetwork.central.din.biz.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.springframework.util.MimeType;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.central.din.biz.TransformService;
import net.solarnetwork.central.support.BasicSharedValueCache;
import net.solarnetwork.central.support.SharedValueCache;
import net.solarnetwork.central.support.xslt.BaseXsltService;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.service.IdentifiableConfiguration;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.BasicTextAreaSettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;

/**
 * {@link TransformService} that uses XSLT to transform input data into datum
 * JSON.
 *
 * <p>
 * This service supports string and {@link InputStream} input data. If
 * {@link InputStream} is provided, it is assumed to be a UTF-8 character
 * stream.
 * </p>
 *
 * @author matt
 * @version 1.2
 */
public class XsltTransformService extends BaseXsltService implements TransformService {

	/**
	 * Constructor.
	 *
	 * @param documentBuilderFactory
	 *        the XML document builder factory
	 * @param transformerFactory
	 *        the XSLT transformer factory to use
	 * @param objectMapper
	 *        the object mapper
	 * @param templatesCacheTtl
	 *        the TTL for the templates cache, or {@literal null} or
	 *        {@literal 0} for no caching
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public XsltTransformService(DocumentBuilderFactory documentBuilderFactory,
			TransformerFactory transformerFactory, ObjectMapper objectMapper,
			Duration templatesCacheTtl) {
		this(documentBuilderFactory, transformerFactory, objectMapper, templatesCacheTtl,
				new BasicSharedValueCache<>());
	}

	/**
	 * Constructor.
	 *
	 * <p>
	 * If a {@code templatesCache} is provided then XSLT templates will be
	 * cached for up to the configured {@code templatesCacheTtl} seconds.
	 * </p>
	 *
	 * @param transformerFactory
	 *        the XSLT transformer factory to use
	 * @param documentBuilderFactory
	 *        the XML document builder factory
	 * @param objectMapper
	 *        the object mapper
	 * @param templatesCacheTtl
	 *        the TTL for the templates cache, or {@literal null} or
	 *        {@literal 0} for no caching
	 * @param templatesCache
	 *        the templates cache to use
	 * @throws IllegalArgumentException
	 *         if any argument except {@code templatesCache} is {@literal null}
	 */
	public XsltTransformService(DocumentBuilderFactory documentBuilderFactory,
			TransformerFactory transformerFactory, ObjectMapper objectMapper, Duration templatesCacheTtl,
			SharedValueCache<String, Templates, String> templatesCache) {
		super("net.solarnetwork.central.din.XsltTransformService", documentBuilderFactory,
				transformerFactory, objectMapper, templatesCacheTtl, templatesCache);
	}

	@Override
	public String getDisplayName() {
		return "XSLT Transform Service";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		return Arrays.asList(new BasicTextAreaSettingSpecifier(SETTING_XSLT, null, true),
				new BasicTextFieldSettingSpecifier(SETTING_XSLT_CACHE_DURATION, null));
	}

	@Override
	public boolean supportsInput(Object input, MimeType type) {
		return input != null && (JSON_TYPE.isCompatibleWith(type) || XML_TYPE.isCompatibleWith(type));
	}

	@Override
	public Iterable<Datum> transform(Object input, MimeType type, IdentifiableConfiguration config,
			Map<String, ?> parameters) throws IOException {
		String inputText = inputText(input);
		Map<String, ?> props = config.getServiceProperties();
		Object xslt = (props != null ? props.get(SETTING_XSLT) : null);
		if ( xslt == null ) {
			return Collections.emptyList();
		}
		Templates templates = templates(xslt.toString(), config, parameters);
		try {
			// configure transform
			Transformer xform = templates.newTransformer();
			if ( parameters != null ) {
				for ( Entry<String, ?> e : parameters.entrySet() ) {
					String key = e.getKey();
					if ( PARAM_PREVIOUS_INPUT.equals(key) ) {
						String previousInputText = inputText(parameters.get(key));
						xform.setParameter(key, previousInputText);
					} else {
						xform.setParameter(key, e.getValue());
					}
				}
			}
			xform.setOutputProperty(OutputKeys.METHOD, "text");
			xform.setOutputProperty(OutputKeys.MEDIA_TYPE, JSON_TYPE.toString());

			// get XML input for XSLT transform
			Source inputSource;
			if ( JSON_TYPE.isCompatibleWith(type) ) {
				// pass JSON as input parameter
				xform.setParameter(XSLT_PARAM_JSON, inputText);
				inputSource = new DOMSource();
			} else {
				try (Reader xmlInput = new StringReader(inputText)) {
					inputSource = new DOMSource(documentBuilder().parse(new InputSource(xmlInput)));
				} catch ( SAXException | ParserConfigurationException e ) {
					log.debug("Error parsing XML input for XSLT transform: {}", e.getMessage(), e);
					throw new IOException("Error parsing XML input for XSLT transform.", e);
				}
			}

			// execute transform, producing Datum JSON object or JSON array of Datum objects
			try (StringWriter jsonOut = new StringWriter(1024)) {
				Result jsonResult = new StreamResult(jsonOut);
				xform.transform(inputSource, jsonResult);
				String json = jsonOut.toString();
				if ( parameters != null
						&& parameters.get(PARAM_XSLT_OUTPUT) instanceof Appendable out ) {
					out.append(json);
				}
				return parseDatumList(json);
			} catch ( TransformerException e ) {
				log.debug("Error executing XSLT transform: {}", e.getMessage(), e);
				throw new IOException("Error executing XSLT transform.", e);
			}
		} catch ( TransformerConfigurationException e ) {
			log.debug("Error executing XSLT: {}", e.getMessage(), e);
			throw new IOException("Error executing XSLT.", e);
		}
	}

	private Templates templates(String xslt, IdentifiableConfiguration config, Map<String, ?> parameters)
			throws IOException {
		return templates(xslt, config,
				parameters != null ? parameters.get(PARAM_CONFIGURATION_CACHE_KEY) : null);
	}

	@SuppressWarnings("MixedMutabilityReturnType")
	private List<Datum> parseDatumList(String json) throws IOException {
		JsonNode root = objectMapper.readTree(json);
		if ( root.isObject() ) {
			Datum d = objectMapper.treeToValue(root, Datum.class);
			if ( !(d == null || d.asSampleOperations().isEmpty()) ) {
				return Collections.singletonList(d);
			}
			return Collections.emptyList();
		}
		List<Datum> result = new ArrayList<>(root.size());
		for ( JsonNode n : root ) {
			Datum d = objectMapper.treeToValue(n, Datum.class);
			if ( !(d == null || d.asSampleOperations().isEmpty()) ) {
				result.add(d);
			}
		}
		return result;
	}

}
