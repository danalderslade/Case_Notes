package com.casemanagement.notes.repository;

import com.casemanagement.notes.dto.CaseNoteDtos.CreateRequest;
import com.casemanagement.notes.dto.CaseNoteDtos.DeleteRequest;
import com.casemanagement.notes.dto.CaseNoteDtos.NoteResponse;
import com.casemanagement.notes.dto.CaseNoteDtos.PagedResponse;
import com.casemanagement.notes.dto.CaseNoteDtos.UpdateRequest;
import com.casemanagement.notes.exception.BadRequestException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

@Repository
public class CaseNoteRepository {

    private static final Pattern COUNTRY_CODE = Pattern.compile("^[a-zA-Z]{2}$");

    private final JdbcTemplate jdbcTemplate;

    public CaseNoteRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public NoteResponse create(String countryCode, CreateRequest request, String plainText) {
        String table = tableName(countryCode);
        String sql = """
                INSERT INTO %s (case_id, staff_id, country_code, note_content, note_plain_text)
                VALUES (?, ?, ?, ?, ?)
                RETURNING id, case_id, staff_id, country_code, note_content, note_plain_text,
                          is_deleted, created_at, updated_at, deleted_at, deleted_by, delete_reason, version
                """.formatted(table);

        return jdbcTemplate.queryForObject(sql, rowMapper(),
                request.getCaseId(),
                request.getStaffId(),
                request.getCountryCode().toUpperCase(Locale.ROOT),
                request.getNoteContent(),
                plainText);
    }

    public NoteResponse findById(String countryCode, UUID id, boolean includeDeleted) {
        String table = tableName(countryCode);
        String sql = """
                SELECT id, case_id, staff_id, country_code, note_content, note_plain_text,
                       is_deleted, created_at, updated_at, deleted_at, deleted_by, delete_reason, version
                FROM %s
                WHERE id = ?
                %s
                """.formatted(table, includeDeleted ? "" : "AND is_deleted = FALSE");

        List<NoteResponse> rows = jdbcTemplate.query(sql, rowMapper(), id);
        return rows.isEmpty() ? null : rows.getFirst();
    }

    public PagedResponse findByCase(String countryCode, String caseId, boolean includeDeleted, int page, int size) {
        String table = tableName(countryCode);
        String filter = includeDeleted ? "" : "AND is_deleted = FALSE";

        String countSql = "SELECT COUNT(*) FROM %s WHERE case_id = ? %s".formatted(table, filter);
        Long total = jdbcTemplate.queryForObject(countSql, Long.class, caseId);
        long totalElements = total == null ? 0L : total;

        String sql = """
                SELECT id, case_id, staff_id, country_code, note_content, note_plain_text,
                       is_deleted, created_at, updated_at, deleted_at, deleted_by, delete_reason, version
                FROM %s
                WHERE case_id = ?
                %s
                ORDER BY created_at DESC
                LIMIT ? OFFSET ?
                """.formatted(table, filter);

        List<NoteResponse> content = jdbcTemplate.query(sql, rowMapper(), caseId, size, page * size);

        int totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        boolean first = page == 0;
        boolean last = totalPages == 0 || page >= totalPages - 1;

        PagedResponse response = new PagedResponse();
        response.setContent(content);
        response.setPageNumber(page);
        response.setPageSize(size);
        response.setTotalElements(totalElements);
        response.setTotalPages(totalPages);
        response.setFirst(first);
        response.setLast(last);
        return response;
    }

    public List<NoteResponse> search(String countryCode, String query, boolean includeDeleted, int page, int size) {
        String table = tableName(countryCode);
        String filter = includeDeleted ? "" : "AND is_deleted = FALSE";

        String sql = """
                SELECT id, case_id, staff_id, country_code, note_content, note_plain_text,
                       is_deleted, created_at, updated_at, deleted_at, deleted_by, delete_reason, version
                FROM %s
                WHERE to_tsvector('english', COALESCE(note_plain_text, '')) @@ plainto_tsquery('english', ?)
                %s
                ORDER BY created_at DESC
                LIMIT ? OFFSET ?
                """.formatted(table, filter);

        return jdbcTemplate.query(sql, rowMapper(), query, size, page * size);
    }

    public NoteResponse update(String countryCode, UUID id, UpdateRequest request, String plainText) {
        String table = tableName(countryCode);
        String sql = """
                UPDATE %s
                SET note_content = ?, note_plain_text = ?
                WHERE id = ?
                  AND is_deleted = FALSE
                  AND version = ?
                RETURNING id, case_id, staff_id, country_code, note_content, note_plain_text,
                          is_deleted, created_at, updated_at, deleted_at, deleted_by, delete_reason, version
                """.formatted(table);

        List<NoteResponse> rows = jdbcTemplate.query(sql, rowMapper(),
                request.getNoteContent(), plainText, id, request.getVersion());

        return rows.isEmpty() ? null : rows.getFirst();
    }

    public NoteResponse softDelete(String countryCode, UUID id, DeleteRequest request) {
        String table = tableName(countryCode);
        String sql = """
                UPDATE %s
                SET is_deleted = TRUE,
                    deleted_at = NOW(),
                    deleted_by = ?,
                    delete_reason = ?
                WHERE id = ?
                  AND is_deleted = FALSE
                RETURNING id, case_id, staff_id, country_code, note_content, note_plain_text,
                          is_deleted, created_at, updated_at, deleted_at, deleted_by, delete_reason, version
                """.formatted(table);

        List<NoteResponse> rows = jdbcTemplate.query(sql, rowMapper(), request.getDeletedBy(), request.getDeleteReason(), id);
        return rows.isEmpty() ? null : rows.getFirst();
    }

    private String tableName(String countryCode) {
        if (countryCode == null || !COUNTRY_CODE.matcher(countryCode).matches()) {
            throw new BadRequestException("countryCode must be a 2-letter ISO code");
        }
        return "case_notes_" + countryCode.toLowerCase(Locale.ROOT);
    }

    private RowMapper<NoteResponse> rowMapper() {
        return (rs, rowNum) -> {
            NoteResponse response = new NoteResponse();
            response.setId(UUID.fromString(rs.getString("id")));
            response.setCaseId(rs.getString("case_id"));
            response.setStaffId(rs.getString("staff_id"));
            response.setCountryCode(rs.getString("country_code").trim());
            response.setNoteContent(rs.getString("note_content"));
            response.setNotePlainText(rs.getString("note_plain_text"));
            response.setDeleted(rs.getBoolean("is_deleted"));
            response.setCreatedAt(rs.getObject("created_at", OffsetDateTime.class));
            response.setUpdatedAt(rs.getObject("updated_at", OffsetDateTime.class));
            response.setDeletedAt(rs.getObject("deleted_at", OffsetDateTime.class));
            response.setDeletedBy(rs.getString("deleted_by"));
            response.setDeleteReason(rs.getString("delete_reason"));
            response.setVersion(rs.getInt("version"));
            return response;
        };
    }
}
