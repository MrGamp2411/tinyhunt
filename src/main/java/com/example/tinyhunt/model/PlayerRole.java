package com.example.tinyhunt.model;

/**
 * The role a participant is currently playing.
 */
public enum PlayerRole {
    RUNNER,
    /**
     * Transitional state while a runner is respawning before becoming a hunter.
     */
    CONVERTING,
    HUNTER;
}
