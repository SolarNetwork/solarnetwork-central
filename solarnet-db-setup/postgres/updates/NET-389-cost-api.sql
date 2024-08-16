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
	]);
$$;
