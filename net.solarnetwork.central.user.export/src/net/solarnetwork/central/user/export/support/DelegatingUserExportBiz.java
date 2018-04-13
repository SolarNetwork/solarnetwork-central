/* ==================================================================
 * DelegatingUserExportBiz.java - 22/03/2018 8:37:44 PM
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

package net.solarnetwork.central.user.export.support;

import java.util.List;
import java.util.Locale;
import net.solarnetwork.central.datum.biz.DatumExportDestinationService;
import net.solarnetwork.central.datum.biz.DatumExportOutputFormatService;
import net.solarnetwork.central.user.export.biz.UserExportBiz;
import net.solarnetwork.central.user.export.domain.UserDatumExportConfiguration;
import net.solarnetwork.central.user.export.domain.UserIdentifiableConfiguration;
import net.solarnetwork.domain.LocalizedServiceInfo;

/**
 * Delegating implementation of {@link UserExportBiz}, mostly to help with AOP.
 * 
 * @author matt
 * @version 1.0
 */
public class DelegatingUserExportBiz implements UserExportBiz {

	private final UserExportBiz delegate;

	/**
	 * Construct with a delegate;
	 * 
	 * @param delegate
	 *        the delegate
	 */
	public DelegatingUserExportBiz(UserExportBiz delegate) {
		super();
		this.delegate = delegate;
	}

	@Override
	public Iterable<DatumExportOutputFormatService> availableOutputFormatServices() {
		return delegate.availableOutputFormatServices();
	}

	@Override
	public Iterable<DatumExportDestinationService> availableDestinationServices() {
		return delegate.availableDestinationServices();
	}

	@Override
	public Iterable<LocalizedServiceInfo> availableOutputCompressionTypes(Locale locale) {
		return delegate.availableOutputCompressionTypes(locale);
	}

	@Override
	public UserDatumExportConfiguration datumExportConfiguration(Long id) {
		return delegate.datumExportConfiguration(id);
	}

	@Override
	public Long saveDatumExportConfiguration(UserDatumExportConfiguration configuration) {
		return delegate.saveDatumExportConfiguration(configuration);
	}

	@Override
	public List<UserDatumExportConfiguration> datumExportsForUser(Long userId) {
		return delegate.datumExportsForUser(userId);
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
