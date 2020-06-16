/* ==================================================================
 * DelegatingUserEventHookBiz.java - 11/06/2020 8:24:57 am
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.event.support;

import java.util.List;
import java.util.Locale;
import net.solarnetwork.central.datum.biz.DatumAppEventProducer;
import net.solarnetwork.central.user.domain.UserLongPK;
import net.solarnetwork.central.user.domain.UserRelatedIdentifiableConfiguration;
import net.solarnetwork.central.user.event.biz.UserEventHookBiz;
import net.solarnetwork.central.user.event.biz.UserNodeEventHookService;
import net.solarnetwork.domain.LocalizedServiceInfo;

/**
 * Delegating implementation of {@link UserEventHookBiz}, mostly to help with
 * AOP.
 * 
 * @author matt
 * @version 1.0
 */
public class DelegatingUserEventHookBiz implements UserEventHookBiz {

	private final UserEventHookBiz delegate;

	/**
	 * Constructor.
	 * 
	 * @param delegate
	 *        the delegate
	 * @throws IllegalArgumentException
	 *         if {@code delegate} is {@literal null}
	 */
	public DelegatingUserEventHookBiz(UserEventHookBiz delegate) {
		super();
		if ( delegate == null ) {
			throw new IllegalArgumentException("The delegate argument must not be null.");
		}
		this.delegate = delegate;
	}

	@Override
	public Iterable<DatumAppEventProducer> availableDatumEventProducers() {
		return delegate.availableDatumEventProducers();
	}

	@Override
	public Iterable<UserNodeEventHookService> availableNodeEventHookServices() {
		return delegate.availableNodeEventHookServices();
	}

	@Override
	public Iterable<LocalizedServiceInfo> availableDatumEventTopics(Locale locale) {
		return delegate.availableDatumEventTopics(locale);
	}

	@Override
	public <T extends UserRelatedIdentifiableConfiguration> T configurationForUser(Long userId,
			Class<T> configurationClass, Long id) {
		return delegate.configurationForUser(userId, configurationClass, id);
	}

	@Override
	public UserLongPK saveConfiguration(UserRelatedIdentifiableConfiguration configuration) {
		return delegate.saveConfiguration(configuration);
	}

	@Override
	public void deleteConfiguration(UserRelatedIdentifiableConfiguration configuration) {
		delegate.deleteConfiguration(configuration);
	}

	@Override
	public <T extends UserRelatedIdentifiableConfiguration> List<T> configurationsForUser(Long userId,
			Class<T> configurationClass) {
		return delegate.configurationsForUser(userId, configurationClass);
	}

}
