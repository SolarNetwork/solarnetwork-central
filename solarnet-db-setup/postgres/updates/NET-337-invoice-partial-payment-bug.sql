CREATE OR REPLACE FUNCTION solarbill.add_invoice_payments(
		  accountid 	BIGINT
		, pay_amount 	NUMERIC(11,2)
		, inv_ids		BIGINT[]
		, pay_ext_key 	CHARACTER VARYING(64) DEFAULT NULL
		, pay_ref 		CHARACTER VARYING(64) DEFAULT NULL
		, pay_type 		SMALLINT DEFAULT 1
		, pay_date 		TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
	)
	RETURNS solarbill.bill_payment
	LANGUAGE plpgsql VOLATILE AS
$$
DECLARE
	pay_rec 		solarbill.bill_payment;
BEGIN
	INSERT INTO solarbill.bill_payment (created,acct_id,pay_type,amount,currency,ext_key,ref)
	SELECT pay_date, a.id, pay_type, pay_amount, a.currency, pay_ext_key, pay_ref
	FROM solarbill.bill_account a
	WHERE a.id = accountid
	RETURNING *
	INTO pay_rec;

	IF NOT FOUND THEN
		RAISE EXCEPTION 'Account % not found.', accountid
		USING ERRCODE = 'integrity_constraint_violation',
			SCHEMA = 'solarbill',
			TABLE = 'bill_account',
			COLUMN = 'id';
    END IF;

	IF inv_ids IS NOT NULL THEN
		WITH payment AS (
			SELECT pay_amount AS payment
		)
		, invoice_payments AS (
			SELECT
				inv.id AS inv_id
				, inv.total_amount - COALESCE(inv.paid_amount, 0::NUMERIC(11,2)) AS due
				, GREATEST(0, LEAST(
					inv.total_amount - COALESCE(inv.paid_amount, 0::NUMERIC(11,2))
					, pay.payment - COALESCE(SUM(inv.total_amount - COALESCE(inv.paid_amount, 0::NUMERIC(11,2))) OVER win, 0::NUMERIC(11,2)))) AS applied
			FROM solarbill.bill_invoice_info inv, payment pay
			WHERE id = ANY(inv_ids) AND acct_id = accountid
			WINDOW win AS (ORDER BY inv.id ROWS BETWEEN UNBOUNDED PRECEDING AND 1 PRECEDING)
		)
		, applied_payments AS (
			SELECT * FROM invoice_payments
			WHERE applied > 0
		)
		INSERT INTO solarbill.bill_invoice_payment (created,acct_id, pay_id, inv_id, amount)
		SELECT pay_date, pay_rec.acct_id, pay_rec.id, applied_payments.inv_id, applied_payments.applied
		FROM applied_payments;

		IF NOT FOUND THEN
			RAISE EXCEPTION 'Invoice(s) % not found for account % payment %.', inv_ids, accountid, pay_amount
			USING ERRCODE = 'integrity_constraint_violation',
				SCHEMA = 'solarbill',
				TABLE = 'bill_invoice_payment',
				COLUMN = 'inv_id',
				HINT = 'The specified invoice(s) may not exist, might be for a different account, or might be fully paid already.';
		END IF;
	END IF;

	RETURN pay_rec;
END
$$;

