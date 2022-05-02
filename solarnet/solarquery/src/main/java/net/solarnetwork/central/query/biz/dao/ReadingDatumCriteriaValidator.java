/* ==================================================================
 * ReadingDatumCriteriaValidator.java - 30/04/2021 9:20:23 AM
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

import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import net.solarnetwork.central.datum.domain.DatumReadingType;
import net.solarnetwork.central.datum.v2.dao.DatumCriteria;
import net.solarnetwork.central.domain.Aggregation;

/**
 * Validator for criteria used for reading queries.
 * 
 * @author matt
 * @version 1.0
 * @since 3.1
 */
public class ReadingDatumCriteriaValidator implements Validator {

	@Override
	public boolean supports(Class<?> clazz) {
		return DatumCriteria.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		validateCriteria((DatumCriteria) target, errors);
	}

	private void validateCriteria(DatumCriteria c, Errors errors) {
		Aggregation agg = c.getAggregation();
		DatumReadingType readingType = c.getReadingType();
		if ( agg != null && agg != Aggregation.None ) {
			validateAggregateCriteria(c, errors, readingType, agg);
		} else {
			validateCriteria(c, errors, readingType);
		}
	}

	private void validateCriteria(DatumCriteria c, Errors errors, DatumReadingType readingType) {
		if ( c.isMostRecent() ) {
			errors.rejectValue("mostRecent", "error.filter.reading.mostRecent.invalid",
					"The mostRecent mode is not supported for reading queries.");
		}
		if ( !c.hasDateOrLocalDateRange() ) {
			errors.reject("error.filter.dateRange.required", "A date range is required.");
		}
	}

	private void validateAggregateCriteria(DatumCriteria c, Errors errors,
			DatumReadingType readingType, Aggregation agg) {
		if ( !(agg == Aggregation.Hour || agg == Aggregation.Day || agg == Aggregation.Month) ) {
			errors.rejectValue("aggregation", "error.filter.reading.aggregation.invalid",
					new Object[] { agg },
					"Only Hour/Day/Month aggregation is supported for reading queries.");
		}
		if ( readingType != DatumReadingType.Difference ) {
			errors.rejectValue("readingType", "error.filter.reading.aggregation.readingType",
					new Object[] { readingType },
					"Only the Difference reading type is supported for aggregate queries.");
		}
		if ( !c.isMostRecent() && !c.hasDateOrLocalDateRange() ) {
			errors.reject("error.filter.dateRange.required", "A date range is required.");
		}
	}

}
