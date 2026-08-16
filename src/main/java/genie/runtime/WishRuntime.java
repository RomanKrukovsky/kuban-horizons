package genie.runtime;

import genie.entity.GenieEntity;
import genie.parser.WishParser;
import genie.parser.WishIntent;
import genie.util.GenieLogger;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;

/**
 * Main orchestrator for wish execution
 * Handles wish lifecycle: parsing → risk assessment → confirmation → execution → memory
 */
public class WishRuntime {
    private final GenieEntity genie;
    private final SafeStrongWishRuntime safeRuntime;
    private final WishParser wishParser;
    private WishState currentState = WishState.IDLE;
    private String currentWishText = "";
    private WishIntent currentWishIntent;
    private long wishStartTime;
    private long wishTimeout = 3000; // 3 seconds timeout for wish processing

    // State machine
    public enum WishState {
        IDLE,           // Waiting for wish
        PARSING,        // Parsing wish text
        ASSESSING,      // Assessing risk
        CONFIRMING,     // Awaiting confirmation
        PREPARING,      // Preparing execution
        EXECUTING,      // Executing wish
        COMPLETING,     // Completing wish
        ROLLING_BACK,   // Rolling back due to failure
        COMPLETE        // Wish completed
    }

    public WishRuntime(GenieEntity genie) {
        this.genie = genie;
        this.safeRuntime = new SafeStrongWishRuntime(genie);
        this.wishParser = new WishParser();
    }

    /**
     * Process a wish from player
     */
    public WishExecutionResult processWish(Player player, String wishText) {
        if (currentState != WishState.IDLE) {
            return new WishExecutionResult(false, "Genie is busy processing another wish", null);
        }

        this.currentWishText = wishText;
        this.wishStartTime = genie.level().getGameTime();
        this.currentState = WishState.PARSING;

        try {
            // Parse wish
            WishIntent intent = wishParser.parse(wishText);
            this.currentWishIntent = intent;

            GenieLogger.info("Genie " + genie.getName().getString() + " parsing wish: " + wishText);

            // Assess risk
            this.currentState = WishState.ASSESSING;
            WishRiskAssessment risk = assessWishRisk(intent, player);

            if (risk.isCritical()) {
                return new WishExecutionResult(false, "Wish too dangerous: " + risk.getReason(), null);
            }

            // Show preview
            this.currentState = WishState.CONFIRMING;
            PreviewService.PreviewResult preview = showWishPreview(player, intent, risk);

            if (!preview.isConfirmed()) {
                return new WishExecutionResult(false, "Wish not confirmed by player", null);
            }

            // Execute wish
            this.currentState = WishState.EXECUTING;
            WishExecutionResult executionResult = executeWish(player, intent, risk);

            // Complete wish
            this.currentState = WishState.COMPLETING;
            completeWish(executionResult);

            return executionResult;

        } catch (Exception e) {
            GenieLogger.error("Error processing wish: " + e.getMessage());
            rollbackWish();
            return new WishExecutionResult(false, "Wish failed: " + e.getMessage(), null);
        } finally {
            this.currentState = WishState.IDLE;
            this.currentWishText = "";
            this.currentWishIntent = null;
        }
    }

    /**
     * Assess risk of a wish
     */
    private WishRiskAssessment assessWishRisk(WishIntent intent, Player player) {
        WishRiskAssessment assessment = new WishRiskAssessment();

        // Check wish type
        switch (intent.getWishType()) {
            case LITERAL:
                assessment.addRisk(0.7f, "Literal wish execution");
                break;
            case CONDITIONAL:
                assessment.addRisk(0.4f, "Conditional wish with complex logic");
                break;
            case WORDLESS:
                assessment.addRisk(0.3f, "Wordless wish requires high proximity");
                break;
            case GENERAL:
                assessment.addRisk(0.2f, "General wish");
                break;
        }

        // Check wish complexity
        int complexity = intent.getComplexity();
        if (complexity > 50) {
            assessment.addRisk(0.5f, "High complexity wish (score: " + complexity + ")");
        }

        // Check player proximity
        if (player != null) {
            double distance = genie.distanceToSqr(player);
            if (distance > 16.0) {
                assessment.addRisk(0.3f, "Player too far away (" + String.format("%.1f", distance) + " blocks)");
            }
        }

        // Check genie state
        if (genie.getHealth() / genie.getMaxHealth() < 0.3f) {
            assessment.addRisk(0.4f, "Genie low on health");
        }

        // Check world state
        if (genie.level() instanceof ServerLevel serverLevel) {
            if (serverLevel.getDifficulty().getId() >= 2) { // Hard difficulty
                assessment.addRisk(0.2f, "Hard difficulty increases risk");
            }
        }

        return assessment;
    }

    /**
     * Show wish preview to player
     */
    private PreviewService.PreviewResult showWishPreview(Player player, WishIntent intent, WishRiskAssessment risk) {
        PreviewService previewService = new PreviewService(genie);
        return previewService.showPreview(player, intent, risk);
    }

    /**
     * Execute the wish
     */
    private WishExecutionResult executeWish(Player player, WishIntent intent, WishRiskAssessment risk) {
        try {
            // Take snapshot before execution
            safeRuntime.takeSnapshot("pre-wish-" + System.currentTimeMillis());

            // Execute based on wish type
            WishExecutor executor = new WishExecutor(genie);
            WishExecutionResult result = executor.execute(intent, player);

            // Record to causality ledger
            recordWishToLedger(intent, player, result.isSuccess());

            // Add to world memory
            recordWishToMemory(intent, player, result);

            return result;

        } catch (Exception e) {
            GenieLogger.error("Wish execution failed: " + e.getMessage());
            return new WishExecutionResult(false, "Execution error: " + e.getMessage(), null);
        }
    }

    /**
     * Complete wish processing
     */
    private void completeWish(WishExecutionResult result) {
        if (result.isSuccess()) {
            genie.addExperience(50); // Reward for successful wish
            genie.sendMessageToOwner("Wish granted! ✨");
        } else {
            genie.sendMessageToOwner("Wish failed: " + result.getMessage());
        }
    }

    /**
     * Rollback wish if something went wrong
     */
    private void rollbackWish() {
        this.currentState = WishState.ROLLING_BACK;
        try {
            safeRuntime.rollbackLastSnapshot();
            GenieLogger.info("Wish rolled back successfully");
        } catch (Exception e) {
            GenieLogger.error("Failed to rollback wish: " + e.getMessage());
        } finally {
            this.currentState = WishState.IDLE;
        }
    }

    /**
     * Record wish to causality ledger
     */
    private void recordWishToLedger(WishIntent intent, @Nullable Player player, boolean success) {
        CausalityLedger ledger = genie.getCausalityLedger();
        if (ledger != null) {
            ledger.addEntry(
                genie.level().getGameTime(),
                player != null ? player.getUUID() : null,
                intent.getWishType().name(),
                intent.getOriginalText(),
                success,
                genie.blockPosition()
            );
        }
    }

    /**
     * Record wish to world memory
     */
    private void recordWishToMemory(WishIntent intent, @Nullable Player player, WishExecutionResult result) {
        WorldGenieMemory memory = genie.getWorldMemory();
        if (memory != null) {
            memory.recordWishEvent(
                player != null ? player.getUUID() : null,
                intent.getOriginalText(),
                result.isSuccess(),
                genie.blockPosition(),
                result.getResultDescription()
            );
        }
    }

    /**
     * Get current wish state
     */
    public WishState getCurrentState() {
        return currentState;
    }

    /**
     * Check if genie is processing a wish
     */
    public boolean isProcessingWish() {
        return currentState != WishState.IDLE &&
               currentState != WishState.COMPLETE &&
               currentState != WishState.ROLLING_BACK;
    }

    /**
     * Get current wish text
     */
    public String getCurrentWishText() {
        return currentWishText;
    }

    /**
     * Get time remaining for wish processing
     */
    public long getWishTimeout() {
        return wishTimeout;
    }

    /**
     * Set wish timeout
     */
    public void setWishTimeout(long timeout) {
        this.wishTimeout = timeout;
    }

    /**
     * Wish execution result container
     */
    public static class WishExecutionResult {
        private final boolean success;
        private final String message;
        private final Object resultData;

        public WishExecutionResult(boolean success, String message, Object resultData) {
            this.success = success;
            this.message = message;
            this.resultData = resultData;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public Object getResultData() {
            return resultData;
        }

        public String getResultDescription() {
            return success ? "Wish granted successfully" : "Wish failed: " + message;
        }
    }

    /**
     * Wish risk assessment container
     */
    public static class WishRiskAssessment {
        private float totalRisk = 0.0f;
        private String criticalReason = "";

        public void addRisk(float amount, String reason) {
            this.totalRisk += amount;
            if (amount >= 0.7f && criticalReason.isEmpty()) {
                this.criticalReason = reason;
            }
        }

        public float getTotalRisk() {
            return totalRisk;
        }

        public boolean isCritical() {
            return totalRisk >= 1.0f || !criticalReason.isEmpty();
        }

        public String getReason() {
            return criticalReason.isEmpty() ? "High risk wish" : criticalReason;
        }
    }
}