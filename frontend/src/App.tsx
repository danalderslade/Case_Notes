// src/App.tsx  — Full Case Notes Module
// Uses TipTap for rich text, clean professional UI

import { useState, useEffect, useCallback } from 'react';
import { useEditor, EditorContent } from '@tiptap/react';
import StarterKit from '@tiptap/starter-kit';
import Underline from '@tiptap/extension-underline';
import TextAlign from '@tiptap/extension-text-align';
import Highlight from '@tiptap/extension-highlight';
import Placeholder from '@tiptap/extension-placeholder';
import {
  Plus, Trash2, Edit3, Search, ChevronLeft, ChevronRight,
  FileText, AlertCircle, X, Save, Clock,
  User, Globe, Bold, Italic, UnderlineIcon, AlignLeft,
  AlignCenter, AlignRight, List, ListOrdered, Highlighter,
  RotateCcw
} from 'lucide-react';
import * as api from './api/notesApi';
import type { CaseNote, Country, PagedResponse } from './types';
import { formatDistanceToNow, format } from 'date-fns';

// ────────────────────────────────────────────────────────────
// Toolbar component for the rich text editor
// ────────────────────────────────────────────────────────────
function EditorToolbar({ editor }: { editor: any }) {
  if (!editor) return null;

  const btn = (active: boolean) =>
    `p-1.5 rounded transition-colors ${active
      ? 'bg-indigo-100 text-indigo-700'
      : 'text-gray-500 hover:bg-gray-100 hover:text-gray-800'}`;

  return (
    <div className="flex flex-wrap items-center gap-0.5 px-3 py-2 border-b border-gray-200 bg-gray-50 rounded-t-lg">
      <button type="button" className={btn(editor.isActive('bold'))}
        onClick={() => editor.chain().focus().toggleBold().run()}>
        <Bold size={15} />
      </button>
      <button type="button" className={btn(editor.isActive('italic'))}
        onClick={() => editor.chain().focus().toggleItalic().run()}>
        <Italic size={15} />
      </button>
      <button type="button" className={btn(editor.isActive('underline'))}
        onClick={() => editor.chain().focus().toggleUnderline().run()}>
        <UnderlineIcon size={15} />
      </button>
      <button type="button" className={btn(editor.isActive('highlight'))}
        onClick={() => editor.chain().focus().toggleHighlight().run()}>
        <Highlighter size={15} />
      </button>
      <span className="w-px h-5 bg-gray-300 mx-1" />
      <button type="button" className={btn(editor.isActive({ textAlign: 'left' }))}
        onClick={() => editor.chain().focus().setTextAlign('left').run()}>
        <AlignLeft size={15} />
      </button>
      <button type="button" className={btn(editor.isActive({ textAlign: 'center' }))}
        onClick={() => editor.chain().focus().setTextAlign('center').run()}>
        <AlignCenter size={15} />
      </button>
      <button type="button" className={btn(editor.isActive({ textAlign: 'right' }))}
        onClick={() => editor.chain().focus().setTextAlign('right').run()}>
        <AlignRight size={15} />
      </button>
      <span className="w-px h-5 bg-gray-300 mx-1" />
      <button type="button" className={btn(editor.isActive('bulletList'))}
        onClick={() => editor.chain().focus().toggleBulletList().run()}>
        <List size={15} />
      </button>
      <button type="button" className={btn(editor.isActive('orderedList'))}
        onClick={() => editor.chain().focus().toggleOrderedList().run()}>
        <ListOrdered size={15} />
      </button>
    </div>
  );
}

// ────────────────────────────────────────────────────────────
// Main App
// ────────────────────────────────────────────────────────────
export default function App() {
  // State
  const [countries, setCountries] = useState<Country[]>([]);
  const [selectedCountry, setSelectedCountry] = useState('GB');
  const [caseId, setCaseId] = useState('CASE-2024-00001');
  const [currentStaffId] = useState('STAFF-001');
  const [notes, setNotes] = useState<PagedResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [searchResults, setSearchResults] = useState<CaseNote[] | null>(null);
  const [page, setPage] = useState(0);

  // Modal state
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [editingNote, setEditingNote] = useState<CaseNote | null>(null);
  const [deletingNote, setDeletingNote] = useState<CaseNote | null>(null);
  const [deleteReason, setDeleteReason] = useState('');
  const [saving, setSaving] = useState(false);

  // Editor
  const editor = useEditor({
    extensions: [
      StarterKit,
      Underline,
      TextAlign.configure({ types: ['heading', 'paragraph'] }),
      Highlight,
      Placeholder.configure({ placeholder: 'Write your case note here…' }),
    ],
    content: '',
  });

  // ── Load countries on mount
  useEffect(() => {
    api.getCountries().then(setCountries).catch(() => {});
  }, []);

  // ── Load notes when case/country/page/filter changes
  const loadNotes = useCallback(async () => {
    if (!caseId.trim()) return;
    setLoading(true);
    setError(null);
    try {
      const data = await api.getNotesByCase(selectedCountry, caseId, { page, size: 10 });
      setNotes(data);
      setSearchResults(null);
    } catch (e: any) {
      setError(e.response?.data?.message ?? 'Failed to load notes');
    } finally {
      setLoading(false);
    }
  }, [caseId, selectedCountry, page]);

  useEffect(() => { loadNotes(); }, [loadNotes]);

  // ── Search
  const handleSearch = async () => {
    if (!searchQuery.trim()) { setSearchResults(null); return; }
    setLoading(true);
    try {
      const results = await api.searchNotes(selectedCountry, searchQuery);
      setSearchResults(results);
    } catch (e: any) {
      setError(e.response?.data?.message ?? 'Search failed');
    } finally {
      setLoading(false);
    }
  };

  // ── Create note
  const handleCreate = async () => {
    if (!editor || editor.isEmpty) return;
    setSaving(true);
    try {
      await api.createNote({
        caseId,
        staffId: currentStaffId,
        countryCode: selectedCountry,
        noteContent: editor.getHTML(),
      });
      editor.commands.clearContent();
      setShowCreateModal(false);
      loadNotes();
    } catch (e: any) {
      setError(e.response?.data?.message ?? 'Failed to create note');
    } finally {
      setSaving(false);
    }
  };

  // ── Update note
  const handleUpdate = async () => {
    if (!editor || !editingNote) return;
    setSaving(true);
    try {
      await api.updateNote(selectedCountry, editingNote.id, {
        noteContent: editor.getHTML(),
        version: editingNote.version,
      });
      setEditingNote(null);
      loadNotes();
    } catch (e: any) {
      setError(e.response?.data?.message ?? 'Failed to update note');
    } finally {
      setSaving(false);
    }
  };

  // ── Delete note
  const handleDelete = async () => {
    if (!deletingNote) return;
    setSaving(true);
    try {
      await api.softDeleteNote(selectedCountry, deletingNote.id, {
        deletedBy: currentStaffId,
        deleteReason,
      });
      setDeletingNote(null);
      setDeleteReason('');
      loadNotes();
    } catch (e: any) {
      setError(e.response?.data?.message ?? 'Failed to delete note');
    } finally {
      setSaving(false);
    }
  };

  // Open edit modal
  const openEdit = (note: CaseNote) => {
    setEditingNote(note);
    editor?.commands.setContent(note.noteContent);
  };

  const displayedNotes = searchResults ?? notes?.content ?? [];

  // ────────────────────────────────────────────────────────
  // Render
  // ────────────────────────────────────────────────────────
  return (
    <div className="min-h-screen bg-gray-50 font-sans">
      {/* Header */}
      <header className="bg-white border-b border-gray-200 sticky top-0 z-20">
        <div className="max-w-5xl mx-auto px-4 py-3 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="p-2 bg-indigo-600 rounded-lg">
              <FileText size={18} className="text-white" />
            </div>
            <div>
              <h1 className="text-base font-semibold text-gray-900">Case Notes</h1>
              <p className="text-xs text-gray-500">Case Management System</p>
            </div>
          </div>
          <div className="flex items-center gap-2 text-sm text-gray-500">
            <User size={14} />
            <span>{currentStaffId}</span>
          </div>
        </div>
      </header>

      <main className="max-w-5xl mx-auto px-4 py-6 space-y-5">
        {/* Controls bar */}
        <div className="bg-white rounded-xl border border-gray-200 p-4 space-y-3">
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
            {/* Country selector */}
            <div>
              <label className="block text-xs font-medium text-gray-600 mb-1">
                <Globe size={12} className="inline mr-1" />Country
              </label>
              <select
                value={selectedCountry}
                onChange={e => { setSelectedCountry(e.target.value); setPage(0); }}
                className="w-full text-sm border border-gray-300 rounded-lg px-3 py-2 focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none">
                {countries.map(c => (
                  <option key={c.isoAlpha2} value={c.isoAlpha2}>
                    {c.isoAlpha2} — {c.countryName}
                  </option>
                ))}
              </select>
            </div>

            {/* Case ID */}
            <div>
              <label className="block text-xs font-medium text-gray-600 mb-1">Case ID</label>
              <input
                type="text"
                value={caseId}
                onChange={e => { setCaseId(e.target.value); setPage(0); }}
                className="w-full text-sm border border-gray-300 rounded-lg px-3 py-2 focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none"
                placeholder="e.g. CASE-2024-00001"
              />
            </div>

            {/* Actions */}
            <div className="flex items-end gap-2">
              <button
                onClick={() => { setShowCreateModal(true); editor?.commands.clearContent(); }}
                className="flex-1 flex items-center justify-center gap-2 bg-indigo-600 hover:bg-indigo-700 text-white text-sm font-medium px-4 py-2 rounded-lg transition-colors">
                <Plus size={16} /> Add Note
              </button>
            </div>
          </div>

          {/* Search */}
          <div className="flex gap-2">
            <input
              type="text"
              value={searchQuery}
              onChange={e => { setSearchQuery(e.target.value); if (!e.target.value) setSearchResults(null); }}
              onKeyDown={e => e.key === 'Enter' && handleSearch()}
              placeholder="Full-text search notes…"
              className="flex-1 text-sm border border-gray-300 rounded-lg px-3 py-2 focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none"
            />
            <button
              onClick={handleSearch}
              className="flex items-center gap-1.5 bg-gray-100 hover:bg-gray-200 text-gray-700 text-sm px-3 py-2 rounded-lg transition-colors">
              <Search size={15} /> Search
            </button>
            {searchResults && (
              <button onClick={() => { setSearchResults(null); setSearchQuery(''); }}
                className="p-2 text-gray-400 hover:text-gray-700 rounded-lg hover:bg-gray-100">
                <RotateCcw size={15} />
              </button>
            )}
          </div>
        </div>

        {/* Error banner */}
        {error && (
          <div className="flex items-start gap-3 bg-red-50 border border-red-200 rounded-xl p-4 text-red-700 text-sm">
            <AlertCircle size={18} className="shrink-0 mt-0.5" />
            <div className="flex-1">{error}</div>
            <button onClick={() => setError(null)}><X size={16} /></button>
          </div>
        )}

        {/* Notes list */}
        <div>
          {searchResults && (
            <p className="text-sm text-gray-500 mb-3">
              {searchResults.length} result{searchResults.length !== 1 ? 's' : ''} for "{searchQuery}"
            </p>
          )}

          {loading ? (
            <div className="flex items-center justify-center py-16 text-gray-400">
              <div className="animate-spin w-6 h-6 border-2 border-indigo-500 border-t-transparent rounded-full mr-3" />
              Loading notes…
            </div>
          ) : displayedNotes.length === 0 ? (
            <div className="flex flex-col items-center justify-center py-16 text-gray-400">
              <FileText size={36} className="mb-3 opacity-30" />
              <p className="text-sm">No notes found for this case.</p>
              <button
                onClick={() => { setShowCreateModal(true); editor?.commands.clearContent(); }}
                className="mt-4 text-sm text-indigo-600 hover:underline">
                Add the first note
              </button>
            </div>
          ) : (
            <div className="overflow-hidden rounded-xl border border-gray-200 bg-white shadow-sm">
              <div className="overflow-x-auto">
                <table className="min-w-full divide-y divide-gray-200 text-sm">
                  <thead className="bg-gray-50 text-xs uppercase tracking-wide text-gray-500">
                    <tr>
                      <th className="px-4 py-3 text-left">Created</th>
                      <th className="px-4 py-3 text-left">Staff</th>
                      <th className="px-4 py-3 text-left">Note</th>
                      <th className="px-4 py-3 text-left">Actions</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-gray-100 bg-white">
                    {displayedNotes.map(note => (
                      <tr key={note.id} className="hover:bg-gray-50">
                        <td className="px-4 py-3 whitespace-nowrap text-gray-600">
                          <div title={format(new Date(note.createdAt), 'PPpp')}>
                            {formatDistanceToNow(new Date(note.createdAt), { addSuffix: true })}
                          </div>
                        </td>
                        <td className="px-4 py-3 whitespace-nowrap font-medium text-gray-700">{note.staffId}</td>
                        <td className="px-4 py-3 text-gray-700">
                          <div className="max-w-xl line-clamp-2" dangerouslySetInnerHTML={{ __html: note.noteContent }} />
                        </td>
                        <td className="px-4 py-3 whitespace-nowrap">
                          <div className="flex items-center gap-1">
                            <button
                              onClick={() => openEdit(note)}
                              className="p-1.5 text-gray-400 hover:text-indigo-600 hover:bg-indigo-50 rounded-lg transition-colors"
                              title="Edit note">
                              <Edit3 size={15} />
                            </button>
                            <button
                              onClick={() => setDeletingNote(note)}
                              className="p-1.5 text-gray-400 hover:text-red-600 hover:bg-red-50 rounded-lg transition-colors"
                              title="Delete note">
                              <Trash2 size={15} />
                            </button>
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}

          {/* Pagination (only for non-search) */}
          {!searchResults && notes && notes.totalPages > 1 && (
            <div className="flex items-center justify-between mt-4 pt-4 border-t border-gray-200">
              <span className="text-sm text-gray-500">
                Page {notes.pageNumber + 1} of {notes.totalPages}
                <span className="ml-2 text-gray-400">({notes.totalElements} notes)</span>
              </span>
              <div className="flex gap-2">
                <button disabled={page === 0} onClick={() => setPage(p => p - 1)}
                  className="p-2 rounded-lg border border-gray-200 text-gray-600 disabled:opacity-40 hover:bg-gray-50 transition-colors">
                  <ChevronLeft size={16} />
                </button>
                <button disabled={notes.last} onClick={() => setPage(p => p + 1)}
                  className="p-2 rounded-lg border border-gray-200 text-gray-600 disabled:opacity-40 hover:bg-gray-50 transition-colors">
                  <ChevronRight size={16} />
                </button>
              </div>
            </div>
          )}
        </div>
      </main>

      {/* ── CREATE / EDIT MODAL ── */}
      {(showCreateModal || editingNote) && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/40 backdrop-blur-sm">
          <div className="bg-white rounded-2xl shadow-2xl w-full max-w-2xl max-h-[90vh] flex flex-col">
            <div className="flex items-center justify-between px-5 py-4 border-b border-gray-200">
              <h2 className="font-semibold text-gray-900">
                {editingNote ? 'Edit Note' : 'Add New Note'}
              </h2>
              <button
                onClick={() => { setShowCreateModal(false); setEditingNote(null); }}
                className="p-1.5 text-gray-400 hover:text-gray-700 hover:bg-gray-100 rounded-lg transition-colors">
                <X size={18} />
              </button>
            </div>

            <div className="flex-1 overflow-auto p-5">
              <div className="border border-gray-300 rounded-lg focus-within:ring-2 focus-within:ring-indigo-500 focus-within:border-indigo-500">
                <EditorToolbar editor={editor} />
                <EditorContent
                  editor={editor}
                  className="min-h-[200px] p-3 prose prose-sm max-w-none focus:outline-none"
                />
              </div>
              <p className="mt-2 text-xs text-gray-400">
                Rich text is stored as HTML. Plain text is extracted automatically for search indexing.
              </p>
            </div>

            <div className="flex items-center justify-end gap-3 px-5 py-4 border-t border-gray-200">
              <button
                onClick={() => { setShowCreateModal(false); setEditingNote(null); }}
                className="px-4 py-2 text-sm text-gray-600 hover:text-gray-900 hover:bg-gray-100 rounded-lg transition-colors">
                Cancel
              </button>
              <button
                onClick={editingNote ? handleUpdate : handleCreate}
                disabled={saving || editor?.isEmpty}
                className="flex items-center gap-2 bg-indigo-600 hover:bg-indigo-700 disabled:opacity-50 text-white text-sm font-medium px-5 py-2 rounded-lg transition-colors">
                {saving
                  ? <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
                  : <Save size={15} />}
                {editingNote ? 'Save Changes' : 'Add Note'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ── DELETE CONFIRMATION MODAL ── */}
      {deletingNote && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/40 backdrop-blur-sm">
          <div className="bg-white rounded-2xl shadow-2xl w-full max-w-md">
            <div className="p-5">
              <div className="flex items-center gap-3 mb-4">
                <div className="p-2.5 bg-red-100 rounded-xl">
                  <Trash2 size={20} className="text-red-600" />
                </div>
                <div>
                  <h2 className="font-semibold text-gray-900">Delete Note</h2>
                  <p className="text-sm text-gray-500">This is a soft delete — the record is retained for audit purposes.</p>
                </div>
              </div>

              <div className="bg-gray-50 rounded-lg p-3 mb-4 text-sm text-gray-700 line-clamp-3"
                dangerouslySetInnerHTML={{ __html: deletingNote.noteContent }} />

              <div className="mb-4">
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Reason for deletion <span className="text-gray-400">(optional)</span>
                </label>
                <textarea
                  value={deleteReason}
                  onChange={e => setDeleteReason(e.target.value)}
                  rows={2}
                  className="w-full text-sm border border-gray-300 rounded-lg px-3 py-2 focus:ring-2 focus:ring-red-500 focus:border-red-500 outline-none resize-none"
                  placeholder="e.g. Note added to wrong case"
                />
              </div>
            </div>

            <div className="flex gap-3 px-5 pb-5">
              <button
                onClick={() => { setDeletingNote(null); setDeleteReason(''); }}
                className="flex-1 px-4 py-2 text-sm text-gray-600 hover:text-gray-900 border border-gray-300 hover:bg-gray-50 rounded-lg transition-colors">
                Cancel
              </button>
              <button
                onClick={handleDelete}
                disabled={saving}
                className="flex-1 flex items-center justify-center gap-2 bg-red-600 hover:bg-red-700 disabled:opacity-50 text-white text-sm font-medium px-4 py-2 rounded-lg transition-colors">
                {saving
                  ? <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
                  : <Trash2 size={15} />}
                Confirm Delete
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
