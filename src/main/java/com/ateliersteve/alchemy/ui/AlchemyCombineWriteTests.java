package com.ateliersteve.alchemy.ui;

import com.ateliersteve.AtelierSteve;

import java.util.List;

public final class AlchemyCombineWriteTests {
    private AlchemyCombineWriteTests() {
    }

    public static void run() {
        testBoardPayloadWrite();
        testSessionPayloadWriteRoundTrip();
        AtelierSteve.LOGGER.info("Alchemy combine write tests passed");
    }

    private static void testBoardPayloadWrite() {
        AlchemyCombineBoard.State board = new AlchemyCombineBoard.State(5, 5);
        Integer[] payload = board.writePayload();
        require(payload.length == 25, "Board payload length must be 25");
        for (Integer value : payload) {
            require(value != null && value == 0, "Empty board payload should contain only 0 values");
        }
    }

    private static void testSessionPayloadWriteRoundTrip() {
        AlchemyCombineSessionSnapshot initial = AlchemyCombineSessionSnapshot.fromStacks(List.of());
        Integer[] payload = initial.toSyncPayload();
        require(payload.length >= 7, "Session payload is unexpectedly short");
        require(payload[0] != null && payload[0] == 3, "Session payload version must be 3");
        AlchemyCombineSessionSnapshot restored = AlchemyCombineSessionSnapshot.fromSyncPayload(initial, payload, 5);
        require(restored != null, "Session payload round-trip returned null state");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
