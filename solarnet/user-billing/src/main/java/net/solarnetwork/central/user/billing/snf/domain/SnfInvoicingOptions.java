/* ==================================================================
 * SnfInvoicingOptions.java - 30/07/2020 9:22:35 AM
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.billing.snf.domain;

import java.util.Objects;
import net.solarnetwork.central.user.billing.snf.SnfInvoicingSystem;

/**
 * Options to use during invoice generation.
 * 
 * @author matt
 * @version 1.0
 */
public class SnfInvoicingOptions implements SnfInvoicingSystem.InvoiceGenerationOptions {

	/**
	 * Get an instance with the default options configured.
	 * 
	 * <p>
	 * The {@code dryRun} flag will be {@literal false} and
	 * {@code useAccountCredit} will be {@literal true}.
	 * </p>
	 * 
	 * @return the options, never {@literal null}
	 */
	public static SnfInvoicingOptions defaultOptions() {
		return new SnfInvoicingOptions();
	}

	/**
	 * Get an instance with the "dry run" options configured.
	 * 
	 * <p>
	 * The {@code dryRun} flag will be {@literal true} and
	 * {@code useAccountCredit} will be {@literal false}.
	 * </p>
	 * 
	 * @return the options, never {@literal null}
	 */
	public static SnfInvoicingOptions dryRunOptions() {
		return new SnfInvoicingOptions(true, false);
	}

	private boolean dryRun;
	private boolean useAccountCredit;

	/**
	 * Default constructor.
	 * 
	 * <p>
	 * The {@code dryRun} flag will be {@literal false} and
	 * {@code useAccountCredit} will be {@literal true}.
	 * </p>
	 */
	public SnfInvoicingOptions() {
		this(false, true);
	}

	/**
	 * Constructor.
	 * 
	 * @param dryRun
	 *        the "dry run" flag
	 * @param useAccountCredit
	 *        the "use account credit" flag
	 */
	public SnfInvoicingOptions(boolean dryRun, boolean useAccountCredit) {
		super();
		this.dryRun = dryRun;
		this.useAccountCredit = useAccountCredit;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("SnfInvoicingOptions{dryRun=");
		builder.append(dryRun);
		builder.append(", useAccountCredit=");
		builder.append(useAccountCredit);
		builder.append("}");
		return builder.toString();
	}

	@Override
	public int hashCode() {
		return Objects.hash(dryRun, useAccountCredit);
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( !(obj instanceof SnfInvoicingOptions) ) {
			return false;
		}
		SnfInvoicingOptions other = (SnfInvoicingOptions) obj;
		return dryRun == other.dryRun && useAccountCredit == other.useAccountCredit;
	}

	@Override
	public boolean isDryRun() {
		return dryRun;
	}

	/**
	 * Set the "dry run" flag.
	 * 
	 * @param dryRun
	 *        {@literal true} if an invoice should be generated but not
	 *        persisted
	 */
	public void setDryRun(boolean dryRun) {
		this.dryRun = dryRun;
	}

	@Override
	public boolean isUseAccountCredit() {
		return useAccountCredit;
	}

	/**
	 * Set the "use account credit" flag.
	 * 
	 * @param useAccountCredit
	 *        {@literal true} to use available account credit by adding a credit
	 *        item to the generated invoice
	 */
	public void setUseAccountCredit(boolean useAccountCredit) {
		this.useAccountCredit = useAccountCredit;
	}

}
