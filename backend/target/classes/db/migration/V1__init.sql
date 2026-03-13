-- ============================================================
-- Case Management Notes Module - PostgreSQL Schema
-- Supports per-country tables using ISO 3166-1 alpha-2 codes
-- ============================================================

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ============================================================
-- ISO 3166-1 Country Reference Table
-- ============================================================
CREATE TABLE IF NOT EXISTS ref_countries (
    iso_alpha2   CHAR(2)      PRIMARY KEY,
    iso_alpha3   CHAR(3)      NOT NULL UNIQUE,
    iso_numeric  CHAR(3)      NOT NULL UNIQUE,
    country_name VARCHAR(100) NOT NULL,
    is_active    BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- Seed ISO 3166-1 countries (global)
INSERT INTO ref_countries (iso_alpha2, iso_alpha3, iso_numeric, country_name) VALUES
    ('AD', 'AND', '020', 'Principality of Andorra'),
    ('AE', 'ARE', '784', 'United Arab Emirates'),
    ('AF', 'AFG', '004', 'Islamic Republic of Afghanistan'),
    ('AG', 'ATG', '028', 'Antigua and Barbuda'),
    ('AI', 'AIA', '660', 'Anguilla'),
    ('AL', 'ALB', '008', 'Republic of Albania'),
    ('AM', 'ARM', '051', 'Republic of Armenia'),
    ('AO', 'AGO', '024', 'Republic of Angola'),
    ('AQ', 'ATA', '010', 'Antarctica'),
    ('AR', 'ARG', '032', 'Argentine Republic'),
    ('AS', 'ASM', '016', 'American Samoa'),
    ('AT', 'AUT', '040', 'Republic of Austria'),
    ('AU', 'AUS', '036', 'Australia'),
    ('AW', 'ABW', '533', 'Aruba'),
    ('AX', 'ALA', '248', 'Aland Islands'),
    ('AZ', 'AZE', '031', 'Republic of Azerbaijan'),
    ('BA', 'BIH', '070', 'Republic of Bosnia and Herzegovina'),
    ('BB', 'BRB', '052', 'Barbados'),
    ('BD', 'BGD', '050', 'People''s Republic of Bangladesh'),
    ('BE', 'BEL', '056', 'Kingdom of Belgium'),
    ('BF', 'BFA', '854', 'Burkina Faso'),
    ('BG', 'BGR', '100', 'Republic of Bulgaria'),
    ('BH', 'BHR', '048', 'Kingdom of Bahrain'),
    ('BI', 'BDI', '108', 'Republic of Burundi'),
    ('BJ', 'BEN', '204', 'Republic of Benin'),
    ('BL', 'BLM', '652', 'Saint Barthelemy'),
    ('BM', 'BMU', '060', 'Bermuda'),
    ('BN', 'BRN', '096', 'Brunei Darussalam'),
    ('BO', 'BOL', '068', 'Plurinational State of Bolivia'),
    ('BQ', 'BES', '535', 'Bonaire, Sint Eustatius and Saba'),
    ('BR', 'BRA', '076', 'Federative Republic of Brazil'),
    ('BS', 'BHS', '044', 'Commonwealth of the Bahamas'),
    ('BT', 'BTN', '064', 'Kingdom of Bhutan'),
    ('BV', 'BVT', '074', 'Bouvet Island'),
    ('BW', 'BWA', '072', 'Republic of Botswana'),
    ('BY', 'BLR', '112', 'Republic of Belarus'),
    ('BZ', 'BLZ', '084', 'Belize'),
    ('CA', 'CAN', '124', 'Canada'),
    ('CC', 'CCK', '166', 'Cocos (Keeling) Islands'),
    ('CD', 'COD', '180', 'Congo, The Democratic Republic of the'),
    ('CF', 'CAF', '140', 'Central African Republic'),
    ('CG', 'COG', '178', 'Republic of the Congo'),
    ('CH', 'CHE', '756', 'Swiss Confederation'),
    ('CI', 'CIV', '384', 'Republic of Cote d''Ivoire'),
    ('CK', 'COK', '184', 'Cook Islands'),
    ('CL', 'CHL', '152', 'Republic of Chile'),
    ('CM', 'CMR', '120', 'Republic of Cameroon'),
    ('CN', 'CHN', '156', 'People''s Republic of China'),
    ('CO', 'COL', '170', 'Republic of Colombia'),
    ('CR', 'CRI', '188', 'Republic of Costa Rica'),
    ('CU', 'CUB', '192', 'Republic of Cuba'),
    ('CV', 'CPV', '132', 'Republic of Cabo Verde'),
    ('CW', 'CUW', '531', 'Curacao'),
    ('CX', 'CXR', '162', 'Christmas Island'),
    ('CY', 'CYP', '196', 'Republic of Cyprus'),
    ('CZ', 'CZE', '203', 'Czech Republic'),
    ('DE', 'DEU', '276', 'Federal Republic of Germany'),
    ('DJ', 'DJI', '262', 'Republic of Djibouti'),
    ('DK', 'DNK', '208', 'Kingdom of Denmark'),
    ('DM', 'DMA', '212', 'Commonwealth of Dominica'),
    ('DO', 'DOM', '214', 'Dominican Republic'),
    ('DZ', 'DZA', '012', 'People''s Democratic Republic of Algeria'),
    ('EC', 'ECU', '218', 'Republic of Ecuador'),
    ('EE', 'EST', '233', 'Republic of Estonia'),
    ('EG', 'EGY', '818', 'Arab Republic of Egypt'),
    ('EH', 'ESH', '732', 'Western Sahara'),
    ('ER', 'ERI', '232', 'the State of Eritrea'),
    ('ES', 'ESP', '724', 'Kingdom of Spain'),
    ('ET', 'ETH', '231', 'Federal Democratic Republic of Ethiopia'),
    ('FI', 'FIN', '246', 'Republic of Finland'),
    ('FJ', 'FJI', '242', 'Republic of Fiji'),
    ('FK', 'FLK', '238', 'Falkland Islands (Malvinas)'),
    ('FM', 'FSM', '583', 'Federated States of Micronesia'),
    ('FO', 'FRO', '234', 'Faroe Islands'),
    ('FR', 'FRA', '250', 'French Republic'),
    ('GA', 'GAB', '266', 'Gabonese Republic'),
    ('GB', 'GBR', '826', 'United Kingdom of Great Britain and Northern Ireland'),
    ('GD', 'GRD', '308', 'Grenada'),
    ('GE', 'GEO', '268', 'Georgia'),
    ('GF', 'GUF', '254', 'French Guiana'),
    ('GG', 'GGY', '831', 'Guernsey'),
    ('GH', 'GHA', '288', 'Republic of Ghana'),
    ('GI', 'GIB', '292', 'Gibraltar'),
    ('GL', 'GRL', '304', 'Greenland'),
    ('GM', 'GMB', '270', 'Republic of the Gambia'),
    ('GN', 'GIN', '324', 'Republic of Guinea'),
    ('GP', 'GLP', '312', 'Guadeloupe'),
    ('GQ', 'GNQ', '226', 'Republic of Equatorial Guinea'),
    ('GR', 'GRC', '300', 'Hellenic Republic'),
    ('GS', 'SGS', '239', 'South Georgia and the South Sandwich Islands'),
    ('GT', 'GTM', '320', 'Republic of Guatemala'),
    ('GU', 'GUM', '316', 'Guam'),
    ('GW', 'GNB', '624', 'Republic of Guinea-Bissau'),
    ('GY', 'GUY', '328', 'Republic of Guyana'),
    ('HK', 'HKG', '344', 'Hong Kong Special Administrative Region of China'),
    ('HM', 'HMD', '334', 'Heard Island and McDonald Islands'),
    ('HN', 'HND', '340', 'Republic of Honduras'),
    ('HR', 'HRV', '191', 'Republic of Croatia'),
    ('HT', 'HTI', '332', 'Republic of Haiti'),
    ('HU', 'HUN', '348', 'Hungary'),
    ('ID', 'IDN', '360', 'Republic of Indonesia'),
    ('IE', 'IRL', '372', 'Ireland'),
    ('IL', 'ISR', '376', 'State of Israel'),
    ('IM', 'IMN', '833', 'Isle of Man'),
    ('IN', 'IND', '356', 'Republic of India'),
    ('IO', 'IOT', '086', 'British Indian Ocean Territory'),
    ('IQ', 'IRQ', '368', 'Republic of Iraq'),
    ('IR', 'IRN', '364', 'Islamic Republic of Iran'),
    ('IS', 'ISL', '352', 'Republic of Iceland'),
    ('IT', 'ITA', '380', 'Italian Republic'),
    ('JE', 'JEY', '832', 'Jersey'),
    ('JM', 'JAM', '388', 'Jamaica'),
    ('JO', 'JOR', '400', 'Hashemite Kingdom of Jordan'),
    ('JP', 'JPN', '392', 'Japan'),
    ('KE', 'KEN', '404', 'Republic of Kenya'),
    ('KG', 'KGZ', '417', 'Kyrgyz Republic'),
    ('KH', 'KHM', '116', 'Kingdom of Cambodia'),
    ('KI', 'KIR', '296', 'Republic of Kiribati'),
    ('KM', 'COM', '174', 'Union of the Comoros'),
    ('KN', 'KNA', '659', 'Saint Kitts and Nevis'),
    ('KP', 'PRK', '408', 'Democratic People''s Republic of Korea'),
    ('KR', 'KOR', '410', 'Korea, Republic of'),
    ('KW', 'KWT', '414', 'State of Kuwait'),
    ('KY', 'CYM', '136', 'Cayman Islands'),
    ('KZ', 'KAZ', '398', 'Republic of Kazakhstan'),
    ('LA', 'LAO', '418', 'Lao People''s Democratic Republic'),
    ('LB', 'LBN', '422', 'Lebanese Republic'),
    ('LC', 'LCA', '662', 'Saint Lucia'),
    ('LI', 'LIE', '438', 'Principality of Liechtenstein'),
    ('LK', 'LKA', '144', 'Democratic Socialist Republic of Sri Lanka'),
    ('LR', 'LBR', '430', 'Republic of Liberia'),
    ('LS', 'LSO', '426', 'Kingdom of Lesotho'),
    ('LT', 'LTU', '440', 'Republic of Lithuania'),
    ('LU', 'LUX', '442', 'Grand Duchy of Luxembourg'),
    ('LV', 'LVA', '428', 'Republic of Latvia'),
    ('LY', 'LBY', '434', 'Libya'),
    ('MA', 'MAR', '504', 'Kingdom of Morocco'),
    ('MC', 'MCO', '492', 'Principality of Monaco'),
    ('MD', 'MDA', '498', 'Republic of Moldova'),
    ('ME', 'MNE', '499', 'Montenegro'),
    ('MF', 'MAF', '663', 'Saint Martin (French part)'),
    ('MG', 'MDG', '450', 'Republic of Madagascar'),
    ('MH', 'MHL', '584', 'Republic of the Marshall Islands'),
    ('MK', 'MKD', '807', 'Republic of North Macedonia'),
    ('ML', 'MLI', '466', 'Republic of Mali'),
    ('MM', 'MMR', '104', 'Republic of Myanmar'),
    ('MN', 'MNG', '496', 'Mongolia'),
    ('MO', 'MAC', '446', 'Macao Special Administrative Region of China'),
    ('MP', 'MNP', '580', 'Commonwealth of the Northern Mariana Islands'),
    ('MQ', 'MTQ', '474', 'Martinique'),
    ('MR', 'MRT', '478', 'Islamic Republic of Mauritania'),
    ('MS', 'MSR', '500', 'Montserrat'),
    ('MT', 'MLT', '470', 'Republic of Malta'),
    ('MU', 'MUS', '480', 'Republic of Mauritius'),
    ('MV', 'MDV', '462', 'Republic of Maldives'),
    ('MW', 'MWI', '454', 'Republic of Malawi'),
    ('MX', 'MEX', '484', 'United Mexican States'),
    ('MY', 'MYS', '458', 'Malaysia'),
    ('MZ', 'MOZ', '508', 'Republic of Mozambique'),
    ('NA', 'NAM', '516', 'Republic of Namibia'),
    ('NC', 'NCL', '540', 'New Caledonia'),
    ('NE', 'NER', '562', 'Republic of the Niger'),
    ('NF', 'NFK', '574', 'Norfolk Island'),
    ('NG', 'NGA', '566', 'Federal Republic of Nigeria'),
    ('NI', 'NIC', '558', 'Republic of Nicaragua'),
    ('NL', 'NLD', '528', 'Kingdom of the Netherlands'),
    ('NO', 'NOR', '578', 'Kingdom of Norway'),
    ('NP', 'NPL', '524', 'Federal Democratic Republic of Nepal'),
    ('NR', 'NRU', '520', 'Republic of Nauru'),
    ('NU', 'NIU', '570', 'Niue'),
    ('NZ', 'NZL', '554', 'New Zealand'),
    ('OM', 'OMN', '512', 'Sultanate of Oman'),
    ('PA', 'PAN', '591', 'Republic of Panama'),
    ('PE', 'PER', '604', 'Republic of Peru'),
    ('PF', 'PYF', '258', 'French Polynesia'),
    ('PG', 'PNG', '598', 'Independent State of Papua New Guinea'),
    ('PH', 'PHL', '608', 'Republic of the Philippines'),
    ('PK', 'PAK', '586', 'Islamic Republic of Pakistan'),
    ('PL', 'POL', '616', 'Republic of Poland'),
    ('PM', 'SPM', '666', 'Saint Pierre and Miquelon'),
    ('PN', 'PCN', '612', 'Pitcairn'),
    ('PR', 'PRI', '630', 'Puerto Rico'),
    ('PS', 'PSE', '275', 'the State of Palestine'),
    ('PT', 'PRT', '620', 'Portuguese Republic'),
    ('PW', 'PLW', '585', 'Republic of Palau'),
    ('PY', 'PRY', '600', 'Republic of Paraguay'),
    ('QA', 'QAT', '634', 'State of Qatar'),
    ('RE', 'REU', '638', 'Reunion'),
    ('RO', 'ROU', '642', 'Romania'),
    ('RS', 'SRB', '688', 'Republic of Serbia'),
    ('RU', 'RUS', '643', 'Russian Federation'),
    ('RW', 'RWA', '646', 'Rwandese Republic'),
    ('SA', 'SAU', '682', 'Kingdom of Saudi Arabia'),
    ('SB', 'SLB', '090', 'Solomon Islands'),
    ('SC', 'SYC', '690', 'Republic of Seychelles'),
    ('SD', 'SDN', '729', 'Republic of the Sudan'),
    ('SE', 'SWE', '752', 'Kingdom of Sweden'),
    ('SG', 'SGP', '702', 'Republic of Singapore'),
    ('SH', 'SHN', '654', 'Saint Helena, Ascension and Tristan da Cunha'),
    ('SI', 'SVN', '705', 'Republic of Slovenia'),
    ('SJ', 'SJM', '744', 'Svalbard and Jan Mayen'),
    ('SK', 'SVK', '703', 'Slovak Republic'),
    ('SL', 'SLE', '694', 'Republic of Sierra Leone'),
    ('SM', 'SMR', '674', 'Republic of San Marino'),
    ('SN', 'SEN', '686', 'Republic of Senegal'),
    ('SO', 'SOM', '706', 'Federal Republic of Somalia'),
    ('SR', 'SUR', '740', 'Republic of Suriname'),
    ('SS', 'SSD', '728', 'Republic of South Sudan'),
    ('ST', 'STP', '678', 'Democratic Republic of Sao Tome and Principe'),
    ('SV', 'SLV', '222', 'Republic of El Salvador'),
    ('SX', 'SXM', '534', 'Sint Maarten (Dutch part)'),
    ('SY', 'SYR', '760', 'Syrian Arab Republic'),
    ('SZ', 'SWZ', '748', 'Kingdom of Eswatini'),
    ('TC', 'TCA', '796', 'Turks and Caicos Islands'),
    ('TD', 'TCD', '148', 'Republic of Chad'),
    ('TF', 'ATF', '260', 'French Southern Territories'),
    ('TG', 'TGO', '768', 'Togolese Republic'),
    ('TH', 'THA', '764', 'Kingdom of Thailand'),
    ('TJ', 'TJK', '762', 'Republic of Tajikistan'),
    ('TK', 'TKL', '772', 'Tokelau'),
    ('TL', 'TLS', '626', 'Democratic Republic of Timor-Leste'),
    ('TM', 'TKM', '795', 'Turkmenistan'),
    ('TN', 'TUN', '788', 'Republic of Tunisia'),
    ('TO', 'TON', '776', 'Kingdom of Tonga'),
    ('TR', 'TUR', '792', 'Republic of Turkiye'),
    ('TT', 'TTO', '780', 'Republic of Trinidad and Tobago'),
    ('TV', 'TUV', '798', 'Tuvalu'),
    ('TW', 'TWN', '158', 'Taiwan, Province of China'),
    ('TZ', 'TZA', '834', 'United Republic of Tanzania'),
    ('UA', 'UKR', '804', 'Ukraine'),
    ('UG', 'UGA', '800', 'Republic of Uganda'),
    ('UM', 'UMI', '581', 'United States Minor Outlying Islands'),
    ('US', 'USA', '840', 'United States of America'),
    ('UY', 'URY', '858', 'Eastern Republic of Uruguay'),
    ('UZ', 'UZB', '860', 'Republic of Uzbekistan'),
    ('VA', 'VAT', '336', 'Holy See (Vatican City State)'),
    ('VC', 'VCT', '670', 'Saint Vincent and the Grenadines'),
    ('VE', 'VEN', '862', 'Bolivarian Republic of Venezuela'),
    ('VG', 'VGB', '092', 'British Virgin Islands'),
    ('VI', 'VIR', '850', 'Virgin Islands of the United States'),
    ('VN', 'VNM', '704', 'Socialist Republic of Viet Nam'),
    ('VU', 'VUT', '548', 'Republic of Vanuatu'),
    ('WF', 'WLF', '876', 'Wallis and Futuna'),
    ('WS', 'WSM', '882', 'Independent State of Samoa'),
    ('YE', 'YEM', '887', 'Republic of Yemen'),
    ('YT', 'MYT', '175', 'Mayotte'),
    ('ZA', 'ZAF', '710', 'Republic of South Africa'),
    ('ZM', 'ZMB', '894', 'Republic of Zambia'),
    ('ZW', 'ZWE', '716', 'Republic of Zimbabwe')
ON CONFLICT (iso_alpha2) DO NOTHING;

-- ============================================================
-- Template function: Creates a country-partitioned notes table
-- Usage: SELECT create_country_notes_table('GB');
-- ============================================================
CREATE OR REPLACE FUNCTION create_country_notes_table(p_country_code CHAR(2))
RETURNS VOID AS $$
DECLARE
    v_table_name TEXT;
    v_seq_name   TEXT;
    v_idx_prefix TEXT;
BEGIN
    -- Validate country code exists
    IF NOT EXISTS (SELECT 1 FROM ref_countries WHERE iso_alpha2 = UPPER(p_country_code)) THEN
        RAISE EXCEPTION 'Country code % not found in ref_countries', p_country_code;
    END IF;

    v_table_name := 'case_notes_' || LOWER(p_country_code);
    v_idx_prefix := 'idx_' || LOWER(p_country_code) || '_notes';

    -- Create the notes table for this country
    EXECUTE format('
        CREATE TABLE IF NOT EXISTS %I (
            id              UUID         NOT NULL DEFAULT uuid_generate_v4(),
            case_id         VARCHAR(100) NOT NULL,
            staff_id        VARCHAR(100) NOT NULL,
            country_code    CHAR(2)      NOT NULL DEFAULT %L,
            note_content    TEXT         NOT NULL,  -- Stored as HTML/rich-text (TipTap output)
            note_plain_text TEXT,                    -- Stripped plain-text for search
            is_deleted      BOOLEAN      NOT NULL DEFAULT FALSE,
            deleted_at      TIMESTAMPTZ,
            deleted_by      VARCHAR(100),
            delete_reason   VARCHAR(500),
            created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
            updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
            version         INTEGER      NOT NULL DEFAULT 1,  -- Optimistic locking
            CONSTRAINT %I PRIMARY KEY (id),
            CONSTRAINT %I CHECK (country_code = %L),
            CONSTRAINT %I FOREIGN KEY (country_code) REFERENCES ref_countries(iso_alpha2)
        )',
        v_table_name,
        UPPER(p_country_code),
        v_table_name || '_pkey',
        v_table_name || '_country_chk',
        UPPER(p_country_code),
        v_table_name || '_country_fk'
    );

    -- Indexes
    EXECUTE format('CREATE INDEX IF NOT EXISTS %I ON %I (case_id) WHERE is_deleted = FALSE',
        v_idx_prefix || '_case_id', v_table_name);
    EXECUTE format('CREATE INDEX IF NOT EXISTS %I ON %I (staff_id) WHERE is_deleted = FALSE',
        v_idx_prefix || '_staff_id', v_table_name);
    EXECUTE format('CREATE INDEX IF NOT EXISTS %I ON %I (created_at DESC)',
        v_idx_prefix || '_created_at', v_table_name);
    EXECUTE format('CREATE INDEX IF NOT EXISTS %I ON %I (is_deleted)',
        v_idx_prefix || '_is_deleted', v_table_name);
    EXECUTE format('CREATE INDEX IF NOT EXISTS %I ON %I USING GIN (to_tsvector(''english'', COALESCE(note_plain_text, '''' )))',
        v_idx_prefix || '_fts', v_table_name);

    -- Auto-update updated_at trigger
    EXECUTE format('
        CREATE OR REPLACE TRIGGER trg_%s_updated_at
        BEFORE UPDATE ON %I
        FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at()',
        LOWER(p_country_code) || '_notes',
        v_table_name
    );

    RAISE NOTICE 'Created notes table: %', v_table_name;
END;
$$ LANGUAGE plpgsql;

-- ============================================================
-- Shared trigger function for updated_at
-- ============================================================
CREATE OR REPLACE FUNCTION fn_set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    NEW.version    = OLD.version + 1;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- ============================================================
-- Audit log table (shared across all countries)
-- ============================================================
CREATE TABLE IF NOT EXISTS case_notes_audit (
    audit_id      UUID        NOT NULL DEFAULT uuid_generate_v4() PRIMARY KEY,
    country_code  CHAR(2)     NOT NULL REFERENCES ref_countries(iso_alpha2),
    note_id       UUID        NOT NULL,
    case_id       VARCHAR(100) NOT NULL,
    staff_id      VARCHAR(100) NOT NULL,
    action        VARCHAR(20) NOT NULL CHECK (action IN ('CREATE', 'UPDATE', 'SOFT_DELETE', 'RESTORE')),
    changed_by    VARCHAR(100) NOT NULL,
    changed_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    old_values    JSONB,
    new_values    JSONB,
    ip_address    INET,
    user_agent    TEXT
);

CREATE INDEX IF NOT EXISTS idx_audit_note_id     ON case_notes_audit (note_id);
CREATE INDEX IF NOT EXISTS idx_audit_case_id     ON case_notes_audit (case_id);
CREATE INDEX IF NOT EXISTS idx_audit_changed_at  ON case_notes_audit (changed_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_country     ON case_notes_audit (country_code);

-- ============================================================
-- Create default tables for seeded countries
-- ============================================================
SELECT create_country_notes_table('GB');
SELECT create_country_notes_table('US');
SELECT create_country_notes_table('AU');
SELECT create_country_notes_table('CA');
SELECT create_country_notes_table('IE');
