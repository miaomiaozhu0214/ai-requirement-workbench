# AI Conversation Requirement Flow Acceptance Case

## Purpose

Verify the real LLM conversation loop from a user message to AI Router, requirement extraction, multi-turn refinement, candidate card update, formal requirement creation, requirement pool visibility, and AI Trace visibility.

## Preconditions

- Backend is running with real LLM mode enabled.
- Frontend is running and accessible.
- PostgreSQL migrations have completed successfully.
- Prompt templates include:
  - `default_intent_router`
  - `default_requirement_extract`
  - `default_completeness_check`
  - `default_reply_generate`
  - `default_card_generate`
- Trace page `/ai/traces` is accessible.

## Test Steps

1. Open the conversation page.
2. Create a new conversation.
3. Send:

   ```text
   客户希望合同列表支持导出
   ```

4. Verify the latest Router result:
   - `intent_router` is called.
   - Router intent is `new_requirement`.
   - Follow-up abilities include `requirement_extract`, `completeness_check`, and `reply_generate`.

5. Verify a candidate requirement is generated:
   - Candidate title is close to `合同列表支持导出`.
   - Candidate card appears in the right-side panel.
   - AI asks for missing details such as export scope, permissions, fields, file format, limits, exceptions, or acceptance criteria.

6. Send supplementary information:

   ```text
   导出当前筛选结果，只允许运营人员，字段和列表一致
   ```

7. Verify the latest Router result:
   - `intent_router` is called.
   - Router intent is `supplement_requirement`.

8. Verify the same candidate requirement is updated:
   - Candidate count should not increase for this single supplement.
   - Candidate content includes current filtered results as export scope.
   - Candidate content includes operation staff as permission rule.
   - Candidate content includes fields matching the list.
   - Completeness score should improve or missing items should become more specific.

9. Generate a requirement card by clicking the UI action or sending:

   ```text
   /生成卡片
   ```

10. Verify AI Trace:
    - `intent_router` is called.
    - Router intent is `generate_card`.
    - `card_generate` Trace is recorded.

11. Confirm and create the formal requirement card.

12. Verify the formal requirement is persisted:
    - Requirement pool contains the new requirement.
    - Requirement title is close to `合同列表支持导出`.
    - Requirement source session ID is present.
    - Requirement source candidate ID is present.

13. Open `/ai/traces`.

14. Verify the Trace page displays records for:
    - `intent_router`
    - `requirement_extract`
    - `completeness_check`
    - `reply_generate`
    - `card_generate`

15. Verify each visible Trace row shows:
    - Status
    - Ability type
    - Input summary
    - Model name
    - Prompt template code
    - Prompt version
    - Duration
    - Token fields

16. Click an `intent_router` Trace row.

17. Verify the detail panel shows:
    - Router output JSON
    - Input JSON
    - Model name
    - Prompt template code and version
    - Success or failure status

## Expected Result

- Every user message first creates an `intent_router` Trace.
- The backend Orchestrator chooses follow-up abilities based on Router output and current state.
- New demand text creates a candidate requirement.
- Supplementary text updates the same candidate requirement.
- Card generation creates a `card_generate` Trace.
- Formal requirement is saved in the backend and visible in the requirement pool.
- Trace page and Trace API show consistent ability calls.

## Regression Notes

- Do not accept a pass if Trace API works but `/ai/traces` UI is blank.
- Do not accept a pass if conversation messages directly call extraction without `intent_router`.
- Do not accept a pass if `frontend/src` contains `.js` or `.js.map` artifacts, because Vite may load stale JavaScript instead of TypeScript source.
