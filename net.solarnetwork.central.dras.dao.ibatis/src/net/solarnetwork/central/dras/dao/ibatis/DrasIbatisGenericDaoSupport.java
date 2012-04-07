/* ==================================================================
 * DrasIbatisGenericDaoSupport.java - Jun 4, 2011 5:38:42 PM
 * 
 * Copyright 2007-2011 SolarNetwork.net Dev Team
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
 * $Id$
 * ==================================================================
 */

package net.solarnetwork.central.dras.dao.ibatis;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import net.solarnetwork.central.dao.ibatis.IbatisGenericDaoSupport;
import net.solarnetwork.central.domain.Entity;
import net.solarnetwork.central.dras.domain.Member;

import org.joda.time.DateTime;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.orm.ibatis.SqlMapClientCallback;

import com.ibatis.sqlmap.client.SqlMapExecutor;
import com.ibatis.sqlmap.client.event.RowHandler;

/**
 * Abstract base Ibatis GenericDao for DRAS support.
 * 
 * @author matt
 * @version $Revision$
 */
public abstract class DrasIbatisGenericDaoSupport<T extends Entity<Long>>
extends IbatisGenericDaoSupport<T> {

	/** A query property for an {@code Effective} ID value. */
	public static final String EFFECTIVE_ID_PROPERTY = "effectiveId";
	
	/** A query property for a general member ID value, e.g. group membership. */
	public static final String MEMBER_ID_PROPERTY = "memberId";
	
	/**
	 * Constructor.
	 * 
	 * @param domainClass the domain class
	 */
	public DrasIbatisGenericDaoSupport(Class<? extends T> domainClass) {
		super(domainClass);
	}

	@Override
	protected void preprocessInsert(T datum) {
		if ( datum.getCreated() == null ) {
			// get creation date from current DB transaction
			Object o = getSqlMapClientTemplate().queryForObject("NOW");
			if ( o != null ) {
				BeanWrapper wrapper = PropertyAccessorFactory.forBeanPropertyAccess(datum);
				wrapper.setPropertyValue("created", o);
			}
		}
	}
	
	/**
	 * Persist a domain object ID set related to the entity managed by this DAO.
	 * 
	 * <p>Calls {@link #storeRelatedSet(Long, Class, Set, Long, Map)} passing no
	 * additional properties.</p>
	 * 
	 * @param parentId the ID of the parent of the member set
	 * @param memberClass the class of the members
	 * @param memberIdSet the set of member IDs to store
	 * @param effectiveId the effective ID
	 */
	protected void storeRelatedSet(final Long parentId, Class<?> memberClass,
			final Set<?> memberIdSet, final Long effectiveId) {
		storeRelatedSet(parentId, memberClass, memberIdSet, effectiveId, null);
	}
	
	/**
	 * Persist a domain object ID set related to the entity managed by this DAO.
	 * 
	 * <p>This method will use the {@link #getRelationDelete()} and 
	 * {@link #getRelationInsert()} query names to persist a set of related IDs to 
	 * a single parent entity. It works by issuing a {@code DELETE} based
	 * on the parent ID and effective ID (normally this should not actually
	 * delete anything) and then one {@code INSERT} per ID in {@code memberIdSet}.</p>
	 * 
	 * <p>This method will pass the following query properties:</p>
	 * 
	 * <dl>
	 *   <dt>{@link #ID_PROPERTY}</dt>
	 *   <dd>The {@code parentId} value, passed to all queries.</dd>
	 *   
	 *   <dt>{@link #EFFECTIVE_ID_PROPERTY}</dt>
	 *   <dd>The {@code effectiveId} value, passed to all queries.</dd>
	 *   
	 *   <dt>{@link #MEMBER_ID_PROPERTY}</dt>
	 *   <dd>A single value from the {@code memberIdSet}, passed to 
	 *   just the insert query.</dd>
	 * </dl>
	 * 
	 * @param parentId the ID of the parent of the member set
	 * @param memberClass the class of the members
	 * @param memberIdSet the set of member IDs to store
	 * @param effectiveId the effective ID
	 * @param additionalProps any extra properties to pass to the SQL script
	 * @see #getMemberSet(Long, Class, DateTime)
	 */
	protected void storeRelatedSet(final Long parentId, Class<?> memberClass,
			final Set<?> memberIdSet, final Long effectiveId, 
			final Map<String, ?> additionalProperties) {
		final String memberDomain = getMemberDomainKey(memberClass);
		final String deleteQuery = getRelationDelete()+memberDomain;
		final String insertQuery = getRelationInsert()+memberDomain;
		
		if ( log.isDebugEnabled() ) {
			log.debug("Assigning " +(memberIdSet == null ? 0 : memberIdSet.size())
					+" members to " +getDomainClass().getSimpleName() +" " +parentId +"'s "
					+memberDomain +" set effective " +effectiveId);
		}
		
		getSqlMapClientTemplate().execute(new SqlMapClientCallback<Object>() {
			@Override
			public Object doInSqlMapClient(SqlMapExecutor executor)
					throws SQLException {
				executor.startBatch();
				
				// delete all rows for the given effective ID (usually there should be none)
				Map<String, Object> props = new HashMap<String, Object>(2);
				if ( additionalProperties != null ) {
					props.putAll(additionalProperties);
				}
				props.put(ID_PROPERTY, parentId);
				if ( effectiveId != null ) {
					props.put(EFFECTIVE_ID_PROPERTY, effectiveId);
				}
				executor.delete(deleteQuery, props);
				
				// now insert rows for every member
				if ( memberIdSet != null ) {
					for ( Object memberId : memberIdSet ) {
						props.put(MEMBER_ID_PROPERTY, memberId);
						executor.insert(insertQuery, props);
					}
				}
				executor.executeBatch();
				return null;
			}
		});
	}

	/**
	 * Get a set of members for a given class and parent ID.
	 * 
	 * <p>Calls {@link #getRelatedSet(Long, Class, DateTime, Map)} passing
	 * in no additional properties.</p>
	 * 
	 * @param parentId the parent ID
	 * @param memberClass the member class type
	 * @param effectiveDate the effective date
	 * @return the found members, never <em>null</em>
	 */
	protected <E> Set<E> getRelatedSet(Long parentId, Class<? extends E> memberClass, 
			DateTime effectiveDate) {
		return getRelatedSet(parentId, memberClass, effectiveDate, null);
	}
	
	/**
	 * Get a set of members for a given class and parent ID.
	 * 
	 * <p>Use this method to get the complete set of members. The query
	 * used will be based on {@link #getRelationQueryForParent()}
	 * with {@link #getMemberDomainKey(Class)} appended.</p>
	 * 
	 * <p>This method will pass the following query properties:</p>
	 * 
	 * <dl>
	 *   <dt>{@link #ID_PROPERTY}</dt>
	 *   <dd>The {@code parentId} value.</dd>
	 *   
	 *   <dt>{@link #DATE_PROPERTY}</dt>
	 *   <dd>The {@code effectiveDate} value.</dd>
	 * </dl>
	 * 
	 * @param parentId the parent ID
	 * @param memberClass the member class type
	 * @param effectiveDate the effective date
	 * @param additionalProps any extra properties to pass to the SQL script
	 * @return the found members, never <em>null</em>
	 * @see #storeMemberSet(Long, Class, Set, Long)
	 */
	protected <E> Set<E> getRelatedSet(Long parentId, Class<? extends E> memberClass, 
			DateTime effectiveDate, Map<String, ?> additionalProps) {
		final String memberDomain = getMemberDomainKey(memberClass);
		final String query = getRelationQueryForParent()+memberDomain;
		Map<String, Object> sqlProps = new HashMap<String, Object>(2);
		if ( additionalProps != null ) {
			sqlProps.putAll(additionalProps);
		}
		sqlProps.put(ID_PROPERTY, parentId);
		if ( effectiveDate != null ) {
			sqlProps.put(DATE_PROPERTY, effectiveDate);
		}
		final Set<E> members = new LinkedHashSet<E>();
		getSqlMapClientTemplate().queryWithRowHandler(
				query, sqlProps, new RowHandler() {
			@SuppressWarnings("unchecked")
			@Override
			public void handleRow(Object valueObject) {
				members.add((E)valueObject);
			}
		});
		return members;
	}
	
	/**
	 * Get a set of members for a given class and parent ID.
	 * 
	 * <p>Use this method to get the complete set of members. The query
	 * used will be based on {@link #getRelationQueryForParent()}
	 * with {@link #getMemberDomainKey(Class)} appended.</p>
	 * 
	 * <p>This method will pass the following query properties:</p>
	 * 
	 * <dl>
	 *   <dt>{@link #ID_PROPERTY}</dt>
	 *   <dd>The {@code parentId} value.</dd>
	 *   
	 *   <dt>{@link #DATE_PROPERTY}</dt>
	 *   <dd>The {@code effectiveDate} value.</dd>
	 * </dl>
	 * 
	 * @param parentId the parent ID
	 * @param memberClass the member class type
	 * @param effectiveDate the effective date
	 * @return the found members, never <em>null</em>
	 * @see #storeMemberSet(Long, Class, Set, Long)
	 */
	protected <E> SortedSet<E> getRelatedSortedSet(Long parentId, Class<? extends E> memberClass, 
			DateTime effectiveDate) {
		final String memberDomain = getMemberDomainKey(memberClass);
		final String query = getRelationQueryForParent()+memberDomain;
		Map<String, Object> sqlProps = new HashMap<String, Object>(2);
		sqlProps.put(ID_PROPERTY, parentId);
		if ( effectiveDate != null ) {
			sqlProps.put(DATE_PROPERTY, effectiveDate);
		}
		final SortedSet<E> members = new TreeSet<E>();
		getSqlMapClientTemplate().queryWithRowHandler(
				query, sqlProps, new RowHandler() {
			@SuppressWarnings("unchecked")
			@Override
			public void handleRow(Object valueObject) {
				members.add((E)valueObject);
			}
		});
		return members;
	}
	
	/**
	 * Persist a domain object ID set related to the entity managed by this DAO.
	 * 
	 * @param parentId the ID of the parent of the member set
	 * @param memberClass the class of the members
	 * @param memberIdSet the set of member IDs to store
	 * @param effectiveId the effective ID
	 * @see #storeRelatedSet(Long, Class, Set, Long)
	 * @see #getMemberSet(Long, Class, DateTime)
	 */
	protected void storeMemberSet(final Long parentId, Class<?> memberClass,
			final Set<Long> memberIdSet, final Long effectiveId) {
		storeRelatedSet(parentId, memberClass, memberIdSet, effectiveId);
	}

	/**
	 * Get a set of members for a given class and parent ID.
	 * 
	 * @param parentId the parent ID
	 * @param memberClass the member class type
	 * @param effectiveDate the effective date
	 * @return the found members, never <em>null</em>
	 * @see #getRelatedSet(Long, Class, DateTime)
	 * @see #storeMemberSet(Long, Class, Set, Long)
	 */
	protected Set<Member> getMemberSet(Long parentId, Class<? extends Member> memberClass, 
			DateTime effectiveDate) {
		return getRelatedSet(parentId, memberClass, effectiveDate);
	}

	/**
	 * Query for a 1-to-1 related object.
	 * 
	 * <p>This uses the {@link #getRelationQueryForParent()} query appended
	 * with {@link #getMemberDomainKey(Class)} to query for a single object.</p>
	 * 
	 * <p>This method will pass the following query properties:</p>
	 * 
	 * <dl>
	 *   <dt>{@link #ID_PROPERTY}</dt>
	 *   <dd>The {@code parentId} value.</dd>
	 *   
	 *   <dt>{@link #DATE_PROPERTY}</dt>
	 *   <dd>The {@code effectiveDate} value.</dd>
	 * </dl>
	 * 
	 * @param parentId the parent ID
	 * @param effectiveDate the effective date, or <em>null</em> for current time
	 * @param relatedClass the related object class
	 * @return the related object, or <em>null</em> if not found
	 */
	protected <R> R getRelated(Long parentId, DateTime effectiveDate, 
			Class<? extends R> relatedClass) {
		Map<String, Object> sqlProps = new HashMap<String, Object>(2);
		sqlProps.put(ID_PROPERTY, parentId);
		if ( effectiveDate != null ) {
			sqlProps.put(DATE_PROPERTY, effectiveDate);
		}
		final String query = getRelationQueryForParent()+getMemberDomainKey(relatedClass);
		
		@SuppressWarnings("unchecked")
		R result = (R)getSqlMapClientTemplate().queryForObject(
				query, sqlProps);
		return result;
	}

	/**
	 * Set a 1-to-1 related object relationship.
	 * 
	 * <p>The query name is {@link #getRelationInsert()} appended with
	 * {@link #getMemberDomainKey(Class)}.</p>
	 * 
	 * <p>This method will pass the following query properties:</p>
	 * 
	 * <dl>
	 *   <dt>{@link #ID_PROPERTY}</dt>
	 *   <dd>The {@code parentId} value.</dd>
	 *   
	 *   <dt>{@link #EFFECTIVE_ID_PROPERTY}</dt>
	 *   <dd>The {@code effectiveId} value.</dd>
	 *   
	 *   <dt>{@link #MEMBER_ID_PROPERTY}</dt>
	 *   <dd>The {@code relatedId} value.</dd>
	 * </dl>
	 * 
	 * @param parentId the parent ID
	 * @param relatedId the related object ID
	 * @param effectiveId the effective ID
	 * @param relatedClass the related object class
	 */
	protected void setRelated(Long parentId, Long relatedId, Long effectiveId, Class<?> relatedClass) {
		Map<String, Object> sqlProps = new HashMap<String, Object>(2);
		sqlProps.put(ID_PROPERTY, parentId);
		if ( effectiveId != null ) {
			sqlProps.put(EFFECTIVE_ID_PROPERTY, effectiveId);
		}
		sqlProps.put(MEMBER_ID_PROPERTY, relatedId);
		final String query = getRelationInsert()+getMemberDomainKey(relatedClass);
		getSqlMapClientTemplate().insert(query, sqlProps);
	}

}
