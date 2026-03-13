package com.casemanagement.notes.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class CaseNoteDtos {

    private CaseNoteDtos() {
    }

    public static class CreateRequest {
        @Size(max = 100)
        private String caseId;

        @NotBlank
        @Size(max = 100)
        private String staffId;

        @Size(min = 2, max = 2)
        private String countryCode;

        @NotBlank
        private String noteContent;

        public String getCaseId() {
            return caseId;
        }

        public void setCaseId(String caseId) {
            this.caseId = caseId;
        }

        public String getStaffId() {
            return staffId;
        }

        public void setStaffId(String staffId) {
            this.staffId = staffId;
        }

        public String getCountryCode() {
            return countryCode;
        }

        public void setCountryCode(String countryCode) {
            this.countryCode = countryCode;
        }

        public String getNoteContent() {
            return noteContent;
        }

        public void setNoteContent(String noteContent) {
            this.noteContent = noteContent;
        }
    }

    public static class UpdateRequest {
        @NotBlank
        private String noteContent;

        @NotNull
        @Min(1)
        private Integer version;

        public String getNoteContent() {
            return noteContent;
        }

        public void setNoteContent(String noteContent) {
            this.noteContent = noteContent;
        }

        public Integer getVersion() {
            return version;
        }

        public void setVersion(Integer version) {
            this.version = version;
        }
    }

    public static class DeleteRequest {
        @NotBlank
        @Size(max = 100)
        private String deletedBy;

        @Size(max = 500)
        private String deleteReason;

        public String getDeletedBy() {
            return deletedBy;
        }

        public void setDeletedBy(String deletedBy) {
            this.deletedBy = deletedBy;
        }

        public String getDeleteReason() {
            return deleteReason;
        }

        public void setDeleteReason(String deleteReason) {
            this.deleteReason = deleteReason;
        }
    }

    public static class NoteResponse {
        private UUID id;
        private String caseId;
        private String staffId;
        private String countryCode;
        private String noteContent;
        private String notePlainText;
        private boolean deleted;
        private OffsetDateTime createdAt;
        private OffsetDateTime updatedAt;
        private OffsetDateTime deletedAt;
        private String deletedBy;
        private String deleteReason;
        private Integer version;

        public UUID getId() {
            return id;
        }

        public void setId(UUID id) {
            this.id = id;
        }

        public String getCaseId() {
            return caseId;
        }

        public void setCaseId(String caseId) {
            this.caseId = caseId;
        }

        public String getStaffId() {
            return staffId;
        }

        public void setStaffId(String staffId) {
            this.staffId = staffId;
        }

        public String getCountryCode() {
            return countryCode;
        }

        public void setCountryCode(String countryCode) {
            this.countryCode = countryCode;
        }

        public String getNoteContent() {
            return noteContent;
        }

        public void setNoteContent(String noteContent) {
            this.noteContent = noteContent;
        }

        public String getNotePlainText() {
            return notePlainText;
        }

        public void setNotePlainText(String notePlainText) {
            this.notePlainText = notePlainText;
        }

        public boolean isDeleted() {
            return deleted;
        }

        public void setDeleted(boolean deleted) {
            this.deleted = deleted;
        }

        public OffsetDateTime getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(OffsetDateTime createdAt) {
            this.createdAt = createdAt;
        }

        public OffsetDateTime getUpdatedAt() {
            return updatedAt;
        }

        public void setUpdatedAt(OffsetDateTime updatedAt) {
            this.updatedAt = updatedAt;
        }

        public OffsetDateTime getDeletedAt() {
            return deletedAt;
        }

        public void setDeletedAt(OffsetDateTime deletedAt) {
            this.deletedAt = deletedAt;
        }

        public String getDeletedBy() {
            return deletedBy;
        }

        public void setDeletedBy(String deletedBy) {
            this.deletedBy = deletedBy;
        }

        public String getDeleteReason() {
            return deleteReason;
        }

        public void setDeleteReason(String deleteReason) {
            this.deleteReason = deleteReason;
        }

        public Integer getVersion() {
            return version;
        }

        public void setVersion(Integer version) {
            this.version = version;
        }
    }

    public static class PagedResponse {
        private List<NoteResponse> content;
        private int pageNumber;
        private int pageSize;
        private long totalElements;
        private int totalPages;
        private boolean first;
        private boolean last;

        public List<NoteResponse> getContent() {
            return content;
        }

        public void setContent(List<NoteResponse> content) {
            this.content = content;
        }

        public int getPageNumber() {
            return pageNumber;
        }

        public void setPageNumber(int pageNumber) {
            this.pageNumber = pageNumber;
        }

        public int getPageSize() {
            return pageSize;
        }

        public void setPageSize(int pageSize) {
            this.pageSize = pageSize;
        }

        public long getTotalElements() {
            return totalElements;
        }

        public void setTotalElements(long totalElements) {
            this.totalElements = totalElements;
        }

        public int getTotalPages() {
            return totalPages;
        }

        public void setTotalPages(int totalPages) {
            this.totalPages = totalPages;
        }

        public boolean isFirst() {
            return first;
        }

        public void setFirst(boolean first) {
            this.first = first;
        }

        public boolean isLast() {
            return last;
        }

        public void setLast(boolean last) {
            this.last = last;
        }
    }

    public static class CountryResponse {
        private String isoAlpha2;
        private String countryName;

        public String getIsoAlpha2() {
            return isoAlpha2;
        }

        public void setIsoAlpha2(String isoAlpha2) {
            this.isoAlpha2 = isoAlpha2;
        }

        public String getCountryName() {
            return countryName;
        }

        public void setCountryName(String countryName) {
            this.countryName = countryName;
        }
    }

    public static class PageRequest {
        @Min(0)
        private int page = 0;

        @Min(1)
        @Max(100)
        private int size = 20;

        public int getPage() {
            return page;
        }

        public void setPage(int page) {
            this.page = page;
        }

        public int getSize() {
            return size;
        }

        public void setSize(int size) {
            this.size = size;
        }
    }
}
