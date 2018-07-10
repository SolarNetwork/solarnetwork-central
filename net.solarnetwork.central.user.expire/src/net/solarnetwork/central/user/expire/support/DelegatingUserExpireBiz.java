/* ==================================================================
 * DelegatingUserExpireBiz.java - 9/07/2018 10:21:57 AM
 * 
 * Copyright 2018 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.expire.support;

import java.util.List;
import java.util.Locale;
import net.solarnetwork.central.user.domain.UserIdentifiableConfiguration;
import net.solarnetwork.central.user.expire.biz.UserExpireBiz;
import net.solarnetwork.domain.LocalizedServiceInfo;

/**
 * Delegating implementation of {@link UserExpireBiz}, mostly to help with AOP.
 * 
 * @author matt
 * @version 1.0
 */
public class DelegatingUserExpireBiz implements UserExpireBiz {

	private final UserExpireBiz delegate;

	/**
	 * Construct with a delegate;
	 * 
	 * @param delegate
	 *        the delegate
	 */
	public DelegatingUserExpireBiz(UserExpireBiz delegate) {
		super();
		this.delegate = delegate;
	}

	@Override
	public Iterable<LocalizedServiceInfo> availableAggregationTypes(Locale locale) {
		return delegate.availableAggregationTypes(locale);
	}

	@Override
	public <T extends UserIdentifiableConfiguration> T configurationForUser(Long userId,
			Class<T> configurationClass, Long id) {
		return delegate.configurationForUser(userId, configurationClass, id);
	}

	@Override
	public Long saveConfiguration(UserIdentifiableConfiguration configuration) {
		return delegate.saveConfiguration(configuration);
	}

	@Override
	public void deleteConfiguration(UserIdentifiableConfiguration configuration) {
		delegate.deleteConfiguration(configuration);
	}

	@Override
	public <T extends UserIdentifiableConfiguration> List<T> configurationsForUser(Long userId,
			Class<T> configurationClass) {
		return delegate.configurationsForUser(userId, configurationClass);
	}

}
