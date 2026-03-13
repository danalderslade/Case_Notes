package com.casemanagement.notes.repository;

import com.casemanagement.notes.dto.CaseNoteDtos.CountryResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Locale;

@Repository
public class CountryRepository {

    private final JdbcTemplate jdbcTemplate;

    public CountryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean exists(String countryCode) {
        String sql = "SELECT EXISTS (SELECT 1 FROM ref_countries WHERE iso_alpha2 = ? AND is_active = TRUE)";
        Boolean exists = jdbcTemplate.queryForObject(sql, Boolean.class, countryCode.toUpperCase(Locale.ROOT));
        return Boolean.TRUE.equals(exists);
    }

    public List<CountryResponse> findActiveCountries() {
        String sql = "SELECT iso_alpha2, country_name FROM ref_countries WHERE is_active = TRUE ORDER BY country_name";
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            CountryResponse response = new CountryResponse();
            response.setIsoAlpha2(rs.getString("iso_alpha2").trim());
            response.setCountryName(rs.getString("country_name").trim());
            return response;
        });
    }

    public void provisionCountryTable(String countryCode) {
        jdbcTemplate.queryForObject("SELECT create_country_notes_table(?)", Object.class, countryCode.toUpperCase(Locale.ROOT));
    }
}
