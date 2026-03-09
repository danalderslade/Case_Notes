package com.casemanagement.notes.controller;

import com.casemanagement.notes.dto.CaseNoteDtos.*;
import com.casemanagement.notes.service.CaseNoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/countries/{countryCode}/notes")
@RequiredArgsConstructor
@Tag(name = "Case Notes", description = "Manage case notes with per-country data isolation. " +
        "All operations require a valid ISO 3166-1 alpha-2 country code in the path.")
public class CaseNoteController {

    private final CaseNoteService noteService;

    // ----------------------------------------------------------------
    // POST /v1/countries/{countryCode}/notes
    // ----------------------------------------------------------------

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        summary     = "Create a new case note",
        description = "Creates a new note for the specified case. " +
                      "Rich text content should be submitted as HTML (TipTap editor output)."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Note created",
                     content = @Content(schema = @Schema(implementation = NoteResponse.class))),
        @ApiResponse(responseCode = "400", description = "Validation error"),
        @ApiResponse(responseCode = "404", description = "Country code not found")
    })
    public ResponseEntity<NoteResponse> createNote(
            @Parameter(description = "ISO 3166-1 alpha-2 country code", example = "GB", required = true)
            @PathVariable String countryCode,
            @Valid @RequestBody CreateRequest request) {

        // Ensure path and body country codes are consistent
        request.setCountryCode(countryCode.toUpperCase());
        NoteResponse created = noteService.createNote(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // ----------------------------------------------------------------
    // GET /v1/countries/{countryCode}/notes/{id}
    // ----------------------------------------------------------------

    @GetMapping("/{id}")
    @Operation(
        summary     = "Retrieve a note by ID",
        description = "Returns a single note by its UUID. " +
                      "Soft-deleted notes are excluded by default."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Note found",
                     content = @Content(schema = @Schema(implementation = NoteResponse.class))),
        @ApiResponse(responseCode = "404", description = "Note not found")
    })
    public NoteResponse getNoteById(
            @PathVariable String countryCode,
            @Parameter(description = "UUID of the note", required = true)
            @PathVariable UUID id,
            @Parameter(description = "Include soft-deleted note if true")
            @RequestParam(defaultValue = "false") boolean includeDeleted) {

        return noteService.getNoteById(countryCode, id, includeDeleted);
    }

    // ----------------------------------------------------------------
    // GET /v1/countries/{countryCode}/notes?caseId=...
    // ----------------------------------------------------------------

    @GetMapping
    @Operation(
        summary     = "List notes for a case",
        description = "Returns a paginated list of notes for the given case. " +
                      "Supports optional inclusion of soft-deleted records."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Paginated note list",
                     content = @Content(schema = @Schema(implementation = PagedResponse.class)))
    })
    public PagedResponse getNotesByCase(
            @PathVariable String countryCode,
            @Parameter(description = "Case identifier to filter notes", required = true)
            @RequestParam String caseId,
            @Parameter(description = "Include soft-deleted notes")
            @RequestParam(defaultValue = "false") boolean includeDeleted,
            @Parameter(description = "Zero-based page number")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (max 100)")
            @RequestParam(defaultValue = "20") int size) {

        return noteService.getNotesByCase(countryCode, caseId, includeDeleted, page, size);
    }

    // ----------------------------------------------------------------
    // GET /v1/countries/{countryCode}/notes/search?q=...
    // ----------------------------------------------------------------

    @GetMapping("/search")
    @Operation(
        summary     = "Full-text search notes",
        description = "Searches notes using PostgreSQL full-text search (tsvector). " +
                      "Searches the plain-text content of notes."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Matching notes")
    })
    public List<NoteResponse> searchNotes(
            @PathVariable String countryCode,
            @Parameter(description = "Search query string", required = true, example = "initial contact")
            @RequestParam String q,
            @RequestParam(defaultValue = "false") boolean includeDeleted,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return noteService.searchNotes(countryCode, q, includeDeleted, page, size);
    }

    // ----------------------------------------------------------------
    // PUT /v1/countries/{countryCode}/notes/{id}
    // ----------------------------------------------------------------

    @PutMapping("/{id}")
    @Operation(
        summary     = "Update a case note",
        description = "Updates the rich text content of an existing note. " +
                      "Uses optimistic locking — the current version must be supplied."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Note updated",
                     content = @Content(schema = @Schema(implementation = NoteResponse.class))),
        @ApiResponse(responseCode = "400", description = "Validation error"),
        @ApiResponse(responseCode = "404", description = "Note not found"),
        @ApiResponse(responseCode = "409", description = "Optimistic locking conflict")
    })
    public NoteResponse updateNote(
            @PathVariable String countryCode,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateRequest request) {

        return noteService.updateNote(countryCode, id, request);
    }

    // ----------------------------------------------------------------
    // DELETE /v1/countries/{countryCode}/notes/{id}  (soft delete)
    // ----------------------------------------------------------------

    @DeleteMapping("/{id}")
    @Operation(
        summary     = "Soft-delete a case note",
        description = "Marks a note as deleted without physically removing it. " +
                      "Deleted notes remain in the database and can be retrieved " +
                      "with includeDeleted=true. This action is auditable."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Note soft-deleted",
                     content = @Content(schema = @Schema(implementation = NoteResponse.class))),
        @ApiResponse(responseCode = "404", description = "Note not found or already deleted")
    })
    public NoteResponse softDeleteNote(
            @PathVariable String countryCode,
            @PathVariable UUID id,
            @Valid @RequestBody DeleteRequest request) {

        return noteService.softDeleteNote(countryCode, id, request);
    }
}
