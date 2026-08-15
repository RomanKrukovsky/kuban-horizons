package dev.romankrukovsky.kubanhorizons.client.util;

/**
 * Canonical color palette for the Kuban Horizons client UI.
 *
 * <p>This class is the single source of truth for all color tokens used across the mod's
 * graphical interfaces, including custom screens, HUD elements, and widget rendering.
 * Colors are derived from the project's ART_BIBLE and the blue-violet magical signature
 * theme (GENIE_VISION).
 *
 * <p>Color tokens are organized into semantic groups:
 * <ul>
 *   <li><b>Magical/Genie signature</b> – The core blue-violet palette establishing the mystical aesthetic</li>
 *   <li><b>Risk levels</b> – Traffic-light indicators for confirmation and warning dialogs</li>
 *   <li><b>Text/status</b> – Standardized feedback colors for operation states</li>
 *   <li><b>Focus indication</b> – Subtle highlighting for keyboard/gamepad navigation</li>
 * </ul>
 *
 * <p>All values are ARGB integers in {@code 0xAARRGGBB} format, ready for direct use with
 * Minecraft's rendering pipeline and immediate-mode UI frameworks. The class is intentionally
 * non-instantiable; consumers should reference the public constants directly.
 *
 * <p>Best practice: Never hardcode color literals elsewhere in the codebase. Always import
 * and use {@code KHColors.CONSTANT_NAME} so that global theme adjustments remain localized
 * to this file.
 */
public final class KHColors {
    private KHColors() {}

    // Magical / Genie signature (blue-violet)

    /**
     * Deepest background layer for magical panels and overlays.
     * <p>Usage context: Root container backgrounds in Genie-themed screens (e.g., PocketConfirmScreen,
     * spell selection UIs). Provides the "night sky" foundation that makes higher layers pop.
     * The alpha channel is set high (0xF0) to allow slight translucency over world geometry.
     */
    public static final int MAGIC_DARK = 0xF0090611;

    /**
     * Primary panel surface color for floating magical windows and dialog boxes.
     * <p>Usage context: The main background fill for any Genie-style container. Used as the
     * immediate parent surface on top of {@link #MAGIC_DARK}. Slightly lighter to create
     * depth while remaining within the blue-violet signature range.
     */
    public static final int MAGIC_PANEL = 0xF01A1026;

    /**
     * Secondary accent surface used for inner frames, separators, and nested containers.
     * <p>Usage context: Creates visual layering inside panels (e.g., content wells, list item
     * backgrounds, or inset scroll regions). Applied as a one-step lighter value than
     * {@link #MAGIC_PANEL} to establish clear hierarchy without breaking the mystical tone.
     */
    public static final int MAGIC_ACCENT = 0xF02A183A;

    /**
     * Signature gold highlight color for interactive and selected states.
     * <p>Usage context: Hover, focus, and selection feedback across all magical UI components.
     * Used for button highlights, selected list entries, active spell icons, and any element
     * that needs to draw the player's eye. Contrasts strongly against the purple backgrounds.
     */
    public static final int MAGIC_GOLD = 0xFFE2B84D;

    /**
     * Default label and body text color within the magical theme.
     * <p>Usage context: Primary text color for non-interactive labels, descriptions, and
     * flavor text inside Genie-themed screens. Provides sufficient contrast on
     * {@link #MAGIC_PANEL} while maintaining the subdued, arcane atmosphere.
     */
    public static final int MAGIC_PURPLE = 0xFF59406F;

    // Risk levels (PocketConfirmScreen)

    /**
     * Low-risk / safe state indicator.
     * <p>Usage context: PocketConfirmScreen and similar confirmation dialogs. Displayed when
     * an action is considered safe or beneficial (green traffic-light semantics). Should be
     * used only for positive or neutral outcomes to preserve color-meaning consistency.
     */
    public static final int RISK_LOW = 0xFF2E7D32;

    /**
     * Medium-risk / caution state indicator.
     * <p>Usage context: PocketConfirmScreen warning states. Applied when an action carries
     * moderate consequences or requires careful consideration (amber traffic-light semantics).
     * Used for reversible but non-trivial operations.
     */
    public static final int RISK_MEDIUM = 0xFFF9A825;

    /**
     * High-risk / danger state indicator.
     * <p>Usage context: PocketConfirmScreen destructive action confirmation. Signals
     * irreversible or highly consequential operations (red traffic-light semantics).
     * Should be paired with explicit warning text and, when possible, a second confirmation step.
     */
    public static final int RISK_HIGH = 0xFFC62828;

    // Text / status

    /**
     * Neutral waiting / pending state text color.
     * <p>Usage context: Status labels indicating an operation is in progress or awaiting
     * external input (e.g., "Connecting…", "Loading…"). The muted gray ensures it does not
     * compete visually with success or error states.
     */
    public static final int TEXT_WAITING = 0xFF888888;

    /**
     * Positive outcome / success feedback text color.
     * <p>Usage context: Confirmation messages after successful operations (spell learned,
     * item crafted, connection established). Provides clear positive reinforcement while
     * remaining readable on dark magical backgrounds.
     */
    public static final int TEXT_SUCCESS = 0xFF66BB6A;

    /**
     * Error / failure feedback text color.
     * <p>Usage context: Error messages, validation failures, and negative outcomes. Used in
     * toast notifications, form error labels, and failure states in magical UI flows.
     * Distinct from {@link #RISK_HIGH} to allow simultaneous display of risk level and error text.
     */
    public static final int TEXT_ERROR = 0xFFEF5350;

    // Focus ring (subtle)

    /**
     * Subtle focus ring color for keyboard and controller navigation.
     * <p>Usage context: Drawn as a thin outline around the currently focused widget when
     * the player navigates via keyboard or gamepad. The 50% alpha (0x80) keeps the ring
     * visible without overpowering the underlying element. Uses {@link #MAGIC_GOLD} at
     * reduced opacity to maintain thematic consistency.
     */
    public static final int FOCUS_RING = 0x80E2B84D;

    // OwnerDeathChoiceScreen button accents (subtle, distinct per choice)

    /**
     * Subtle green accent for the "Resurrect Owner" button.
     * <p>Usage context: OwnerDeathChoiceScreen. Represents restoration of life.
     */
    public static final int DEATH_RESURRECT = 0xFF4A7043;

    /**
     * Subtle violet accent for the "Save Soul" button.
     * <p>Usage context: OwnerDeathChoiceScreen. Represents preservation of essence.
     */
    public static final int DEATH_SAVE_SOUL = 0xFF6B4C9A;

    /**
     * Subtle amber accent for the "Rollback Last Wish" button.
     * <p>Usage context: OwnerDeathChoiceScreen. Represents temporal reversal.
     */
    public static final int DEATH_ROLLBACK = 0xFF8B6F3D;

    /**
     * Subtle teal accent for the "Respawn Free" button.
     * <p>Usage context: OwnerDeathChoiceScreen. Represents liberation / new beginning.
     */
    public static final int DEATH_RESPAWN = 0xFF3D7A8B;
}