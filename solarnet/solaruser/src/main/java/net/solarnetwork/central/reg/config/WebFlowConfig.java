/* ==================================================================
 * WebFlowConfig.java - 21/10/2021 5:28:51 PM
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

package net.solarnetwork.central.reg.config;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.webflow.config.AbstractFlowConfiguration;
import org.springframework.webflow.definition.registry.FlowDefinitionRegistry;
import org.springframework.webflow.engine.builder.support.FlowBuilderServices;
import org.springframework.webflow.executor.FlowExecutor;
import org.springframework.webflow.mvc.builder.MvcViewFactoryCreator;
import org.springframework.webflow.mvc.servlet.FlowHandlerAdapter;
import org.springframework.webflow.mvc.servlet.FlowHandlerMapping;
import org.springframework.webflow.security.SecurityFlowExecutionListener;

/**
 * WebFlow configuration.
 * 
 * @author matt
 * @version 1.0
 */
@Configuration
public class WebFlowConfig extends AbstractFlowConfiguration {

	@Value("${debug:false}")
	private boolean debugMode = false;

	@Autowired
	private List<ViewResolver> viewResolvers;

	@Bean
	public FlowDefinitionRegistry flowRegistry() {
		// @formatter:off
		return getFlowDefinitionRegistryBuilder(flowBuilderServices())
				.setBasePath("classpath*:/flows")
                .addFlowLocationPattern("/**/*-flow.xml")
				.build();
		// @formatter:on
	}

	@Bean
	public FlowExecutor flowExecutor() {
		// @formatter:off
		return getFlowExecutorBuilder(flowRegistry())
				.addFlowExecutionListener(new SecurityFlowExecutionListener())
				.build();
		// @formatter:on
	}

	@Bean
	public FlowBuilderServices flowBuilderServices() {
		// @formatter:off
		return getFlowBuilderServicesBuilder().
				setViewFactoryCreator(mvcViewFactoryCreator())
				.setDevelopmentMode(debugMode)
				.build();
		// @formatter:on
	}

	@Bean
	public MvcViewFactoryCreator mvcViewFactoryCreator() {
		MvcViewFactoryCreator factoryCreator = new MvcViewFactoryCreator();
		factoryCreator.setViewResolvers(viewResolvers);
		factoryCreator.setUseSpringBeanBinding(true);
		return factoryCreator;
	}

	@Bean
	public FlowHandlerMapping flowHandlerMapping() {
		FlowHandlerMapping handlerMapping = new FlowHandlerMapping();
		handlerMapping.setOrder(1);
		handlerMapping.setFlowRegistry(flowRegistry());
		return handlerMapping;
	}

	@Bean
	public FlowHandlerAdapter flowHandlerAdapter() {
		FlowHandlerAdapter adapter = new FlowHandlerAdapter();
		adapter.setFlowExecutor(flowExecutor());
		return adapter;
	}

}
