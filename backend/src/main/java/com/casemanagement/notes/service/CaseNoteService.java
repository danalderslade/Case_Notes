package com.casemanagement.notes.service;

import com.casemanagement.notes.dto.CaseNoteDtos.CountryResponse;
import com.casemanagement.notes.dto.CaseNoteDtos.CreateRequest;
import com.casemanagement.notes.dto.CaseNoteDtos.DeleteRequest;
import com.casemanagement.notes.dto.CaseNoteDtos.NoteResponse;
import com.casemanagement.notes.dto.CaseNoteDtos.PagedResponse;
import com.casemanagement.notes.dto.CaseNoteDtos.UpdateRequest;
import com.casemanagement.notes.exception.ConflictException;
import com.casemanagement.notes.exception.NotFoundException;
import com.casemanagement.notes.repository.CaseNoteRepository;
import com.casemanagement.notes.repository.CountryRepository;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class CaseNoteService {

    private final CaseNoteRepository caseNoteRepository;
    private final CountryRepository countryRepository;

    public CaseNoteService(CaseNoteRepository caseNoteRepository, CountryRepository countryRepository) {
        this.caseNoteRepository = caseNoteRepository;
        this.countryRepository = countryRepository;
    }

    public NoteResponse createNote(CreateRequest request) {
        String countryCode = normalizeCountry(request.getCountryCode());
        ensureCountryReady(countryCode);

        request.setCountryCode(countryCode);
        String plainText = extractPlainText(request.getNoteContent());
        return caseNoteRepository.create(countryCode, request, plainText);
    }

    public NoteResponse getNoteById(String countryCode, UUID id, boolean includeDeleted) {
        String normalized = normalizeCountry(countryCode);
        ensureCountryReady(normalized);
        NoteResponse note = caseNoteRepository.findById(normalized, id, includeDeleted);
        if (note == null) {
            throw new NotFoundException("Note not found: " + id);
        }
        return note;
    }

    public PagedResponse getNotesByCase(String countryCode, String caseId, boolean includeDeleted, int page, int size) {
        String normalized = normalizeCountry(countryCode);
        ensureCountryReady(normalized);
        return caseNoteRepository.findByCase(normalized, caseId, includeDeleted, page, size);
    }

    public List<NoteResponse> searchNotes(String countryCode, String query, boolean includeDeleted, int page, int size) {
        String normalized = normalizeCountry(countryCode);
        ensureCountryReady(normalized);
        return caseNoteRepository.search(normalized, query, includeDeleted, page, size);
    }

    public NoteResponse updateNote(String countryCode, UUID id, UpdateRequest request) {
        String normalized = normalizeCountry(countryCode);
        ensureCountryReady(normalized);
        String plainText = extractPlainText(request.getNoteContent());
        NoteResponse note = caseNoteRepository.update(normalized, id, request, plainText);
        if (note == null) {
            throw new ConflictException("Update failed. Note not found, deleted, or version mismatch.");
        }
        return note;
    }

    public NoteResponse softDeleteNote(String countryCode, UUID id, DeleteRequest request) {
        String normalized = normalizeCountry(countryCode);
        ensureCountryReady(normalized);
        NoteResponse note = caseNoteRepository.softDelete(normalized, id, request);
        if (note == null) {
            throw new NotFoundException("Note not found or already deleted: " + id);
        }
        return note;
    }

    public List<CountryResponse> listCountries() {
        return countryRepository.findActiveCountries();
    }

    public void provisionCountry(String countryCode) {
        String normalized = normalizeCountry(countryCode);
        ensureCountryExists(normalized);
        countryRepository.provisionCountryTable(normalized);
    }

    private void ensureCountryExists(String countryCode) {
        if (!countryRepository.exists(normalizeCountry(countryCode))) {
            throw new NotFoundException("Country is not active: " + countryCode);
        }
    }

    private void ensureCountryReady(String countryCode) {
        String normalized = normalizeCountry(countryCode);
        ensureCountryExists(normalized);
        countryRepository.provisionCountryTable(normalized);
    }

    private String normalizeCountry(String countryCode) {
        if (countryCode == null) {
            return null;
        }
        return countryCode.toUpperCase(Locale.ROOT);
    }

    private String extractPlainText(String html) {
        return Jsoup.parse(html == null ? "" : html).text();
    }
}
