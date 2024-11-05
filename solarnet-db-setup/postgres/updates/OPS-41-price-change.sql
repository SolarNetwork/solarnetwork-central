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
	]);
$$;

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
	ELSE
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
	END IF;
END
$$;
