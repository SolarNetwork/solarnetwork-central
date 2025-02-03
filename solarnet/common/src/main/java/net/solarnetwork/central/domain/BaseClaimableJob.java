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
import net.solarnetwork.central.dao.BaseObjectEntity;

/**
 * Abstract implementation support for {@link ClaimableJob}.
 *
 * @author matt
 * @version 2.1
 * @since 1.44
 */
public abstract class BaseClaimableJob<C, R, S extends ClaimableJobState, PK extends Comparable<PK> & Serializable>
		extends BaseObjectEntity<PK> implements ClaimableJob<C, R, S, PK> {

	@Serial
	private static final long serialVersionUID = 6518967007802666051L;

	private S jobState;
	private C configuration;
	private R result;
	private String tokenId;
	private String groupKey;
	private Boolean jobSuccess;
	private String message;
	private Instant started;
	private Instant completed;
	private double percentComplete;

	@Override
	public S getJobState() {
		return jobState;
	}

	/**
	 * Get the job state key value.
	 *
	 * @return the key value, or {@link ClaimableJobState#UNKNOWN_KEY} if not
	 *         known
	 */
	public char getJobStateKey() {
		S state = getJobState();
		return (state != null ? state.getKey() : ClaimableJobState.UNKNOWN_KEY);
	}

	public void setJobState(S jobState) {
		this.jobState = jobState;
	}

	@Override
	public C getConfiguration() {
		return configuration;
	}

	public void setConfiguration(C configuration) {
		this.configuration = configuration;
	}

	@Override
	public R getResult() {
		return result;
	}

	public void setResult(R result) {
		this.result = result;
	}

	@Override
	public String getTokenId() {
		return tokenId;
	}

	/**
	 * Set the authorization token ID.
	 *
	 * @param tokenId
	 *        the token ID to set
	 * @since 2.1
	 */
	public void setTokenId(String tokenId) {
		this.tokenId = tokenId;
	}

	@Override
	public Boolean getJobSuccess() {
		return jobSuccess;
	}

	public void setJobSuccess(Boolean jobSuccess) {
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
	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	@Override
	public Instant getStarted() {
		return started;
	}

	public void setStarted(Instant started) {
		this.started = started;
	}

	@Override
	public Instant getCompleted() {
		return completed;
	}

	public void setCompleted(Instant completed) {
		this.completed = completed;
	}

	@Override
	public double getPercentComplete() {
		return percentComplete;
	}

	public void setPercentComplete(double percentComplete) {
		this.percentComplete = percentComplete;
	}

	@Override
	public String getGroupKey() {
		return groupKey;
	}

	public void setGroupKey(String groupKey) {
		this.groupKey = groupKey;
	}

}
