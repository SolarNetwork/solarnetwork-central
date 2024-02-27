/* ==================================================================
 * DatumInputTransformServiceConfig.java - 24/02/2024 11:43:58 am
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

package net.solarnetwork.central.din.config;

import java.time.Duration;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.sf.saxon.TransformerFactoryImpl;
import net.solarnetwork.central.din.biz.impl.XsltTransformService;
import net.solarnetwork.central.support.BasicSharedValueCache;
import net.solarnetwork.central.support.CacheSettings;
import net.solarnetwork.central.support.SharedValueCache;

/**
 * Configuration for transform services.
 *
 * @author matt
 * @version 1.0
 */
@Configuration(proxyBeanMethods = false)
public class DatumInputTransformServiceConfig implements URIResolver {

	public static final String XSLT_TEMPLATES_QUALIFIER = "xslt-templates";

	@Bean
	@ConfigurationProperties(prefix = "app.din.xslt.templates-cache")
	@Qualifier(XSLT_TEMPLATES_QUALIFIER)
	public CacheSettings xsltTemplatesCacheSettings() {
		return new CacheSettings();
	}

	@Qualifier(XSLT_TEMPLATES_QUALIFIER)
	@Bean
	public SharedValueCache<String, Templates, String> xsltTemplatesCache() {
		return new BasicSharedValueCache<>();
	}

	@Bean
	public XsltTransformService xsltTransformService(ObjectMapper objectMapper,
			@Qualifier(XSLT_TEMPLATES_QUALIFIER) CacheSettings templatesCacheSettings,
			@Qualifier(XSLT_TEMPLATES_QUALIFIER) SharedValueCache<String, Templates, String> templatesCache)
			throws ParserConfigurationException {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newNSInstance();
		dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

		TransformerFactoryImpl tf = new net.sf.saxon.TransformerFactoryImpl();
		tf.setURIResolver(this);

		var service = new XsltTransformService(dbf, tf, objectMapper,
				Duration.ofSeconds(templatesCacheSettings.getTtl()), templatesCache);

		ResourceBundleMessageSource msgSource = new ResourceBundleMessageSource();
		msgSource.setBasenames(XsltTransformService.class.getName());
		service.setMessageSource(msgSource);

		return service;
	}

	@Override
	public Source resolve(String href, String base) throws TransformerException {
		throw new UnsupportedOperationException("External resources are not allowed (" + href + ").");
	}

}
