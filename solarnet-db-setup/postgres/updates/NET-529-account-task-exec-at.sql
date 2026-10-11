/* ============================================================================
 * NET-529: let an account task be deferred to a later attempt.
 *
 * The claim function always returned the oldest task. A task whose handler asked
 * to be retried, or failed, stayed the oldest task, so the account task job claimed
 * it again straight away, repeatedly, while the tasks behind it waited.
 *
 * The new exec_at column holds the earliest time a task may be claimed. It defaults
 * to the time the task is added, and the job moves it forward to defer a task. The
 * created column cannot be used for this, because invoice generation tasks use it
 * as the start of the month to invoice.
 * ============================================================================
 */

ALTER TABLE solarbill.bill_account_task
	ADD COLUMN IF NOT EXISTS exec_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP;

/**
 * "Claim" an account task, so it may be processed by some external job. This function must be
 * called within a transaction. The returned row will be locked, so that the external job can
 * delete it once complete. The oldest row whose `exec_at` time has arrived is returned.
 */
CREATE OR REPLACE FUNCTION solarbill.claim_bill_account_task()
  RETURNS solarbill.bill_account_task LANGUAGE SQL VOLATILE AS
$$
	SELECT * FROM solarbill.bill_account_task
	WHERE exec_at <= CURRENT_TIMESTAMP
	ORDER BY created
	LIMIT 1
	FOR UPDATE SKIP LOCKED
$$;
