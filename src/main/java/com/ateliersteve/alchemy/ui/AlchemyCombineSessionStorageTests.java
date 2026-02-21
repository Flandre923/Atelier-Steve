package com.ateliersteve.alchemy.ui;

import com.ateliersteve.AtelierSteve;

import java.util.List;
import java.util.UUID;

public final class AlchemyCombineSessionStorageTests {
    private AlchemyCombineSessionStorageTests() {
    }

    public static void run() {
        testConsumeFromPrimarySide();
        testConsumeFallbackAcrossSides();
        testCopyHelpersHandleNullAndEmpty();
        AtelierSteve.LOGGER.info("Alchemy combine storage tests passed");
    }

    private static void testConsumeFromPrimarySide() {
        AlchemyCombineSessionStorage storage = new AlchemyCombineSessionStorage();
        UUID playerId = UUID.randomUUID();
        storage.requestServer(playerId, AtelierSteve.id("test_primary"), List.of(), List.of());

        AlchemyCombineUI.PendingCombine pending = storage.consumePending(playerId, false);
        require(pending != null, "Expected pending combine on server side");
        require(AtelierSteve.id("test_primary").equals(pending.recipeId()), "Unexpected recipe id in pending combine");
        require(storage.consumePending(playerId, false) == null, "Pending combine should be consumed once");
    }

    private static void testConsumeFallbackAcrossSides() {
        AlchemyCombineSessionStorage storage = new AlchemyCombineSessionStorage();
        UUID playerId = UUID.randomUUID();
        storage.requestServer(playerId, AtelierSteve.id("test_fallback"), List.of(), List.of());

        AlchemyCombineUI.PendingCombine pending = storage.consumePending(playerId, true);
        require(pending != null, "Expected fallback pending combine from opposite side");
        require(AtelierSteve.id("test_fallback").equals(pending.recipeId()), "Unexpected fallback recipe id");
    }

    private static void testCopyHelpersHandleNullAndEmpty() {
        require(AlchemyCombineSessionStorage.copyStacks(null).isEmpty(), "copyStacks should return empty for null input");
        require(AlchemyCombineSessionStorage.copyStacks(List.of()).isEmpty(), "copyStacks should return empty for empty input");
        require(AlchemyCombineSessionStorage.copyReservedMaterials(null).isEmpty(), "copyReservedMaterials should return empty for null input");
        require(AlchemyCombineSessionStorage.copyReservedMaterials(List.of()).isEmpty(), "copyReservedMaterials should return empty for empty input");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
