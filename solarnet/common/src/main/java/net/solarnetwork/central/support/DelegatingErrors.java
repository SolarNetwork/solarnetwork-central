/* ==================================================================
 * DelegatingErrors.java - 30/04/2021 1:23:30 PM
 * 
 * Copyright 2021 SolarNetwork.net Dev Team
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

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;

/**
 * Helper class to make overwriting some methods on an existing Errors instance.
 * 
 * <p>
 * Extending classes can pick/choose methods to overwrite, to make overwriting
 * easier.
 * </p>
 * 
 * @author matt
 * @version 1.0
 * @since 2.10
 */
public class DelegatingErrors implements Errors {

	/** The delegate errors object. */
	protected final Errors errors;

	/**
	 * Constructor.
	 * 
	 * @param errors
	 *        the errors to delegate to
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public DelegatingErrors(Errors errors) {
		super();
		this.errors = requireNonNullArgument(errors, "errors");
	}

	@Override
	public String getObjectName() {
		return errors.getObjectName();
	}

	@Override
	public void setNestedPath(String nestedPath) {
		errors.setNestedPath(nestedPath);
	}

	@Override
	public String getNestedPath() {
		return errors.getNestedPath();
	}

	@Override
	public void pushNestedPath(String subPath) {
		errors.pushNestedPath(subPath);
	}

	@Override
	public void popNestedPath() throws IllegalStateException {
		errors.popNestedPath();
	}

	@Override
	public void reject(String errorCode) {
		errors.reject(errorCode);
	}

	@Override
	public void reject(String errorCode, String defaultMessage) {
		errors.reject(errorCode, defaultMessage);
	}

	@Override
	public void reject(String errorCode, Object @Nullable [] errorArgs,
			@Nullable String defaultMessage) {
		errors.reject(errorCode, errorArgs, defaultMessage);
	}

	@Override
	public void rejectValue(@Nullable String field, String errorCode) {
		errors.rejectValue(field, errorCode);
	}

	@Override
	public void rejectValue(@Nullable String field, String errorCode, String defaultMessage) {
		errors.rejectValue(field, errorCode, defaultMessage);
	}

	@Override
	public void rejectValue(@Nullable String field, String errorCode, Object @Nullable [] errorArgs,
			@Nullable String defaultMessage) {
		errors.rejectValue(field, errorCode, errorArgs, defaultMessage);
	}

	@Override
	public void addAllErrors(Errors errors) {
		errors.addAllErrors(errors);
	}

	@Override
	public boolean hasErrors() {
		return errors.hasErrors();
	}

	@Override
	public int getErrorCount() {
		return errors.getErrorCount();
	}

	@Override
	public List<ObjectError> getAllErrors() {
		return errors.getAllErrors();
	}

	@Override
	public boolean hasGlobalErrors() {
		return errors.hasGlobalErrors();
	}

	@Override
	public int getGlobalErrorCount() {
		return errors.getGlobalErrorCount();
	}

	@Override
	public List<ObjectError> getGlobalErrors() {
		return errors.getGlobalErrors();
	}

	@Override
	public @Nullable ObjectError getGlobalError() {
		return errors.getGlobalError();
	}

	@Override
	public boolean hasFieldErrors() {
		return errors.hasFieldErrors();
	}

	@Override
	public int getFieldErrorCount() {
		return errors.getFieldErrorCount();
	}

	@Override
	public List<FieldError> getFieldErrors() {
		return errors.getFieldErrors();
	}

	@Override
	public @Nullable FieldError getFieldError() {
		return errors.getFieldError();
	}

	@Override
	public boolean hasFieldErrors(String field) {
		return errors.hasFieldErrors(field);
	}

	@Override
	public int getFieldErrorCount(String field) {
		return errors.getFieldErrorCount(field);
	}

	@Override
	public List<FieldError> getFieldErrors(String field) {
		return errors.getFieldErrors(field);
	}

	@Override
	public @Nullable FieldError getFieldError(String field) {
		return errors.getFieldError(field);
	}

	@Override
	public @Nullable Object getFieldValue(String field) {
		return errors.getFieldValue(field);
	}

	@Override
	public @Nullable Class<?> getFieldType(String field) {
		return errors.getFieldType(field);
	}

}
