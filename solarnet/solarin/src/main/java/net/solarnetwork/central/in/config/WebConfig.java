/* ==================================================================
 * WebConfig.java - 10/10/2021 12:56:05 PM
 * 
 * Copyright 2021 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.in.config;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.ViewResolverRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import net.solarnetwork.central.web.PingController;
import net.solarnetwork.central.web.support.WebServiceControllerSupport;
import net.solarnetwork.central.web.support.WebServiceErrorAttributes;
import net.solarnetwork.central.web.support.WebServiceGlobalControllerSupport;
import net.solarnetwork.codec.BindingResultSerializer;
import net.solarnetwork.codec.PropertySerializer;
import net.solarnetwork.codec.PropertySerializerRegistrar;
import net.solarnetwork.codec.TimeZonePropertySerializer;
import net.solarnetwork.service.PingTest;
import net.solarnetwork.web.support.JSONView;
import net.solarnetwork.web.support.SimpleXmlView;

/**
 * Web layer configuration.
 * 
 * @author matt
 * @version 1.0
 */
@Configuration
@Import({ WebServiceErrorAttributes.class, WebServiceControllerSupport.class,
		WebServiceGlobalControllerSupport.class })
public class WebConfig implements WebMvcConfigurer {

	@Autowired(required = false)
	private List<PingTest> pingTests;

	@Bean
	public PingController pingController() {
		PingController controller = new PingController();
		controller.setTests(pingTests);
		return controller;
	}

	@SuppressWarnings("deprecation")
	@Override
	public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
		// @formatter:off
		configurer.favorPathExtension(true)
			.favorParameter(false)
			.ignoreAcceptHeader(false)
			.useRegisteredExtensionsOnly(true)
			.defaultContentType(MediaType.APPLICATION_JSON)
			.mediaType("xml", MediaType.TEXT_XML)
			.mediaType("json", MediaType.APPLICATION_JSON);
		// @formatter:on
	}

	@Override
	public void configureViewResolvers(ViewResolverRegistry registry) {
		registry.beanName();
	}

	@Bean
	public PropertySerializerRegistrar propertySerializerRegistrar() {
		PropertySerializerRegistrar reg = new PropertySerializerRegistrar();
		Map<String, PropertySerializer> sers = new LinkedHashMap<>(4);
		sers.put("sun.util.calendar.ZoneInfo", new TimeZonePropertySerializer());
		sers.put("org.springframework.validation.BeanPropertyBindingResult",
				new BindingResultSerializer());
		reg.setClassSerializers(sers);
		return reg;
	}

	@Bean
	public SimpleXmlView xml() {
		SimpleXmlView view = new SimpleXmlView();
		view.setContentType("text/xml;charset=UTF-8");
		view.setPropertySerializerRegistrar(propertySerializerRegistrar());
		view.setClassNamesAllowedForNesting(Collections.singleton("net.solarnetwork"));
		return view;
	}

	@Bean
	public JSONView json() {
		JSONView view = new JSONView();
		view.setContentType("application/json;charset=UTF-8");
		view.setPropertySerializerRegistrar(propertySerializerRegistrar());
		view.setIncludeParentheses(false);
		return view;
	}

}
