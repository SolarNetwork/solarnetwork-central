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

import static net.solarnetwork.central.security.AuthorizationException.requireNonNullObject;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import net.solarnetwork.central.ValidationException;
import net.solarnetwork.central.datum.v2.dao.BasicDatumCriteria;
import net.solarnetwork.central.datum.v2.dao.ObjectDatumStreamAliasEntityDao;
import net.solarnetwork.central.datum.v2.dao.ObjectDatumStreamAliasFilter;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamAliasEntity;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamAliasMatchType;
import net.solarnetwork.central.domain.Securable;
import net.solarnetwork.central.support.ExceptionUtils;
import net.solarnetwork.central.user.datum.stream.biz.UserDatumStreamAliasBiz;
import net.solarnetwork.central.user.datum.stream.domain.ObjectDatumStreamAliasEntityInput;
import net.solarnetwork.dao.FilterResults;

/**
 * DAO-based implementation of {@link UserDatumStreamAliasBiz}.
 * 
 * @author matt
 * @version 1.0
 */
@Securable
public class DaoUserDatumStreamAliasBiz implements UserDatumStreamAliasBiz {

	private final ObjectDatumStreamAliasEntityDao aliasDao;

	private @Nullable Validator validator;

	/**
	 * Constructor.
	 * 
	 * @param aliasDao
	 *        the alias DAO
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public DaoUserDatumStreamAliasBiz(ObjectDatumStreamAliasEntityDao aliasDao) {
		super();
		this.aliasDao = requireNonNullArgument(aliasDao, "aliasDao");
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
		final ObjectDatumStreamAliasEntity alias = input.toEntity(aliasId, Instant.now());
		final var pk = aliasDao.save(alias);
		return requireNonNullObject(aliasDao.get(pk), pk);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void deleteAliases(Long userId, @Nullable ObjectDatumStreamAliasFilter filter) {
		final var f = new BasicDatumCriteria();
		f.copyFrom(filter);
		f.setUserId(userId);
		aliasDao.delete(f);
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
