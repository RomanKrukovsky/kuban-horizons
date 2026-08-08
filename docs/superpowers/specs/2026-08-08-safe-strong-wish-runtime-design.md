# Safe Strong-Wish Runtime Design

**Date:** 2026-08-08  
**Status:** Approved design, pending implementation plan

## 1. Scope

The first production slice is an infrastructure-first Safe Strong-Wish Runtime demonstrated by one complete operation: named snapshots and reversible restoration of a selected region at its original dimension and coordinates.

The cloud model `euromodels/gpt-5.6-sol` is the Genie's primary intelligence, personality, dialogue engine, and planner. The server remains the sole authority for validation, consent, persistence, and world mutation. If EuroModels is unavailable, deterministic snapshot and restore operations remain available while arbitrary strong wishes are disabled.

This slice does not implement every feature in `GENIE_CONCEPT.md`. It establishes the transaction and capability boundary on which later operations, transformation, vessels, memory, pocket realms, and playable-Genie systems depend.

## 2. Architectural invariants

1. Genie identity is independent of its Minecraft avatar entity.
2. Physical damage cannot terminate a true Genie identity.
3. An LLM proposal never grants world authority.
4. Omnipotence is a large set of verified server capabilities, not arbitrary Java, commands, processes, or scripts.
5. No mutation begins until a complete and verified before-image is durable.
6. A confirmed operation either commits and verifies or rolls back to its verified prior state.
7. Unsupported state is reported accurately; the runtime never claims to have restored it.

## 3. Selected architecture

Use a domain-specific snapshot and restore transaction engine behind a narrow typed-plan layer. Preserve a future path to a general capability kernel through versioned primitive descriptors, opaque references, resource limits, and a strict proposal/authority split.

Initial planner-visible capabilities are coarse:

- `snapshot.create@1`
- `snapshot.inspect@1`
- `snapshot.previewRestore@1`

Actual restore is not planner-authorized. It requires a separate server-issued `ConfirmedRestore` created from an immutable preview and explicit player confirmation.

Deferred until multiple mutation families justify them:

- general DAG plans;
- loops, branches, expressions, or scripting;
- generic low-level block or NBT mutation primitives;
- public third-party adapter API;
- full event sourcing or content-addressed global storage;
- object-level provenance and historical replay;
- concurrent overlapping transactions.

A coarse append-only audit journal records correlation IDs, plan and preview digests, transaction transitions, compatibility findings, and terminal outcomes. It is not the recovery authority.

## 4. Components

### 4.1 WishCoordinator

The server entry point for commands, selection actions, dialogue intents, and confirmations.

Responsibilities:

- enforce the recovery gate;
- route deterministic requests directly;
- send free text to the Genie planner;
- validate all proposed plans through `PlanGate`;
- return structured dialogue, preview, rejection, or durable outcomes;
- ensure world interaction occurs on the server thread.

### 4.2 GeniePlanner

`EuromodelsGeniePlanner` is primary and reads its credential only from `EUROMODELS_API_KEY`. The key is never logged, persisted, sent to the client, or included in prompts.

`DeterministicGeniePlanner` supports predefined dialogue and trusted snapshot/restore requests when the cloud provider is unavailable.

The planner receives a bounded projection containing available schemas, selection summary, snapshot metadata, limits, workflow state, and relevant Genie memory. It receives no live world handles, snapshot bytes, arbitrary NBT, filesystem paths, secrets, or execution facilities.

Dialogue and action are separate outputs. A `DialogueResponse` may contain an optional `ProposedWishIntent`, which independently passes through `PlanGate`.

### 4.3 PlanGate

The first format is a bounded linear plan with an exact schema version, intent, finite list of primitive calls, and expiry.

Validation rejects:

- unknown fields or primitive versions;
- type coercions;
- excessive document or step sizes;
- loops, branches, recursion, expressions, and variable interpolation;
- unavailable capabilities;
- excessive cumulative chunk, memory, disk, or time estimates;
- any attempt to represent confirmation as an LLM-supplied value.

### 4.4 SelectionService

A two-point magical tool creates an immutable same-dimension inclusive AABB. The server validates reach, dimension, build limits, permissions, overflow, and intersecting chunk count. The client sends point interactions, not a trusted ready-made AABB.

### 4.5 SnapshotService

Creates, inspects, and verifies immutable named snapshots. It owns admission, chunk tickets, point-in-time capture, adapter dispatch, encoding, checksums, atomic publication, and quota accounting.

A snapshot does not exist for users until its manifest is durably published after all shards verify.

### 4.6 PreviewService

Produces a side-effect-free immutable restore preview containing:

- snapshot ID and digest;
- original dimension and AABB;
- estimated changed blocks, block entities, ticks, and entities;
- resource requirements;
- player relocation plan;
- compatibility findings and severity;
- state that will remain unchanged;
- risk class and required consent;
- canonical preview digest and expiry.

Read-only chunk acquisition is allowed. World mutation is not.

### 4.7 ConfirmationAuthority

Issues a server-generated, single-use, expiring challenge bound to:

- actor UUID;
- snapshot ID and content digest;
- exact dimension and AABB;
- preview digest;
- compatibility report digest;
- explicit consent flags.

Immediately before mutation, the runtime recomputes the preview. Any material change makes confirmation stale and requires a new preview.

### 4.8 RestoreTransactionService

Accepts only `ConfirmedRestore`. It owns resource reservation, isolation, before-image capture, write-ahead journaling, player relocation, staged mutation, verification, rollback, retained undo, and the immutable transaction report.

Callers cannot invoke individual mutation stages.

### 4.9 RecoveryService

Runs before new strong wishes are accepted in an affected dimension. It resumes rollback idempotently after interrupted apply, verify, or rollback. If it cannot prove a valid terminal state, it enters `FAILED_SAFE` and blocks further strong mutations there.

### 4.10 WorldStateAdapterRegistry

Initial supported domains are:

- block states and fluids;
- block entities;
- scheduled block ticks;
- scheduled fluid ticks;
- eligible ordinary non-player entities;
- explicitly supported mod payloads.

Every state item has exactly one adapter owner. Overlapping claims are admission errors. The adapter boundary remains internal until real integrations prove it.

Derived state such as lighting, heightmaps, POI, and navigation is rebuilt and revalidated rather than copied literally.

## 5. Snapshot semantics and format

A snapshot represents one logical point in time for one inclusive AABB in one dimension. Chunk sharding is a storage detail; the AABB remains authoritative. Restore targets only the original location.

Each snapshot has a manifest and immutable chunk shards. Durable identifiers use namespaced registry names, never runtime numeric IDs. The manifest records format version, metadata, bounds, capture time, ordered shard descriptors, sizes, checksums, supported domains, adapter versions, registry fingerprint, and compatibility findings.

Shards contain only data within the AABB:

- palette-encoded block states using namespaced IDs and canonical properties;
- block entities with type, relative position, native payload, and adapter ownership;
- scheduled ticks with target, relative position, relative delay, priority, and supported ordering data;
- eligible entities with type, snapshot-local identity, UUID, transform, motion, native payload, and bounded passenger/leash relationships;
- versioned adapter payloads.

Players, Genie identities, global system entities, and unsupported unbounded graphs are excluded from ordinary entity replacement.

Snapshots are written to temporary generations. Shards are flushed and verified, then the manifest is flushed and published last. Parent directory metadata is flushed where supported. Invalid or unpublished generations are invisible and later quarantined or cleaned safely.

Unsupported format versions are rejected. Explicit migration creates a new derived generation and never rewrites the source snapshot in place.

## 6. Compatibility policy

Each state domain has one severity:

- `REQUIRED`: capture, restore, and verification must succeed; otherwise reject or rollback.
- `OPTIONAL_REVERSIBLE`: omission is allowed only when previewed, separately accepted, and fully rollback-safe; success becomes complete-with-warnings.
- `REPORT_ONLY`: runtime leaves the external state unchanged where possible and reports that it was not restored.

Best effort never means silently replacing an unknown block with air or deleting an unknown entity. Unknown material state is conservative and normally blocks restore unless a safe adapter or explicit reversible degradation exists.

Registry and adapter comparisons classify a snapshot as `EXACT`, `MIGRATABLE`, `DEGRADED_ALLOWED`, or `BLOCKED`.

## 7. Resource policy

Committed snapshots and retained post-commit undo share a 512 MiB aggregate budget per world. There is no fixed count and no automatic eviction. New captures are rejected if they cannot fit safely.

Recovery material has a separate protected operational reserve so quota exhaustion cannot prevent rollback. It cannot be deleted while recovery references it.

Pilot defaults:

- elevated confirmation above 64 intersecting chunks;
- hard maximum of 256 chunks;
- at most 32 temporary chunk tickets at once;
- approximately 64 MiB working-memory target;
- 128 MiB hard mod buffer ceiling;
- approximately 4 ms of scheduled work per server tick.

These are configurable safety defaults subject to profiling, not universal performance guarantees.

Admission considers volume, chunk count, block-entity density, entities, scheduled ticks, adapter estimates, worst-case storage, free disk space, and memory.

## 8. Transaction protocol

States:

```text
PREPARING → PREPARED → APPLYING → VERIFYING → COMMITTED
                       ↘ ROLLING_BACK → ROLLED_BACK
unprovable safety → FAILED_SAFE
```

`PREPARING` cannot mutate the world. Before `PREPARED`, the runtime acquires the region lock, reserves disk, pins chunks, computes relocation, coordinates autosave, captures the expected current-state digest, writes the complete supported before-image, flushes it, reads it back, and verifies checksums and structure.

Only then is `PREPARED` published durably.

Logical apply order:

1. revalidate isolation and current-state digest;
2. relocate affected players;
3. remove eligible current entities;
4. replace scheduled ticks;
5. prepare replacement of block entities;
6. restore block and fluid states using controlled updates;
7. restore block entities;
8. restore scheduled ticks;
9. recreate entities;
10. restore passenger, leash, and supported ownership links;
11. apply adapter state;
12. reconcile derived state;
13. compare canonical domain digests.

The exact Minecraft-specific ordering must be validated by GameTests before being treated as final implementation behavior.

An entity UUID collision inside the mutation closure is handled by normal replacement. A collision with an external entity never deletes that entity; the restored entity receives a remapped UUID and internal snapshot references are updated. Unresolvable required references trigger rollback.

## 9. Isolation

The runtime pins affected chunks, blocks overlapping Genie transactions, prevents player entry into the mutation boundary, coordinates autosave, and applies mutations on the server thread. Cooperating adapters participate in lifecycle gates.

Strict isolation is guaranteed for vanilla and cooperating adapters. Arbitrary third-party direct mutation cannot be universally locked; detected concurrent changes cause abort or rollback. The system does not claim stronger isolation than it can enforce.

## 10. Verification and outcomes

Canonical state digests normalize ordering without ignoring meaningful values.

Terminal user-visible outcomes are:

- `REJECTED`
- `STALE_PREVIEW`
- `RESOURCE_LIMIT`
- `COMPATIBILITY_BLOCKED`
- `COMPLETED`
- `COMPLETED_WITH_WARNINGS`
- `ROLLED_BACK`
- `FAILED_SAFE`

All required domains must match for commit. Only explicitly previewed and accepted optional differences permit `COMPLETED_WITH_WARNINGS`. Report-only state is never included in a claim that the region was restored.

Genie dialogue may explain a structured outcome in character but cannot alter status, scope, counts, warnings, or guarantees.

## 11. Crash recovery

Each transaction has checksummed metadata, monotonically sequenced journal records, complete before-image shards, target reference, verification artifacts, and a final report.

Recovery behavior:

- incomplete `PREPARING`: no mutation was authorized; quarantine or clean temporary data;
- `PREPARED`, `APPLYING`, or `VERIFYING`: rollback from before-image;
- `ROLLING_BACK`: continue rollback idempotently;
- `ROLLED_BACK`: verify the prior-state digest and close;
- `COMMITTED`: verify commit, report, and undo metadata without automatic rollback;
- corrupt/torn journal tail: use only the last proven record and act conservatively;
- missing or unprovable before-image after mutation authority: `FAILED_SAFE`.

The durability contract covers process crashes, exceptions, disk-full conditions, short writes, detectable torn/corrupt writes, crashes during apply, and repeated crashes during rollback. Physical media loss and irrecoverable filesystem corruption are outside the automatic guarantee and result in `FAILED_SAFE` rather than a false success claim.

## 12. Undo retention

After commit, the before-image becomes retained undo for 24 hours or until the next successful overlapping restore, whichever occurs first. It remains protected while recovery references it and counts against the 512 MiB budget.

Undo is technical restoration of the previous transaction state, not semantic reversal of a wish.

## 13. Player handling

Players are never replaced as ordinary snapshot entities. Before mutation, the runtime finds a safe position outside the AABB, then tries a validated personal anchor and world spawn as controlled fallbacks. Positions must avoid collision, lava, fire, void, world-limit violations, and the mutation boundary.

Failure to find a safe location rejects restore before mutation.

Ordinary restore consent does not authorize changes to player inventory, XP, advancements, attributes, or persistent progression. Those require separate operation types and explicit consent.

## 14. Genie systemic entity model

The wider Genie architecture uses persistent `GenieIdentity` and versioned component state independent of avatar entity NBT. Manifestation, binding, suppression, dimension access, and power authority are orthogonal states rather than one combinatorial enum.

True Genies have no terminal `DEAD` state. Physical, environmental, and ordinary magical-physical damage may affect avatar integrity and cosmetic reactions but cannot terminate identity. Avatar dispersal removes the projection and later remanifests it at a valid anchor.

Real threats act through sealing, contracts, vessel binding, exile, manifestation suppression, dimensional constraints, and wish-authority restrictions.

A vessel binds to `GenieId`, not an avatar entity. Its holder receives wish-request authority, not control of the Genie. Destroying a vessel removes an anchor and may cause instability but not death.

Player transformation is a separate domain transaction using the same preview, explicit confirmation, complete before-image, durable prepare, apply, verify, commit-or-rollback contract. Client cinematics do not define semantic transaction state.

Genie memory is divided into episodic, relational, world-fact, wish-history, personality, and story records. A bounded retrieval layer supplies relevant summaries to the planner. Stored memory is untrusted context and cannot expand capabilities.

Low-level companion behavior remains deterministic. The LLM handles dialogue, wish interpretation, unusual social situations, and high-level planning rather than running every tick.

## 15. Future capability progression

After transactional restore is proven, recommended capability order is:

1. `structure.place`
2. `terrain.transform`
3. `entity.transform`
4. `weather.set`
5. `time.modify`
6. `teleport.safe`
7. `biome.transform`
8. `pocketRealm.open`
9. `conditionalWish.register`

Each capability needs a versioned schema, deterministic risk assessment, declared scope, resource admission, preview, consent policy, executor, verifier, rollback strategy, and compatibility report.

## 16. Testing strategy

Tests verify observable world and durable state, not return values alone.

Pure tests cover AABB arithmetic, serialization, digest stability, strict plan parsing, confirmation binding, quota accounting, state transitions, compatibility classification, UUID remapping, and memory projection limits.

Durable-store tests inject failures before and after writes, flushes, renames, and directory publication, plus short writes, disk-full, corrupt checksums, and torn journal tails. Repeated recovery must be idempotent.

A deterministic fake world adapter verifies stage order, stale previews, lock conflicts, rollback, optional degradation, and `FAILED_SAFE`.

NeoForge GameTests cover blocks, fluids, block entities and inventories, scheduled ticks, entities and passenger graphs, UUID collisions, player relocation, chunk boundaries, world-height limits, and derived-state reconciliation.

LLM contract tests use scripted providers for valid plans, unknown primitives, bad versions, extra fields, oversized plans, unauthorized restore attempts, memory prompt injection, malformed text, timeout, and HTTP failure. Live cloud calls are excluded from automated tests.

## 17. Acceptance criteria

The first slice is complete only when:

- a two-point selection creates a named snapshot;
- the snapshot survives restart;
- capture and preview do not mutate world state;
- restore requires digest-bound, single-use confirmation;
- supported state restores at the original dimension and coordinates;
- affected players relocate safely without progression rollback;
- required mismatches produce verified rollback;
- fault injection covers every documented persistence boundary;
- startup recovery is idempotent;
- deterministic snapshot and restore remain usable without EuroModels;
- the cloud Genie requests preview through the same safe runtime;
- outcome and compatibility reports are independent of LLM wording;
- tests assert observable state;
- unsupported state and limitations are visible to the player;
- pre-existing repository changes remain untouched.

## 18. Delivery sequence

1. Recovery gate and fault-injecting durable storage.
2. Two-point selection and capture-only immutable snapshots.
3. Side-effect-free preview and digest-bound confirmation.
4. Restore transaction, verification, rollback, startup recovery, and retained undo.
5. Deterministic command fallback.
6. EuroModels planner adapter using `EUROMODELS_API_KEY`.
7. Only after the pilot is proven, extract shared capability-kernel abstractions for the next mutation family.
