/**
 * This file is part of SIMPL4(http://simpl4.org).
 *
 * 	Copyright [2014] [Manfred Sattler] <manfred@ms123.org>
 *
 * SIMPL4 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * SIMPL4 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with SIMPL4.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.ms123.common.form.constraints;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.Date;
import java.util.Calendar;

/**
 * Check that the number being validated is less than or equal to the maximum
 * value specified.
 *
 * @author Hardy Ferentschik
 */
public class DateMaxValidator implements ConstraintValidator<DateMax, Date> {

	private long maxValue;
	private int tolerance = 0;

	public void initialize(DateMax maxValue) {
		try {
			Date date = new Date(maxValue.value());
			this.maxValue = getMillis(date) + tolerance;
			System.out.println("DateMaxValidator.initialize:" + new Date(this.maxValue));
		} catch (Exception nfe) {
			nfe.printStackTrace();
		}
	}

	public boolean isValid(Date value, ConstraintValidatorContext constraintValidatorContext) {
		System.out.println("DateMaxValidator.isValid.value:" + value);
		if (value == null) {
			return true;
		}
		long cmpValue = getMillis(value);
		System.out.println("DateMaxValidator.isValid:" + (maxValue>cmpValue)+"/"+new Date(cmpValue));
		return maxValue>cmpValue;
	}
	private long getMillis(Date date){
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		cal.add(Calendar.DAY_OF_YEAR, 1);
		cal.add(Calendar.MILLISECOND, -1);
		return cal.getTimeInMillis();
	}
}
