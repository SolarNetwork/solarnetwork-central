/* ==================================================================
 * ReadingDatumFilterValidator.java - 30/04/2021 12:41:16 PM
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

package net.solarnetwork.central.query.biz.dao;

import java.time.Period;
import org.springframework.validation.Errors;
import org.springframework.validation.SmartValidator;
import net.solarnetwork.central.datum.domain.DatumReadingType;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumFilter;
import net.solarnetwork.central.datum.v2.dao.BasicDatumCriteria;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumKind;
import net.solarnetwork.central.datum.v2.support.DatumUtils;
import net.solarnetwork.central.support.DelegatingErrors;

/**
 * Validator for filter used for reading queries.
 * 
 * <p>
 * This validator is used to support consumers of the QueryBiz API.
 * </p>
 * 
 * @author matt
 * @version 2.0
 */
public class ReadingDatumFilterValidator extends ReadingDatumCriteriaValidator
		implements SmartValidator {

	@Override
	public boolean supports(Class<?> clazz) {
		return GeneralNodeDatumFilter.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		validate(target, errors, (Object[]) null);
	}

	@Override
	public void validate(Object target, Errors errors, Object... validationHints) {
		GeneralNodeDatumFilter filter = (GeneralNodeDatumFilter) target;
		DatumReadingType readingType = DatumReadingType.Difference;
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
		super.validate(c, new DelegatingErrors(errors) {

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
