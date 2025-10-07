package com.example.tinyhunt.game;

/**
 * Reasons for concluding a match.
 */
public enum GameEndReason {
    HUNTERS_ELIMINATED_ALL,
    RUNNERS_SURVIVED,
    MANUAL_STOP,
    CONFIGURATION_ERROR;
}
