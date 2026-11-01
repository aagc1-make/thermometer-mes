# Local Knowledge Base Implementation Plan

> **For agentic workers:** Use superpowers:executing-plans to implement this authorized change inline. Track completed steps below and in the task ledger.

**Goal:** Add browser-local document import, version management, retrieval and citations to the MES assistant without backend changes.

**Architecture:** A pure document/retrieval module and IndexedDB adapter support a lazy-loaded management component. The chat explicitly separates business and knowledge modes, renders citations, and reuses the existing consent-based model analysis endpoint.

**Tech Stack:** Vue 2, Element UI, IndexedDB, bundled PDF.js, browser-native DOCX decompression, Node test.

**Spec:** `docs/superpowers/specs/2026-11-01-local-knowledge-design.md`

## Constraints

Browser-local, account-scoped storage; no new backend interface, database table, vector service or automatic document upload. Limits: 5 MiB/file, 300000 characters/document, 40 versions, 1000000 total characters. Source text must remain plain text in rendering.

## Tasks

1. Pure engine: `src/utils/knowledgeBase.js`; tests `scripts/tests/knowledge-base.test.cjs`. Watch failing tests for section splitting, relevant citations, unrelated questions, active versions, duplicate versions and limits; implement and pass.
2. Storage and import: `src/utils/knowledgeStorage.js`, `src/utils/knowledgeImport.js`; account-scoped IndexedDB commits, UTF-8/GB18030 text, bundled PDF.js pages and DOCX paragraphs. Verify parser boundaries and real browser persistence; treat transaction abort as failure.
3. Management and chat: `src/views/mes/ai/KnowledgeBase.vue`, `KnowledgeAnswer.vue`, and small additions to `index.vue`. Manage import/preview/version enable/delete, source navigation and explicit knowledge mode. Test that knowledge questions do not invoke business APIs and live business queries remain unchanged.
4. Verify: full offline suite, focused Vue syntax/lint, production build, real browser import/search/refresh/version/delete and DOCX/PDF extraction. Obtain one read-only review of the complete change and address concrete issues. Document actual limits and verification results.

## Review focus

- A stale/deleted citation cannot silently open a different version.
- Switching account cannot expose a previous account's document list.
- Import quota/transaction failure must not retire the previous active version.
- Imported text cannot render active HTML or trigger a business write action.
- Empty/scanned/corrupt documents and unsupported types fail explicitly.

## Progress

- Baseline: 132 offline tests passed before implementation.
- User chose browser-local storage. Proceed with the authorized scope; no backend implementation is needed.
- Task 1 complete: all eight engine tests passed after observing eight expected failures.
- Ruling: reuse browser-native DecompressionStream for the single DOCX XML entry rather than install JSZip; capability was verified in the available browser. XML decompression is bounded and old browsers receive a paste-text fallback message. No new npm dependency is needed.
- Tasks 2–3 complete: frontend imports, IndexedDB persistence, version management, explicit chat mode and citations implemented. Imports and lifecycle verified in a separate localhost browser context.
- Review: fixed the reproduced PDF CMap omission and native same-file input state issue. Header-only and weakly related results now rank below substantive text or are filtered; behavior tests observed red then green.
- Final verification complete: 150/150 offline tests passed; all three affected Vue SFCs parsed; new utilities/components passed focused ESLint; production build succeeded. Browser lifecycle/version/deleted-source/account-isolation checks passed, and synthetic documents were removed from the isolated test profile. Read-only final review found no remaining important issue. No backend or deployment changes are part of this task.
