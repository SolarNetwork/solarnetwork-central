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

import static net.solarnetwork.central.security.AuthorizationException.requireNonNullObject;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.StreamSupport;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MimeType;
import org.springframework.validation.BindingResult;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import net.solarnetwork.central.ValidationException;
import net.solarnetwork.central.dao.UserModifiableEnabledStatusDao;
import net.solarnetwork.central.din.biz.TransformService;
import net.solarnetwork.central.din.dao.BasicFilter;
import net.solarnetwork.central.din.dao.CredentialConfigurationDao;
import net.solarnetwork.central.din.dao.DatumInputFilter;
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
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.domain.UserUuidLongCompositePK;
import net.solarnetwork.central.domain.UserUuidPK;
import net.solarnetwork.central.support.ExceptionUtils;
import net.solarnetwork.central.user.din.biz.UserDatumInputBiz;
import net.solarnetwork.central.user.din.domain.DatumInputConfigurationInput;
import net.solarnetwork.central.user.din.domain.TransformOutput;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.dao.FilterableDao;
import net.solarnetwork.dao.GenericDao;
import net.solarnetwork.domain.LocalizedServiceInfo;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.domain.datum.DatumId;
import net.solarnetwork.service.LocalizedServiceInfoProvider;
import net.solarnetwork.service.PasswordEncoder;

/**
 * DAO based implementation of {@Link UserDatumInputBiz}.
 *
 * @author matt
 * @version 1.0
 */
public class DaoUserDatumInputBiz implements UserDatumInputBiz {

	private final CredentialConfigurationDao credentialDao;
	private final TransformConfigurationDao transformDao;
	private final EndpointConfigurationDao endpointDao;
	private final EndpointAuthConfigurationDao endpointAuthDao;
	private final Collection<TransformService> transformServices;

	private Validator validator;
	private PasswordEncoder passwordEncoder;

	/**
	 * Constructor.
	 *
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
	public DaoUserDatumInputBiz(CredentialConfigurationDao credentialDao,
			TransformConfigurationDao transformDao, EndpointConfigurationDao endpointDao,
			EndpointAuthConfigurationDao endpointAuthDao,
			Collection<TransformService> transformServices) {
		super();
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
	public <C extends DatumInputConfigurationEntity<C, K>, K extends CompositeKey & Comparable<K> & Serializable & UserIdRelated> FilterResults<C, K> configurationsForUser(
			Long userId, DatumInputFilter filter, Class<C> configurationClass) {
		requireNonNullArgument(userId, "userId");
		requireNonNullArgument(configurationClass, "configurationClass");
		BasicFilter f = new BasicFilter(filter);
		f.setUserId(userId);
		FilterableDao<C, K, DatumInputFilter> dao = filterableDao(configurationClass);
		FilterResults<C, K> result = dao.findFiltered(f, f.getSorts(), f.getOffset(), f.getMax());
		// remove credentials before returning
		for ( C c : result ) {
			c.eraseCredentials();
		}
		return result;
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public <C extends DatumInputConfigurationEntity<C, K>, K extends CompositeKey & Comparable<K> & Serializable & UserIdRelated> C configurationForId(
			K id, Class<C> configurationClass) {
		requireNonNullArgument(id, "id");
		requireNonNullArgument(id.getUserId(), "id.userId");
		requireNonNullArgument(configurationClass, "configurationClass");
		GenericDao<C, K> dao = genericDao(configurationClass);
		C result = requireNonNullObject(dao.get(id), id);
		// remove credentials before returning
		result.eraseCredentials();
		return result;
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public <C extends DatumInputConfigurationEntity<C, K>, K extends CompositeKey & Comparable<K> & Serializable & UserIdRelated> void enableConfiguration(
			K id, boolean enabled, Class<C> configurationClass) {
		requireNonNullArgument(id, "id");
		requireNonNullArgument(id.getUserId(), "id.userId");
		requireNonNullArgument(configurationClass, "configurationClass");

		BasicFilter filter = new BasicFilter();
		filter.setUserId(id.getUserId());
		if ( id instanceof UserLongCompositePK pk && pk.entityIdIsAssigned() ) {
			if ( CredentialConfiguration.class.isAssignableFrom(configurationClass) ) {
				filter.setCredentialId(pk.getEntityId());
			}
		} else if ( id instanceof UserUuidPK pk && pk.uuidIsAssigned()
				&& EndpointConfiguration.class.isAssignableFrom(configurationClass) ) {
			filter.setEndpointId(pk.getUuid());
		} else if ( id instanceof UserUuidLongCompositePK pk
				&& EndpointAuthConfiguration.class.isAssignableFrom(configurationClass) ) {
			filter.setEndpointId(pk.getGroupId());
			filter.setCredentialId(pk.getEntityId());
		}

		UserModifiableEnabledStatusDao<DatumInputFilter> dao = statusDao(configurationClass);
		dao.updateEnabledStatus(id.getUserId(), filter, enabled);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public <T extends DatumInputConfigurationInput<C, K>, C extends DatumInputConfigurationEntity<C, K>, K extends CompositeKey & Comparable<K> & Serializable & UserIdRelated> C saveConfiguration(
			K id, T input) {
		requireNonNullArgument(id, "id");
		requireNonNullArgument(id.getUserId(), "id.userId");
		requireNonNullArgument(input, "input");

		validateInput(input);

		C config = input.toEntity(id);

		if ( passwordEncoder != null && config instanceof CredentialConfiguration c ) {
			if ( !passwordEncoder.isPasswordEncrypted(c.getPassword()) ) {
				c.setPassword(passwordEncoder.encode(c.getPassword()));
			}
		}

		@SuppressWarnings("unchecked")
		GenericDao<C, K> dao = genericDao((Class<C>) config.getClass());
		K updatedId = requireNonNullObject(dao.save(config), id);
		C result = requireNonNullObject(dao.get(updatedId), updatedId);
		result.eraseCredentials();
		return result;

	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public <C extends DatumInputConfigurationEntity<C, K>, K extends CompositeKey & Comparable<K> & Serializable & UserIdRelated> void deleteConfiguration(
			K id, Class<C> configurationClass) {
		requireNonNullArgument(id, "id");
		requireNonNullArgument(id.getUserId(), "id.userId");
		requireNonNullArgument(configurationClass, "configurationClass");
		GenericDao<C, K> dao = genericDao(configurationClass);
		C pk = dao.entityKey(id);
		dao.delete(pk);
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public TransformOutput previewTransform(UserLongCompositePK id, UUID endpointId,
			MimeType contentType, InputStream in, Map<String, ?> parameters) throws IOException {
		final UserLongCompositePK xformPk = new UserLongCompositePK(id.getUserId(),
				requireNonNullArgument(id.getEntityId(), "transformId"));
		final TransformConfiguration xform = requireNonNullObject(transformDao.get(xformPk), xformPk);

		final EndpointConfiguration endpoint = (endpointId != null
				? requireNonNullObject(endpointDao.get(new UserUuidPK(id.getUserId(), endpointId)),
						endpointId)
				: null);

		final String xformServiceId = requireNonNullArgument(xform.getServiceIdentifier(),
				"transform.serviceIdentifier");
		final TransformService xformService = requireNonNullObject(transformService(xformServiceId),
				xformServiceId);

		if ( !xformService.supportsInput(requireNonNullArgument(in, "in"),
				requireNonNullArgument(contentType, "contentType")) ) {
			throw new IllegalArgumentException(
					"Transform service %s does not support input type %s with %s."
							.formatted(xformServiceId, contentType, in.getClass().getSimpleName()));
		}

		var xsltOutput = new StringBuilder();
		Iterable<Datum> datum = null;
		String msg = null;
		try {
			var params = new HashMap<String, Object>(8);
			if ( parameters != null ) {
				params.putAll(parameters);
			}
			params.put(TransformService.PARAM_USER_ID, id.getUserId());
			params.put(TransformService.PARAM_ENDPOINT_ID,
					(endpointId != null ? endpointId : UserUuidPK.UNASSIGNED_UUID_ID).toString());
			params.put(TransformService.PARAM_TRANSFORM_ID, id.getEntityId());
			params.put(TransformService.PARAM_CONFIGURATION_CACHE_KEY, xformPk.ident());
			params.put(TransformService.PARAM_XSLT_OUTPUT, xsltOutput);
			params.put(TransformService.PARAM_PREVIEW, true);
			datum = xformService.transform(in, contentType, xform, params);
			if ( datum != null && endpoint != null
					&& (endpoint.getNodeId() != null || endpoint.getSourceId() != null) ) {
				datum = StreamSupport.stream(datum.spliterator(), false)
						.map(d -> d.copyWithId(DatumId.nodeId(
								endpoint.getNodeId() != null ? endpoint.getNodeId() : d.getObjectId(),
								endpoint.getSourceId() != null ? endpoint.getSourceId()
										: d.getSourceId(),
								d.getTimestamp())))
						.toList();
			}
		} catch ( Exception e ) {
			Throwable root = e;
			while ( root.getCause() != null ) {
				root = root.getCause();
			}
			msg = e.getMessage();
			if ( !msg.equals(root.getMessage()) ) {
				msg += " " + root.getMessage();
			}
		}

		return new TransformOutput(datum, xsltOutput.toString(), msg);
	}

	private TransformService transformService(String serviceId) {
		for ( TransformService service : transformServices ) {
			if ( serviceId.equals(service.getId()) ) {
				return service;
			}
		}
		return null;
	}

	private void validateInput(final Object input) {
		validateInput(input, getValidator());
	}

	private static void validateInput(final Object input, final Validator v) {
		if ( input == null || v == null ) {
			return;
		}
		var violations = v.validate(input);
		if ( violations == null || violations.isEmpty() ) {
			return;
		}
		BindingResult errors = ExceptionUtils
				.toBindingResult(new ConstraintViolationException(violations), v);
		if ( errors.hasErrors() ) {
			throw new ValidationException(errors);
		}
	}

	@SuppressWarnings("unchecked")
	private <C extends DatumInputConfigurationEntity<C, K>, K extends CompositeKey & Comparable<K> & Serializable & UserIdRelated> GenericDao<C, K> genericDao(
			Class<C> clazz) {
		GenericDao<C, K> result = null;
		if ( CredentialConfiguration.class.isAssignableFrom(clazz) ) {
			result = (GenericDao<C, K>) (credentialDao);
		} else if ( TransformConfiguration.class.isAssignableFrom(clazz) ) {
			result = (GenericDao<C, K>) (transformDao);
		} else if ( EndpointConfiguration.class.isAssignableFrom(clazz) ) {
			result = (GenericDao<C, K>) (endpointDao);
		} else if ( EndpointAuthConfiguration.class.isAssignableFrom(clazz) ) {
			result = (GenericDao<C, K>) (endpointAuthDao);
		}
		if ( result != null ) {
			return result;
		}
		throw new UnsupportedOperationException("Configuration type %s not supported.".formatted(clazz));
	}

	@SuppressWarnings("unchecked")
	private <F extends DatumInputFilter> UserModifiableEnabledStatusDao<F> statusDao(Class<?> clazz) {
		UserModifiableEnabledStatusDao<F> result = null;
		if ( CredentialConfiguration.class.isAssignableFrom(clazz) ) {
			result = (UserModifiableEnabledStatusDao<F>) (credentialDao);
		} else if ( EndpointConfiguration.class.isAssignableFrom(clazz) ) {
			result = (UserModifiableEnabledStatusDao<F>) (endpointDao);
		} else if ( EndpointAuthConfiguration.class.isAssignableFrom(clazz) ) {
			result = (UserModifiableEnabledStatusDao<F>) (endpointAuthDao);
		}
		if ( result != null ) {
			return result;
		}
		throw new UnsupportedOperationException("Configuration type %s not supported.".formatted(clazz));
	}

	@SuppressWarnings("unchecked")
	private <C extends DatumInputConfigurationEntity<C, K>, K extends CompositeKey & Comparable<K> & Serializable & UserIdRelated, F extends DatumInputFilter> FilterableDao<C, K, F> filterableDao(
			Class<C> clazz) {
		FilterableDao<C, K, F> result = null;
		if ( CredentialConfiguration.class.isAssignableFrom(clazz) ) {
			result = (FilterableDao<C, K, F>) (credentialDao);
		} else if ( TransformConfiguration.class.isAssignableFrom(clazz) ) {
			result = (FilterableDao<C, K, F>) (transformDao);
		} else if ( EndpointConfiguration.class.isAssignableFrom(clazz) ) {
			result = (FilterableDao<C, K, F>) (endpointDao);
		} else if ( EndpointAuthConfiguration.class.isAssignableFrom(clazz) ) {
			result = (FilterableDao<C, K, F>) (endpointAuthDao);
		}
		if ( result != null ) {
			return result;
		}
		throw new UnsupportedOperationException("Configuration type %s not supported.".formatted(clazz));
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

	/**
	 * Get the password encoder.
	 *
	 * @return the password encoder
	 */
	public PasswordEncoder getPasswordEncoder() {
		return passwordEncoder;
	}

	/**
	 * Set the password encoder.
	 *
	 * @param passwordEncoder
	 *        the encoder to set
	 */
	public void setPasswordEncoder(PasswordEncoder passwordEncoder) {
		this.passwordEncoder = passwordEncoder;
	}

}
