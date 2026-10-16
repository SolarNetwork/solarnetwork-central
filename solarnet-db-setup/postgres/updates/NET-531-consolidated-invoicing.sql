/* ============================================================================
 * NET-531: consolidated invoicing.
 *
 * A consolidated invoice groups the account invoices of a set of linked
 * accounts for one period, and is billed to one of them: the primary account.
 * It has no items of its own. The account invoices keep their items, and the
 * account balances are maintained from those as before.
 *
 * Consolidated invoice IDs come from the account invoice sequence, so their
 * invoice numbers never collide with an account invoice number.
 * ============================================================================
 */

-- table to store immutable consolidated invoice information
CREATE TABLE IF NOT EXISTS solarbill.bill_consol_invoice (
	id				BIGINT NOT NULL DEFAULT nextval('solarbill.bill_inv_seq'),
	created 		TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	acct_id			BIGINT NOT NULL,
	addr_id			BIGINT NOT NULL,
	date_start 		DATE NOT NULL,
	date_end 		DATE NOT NULL,
	currency		CHARACTER VARYING(3) NOT NULL,
	CONSTRAINT bill_consol_invoice_pkey PRIMARY KEY (id),
	CONSTRAINT bill_consol_invoice_acct_fk FOREIGN KEY (acct_id)
		REFERENCES solarbill.bill_account (id) MATCH SIMPLE
		ON UPDATE NO ACTION ON DELETE NO ACTION,
	CONSTRAINT bill_consol_invoice_address_fk FOREIGN KEY (addr_id)
		REFERENCES solarbill.bill_address (id) MATCH SIMPLE
		ON UPDATE NO ACTION ON DELETE NO ACTION
);

CREATE INDEX IF NOT EXISTS bill_consol_invoice_acct_date_idx
ON solarbill.bill_consol_invoice (acct_id, date_start DESC);

-- table to store the account invoices a consolidated invoice is made of
CREATE TABLE IF NOT EXISTS solarbill.bill_consol_invoice_member (
	consol_id		BIGINT NOT NULL,
	inv_id			BIGINT NOT NULL,
	CONSTRAINT bill_consol_invoice_member_pkey PRIMARY KEY (consol_id, inv_id),
	CONSTRAINT bill_consol_invoice_member_consol_fk FOREIGN KEY (consol_id)
		REFERENCES solarbill.bill_consol_invoice (id) MATCH SIMPLE
		ON UPDATE NO ACTION ON DELETE CASCADE,
	CONSTRAINT bill_consol_invoice_member_inv_fk FOREIGN KEY (inv_id)
		REFERENCES solarbill.bill_invoice (id) MATCH SIMPLE
		ON UPDATE NO ACTION ON DELETE NO ACTION
);

-- an account invoice belongs to at most one consolidated invoice
CREATE UNIQUE INDEX IF NOT EXISTS bill_consol_invoice_member_inv_idx
ON solarbill.bill_consol_invoice_member (inv_id);

/**
 * View to show consolidated invoice details including account information, and the total and
 * paid amounts of the account invoices it is made of.
 */
CREATE OR REPLACE VIEW solarbill.bill_consol_invoice_info AS
	SELECT
		  inv.id
		, 'INV-' || solarcommon.to_baseX(inv.id, 36) AS inv_num
		, inv.created
		, inv.acct_id
		, inv.addr_id
		, inv.date_start
		, inv.currency
		, act.user_id
		, adr.email
		, adr.disp_name
		, adr.country
		, adr.time_zone
		, mem.inv_count
		, mem.total_amount
		, mem.paid_amount
	FROM solarbill.bill_consol_invoice inv
	INNER JOIN solarbill.bill_account act ON act.id = inv.acct_id
	INNER JOIN solarbill.bill_address adr ON adr.id = inv.addr_id
	LEFT JOIN LATERAL (
		SELECT
			  COUNT(m.inv_id) AS inv_count
			, SUM(minv.total_amount) AS total_amount
			, SUM(minv.paid_amount) AS paid_amount
		FROM solarbill.bill_consol_invoice_member m
		INNER JOIN solarbill.bill_invoice_info minv ON minv.id = m.inv_id
		WHERE m.consol_id = inv.id
		) mem ON TRUE;

/**
 * Make a payment against a consolidated invoice.
 *
 * The payment is split into one payment per account: each account invoice of the consolidated
 * invoice is paid up to its unpaid amount, going in oldest to newest invoice order, up to an
 * overall maximum of the payment amount. Any amount left over is added as a payment to the
 * consolidated invoice's (primary) account, without being applied to an invoice.
 *
 * For example, to pay a consolidated invoice:
 *
 *     SELECT * FROM solarbill.add_consol_invoice_payments(
 *           consolid => 123
 *         , pay_amount => '2.34'::NUMERIC
 *         , pay_date => CURRENT_TIMESTAMP
 *     );
 *
 * @param consolid 		the consolidated invoice ID to apply the payment to
 * @param pay_amount 	the payment amount
 * @param pay_ext_key	the optional payment external key
 * @param pay_ref		the optional payment reference; defaults to the consolidated invoice number
 * @param pay_type		the payment type
 * @param pay_date		the payment date; defaults to current time
 * @return the added payments, one per account
 */
CREATE OR REPLACE FUNCTION solarbill.add_consol_invoice_payments(
		  consolid 		BIGINT
		, pay_amount 	NUMERIC(11,2)
		, pay_ext_key 	CHARACTER VARYING(64) DEFAULT NULL
		, pay_ref 		CHARACTER VARYING(64) DEFAULT NULL
		, pay_type 		SMALLINT DEFAULT 1
		, pay_date 		TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
	)
	RETURNS SETOF solarbill.bill_payment
	LANGUAGE plpgsql VOLATILE AS
$$
DECLARE
	consol		solarbill.bill_consol_invoice;
	remaining	NUMERIC(11,2) := pay_amount;
	inv			RECORD;
	applied		NUMERIC(11,2);
BEGIN
	SELECT * FROM solarbill.bill_consol_invoice WHERE id = consolid
	INTO consol;

	IF NOT FOUND THEN
		RAISE EXCEPTION 'Consolidated invoice % not found.', consolid
		USING ERRCODE = 'integrity_constraint_violation',
			SCHEMA = 'solarbill',
			TABLE = 'bill_consol_invoice',
			COLUMN = 'id';
	END IF;

	pay_ref := COALESCE(pay_ref, 'INV-' || solarcommon.to_baseX(consolid, 36));

	FOR inv IN
		SELECT
			  minv.id
			, minv.acct_id
			, COALESCE(minv.total_amount, 0::NUMERIC(11,2))
				- COALESCE(minv.paid_amount, 0::NUMERIC(11,2)) AS due
		FROM solarbill.bill_consol_invoice_member m
		INNER JOIN solarbill.bill_invoice_info minv ON minv.id = m.inv_id
		WHERE m.consol_id = consolid
		ORDER BY minv.id
	LOOP
		EXIT WHEN remaining <= 0::NUMERIC(11,2);
		applied := LEAST(inv.due, remaining);
		IF applied > 0::NUMERIC(11,2) THEN
			RETURN NEXT solarbill.add_invoice_payments(inv.acct_id, applied, ARRAY[inv.id],
				pay_ext_key, pay_ref, pay_type, pay_date);
			remaining := remaining - applied;
		END IF;
	END LOOP;

	IF remaining > 0::NUMERIC(11,2) THEN
		RETURN NEXT solarbill.add_invoice_payments(consol.acct_id, remaining, NULL::BIGINT[],
			pay_ext_key, pay_ref, pay_type, pay_date);
	END IF;

	RETURN;
END
$$;
