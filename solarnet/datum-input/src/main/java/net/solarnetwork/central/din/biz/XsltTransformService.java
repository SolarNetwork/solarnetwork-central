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

package net.solarnetwork.central.din.biz;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import javax.xml.parsers.DocumentBuilder;
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
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.util.MimeType;
import org.w3c.dom.Document;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.service.IdentifiableConfiguration;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.BaseSettingsSpecifierLocalizedServiceInfoProvider;
import net.solarnetwork.settings.support.BasicTextAreaSettingSpecifier;
import net.solarnetwork.util.CachedResult;

/**
 * {@link TransformService} that uses XSLT to transform input data into datum
 * JSON. JSON.
 *
 * @author matt
 * @version 1.0
 */
public class XsltTransformService extends BaseSettingsSpecifierLocalizedServiceInfoProvider<String>
		implements TransformService, EntityResolver {

	/** The setting key for the XSLT stylesheet. */
	public static final String SETTING_XSLT = "xslt";

	/** The XSLT input parameter for input JSON. */
	public static final String PARAM_JSON = "input-json";

	private final TransformerFactory transformerFactory;
	private final DocumentBuilderFactory documentBuilderFactory;
	private final ObjectMapper objectMapper;
	private final Duration templatesCacheTtl;
	private final ConcurrentMap<String, CachedResult<Templates>> templatesCache;

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
				new ConcurrentHashMap<>(32, 0.9f, 2));
	}

	/**
	 * Constructor.
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
			ConcurrentMap<String, CachedResult<Templates>> templatesCache) {
		super("net.solarnetwork.central.din.biz.XsltTransformService");
		this.documentBuilderFactory = requireNonNullArgument(documentBuilderFactory,
				"documentBuilderFactory");
		this.transformerFactory = requireNonNullArgument(transformerFactory, "transformerFactory");
		this.objectMapper = requireNonNullArgument(objectMapper, "objectMapper");
		this.templatesCacheTtl = templatesCacheTtl != null && templatesCache != null ? templatesCacheTtl
				: Duration.ZERO;
		this.templatesCache = templatesCache;
	}

	@Override
	public String getDisplayName() {
		return "XSLT Transform Service";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		return Collections.singletonList(new BasicTextAreaSettingSpecifier(SETTING_XSLT, null, true));
	}

	@Override
	public boolean supportsInput(Object input, MimeType type) {
		return input != null && (JSON_TYPE.isCompatibleWith(type) || XML_TYPE.isCompatibleWith(type));
	}

	@Override
	public Iterable<Datum> transform(Object input, MimeType type, IdentifiableConfiguration config,
			Map<String, ?> parameters) throws IOException {
		String inputText = input.toString();
		Map<String, ?> props = config.getServiceProperties();
		Object xslt = (props != null ? props.get(SETTING_XSLT) : null);
		if ( xslt == null ) {
			return Collections.emptyList();
		}
		Templates templates = templates(xslt.toString());
		try {
			// configure transform
			Transformer xform = templates.newTransformer();
			if ( parameters != null ) {
				for ( Entry<String, ?> e : parameters.entrySet() ) {
					xform.setParameter(e.getKey(), e.getValue());
				}
			}
			xform.setOutputProperty(OutputKeys.METHOD, "text");
			xform.setOutputProperty(OutputKeys.MEDIA_TYPE, JSON_TYPE.toString());

			// get XML input for XSLT transform
			Source inputSource = null;
			if ( JSON_TYPE.isCompatibleWith(type) ) {
				// pass JSON as input parameter
				xform.setParameter(PARAM_JSON, inputText);
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
				return parseDatumList(jsonOut.toString());
			} catch ( TransformerException e ) {
				log.debug("Error executing XSLT transform: {}", e.getMessage(), e);
				throw new IOException("Error executing XSLT transform.", e);
			}
		} catch ( TransformerConfigurationException e ) {
			log.debug("Error executing XSLT: {}", e.getMessage(), e);
			throw new IOException("Error executing XSLT.", e);
		}
	}

	private DocumentBuilder documentBuilder() throws ParserConfigurationException {
		DocumentBuilder db = documentBuilderFactory.newDocumentBuilder();
		db.setEntityResolver(this);
		return db;
	}

	@Override
	public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
		// TODO Auto-generated method stub
		return null;
	}

	private Templates templates(String xslt) throws IOException {
		String xsltCacheKey = null;
		CachedResult<Templates> cachedXslt = null;
		if ( templatesCacheTtl.getSeconds() > 0 ) {
			xsltCacheKey = DigestUtils.sha256Hex(xslt);
			cachedXslt = templatesCache.get(xsltCacheKey);
			if ( cachedXslt != null && cachedXslt.isValid() ) {
				return cachedXslt.getResult();
			}
		}

		Templates t = null;
		try (Reader xsltInput = new StringReader(xslt)) {
			Document xsltDoc = documentBuilder().parse(new InputSource(xsltInput));
			t = transformerFactory.newTemplates(new DOMSource(xsltDoc));
		} catch ( SAXException | ParserConfigurationException e ) {
			log.debug("Error parsing XSLT source: {}", e.getMessage(), e);
			throw new IOException("Error parsing XSLT source.", e);
		} catch ( TransformerConfigurationException e ) {
			log.debug("Error creating XSLT source: {}", e.getMessage(), e);
			throw new IOException("Error creating XSLT source.", e);
		}

		CachedResult<Templates> cacheEntry = new CachedResult<>(t, templatesCacheTtl.getSeconds(),
				TimeUnit.SECONDS);
		if ( xsltCacheKey != null ) {
			if ( cachedXslt != null ) {
				templatesCache.replace(xsltCacheKey, cachedXslt, cacheEntry);
			} else {
				templatesCache.putIfAbsent(xsltCacheKey, cacheEntry);
			}
		}
		return t;
	}

	private List<Datum> parseDatumList(String json) throws IOException {
		JsonNode root = objectMapper.readTree(json);
		if ( root.isObject() ) {
			return Collections.singletonList(objectMapper.treeToValue(root, Datum.class));
		}
		List<Datum> result = new ArrayList<>(root.size());
		for ( JsonNode n : root ) {
			result.add(objectMapper.treeToValue(n, Datum.class));
		}
		return result;
	}

}
