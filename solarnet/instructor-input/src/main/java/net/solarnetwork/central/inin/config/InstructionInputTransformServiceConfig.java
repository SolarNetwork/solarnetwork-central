/* ==================================================================
 * InstructionInputTransformServiceConfig.java - 30/03/2024 7:19:22 am
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

package net.solarnetwork.central.inin.config;

import static net.solarnetwork.central.inin.config.SolarNetInstructionInputConfiguration.INSTRUCTION_INPUT;
import java.time.Duration;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Templates;
import javax.xml.transform.URIResolver;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.sf.saxon.TransformerFactoryImpl;
import net.solarnetwork.central.inin.biz.impl.DataUriResolver;
import net.solarnetwork.central.inin.biz.impl.XsltRequestTransformService;
import net.solarnetwork.central.inin.biz.impl.XsltResponseTransformService;
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
public class InstructionInputTransformServiceConfig {

	/** Qualifier for request templates. */
	public static final String REQ_XSLT_TEMPLATES_QUALIFIER = "req-xslt-templates";

	/** Qualifier for response templates. */
	public static final String RES_XSLT_TEMPLATES_QUALIFIER = "res-xslt-templates";

	@Bean
	@ConfigurationProperties(prefix = "app.inin.xslt.req-templates-cache")
	@Qualifier(REQ_XSLT_TEMPLATES_QUALIFIER)
	public CacheSettings requestXsltTemplatesCacheSettings() {
		return new CacheSettings();
	}

	@Qualifier(REQ_XSLT_TEMPLATES_QUALIFIER)
	@Bean
	public SharedValueCache<String, Templates, String> requestXsltTemplatesCache() {
		return new BasicSharedValueCache<>();
	}

	@Bean
	@ConfigurationProperties(prefix = "app.inin.xslt.res-templates-cache")
	@Qualifier(RES_XSLT_TEMPLATES_QUALIFIER)
	public CacheSettings responseXsltTemplatesCacheSettings() {
		return new CacheSettings();
	}

	@Qualifier(RES_XSLT_TEMPLATES_QUALIFIER)
	@Bean
	public SharedValueCache<String, Templates, String> responseXsltTemplatesCache() {
		return new BasicSharedValueCache<>();
	}

	@Bean
	@Qualifier(INSTRUCTION_INPUT)
	public DataUriResolver ininDataUriResolver() {
		return new DataUriResolver();
	}

	@Bean
	public XsltRequestTransformService requestXsltTransformService(ObjectMapper objectMapper,
			@Qualifier(REQ_XSLT_TEMPLATES_QUALIFIER) CacheSettings templatesCacheSettings,
			@Qualifier(REQ_XSLT_TEMPLATES_QUALIFIER) SharedValueCache<String, Templates, String> templatesCache,
			@Qualifier(INSTRUCTION_INPUT) URIResolver uriResolver) throws ParserConfigurationException {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newNSInstance();

		// disable external access, see
		// https://docs.oracle.com/en/java/javase/17/security/java-api-xml-processing-jaxp-security-guide.html
		dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

		TransformerFactoryImpl tf = new net.sf.saxon.TransformerFactoryImpl();
		tf.setURIResolver(uriResolver);

		var service = new XsltRequestTransformService(dbf, tf, objectMapper,
				Duration.ofSeconds(templatesCacheSettings.getTtl()), templatesCache);

		ResourceBundleMessageSource msgSource = new ResourceBundleMessageSource();
		msgSource.setBasenames(XsltRequestTransformService.class.getName());
		service.setMessageSource(msgSource);

		return service;
	}

	@Bean
	public XsltResponseTransformService responseXsltTransformService(ObjectMapper objectMapper,
			@Qualifier(RES_XSLT_TEMPLATES_QUALIFIER) CacheSettings templatesCacheSettings,
			@Qualifier(RES_XSLT_TEMPLATES_QUALIFIER) SharedValueCache<String, Templates, String> templatesCache,
			@Qualifier(INSTRUCTION_INPUT) URIResolver uriResolver) throws ParserConfigurationException {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newNSInstance();

		// disable external access, see
		// https://docs.oracle.com/en/java/javase/17/security/java-api-xml-processing-jaxp-security-guide.html
		dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

		TransformerFactoryImpl tf = new net.sf.saxon.TransformerFactoryImpl();
		tf.setURIResolver(uriResolver);

		var service = new XsltResponseTransformService(dbf, tf, objectMapper,
				Duration.ofSeconds(templatesCacheSettings.getTtl()), templatesCache);

		ResourceBundleMessageSource msgSource = new ResourceBundleMessageSource();
		msgSource.setBasenames(XsltResponseTransformService.class.getName());
		service.setMessageSource(msgSource);

		return service;
	}

}
