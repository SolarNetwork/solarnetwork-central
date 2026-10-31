/* ==================================================================
 * DaoUserDatumStreamAliasBiz.java - 30/03/2026 10:05:30 am
 * 
 * Copyright 2026 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.datum.stream.biz.impl;

import static net.solarnetwork.central.domain.EntityConstants.UNASSIGNED_UUID_ID;
import static net.solarnetwork.central.security.AuthorizationException.requireNonNullObject;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import net.solarnetwork.central.ValidationException;
import net.solarnetwork.central.datum.v2.dao.BasicDatumCriteria;
import net.solarnetwork.central.datum.v2.dao.BasicObjectDatumStreamAliasGrantFilter;
import net.solarnetwork.central.datum.v2.dao.ObjectDatumStreamAliasEntityDao;
import net.solarnetwork.central.datum.v2.dao.ObjectDatumStreamAliasFilter;
import net.solarnetwork.central.datum.v2.dao.ObjectDatumStreamAliasGrantEntityDao;
import net.solarnetwork.central.datum.v2.dao.ObjectDatumStreamAliasGrantFilter;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamAliasEntity;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamAliasGrantEntity;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamAliasGrantStream;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamAliasMatchType;
import net.solarnetwork.central.domain.EntityConstants;
import net.solarnetwork.central.domain.Securable;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.domain.UserLongStringCompositePK;
import net.solarnetwork.central.support.ExceptionUtils;
import net.solarnetwork.central.user.datum.stream.biz.UserDatumStreamAliasBiz;
import net.solarnetwork.central.user.datum.stream.domain.ObjectDatumStreamAliasEntityInput;
import net.solarnetwork.central.user.datum.stream.domain.ObjectDatumStreamAliasGrantEntityInput;
import net.solarnetwork.dao.FilterResults;

/**
 * DAO-based implementation of {@link UserDatumStreamAliasBiz}.
 * 
 * @author matt
 * @version 1.2
 */
@Securable
public class DaoUserDatumStreamAliasBiz implements UserDatumStreamAliasBiz {

	private final ObjectDatumStreamAliasEntityDao aliasDao;
	private final ObjectDatumStreamAliasGrantEntityDao grantDao;

	private @Nullable Validator validator;

	/**
	 * Constructor.
	 * 
	 * @param aliasDao
	 *        the alias DAO
	 * @param grantDao
	 *        the alias grant DAO
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 * @since 1.2
	 */
	public DaoUserDatumStreamAliasBiz(ObjectDatumStreamAliasEntityDao aliasDao,
			ObjectDatumStreamAliasGrantEntityDao grantDao) {
		super();
		this.aliasDao = requireNonNullArgument(aliasDao, "aliasDao");
		this.grantDao = requireNonNullArgument(grantDao, "grantDao");
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public ObjectDatumStreamAliasEntity aliasForUser(Long userId, UUID id) {
		final var f = new BasicDatumCriteria();
		f.setStreamAliasMatchType(ObjectDatumStreamAliasMatchType.AliasOnly);
		f.setUserId(userId);
		f.setStreamId(id);
		return requireNonNullObject(aliasDao.findFiltered(f).firstResult(), id);
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public FilterResults<ObjectDatumStreamAliasEntity, UUID> listAliases(Long userId,
			@Nullable ObjectDatumStreamAliasFilter filter) {
		final var f = new BasicDatumCriteria();
		f.copyFrom(filter);
		f.setUserId(userId);
		return aliasDao.findFiltered(f);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public ObjectDatumStreamAliasEntity saveAlias(Long userId, UUID aliasId,
			ObjectDatumStreamAliasEntityInput input) {
		validateInput(input);
		if ( EntityConstants.isAssigned(aliasId) ) {
			requireNonNullObject(aliasDao.get(aliasId), aliasId);
		}
		final ObjectDatumStreamAliasEntity alias = input.toEntity(aliasId, Instant.now());
		final var pk = aliasDao.save(alias);
		return requireNonNullObject(aliasDao.get(pk), pk);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public List<ObjectDatumStreamAliasEntity> saveAliases(Long userId,
			List<ObjectDatumStreamAliasEntityInput> inputs) {
		if ( requireNonNullArgument(inputs, "inputs").size() > MAX_BULK_ALIASES ) {
			throw new IllegalArgumentException(
					"No more than %d aliases can be saved at once.".formatted(MAX_BULK_ALIASES));
		}
		for ( ObjectDatumStreamAliasEntityInput input : inputs ) {
			validateInput(requireNonNullArgument(input, "input"));
		}
		final Instant now = Instant.now();
		final var result = new ArrayList<ObjectDatumStreamAliasEntity>(inputs.size());
		for ( ObjectDatumStreamAliasEntityInput input : inputs ) {
			final var pk = aliasDao.save(input.toEntity(UNASSIGNED_UUID_ID, now));
			result.add(requireNonNullObject(aliasDao.get(pk), pk));
		}
		return result;
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void deleteAliases(Long userId, @Nullable ObjectDatumStreamAliasFilter filter) {
		final var f = new BasicDatumCriteria();
		f.copyFrom(filter);
		f.setUserId(userId);
		aliasDao.delete(f);
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public ObjectDatumStreamAliasGrantEntity aliasGrantForUser(Long userId, Long grantId) {
		final var pk = new UserLongCompositePK(userId, grantId);
		return requireNonNullObject(grantDao.get(pk), pk);
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public FilterResults<ObjectDatumStreamAliasGrantEntity, UserLongCompositePK> listAliasGrants(
			Long userId, @Nullable ObjectDatumStreamAliasGrantFilter filter) {
		final var f = new BasicObjectDatumStreamAliasGrantFilter(filter);
		f.setUserId(userId);
		return grantDao.findFiltered(f);
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public FilterResults<ObjectDatumStreamAliasGrantEntity, UserLongCompositePK> listReceivedAliasGrants(
			Long userId, @Nullable ObjectDatumStreamAliasGrantFilter filter) {
		final var f = new BasicObjectDatumStreamAliasGrantFilter(filter);
		f.setGranteeId(userId);
		f.setEnabled(true);
		return grantDao.findFiltered(f);
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public FilterResults<ObjectDatumStreamAliasGrantStream, UserLongStringCompositePK> listAvailableAliasStreams(
			Long userId, Long aliasNodeId, @Nullable ObjectDatumStreamAliasGrantFilter filter) {
		return grantDao.findAvailableStreams(userId, aliasNodeId, filter);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public ObjectDatumStreamAliasGrantEntity saveAliasGrant(Long userId, Long grantId,
			ObjectDatumStreamAliasGrantEntityInput input) {
		validateInput(input);
		final var id = new UserLongCompositePK(userId, grantId);
		if ( id.entityIdIsAssigned() ) {
			requireNonNullObject(grantDao.get(id), id);
		}
		final var pk = grantDao.save(input.toEntity(id, Instant.now()));
		return requireNonNullObject(grantDao.get(pk), pk);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public ObjectDatumStreamAliasGrantEntity updateAliasGrantEnabled(Long userId, Long grantId,
			boolean enabled) {
		final var pk = new UserLongCompositePK(userId, grantId);
		final var filter = new BasicObjectDatumStreamAliasGrantFilter();
		filter.setGrantId(grantId);
		grantDao.updateEnabledStatus(userId, filter, enabled);
		return requireNonNullObject(grantDao.get(pk), pk);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void deleteAliasGrant(Long userId, Long grantId) {
		grantDao.delete(grantDao.entityKey(new UserLongCompositePK(userId, grantId)));
	}

	private void validateInput(final @Nullable Object input) {
		validateInput(input, getValidator());
	}

	private static void validateInput(final @Nullable Object input, final @Nullable Validator v) {
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

	/**
	 * Get the validator.
	 *
	 * @return the validator
	 */
	public @Nullable Validator getValidator() {
		return validator;
	}

	/**
	 * Set the validator.
	 *
	 * @param validator
	 *        the validator to set
	 */
	public void setValidator(@Nullable Validator validator) {
		this.validator = validator;
	}

}
