/* ==================================================================
 * BaseClaimableJob.java - 26/11/2018 10:07:10 AM
 *
 * Copyright 2018 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.domain;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.central.dao.BaseObjectEntity;

/**
 * Abstract implementation support for {@link ClaimableJob}.
 *
 * @param <C>
 *        the job configuration type
 * @param <R>
 *        the job result type
 * @param <S>
 *        the job state type
 * @param <K>
 *        the job entity primary key type
 * @author matt
 * @version 3.1
 * @since 1.44
 */
public abstract class BaseClaimableJob<C, R, S extends ClaimableJobState, K extends Comparable<K> & Serializable>
		extends BaseObjectEntity<K> implements ClaimableJob<C, R, S, K> {

	@Serial
	private static final long serialVersionUID = 6518967007802666051L;

	private @Nullable S jobState;
	private @Nullable C configuration;
	private @Nullable R result;
	private @Nullable String tokenId;
	private @Nullable String groupKey;
	private @Nullable Boolean jobSuccess;
	private @Nullable String message;
	private @Nullable Instant started;
	private @Nullable Instant completed;
	private double percentComplete;

	/**
	 * Constructor.
	 */
	public BaseClaimableJob() {
		super();
	}

	@Override
	public final @Nullable S getJobState() {
		return jobState;
	}

	/**
	 * Get the job state key value.
	 *
	 * @return the key value, or {@link ClaimableJobState#UNKNOWN_KEY} if not
	 *         known
	 */
	public final char getJobStateKey() {
		S state = getJobState();
		return (state != null ? state.getKey() : ClaimableJobState.UNKNOWN_KEY);
	}

	public final void setJobState(@Nullable S jobState) {
		this.jobState = jobState;
	}

	/**
	 * Get the configuration.
	 * 
	 * <p>
	 * This will call {@link #didGetConfiguration(Object)} and return the result
	 * of that method.
	 * </p>
	 * 
	 * {@inheritDoc}
	 * 
	 * @see #didGetConfiguration(Object)
	 */
	@Override
	public final @Nullable C getConfiguration() {
		return didGetConfiguration(configuration);
	}

	/**
	 * Hook called after configuration is accessed.
	 * 
	 * @param configuration
	 *        the configuration that was accessed
	 * @return the configuration to actually return
	 * @see #getConfiguration()
	 * @since 3.1
	 */
	protected @Nullable C didGetConfiguration(@Nullable C configuration) {
		return configuration;
	}

	/**
	 * Set the configuration.
	 * 
	 * <p>
	 * This will call {@link #didSetConfiguration(Object)}, passing
	 * {@code configuration}.
	 * </p>
	 * 
	 * @param configuration
	 *        the configuration to set
	 * @see #didSetConfiguration(Object)
	 * @see #replaceConfiguration(Object)
	 * @since 3.1
	 */
	public final void setConfiguration(@Nullable C configuration) {
		this.configuration = configuration;
		didSetConfiguration(configuration);
	}

	/**
	 * Hook called after configuration is set.
	 * 
	 * @param configuration
	 *        the configuration
	 * @see #setConfiguration(Object)
	 * @since 3.1
	 */
	protected void didSetConfiguration(@Nullable C configuration) {
		// extending classes can override
	}

	/**
	 * Get the configuration.
	 * 
	 * <p>
	 * This method does not call {@link #didGetConfiguration(Object)}.
	 * </p>
	 * 
	 * @return the configuration
	 * @since 3.1
	 */
	protected final @Nullable C configuration() {
		return this.configuration;
	}

	/**
	 * Replace the configuration.
	 * 
	 * <p>
	 * This method does not call {@link #didSetConfiguration(Object)}.
	 * </p>
	 * 
	 * @param configuration
	 *        the configuration to set
	 * @since 3.1
	 */
	protected final void replaceConfiguration(@Nullable C configuration) {
		this.configuration = configuration;
	}

	@Override
	public final @Nullable R getResult() {
		return result;
	}

	public final void setResult(@Nullable R result) {
		this.result = result;
	}

	@Override
	public final @Nullable String getTokenId() {
		return tokenId;
	}

	/**
	 * Set the authorization token ID.
	 *
	 * @param tokenId
	 *        the token ID to set
	 * @since 2.1
	 */
	public final void setTokenId(@Nullable String tokenId) {
		this.tokenId = tokenId;
	}

	@Override
	public final @Nullable Boolean getJobSuccess() {
		return jobSuccess;
	}

	public final void setJobSuccess(@Nullable Boolean jobSuccess) {
		this.jobSuccess = jobSuccess;
	}

	/**
	 * Get the success flag.
	 *
	 * @return {@literal true} if {@link #getJobSuccess()} is {@literal true}
	 */
	public boolean isSuccess() {
		Boolean jobSuccess = getJobSuccess();
		return (jobSuccess != null && jobSuccess);
	}

	@Override
	public final @Nullable String getMessage() {
		return message;
	}

	public final void setMessage(@Nullable String message) {
		this.message = message;
	}

	@Override
	public final @Nullable Instant getStarted() {
		return started;
	}

	public final void setStarted(@Nullable Instant started) {
		this.started = started;
	}

	@Override
	public final @Nullable Instant getCompleted() {
		return completed;
	}

	public final void setCompleted(@Nullable Instant completed) {
		this.completed = completed;
	}

	@Override
	public final double getPercentComplete() {
		return percentComplete;
	}

	public final void setPercentComplete(double percentComplete) {
		this.percentComplete = percentComplete;
	}

	@Override
	public final @Nullable String getGroupKey() {
		return groupKey;
	}

	public final void setGroupKey(@Nullable String groupKey) {
		this.groupKey = groupKey;
	}

}
