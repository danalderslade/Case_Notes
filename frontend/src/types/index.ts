export interface CaseNote {
  id: string;
  caseId: string;
  staffId: string;
  countryCode: string;
  noteContent: string;
  notePlainText: string;
  createdAt: string;
  updatedAt: string;
  version: number;
}

export interface Country {
  isoAlpha2: string;
  countryName: string;
}

export interface PagedResponse {
  content: CaseNote[];
  pageNumber: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export interface CreateNoteRequest {
  caseId: string;
  staffId: string;
  countryCode: string;
  noteContent: string;
}

export interface UpdateNoteRequest {
  noteContent: string;
  version: number;
}

export interface DeleteNoteRequest {
  deletedBy: string;
  deleteReason?: string;
}
