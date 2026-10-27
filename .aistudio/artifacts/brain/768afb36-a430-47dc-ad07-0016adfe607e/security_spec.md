# Security Specification — Fellah Numérique

## 1. Data Invariants
1. **User Isolation on Parcels (`/parcels/{parcelId}`)**: Every agricultural parcel document belongs exclusively to `userId == request.auth.uid`. Unauthenticated callers and non-owners cannot read (`get`/`list`), create, update, or delete another user's parcel.
2. **Strict Schema & Bounds**: Every string and array enforces explicit length/size limits (`isValidId`, `.size()`), numeric agrological telemetry (`soilPh`, `ndviScore`, `salinityEc`, `areaHectares`, `estimatedYieldTonsPerHa`) enforces physical domain bounds, and timestamps enforce `is timestamp && <= request.time`.
3. **Immutable Ownership & Creation Metadata**: `userId` and `createdAt` cannot be mutated on `update`; `parcelId` is also immutable on `market_listings`.
4. **Relational Integrity on Market Listings (`/market_listings/{listingId}`)**: A market listing can only be created if `exists(/databases/$(database)/documents/parcels/$(incoming().parcelId))` AND the referenced parcel's `userId == request.auth.uid`.
5. **Terminal State Locking**: Once a market listing reaches `status == 'sold'`, all subsequent updates are rejected.
6. **Query Enforcement**: `allow list` never uses blanket `isSignedIn()`; `/parcels` requires `resource.data.userId == request.auth.uid`, and `/market_listings` requires `resource.data.userId == request.auth.uid || resource.data.status == 'active'`.

## 2. Dirty Dozen Payloads
1. Unauthenticated read/write to `/parcels/p1` -> Reject.
2. Cross-user `get` on `/parcels/bob_parcel` by Alice -> Reject.
3. Unfiltered `list` query on `/parcels` without `where('userId', '==', uid)` -> Reject.
4. Identity spoofing on `create`: Alice writes `userId: 'bob_456'` -> Reject.
5. Shadow update with ghost field `isAdmin: true` on `/parcels/p1` -> Reject (`hasOnly`).
6. Value poisoning: `soilPh: 'acidic'` or `soilPh: 99.0` -> Reject (`isValidParcel`).
7. Oversized or poisoned document ID (`parcelId` with spaces/slashes or >128 chars) -> Reject (`isValidId`).
8. Unbounded array attack: `polygonPoints` with 50 items or fewer than 3 vertices -> Reject.
9. Immutable field tampering: changing `createdAt` or `userId` on parcel `update` -> Reject.
10. Orphaned market listing: creating `/market_listings/m1` referencing non-existent or Bob's `parcelId` -> Reject.
11. Terminal state bypass: updating `/market_listings/m1` after `status == 'sold'` -> Reject.
12. Future timestamp spoofing: `createdAt` set ahead of `request.time` -> Reject.
