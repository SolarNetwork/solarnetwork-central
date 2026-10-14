--
-- PostgreSQL database dump
--

\restrict lWzuSTpqFHVsyJfAfQzlfbrwZp7p6rvWORyEcqOTiw7LEi22p6Cv0EIMtovvTaU

-- Dumped from database version 17.11 (Debian 17.11-0+deb13u1)
-- Dumped by pg_dump version 18.6

-- Started on 2026-10-14 16:12:25 NZDT

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- TOC entry 225 (class 1259 OID 137346)
-- Name: accounts; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.accounts (
    guid character varying(32) NOT NULL,
    name character varying(2048) NOT NULL,
    account_type character varying(2048) NOT NULL,
    commodity_guid character varying(32),
    commodity_scu integer NOT NULL,
    non_std_scu integer NOT NULL,
    parent_guid character varying(32),
    code character varying(2048),
    description character varying(2048),
    hidden integer,
    placeholder integer
);


--
-- TOC entry 238 (class 1259 OID 137422)
-- Name: billterms; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.billterms (
    guid character varying(32) NOT NULL,
    name character varying(2048) NOT NULL,
    description character varying(2048) NOT NULL,
    refcount integer NOT NULL,
    invisible integer NOT NULL,
    parent character varying(32),
    type character varying(2048) NOT NULL,
    duedays integer,
    discountdays integer,
    discount_num bigint,
    discount_denom bigint,
    cutoff integer
);


--
-- TOC entry 223 (class 1259 OID 137334)
-- Name: books; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.books (
    guid character varying(32) NOT NULL,
    root_account_guid character varying(32) NOT NULL,
    root_template_guid character varying(32) NOT NULL
);


--
-- TOC entry 228 (class 1259 OID 137361)
-- Name: budget_amounts; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.budget_amounts (
    id integer NOT NULL,
    budget_guid character varying(32) NOT NULL,
    account_guid character varying(32) NOT NULL,
    period_num integer NOT NULL,
    amount_num bigint NOT NULL,
    amount_denom bigint NOT NULL
);


--
-- TOC entry 227 (class 1259 OID 137360)
-- Name: budget_amounts_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.budget_amounts_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- TOC entry 4261 (class 0 OID 0)
-- Dependencies: 227
-- Name: budget_amounts_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.budget_amounts_id_seq OWNED BY public.budget_amounts.id;


--
-- TOC entry 226 (class 1259 OID 137353)
-- Name: budgets; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.budgets (
    guid character varying(32) NOT NULL,
    name character varying(2048) NOT NULL,
    description character varying(2048),
    num_periods integer NOT NULL
);


--
-- TOC entry 224 (class 1259 OID 137339)
-- Name: commodities; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.commodities (
    guid character varying(32) NOT NULL,
    namespace character varying(2048) NOT NULL,
    mnemonic character varying(2048) NOT NULL,
    fullname character varying(2048),
    cusip character varying(2048),
    fraction integer NOT NULL,
    quote_flag integer NOT NULL,
    quote_source character varying(2048),
    quote_tz character varying(2048)
);


--
-- TOC entry 239 (class 1259 OID 137429)
-- Name: customers; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.customers (
    guid character varying(32) NOT NULL,
    name character varying(2048) NOT NULL,
    id character varying(2048) NOT NULL,
    notes character varying(2048) NOT NULL,
    active integer NOT NULL,
    discount_num bigint NOT NULL,
    discount_denom bigint NOT NULL,
    credit_num bigint NOT NULL,
    credit_denom bigint NOT NULL,
    currency character varying(32) NOT NULL,
    tax_override integer NOT NULL,
    addr_name character varying(1024),
    addr_addr1 character varying(1024),
    addr_addr2 character varying(1024),
    addr_addr3 character varying(1024),
    addr_addr4 character varying(1024),
    addr_phone character varying(128),
    addr_fax character varying(128),
    addr_email character varying(256),
    shipaddr_name character varying(1024),
    shipaddr_addr1 character varying(1024),
    shipaddr_addr2 character varying(1024),
    shipaddr_addr3 character varying(1024),
    shipaddr_addr4 character varying(1024),
    shipaddr_phone character varying(128),
    shipaddr_fax character varying(128),
    shipaddr_email character varying(256),
    terms character varying(32),
    tax_included integer,
    taxtable character varying(32)
);


--
-- TOC entry 240 (class 1259 OID 137436)
-- Name: employees; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.employees (
    guid character varying(32) NOT NULL,
    username character varying(2048) NOT NULL,
    id character varying(2048) NOT NULL,
    language character varying(2048) NOT NULL,
    acl character varying(2048) NOT NULL,
    active integer NOT NULL,
    currency character varying(32) NOT NULL,
    ccard_guid character varying(32),
    workday_num bigint NOT NULL,
    workday_denom bigint NOT NULL,
    rate_num bigint NOT NULL,
    rate_denom bigint NOT NULL,
    addr_name character varying(1024),
    addr_addr1 character varying(1024),
    addr_addr2 character varying(1024),
    addr_addr3 character varying(1024),
    addr_addr4 character varying(1024),
    addr_phone character varying(128),
    addr_fax character varying(128),
    addr_email character varying(256)
);


--
-- TOC entry 241 (class 1259 OID 137443)
-- Name: entries; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.entries (
    guid character varying(32) NOT NULL,
    date timestamp without time zone NOT NULL,
    date_entered timestamp without time zone,
    description character varying(2048),
    action character varying(2048),
    notes character varying(2048),
    quantity_num bigint,
    quantity_denom bigint,
    i_acct character varying(32),
    i_price_num bigint,
    i_price_denom bigint,
    i_discount_num bigint,
    i_discount_denom bigint,
    invoice character varying(32),
    i_disc_type character varying(2048),
    i_disc_how character varying(2048),
    i_taxable integer,
    i_taxincluded integer,
    i_taxtable character varying(32),
    b_acct character varying(32),
    b_price_num bigint,
    b_price_denom bigint,
    bill character varying(32),
    b_taxable integer,
    b_taxincluded integer,
    b_taxtable character varying(32),
    b_paytype integer,
    billable integer,
    billto_type integer,
    billto_guid character varying(32),
    order_guid character varying(32)
);


--
-- TOC entry 221 (class 1259 OID 137326)
-- Name: gnclock; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.gnclock (
    hostname character varying(255),
    pid integer
);


--
-- TOC entry 242 (class 1259 OID 137450)
-- Name: invoices; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.invoices (
    guid character varying(32) NOT NULL,
    id character varying(2048) NOT NULL,
    date_opened timestamp without time zone,
    date_posted timestamp without time zone,
    notes character varying(2048) NOT NULL,
    active integer NOT NULL,
    currency character varying(32) NOT NULL,
    owner_type integer,
    owner_guid character varying(32),
    terms character varying(32),
    billing_id character varying(2048),
    post_txn character varying(32),
    post_lot character varying(32),
    post_acc character varying(32),
    billto_type integer,
    billto_guid character varying(32),
    charge_amt_num bigint,
    charge_amt_denom bigint
);


--
-- TOC entry 243 (class 1259 OID 137457)
-- Name: jobs; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.jobs (
    guid character varying(32) NOT NULL,
    id character varying(2048) NOT NULL,
    name character varying(2048) NOT NULL,
    reference character varying(2048) NOT NULL,
    active integer NOT NULL,
    owner_type integer,
    owner_guid character varying(32)
);


--
-- TOC entry 237 (class 1259 OID 137417)
-- Name: lots; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.lots (
    guid character varying(32) NOT NULL,
    account_guid character varying(32),
    is_closed integer NOT NULL
);


--
-- TOC entry 244 (class 1259 OID 137464)
-- Name: orders; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.orders (
    guid character varying(32) NOT NULL,
    id character varying(2048) NOT NULL,
    notes character varying(2048) NOT NULL,
    reference character varying(2048) NOT NULL,
    active integer NOT NULL,
    date_opened timestamp without time zone NOT NULL,
    date_closed timestamp without time zone NOT NULL,
    owner_type integer NOT NULL,
    owner_guid character varying(32) NOT NULL
);


--
-- TOC entry 229 (class 1259 OID 137367)
-- Name: prices; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.prices (
    guid character varying(32) NOT NULL,
    commodity_guid character varying(32) NOT NULL,
    currency_guid character varying(32) NOT NULL,
    date timestamp without time zone NOT NULL,
    source character varying(2048),
    type character varying(2048),
    value_num bigint NOT NULL,
    value_denom bigint NOT NULL
);


--
-- TOC entry 235 (class 1259 OID 137402)
-- Name: recurrences; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.recurrences (
    id integer NOT NULL,
    obj_guid character varying(32) NOT NULL,
    recurrence_mult integer NOT NULL,
    recurrence_period_type character varying(2048) NOT NULL,
    recurrence_period_start date NOT NULL,
    recurrence_weekend_adjust character varying(2048) NOT NULL
);


--
-- TOC entry 234 (class 1259 OID 137401)
-- Name: recurrences_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.recurrences_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- TOC entry 4262 (class 0 OID 0)
-- Dependencies: 234
-- Name: recurrences_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.recurrences_id_seq OWNED BY public.recurrences.id;


--
-- TOC entry 236 (class 1259 OID 137410)
-- Name: schedxactions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.schedxactions (
    guid character varying(32) NOT NULL,
    name character varying(2048),
    enabled integer NOT NULL,
    start_date date,
    end_date date,
    last_occur date,
    num_occur integer NOT NULL,
    rem_occur integer NOT NULL,
    auto_create integer NOT NULL,
    auto_notify integer NOT NULL,
    adv_creation integer NOT NULL,
    adv_notify integer NOT NULL,
    instance_count integer NOT NULL,
    template_act_guid character varying(32) NOT NULL
);


--
-- TOC entry 233 (class 1259 OID 137392)
-- Name: slots; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.slots (
    id integer NOT NULL,
    obj_guid character varying(32) NOT NULL,
    name character varying(4096) NOT NULL,
    slot_type integer NOT NULL,
    int64_val bigint,
    string_val character varying(4096),
    double_val double precision,
    timespec_val timestamp without time zone,
    guid_val character varying(32),
    numeric_val_num bigint,
    numeric_val_denom bigint,
    gdate_val date
);


--
-- TOC entry 232 (class 1259 OID 137391)
-- Name: slots_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.slots_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- TOC entry 4263 (class 0 OID 0)
-- Dependencies: 232
-- Name: slots_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.slots_id_seq OWNED BY public.slots.id;


--
-- TOC entry 231 (class 1259 OID 137382)
-- Name: splits; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.splits (
    guid character varying(32) NOT NULL,
    tx_guid character varying(32) NOT NULL,
    account_guid character varying(32) NOT NULL,
    memo character varying(2048) NOT NULL,
    action character varying(2048) NOT NULL,
    reconcile_state character varying(1) NOT NULL,
    reconcile_date timestamp without time zone,
    value_num bigint NOT NULL,
    value_denom bigint NOT NULL,
    quantity_num bigint NOT NULL,
    quantity_denom bigint NOT NULL,
    lot_guid character varying(32)
);


--
-- TOC entry 247 (class 1259 OID 137477)
-- Name: taxtable_entries; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.taxtable_entries (
    id integer NOT NULL,
    taxtable character varying(32) NOT NULL,
    account character varying(32) NOT NULL,
    amount_num bigint NOT NULL,
    amount_denom bigint NOT NULL,
    type integer NOT NULL
);


--
-- TOC entry 246 (class 1259 OID 137476)
-- Name: taxtable_entries_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.taxtable_entries_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- TOC entry 4264 (class 0 OID 0)
-- Dependencies: 246
-- Name: taxtable_entries_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.taxtable_entries_id_seq OWNED BY public.taxtable_entries.id;


--
-- TOC entry 245 (class 1259 OID 137471)
-- Name: taxtables; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.taxtables (
    guid character varying(32) NOT NULL,
    name character varying(50) NOT NULL,
    refcount bigint NOT NULL,
    invisible integer NOT NULL,
    parent character varying(32)
);


--
-- TOC entry 230 (class 1259 OID 137374)
-- Name: transactions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.transactions (
    guid character varying(32) NOT NULL,
    currency_guid character varying(32) NOT NULL,
    num character varying(2048) NOT NULL,
    post_date timestamp without time zone,
    enter_date timestamp without time zone,
    description character varying(2048)
);


--
-- TOC entry 248 (class 1259 OID 137483)
-- Name: vendors; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.vendors (
    guid character varying(32) NOT NULL,
    name character varying(2048) NOT NULL,
    id character varying(2048) NOT NULL,
    notes character varying(2048) NOT NULL,
    currency character varying(32) NOT NULL,
    active integer NOT NULL,
    tax_override integer NOT NULL,
    addr_name character varying(1024),
    addr_addr1 character varying(1024),
    addr_addr2 character varying(1024),
    addr_addr3 character varying(1024),
    addr_addr4 character varying(1024),
    addr_phone character varying(128),
    addr_fax character varying(128),
    addr_email character varying(256),
    terms character varying(32),
    tax_inc character varying(2048),
    tax_table character varying(32)
);


--
-- TOC entry 222 (class 1259 OID 137329)
-- Name: versions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.versions (
    table_name character varying(50) NOT NULL,
    table_version integer NOT NULL
);


--
-- TOC entry 4029 (class 2604 OID 137364)
-- Name: budget_amounts id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.budget_amounts ALTER COLUMN id SET DEFAULT nextval('public.budget_amounts_id_seq'::regclass);


--
-- TOC entry 4031 (class 2604 OID 137405)
-- Name: recurrences id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.recurrences ALTER COLUMN id SET DEFAULT nextval('public.recurrences_id_seq'::regclass);


--
-- TOC entry 4030 (class 2604 OID 137395)
-- Name: slots id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.slots ALTER COLUMN id SET DEFAULT nextval('public.slots_id_seq'::regclass);


--
-- TOC entry 4032 (class 2604 OID 137480)
-- Name: taxtable_entries id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.taxtable_entries ALTER COLUMN id SET DEFAULT nextval('public.taxtable_entries_id_seq'::regclass);


--
-- TOC entry 4232 (class 0 OID 137346)
-- Dependencies: 225
-- Data for Name: accounts; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.accounts (guid, name, account_type, commodity_guid, commodity_scu, non_std_scu, parent_guid, code, description, hidden, placeholder) FROM stdin;
884ec042d1fe494a83b1ef4c9172815a	Root Account	ROOT	\N	0	0	\N			0	0
\.


--
-- TOC entry 4245 (class 0 OID 137422)
-- Dependencies: 238
-- Data for Name: billterms; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.billterms (guid, name, description, refcount, invisible, parent, type, duedays, discountdays, discount_num, discount_denom, cutoff) FROM stdin;
\.


--
-- TOC entry 4230 (class 0 OID 137334)
-- Dependencies: 223
-- Data for Name: books; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.books (guid, root_account_guid, root_template_guid) FROM stdin;
aecd823df5c7421b904ee5ff33d2a5a1	884ec042d1fe494a83b1ef4c9172815a	69674b61f2d74beabc0d06a7d8b76208
\.


--
-- TOC entry 4235 (class 0 OID 137361)
-- Dependencies: 228
-- Data for Name: budget_amounts; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.budget_amounts (id, budget_guid, account_guid, period_num, amount_num, amount_denom) FROM stdin;
\.


--
-- TOC entry 4233 (class 0 OID 137353)
-- Dependencies: 226
-- Data for Name: budgets; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.budgets (guid, name, description, num_periods) FROM stdin;
\.


--
-- TOC entry 4231 (class 0 OID 137339)
-- Dependencies: 224
-- Data for Name: commodities; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.commodities (guid, namespace, mnemonic, fullname, cusip, fraction, quote_flag, quote_source, quote_tz) FROM stdin;
\.


--
-- TOC entry 4246 (class 0 OID 137429)
-- Dependencies: 239
-- Data for Name: customers; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.customers (guid, name, id, notes, active, discount_num, discount_denom, credit_num, credit_denom, currency, tax_override, addr_name, addr_addr1, addr_addr2, addr_addr3, addr_addr4, addr_phone, addr_fax, addr_email, shipaddr_name, shipaddr_addr1, shipaddr_addr2, shipaddr_addr3, shipaddr_addr4, shipaddr_phone, shipaddr_fax, shipaddr_email, terms, tax_included, taxtable) FROM stdin;
\.


--
-- TOC entry 4247 (class 0 OID 137436)
-- Dependencies: 240
-- Data for Name: employees; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.employees (guid, username, id, language, acl, active, currency, ccard_guid, workday_num, workday_denom, rate_num, rate_denom, addr_name, addr_addr1, addr_addr2, addr_addr3, addr_addr4, addr_phone, addr_fax, addr_email) FROM stdin;
\.


--
-- TOC entry 4248 (class 0 OID 137443)
-- Dependencies: 241
-- Data for Name: entries; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.entries (guid, date, date_entered, description, action, notes, quantity_num, quantity_denom, i_acct, i_price_num, i_price_denom, i_discount_num, i_discount_denom, invoice, i_disc_type, i_disc_how, i_taxable, i_taxincluded, i_taxtable, b_acct, b_price_num, b_price_denom, bill, b_taxable, b_taxincluded, b_taxtable, b_paytype, billable, billto_type, billto_guid, order_guid) FROM stdin;
\.


--
-- TOC entry 4228 (class 0 OID 137326)
-- Dependencies: 221
-- Data for Name: gnclock; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.gnclock (hostname, pid) FROM stdin;
\.


--
-- TOC entry 4249 (class 0 OID 137450)
-- Dependencies: 242
-- Data for Name: invoices; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.invoices (guid, id, date_opened, date_posted, notes, active, currency, owner_type, owner_guid, terms, billing_id, post_txn, post_lot, post_acc, billto_type, billto_guid, charge_amt_num, charge_amt_denom) FROM stdin;
\.


--
-- TOC entry 4250 (class 0 OID 137457)
-- Dependencies: 243
-- Data for Name: jobs; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.jobs (guid, id, name, reference, active, owner_type, owner_guid) FROM stdin;
\.


--
-- TOC entry 4244 (class 0 OID 137417)
-- Dependencies: 237
-- Data for Name: lots; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.lots (guid, account_guid, is_closed) FROM stdin;
\.


--
-- TOC entry 4251 (class 0 OID 137464)
-- Dependencies: 244
-- Data for Name: orders; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.orders (guid, id, notes, reference, active, date_opened, date_closed, owner_type, owner_guid) FROM stdin;
\.


--
-- TOC entry 4236 (class 0 OID 137367)
-- Dependencies: 229
-- Data for Name: prices; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.prices (guid, commodity_guid, currency_guid, date, source, type, value_num, value_denom) FROM stdin;
\.


--
-- TOC entry 4242 (class 0 OID 137402)
-- Dependencies: 235
-- Data for Name: recurrences; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.recurrences (id, obj_guid, recurrence_mult, recurrence_period_type, recurrence_period_start, recurrence_weekend_adjust) FROM stdin;
\.


--
-- TOC entry 4243 (class 0 OID 137410)
-- Dependencies: 236
-- Data for Name: schedxactions; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.schedxactions (guid, name, enabled, start_date, end_date, last_occur, num_occur, rem_occur, auto_create, auto_notify, adv_creation, adv_notify, instance_count, template_act_guid) FROM stdin;
\.


--
-- TOC entry 4240 (class 0 OID 137392)
-- Dependencies: 233
-- Data for Name: slots; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.slots (id, obj_guid, name, slot_type, int64_val, string_val, double_val, timespec_val, guid_val, numeric_val_num, numeric_val_denom, gdate_val) FROM stdin;
\.


--
-- TOC entry 4238 (class 0 OID 137382)
-- Dependencies: 231
-- Data for Name: splits; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.splits (guid, tx_guid, account_guid, memo, action, reconcile_state, reconcile_date, value_num, value_denom, quantity_num, quantity_denom, lot_guid) FROM stdin;
\.


--
-- TOC entry 4254 (class 0 OID 137477)
-- Dependencies: 247
-- Data for Name: taxtable_entries; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.taxtable_entries (id, taxtable, account, amount_num, amount_denom, type) FROM stdin;
\.


--
-- TOC entry 4252 (class 0 OID 137471)
-- Dependencies: 245
-- Data for Name: taxtables; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.taxtables (guid, name, refcount, invisible, parent) FROM stdin;
\.


--
-- TOC entry 4237 (class 0 OID 137374)
-- Dependencies: 230
-- Data for Name: transactions; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.transactions (guid, currency_guid, num, post_date, enter_date, description) FROM stdin;
\.


--
-- TOC entry 4255 (class 0 OID 137483)
-- Dependencies: 248
-- Data for Name: vendors; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.vendors (guid, name, id, notes, currency, active, tax_override, addr_name, addr_addr1, addr_addr2, addr_addr3, addr_addr4, addr_phone, addr_fax, addr_email, terms, tax_inc, tax_table) FROM stdin;
\.


--
-- TOC entry 4229 (class 0 OID 137329)
-- Dependencies: 222
-- Data for Name: versions; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.versions (table_name, table_version) FROM stdin;
Gnucash	5000010
Gnucash-Resave	19920
books	1
commodities	1
accounts	1
budgets	1
budget_amounts	1
prices	3
transactions	4
splits	5
slots	4
recurrences	2
schedxactions	1
lots	2
billterms	2
customers	2
employees	2
entries	4
invoices	4
jobs	1
orders	1
taxtables	2
taxtable_entries	3
vendors	1
\.


--
-- TOC entry 4265 (class 0 OID 0)
-- Dependencies: 227
-- Name: budget_amounts_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.budget_amounts_id_seq', 1, false);


--
-- TOC entry 4266 (class 0 OID 0)
-- Dependencies: 234
-- Name: recurrences_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.recurrences_id_seq', 1, false);


--
-- TOC entry 4267 (class 0 OID 0)
-- Dependencies: 232
-- Name: slots_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.slots_id_seq', 1433, true);


--
-- TOC entry 4268 (class 0 OID 0)
-- Dependencies: 246
-- Name: taxtable_entries_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.taxtable_entries_id_seq', 555, true);


--
-- TOC entry 4040 (class 2606 OID 137352)
-- Name: accounts accounts_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.accounts
    ADD CONSTRAINT accounts_pkey PRIMARY KEY (guid);


--
-- TOC entry 4064 (class 2606 OID 137428)
-- Name: billterms billterms_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.billterms
    ADD CONSTRAINT billterms_pkey PRIMARY KEY (guid);


--
-- TOC entry 4036 (class 2606 OID 137338)
-- Name: books books_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.books
    ADD CONSTRAINT books_pkey PRIMARY KEY (guid);


--
-- TOC entry 4044 (class 2606 OID 137366)
-- Name: budget_amounts budget_amounts_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.budget_amounts
    ADD CONSTRAINT budget_amounts_pkey PRIMARY KEY (id);


--
-- TOC entry 4042 (class 2606 OID 137359)
-- Name: budgets budgets_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.budgets
    ADD CONSTRAINT budgets_pkey PRIMARY KEY (guid);


--
-- TOC entry 4038 (class 2606 OID 137345)
-- Name: commodities commodities_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.commodities
    ADD CONSTRAINT commodities_pkey PRIMARY KEY (guid);


--
-- TOC entry 4066 (class 2606 OID 137435)
-- Name: customers customers_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.customers
    ADD CONSTRAINT customers_pkey PRIMARY KEY (guid);


--
-- TOC entry 4068 (class 2606 OID 137442)
-- Name: employees employees_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.employees
    ADD CONSTRAINT employees_pkey PRIMARY KEY (guid);


--
-- TOC entry 4070 (class 2606 OID 137449)
-- Name: entries entries_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.entries
    ADD CONSTRAINT entries_pkey PRIMARY KEY (guid);


--
-- TOC entry 4072 (class 2606 OID 137456)
-- Name: invoices invoices_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.invoices
    ADD CONSTRAINT invoices_pkey PRIMARY KEY (guid);


--
-- TOC entry 4074 (class 2606 OID 137463)
-- Name: jobs jobs_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.jobs
    ADD CONSTRAINT jobs_pkey PRIMARY KEY (guid);


--
-- TOC entry 4062 (class 2606 OID 137421)
-- Name: lots lots_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.lots
    ADD CONSTRAINT lots_pkey PRIMARY KEY (guid);


--
-- TOC entry 4076 (class 2606 OID 137470)
-- Name: orders orders_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.orders
    ADD CONSTRAINT orders_pkey PRIMARY KEY (guid);


--
-- TOC entry 4046 (class 2606 OID 137373)
-- Name: prices prices_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.prices
    ADD CONSTRAINT prices_pkey PRIMARY KEY (guid);


--
-- TOC entry 4058 (class 2606 OID 137409)
-- Name: recurrences recurrences_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.recurrences
    ADD CONSTRAINT recurrences_pkey PRIMARY KEY (id);


--
-- TOC entry 4060 (class 2606 OID 137416)
-- Name: schedxactions schedxactions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.schedxactions
    ADD CONSTRAINT schedxactions_pkey PRIMARY KEY (guid);


--
-- TOC entry 4056 (class 2606 OID 137399)
-- Name: slots slots_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.slots
    ADD CONSTRAINT slots_pkey PRIMARY KEY (id);


--
-- TOC entry 4052 (class 2606 OID 137388)
-- Name: splits splits_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.splits
    ADD CONSTRAINT splits_pkey PRIMARY KEY (guid);


--
-- TOC entry 4080 (class 2606 OID 137482)
-- Name: taxtable_entries taxtable_entries_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.taxtable_entries
    ADD CONSTRAINT taxtable_entries_pkey PRIMARY KEY (id);


--
-- TOC entry 4078 (class 2606 OID 137475)
-- Name: taxtables taxtables_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.taxtables
    ADD CONSTRAINT taxtables_pkey PRIMARY KEY (guid);


--
-- TOC entry 4048 (class 2606 OID 137380)
-- Name: transactions transactions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.transactions
    ADD CONSTRAINT transactions_pkey PRIMARY KEY (guid);


--
-- TOC entry 4082 (class 2606 OID 137489)
-- Name: vendors vendors_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vendors
    ADD CONSTRAINT vendors_pkey PRIMARY KEY (guid);


--
-- TOC entry 4034 (class 2606 OID 137333)
-- Name: versions versions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.versions
    ADD CONSTRAINT versions_pkey PRIMARY KEY (table_name);


--
-- TOC entry 4054 (class 1259 OID 137400)
-- Name: slots_guid_index; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX slots_guid_index ON public.slots USING btree (obj_guid);


--
-- TOC entry 4050 (class 1259 OID 137390)
-- Name: splits_account_guid_index; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX splits_account_guid_index ON public.splits USING btree (account_guid);


--
-- TOC entry 4053 (class 1259 OID 137389)
-- Name: splits_tx_guid_index; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX splits_tx_guid_index ON public.splits USING btree (tx_guid);


--
-- TOC entry 4049 (class 1259 OID 137381)
-- Name: tx_post_date_index; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX tx_post_date_index ON public.transactions USING btree (post_date);


-- Completed on 2026-10-14 16:12:26 NZDT

--
-- PostgreSQL database dump complete
--

\unrestrict lWzuSTpqFHVsyJfAfQzlfbrwZp7p6rvWORyEcqOTiw7LEi22p6Cv0EIMtovvTaU

