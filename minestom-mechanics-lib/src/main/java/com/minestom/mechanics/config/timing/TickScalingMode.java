package com.minestom.mechanics.config.timing;

/**
 * Mode for tick-based value scaling across different server TPS.
 *
 * <p>All config values are stored as "base at 20 TPS". At runtime:</p>
 * <ul>
 *   <li><b>SCALED</b> — Values are scaled so real-time durations stay consistent.
 *       Example: 10 ticks at 40 TPS becomes 20 ticks (0.5s either way).</li>
 *   <li><b>UNSCALED</b> — Tick counts stay literal (10 ticks = 10 server ticks
 *       regardless of TPS). At 40 TPS that's 0.25s.</li>
 * </ul>
 */
public enum TickScalingMode {
    /** Scale tick-based values so real-time behavior matches 20 TPS. */
    SCALED,

    /** Use tick counts literally; no scaling. */
    UNSCALED
}
