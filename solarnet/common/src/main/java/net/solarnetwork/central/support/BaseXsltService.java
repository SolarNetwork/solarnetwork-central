/* ==================================================================
 * BaseXsltService.java - 29/03/2024 2:38:21 pm
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

package net.solarnetwork.central.support;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Templates;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.util.FileCopyUtils;
import org.w3c.dom.Document;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.service.IdentifiableConfiguration;
import net.solarnetwork.settings.support.BaseSettingsSpecifierLocalizedServiceInfoProvider;

/**
 * Base service class for XSLT support.
 *
 * @author matt
 * @version 1.0
 */
public abstract class BaseXsltService extends BaseSettingsSpecifierLocalizedServiceInfoProvider<String>
		implements EntityResolver {

	/** The setting key for the XSLT stylesheet. */
	public static final String SETTING_XSLT = "xslt";

	/** The setting key for the XSLT stylesheet cache seconds. */
	public static final String SETTING_XSLT_CACHE_DURATION = "cache-seconds";

	/** The XSLT input parameter for input JSON. */
	public static final String XSLT_PARAM_JSON = "input-json";

	/** The transformer factory. */
	protected final TransformerFactory transformerFactory;

	/** The document builder factory. */
	protected final DocumentBuilderFactory documentBuilderFactory;

	/** A JSON mapper. */
	protected final ObjectMapper objectMapper;

	/** A templates cache duration. */
	protected final Duration templatesCacheTtl;

	/** A cache for templates. */
	protected final SharedValueCache<String, Templates, String> templatesCache;

	/**
	 * Constructor.
	 *
	 * @param serviceId
	 *        the service ID to use
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
	public BaseXsltService(String serviceId, DocumentBuilderFactory documentBuilderFactory,
			TransformerFactory transformerFactory, ObjectMapper objectMapper,
			Duration templatesCacheTtl) {
		this(serviceId, documentBuilderFactory, transformerFactory, objectMapper, templatesCacheTtl,
				new BasicSharedValueCache<>());
	}

	/**
	 * Constructor.
	 *
	 * <p>
	 * If a {@code templatesCache} is provided then XSLT templates will be
	 * cached for up to the configured {@code templatesCacheTtl} seconds. The
	 * templates cache key can be provided by a {@link #PARAM_XSLT_CACHE_KEY}
	 * parameter value, or will be derived from the XSLT value itself.
	 * </p>
	 *
	 * @param serviceId
	 *        the service ID to use
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
	public BaseXsltService(String serviceId, DocumentBuilderFactory documentBuilderFactory,
			TransformerFactory transformerFactory, ObjectMapper objectMapper, Duration templatesCacheTtl,
			SharedValueCache<String, Templates, String> templatesCache) {
		super(serviceId);
		this.documentBuilderFactory = requireNonNullArgument(documentBuilderFactory,
				"documentBuilderFactory");
		this.transformerFactory = requireNonNullArgument(transformerFactory, "transformerFactory");
		this.objectMapper = requireNonNullArgument(objectMapper, "objectMapper");
		this.templatesCacheTtl = templatesCacheTtl != null && templatesCache != null ? templatesCacheTtl
				: Duration.ZERO;
		this.templatesCache = templatesCache;
	}

	@Override
	public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
		return null;
	}

	/**
	 * Get a new document builder.
	 * 
	 * <p>
	 * This instance will be configured as the {@link EntityResolver}.
	 * </p>
	 * 
	 * @return the new document builder
	 * @throws ParserConfigurationException
	 *         if an XML error occurs
	 */
	protected DocumentBuilder documentBuilder() throws ParserConfigurationException {
		DocumentBuilder db = documentBuilderFactory.newDocumentBuilder();
		db.setEntityResolver(this);
		return db;
	}

	/**
	 * Get a {@link Templates} instance.
	 * 
	 * @param xslt
	 *        the XSLT to parse
	 * @param config
	 *        optional settings to use, for example
	 *        {@link #SETTING_XSLT_CACHE_DURATION}
	 * @param cacheKey
	 *        optional cache key to use (will be converted to a string),
	 *        otherwise generate a key based on the XSLT itself
	 * @return the templates, never {@literal null}
	 * @throws IOException
	 *         if any error occurs parsing the XSLT
	 */
	protected Templates templates(String xslt, IdentifiableConfiguration config, Object cacheKey)
			throws IOException {
		final long cacheTtlSeconds = templatesCacheTtlSeconds(config);

		String xsltCacheKey = null;
		String xsltSharedKey = null;
		Templates t = null;

		if ( cacheTtlSeconds > 0 ) {
			if ( cacheKey != null ) {
				xsltCacheKey = cacheKey.toString();
			} else {
				xsltCacheKey = DigestUtils.sha256Hex(xslt);
				xsltSharedKey = xsltCacheKey;
			}
			t = templatesCache.get(xsltCacheKey);
			if ( t != null ) {
				return t;
			}
		}

		Function<String, Templates> provider = (key) -> {
			try (Reader xsltInput = new StringReader(xslt)) {
				Document xsltDoc = documentBuilder().parse(new InputSource(xsltInput));
				return transformerFactory.newTemplates(new DOMSource(xsltDoc));
			} catch ( SAXException | ParserConfigurationException | IOException e ) {
				log.debug("Error parsing XSLT source: {}", e.getMessage(), e);
				throw new IllegalStateException("Error parsing XSLT source.", e);
			} catch ( TransformerConfigurationException e ) {
				log.debug("Error creating XSLT source: {}", e.getMessage(), e);
				throw new IllegalStateException("Error creating XSLT source.", e);
			}
		};

		if ( xsltSharedKey == null ) {
			xsltSharedKey = DigestUtils.sha256Hex(xslt);
		}
		try {
			if ( xsltCacheKey != null ) {
				t = templatesCache.put(xsltCacheKey, xsltSharedKey, provider, cacheTtlSeconds);
			} else {
				t = provider.apply(xsltCacheKey);
			}
		} catch ( IllegalStateException e ) {
			throw new IOException(e.getMessage(), e.getCause());
		}
		return t;
	}

	/**
	 * Get the templates cache TTL seconds.
	 * 
	 * @param config
	 *        the optional settings
	 * @return the TTL, in seconds
	 */
	protected long templatesCacheTtlSeconds(IdentifiableConfiguration config) {
		Map<String, ?> props = (config != null ? config.getServiceProperties() : null);
		Object val = (props != null ? props.get(SETTING_XSLT_CACHE_DURATION) : null);
		if ( val != null ) {
			if ( val instanceof Number n ) {
				return n.longValue();
			}
			try {
				return Long.parseLong(val.toString());
			} catch ( NumberFormatException e ) {
				// ignore
			}
		}
		return templatesCacheTtl.getSeconds();
	}

	/** A regular expression matching an XML {@literal DOCTYPE} declaration. */
	public static final Pattern DOCTYPE_PAT = Pattern.compile("<!DOCTYPE[^>]*>",
			Pattern.CASE_INSENSITIVE);

	/**
	 * Get textual input.
	 * 
	 * @param input
	 *        the input, can be an {@link InputStream}, {@link Reader}, or
	 *        anything else will have {@link Object#toString()} invoked; UTF-8
	 *        will be assumed for streams
	 * @return the text value, never {@literal null}
	 * @throws IOException
	 *         if any IO error occurs
	 */
	protected static String inputText(Object input) throws IOException {
		String result = "";
		if ( input instanceof InputStream stream ) {
			result = FileCopyUtils.copyToString(new InputStreamReader(stream, StandardCharsets.UTF_8));
		} else if ( input instanceof Reader reader ) {
			result = FileCopyUtils.copyToString(reader);
		} else if ( input != null ) {
			result = input.toString();
		}
		// remove <!DOCTYPE> declaration
		return DOCTYPE_PAT.matcher(result).replaceFirst("");
	}

}
