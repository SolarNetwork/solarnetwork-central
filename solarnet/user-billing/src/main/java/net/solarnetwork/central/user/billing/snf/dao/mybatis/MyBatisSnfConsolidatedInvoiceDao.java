/* ==================================================================
 * MyBatisSnfConsolidatedInvoiceDao.java - 15/10/2026 8:58:02 am
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

package net.solarnetwork.central.user.billing.snf.dao.mybatis;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.central.dao.mybatis.support.BaseMyBatisGenericDaoSupport;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.user.billing.snf.dao.SnfConsolidatedInvoiceDao;
import net.solarnetwork.central.user.billing.snf.domain.SnfConsolidatedInvoice;
import net.solarnetwork.central.user.billing.snf.domain.SnfConsolidatedInvoiceFilter;
import net.solarnetwork.dao.BasicFilterResults;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.SortDescriptor;

/**
 * MyBatis implementation of {@link SnfConsolidatedInvoiceDao}.
 *
 * @author matt
 * @version 1.0
 */
public class MyBatisSnfConsolidatedInvoiceDao
		extends BaseMyBatisGenericDaoSupport<SnfConsolidatedInvoice, UserLongCompositePK>
		implements SnfConsolidatedInvoiceDao {

	/** Query name enumeration. */
	public enum QueryName {

		/** Find consolidated invoices matching a filter. */
		FindFiltered("find-SnfConsolidatedInvoice-for-filter"),

		/** Add an account invoice to a consolidated invoice. */
		InsertMember("insert-SnfConsolidatedInvoice-member"),

		;

		private final String queryName;

		QueryName(String queryName) {
			this.queryName = queryName;
		}

		/**
		 * Get the query name.
		 *
		 * @return the query name
		 */
		public String getQueryName() {
			return queryName;
		}

		/**
		 * Get the count query name.
		 *
		 * @return the count query name
		 */
		public String getCountQueryName() {
			return queryName + "-count";
		}
	}

	/**
	 * Constructor.
	 */
	public MyBatisSnfConsolidatedInvoiceDao() {
		super(SnfConsolidatedInvoice.class, UserLongCompositePK.class);
	}

	@Override
	protected UserLongCompositePK handleInsert(SnfConsolidatedInvoice entity) {
		UserLongCompositePK id = super.handleInsert(entity);
		if ( !id.entityIdIsAssigned() ) {
			id = new UserLongCompositePK(id.getUserId(), entity.getInvoiceId());
		}
		final List<UserLongCompositePK> invoiceIds = entity.getInvoiceIds();
		if ( invoiceIds != null ) {
			for ( UserLongCompositePK invoiceId : invoiceIds ) {
				getSqlSession().insert(QueryName.InsertMember.getQueryName(),
						Map.of("consolId", id.getEntityId(), "invoiceId", invoiceId.getEntityId()));
			}
		}
		return id;
	}

	@Override
	public FilterResults<SnfConsolidatedInvoice, UserLongCompositePK> findFiltered(
			SnfConsolidatedInvoiceFilter filter, @Nullable List<SortDescriptor> sorts,
			@Nullable Long offset, @Nullable Integer max) {
		if ( offset != null || max != null || sorts != null ) {
			filter = filter.clone();
			filter.setSorts(sorts);
			filter.setMax(max);
			// force offset to 0 if implied
			filter.setOffset(Objects.requireNonNullElse(offset, 0L));
		}

		// attempt count first, if max NOT specified as -1
		Long totalCount = null;
		if ( max == null || max != -1 ) {
			SnfConsolidatedInvoiceFilter countFilter = filter.clone();
			countFilter.setOffset(null);
			countFilter.setMax(null);
			Number n = getSqlSession().selectOne(QueryName.FindFiltered.getCountQueryName(),
					countFilter);
			if ( n != null ) {
				totalCount = n.longValue();
			}
		}

		List<SnfConsolidatedInvoice> results = selectList(QueryName.FindFiltered.getQueryName(),
				filter, null, null);
		return new BasicFilterResults<>(results, totalCount, offset != null ? offset.intValue() : 0,
				results.size());
	}

}
