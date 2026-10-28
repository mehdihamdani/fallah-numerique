# Security Specification — Fellah Numérique & Fellah AI

## 1. Data Invariants
1. **User Isolation on Parcels (`/parcels/{parcelId}`)**: Every agricultural parcel document belongs exclusively to `userId == request.auth.uid`. Unauthenticated callers and non-owners cannot read (`get`/`list`), create, update, or delete another user's parcel.
2. **Strict Schema & Bounds**: Every string and array enforces explicit length/size limits (`isValidId`, `.size()`), numeric agrological telemetry (`soilPh`, `ndviScore`, `salinityEc`, `areaHectares`, `estimatedYieldTonsPerHa`) enforces physical domain bounds, and timestamps enforce `is timestamp && <= request.time`.
3. **Immutable Ownership & Creation Metadata**: `userId` and `createdAt` cannot be mutated on `update`; `parcelId` is also immutable on `market_listings`.
4. **Relational Integrity on Market Listings (`/market_listings/{listingId}`)**: A market listing can only be created if `exists(/databases/$(database)/documents/parcels/$(incoming().parcelId))` AND the referenced parcel's `userId == request.auth.uid`.
5. **Terminal State Locking**: Once a market listing reaches `status == 'sold'`, transport reaches `'delivered'`, or marketplace order reaches `'completed'`/`'cancelled'`, subsequent updates are rejected.
6. **Query Enforcement**: `allow list` never uses blanket `isSignedIn()`; `/parcels`, `/ai_conversations`, and `/ai_messages` require `resource.data.userId == request.auth.uid`, and `/market_listings` / `/partner_catalog` require `resource.data.userId == request.auth.uid || resource.data.status == 'active'`.
7. **Fellah AI Conversation & Message Isolation (`/ai_conversations/{conversationId}`, `/ai_messages/{messageId}`)**:
   - Only authenticated owners (`userId == request.auth.uid`) can create, read, list, or delete their own conversations and messages.
   - Creating an `/ai_messages/{messageId}` document requires relational verification that `/ai_conversations/{incoming().conversationId}` exists AND belongs to `request.auth.uid`.
   - AI messages are immutable once written (`allow update: if false`), bounded (`content` 1..4000 chars, `verifiedDataSummary` 0..1500 chars), and strictly typed (`sender in ['user', 'assistant']`).
8. **Fellah AI Daily Usage Quota Enforcement (`/ai_usage_quotas/{userId}`)**:
   - Keyed strictly by `userId == request.auth.uid`, non-deletable (`allow delete: if false`), and bounded (`dailyRequestCount` `0..100`, `dailyTokensEstimate` `0..300000`, `dayKey` matching `YYYY-MM-DD`).
9. **Fellah Matin Editorial RBAC, Approved-Only Visibility & HTTPS URL Validation (`/news_articles`, `/news_sources`, `/editorial_admins`, `/user_news_preferences`)**:
   - Regular signed-in users can read ONLY `moderationStatus == 'approved'` articles and `isEnabled == true` news sources; `pending_review` and `rejected` items are strictly blocked.
   - Only `isEditorialAdmin()` (`mehdi.hamdani.net@gmail.com` or server-provisioned `/editorial_admins/{uid}`) can create, approve/reject, update, or delete `/news_articles` and `/news_sources`.
   - `originalUrl`, `baseUrl`, and `feedUrl` strictly enforce `^https://...` (rejecting `http://`, `javascript:`, and `data:` schemes).
   - `/user_news_preferences/{userId}` enforces strict owner isolation (`request.auth.uid == userId`) and validates `preferredCategories` against the 10 canonical news category codes.

## 2. Dirty Dozen Payloads
1. Unauthenticated read/write to `/parcels/p1` or `/ai_conversations/c1` -> Reject.
2. Cross-user `get` or `delete` on `/parcels/bob_parcel` or `/ai_conversations/bob_conv` by Alice -> Reject.
3. Unfiltered `list` query on `/parcels` or `/ai_conversations` without `where('userId', '==', uid)` -> Reject.
4. Identity spoofing on `create`: Alice writes `userId: 'bob_456'` -> Reject.
5. Shadow update with ghost field `isAdmin: true` on `/parcels/p1` or `/ai_conversations/c1` -> Reject (`hasOnly`).
6. Value poisoning: `soilPh: 'acidic'`, `soilPh: 99.0`, or `dailyRequestCount: 99999` -> Reject.
7. Oversized or poisoned document ID (`parcelId`/`conversationId` with spaces/slashes or >128 chars) -> Reject (`isValidId`).
8. Unbounded array/text attack: `polygonPoints` >20 items or AI message `content` >4000 chars -> Reject.
9. Immutable field tampering: changing `createdAt`, `userId`, or mutating an existing `/ai_messages/{id}` -> Reject.
10. Orphaned relational record: creating `/market_listings/m1` referencing Bob's `parcelId`, or `/ai_messages/msg1` referencing Bob's/missing `conversationId` -> Reject.
11. Terminal state bypass: updating `/market_listings/m1` after `status == 'sold'` -> Reject.
12. Future timestamp spoofing: `createdAt` or `updatedAt` set ahead of `request.time` -> Reject.
