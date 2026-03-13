package com.casemanagement.notes.controller;

import com.casemanagement.notes.dto.CaseNoteDtos.CountryResponse;
import com.casemanagement.notes.service.CaseNoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/countries")
@Tag(name = "Countries", description = "Country metadata and table provisioning")
public class CountryController {

    private final CaseNoteService noteService;

    public CountryController(CaseNoteService noteService) {
        this.noteService = noteService;
    }

    @GetMapping
    @Operation(summary = "List active countries")
    public List<CountryResponse> listCountries() {
        return noteService.listCountries();
    }

    @PostMapping("/{countryCode}/provision")
    @Operation(summary = "Provision per-country notes table")
    public ResponseEntity<Map<String, String>> provisionCountry(@PathVariable String countryCode) {
        noteService.provisionCountry(countryCode);
        return ResponseEntity.ok(Map.of("status", "ok", "countryCode", countryCode.toUpperCase()));
    }
}
