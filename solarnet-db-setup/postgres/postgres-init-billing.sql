CREATE SEQUENCE IF NOT EXISTS solarbill.bill_seq;
CREATE SEQUENCE IF NOT EXISTS solarbill.bill_inv_seq MINVALUE 1000 INCREMENT BY 1;

-- table to store billing address records, so invoices can maintain immutable
-- reference to billing address used at invoice generation time
CREATE TABLE IF NOT EXISTS solarbill.bill_address (
	id				BIGINT NOT NULL DEFAULT nextval('solarbill.bill_seq'),
	created 		TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	user_id			BIGINT NOT NULL,
	disp_name		CHARACTER VARYING(128) NOT NULL,
	email			citext NOT NULL,
	country			CHARACTER VARYING(2) NOT NULL,
	time_zone		CHARACTER VARYING(64) NOT NULL,
	region			CHARACTER VARYING(128),
	state_prov		CHARACTER VARYING(128),
	locality		CHARACTER VARYING(128),
	postal_code		CHARACTER VARYING(32),
	address			CHARACTER VARYING(256)[],
	CONSTRAINT bill_address_pkey PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS bill_address_user_idx ON solarbill.bill_address (user_id, created DESC);

-- table to store billing account information, with reference to current address
CREATE TABLE IF NOT EXISTS solarbill.bill_account (
	id				BIGINT NOT NULL DEFAULT nextval('solarbill.bill_seq'),
	created 		TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	user_id			BIGINT NOT NULL,
	addr_id			BIGINT NOT NULL,
	currency		CHARACTER VARYING(3) NOT NULL,
	locale			CHARACTER VARYING(5) NOT NULL,
	CONSTRAINT bill_account_pkey PRIMARY KEY (id),
	CONSTRAINT bill_account_address_fk FOREIGN KEY (addr_id)
		REFERENCES solarbill.bill_address (id) MATCH SIMPLE
		ON UPDATE NO ACTION ON DELETE NO ACTION
);

CREATE UNIQUE INDEX IF NOT EXISTS bill_account_user_idx ON solarbill.bill_account (user_id);

-- table to store immutable invoice information
CREATE TABLE IF NOT EXISTS solarbill.bill_invoice (
	id				BIGINT NOT NULL DEFAULT nextval('solarbill.bill_inv_seq'),
	created 		TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	acct_id			BIGINT NOT NULL,
	addr_id			BIGINT NOT NULL,
	date_start 		DATE NOT NULL,
	date_end 		DATE NOT NULL,
	currency		CHARACTER VARYING(3) NOT NULL,
	CONSTRAINT bill_invoice_pkey PRIMARY KEY (id),
	CONSTRAINT bill_invoice_acct_fk FOREIGN KEY (acct_id)
		REFERENCES solarbill.bill_account (id) MATCH SIMPLE
		ON UPDATE NO ACTION ON DELETE NO ACTION,
	CONSTRAINT bill_invoice_address_fk FOREIGN KEY (addr_id)
		REFERENCES solarbill.bill_address (id) MATCH SIMPLE
		ON UPDATE NO ACTION ON DELETE NO ACTION
);

CREATE INDEX IF NOT EXISTS bill_invoice_acct_date_idx ON solarbill.bill_invoice (acct_id, date_start DESC);

-- table to store immutable invoice item information
CREATE TABLE IF NOT EXISTS solarbill.bill_invoice_item (
	id				uuid NOT NULL DEFAULT uuid_generate_v4(),
	created 		TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	inv_id			BIGINT NOT NULL,
	item_type		SMALLINT NOT NULL DEFAULT 0,
	amount			NUMERIC(11,2) NOT NULL,
	quantity		NUMERIC NOT NULL,
	item_key		CHARACTER VARYING(64) NOT NULL,
	jmeta			jsonb,
	CONSTRAINT bill_invoice_item_pkey PRIMARY KEY (inv_id, id),
	CONSTRAINT bill_invoice_item_inv_fk FOREIGN KEY (inv_id)
		REFERENCES solarbill.bill_invoice (id) MATCH SIMPLE
		ON UPDATE NO ACTION ON DELETE NO ACTION
);

-- table to store billing invoice usage records
CREATE TABLE IF NOT EXISTS solarbill.bill_invoice_node_usage (
	inv_id				BIGINT NOT NULL,
	node_id				BIGINT NOT NULL,
	created 			TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    prop_count 			BIGINT NOT NULL DEFAULT 0,
    datum_q_count 		BIGINT NOT NULL DEFAULT 0,
    datum_s_count		BIGINT NOT NULL DEFAULT 0,
    instr_issued_count	BIGINT NOT NULL DEFAULT 0,
    flux_data_in_count	BIGINT NOT NULL DEFAULT 0,
	CONSTRAINT bill_invoice_usage_pkey PRIMARY KEY (inv_id, node_id),
	CONSTRAINT bill_invoice_usage_inv_fk FOREIGN KEY (inv_id)
		REFERENCES solarbill.bill_invoice (id) MATCH SIMPLE
		ON UPDATE NO ACTION ON DELETE NO ACTION
);

-- table to keep track of account payment status
-- there is no currency tracked here, just charges and payments to know the account status
CREATE TABLE IF NOT EXISTS solarbill.bill_account_balance (
	acct_id			BIGINT NOT NULL,
	created 		TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	charge_total	NUMERIC(19,2) NOT NULL,
	payment_total	NUMERIC(19,2) NOT NULL,
	avail_credit	NUMERIC(11,2) NOT NULL DEFAULT 0,
	CONSTRAINT bill_account_balance_pkey PRIMARY KEY (acct_id),
	CONSTRAINT bill_account_balance_acct_fk FOREIGN KEY (acct_id)
		REFERENCES solarbill.bill_account (id) MATCH SIMPLE
		ON UPDATE NO ACTION ON DELETE NO ACTION
);

/**
 * Trigger function to add/subtract from bill_account_balance as invoice items
 * are updated.
 */
CREATE OR REPLACE FUNCTION solarbill.maintain_bill_account_balance_charge()
	RETURNS "trigger"  LANGUAGE 'plpgsql' VOLATILE AS $$
DECLARE
	diff NUMERIC(19,2) := 0;
	acct BIGINT;
BEGIN
	SELECT acct_id FROM solarbill.bill_invoice
	WHERE id = (CASE
					WHEN TG_OP IN ('INSERT', 'UPDATE') THEN NEW.inv_id
					ELSE OLD.inv_id
				END)
	INTO acct;
	CASE TG_OP
		WHEN 'INSERT' THEN
			diff := CASE NEW.item_key WHEN 'account-credit-add' THEN 0 ELSE NEW.amount END;
		WHEN 'UPDATE' THEN
			diff := CASE NEW.item_key WHEN 'account-credit-add' THEN 0 ELSE NEW.amount - OLD.amount END;
		ELSE
			diff := CASE OLD.item_key WHEN 'account-credit-add' THEN 0 ELSE -OLD.amount END;
	END CASE;
	IF (diff < 0::NUMERIC(19,2)) OR (diff > 0::NUMERIC(19,2)) THEN
		INSERT INTO solarbill.bill_account_balance (acct_id, charge_total, payment_total)
		VALUES (acct, diff, 0)
		ON CONFLICT (acct_id) DO UPDATE
			SET charge_total =
				solarbill.bill_account_balance.charge_total + EXCLUDED.charge_total;
	END IF;

	CASE TG_OP
		WHEN 'INSERT', 'UPDATE' THEN
			RETURN NEW;
		ELSE
			RETURN OLD;
	END CASE;
END;
$$;

CREATE TRIGGER bill_account_balance_charge_tracker
    AFTER INSERT OR DELETE OR UPDATE
    ON solarbill.bill_invoice_item
    FOR EACH ROW
    EXECUTE PROCEDURE solarbill.maintain_bill_account_balance_charge();

/**
 * Claim a portion of the available credit in a bill_account_balance record.
 *
 * This will never claim more than the available credit in the account balance. Thus the returned
 * amount might be less than the requested amount.
 *
 * @param accountid the ID of the account to claim credit from
 * @param max_claim the maximum amount to claim, or `NULL` for the full amount available
 */
CREATE OR REPLACE FUNCTION solarbill.claim_account_credit(
	accountid BIGINT,
	max_claim NUMERIC(11,2) DEFAULT NULL
) RETURNS NUMERIC(11,2) LANGUAGE SQL VOLATILE AS
$$
	WITH claim AS (
		SELECT GREATEST(0::NUMERIC(11,2), LEAST(avail_credit, COALESCE(max_claim, avail_credit))) AS claim
		FROM solarbill.bill_account_balance
		WHERE acct_id = accountid
		FOR UPDATE
	)
	UPDATE solarbill.bill_account_balance
	SET avail_credit = avail_credit - claim.claim
	FROM claim
	WHERE acct_id = accountid
	RETURNING COALESCE(claim.claim, 0::NUMERIC(11,2))
$$;

-- table to store bill payment and credit information
-- pay_type specifies what type of payment, i.e. payment vs credit
CREATE TABLE IF NOT EXISTS solarbill.bill_payment (
	id				UUID NOT NULL DEFAULT uuid_generate_v4(),
	created 		TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	acct_id			BIGINT NOT NULL,
	pay_type		SMALLINT NOT NULL DEFAULT 0,
	amount			NUMERIC(11,2) NOT NULL,
	currency		CHARACTER VARYING(3) NOT NULL,
	ext_key			CHARACTER VARYING(64),
	ref				TEXT,
	CONSTRAINT bill_payment_pkey PRIMARY KEY (acct_id, id),
	CONSTRAINT bill_payment_account_fk FOREIGN KEY (acct_id)
		REFERENCES solarbill.bill_account (id) MATCH SIMPLE
		ON UPDATE NO ACTION ON DELETE NO ACTION
);

CREATE INDEX IF NOT EXISTS bill_payment_account_created_idx
ON solarbill.bill_payment (acct_id, created DESC);

/**
 * Trigger function to add/subtract from bill_account_balance as invoice items
 * are updated.
 */
CREATE OR REPLACE FUNCTION solarbill.maintain_bill_account_balance_payment()
	RETURNS "trigger"  LANGUAGE 'plpgsql' VOLATILE AS $$
DECLARE
	diff NUMERIC(19,2) := 0;
	acct BIGINT;
BEGIN
	CASE TG_OP
		WHEN 'INSERT' THEN
			diff := NEW.amount;
			acct := NEW.acct_id;
		WHEN 'UPDATE' THEN
			diff := NEW.amount - OLD.amount;
			acct := NEW.acct_id;
		ELSE
			diff := -OLD.amount;
			acct := OLD.acct_id;
	END CASE;
	IF (diff < 0::NUMERIC(19,2)) OR (diff > 0::NUMERIC(19,2)) THEN
		INSERT INTO solarbill.bill_account_balance (acct_id, charge_total, payment_total)
		VALUES (acct, 0, diff)
		ON CONFLICT (acct_id) DO UPDATE
			SET payment_total =
				solarbill.bill_account_balance.payment_total + EXCLUDED.payment_total;
	END IF;

	CASE TG_OP
		WHEN 'INSERT', 'UPDATE' THEN
			RETURN NEW;
		ELSE
			RETURN OLD;
	END CASE;
END;
$$;

CREATE TRIGGER bill_account_balance_payment_tracker
    AFTER INSERT OR DELETE OR UPDATE
    ON solarbill.bill_payment
    FOR EACH ROW
    EXECUTE PROCEDURE solarbill.maintain_bill_account_balance_payment();

-- table to track payments associated with invoices
CREATE TABLE IF NOT EXISTS solarbill.bill_invoice_payment (
	id				UUID NOT NULL DEFAULT uuid_generate_v4(),
	created 		TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	acct_id			BIGINT NOT NULL,
	pay_id			UUID NOT NULL,
	inv_id			BIGINT NOT NULL,
	amount			NUMERIC(11,2) NOT NULL,
	CONSTRAINT bill_invoice_payment_pkey PRIMARY KEY (id),
	CONSTRAINT bill_invoice_payment_payment_fk FOREIGN KEY (pay_id, acct_id)
		REFERENCES solarbill.bill_payment (id, acct_id) MATCH SIMPLE
		ON UPDATE NO ACTION ON DELETE NO ACTION,
	CONSTRAINT bill_invoice_payment_invoice_fk FOREIGN KEY (inv_id)
		REFERENCES solarbill.bill_invoice (id) MATCH SIMPLE
		ON UPDATE NO ACTION ON DELETE NO ACTION
);

CREATE INDEX IF NOT EXISTS bill_invoice_payment_acct_inv_idx
ON solarbill.bill_invoice_payment (acct_id,inv_id);

CREATE INDEX IF NOT EXISTS bill_invoice_payment_pay_idx
ON solarbill.bill_invoice_payment (pay_id);

/**
 * Trigger function to prevent invoice payments from exceeding the payment amount.
 */
CREATE OR REPLACE FUNCTION solarbill.validate_bill_invoice_payment()
	RETURNS "trigger"  LANGUAGE 'plpgsql' VOLATILE AS $$
DECLARE
	avail 	NUMERIC(19,2) := 0;
	ded_tot	NUMERIC(19,2) := 0;
	app_tot NUMERIC(19,2) := 0;
	chg_tot	NUMERIC(19,2) := 0;
BEGIN
	SELECT amount FROM solarbill.bill_payment
	WHERE acct_id = NEW.acct_id AND id = NEW.pay_id
	INTO avail;

	-- verify all invoice payments referencing this payment don't exceed funds
	-- and all invoice payments don't exceed invoice charge total
	-- by tracking sum of invoice payments deducted from this payment
	-- and the sum of invoice payments applied to this invoice
	SELECT
		SUM(CASE pay_id WHEN NEW.pay_id THEN amount ELSE 0 END)::NUMERIC(19,2),
		SUM(CASE inv_id WHEN NEW.inv_id THEN amount ELSE 0 END)::NUMERIC(19,2)
	FROM solarbill.bill_invoice_payment
	WHERE acct_id = NEW.acct_id AND (pay_id = NEW.pay_id OR inv_id = NEW.inv_id)
	INTO ded_tot, app_tot;

	SELECT SUM(amount)::NUMERIC(19,2) FROM solarbill.bill_invoice_item
	WHERE inv_id = NEW.inv_id
	INTO chg_tot;

	IF (ded_tot > avail) THEN
		RAISE EXCEPTION 'Invoice payments total amount % exceeds payment % amount %', ded_tot, NEW.pay_id, avail
		USING ERRCODE = 'integrity_constraint_violation',
			SCHEMA = 'solarbill',
			TABLE = 'bill_invoice_payment',
			COLUMN = 'amount',
			HINT = 'Sum of invoice payments must not exceed the solarbill.bill_payment.amount they relate to.';
	ELSIF (app_tot > chg_tot) THEN
		RAISE EXCEPTION 'Applied invoice payments total amount % exceeds invoice % amount %', app_tot, NEW.inv_id, chg_tot
		USING ERRCODE = 'integrity_constraint_violation',
			SCHEMA = 'solarbill',
			TABLE = 'bill_invoice_payment',
			COLUMN = 'amount',
			HINT = 'Sum of invoice payments must not exceed the sum of solarbill.bill_invoice_item.amount they relate to.';
	END IF;
	RETURN NULL;
END;
$$;

CREATE TRIGGER bill_invoice_payment_checker
    AFTER INSERT OR UPDATE
    ON solarbill.bill_invoice_payment
    FOR EACH ROW
    EXECUTE PROCEDURE solarbill.validate_bill_invoice_payment();

/**
 * Trigger function to prevent payment modifications from going under total invoice payments amount.
 */
CREATE OR REPLACE FUNCTION solarbill.validate_bill_payment()
	RETURNS "trigger"  LANGUAGE 'plpgsql' VOLATILE AS $$
DECLARE
	avail 	NUMERIC(19,2) := NEW.amount;
	inv_tot	NUMERIC(19,2) := 0;
BEGIN
	SELECT SUM(amount)::NUMERIC(19,2) FROM solarbill.bill_invoice_payment
	WHERE acct_id = NEW.acct_id AND pay_id = NEW.id
	INTO inv_tot;

	IF (inv_tot > avail) THEN
		RAISE EXCEPTION 'Invoice payments total amount % exceeds payment % amount %', inv_tot, NEW.id, avail
		USING ERRCODE = 'integrity_constraint_violation',
			SCHEMA = 'solarbill',
			TABLE = 'bill_payment',
			COLUMN = 'amount',
			HINT = 'Sum of invoice payments must not exceed the solarbill.bill_payment.amount they relate to.';
	END IF;
	RETURN NULL;
END;
$$;

CREATE TRIGGER bill_payment_checker
    AFTER UPDATE
    ON solarbill.bill_payment
    FOR EACH ROW
    EXECUTE PROCEDURE solarbill.validate_bill_payment();

-- table to hold asynchronous account tasks
CREATE TABLE IF NOT EXISTS solarbill.bill_account_task (
	id				uuid NOT NULL DEFAULT uuid_generate_v4(),
	acct_id			BIGINT NOT NULL,
	task_type		SMALLINT NOT NULL DEFAULT 0,
	created 		TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	jdata			jsonb,
	CONSTRAINT bill_account_task_pkey PRIMARY KEY (id),
	CONSTRAINT bill_account_task_acct_fk FOREIGN KEY (acct_id)
		REFERENCES solarbill.bill_account (id) MATCH SIMPLE
		ON UPDATE NO ACTION ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS bill_account_task_created_idx
ON solarbill.bill_account_task (created);

/**
 * "Claim" an account task, so it may be processed by some external job. This function must be
 * called within a transaction. The returned row will be locked, so that the external job can
 * delete it once complete. The oldest available row is returned.
 */
CREATE OR REPLACE FUNCTION solarbill.claim_bill_account_task()
  RETURNS solarbill.bill_account_task LANGUAGE SQL VOLATILE AS
$$
	SELECT * FROM solarbill.bill_account_task
	ORDER BY created
	LIMIT 1
	FOR UPDATE SKIP LOCKED
$$;

/**
 * Table to hold tax rates over time that are applied to specific invoice items for accounts in
 * specific "tax zones". Zones are like geographic constructs like countries, states, cities, etc.
 */
CREATE TABLE IF NOT EXISTS solarbill.bill_tax_code (
	id				BIGINT NOT NULL DEFAULT nextval('solarbill.bill_seq'),
	created 		TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	tax_zone 		VARCHAR(36) NOT NULL,
	item_key	 	VARCHAR(64) NOT NULL,
	tax_code 		VARCHAR(255) NOT NULL,
	tax_rate 		NUMERIC(15,9) NOT NULL,
	valid_from 		TIMESTAMP WITH TIME ZONE NOT NULL,
	valid_to 		TIMESTAMP WITH TIME ZONE,
	CONSTRAINT bill_tax_codes_pkey PRIMARY KEY (id)
);

CREATE INDEX bill_tax_code_item_idx ON solarbill.bill_tax_code (tax_zone, item_key, tax_code);

/**
 * Get the billing price tier effective dates, i.e. all dates where the rates changed.
 */
CREATE OR REPLACE FUNCTION solarbill.billing_usage_tier_effective_dates()
	RETURNS TABLE(
		effective_date DATE
	)
	LANGUAGE SQL IMMUTABLE AS
$$
	SELECT unnest(ARRAY[
		  '2008-01-01'::DATE
		, '2020-06-01'::DATE
		, '2021-06-01'::DATE
		, '2022-10-01'::DATE
		, '2023-10-01'::DATE
		, '2024-02-01'::DATE
		, '2024-10-01'::DATE
		, '2024-11-01'::DATE
		, '2025-02-01'::DATE
	]);
$$;

/**
 * Get the billing price tiers for a specific point in time.
 *
 * @param ts the billing effective date; defaults to the current date if not provided
 */
CREATE OR REPLACE FUNCTION solarbill.billing_usage_tiers(ts date DEFAULT CURRENT_DATE)
	RETURNS TABLE(
		meter_key TEXT,
		min BIGINT,
		cost NUMERIC,
		effective_date DATE
	)
	LANGUAGE plpgsql IMMUTABLE AS
$$
BEGIN
	IF ts < '2020-06-01'::DATE THEN
		RETURN QUERY SELECT *, '2008-01-01'::DATE AS effective_date FROM ( VALUES
			  ('datum-props-in', 		0::BIGINT, 	0.000009::NUMERIC)
			, ('datum-out', 			0::BIGINT, 	0.000002::NUMERIC)
			, ('datum-days-stored', 	0::BIGINT, 	0.000000006::NUMERIC)
		) AS t(min, meter_key, cost);
	ELSIF ts < '2021-06-01'::DATE THEN
		RETURN QUERY SELECT *, '2020-06-01'::DATE FROM ( VALUES
			  ('datum-props-in', 		0::BIGINT, 			0.000009::NUMERIC)
			, ('datum-props-in', 		50000::BIGINT, 		0.000006::NUMERIC)
			, ('datum-props-in', 		400000::BIGINT, 	0.000004::NUMERIC)
			, ('datum-props-in', 		1000000::BIGINT, 	0.000002::NUMERIC)

			, ('datum-out',				0::BIGINT, 			0.000002::NUMERIC)
			, ('datum-out',				50000::BIGINT, 		0.000001::NUMERIC)
			, ('datum-out',				400000::BIGINT, 	0.0000005::NUMERIC)
			, ('datum-out',				1000000::BIGINT, 	0.0000002::NUMERIC)

			, ('datum-days-stored', 	0::BIGINT, 			0.0000004::NUMERIC)
			, ('datum-days-stored', 	50000::BIGINT, 		0.0000002::NUMERIC)
			, ('datum-days-stored', 	400000::BIGINT, 	0.00000005::NUMERIC)
			, ('datum-days-stored', 	1000000::BIGINT, 	0.000000006::NUMERIC)
		) AS t(min, meter_key, cost);
	ELSIF ts < '2022-10-01'::DATE THEN
		RETURN QUERY SELECT *, '2021-06-01'::DATE FROM ( VALUES
			  ('datum-props-in', 		0::BIGINT, 				0.000005::NUMERIC)
			, ('datum-props-in', 		500000::BIGINT, 		0.000003::NUMERIC)
			, ('datum-props-in', 		10000000::BIGINT, 		0.0000008::NUMERIC)
			, ('datum-props-in', 		500000000::BIGINT, 		0.0000002::NUMERIC)

			, ('datum-out',				0::BIGINT, 				0.0000001::NUMERIC)
			, ('datum-out',				10000000::BIGINT, 		0.00000004::NUMERIC)
			, ('datum-out',				1000000000::BIGINT, 	0.000000004::NUMERIC)
			, ('datum-out',				100000000000::BIGINT, 	0.000000001::NUMERIC)

			, ('datum-days-stored', 	0::BIGINT, 				0.00000005::NUMERIC)
			, ('datum-days-stored', 	10000000::BIGINT, 		0.00000001::NUMERIC)
			, ('datum-days-stored', 	1000000000::BIGINT, 	0.000000003::NUMERIC)
			, ('datum-days-stored', 	100000000000::BIGINT,	0.000000002::NUMERIC)
		) AS t(min, meter_key, cost);
	ELSIF ts < '2023-10-01'::DATE THEN
		RETURN QUERY SELECT *, '2022-10-01'::DATE FROM ( VALUES
			  ('datum-props-in', 		0::BIGINT, 				0.000005::NUMERIC)
			, ('datum-props-in', 		500000::BIGINT, 		0.000003::NUMERIC)
			, ('datum-props-in', 		10000000::BIGINT, 		0.0000008::NUMERIC)
			, ('datum-props-in', 		500000000::BIGINT, 		0.0000002::NUMERIC)

			, ('datum-out',				0::BIGINT, 				0.0000001::NUMERIC)
			, ('datum-out',				10000000::BIGINT, 		0.00000004::NUMERIC)
			, ('datum-out',				1000000000::BIGINT, 	0.000000004::NUMERIC)
			, ('datum-out',				100000000000::BIGINT, 	0.000000001::NUMERIC)

			, ('datum-days-stored', 	0::BIGINT, 				0.00000005::NUMERIC)
			, ('datum-days-stored', 	10000000::BIGINT, 		0.00000001::NUMERIC)
			, ('datum-days-stored', 	1000000000::BIGINT, 	0.000000003::NUMERIC)
			, ('datum-days-stored', 	100000000000::BIGINT,	0.000000002::NUMERIC)

			, ('ocpp-chargers', 		0::BIGINT, 				2::NUMERIC)
			, ('ocpp-chargers', 		250::BIGINT, 			1::NUMERIC)
			, ('ocpp-chargers', 		12500::BIGINT, 			0.5::NUMERIC)
			, ('ocpp-chargers', 		500000::BIGINT, 		0.3::NUMERIC)

			, ('oscp-cap-groups', 		0::BIGINT, 				50::NUMERIC)
			, ('oscp-cap-groups', 		30::BIGINT, 			30::NUMERIC)
			, ('oscp-cap-groups', 		100::BIGINT, 			15::NUMERIC)
			, ('oscp-cap-groups', 		300::BIGINT, 			10::NUMERIC)
		) AS t(min, meter_key, cost);
	ELSEIF ts < '2024-02-01'::DATE THEN
		RETURN QUERY SELECT *, '2023-10-01'::DATE FROM ( VALUES
			  ('datum-props-in', 		0::BIGINT, 				0.000005::NUMERIC)
			, ('datum-props-in', 		500000::BIGINT, 		0.000003::NUMERIC)
			, ('datum-props-in', 		10000000::BIGINT, 		0.0000008::NUMERIC)
			, ('datum-props-in', 		500000000::BIGINT, 		0.0000002::NUMERIC)

			, ('datum-out',				0::BIGINT, 				0.0000001::NUMERIC)
			, ('datum-out',				10000000::BIGINT, 		0.00000004::NUMERIC)
			, ('datum-out',				1000000000::BIGINT, 	0.000000004::NUMERIC)
			, ('datum-out',				100000000000::BIGINT, 	0.000000001::NUMERIC)

			, ('datum-days-stored', 	0::BIGINT, 				0.00000005::NUMERIC)
			, ('datum-days-stored', 	10000000::BIGINT, 		0.00000001::NUMERIC)
			, ('datum-days-stored', 	1000000000::BIGINT, 	0.000000003::NUMERIC)
			, ('datum-days-stored', 	100000000000::BIGINT,	0.000000002::NUMERIC)

			, ('instr-issued', 			0::BIGINT, 				0.0001::NUMERIC)
			, ('instr-issued', 			10000::BIGINT, 			0.00005::NUMERIC)
			, ('instr-issued', 			100000::BIGINT, 		0.00002::NUMERIC)
			, ('instr-issued', 			1000000::BIGINT,		0.00001::NUMERIC)

			, ('ocpp-chargers', 		0::BIGINT, 				2::NUMERIC)
			, ('ocpp-chargers', 		250::BIGINT, 			1::NUMERIC)
			, ('ocpp-chargers', 		12500::BIGINT, 			0.5::NUMERIC)
			, ('ocpp-chargers', 		500000::BIGINT, 		0.3::NUMERIC)

			, ('oscp-cap-groups', 		0::BIGINT, 				50::NUMERIC)
			, ('oscp-cap-groups', 		30::BIGINT, 			30::NUMERIC)
			, ('oscp-cap-groups', 		100::BIGINT, 			15::NUMERIC)
			, ('oscp-cap-groups', 		300::BIGINT, 			10::NUMERIC)

			, ('dnp3-data-points', 		0::BIGINT, 				1::NUMERIC)
			, ('dnp3-data-points', 		20::BIGINT, 			0.6::NUMERIC)
			, ('dnp3-data-points', 		100::BIGINT, 			0.4::NUMERIC)
			, ('dnp3-data-points', 		500::BIGINT, 			0.2::NUMERIC)
		) AS t(min, meter_key, cost);
	ELSEIF ts < '2024-10-01'::DATE THEN
		RETURN QUERY SELECT *, '2024-02-01'::DATE FROM ( VALUES
			  ('datum-props-in', 		0::BIGINT, 				0.000005::NUMERIC)
			, ('datum-props-in', 		500000::BIGINT, 		0.000003::NUMERIC)
			, ('datum-props-in', 		10000000::BIGINT, 		0.0000008::NUMERIC)
			, ('datum-props-in', 		500000000::BIGINT, 		0.0000002::NUMERIC)

			, ('datum-out',				0::BIGINT, 				0.0000001::NUMERIC)
			, ('datum-out',				10000000::BIGINT, 		0.00000004::NUMERIC)
			, ('datum-out',				1000000000::BIGINT, 	0.000000004::NUMERIC)
			, ('datum-out',				100000000000::BIGINT, 	0.000000001::NUMERIC)

			, ('datum-days-stored', 	0::BIGINT, 				0.00000005::NUMERIC)
			, ('datum-days-stored', 	10000000::BIGINT, 		0.00000001::NUMERIC)
			, ('datum-days-stored', 	1000000000::BIGINT, 	0.000000003::NUMERIC)
			, ('datum-days-stored', 	100000000000::BIGINT,	0.000000002::NUMERIC)

			, ('instr-issued', 			0::BIGINT, 				0.0001::NUMERIC)
			, ('instr-issued', 			10000::BIGINT, 			0.00005::NUMERIC)
			, ('instr-issued', 			100000::BIGINT, 		0.00002::NUMERIC)
			, ('instr-issued', 			1000000::BIGINT,		0.00001::NUMERIC)

			, ('ocpp-chargers', 		0::BIGINT, 				2::NUMERIC)
			, ('ocpp-chargers', 		250::BIGINT, 			1::NUMERIC)
			, ('ocpp-chargers', 		12500::BIGINT, 			0.5::NUMERIC)
			, ('ocpp-chargers', 		500000::BIGINT, 		0.3::NUMERIC)

			, ('dnp3-data-points', 		0::BIGINT, 				1::NUMERIC)
			, ('dnp3-data-points', 		20::BIGINT, 			0.6::NUMERIC)
			, ('dnp3-data-points', 		100::BIGINT, 			0.4::NUMERIC)
			, ('dnp3-data-points', 		500::BIGINT, 			0.2::NUMERIC)

			, ('oscp-cap-groups', 		0::BIGINT, 				2::NUMERIC)
			, ('oscp-cap-groups', 		100::BIGINT, 			1.5::NUMERIC)
			, ('oscp-cap-groups', 		500::BIGINT, 			1.25::NUMERIC)
			, ('oscp-cap-groups', 		1250::BIGINT, 			1::NUMERIC)

			, ('oscp-cap', 				0::BIGINT, 				0.00003::NUMERIC)
			, ('oscp-cap', 				6000000::BIGINT, 		0.000025::NUMERIC)
			, ('oscp-cap', 				40000000::BIGINT, 		0.0000175::NUMERIC)
			, ('oscp-cap', 				100000000::BIGINT, 		0.00001::NUMERIC)
		) AS t(min, meter_key, cost);
	ELSEIF ts < '2024-11-01'::DATE THEN
		RETURN QUERY SELECT *, '2024-10-01'::DATE FROM ( VALUES
			  ('datum-props-in', 		0::BIGINT, 				0.000005::NUMERIC)
			, ('datum-props-in', 		500000::BIGINT, 		0.000003::NUMERIC)
			, ('datum-props-in', 		10000000::BIGINT, 		0.0000008::NUMERIC)
			, ('datum-props-in', 		500000000::BIGINT, 		0.0000002::NUMERIC)

			, ('datum-out',				0::BIGINT, 				0.0000001::NUMERIC)
			, ('datum-out',				10000000::BIGINT, 		0.00000004::NUMERIC)
			, ('datum-out',				1000000000::BIGINT, 	0.000000004::NUMERIC)
			, ('datum-out',				100000000000::BIGINT, 	0.000000001::NUMERIC)

			, ('datum-days-stored', 	0::BIGINT, 				0.00000005::NUMERIC)
			, ('datum-days-stored', 	10000000::BIGINT, 		0.00000001::NUMERIC)
			, ('datum-days-stored', 	1000000000::BIGINT, 	0.000000003::NUMERIC)
			, ('datum-days-stored', 	100000000000::BIGINT,	0.000000002::NUMERIC)

			, ('instr-issued', 			0::BIGINT, 				0.0001::NUMERIC)
			, ('instr-issued', 			10000::BIGINT, 			0.00005::NUMERIC)
			, ('instr-issued', 			100000::BIGINT, 		0.00002::NUMERIC)
			, ('instr-issued', 			1000000::BIGINT,		0.00001::NUMERIC)

			, ('flux-data-in', 			0::BIGINT, 				0.00000001::NUMERIC)
			, ('flux-data-in', 			1000000000::BIGINT, 	0.000000006::NUMERIC)
			, ('flux-data-in', 			10000000000::BIGINT, 	0.000000003::NUMERIC)
			, ('flux-data-in', 			100000000000::BIGINT,	0.0000000015::NUMERIC)

			, ('flux-data-out', 		0::BIGINT, 				0.000000009::NUMERIC)
			, ('flux-data-out', 		1000000000::BIGINT, 	0.0000000055::NUMERIC)
			, ('flux-data-out', 		10000000000::BIGINT, 	0.0000000025::NUMERIC)
			, ('flux-data-out', 		100000000000::BIGINT,	0.0000000012::NUMERIC)

			, ('ocpp-chargers', 		0::BIGINT, 				2::NUMERIC)
			, ('ocpp-chargers', 		250::BIGINT, 			1::NUMERIC)
			, ('ocpp-chargers', 		12500::BIGINT, 			0.5::NUMERIC)
			, ('ocpp-chargers', 		500000::BIGINT, 		0.3::NUMERIC)

			, ('dnp3-data-points', 		0::BIGINT, 				1::NUMERIC)
			, ('dnp3-data-points', 		20::BIGINT, 			0.6::NUMERIC)
			, ('dnp3-data-points', 		100::BIGINT, 			0.4::NUMERIC)
			, ('dnp3-data-points', 		500::BIGINT, 			0.2::NUMERIC)

			, ('oscp-cap-groups', 		0::BIGINT, 				2::NUMERIC)
			, ('oscp-cap-groups', 		100::BIGINT, 			1.5::NUMERIC)
			, ('oscp-cap-groups', 		500::BIGINT, 			1.25::NUMERIC)
			, ('oscp-cap-groups', 		1250::BIGINT, 			1::NUMERIC)

			, ('oscp-cap', 				0::BIGINT, 				0.00003::NUMERIC)
			, ('oscp-cap', 				6000000::BIGINT, 		0.000025::NUMERIC)
			, ('oscp-cap', 				40000000::BIGINT, 		0.0000175::NUMERIC)
			, ('oscp-cap', 				100000000::BIGINT, 		0.00001::NUMERIC)

			, ('oauth-client-creds', 	0::BIGINT, 				10::NUMERIC)
			, ('oauth-client-creds', 	100::BIGINT, 			5::NUMERIC)
			, ('oauth-client-creds', 	500::BIGINT, 			2.5::NUMERIC)
		) AS t(min, meter_key, cost);
	ELSEIF ts < '2025-02-01'::DATE THEN
		RETURN QUERY SELECT *, '2024-11-01'::DATE FROM ( VALUES
			  ('datum-props-in', 		0::BIGINT, 				0.00000575::NUMERIC)
			, ('datum-props-in', 		500000::BIGINT, 		0.00000345::NUMERIC)
			, ('datum-props-in', 		10000000::BIGINT, 		0.00000092::NUMERIC)
			, ('datum-props-in', 		500000000::BIGINT, 		0.00000023::NUMERIC)

			, ('datum-out',				0::BIGINT, 				0.000000115::NUMERIC)
			, ('datum-out',				10000000::BIGINT, 		0.000000046::NUMERIC)
			, ('datum-out',				1000000000::BIGINT, 	0.000000005::NUMERIC)
			, ('datum-out',				100000000000::BIGINT, 	0.000000002::NUMERIC)

			, ('datum-days-stored', 	0::BIGINT, 				0.0000000575::NUMERIC)
			, ('datum-days-stored', 	10000000::BIGINT, 		0.0000000115::NUMERIC)
			, ('datum-days-stored', 	1000000000::BIGINT, 	0.00000000345::NUMERIC)
			, ('datum-days-stored', 	100000000000::BIGINT,	0.0000000023::NUMERIC)

			, ('instr-issued', 			0::BIGINT, 				0.0001::NUMERIC)
			, ('instr-issued', 			10000::BIGINT, 			0.00005::NUMERIC)
			, ('instr-issued', 			100000::BIGINT, 		0.00002::NUMERIC)
			, ('instr-issued', 			1000000::BIGINT,		0.00001::NUMERIC)

			, ('flux-data-in', 			0::BIGINT, 				0.00000001::NUMERIC)
			, ('flux-data-in', 			1000000000::BIGINT, 	0.000000006::NUMERIC)
			, ('flux-data-in', 			10000000000::BIGINT, 	0.000000003::NUMERIC)
			, ('flux-data-in', 			100000000000::BIGINT,	0.0000000015::NUMERIC)

			, ('flux-data-out', 		0::BIGINT, 				0.000000009::NUMERIC)
			, ('flux-data-out', 		1000000000::BIGINT, 	0.0000000055::NUMERIC)
			, ('flux-data-out', 		10000000000::BIGINT, 	0.0000000025::NUMERIC)
			, ('flux-data-out', 		100000000000::BIGINT,	0.0000000012::NUMERIC)

			, ('ocpp-chargers', 		0::BIGINT, 				2::NUMERIC)
			, ('ocpp-chargers', 		250::BIGINT, 			1::NUMERIC)
			, ('ocpp-chargers', 		12500::BIGINT, 			0.5::NUMERIC)
			, ('ocpp-chargers', 		500000::BIGINT, 		0.3::NUMERIC)

			, ('dnp3-data-points', 		0::BIGINT, 				1::NUMERIC)
			, ('dnp3-data-points', 		20::BIGINT, 			0.6::NUMERIC)
			, ('dnp3-data-points', 		100::BIGINT, 			0.4::NUMERIC)
			, ('dnp3-data-points', 		500::BIGINT, 			0.2::NUMERIC)

			, ('oscp-cap-groups', 		0::BIGINT, 				2::NUMERIC)
			, ('oscp-cap-groups', 		100::BIGINT, 			1.5::NUMERIC)
			, ('oscp-cap-groups', 		500::BIGINT, 			1.25::NUMERIC)
			, ('oscp-cap-groups', 		1250::BIGINT, 			1::NUMERIC)

			, ('oscp-cap', 				0::BIGINT, 				0.00003::NUMERIC)
			, ('oscp-cap', 				6000000::BIGINT, 		0.000025::NUMERIC)
			, ('oscp-cap', 				40000000::BIGINT, 		0.0000175::NUMERIC)
			, ('oscp-cap', 				100000000::BIGINT, 		0.00001::NUMERIC)

			, ('oauth-client-creds', 	0::BIGINT, 				10::NUMERIC)
			, ('oauth-client-creds', 	100::BIGINT, 			5::NUMERIC)
			, ('oauth-client-creds', 	500::BIGINT, 			2.5::NUMERIC)
		) AS t(min, meter_key, cost);
	ELSE
		RETURN QUERY SELECT *, '2025-02-01'::DATE FROM ( VALUES
			  ('datum-props-in', 		0::BIGINT, 				0.00000575::NUMERIC)
			, ('datum-props-in', 		500000::BIGINT, 		0.00000345::NUMERIC)
			, ('datum-props-in', 		10000000::BIGINT, 		0.00000092::NUMERIC)
			, ('datum-props-in', 		500000000::BIGINT, 		0.00000023::NUMERIC)

			, ('datum-out',				0::BIGINT, 				0.000000115::NUMERIC)
			, ('datum-out',				10000000::BIGINT, 		0.000000046::NUMERIC)
			, ('datum-out',				1000000000::BIGINT, 	0.000000005::NUMERIC)
			, ('datum-out',				100000000000::BIGINT, 	0.000000002::NUMERIC)

			, ('datum-days-stored', 	0::BIGINT, 				0.0000000575::NUMERIC)
			, ('datum-days-stored', 	10000000::BIGINT, 		0.0000000115::NUMERIC)
			, ('datum-days-stored', 	1000000000::BIGINT, 	0.00000000345::NUMERIC)
			, ('datum-days-stored', 	100000000000::BIGINT,	0.0000000023::NUMERIC)

			, ('instr-issued', 			0::BIGINT, 				0.0001::NUMERIC)
			, ('instr-issued', 			10000::BIGINT, 			0.00005::NUMERIC)
			, ('instr-issued', 			100000::BIGINT, 		0.00002::NUMERIC)
			, ('instr-issued', 			1000000::BIGINT,		0.00001::NUMERIC)

			, ('flux-data-in', 			0::BIGINT, 				0.00000001::NUMERIC)
			, ('flux-data-in', 			1000000000::BIGINT, 	0.000000006::NUMERIC)
			, ('flux-data-in', 			10000000000::BIGINT, 	0.000000003::NUMERIC)
			, ('flux-data-in', 			100000000000::BIGINT,	0.0000000015::NUMERIC)

			, ('flux-data-out', 		0::BIGINT, 				0.000000009::NUMERIC)
			, ('flux-data-out', 		1000000000::BIGINT, 	0.0000000055::NUMERIC)
			, ('flux-data-out', 		10000000000::BIGINT, 	0.0000000025::NUMERIC)
			, ('flux-data-out', 		100000000000::BIGINT,	0.0000000012::NUMERIC)

			, ('ocpp-chargers', 		0::BIGINT, 				2::NUMERIC)
			, ('ocpp-chargers', 		250::BIGINT, 			1::NUMERIC)
			, ('ocpp-chargers', 		12500::BIGINT, 			0.5::NUMERIC)
			, ('ocpp-chargers', 		500000::BIGINT, 		0.3::NUMERIC)

			, ('dnp3-data-points', 		0::BIGINT, 				1::NUMERIC)
			, ('dnp3-data-points', 		20::BIGINT, 			0.6::NUMERIC)
			, ('dnp3-data-points', 		100::BIGINT, 			0.4::NUMERIC)
			, ('dnp3-data-points', 		500::BIGINT, 			0.2::NUMERIC)

			, ('oscp-cap-groups', 		0::BIGINT, 				2::NUMERIC)
			, ('oscp-cap-groups', 		100::BIGINT, 			1.5::NUMERIC)
			, ('oscp-cap-groups', 		500::BIGINT, 			1.25::NUMERIC)
			, ('oscp-cap-groups', 		1250::BIGINT, 			1::NUMERIC)

			, ('oscp-cap', 				0::BIGINT, 				0.00003::NUMERIC)
			, ('oscp-cap', 				6000000::BIGINT, 		0.000025::NUMERIC)
			, ('oscp-cap', 				40000000::BIGINT, 		0.0000175::NUMERIC)
			, ('oscp-cap', 				100000000::BIGINT, 		0.00001::NUMERIC)

			, ('oauth-client-creds', 	0::BIGINT, 				10::NUMERIC)
			, ('oauth-client-creds', 	100::BIGINT, 			5::NUMERIC)
			, ('oauth-client-creds', 	500::BIGINT, 			2.5::NUMERIC)

			, ('c2c-data', 				0::BIGINT, 				0.00000020::NUMERIC)
			, ('c2c-data', 				1000000000::BIGINT, 	0.00000009::NUMERIC)
			, ('c2c-data', 				10000000000::BIGINT, 	0.00000003::NUMERIC)
			, ('c2c-data', 				100000000000::BIGINT,	0.000000015::NUMERIC)
		) AS t(min, meter_key, cost);
	END IF;
END
$$;

/**
 * Calculate the metered usage amounts for an account over a billing period, by node.
 *
 * @param userid the ID of the user to calculate the billing information for
 * @param ts_min the start date to calculate the usage for (inclusive)
 * @param ts_max the end date to calculate the usage for (exclusive)
 */
CREATE OR REPLACE FUNCTION solarbill.billing_usage(userid BIGINT, ts_min TIMESTAMP, ts_max TIMESTAMP)
	RETURNS TABLE(
		  node_id BIGINT
		, prop_in BIGINT
		, datum_stored BIGINT
		, datum_out BIGINT
		, instr_issued BIGINT
		, flux_data_in BIGINT
	) LANGUAGE sql STABLE AS
$$
	WITH nodes AS (
		SELECT nlt.time_zone,
			ts_min AT TIME ZONE nlt.time_zone AS sdate,
			ts_max AT TIME ZONE nlt.time_zone AS edate,
			array_agg(DISTINCT nlt.node_id) AS nodes
		FROM solarnet.node_local_time nlt
		INNER JOIN solaruser.user_node un ON un.node_id = nlt.node_id
		WHERE un.user_id = userid
		GROUP BY nlt.time_zone
	)
	, stored AS (
		SELECT
			meta.node_id
			, SUM(acc.datum_count + acc.datum_hourly_count + acc.datum_daily_count + acc.datum_monthly_count) AS datum_count
		FROM nodes nodes
		INNER JOIN solardatm.da_datm_meta meta ON meta.node_id = ANY(nodes.nodes)
		INNER JOIN solardatm.aud_acc_datm_daily acc ON acc.stream_id = meta.stream_id
			AND acc.ts_start >= nodes.sdate AND acc.ts_start < nodes.edate
		GROUP BY meta.node_id
	)
	, datum AS (
		SELECT
			meta.node_id
			, SUM(a.prop_count)::bigint AS prop_count
			, SUM(a.datum_q_count)::bigint AS datum_q_count
			, SUM(a.flux_byte_count)::bigint AS flux_byte_count
		FROM nodes nodes
		INNER JOIN solardatm.da_datm_meta meta ON meta.node_id = ANY(nodes.nodes)
		INNER JOIN solardatm.aud_datm_daily a ON a.stream_id = meta.stream_id
			AND a.ts_start >= nodes.sdate AND a.ts_start < nodes.edate
		GROUP BY meta.node_id
	)
	, svc AS (
		SELECT
			a.node_id
			, (SUM(a.cnt) FILTER (WHERE a.service = 'inst'))::BIGINT AS instr_issued
		FROM nodes nodes
		INNER JOIN solardatm.aud_node_daily a ON a.node_id = ANY(nodes.nodes)
			AND a.ts_start >= nodes.sdate AND a.ts_start < nodes.edate
		GROUP BY a.node_id
	)
	SELECT
		  COALESCE(s.node_id, a.node_id, svc.node_id) AS node_id
		, COALESCE(a.prop_count, 0)::BIGINT AS prop_in
		, COALESCE(s.datum_count, 0)::BIGINT AS datum_stored
		, COALESCE(a.datum_q_count, 0)::BIGINT AS datum_out
		, COALESCE(svc.instr_issued, 0)::BIGINT AS instr_issued
		, COALESCE(a.flux_byte_count, 0)::BIGINT AS flux_data_in
	FROM stored s
	FULL OUTER JOIN datum a ON a.node_id = s.node_id
	FULL OUTER JOIN svc ON svc.node_id = s.node_id
$$;

/**
 * Calculate the usage associated with billing tiers for a given user on a given month, by node.
 *
 * This calls the `solarbill.billing_usage_tiers()` function to determine the pricing tiers to use
 * at the given `effective_date`.
 *
 * @param userid the ID of the user to calculate the billing information for
 * @param ts_min the start date to calculate the costs for (inclusive)
 * @param ts_max the end date to calculate the costs for (exclusive)
 * @param effective_date optional pricing date, to calculate the tiers effective at that time
 */
CREATE OR REPLACE FUNCTION solarbill.billing_node_tier_details(userid BIGINT, ts_min TIMESTAMP, ts_max TIMESTAMP, effective_date date DEFAULT CURRENT_DATE)
	RETURNS TABLE(
		node_id		BIGINT,
		meter_key 	TEXT,
		tier_min 	BIGINT,
		tier_count 	BIGINT
	) LANGUAGE sql STABLE AS
$$
	WITH tiers AS (
		SELECT * FROM solarbill.billing_usage_tiers(effective_date)
	)
	, usage AS (
		SELECT
			  node_id
			, prop_in
			, datum_stored
			, datum_out
			, instr_issued
			, flux_data_in
		FROM solarbill.billing_usage(userid, ts_min, ts_max)
		WHERE prop_in > 0
			OR datum_stored > 0
			OR datum_out > 0
			OR instr_issued > 0
			OR flux_data_in > 0
	)
	SELECT
		  n.node_id
		, tiers.meter_key
		, tiers.min AS tier_min
		, LEAST(GREATEST(CASE meter_key
			WHEN 'datum-props-in' THEN n.prop_in
			WHEN 'datum-days-stored' THEN n.datum_stored
			WHEN 'datum-out' THEN n.datum_out
			WHEN 'instr-issued' THEN n.instr_issued
			WHEN 'flux-data-in' THEN n.flux_data_in
			ELSE NULL END - tiers.min, 0), COALESCE(LEAD(tiers.min) OVER win - tiers.min, GREATEST(CASE meter_key
			WHEN 'datum-props-in' THEN n.prop_in
			WHEN 'datum-days-stored' THEN n.datum_stored
			WHEN 'datum-out' THEN n.datum_out
			WHEN 'instr-issued' THEN n.instr_issued
			WHEN 'flux-data-in' THEN n.flux_data_in
			ELSE NULL END - tiers.min, 0))) AS tier_count
	FROM usage n
	CROSS JOIN tiers
	WINDOW win AS (PARTITION BY n.node_id, tiers.meter_key ORDER BY tiers.min)
$$;

/**
 * Calculate the usage associated with billing tiers for a given user on a given month, by node.
 *
 * This calls the `solarbill.billing_node_tier_details()` function to determine the pricing tiers to use
 * at the given `effective_date`.
 *
 * @param userid the ID of the user to calculate the billing information for
 * @param ts_min the start date to calculate the costs for (inclusive)
 * @param ts_max the end date to calculate the costs for (exclusive)
 * @param effective_date optional pricing date, to calculate the costs effective at that time
 */
CREATE OR REPLACE FUNCTION solarbill.billing_node_details(userid BIGINT, ts_min TIMESTAMP, ts_max TIMESTAMP, effective_date DATE DEFAULT CURRENT_DATE)
	RETURNS TABLE(
		node_id 				BIGINT,
		prop_in 				BIGINT,
		prop_in_tiers 			NUMERIC[],
		datum_stored 			BIGINT,
		datum_stored_tiers 		NUMERIC[],
		datum_out 				BIGINT,
		datum_out_tiers 		NUMERIC[],
		instr_issued 			BIGINT,
		instr_issued_tiers 		NUMERIC[],
		flux_data_in 			BIGINT,
		flux_data_in_tiers 		NUMERIC[]
	) LANGUAGE sql STABLE AS
$$
	WITH tiers AS (
		SELECT * FROM solarbill.billing_node_tier_details(userid, ts_min, ts_max, effective_date)
	)
	, counts AS (
		SELECT
			  node_id
			, meter_key
			, SUM(tier_count)::BIGINT AS total_count
			, ARRAY_AGG(tier_count::NUMERIC) AS tier_counts
		FROM tiers
		WHERE tier_count > 0
		GROUP BY node_id, meter_key
	)
	SELECT
		  node_id
		, SUM(CASE meter_key WHEN 'datum-props-in' THEN total_count ELSE NULL END)::BIGINT AS prop_in
		, solarcommon.first(CASE meter_key WHEN 'datum-props-in' THEN tier_counts ELSE NULL END) AS prop_in_tiers

		, SUM(CASE meter_key WHEN 'datum-days-stored' THEN total_count ELSE NULL END)::BIGINT AS datum_stored
		, solarcommon.first(CASE meter_key WHEN 'datum-days-stored' THEN tier_counts ELSE NULL END) AS datum_stored_tiers

		, SUM(CASE meter_key WHEN 'datum-out' THEN total_count ELSE NULL END)::BIGINT AS datum_out
		, solarcommon.first(CASE meter_key WHEN 'datum-out' THEN tier_counts ELSE NULL END) AS datum_out_tiers

		, SUM(CASE meter_key WHEN 'instr-issued' THEN total_count ELSE NULL END)::BIGINT AS instr_issued
		, solarcommon.first(CASE meter_key WHEN 'instr-issued' THEN tier_counts ELSE NULL END) AS instr_issued_tiers

		, SUM(CASE meter_key WHEN 'flux-data-in' THEN total_count ELSE NULL END)::BIGINT AS flux_data_in
		, solarcommon.first(CASE meter_key WHEN 'flux-data-in' THEN tier_counts ELSE NULL END) AS flux_data_in_tiers
	FROM counts
	GROUP BY node_id
$$;


/**
 * Calculate the costs associated with billing tiers for a given user on a given month.
 *
 * This calls the `solarbill.billing_usage_tiers()` function to determine the pricing tiers to use
 * at the given `effective_date`.
 *
 * @param userid the ID of the user to calculate the billing information for
 * @param ts_min the start date to calculate the costs for (inclusive)
 * @param ts_max the end date to calculate the costs for (exclusive)
 * @param effective_date optional pricing date, to calculate the costs effective at that time
 */
CREATE OR REPLACE FUNCTION solarbill.billing_usage_tier_details(userid BIGINT, ts_min TIMESTAMP, ts_max TIMESTAMP, effective_date date DEFAULT CURRENT_DATE)
	RETURNS TABLE(
		meter_key 	TEXT,
		tier_min 	BIGINT,
		tier_count 	BIGINT,
		tier_rate 	NUMERIC,
		tier_cost 	NUMERIC
	) LANGUAGE sql STABLE AS
$$
	WITH tiers AS (
		SELECT * FROM solarbill.billing_usage_tiers(effective_date)
	)
	, usage AS (
		SELECT
			  SUM(prop_in)::BIGINT AS prop_in
			, SUM(datum_stored)::BIGINT AS datum_stored
			, SUM(datum_out)::BIGINT AS datum_out
			, SUM(instr_issued)::BIGINT AS instr_issued
			, SUM(flux_data_in)::BIGINT AS flux_data_in
		FROM solarbill.billing_usage(userid, ts_min, ts_max)
	)
	, ocpp AS (
		SELECT count(*) AS ocpp_charger_count
		FROM solarev.ocpp_charge_point
		WHERE user_id = userid AND enabled = TRUE
	)
	, oscp AS (
		SELECT count(*) AS oscp_cap_group_count
		FROM solaroscp.oscp_cg_conf
		WHERE user_id = userid AND enabled = TRUE
	)
	, dnp3 AS (
		SELECT count(*) AS dnp3_data_point_count FROM (
			SELECT user_id, server_id, idx, 'm' AS dtype
			FROM solardnp3.dnp3_server_meas
			WHERE user_id = userid AND enabled = TRUE
			UNION ALL
			SELECT user_id, server_id, idx, 'c' AS dtype
			FROM solardnp3.dnp3_server_ctrl
			WHERE user_id = userid AND enabled = TRUE
		) counts
	)
	, oscp_cap AS (
		WITH oscp AS (
			-- extract datum stream + instantaneous properties from OSCP assets
			SELECT oac.node_id, oac.source_id, unnest(iprops) AS prop_name
			FROM solaroscp.oscp_asset_conf oac
			WHERE oac.user_id = userid AND enabled = TRUE
		)
		, m AS (
			-- extract stream ID and instantaneous property index from stream metadata
			SELECT m.stream_id
				, m.names_i
				, array_position(m.names_i, oscp.prop_name) AS prop_idx
				, COALESCE(l.time_zone, 'UTC') AS time_zone
			FROM oscp
			INNER JOIN solardatm.da_datm_meta m ON m.node_id = oscp.node_id AND m.source_id = oscp.source_id
			LEFT OUTER JOIN solarnet.sn_node n ON n.node_id = m.node_id
			LEFT OUTER JOIN solarnet.sn_loc l ON l.id = n.loc_id
		)
		, d AS (
			-- extract maximum value seen on instantaneous stat for each stream + property
			SELECT d.stream_id, MAX(d.stat_i[m.prop_idx][3]) AS prop_max
			FROM m
			INNER JOIN solardatm.agg_datm_daily d ON d.stream_id = m.stream_id
			WHERE d.ts_start >= ts_min AT TIME ZONE m.time_zone
				AND d.ts_start < ts_max AT TIME ZONE m.time_zone
			GROUP BY d.stream_id

		)
		SELECT COALESCE(SUM(d.prop_max), 0)::BIGINT AS oscp_cap
		FROM d
	)
	, usvc AS (
		WITH tz AS (
			SELECT COALESCE(l.time_zone, 'UTC') AS time_zone
			FROM solaruser.user_user u
			LEFT OUTER JOIN solarnet.sn_loc l ON l.id = u.loc_id
			WHERE u.id = userid
		)
		SELECT
			  (SUM(a.cnt) FILTER (WHERE a.service = 'flxo'))::BIGINT AS flux_data_out
			, (SUM(a.cnt) FILTER (WHERE a.service = 'ccio'))::BIGINT AS c2c_data
		FROM solardatm.aud_user_daily a, tz
		WHERE a.user_id = userid
			AND a.ts_start >= ts_min AT TIME ZONE tz.time_zone
			AND a.ts_start < ts_max AT TIME ZONE tz.time_zone
	)
	, oauth AS (
		WITH cnts AS (
			SELECT COUNT(*) AS cnt
			FROM solaroscp.oscp_fp_token
			WHERE user_id = userid
				AND oauth = TRUE

			UNION ALL

			SELECT COUNT(*) AS cnt
			FROM solardin.inin_credential
			WHERE user_id = userid
				AND oauth = TRUE
		)
		SELECT SUM(a.cnt)::BIGINT AS oauth_client_creds_count
		FROM cnts a
	)
	SELECT
		  tiers.meter_key
		, tiers.min AS tier_min
		, LEAST(GREATEST(CASE meter_key
			WHEN 'datum-props-in' THEN n.prop_in
			WHEN 'datum-days-stored' THEN n.datum_stored
			WHEN 'datum-out' THEN n.datum_out
			WHEN 'instr-issued' THEN n.instr_issued
			WHEN 'flux-data-in' THEN n.flux_data_in
			WHEN 'flux-data-out' THEN usvc.flux_data_out
			WHEN 'ocpp-chargers' THEN ocpp.ocpp_charger_count
			WHEN 'oscp-cap-groups' THEN oscp.oscp_cap_group_count
			WHEN 'dnp3-data-points' THEN dnp3.dnp3_data_point_count
			WHEN 'oscp-cap' THEN oscp_cap.oscp_cap
			WHEN 'oauth-client-creds' THEN oauth.oauth_client_creds_count
			WHEN 'c2c-data' THEN usvc.c2c_data
			ELSE NULL END - tiers.min, 0), COALESCE(LEAD(tiers.min) OVER win - tiers.min, GREATEST(CASE meter_key
			WHEN 'datum-props-in' THEN n.prop_in
			WHEN 'datum-days-stored' THEN n.datum_stored
			WHEN 'datum-out' THEN n.datum_out
			WHEN 'instr-issued' THEN n.instr_issued
			WHEN 'flux-data-in' THEN n.flux_data_in
			WHEN 'flux-data-out' THEN usvc.flux_data_out
			WHEN 'ocpp-chargers' THEN ocpp.ocpp_charger_count
			WHEN 'oscp-cap-groups' THEN oscp.oscp_cap_group_count
			WHEN 'dnp3-data-points' THEN dnp3.dnp3_data_point_count
			WHEN 'oscp-cap' THEN oscp_cap.oscp_cap
			WHEN 'oauth-client-creds' THEN oauth.oauth_client_creds_count
			WHEN 'c2c-data' THEN usvc.c2c_data
			ELSE NULL END - tiers.min, 0))) AS tier_count
		, tiers.cost AS tier_rate
		, LEAST(GREATEST(CASE meter_key
			WHEN 'datum-props-in' THEN n.prop_in
			WHEN 'datum-days-stored' THEN n.datum_stored
			WHEN 'datum-out' THEN n.datum_out
			WHEN 'instr-issued' THEN n.instr_issued
			WHEN 'flux-data-in' THEN n.flux_data_in
			WHEN 'flux-data-out' THEN usvc.flux_data_out
			WHEN 'ocpp-chargers' THEN ocpp.ocpp_charger_count
			WHEN 'oscp-cap-groups' THEN oscp.oscp_cap_group_count
			WHEN 'dnp3-data-points' THEN dnp3.dnp3_data_point_count
			WHEN 'oscp-cap' THEN oscp_cap.oscp_cap
			WHEN 'oauth-client-creds' THEN oauth.oauth_client_creds_count
			WHEN 'c2c-data' THEN usvc.c2c_data
			ELSE NULL END - tiers.min, 0), COALESCE(LEAD(tiers.min) OVER win - tiers.min, GREATEST(CASE meter_key
			WHEN 'datum-props-in' THEN n.prop_in
			WHEN 'datum-days-stored' THEN n.datum_stored
			WHEN 'datum-out' THEN n.datum_out
			WHEN 'instr-issued' THEN n.instr_issued
			WHEN 'flux-data-in' THEN n.flux_data_in
			WHEN 'flux-data-out' THEN usvc.flux_data_out
			WHEN 'ocpp-chargers' THEN ocpp.ocpp_charger_count
			WHEN 'oscp-cap-groups' THEN oscp.oscp_cap_group_count
			WHEN 'dnp3-data-points' THEN dnp3.dnp3_data_point_count
			WHEN 'oscp-cap' THEN oscp_cap.oscp_cap
			WHEN 'oauth-client-creds' THEN oauth.oauth_client_creds_count
			WHEN 'c2c-data' THEN usvc.c2c_data
			ELSE NULL END - tiers.min, 0))) * tiers.cost AS tier_cost
	FROM usage n, ocpp, oscp, dnp3, oscp_cap, usvc, oauth
	CROSS JOIN tiers
	WINDOW win AS (PARTITION BY tiers.meter_key ORDER BY tiers.min)
$$;

/**
 * Calculate the costs associated with billing tiers for a given user on a given month,
 * with tiers aggregated.
 *
 * This calls the `solarbill.billing_usage_tier_details()` function to determine the pricing tiers to use
 * at the given `effective_date`.
 *
 * @param userid the ID of the user to calculate the billing information for
 * @param ts_min the start date to calculate the costs for (inclusive)
 * @param ts_max the end date to calculate the costs for (exclusive)
 * @param effective_date optional pricing date, to calculate the costs effective at that time
 */
CREATE OR REPLACE FUNCTION solarbill.billing_usage_details(userid BIGINT, ts_min TIMESTAMP, ts_max TIMESTAMP, effective_date DATE DEFAULT CURRENT_DATE)
	RETURNS TABLE(
		total_cost 						NUMERIC,
		prop_in 						BIGINT,
		prop_in_cost 					NUMERIC,
		prop_in_tiers 					NUMERIC[],
		prop_in_tiers_cost 				NUMERIC[],
		datum_stored 					BIGINT,
		datum_stored_cost 				NUMERIC,
		datum_stored_tiers 				NUMERIC[],
		datum_stored_tiers_cost 		NUMERIC[],
		datum_out 						BIGINT,
		datum_out_cost 					NUMERIC,
		datum_out_tiers 				NUMERIC[],
		datum_out_tiers_cost 			NUMERIC[],
		instr_issued 					BIGINT,
		instr_issued_cost 				NUMERIC,
		instr_issued_tiers 				NUMERIC[],
		instr_issued_tiers_cost 		NUMERIC[],
		flux_data_in 					BIGINT,
		flux_data_in_cost 				NUMERIC,
		flux_data_in_tiers 				NUMERIC[],
		flux_data_in_tiers_cost 		NUMERIC[],
		flux_data_out 					BIGINT,
		flux_data_out_cost 				NUMERIC,
		flux_data_out_tiers 			NUMERIC[],
		flux_data_out_tiers_cost 		NUMERIC[],
		ocpp_chargers					BIGINT,
		ocpp_chargers_cost				NUMERIC,
		ocpp_chargers_tiers				NUMERIC[],
		ocpp_chargers_tiers_cost		NUMERIC[],
		oscp_cap_groups					BIGINT,
		oscp_cap_groups_cost			NUMERIC,
		oscp_cap_groups_tiers			NUMERIC[],
		oscp_cap_groups_tiers_cost		NUMERIC[],
		dnp3_data_points				BIGINT,
		dnp3_data_points_cost			NUMERIC,
		dnp3_data_points_tiers			NUMERIC[],
		dnp3_data_points_tiers_cost		NUMERIC[],
		oscp_cap						BIGINT,
		oscp_cap_cost					NUMERIC,
		oscp_cap_tiers					NUMERIC[],
		oscp_cap_tiers_cost				NUMERIC[],
		oauth_client_creds				BIGINT,
		oauth_client_creds_cost			NUMERIC,
		oauth_client_creds_tiers		NUMERIC[],
		oauth_client_creds_tiers_cost	NUMERIC[],
		c2c_data						BIGINT,
		c2c_data_cost					NUMERIC,
		c2c_data_tiers					NUMERIC[],
		c2c_data_tiers_cost				NUMERIC[]
	) LANGUAGE sql STABLE AS
$$
	WITH tier_costs AS (
		SELECT * FROM solarbill.billing_usage_tier_details(userid, ts_min, ts_max, effective_date)
	)
	, costs AS (
		SELECT
			  meter_key
			, SUM(tier_count)::BIGINT AS total_count
			, SUM(tier_cost) AS total_cost
			, ARRAY_AGG(tier_count::NUMERIC) AS tier_counts
			, ARRAY_AGG(tier_cost) AS tier_costs
		FROM tier_costs
		WHERE tier_count > 0
		GROUP BY meter_key
	)
	SELECT
		  SUM(total_cost) AS total_cost

		, SUM(CASE meter_key WHEN 'datum-props-in' THEN total_count ELSE NULL END)::BIGINT AS prop_in
		, SUM(CASE meter_key WHEN 'datum-props-in' THEN total_cost ELSE NULL END) AS prop_in_cost
		, solarcommon.first(CASE meter_key WHEN 'datum-props-in' THEN tier_counts ELSE NULL END) AS prop_in_tiers
		, solarcommon.first(CASE meter_key WHEN 'datum-props-in' THEN tier_costs ELSE NULL END) AS prop_in_tiers_cost

		, SUM(CASE meter_key WHEN 'datum-days-stored' THEN total_count ELSE NULL END)::BIGINT AS datum_stored
		, SUM(CASE meter_key WHEN 'datum-days-stored' THEN total_cost ELSE NULL END) AS datum_stored_cost
		, solarcommon.first(CASE meter_key WHEN 'datum-days-stored' THEN tier_counts ELSE NULL END) AS datum_stored_tiers
		, solarcommon.first(CASE meter_key WHEN 'datum-days-stored' THEN tier_costs ELSE NULL END) AS datum_stored_tiers_cost

		, SUM(CASE meter_key WHEN 'datum-out' THEN total_count ELSE NULL END)::BIGINT AS datum_out
		, SUM(CASE meter_key WHEN 'datum-out' THEN total_cost ELSE NULL END) AS datum_out_cost
		, solarcommon.first(CASE meter_key WHEN 'datum-out' THEN tier_counts ELSE NULL END) AS datum_out_tiers
		, solarcommon.first(CASE meter_key WHEN 'datum-out' THEN tier_costs ELSE NULL END) AS datum_out_tiers_cost

		, SUM(CASE meter_key WHEN 'instr-issued' THEN total_count ELSE NULL END)::BIGINT AS instr_issued
		, SUM(CASE meter_key WHEN 'instr-issued' THEN total_cost ELSE NULL END) AS instr_issued_cost
		, solarcommon.first(CASE meter_key WHEN 'instr-issued' THEN tier_counts ELSE NULL END) AS instr_issued_tiers
		, solarcommon.first(CASE meter_key WHEN 'instr-issued' THEN tier_costs ELSE NULL END) AS instr_issued_tiers_cost

		, SUM(CASE meter_key WHEN 'flux-data-in' THEN total_count ELSE NULL END)::BIGINT AS flux_data_in
		, SUM(CASE meter_key WHEN 'flux-data-in' THEN total_cost ELSE NULL END) AS flux_data_in_cost
		, solarcommon.first(CASE meter_key WHEN 'flux-data-in' THEN tier_counts ELSE NULL END) AS flux_data_in_tiers
		, solarcommon.first(CASE meter_key WHEN 'flux-data-in' THEN tier_costs ELSE NULL END) AS flux_data_in_tiers_cost

		, SUM(CASE meter_key WHEN 'flux-data-out' THEN total_count ELSE NULL END)::BIGINT AS flux_data_out
		, SUM(CASE meter_key WHEN 'flux-data-out' THEN total_cost ELSE NULL END) AS flux_data_out_cost
		, solarcommon.first(CASE meter_key WHEN 'flux-data-out' THEN tier_counts ELSE NULL END) AS flux_data_out_tiers
		, solarcommon.first(CASE meter_key WHEN 'flux-data-out' THEN tier_costs ELSE NULL END) AS flux_data_out_tiers_cost

		, SUM(CASE meter_key WHEN 'ocpp-chargers' THEN total_count ELSE NULL END)::BIGINT AS ocpp_chargers
		, SUM(CASE meter_key WHEN 'ocpp-chargers' THEN total_cost ELSE NULL END) AS ocpp_chargers_cost
		, solarcommon.first(CASE meter_key WHEN 'ocpp-chargers' THEN tier_counts ELSE NULL END) AS ocpp_chargers_tiers
		, solarcommon.first(CASE meter_key WHEN 'ocpp-chargers' THEN tier_costs ELSE NULL END) AS ocpp_chargers_tiers_cost

		, SUM(CASE meter_key WHEN 'oscp-cap-groups' THEN total_count ELSE NULL END)::BIGINT AS oscp_cap_groups
		, SUM(CASE meter_key WHEN 'oscp-cap-groups' THEN total_cost ELSE NULL END) AS oscp_cap_groups_cost
		, solarcommon.first(CASE meter_key WHEN 'oscp-cap-groups' THEN tier_counts ELSE NULL END) AS oscp_cap_groups_tiers
		, solarcommon.first(CASE meter_key WHEN 'oscp-cap-groups' THEN tier_costs ELSE NULL END) AS oscp_cap_groups_tiers_cost

		, SUM(CASE meter_key WHEN 'dnp3-data-points' THEN total_count ELSE NULL END)::BIGINT AS dnp3_data_points
		, SUM(CASE meter_key WHEN 'dnp3-data-points' THEN total_cost ELSE NULL END) AS dnp3_data_points_cost
		, solarcommon.first(CASE meter_key WHEN 'dnp3-data-points' THEN tier_counts ELSE NULL END) AS dnp3_data_points_tiers
		, solarcommon.first(CASE meter_key WHEN 'dnp3-data-points' THEN tier_costs ELSE NULL END) AS dnp3_data_points_tiers_cost

		, SUM(CASE meter_key WHEN 'oscp-cap' THEN total_count ELSE NULL END)::BIGINT AS oscp_cap
		, SUM(CASE meter_key WHEN 'oscp-cap' THEN total_cost ELSE NULL END) AS oscp_cap_cost
		, solarcommon.first(CASE meter_key WHEN 'oscp-cap' THEN tier_counts ELSE NULL END) AS oscp_cap_tiers
		, solarcommon.first(CASE meter_key WHEN 'oscp-cap' THEN tier_costs ELSE NULL END) AS oscp_cap_tiers_cost

		, SUM(CASE meter_key WHEN 'oauth-client-creds' THEN total_count ELSE NULL END)::BIGINT AS oauth_client_creds
		, SUM(CASE meter_key WHEN 'oauth-client-creds' THEN total_cost ELSE NULL END) AS oauth_client_creds_cost
		, solarcommon.first(CASE meter_key WHEN 'oauth-client-creds' THEN tier_counts ELSE NULL END) AS oauth_client_creds_tiers
		, solarcommon.first(CASE meter_key WHEN 'oauth-client-creds' THEN tier_costs ELSE NULL END) AS oauth_client_creds_tiers_cost

		, SUM(CASE meter_key WHEN 'c2c-data' THEN total_count ELSE NULL END)::BIGINT AS c2c_data
		, SUM(CASE meter_key WHEN 'c2c-data' THEN total_cost ELSE NULL END) AS c2c_data_cost
		, solarcommon.first(CASE meter_key WHEN 'c2c-data' THEN tier_counts ELSE NULL END) AS c2c_data_tiers
		, solarcommon.first(CASE meter_key WHEN 'c2c-data' THEN tier_costs ELSE NULL END) AS c2c_data_tiers_cost
	FROM costs
	HAVING
		   SUM(CASE meter_key WHEN 'datum-props-in' THEN total_count ELSE NULL END)::BIGINT > 0
		OR SUM(CASE meter_key WHEN 'datum-days-stored' THEN total_count ELSE NULL END)::BIGINT > 0
		OR SUM(CASE meter_key WHEN 'datum-out' THEN total_count ELSE NULL END)::BIGINT > 0
		OR SUM(CASE meter_key WHEN 'instr-issued' THEN total_count ELSE NULL END)::BIGINT > 0
		OR SUM(CASE meter_key WHEN 'flux-data-in' THEN total_count ELSE NULL END)::BIGINT > 0
		OR SUM(CASE meter_key WHEN 'flux-data-out' THEN total_count ELSE NULL END)::BIGINT > 0
		OR SUM(CASE meter_key WHEN 'ocpp-chargers' THEN total_count ELSE NULL END)::BIGINT > 0
		OR SUM(CASE meter_key WHEN 'oscp-cap-groups' THEN total_count ELSE NULL END)::BIGINT > 0
		OR SUM(CASE meter_key WHEN 'dnp3-data-points' THEN total_count ELSE NULL END)::BIGINT > 0
		OR SUM(CASE meter_key WHEN 'oscp-cap' THEN total_count ELSE NULL END)::BIGINT > 0
		OR SUM(CASE meter_key WHEN 'oauth-client-creds' THEN total_count ELSE NULL END)::BIGINT > 0
		OR SUM(CASE meter_key WHEN 'c2c-data' THEN total_count ELSE NULL END)::BIGINT > 0
$$;

/**
 * Make a payment, optionally adding an invoice payment.
 *
 * For example, to pay an invoice:
 *
 *     SELECT * FROM solarbill.add_payment(
 *           accountid => 123
 *         , pay_amount => '2.34'::NUMERIC
 *         , pay_ref => 345::TEXT
 *         , pay_date => CURRENT_TIMESTAMP
 *     );
 *
 * @param accountid 	the account ID to add the payment to
 * @param pay_amount 	the payment amount
 * @param pay_ext_key	the optional payment external key
 * @param pay_ref		the optional invoice payment reference; if an invoice ID then apply
 * 						the payment to the given invoice
 * @param pay_type		the payment type; the payment type
 * @param pay_date		the payment date; defaults to current time
 */
CREATE OR REPLACE FUNCTION solarbill.add_payment(
		  accountid 	BIGINT
		, pay_amount 	NUMERIC(11,2)
		, pay_ext_key 	CHARACTER VARYING(64) DEFAULT NULL
		, pay_ref 		CHARACTER VARYING(64) DEFAULT NULL
		, pay_type 		SMALLINT DEFAULT 1
		, pay_date 		TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
	)
	RETURNS solarbill.bill_payment
	LANGUAGE plpgsql VOLATILE AS
$$
DECLARE
	invid	BIGINT := solarcommon.to_bigint(pay_ref);
	pay_rec solarbill.bill_payment;
BEGIN

	INSERT INTO solarbill.bill_payment (created,acct_id,pay_type,amount,currency,ext_key,ref)
	SELECT pay_date, a.id, pay_type, pay_amount, a.currency, pay_ext_key,
		CASE invid WHEN NULL THEN pay_ref ELSE NULL END AS ref
	FROM solarbill.bill_account a
	WHERE a.id = accountid
	RETURNING *
	INTO pay_rec;

	IF invid IS NOT NULL THEN
		WITH tot AS (
			SELECT SUM(ip.amount) AS total
			FROM solarbill.bill_invoice_payment ip WHERE ip.inv_id = invid
		)
		INSERT INTO solarbill.bill_invoice_payment (created,acct_id, pay_id, inv_id, amount)
		SELECT pay_date, pay_rec.acct_id, pay_rec.id, invid, LEAST(pay_amount, tot.total)
		FROM tot;
	END IF;

	RETURN pay_rec;
END
$$;

/**
 * View to show invoice details including account information, total amount, and paid amount.
 */
CREATE OR REPLACE VIEW solarbill.bill_invoice_info AS
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
		, itm.item_count
		, itm.total_amount
		, pay.paid_amount
	FROM solarbill.bill_invoice inv
	INNER JOIN solarbill.bill_account act ON act.id = inv.acct_id
	INNER JOIN solarbill.bill_address adr ON adr.id = inv.addr_id
	LEFT JOIN LATERAL (
		SELECT
			  COUNT(itm.id) AS item_count
			, SUM(itm.amount) AS total_amount
		FROM solarbill.bill_invoice_item itm
		WHERE itm.inv_id = inv.id
		)  itm ON TRUE
	LEFT JOIN LATERAL (
		SELECT SUM(pay.amount) AS paid_amount
		FROM solarbill.bill_invoice_payment pay
		WHERE pay.inv_id = inv.id
		) pay ON TRUE;

/**
 * View to show account details including address information and balance..
 */
CREATE OR REPLACE VIEW solarbill.bill_account_info AS
	SELECT
		  act.id
		, act.created
		, act.user_id
		, act.currency
		, act.locale
		, act.addr_id
		, adr.email
		, adr.disp_name
		, adr.country
		, adr.time_zone
		, bal.charge_total
		, bal.payment_total
		, bal.avail_credit
	FROM solarbill.bill_account act
	INNER JOIN solarbill.bill_address adr ON adr.id = act.addr_id
	LEFT OUTER JOIN solarbill.bill_account_balance bal ON bal.acct_id = act.id;

/**
 * Make a payment against a set of invoices.
 *
 * The payment is applied to invoices such that the full invoice amount is applied
 * going in oldest to newest invoice order, to up an overall maximum amount of the
 * payment amount.
 *
 * For example, to pay an invoice:
 *
 *     SELECT * FROM solarbill.add_invoice_payments(
 *           accountid => 123
 *         , pay_amount => '2.34'::NUMERIC
 *         , pay_date => CURRENT_TIMESTAMP
 *         , inv_ids => ARRAY[1,2,3]
 *     );
 *
 * @param accountid 	the account ID to add the payment to
 * @param pay_amount 	the payment amount
 * @param inv_ids		the invoice IDs to apply payments to
 * @param pay_ext_key	the optional payment external key
 * @param pay_ref		the optional invoice payment reference; if an invoice ID then apply
 * 						the payment to the given invoice
 * @param pay_type		the payment type; the payment type
 * @param pay_date		the payment date; defaults to current time
 */
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
