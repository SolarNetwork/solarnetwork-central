/* ==================================================================
 * StreamDatumFilterValidator.java - 6/05/2022 5:12:56 pm
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

package net.solarnetwork.central.query.support;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.time.Period;
import org.springframework.validation.Errors;
import org.springframework.validation.SmartValidator;
import org.springframework.validation.Validator;
import net.solarnetwork.central.datum.domain.DatumReadingType;
import net.solarnetwork.central.datum.domain.StreamDatumFilter;
import net.solarnetwork.central.datum.v2.dao.BasicDatumCriteria;
import net.solarnetwork.central.datum.v2.dao.DatumCriteria;
import net.solarnetwork.central.datum.v2.support.DatumUtils;
import net.solarnetwork.central.support.DelegatingErrors;
import net.solarnetwork.domain.datum.ObjectDatumKind;

/**
 * Validator for filter used for datum queries using the
 * {@link StreamDatumFilter} API.
 * 
 * @author matt
 * @version 1.0
 * @since 1.3
 */
public class StreamDatumFilterValidator implements SmartValidator {

	private final Validator delegate;

	/**
	 * Constructor.
	 * 
	 * @param delegate
	 *        the validator for {@link DatumCriteria} objects
	 */
	public StreamDatumFilterValidator(Validator delegate) {
		super();
		this.delegate = requireNonNullArgument(delegate, "delegate");
	}

	@Override
	public boolean supports(Class<?> clazz) {
		return StreamDatumFilter.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		validate(target, errors, (Object[]) null);
	}

	@Override
	public void validate(Object target, Errors errors, Object... validationHints) {
		StreamDatumFilter filter = (StreamDatumFilter) target;
		DatumReadingType readingType = null;
		Period tolerance = null;
		if ( validationHints != null ) {
			for ( Object o : validationHints ) {
				if ( o instanceof DatumReadingType ) {
					readingType = (DatumReadingType) o;
				} else if ( o instanceof Period ) {
					tolerance = (Period) o;
				}
			}
		}
		BasicDatumCriteria c = DatumUtils.criteriaFromFilter(filter);
		c.setObjectKind(ObjectDatumKind.Node);
		c.setReadingType(readingType);
		c.setTimeTolerance(tolerance);
		delegate.validate(c, new DelegatingErrors(errors) {

			@Override
			public void rejectValue(String field, String errorCode, Object[] errorArgs,
					String defaultMessage) {
				if ( "readingType".equals(field) || "timeTolerance".equals(field) ) {
					reject(errorCode, errorArgs, defaultMessage);
				} else {
					super.rejectValue(field, errorCode, errorArgs, defaultMessage);
				}
			}

		});
	}

}
