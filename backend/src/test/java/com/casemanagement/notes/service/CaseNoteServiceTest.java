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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CaseNoteServiceTest {

    @Mock
    private CaseNoteRepository caseNoteRepository;

    @Mock
    private CountryRepository countryRepository;

    private CaseNoteService service;

    @BeforeEach
    void setUp() {
        service = new CaseNoteService(caseNoteRepository, countryRepository);
    }

    @Test
    void createNoteNormalizesCountryAndExtractsPlainText() {
        CreateRequest request = new CreateRequest();
        request.setCaseId("CASE-123");
        request.setStaffId("STAFF-1");
        request.setCountryCode("gb");
        request.setNoteContent("<p>Hello <strong>world</strong></p>");

        NoteResponse expected = noteResponse();

        when(countryRepository.exists("GB")).thenReturn(true);
        when(caseNoteRepository.create(eq("GB"), any(CreateRequest.class), eq("Hello world"))).thenReturn(expected);

        NoteResponse actual = service.createNote(request);

        assertSame(expected, actual);
        assertEquals("GB", request.getCountryCode());
        verify(countryRepository).exists("GB");
        verify(countryRepository).provisionCountryTable("GB");
        verify(caseNoteRepository).create("GB", request, "Hello world");
    }

    @Test
    void getNoteByIdReturnsRepositoryResult() {
        UUID noteId = UUID.randomUUID();
        NoteResponse expected = noteResponse();

        when(countryRepository.exists("GB")).thenReturn(true);
        when(caseNoteRepository.findById("GB", noteId, true)).thenReturn(expected);

        NoteResponse actual = service.getNoteById("gb", noteId, true);

        assertSame(expected, actual);
        verify(countryRepository).exists("GB");
        verify(countryRepository).provisionCountryTable("GB");
        verify(caseNoteRepository).findById("GB", noteId, true);
    }

    @Test
    void getNoteByIdThrowsWhenNotFound() {
        UUID noteId = UUID.randomUUID();

        when(countryRepository.exists("GB")).thenReturn(true);
        when(caseNoteRepository.findById("GB", noteId, false)).thenReturn(null);

        NotFoundException error = assertThrows(NotFoundException.class,
                () -> service.getNoteById("gb", noteId, false));

        assertEquals("Note not found: " + noteId, error.getMessage());
    }

    @Test
    void getNotesByCaseReturnsPagedResponse() {
        PagedResponse expected = new PagedResponse();

        when(countryRepository.exists("GB")).thenReturn(true);
        when(caseNoteRepository.findByCase("GB", "CASE-123", false, 2, 10)).thenReturn(expected);

        PagedResponse actual = service.getNotesByCase("gb", "CASE-123", false, 2, 10);

        assertSame(expected, actual);
        verify(caseNoteRepository).findByCase("GB", "CASE-123", false, 2, 10);
    }

    @Test
    void searchNotesReturnsRepositoryResults() {
        List<NoteResponse> expected = List.of(noteResponse());

        when(countryRepository.exists("GB")).thenReturn(true);
        when(caseNoteRepository.search("GB", "housing", false, 0, 20)).thenReturn(expected);

        List<NoteResponse> actual = service.searchNotes("gb", "housing", false, 0, 20);

        assertSame(expected, actual);
        verify(caseNoteRepository).search("GB", "housing", false, 0, 20);
    }

    @Test
    void updateNoteSendsPlainTextAndReturnsUpdatedNote() {
        UUID noteId = UUID.randomUUID();
        UpdateRequest request = new UpdateRequest();
        request.setNoteContent("<div>Updated <em>note</em></div>");
        request.setVersion(3);

        NoteResponse expected = noteResponse();
        ArgumentCaptor<String> plainTextCaptor = ArgumentCaptor.forClass(String.class);

        when(countryRepository.exists("GB")).thenReturn(true);
        when(caseNoteRepository.update(eq("GB"), eq(noteId), eq(request), plainTextCaptor.capture()))
                .thenReturn(expected);

        NoteResponse actual = service.updateNote("gb", noteId, request);

        assertSame(expected, actual);
        assertEquals("Updated note", plainTextCaptor.getValue());
    }

    @Test
    void updateNoteThrowsConflictWhenRepositoryReturnsNull() {
        UUID noteId = UUID.randomUUID();
        UpdateRequest request = new UpdateRequest();
        request.setNoteContent("<p>Updated</p>");
        request.setVersion(2);

        when(countryRepository.exists("GB")).thenReturn(true);
        when(caseNoteRepository.update(eq("GB"), eq(noteId), eq(request), eq("Updated"))).thenReturn(null);

        ConflictException error = assertThrows(ConflictException.class,
                () -> service.updateNote("gb", noteId, request));

        assertEquals("Update failed. Note not found, deleted, or version mismatch.", error.getMessage());
    }

    @Test
    void softDeleteReturnsDeletedNote() {
        UUID noteId = UUID.randomUUID();
        DeleteRequest request = new DeleteRequest();
        request.setDeletedBy("STAFF-2");
        request.setDeleteReason("Duplicate entry");

        NoteResponse expected = noteResponse();

        when(countryRepository.exists("GB")).thenReturn(true);
        when(caseNoteRepository.softDelete("GB", noteId, request)).thenReturn(expected);

        NoteResponse actual = service.softDeleteNote("gb", noteId, request);

        assertSame(expected, actual);
        verify(caseNoteRepository).softDelete("GB", noteId, request);
    }

    @Test
    void softDeleteThrowsWhenNoteMissing() {
        UUID noteId = UUID.randomUUID();
        DeleteRequest request = new DeleteRequest();
        request.setDeletedBy("STAFF-2");

        when(countryRepository.exists("GB")).thenReturn(true);
        when(caseNoteRepository.softDelete("GB", noteId, request)).thenReturn(null);

        NotFoundException error = assertThrows(NotFoundException.class,
                () -> service.softDeleteNote("gb", noteId, request));

        assertEquals("Note not found or already deleted: " + noteId, error.getMessage());
    }

    @Test
    void listCountriesReturnsActiveCountries() {
        CountryResponse country = new CountryResponse();
        country.setIsoAlpha2("GB");
        country.setCountryName("United Kingdom");
        List<CountryResponse> expected = List.of(country);

        when(countryRepository.findActiveCountries()).thenReturn(expected);

        List<CountryResponse> actual = service.listCountries();

        assertSame(expected, actual);
        verify(countryRepository).findActiveCountries();
    }

    @Test
    void provisionCountryNormalizesCodeAndProvisionsTable() {
        when(countryRepository.exists("GB")).thenReturn(true);

        service.provisionCountry("gb");

        verify(countryRepository).exists("GB");
        verify(countryRepository).provisionCountryTable("GB");
    }

    @Test
    void provisionCountryThrowsWhenCountryInactive() {
        when(countryRepository.exists("GB")).thenReturn(false);

        NotFoundException error = assertThrows(NotFoundException.class,
                () -> service.provisionCountry("gb"));

        assertEquals("Country is not active: GB", error.getMessage());
        verify(countryRepository, never()).provisionCountryTable(any());
    }

    @Test
    void createNoteThrowsWhenCountryInactive() {
        CreateRequest request = new CreateRequest();
        request.setCountryCode("gb");

        when(countryRepository.exists("GB")).thenReturn(false);

        NotFoundException error = assertThrows(NotFoundException.class,
                () -> service.createNote(request));

        assertEquals("Country is not active: GB", error.getMessage());
        verify(caseNoteRepository, never()).create(any(), any(), any());
    }

    private NoteResponse noteResponse() {
        NoteResponse response = new NoteResponse();
        response.setId(UUID.randomUUID());
        response.setCaseId("CASE-123");
        response.setStaffId("STAFF-1");
        response.setCountryCode("GB");
        response.setNoteContent("<p>Hello</p>");
        response.setNotePlainText("Hello");
        response.setVersion(1);
        assertNotNull(response.getId());
        return response;
    }
}