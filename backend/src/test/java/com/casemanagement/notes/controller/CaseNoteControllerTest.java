package com.casemanagement.notes.controller;

import com.casemanagement.notes.dto.CaseNoteDtos.CreateRequest;
import com.casemanagement.notes.dto.CaseNoteDtos.NoteResponse;
import com.casemanagement.notes.service.CaseNoteService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CaseNoteController.class)
class CaseNoteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CaseNoteService caseNoteService;

    @Test
    void createNoteAcceptsCaseAndCountryFromRouteAndQuery() throws Exception {
        CreateRequest request = new CreateRequest();
        request.setStaffId("STAFF-001");
        request.setNoteContent("<p>Initial contact made.</p>");

        NoteResponse response = new NoteResponse();
        response.setId(UUID.randomUUID());

        when(caseNoteService.createNote(any(CreateRequest.class))).thenReturn(response);

        mockMvc.perform(post("/v1/cases/{caseId}/notes", "CASE-2024-00001")
                        .queryParam("countryCode", "gb")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        ArgumentCaptor<CreateRequest> captor = ArgumentCaptor.forClass(CreateRequest.class);
        verify(caseNoteService).createNote(captor.capture());

        assertEquals("CASE-2024-00001", captor.getValue().getCaseId());
        assertEquals("GB", captor.getValue().getCountryCode());
        assertEquals("STAFF-001", captor.getValue().getStaffId());
        assertEquals("<p>Initial contact made.</p>", captor.getValue().getNoteContent());
    }
}