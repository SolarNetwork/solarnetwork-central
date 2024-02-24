/* ==================================================================
 * DaoUserDatumInputBiz.java - 25/02/2024 7:25:08 am
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

package net.solarnetwork.central.user.din.biz.impl;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.Serializable;
import java.util.Collection;
import java.util.Locale;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import jakarta.validation.Validator;
import net.solarnetwork.central.common.dao.GenericCompositeKey2Dao;
import net.solarnetwork.central.common.dao.GenericCompositeKey3Dao;
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;
import net.solarnetwork.central.din.biz.TransformService;
import net.solarnetwork.central.din.dao.CredentialConfigurationDao;
import net.solarnetwork.central.din.dao.EndpointAuthConfigurationDao;
import net.solarnetwork.central.din.dao.EndpointConfigurationDao;
import net.solarnetwork.central.din.dao.TransformConfigurationDao;
import net.solarnetwork.central.din.domain.CredentialConfiguration;
import net.solarnetwork.central.din.domain.DatumInputConfigurationEntity;
import net.solarnetwork.central.din.domain.EndpointAuthConfiguration;
import net.solarnetwork.central.din.domain.EndpointConfiguration;
import net.solarnetwork.central.din.domain.TransformConfiguration;
import net.solarnetwork.central.domain.CompositeKey;
import net.solarnetwork.central.domain.UserIdRelated;
import net.solarnetwork.central.user.din.biz.UserDatumInputBiz;
import net.solarnetwork.central.user.din.domain.DatumInputConfigurationInput;
import net.solarnetwork.domain.LocalizedServiceInfo;
import net.solarnetwork.service.LocalizedServiceInfoProvider;

/**
 * DAO based implementation of {@Link UserDatumInputBiz}.
 *
 * @author matt
 * @version 1.0
 */
public class DaoUserDatumInputBiz implements UserDatumInputBiz {

	private final SolarNodeOwnershipDao nodeOwnershipDao;
	private final CredentialConfigurationDao credentialDao;
	private final TransformConfigurationDao transformDao;
	private final EndpointConfigurationDao endpointDao;
	private final EndpointAuthConfigurationDao endpointAuthDao;
	private final Collection<TransformService> transformServices;

	private Validator validator;

	/**
	 * Constructor.
	 *
	 * @param nodeOwnershipDao
	 *        the node ownership DAO
	 * @param credentialDao
	 *        the credential DAO
	 * @param transformDao
	 *        the transform DAO
	 * @param endpointDao
	 *        the endpoint DAO
	 * @param endpointAuthDao
	 *        the endpoint authorization DAO
	 * @param transformServices
	 *        the transform services
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public DaoUserDatumInputBiz(SolarNodeOwnershipDao nodeOwnershipDao,
			CredentialConfigurationDao credentialDao, TransformConfigurationDao transformDao,
			EndpointConfigurationDao endpointDao, EndpointAuthConfigurationDao endpointAuthDao,
			Collection<TransformService> transformServices) {
		super();
		this.nodeOwnershipDao = requireNonNullArgument(nodeOwnershipDao, "nodeOwnershipDao");
		this.credentialDao = requireNonNullArgument(credentialDao, "credentialDao");
		this.transformDao = requireNonNullArgument(transformDao, "transformDao");
		this.endpointDao = requireNonNullArgument(endpointDao, "endpointDao");
		this.endpointAuthDao = requireNonNullArgument(endpointAuthDao, "endpointAuthDao");
		this.transformServices = requireNonNullArgument(transformServices, "transformServices");
	}

	@Override
	public Iterable<LocalizedServiceInfo> availableTransformServices(Locale locale) {
		return LocalizedServiceInfoProvider.localizedServiceSettings(transformServices, locale);
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public <C extends DatumInputConfigurationEntity<?, ?>> Collection<C> configurationsForUser(
			Long userId, Class<C> configurationClass) {
		requireNonNullArgument(userId, "userId");
		requireNonNullArgument(configurationClass, "configurationClass");
		Collection<C> result = null;
		if ( CredentialConfiguration.class.isAssignableFrom(configurationClass) ) {
			result = findAllForUser(userId, credentialDao);
		} else if ( TransformConfiguration.class.isAssignableFrom(configurationClass) ) {
			result = findAllForUser(userId, transformDao);
		} else if ( EndpointConfiguration.class.isAssignableFrom(configurationClass) ) {
			result = findAllForUser(userId, endpointDao);
		} else if ( EndpointAuthConfiguration.class.isAssignableFrom(configurationClass) ) {
			result = findAllForUser(userId, endpointAuthDao);
		}
		if ( result != null ) {
			// remove credentials before returning
			for ( C c : result ) {
				c.eraseCredentials();
			}
			return result;

		}
		throw new UnsupportedOperationException(
				"Configuration type %s not supported.".formatted(configurationClass));
	}

	@SuppressWarnings("unchecked")
	private <C extends DatumInputConfigurationEntity<?, ?>> Collection<C> findAllForUser(Long user,
			GenericCompositeKey2Dao<?, ?, Long, ?> dao) {
		return (Collection<C>) dao.findAll(user, null);
	}

	@SuppressWarnings("unchecked")
	private <C extends DatumInputConfigurationEntity<?, ?>> Collection<C> findAllForUser(Long user,
			GenericCompositeKey3Dao<?, ?, Long, ?, ?> dao) {
		return (Collection<C>) dao.findAll(user, null, null);
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public <C extends DatumInputConfigurationEntity<?, K>, K extends CompositeKey & Comparable<K> & Serializable & UserIdRelated> C configurationForUser(
			K id, Class<C> configurationClass) {
		// TODO Auto-generated method stub
		return null;
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public <T extends DatumInputConfigurationInput<C, K>, C extends DatumInputConfigurationEntity<C, K>, K extends CompositeKey & Comparable<K> & Serializable & UserIdRelated> C saveConfiguration(
			K id, T input) {
		// TODO Auto-generated method stub
		return null;
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public <C extends DatumInputConfigurationEntity<?, K>, K extends CompositeKey & Comparable<K> & Serializable & UserIdRelated> void deleteConfiguration(
			K id, Class<C> configurationClass) {
		// TODO Auto-generated method stub

	}

	/**
	 * Get the validator.
	 *
	 * @return the validator
	 */
	public Validator getValidator() {
		return validator;
	}

	/**
	 * Set the validator.
	 *
	 * @param validator
	 *        the validator to set
	 */
	public void setValidator(Validator validator) {
		this.validator = validator;
	}

}
