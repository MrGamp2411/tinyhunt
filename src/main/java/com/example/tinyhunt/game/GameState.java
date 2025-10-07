package com.example.tinyhunt.game;

/**
 * High-level game lifecycle.
 */
public enum GameState {
    WAITING,
    COUNTDOWN,
    RUNNING,
    ENDING;

    public boolean canJoin() {
        return this == WAITING || this == COUNTDOWN;
    }
}
