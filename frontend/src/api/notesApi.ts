import axios from 'axios';
import type {
  CaseNote,
  Country,
  CreateNoteRequest,
  DeleteNoteRequest,
  PagedResponse,
  UpdateNoteRequest,
} from '../types';

const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '/api',
  timeout: 10000,
});

export async function getCountries(): Promise<Country[]> {
  const response = await apiClient.get<Country[]>('/v1/countries');
  return response.data;
}

export async function createNote(payload: CreateNoteRequest): Promise<CaseNote> {
  const response = await apiClient.post<CaseNote>(`/v1/cases/${payload.caseId}/notes`, payload, {
    params: {
      countryCode: payload.countryCode,
    },
  });
  return response.data;
}

export async function getNotesByCase(
  countryCode: string,
  caseId: string,
  options?: { page?: number; size?: number }
): Promise<PagedResponse> {
  const response = await apiClient.get<PagedResponse>(`/v1/cases/${caseId}/notes`, {
    params: {
      countryCode,
      page: options?.page ?? 0,
      size: options?.size ?? 20,
    },
  });
  return response.data;
}

export async function searchNotes(
  countryCode: string,
  query: string,
  options?: { page?: number; size?: number }
): Promise<CaseNote[]> {
  const response = await apiClient.get<CaseNote[]>(`/v1/notes/search`, {
    params: {
      countryCode,
      q: query,
      page: options?.page ?? 0,
      size: options?.size ?? 20,
    },
  });
  return response.data;
}

export async function updateNote(countryCode: string, noteId: string, payload: UpdateNoteRequest): Promise<CaseNote> {
  const response = await apiClient.put<CaseNote>(`/v1/notes/${noteId}`, payload, {
    params: {
      countryCode,
    },
  });
  return response.data;
}

export async function softDeleteNote(countryCode: string, noteId: string, payload: DeleteNoteRequest): Promise<CaseNote> {
  const response = await apiClient.delete<CaseNote>(`/v1/notes/${noteId}`, {
    params: {
      countryCode,
    },
    data: payload,
  });
  return response.data;
}
