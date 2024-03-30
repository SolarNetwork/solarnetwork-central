/* ==================================================================
 * DaoUserInstructionInputBiz.java - 29/03/2024 9:50:21 am
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

package net.solarnetwork.central.user.inin.biz.impl;

import static net.solarnetwork.central.security.AuthorizationException.requireNonNullObject;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MimeType;
import org.springframework.validation.BindingResult;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import net.solarnetwork.central.ValidationException;
import net.solarnetwork.central.dao.UserModifiableEnabledStatusDao;
import net.solarnetwork.central.domain.CompositeKey;
import net.solarnetwork.central.domain.UserIdRelated;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.domain.UserUuidLongCompositePK;
import net.solarnetwork.central.domain.UserUuidPK;
import net.solarnetwork.central.inin.biz.InstructionInputEndpointBiz;
import net.solarnetwork.central.inin.biz.RequestTransformService;
import net.solarnetwork.central.inin.biz.ResponseTransformService;
import net.solarnetwork.central.inin.biz.TransformConstants;
import net.solarnetwork.central.inin.dao.BasicFilter;
import net.solarnetwork.central.inin.dao.CredentialConfigurationDao;
import net.solarnetwork.central.inin.dao.EndpointAuthConfigurationDao;
import net.solarnetwork.central.inin.dao.EndpointConfigurationDao;
import net.solarnetwork.central.inin.dao.InstructionInputFilter;
import net.solarnetwork.central.inin.dao.TransformConfigurationDao;
import net.solarnetwork.central.inin.domain.CredentialConfiguration;
import net.solarnetwork.central.inin.domain.EndpointAuthConfiguration;
import net.solarnetwork.central.inin.domain.EndpointConfiguration;
import net.solarnetwork.central.inin.domain.InstructionInputConfigurationEntity;
import net.solarnetwork.central.inin.domain.TransformConfiguration.RequestTransformConfiguration;
import net.solarnetwork.central.inin.domain.TransformConfiguration.ResponseTransformConfiguration;
import net.solarnetwork.central.instructor.domain.NodeInstruction;
import net.solarnetwork.central.support.ExceptionUtils;
import net.solarnetwork.central.user.inin.biz.UserInstructionInputBiz;
import net.solarnetwork.central.user.inin.domain.InstructionInputConfigurationInput;
import net.solarnetwork.central.user.inin.domain.TransformInstructionResults;
import net.solarnetwork.central.user.inin.domain.TransformOutput;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.dao.FilterableDao;
import net.solarnetwork.dao.GenericDao;
import net.solarnetwork.domain.LocalizedServiceInfo;
import net.solarnetwork.service.LocalizedServiceInfoProvider;
import net.solarnetwork.service.PasswordEncoder;
import net.solarnetwork.util.StringUtils;

/**
 * DAO based implementation of {@Link UserInstructionInputBiz}.
 *
 * @author matt
 * @version 1.0
 */
public class DaoUserInstructionInputBiz implements UserInstructionInputBiz {

	private final CredentialConfigurationDao credentialDao;
	private final TransformConfigurationDao<RequestTransformConfiguration> requestTransformDao;
	private final TransformConfigurationDao<ResponseTransformConfiguration> responseTransformDao;
	private final EndpointConfigurationDao endpointDao;
	private final EndpointAuthConfigurationDao endpointAuthDao;
	private final Collection<RequestTransformService> requestTransformServices;
	private final Collection<ResponseTransformService> responseTransformServices;

	private Validator validator;
	private PasswordEncoder passwordEncoder;

	/**
	 * Constructor.
	 *
	 * @param credentialDao
	 *        the credential DAO
	 * @param requestTransformDao
	 *        the transform DAO
	 * @param responseTransformDao
	 *        the transform DAO
	 * @param endpointDao
	 *        the endpoint DAO
	 * @param endpointAuthDao
	 *        the endpoint authorization DAO
	 * @param requestTransformServices
	 *        the request transform services
	 * @param responseTransformServices
	 *        the response transform services
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public DaoUserInstructionInputBiz(CredentialConfigurationDao credentialDao,
			TransformConfigurationDao<RequestTransformConfiguration> requestTransformDao,
			TransformConfigurationDao<ResponseTransformConfiguration> responseTransformDao,
			EndpointConfigurationDao endpointDao, EndpointAuthConfigurationDao endpointAuthDao,
			Collection<RequestTransformService> requestTransformServices,
			Collection<ResponseTransformService> responseTransformServices) {
		super();
		this.credentialDao = requireNonNullArgument(credentialDao, "credentialDao");
		this.requestTransformDao = requireNonNullArgument(requestTransformDao, "requestTransformDao");
		this.responseTransformDao = requireNonNullArgument(responseTransformDao, "responseTransformDao");
		this.endpointDao = requireNonNullArgument(endpointDao, "endpointDao");
		this.endpointAuthDao = requireNonNullArgument(endpointAuthDao, "endpointAuthDao");
		this.requestTransformServices = requireNonNullArgument(requestTransformServices,
				"requestTransformServices");
		this.responseTransformServices = requireNonNullArgument(responseTransformServices,
				"responseTransformServices");
	}

	@Override
	public Iterable<LocalizedServiceInfo> availableRequestTransformServices(Locale locale) {
		return LocalizedServiceInfoProvider.localizedServiceSettings(requestTransformServices, locale);
	}

	@Override
	public Iterable<LocalizedServiceInfo> availableResponseTransformServices(Locale locale) {
		return LocalizedServiceInfoProvider.localizedServiceSettings(responseTransformServices, locale);
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public <C extends InstructionInputConfigurationEntity<C, K>, K extends CompositeKey & Comparable<K> & Serializable & UserIdRelated> FilterResults<C, K> configurationsForUser(
			Long userId, InstructionInputFilter filter, Class<C> configurationClass) {
		requireNonNullArgument(userId, "userId");
		requireNonNullArgument(configurationClass, "configurationClass");
		BasicFilter f = new BasicFilter(filter);
		f.setUserId(userId);
		FilterableDao<C, K, InstructionInputFilter> dao = filterableDao(configurationClass);
		FilterResults<C, K> result = dao.findFiltered(f, f.getSorts(), f.getOffset(), f.getMax());
		// remove credentials before returning
		for ( C c : result ) {
			c.eraseCredentials();
		}
		return result;
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public <C extends InstructionInputConfigurationEntity<C, K>, K extends CompositeKey & Comparable<K> & Serializable & UserIdRelated> C configurationForId(
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
	public <C extends InstructionInputConfigurationEntity<C, K>, K extends CompositeKey & Comparable<K> & Serializable & UserIdRelated> void enableConfiguration(
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

		UserModifiableEnabledStatusDao<InstructionInputFilter> dao = statusDao(configurationClass);
		dao.updateEnabledStatus(id.getUserId(), filter, enabled);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public <T extends InstructionInputConfigurationInput<C, K>, C extends InstructionInputConfigurationEntity<C, K>, K extends CompositeKey & Comparable<K> & Serializable & UserIdRelated> C saveConfiguration(
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
	public <C extends InstructionInputConfigurationEntity<C, K>, K extends CompositeKey & Comparable<K> & Serializable & UserIdRelated> void deleteConfiguration(
			K id, Class<C> configurationClass) {
		requireNonNullArgument(id, "id");
		requireNonNullArgument(id.getUserId(), "id.userId");
		requireNonNullArgument(configurationClass, "configurationClass");
		GenericDao<C, K> dao = genericDao(configurationClass);
		C pk = dao.entityKey(id);
		dao.delete(pk);
	}

	private static Set<Long> nodeIds(EndpointConfiguration endpoint, Map<String, ?> parameters) {
		Set<Long> nodeIds = endpoint.getNodeIds();
		if ( parameters != null && parameters.containsKey(InstructionInputEndpointBiz.PARAM_NODE_IDS) ) {
			Object val = parameters.get(InstructionInputEndpointBiz.PARAM_NODE_IDS);
			if ( val instanceof String s ) {
				Set<String> vals = StringUtils.commaDelimitedStringToSet(s);
				if ( vals != null && !vals.isEmpty() ) {
					try {
						Set<Long> ids = new LinkedHashSet<>(vals.size());
						for ( String v : vals ) {
							ids.add(Long.valueOf(v));
						}
						nodeIds = ids;
					} catch ( IllegalArgumentException e ) {
						// ignore
					}
				}
			}
		}
		return nodeIds;
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public TransformOutput previewTransform(UserUuidPK id, MimeType contentType, InputStream in,
			MimeType outputType, Collection<TransformInstructionResults> instructionResults,
			Map<String, ?> parameters) throws IOException {
		final EndpointConfiguration endpoint = requireNonNullObject(
				endpointDao.get(requireNonNullArgument(id, "id")), id);

		final UserLongCompositePK reqXformPk = new UserLongCompositePK(id.getUserId(),
				requireNonNullArgument(endpoint.getRequestTransformId(), "endpoint.requestTransformId"));
		final RequestTransformConfiguration reqXform = requireNonNullObject(
				requestTransformDao.get(reqXformPk), reqXformPk);

		final UserLongCompositePK resXformPk = new UserLongCompositePK(id.getUserId(),
				requireNonNullArgument(endpoint.getResponseTransformId(),
						"endpoint.responseTransformId"));
		final ResponseTransformConfiguration resXform = requireNonNullObject(
				responseTransformDao.get(resXformPk), resXformPk);

		final String reqXformServiceId = requireNonNullArgument(reqXform.getServiceIdentifier(),
				"requestTransform.serviceIdentifier");
		final RequestTransformService reqXformService = requireNonNullObject(
				requestTransformService(reqXformServiceId), reqXformServiceId);

		if ( !reqXformService.supportsInput(requireNonNullArgument(in, "in"),
				requireNonNullArgument(contentType, "contentType")) ) {
			throw new IllegalArgumentException(
					"Request transform service %s does not support input type %s with %s."
							.formatted(reqXformServiceId, contentType, in.getClass().getSimpleName()));
		}

		final String resXformServiceId = requireNonNullArgument(resXform.getServiceIdentifier(),
				"responseTransform.serviceIdentifier");
		final ResponseTransformService resXformService = requireNonNullObject(
				responseTransformService(resXformServiceId), resXformServiceId);

		if ( !resXformService.supportsOutputType(requireNonNullArgument(outputType, "outputType")) ) {
			throw new IllegalArgumentException(
					"Response transform service %s does not support output type %s."
							.formatted(resXformServiceId, contentType));
		}

		Set<Long> nodeIds = nodeIds(endpoint, parameters);

		var xsltOutput = new StringBuilder();
		String output = null;
		Iterable<NodeInstruction> instructions = null;
		String msg = null;
		try {
			var params = new HashMap<String, Object>(8);
			if ( parameters != null ) {
				params.putAll(parameters);
			}
			params.put(TransformConstants.PARAM_USER_ID, id.getUserId());
			params.put(TransformConstants.PARAM_ENDPOINT_ID, endpoint.getEndpointId());
			params.put(TransformConstants.PARAM_TRANSFORM_ID, endpoint.getRequestTransformId());
			params.put(TransformConstants.PARAM_CONFIGURATION_CACHE_KEY, reqXformPk.ident());
			params.put(TransformConstants.PARAM_DEBUG_OUTPUT, xsltOutput);
			params.put(TransformConstants.PARAM_PREVIEW, true);
			instructions = reqXformService.transformInput(in, contentType, reqXform, params);
			if ( instructions != null ) {
				if ( nodeIds != null ) {
					List<NodeInstruction> nodeInstructions = new ArrayList<>(8);
					for ( NodeInstruction instr : instructions ) {
						for ( Long nodeId : nodeIds ) {
							nodeInstructions.add(instr.copyWithNodeId(nodeId));
						}
					}
					instructions = nodeInstructions;
				}

				// handle response transform
				if ( instructionResults != null && !instructionResults.isEmpty() ) {
					Map<Long, TransformInstructionResults> nodeResults = instructionResults.stream()
							.collect(Collectors.toMap(TransformInstructionResults::nodeId,
									Function.identity()));
					List<NodeInstruction> nodeInstructions = new ArrayList<>(8);
					for ( NodeInstruction instr : instructions ) {
						TransformInstructionResults xformResult = nodeResults.get(instr.getNodeId());
						if ( xformResult != null ) {
							NodeInstruction copy = instr.clone();
							if ( xformResult.state() != null ) {
								copy.setState(xformResult.state());
							}
							if ( xformResult.resultParameters() != null ) {
								copy.setResultParameters(xformResult.resultParameters());
							}
							nodeInstructions.add(copy);
						} else {
							nodeInstructions.add(instr);
						}
					}
					instructions = nodeInstructions;
				}

				params.put(TransformConstants.PARAM_TRANSFORM_ID, endpoint.getResponseTransformId());
				params.put(TransformConstants.PARAM_CONFIGURATION_CACHE_KEY, resXformPk.ident());
				ByteArrayOutputStream byos = new ByteArrayOutputStream();
				resXformService.transformOutput(instructions, outputType, resXform, parameters, byos);

				if ( TransformConstants.JSON_TYPE.isCompatibleWith(outputType)
						|| "text".equals(outputType.getType()) ) {
					output = byos.toString(StandardCharsets.UTF_8); // assumed UTF-8
				} else {
					output = Base64.getEncoder().encodeToString(byos.toByteArray());
				}
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

		return new TransformOutput(instructions, output, xsltOutput.toString(), msg);
	}

	private RequestTransformService requestTransformService(String serviceId) {
		for ( RequestTransformService service : requestTransformServices ) {
			if ( serviceId.equals(service.getId()) ) {
				return service;
			}
		}
		return null;
	}

	private ResponseTransformService responseTransformService(String serviceId) {
		for ( ResponseTransformService service : responseTransformServices ) {
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
	private <C extends InstructionInputConfigurationEntity<C, K>, K extends CompositeKey & Comparable<K> & Serializable & UserIdRelated> GenericDao<C, K> genericDao(
			Class<C> clazz) {
		GenericDao<C, K> result = null;
		if ( CredentialConfiguration.class.isAssignableFrom(clazz) ) {
			result = (GenericDao<C, K>) (credentialDao);
		} else if ( RequestTransformConfiguration.class.isAssignableFrom(clazz) ) {
			result = (GenericDao<C, K>) (requestTransformDao);
		} else if ( ResponseTransformConfiguration.class.isAssignableFrom(clazz) ) {
			result = (GenericDao<C, K>) (responseTransformDao);
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
	private <F extends InstructionInputFilter> UserModifiableEnabledStatusDao<F> statusDao(
			Class<?> clazz) {
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
	private <C extends InstructionInputConfigurationEntity<C, K>, K extends CompositeKey & Comparable<K> & Serializable & UserIdRelated, F extends InstructionInputFilter> FilterableDao<C, K, F> filterableDao(
			Class<C> clazz) {
		FilterableDao<C, K, F> result = null;
		if ( CredentialConfiguration.class.isAssignableFrom(clazz) ) {
			result = (FilterableDao<C, K, F>) (credentialDao);
		} else if ( RequestTransformConfiguration.class.isAssignableFrom(clazz) ) {
			result = (FilterableDao<C, K, F>) (requestTransformDao);
		} else if ( ResponseTransformConfiguration.class.isAssignableFrom(clazz) ) {
			result = (FilterableDao<C, K, F>) (responseTransformDao);
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
