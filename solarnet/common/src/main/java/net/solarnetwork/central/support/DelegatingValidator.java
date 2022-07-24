/* ==================================================================
 * DelegatingValidator.java - 24/07/2022 7:48:01 am
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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
import java.util.Map;
import org.springframework.validation.Errors;
import org.springframework.validation.SmartValidator;
import org.springframework.validation.Validator;

/**
 * Validator that delegates to other validators based on the class of the object
 * being validated.
 * 
 * @author matt
 * @version 1.0
 */
public class DelegatingValidator implements SmartValidator {

	private final Map<String, Validator> validators;

	/**
	 * Constructor.
	 * 
	 * @param validators
	 *        the validators
	 */
	public DelegatingValidator(Map<String, Validator> validators) {
		super();
		this.validators = requireNonNullArgument(validators, "validators");
	}

	@Override
	public boolean supports(Class<?> clazz) {
		return validators.containsKey(clazz.getName());
	}

	@Override
	public void validate(Object target, Errors errors) {
		validate(target, errors, (Object[]) null);
	}

	@Override
	public void validate(Object target, Errors errors, Object... validationHints) {
		final String key = (target != null ? target.getClass().getName() : null);
		if ( key == null ) {
			errors.reject("validation.null", "Object must not be null.");
			return;
		}
		Validator delegate = validators.get(key);
		if ( delegate == null ) {
			return;
		}
		if ( delegate instanceof SmartValidator ) {
			((SmartValidator) delegate).validate(target, errors, validationHints);
		} else {
			delegate.validate(target, errors);
		}
	}

}
