/* ==================================================================
 * SecurityFlowExecutionListener.java - Feb 2, 2010 4:38:41 PM
 * 
 * Copyright 2007-2010 SolarNetwork.net Dev Team
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
 * $Id$
 * ==================================================================
 */

package net.solarnetwork.central.security.web;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.springframework.security.access.AccessDecisionManager;
import org.springframework.security.access.AccessDecisionVoter;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.access.SecurityConfig;
import org.springframework.security.access.vote.AbstractAccessDecisionManager;
import org.springframework.security.access.vote.AffirmativeBased;
import org.springframework.security.access.vote.RoleVoter;
import org.springframework.security.access.vote.UnanimousBased;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.webflow.definition.FlowDefinition;
import org.springframework.webflow.definition.StateDefinition;
import org.springframework.webflow.definition.TransitionDefinition;
import org.springframework.webflow.execution.EnterStateVetoException;
import org.springframework.webflow.execution.FlowExecutionListenerAdapter;
import org.springframework.webflow.execution.RequestContext;
import org.springframework.webflow.security.SecurityRule;

/**
 * Implementation of {@link FlowExecutionListenerAdapter} compatible with 
 * Spring Security 3.0.
 * 
 * <p>Spring Web Flow 2.0.8 isn't compatible with Spring Security 3.0.
 * Adapted from http://forum.springsource.org/showthread.php?t=83163.</p>
 * 
 * @author matt
 * @version $Id$
 */
public class SecurityFlowExecutionListener extends FlowExecutionListenerAdapter {

	private AccessDecisionManager accessDecisionManager;

	/**
	 * Get the access decision manager that makes flow authorization decisions.
	 * @return the decision manager
	 */
	public AccessDecisionManager getAccessDecisionManager() {
		return accessDecisionManager;
	}

	/**
	 * Set the access decision manager that makes flow authorization decisions.
	 * @param accessDecisionManager the decision manager to user
	 */
	public void setAccessDecisionManager(AccessDecisionManager accessDecisionManager) {
		this.accessDecisionManager = accessDecisionManager;
	}

	public void sessionCreating(RequestContext context, FlowDefinition definition) {
		SecurityRule rule = (SecurityRule) definition.getAttributes().get(SecurityRule.SECURITY_ATTRIBUTE_NAME);
		if (rule != null) {
			decide(rule, definition);
		}
	}

	public void stateEntering(RequestContext context, StateDefinition state) throws EnterStateVetoException {
		SecurityRule rule = (SecurityRule) state.getAttributes().get(SecurityRule.SECURITY_ATTRIBUTE_NAME);
		if (rule != null) {
			decide(rule, state);
		}
	}

	public void transitionExecuting(RequestContext context, TransitionDefinition transition) {
		SecurityRule rule = (SecurityRule) transition.getAttributes().get(SecurityRule.SECURITY_ATTRIBUTE_NAME);
		if (rule != null) {
			decide(rule, transition);
		}
	}

	/**
	 * Performs a Spring Security authorization decision. Decision will use the provided AccessDecisionManager. If no
	 * AccessDecisionManager is provided a role based manager will be selected according to the comparison type of the
	 * rule.
	 * @param rule the rule to base the decision
	 * @param object the execution listener phase
	 */
	protected void decide(SecurityRule rule, Object object) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		List<ConfigAttribute> config = getConfigAttributes(rule);
		if (accessDecisionManager != null) {
			accessDecisionManager.decide(authentication, object, config);
		} else {
			AbstractAccessDecisionManager abstractAccessDecisionManager;
			List<AccessDecisionVoter> voters = new ArrayList<AccessDecisionVoter>();
			voters.add(new RoleVoter());
			if (rule.getComparisonType() == SecurityRule.COMPARISON_ANY) {
				abstractAccessDecisionManager = new AffirmativeBased();
			} else if (rule.getComparisonType() == SecurityRule.COMPARISON_ALL) {
				abstractAccessDecisionManager = new UnanimousBased();
			} else {
				throw new IllegalStateException("Unknown SecurityRule match type: " + rule.getComparisonType());
			}
			abstractAccessDecisionManager.setDecisionVoters(voters);
			abstractAccessDecisionManager.decide(authentication, object, config);
		}
	}

	/**
	 * Convert SecurityRule into a form understood by Spring Security
	 * @param rule the rule to convert
	 * @return list of ConfigAttributes for Spring Security
	 */
	@SuppressWarnings("unchecked")
	protected List<ConfigAttribute> getConfigAttributes(SecurityRule rule) {
		List<ConfigAttribute> configAttributes = new ArrayList<ConfigAttribute>();
		Iterator<String> attributeIt = rule.getAttributes().iterator();
		while (attributeIt.hasNext()) {
			configAttributes.add(new SecurityConfig(attributeIt.next()));
		}
		return configAttributes;
	}

}
