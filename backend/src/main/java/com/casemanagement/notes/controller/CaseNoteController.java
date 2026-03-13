package com.casemanagement.notes.controller;

import com.casemanagement.notes.dto.CaseNoteDtos.CreateRequest;
import com.casemanagement.notes.dto.CaseNoteDtos.DeleteRequest;
import com.casemanagement.notes.dto.CaseNoteDtos.NoteResponse;
import com.casemanagement.notes.dto.CaseNoteDtos.PagedResponse;
import com.casemanagement.notes.dto.CaseNoteDtos.UpdateRequest;
import com.casemanagement.notes.service.CaseNoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/countries/{countryCode}/notes")
@Tag(name = "Case Notes", description = "Manage case notes with per-country data isolation")
public class CaseNoteController {

    private final CaseNoteService noteService;

    public CaseNoteController(CaseNoteService noteService) {
        this.noteService = noteService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new case note")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Note created",
                    content = @Content(schema = @Schema(implementation = NoteResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "404", description = "Country code not found")
    })
    public ResponseEntity<NoteResponse> createNote(
            @Parameter(description = "ISO country code", example = "GB", required = true)
            @PathVariable String countryCode,
            @Valid @RequestBody CreateRequest request) {

        request.setCountryCode(countryCode.toUpperCase());
        NoteResponse created = noteService.createNote(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Retrieve a note by ID")
    public NoteResponse getNoteById(
            @PathVariable String countryCode,
            @PathVariable UUID id,
            @RequestParam(defaultValue = "false") boolean includeDeleted) {

        return noteService.getNoteById(countryCode, id, includeDeleted);
    }

    @GetMapping
    @Operation(summary = "List notes for a case")
    public PagedResponse getNotesByCase(
            @PathVariable String countryCode,
            @RequestParam String caseId,
            @RequestParam(defaultValue = "false") boolean includeDeleted,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return noteService.getNotesByCase(countryCode, caseId, includeDeleted, page, size);
    }

    @GetMapping("/search")
    @Operation(summary = "Full-text search notes")
    public List<NoteResponse> searchNotes(
            @PathVariable String countryCode,
            @RequestParam String q,
            @RequestParam(defaultValue = "false") boolean includeDeleted,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return noteService.searchNotes(countryCode, q, includeDeleted, page, size);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a case note")
    public NoteResponse updateNote(
            @PathVariable String countryCode,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateRequest request) {

        return noteService.updateNote(countryCode, id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete a case note")
    public NoteResponse softDeleteNote(
            @PathVariable String countryCode,
            @PathVariable UUID id,
            @Valid @RequestBody DeleteRequest request) {

        return noteService.softDeleteNote(countryCode, id, request);
    }
}
