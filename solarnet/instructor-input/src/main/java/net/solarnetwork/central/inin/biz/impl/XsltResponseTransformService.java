/* ==================================================================
 * BaseXsltTransformService.java - 29/03/2024 2:38:21 pm
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

package net.solarnetwork.central.inin.biz.impl;

import static net.solarnetwork.central.inin.biz.TransformConstants.PARAM_CONFIGURATION_CACHE_KEY;
import java.io.IOException;
import java.io.OutputStream;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.xml.parsers.DocumentBuilderFactory;
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
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.central.inin.biz.ResponseTransformService;
import net.solarnetwork.central.instructor.domain.NodeInstruction;
import net.solarnetwork.central.support.BasicSharedValueCache;
import net.solarnetwork.central.support.SharedValueCache;
import net.solarnetwork.central.support.xslt.BaseXsltService;
import net.solarnetwork.service.IdentifiableConfiguration;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.BasicTextAreaSettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;

/**
 * XSLT implementation of {@link ResponseTransformService}.
 *
 * <p>
 * The {@code instructions} passed to
 * {@link #transformOutput(Iterable, MimeType, IdentifiableConfiguration, Map, OutputStream)}
 * will be converted to JSON and passed to the XSTL templates as the
 * {@link BaseXsltService#XSLT_PARAM_JSON} input parameter.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
public class XsltResponseTransformService extends BaseXsltService implements ResponseTransformService {

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
	public XsltResponseTransformService(DocumentBuilderFactory documentBuilderFactory,
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
	 * cached for up to the configured {@code templatesCacheTtl} seconds. The
	 * templates cache key can be provided by a {@link #PARAM_XSLT_CACHE_KEY}
	 * parameter value, or will be derived from the XSLT value itself.
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
	public XsltResponseTransformService(DocumentBuilderFactory documentBuilderFactory,
			TransformerFactory transformerFactory, ObjectMapper objectMapper, Duration templatesCacheTtl,
			SharedValueCache<String, Templates, String> templatesCache) {
		super("net.solarnetwork.central.inin.XsltResponseTransformService", documentBuilderFactory,
				transformerFactory, objectMapper, templatesCacheTtl, templatesCache);
	}

	@Override
	public String getDisplayName() {
		return "XSLT Request Transform Service";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		return Arrays.asList(new BasicTextAreaSettingSpecifier(SETTING_XSLT, null, true),
				new BasicTextFieldSettingSpecifier(SETTING_XSLT_CACHE_DURATION, null));
	}

	@Override
	public boolean supportsOutputType(MimeType type) {
		return true;
	}

	@Override
	public void transformOutput(Iterable<NodeInstruction> instructions, MimeType type,
			IdentifiableConfiguration config, Map<String, ?> parameters, OutputStream out)
			throws IOException {
		if ( instructions == null ) {
			return;
		}
		final String json = objectMapper.writeValueAsString(instructions);
		Map<String, ?> props = config.getServiceProperties();
		Object xslt = (props != null ? props.get(SETTING_XSLT) : null);
		if ( xslt == null ) {
			return;
		}
		Templates templates = templates(xslt.toString(), config, parameters);
		try {
			// configure transform
			Transformer xform = templates.newTransformer();
			if ( parameters != null ) {
				for ( Entry<String, ?> e : parameters.entrySet() ) {
					String key = e.getKey();
					xform.setParameter(key, e.getValue());
				}
			}

			// get JSON + (empty) XML input for XSLT transform
			xform.setParameter(XSLT_PARAM_JSON, json);
			Source inputSource = new DOMSource();

			// execute transform
			try {
				Result result = new StreamResult(out);
				xform.transform(inputSource, result);
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

}
