/* ==================================================================
 * UsageRecord.java - 22/08/2017 11:32:09 AM
 * 
 * Copyright 2017 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.billing.killbill.domain;

import java.math.BigDecimal;
import org.joda.time.LocalDate;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * A record of usage for a day.
 * 
 * @author matt
 * @version 1.0
 */
@JsonPropertyOrder({ "recordDate", "amount" })
public class UsageRecord {

	private LocalDate recordDate;
	private BigDecimal amount;

	/**
	 * Default constructor.
	 */
	public UsageRecord() {
		super();
	}

	/**
	 * Construct with values.
	 * 
	 * @param recordDate
	 *        the record date
	 * @param amount
	 *        the amount
	 */
	public UsageRecord(LocalDate recordDate, BigDecimal amount) {
		super();
		this.recordDate = recordDate;
		this.amount = amount;
	}

	@Override
	public String toString() {
		return "UsageRecord{recordDate=" + recordDate + ",amount=" + amount + "}";
	}

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	public LocalDate getRecordDate() {
		return recordDate;
	}

	public void setRecordDate(LocalDate recordDate) {
		this.recordDate = recordDate;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}

}
