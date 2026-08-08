# Recovery Journal Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the first production code slice of the Safe Strong-Wish Runtime: immutable recovery records and a checksummed, durable append-only journal that reconstructs the last proven transaction facts after torn or corrupt writes.

**Architecture:** Keep the first slice independent of Minecraft world APIs so durability behavior can be tested deterministically. `RecoveryJournal` owns canonical binary framing, complete writes, `FileChannel.force(true)`, checksum verification, bounded parsing, and proven-prefix recovery; later startup recovery and snapshot publication consume this API without gaining low-level write access.

**Tech Stack:** Java 25, Gradle 9.2.1, JUnit Jupiter 5.13.4, Java NIO `FileChannel`, SHA-256.

## Global Constraints

- Project root: `/Users/romanmolodyko/Documents/kuban-horizon`.
- Target Minecraft 26.2, NeoForge 26.2.0.48-beta, and Java 25.
- Preserve all pre-existing dirty and untracked files; do not modify them.
- Do not touch `KHGameTests.java`, `KubanHorizons.java`, `KubanGenie.java`, `PlayerGenieAttachment.java`, project-status documents, or existing untracked Genie files.
- Do not expose arbitrary code, Minecraft world handles, mutable collections, secrets, or filesystem internals through recovery-domain APIs.
- The first invalid journal frame terminates the proven prefix; scanning never skips corruption or repairs the file.
- Sequence ordering, not wall-clock time, establishes durable transaction order.
- Cap all untrusted lengths before allocation.
- Do not commit unless the user explicitly requests it.
- This plan intentionally covers only delivery-sequence item 1's journal foundation. Startup wiring, recovery gates, snapshot shards, manifest-last publication, world capture, restore, confirmation, and EuroModels integration require subsequent plans.

## File Map

- Modify `build.gradle`: add the isolated JUnit 5 test lane.
- Create `src/main/java/dev/romankrukovsky/kubanhorizons/genie/runtime/recovery/TransactionState.java`: durable transaction vocabulary.
- Create `src/main/java/dev/romankrukovsky/kubanhorizons/genie/runtime/recovery/RecoveryRecord.java`: validated immutable journal fact.
- Create `src/main/java/dev/romankrukovsky/kubanhorizons/genie/runtime/recovery/RecoveryScan.java`: immutable proven-prefix result.
- Create `src/main/java/dev/romankrukovsky/kubanhorizons/genie/runtime/recovery/RecoveryJournal.java`: framing, durable append, and conservative scan.
- Create `src/test/java/dev/romankrukovsky/kubanhorizons/genie/runtime/recovery/RecoveryRecordTest.java`: domain invariant tests.
- Create `src/test/java/dev/romankrukovsky/kubanhorizons/genie/runtime/recovery/RecoveryJournalTest.java`: persistence, corruption, and failure-injection tests.

---

### Task 1: Establish the Pure Java Test Lane

**Files:**
- Modify: `build.gradle:90-96`

**Interfaces:**
- Consumes: existing Gradle Java and NeoForge configuration.
- Produces: JUnit Jupiter tests runnable with `./gradlew test`.

- [ ] **Step 1: Add a failing smoke test in the future recovery package**

Create `src/test/java/dev/romankrukovsky/kubanhorizons/genie/runtime/recovery/RecoveryRecordTest.java` temporarily with:

```java
package dev.romankrukovsky.kubanhorizons.genie.runtime.recovery;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class RecoveryRecordTest {
    @Test
    void junitRuns() {
        assertTrue(true);
    }
}
```

- [ ] **Step 2: Run the focused test before configuring JUnit**

Run:

```bash
./gradlew test --tests 'dev.romankrukovsky.kubanhorizons.genie.runtime.recovery.RecoveryRecordTest'
```

Expected: FAIL because JUnit Jupiter is not on the test compile/runtime classpath.

- [ ] **Step 3: Configure JUnit Jupiter**

Extend the dependencies and test task in `build.gradle`:

```groovy
dependencies {
    implementation "maven.modrinth:geckolib:${geckolib_version}"

    testImplementation platform('org.junit:junit-bom:5.13.4')
    testImplementation 'org.junit.jupiter:junit-jupiter'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
}

tasks.named('test', Test).configure {
    useJUnitPlatform()
}
```

Do not alter the existing NeoForge or GeckoLib declarations.

- [ ] **Step 4: Run the focused test**

Run:

```bash
./gradlew test --tests 'dev.romankrukovsky.kubanhorizons.genie.runtime.recovery.RecoveryRecordTest'
```

Expected: PASS, one test executed.

- [ ] **Step 5: Inspect the diff**

Run:

```bash
git diff -- build.gradle src/test/java/dev/romankrukovsky/kubanhorizons/genie/runtime/recovery/RecoveryRecordTest.java
```

Expected: only JUnit configuration and the smoke test appear; no pre-existing dirty file is altered.

### Task 2: Define Immutable Recovery Domain Types

**Files:**
- Create: `src/main/java/dev/romankrukovsky/kubanhorizons/genie/runtime/recovery/TransactionState.java`
- Create: `src/main/java/dev/romankrukovsky/kubanhorizons/genie/runtime/recovery/RecoveryRecord.java`
- Create: `src/main/java/dev/romankrukovsky/kubanhorizons/genie/runtime/recovery/RecoveryScan.java`
- Replace: `src/test/java/dev/romankrukovsky/kubanhorizons/genie/runtime/recovery/RecoveryRecordTest.java`

**Interfaces:**
- Consumes: Java `UUID`, `Instant`, `List`, and `Optional`.
- Produces:
  - `TransactionState` enum with `PREPARING`, `PREPARED`, `APPLYING`, `VERIFYING`, `ROLLING_BACK`, `COMMITTED`, `ROLLED_BACK`, `FAILED_SAFE`.
  - `RecoveryRecord(UUID transactionId, long sequence, Instant recordedAt, TransactionState state, String payloadDigest)`.
  - `RecoveryScan(List<RecoveryRecord> provenRecords, boolean discardedInvalidTail)` and `Optional<RecoveryRecord> lastProvenRecord()`.

- [ ] **Step 1: Replace the smoke test with invariant tests**

Use:

```java
package dev.romankrukovsky.kubanhorizons.genie.runtime.recovery;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecoveryRecordTest {
    private static final String DIGEST = "0".repeat(64);

    @Test
    void acceptsCanonicalRecord() {
        UUID transactionId = UUID.randomUUID();
        Instant recordedAt = Instant.parse("2026-08-08T12:00:00Z");

        RecoveryRecord record = new RecoveryRecord(
                transactionId,
                0,
                recordedAt,
                TransactionState.PREPARING,
                DIGEST
        );

        assertEquals(transactionId, record.transactionId());
        assertEquals(0, record.sequence());
        assertEquals(recordedAt, record.recordedAt());
        assertEquals(TransactionState.PREPARING, record.state());
        assertEquals(DIGEST, record.payloadDigest());
    }

    @Test
    void rejectsInvalidFields() {
        UUID transactionId = UUID.randomUUID();
        Instant recordedAt = Instant.EPOCH;

        assertThrows(NullPointerException.class, () ->
                new RecoveryRecord(null, 0, recordedAt, TransactionState.PREPARING, DIGEST));
        assertThrows(IllegalArgumentException.class, () ->
                new RecoveryRecord(transactionId, -1, recordedAt, TransactionState.PREPARING, DIGEST));
        assertThrows(NullPointerException.class, () ->
                new RecoveryRecord(transactionId, 0, null, TransactionState.PREPARING, DIGEST));
        assertThrows(NullPointerException.class, () ->
                new RecoveryRecord(transactionId, 0, recordedAt, null, DIGEST));
        assertThrows(IllegalArgumentException.class, () ->
                new RecoveryRecord(transactionId, 0, recordedAt, TransactionState.PREPARING, "A".repeat(64)));
        assertThrows(IllegalArgumentException.class, () ->
                new RecoveryRecord(transactionId, 0, recordedAt, TransactionState.PREPARING, "0".repeat(63)));
    }

    @Test
    void recoveryScanDefensivelyCopiesRecords() {
        RecoveryRecord record = new RecoveryRecord(
                UUID.randomUUID(), 0, Instant.EPOCH, TransactionState.PREPARING, DIGEST);
        ArrayList<RecoveryRecord> mutable = new ArrayList<>(List.of(record));

        RecoveryScan scan = new RecoveryScan(mutable, false);
        mutable.clear();

        assertEquals(List.of(record), scan.provenRecords());
        assertEquals(record, scan.lastProvenRecord().orElseThrow());
        assertThrows(UnsupportedOperationException.class, () -> scan.provenRecords().clear());
        assertFalse(scan.discardedInvalidTail());
    }

    @Test
    void emptyScanHasNoLastRecord() {
        RecoveryScan scan = new RecoveryScan(List.of(), true);

        assertTrue(scan.lastProvenRecord().isEmpty());
        assertTrue(scan.discardedInvalidTail());
    }
}
```

- [ ] **Step 2: Run tests to verify missing production types**

Run:

```bash
./gradlew test --tests 'dev.romankrukovsky.kubanhorizons.genie.runtime.recovery.RecoveryRecordTest'
```

Expected: FAIL to compile because the three production types do not exist.

- [ ] **Step 3: Add `TransactionState`**

```java
package dev.romankrukovsky.kubanhorizons.genie.runtime.recovery;

public enum TransactionState {
    PREPARING,
    PREPARED,
    APPLYING,
    VERIFYING,
    ROLLING_BACK,
    COMMITTED,
    ROLLED_BACK,
    FAILED_SAFE
}
```

- [ ] **Step 4: Add `RecoveryRecord` with strict invariants**

```java
package dev.romankrukovsky.kubanhorizons.genie.runtime.recovery;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

public record RecoveryRecord(
        UUID transactionId,
        long sequence,
        Instant recordedAt,
        TransactionState state,
        String payloadDigest
) {
    private static final Pattern SHA_256 = Pattern.compile("[0-9a-f]{64}");

    public RecoveryRecord {
        Objects.requireNonNull(transactionId, "transactionId");
        Objects.requireNonNull(recordedAt, "recordedAt");
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(payloadDigest, "payloadDigest");
        if (sequence < 0) {
            throw new IllegalArgumentException("sequence must not be negative");
        }
        if (!SHA_256.matcher(payloadDigest).matches()) {
            throw new IllegalArgumentException("payloadDigest must be lowercase SHA-256 hex");
        }
    }
}
```

- [ ] **Step 5: Add `RecoveryScan` with defensive copying**

```java
package dev.romankrukovsky.kubanhorizons.genie.runtime.recovery;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record RecoveryScan(
        List<RecoveryRecord> provenRecords,
        boolean discardedInvalidTail
) {
    public RecoveryScan {
        provenRecords = List.copyOf(Objects.requireNonNull(provenRecords, "provenRecords"));
    }

    public Optional<RecoveryRecord> lastProvenRecord() {
        if (provenRecords.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(provenRecords.getLast());
    }
}
```

- [ ] **Step 6: Run focused domain tests**

Run:

```bash
./gradlew test --tests 'dev.romankrukovsky.kubanhorizons.genie.runtime.recovery.RecoveryRecordTest'
```

Expected: PASS, four tests executed.

### Task 3: Implement Canonical Durable Journal Append and Round Trip

**Files:**
- Create: `src/main/java/dev/romankrukovsky/kubanhorizons/genie/runtime/recovery/RecoveryJournal.java`
- Create: `src/test/java/dev/romankrukovsky/kubanhorizons/genie/runtime/recovery/RecoveryJournalTest.java`

**Interfaces:**
- Consumes: the types from Task 2.
- Produces:
  - `public RecoveryJournal(Path file)`.
  - package-private `RecoveryJournal(Path file, Durability durability)` for tests.
  - `public void append(RecoveryRecord record) throws IOException`.
  - `public RecoveryScan scan() throws IOException`.
  - nested package-private `@FunctionalInterface Durability { void force(FileChannel channel) throws IOException; }`.

- [ ] **Step 1: Write failing round-trip and force tests**

Start `RecoveryJournalTest` with:

```java
package dev.romankrukovsky.kubanhorizons.genie.runtime.recovery;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RecoveryJournalTest {
    private static final String DIGEST = "1".repeat(64);

    @TempDir
    Path tempDirectory;

    @Test
    void appendThenReopenAndScanReturnsEquivalentRecord() throws IOException {
        Path journalPath = tempDirectory.resolve("transaction.journal");
        RecoveryRecord record = record(0, TransactionState.PREPARING);

        new RecoveryJournal(journalPath).append(record);
        RecoveryScan scan = new RecoveryJournal(journalPath).scan();

        assertEquals(List.of(record), scan.provenRecords());
        assertFalse(scan.discardedInvalidTail());
    }

    @Test
    void multipleAppendsRemainInPhysicalOrder() throws IOException {
        Path journalPath = tempDirectory.resolve("transaction.journal");
        RecoveryRecord first = record(0, TransactionState.PREPARING);
        RecoveryRecord second = new RecoveryRecord(
                first.transactionId(), 1, Instant.EPOCH.plusSeconds(1),
                TransactionState.PREPARED, DIGEST);
        RecoveryJournal journal = new RecoveryJournal(journalPath);

        journal.append(first);
        journal.append(second);

        assertEquals(List.of(first, second), new RecoveryJournal(journalPath).scan().provenRecords());
    }

    @Test
    void appendForcesDataBeforeReturning() throws IOException {
        AtomicInteger forceCalls = new AtomicInteger();
        RecoveryJournal journal = new RecoveryJournal(
                tempDirectory.resolve("transaction.journal"),
                channel -> forceCalls.incrementAndGet()
        );

        journal.append(record(0, TransactionState.PREPARING));

        assertEquals(1, forceCalls.get());
    }

    @Test
    void durabilityFailurePropagates() {
        RecoveryJournal journal = new RecoveryJournal(
                tempDirectory.resolve("transaction.journal"),
                channel -> { throw new IOException("injected force failure"); }
        );

        IOException failure = assertThrows(IOException.class, () ->
                journal.append(record(0, TransactionState.PREPARING)));

        assertEquals("injected force failure", failure.getMessage());
    }

    private static RecoveryRecord record(long sequence, TransactionState state) {
        return new RecoveryRecord(
                UUID.fromString("67e50dce-2871-4ae7-b32f-753bd00a77a8"),
                sequence,
                Instant.EPOCH.plusSeconds(sequence),
                state,
                DIGEST
        );
    }
}
```

- [ ] **Step 2: Run tests to verify `RecoveryJournal` is missing**

Run:

```bash
./gradlew test --tests 'dev.romankrukovsky.kubanhorizons.genie.runtime.recovery.RecoveryJournalTest'
```

Expected: FAIL to compile because `RecoveryJournal` does not exist.

- [ ] **Step 3: Implement the framing constants and constructors**

Create `RecoveryJournal` with these exact constants and seam:

```java
public final class RecoveryJournal {
    private static final int MAGIC = 0x4B48524A;
    private static final short FORMAT_VERSION = 1;
    private static final int CHECKSUM_BYTES = 32;
    private static final int HEADER_BYTES = Integer.BYTES + Short.BYTES + Integer.BYTES;
    private static final int MAX_PAYLOAD_BYTES = 4 * 1024;

    private final Path file;
    private final Durability durability;

    public RecoveryJournal(Path file) {
        this(file, channel -> channel.force(true));
    }

    RecoveryJournal(Path file, Durability durability) {
        this.file = Objects.requireNonNull(file, "file");
        this.durability = Objects.requireNonNull(durability, "durability");
    }

    @FunctionalInterface
    interface Durability {
        void force(FileChannel channel) throws IOException;
    }
}
```

- [ ] **Step 4: Implement canonical payload encoding**

Use a fixed order and bounded UTF-8 digest field:

```java
private static byte[] encode(RecoveryRecord record) throws IOException {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    try (DataOutputStream output = new DataOutputStream(bytes)) {
        output.writeLong(record.transactionId().getMostSignificantBits());
        output.writeLong(record.transactionId().getLeastSignificantBits());
        output.writeLong(record.sequence());
        output.writeLong(record.recordedAt().getEpochSecond());
        output.writeInt(record.recordedAt().getNano());
        output.writeByte(record.state().ordinal());
        byte[] digest = record.payloadDigest().getBytes(StandardCharsets.US_ASCII);
        output.writeByte(digest.length);
        output.write(digest);
    }
    byte[] payload = bytes.toByteArray();
    if (payload.length > MAX_PAYLOAD_BYTES) {
        throw new IOException("Recovery record exceeds maximum payload size");
    }
    return payload;
}
```

- [ ] **Step 5: Implement SHA-256 and complete writes**

```java
private static byte[] checksum(byte[] payload) {
    try {
        return MessageDigest.getInstance("SHA-256").digest(payload);
    } catch (NoSuchAlgorithmException exception) {
        throw new IllegalStateException("SHA-256 is unavailable", exception);
    }
}

private static void writeFully(FileChannel channel, ByteBuffer buffer) throws IOException {
    while (buffer.hasRemaining()) {
        int written = channel.write(buffer);
        if (written < 0) {
            throw new EOFException("Journal channel closed during write");
        }
        if (written == 0) {
            Thread.onSpinWait();
        }
    }
}
```

- [ ] **Step 6: Implement durable append**

`append` must create parent directories, open with `CREATE`, `WRITE`, and `APPEND`, write one complete frame, and force before returning:

```java
public void append(RecoveryRecord record) throws IOException {
    Objects.requireNonNull(record, "record");
    Path parent = file.toAbsolutePath().getParent();
    if (parent != null) {
        Files.createDirectories(parent);
    }
    byte[] payload = encode(record);
    byte[] checksum = checksum(payload);
    ByteBuffer frame = ByteBuffer.allocate(HEADER_BYTES + payload.length + CHECKSUM_BYTES)
            .order(ByteOrder.BIG_ENDIAN);
    frame.putInt(MAGIC);
    frame.putShort(FORMAT_VERSION);
    frame.putInt(payload.length);
    frame.put(payload);
    frame.put(checksum);
    frame.flip();
    try (FileChannel channel = FileChannel.open(
            file, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND)) {
        writeFully(channel, frame);
        durability.force(channel);
    }
}
```

- [ ] **Step 7: Implement strict payload decoding and basic scan**

`decode` must reject trailing bytes, invalid state ordinals, non-64-byte digest fields, invalid nanoseconds, malformed UUID/time data, and any constructor rejection by wrapping it in `IOException`.

`scan` must return `new RecoveryScan(List.of(), false)` for a missing or empty journal; otherwise read one header, payload, and checksum at a time. It must only add a record after all three parts and checksum validate. Sequence validation belongs to Task 4.

Use `MessageDigest.isEqual(expected, checksum(payload))` for checksum comparison and `readFully` that returns `false` only when EOF occurs before the requested buffer fills.

- [ ] **Step 8: Run round-trip tests**

Run:

```bash
./gradlew test --tests 'dev.romankrukovsky.kubanhorizons.genie.runtime.recovery.RecoveryJournalTest'
```

Expected: PASS, four tests executed.

### Task 4: Prove Conservative Recovery from Torn and Corrupt Tails

**Files:**
- Modify: `src/main/java/dev/romankrukovsky/kubanhorizons/genie/runtime/recovery/RecoveryJournal.java`
- Modify: `src/test/java/dev/romankrukovsky/kubanhorizons/genie/runtime/recovery/RecoveryJournalTest.java`

**Interfaces:**
- Consumes: Task 3 journal framing.
- Produces: `scan()` that returns only a contiguous proven prefix and sets `discardedInvalidTail=true` at the first invalid frame.

- [ ] **Step 1: Add tests for each torn-frame boundary**

Add a parameterized test using `@ValueSource(ints = {1, 5, 9, 12, 40})`. Create two valid records, preserve the first frame length, truncate the file at `firstFrameLength + cutBytes`, then assert:

```java
assertEquals(List.of(first), scan.provenRecords());
assertTrue(scan.discardedInvalidTail());
```

Expose no production frame helpers. In the test, compute the first frame size from the known format:

```java
private static final int PAYLOAD_BYTES = 16 + 8 + 8 + 4 + 1 + 1 + 64;
private static final int FRAME_BYTES = 4 + 2 + 4 + PAYLOAD_BYTES + 32;
```

Use `FileChannel.truncate(FRAME_BYTES + cutBytes)` after appending both records.

- [ ] **Step 2: Add checksum-corruption test**

Append two records, flip one byte in the second payload at offset `FRAME_BYTES + 10`, reopen, and assert that only the first record is proven and the invalid-tail flag is true.

- [ ] **Step 3: Add format and length rejection tests**

Create files whose first frame has:

- correct magic and unsupported format version `2`;
- correct magic/version and payload length `MAX_PAYLOAD_BYTES + 1`;
- incorrect magic.

For each, assert an empty proven prefix and `discardedInvalidTail=true`. Keep `MAX_PAYLOAD_BYTES` private in production; use the known design limit `4 * 1024` in tests.

- [ ] **Step 4: Add sequence monotonicity tests**

Append records for the same transaction with sequences `0, 1, 1` and separately `0, 2, 1`. Assert the proven prefixes stop before the duplicate or decreasing record.

Also append interleaved records for two transaction UUIDs with sequences `A0, B0, A1, B1` and assert all four are proven. Sequence is tracked independently per transaction.

- [ ] **Step 5: Add idempotent damaged-scan test**

Corrupt a second frame, invoke `scan()` twice with fresh `RecoveryJournal` instances, and assert the two `RecoveryScan` values are equal and file size is unchanged.

- [ ] **Step 6: Run tests to expose incomplete scan behavior**

Run:

```bash
./gradlew test --tests 'dev.romankrukovsky.kubanhorizons.genie.runtime.recovery.RecoveryJournalTest'
```

Expected: FAIL in torn-tail, corruption, version, length, magic, or sequence cases until strict scanning is complete.

- [ ] **Step 7: Implement proven-prefix scanning**

Update `scan()` so that:

1. EOF exactly at a frame boundary returns the records with `discardedInvalidTail=false`.
2. Partial header, payload, or checksum returns the records with `true`.
3. Bad magic, unsupported version, invalid or oversized length, checksum mismatch, decode failure, duplicate sequence, or decreasing sequence returns the records with `true`.
4. It stops immediately at the first invalid frame.
5. It never writes, truncates, renames, or repairs the journal.

Track sequence with:

```java
Map<UUID, Long> lastSequences = new HashMap<>();
Long previous = lastSequences.get(record.transactionId());
if (previous != null && record.sequence() <= previous) {
    return new RecoveryScan(records, true);
}
lastSequences.put(record.transactionId(), record.sequence());
```

To distinguish clean EOF from a partial header, read the first byte separately. If it is absent, scanning ended cleanly; if present, fill the rest of the header and flag invalid tail if incomplete.

- [ ] **Step 8: Run the complete focused suite**

Run:

```bash
./gradlew test --tests 'dev.romankrukovsky.kubanhorizons.genie.runtime.recovery.*'
```

Expected: all recovery tests PASS.

### Task 5: Verify the First Slice and Preserve Repository State

**Files:**
- Verify only; no additional files.

**Interfaces:**
- Consumes: Tasks 1-4.
- Produces: verified recovery-journal foundation without modifying user-owned dirty files.

- [ ] **Step 1: Run all pure unit tests**

Run:

```bash
./gradlew test
```

Expected: BUILD SUCCESSFUL; all JUnit tests pass.

- [ ] **Step 2: Compile production sources**

Run:

```bash
./gradlew compileJava
```

Expected: BUILD SUCCESSFUL under Java 25.

- [ ] **Step 3: Run existing NeoForge GameTests**

Run:

```bash
./gradlew runGameTestServer
```

Expected: process exits successfully and existing registered GameTests pass. If the repository's task name differs, run `./gradlew tasks --all`, locate the ModDev `gameTestServer` run task, and use the exact listed name; report the adjustment rather than claiming the skipped command passed.

- [ ] **Step 4: Inspect only intended changes**

Run:

```bash
git status --short
git diff -- build.gradle \
  src/main/java/dev/romankrukovsky/kubanhorizons/genie/runtime/recovery \
  src/test/java/dev/romankrukovsky/kubanhorizons/genie/runtime/recovery
```

Expected new/modified implementation paths:

```text
 M build.gradle
?? src/main/java/dev/romankrukovsky/kubanhorizons/genie/runtime/recovery/
?? src/test/java/dev/romankrukovsky/kubanhorizons/genie/runtime/recovery/
?? docs/superpowers/plans/2026-08-08-recovery-journal-foundation.md
```

The pre-existing dirty paths remain dirty but show no new task-related edits.

- [ ] **Step 5: Report exact results and remaining scope**

Report each command, exit code, test count where available, and any warning or blocker. State explicitly that this slice proves journal durability and conservative scanning only; it does not yet implement snapshot capture, world restore, startup recovery wiring, or LLM planning.
