/* ==================================================================
 * DelegatingAppConfigurationBiz.java - 2/10/2017 10:24:39 AM
 * 
 * Copyright 2017 SolarNetwork.net Dev Team
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

import net.solarnetwork.central.biz.AppConfigurationBiz;
import net.solarnetwork.central.domain.AppConfiguration;

/**
 * Implementation of {@link AppConfigurationBiz} that delegates to another
 * {@link AppConfigurationBiz}. Designed for use with AOP.
 * 
 * @author matt
 * @version 1.0
 * @since 1.35
 */
public class DelegatingAppConfigurationBiz implements AppConfigurationBiz {

	private final AppConfigurationBiz delegate;

	/**
	 * Construct with a delegate.
	 * 
	 * @param delegate
	 *        the delegate
	 */
	public DelegatingAppConfigurationBiz(AppConfigurationBiz delegate) {
		super();
		this.delegate = delegate;
	}

	@Override
	public AppConfiguration getAppConfiguration() {
		return delegate.getAppConfiguration();
	}

}
