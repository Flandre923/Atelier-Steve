package com.ateliersteve.alchemy.ui;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

final class AlchemyCombineSessionStorage {
    private final Map<UUID, AlchemyCombineUI.PendingCombine> pendingServer = new ConcurrentHashMap<>();
    private final Map<UUID, AlchemyCombineUI.PendingCombine> pendingClient = new ConcurrentHashMap<>();
    private final Map<UUID, AlchemyCombineSessionSnapshot> sessionServer = new ConcurrentHashMap<>();
    private final Map<UUID, AlchemyCombineSessionSnapshot> sessionClient = new ConcurrentHashMap<>();
    private final Map<UUID, Integer[]> boardServer = new ConcurrentHashMap<>();
    private final Map<UUID, Integer[]> boardClient = new ConcurrentHashMap<>();

    void requestServer(UUID playerId, ResourceLocation recipeId, List<ItemStack> selectedStacks, List<AlchemyCombineUI.ReservedMaterialRef> reservedMaterials) {
        if (playerId == null || recipeId == null) {
            return;
        }
        pendingServer.put(playerId, new AlchemyCombineUI.PendingCombine(recipeId, copyStacks(selectedStacks), copyReservedMaterials(reservedMaterials)));
    }

    void requestClient(UUID playerId, ResourceLocation recipeId, List<ItemStack> selectedStacks) {
        if (playerId == null || recipeId == null) {
            return;
        }
        pendingClient.put(playerId, new AlchemyCombineUI.PendingCombine(recipeId, copyStacks(selectedStacks), List.of()));
    }

    AlchemyCombineUI.PendingCombine consumePending(UUID playerId, boolean isClientSide) {
        if (playerId == null) {
            return null;
        }
        Map<UUID, AlchemyCombineUI.PendingCombine> primary = isClientSide ? pendingClient : pendingServer;
        AlchemyCombineUI.PendingCombine pending = primary.remove(playerId);
        if (pending == null) {
            Map<UUID, AlchemyCombineUI.PendingCombine> fallback = isClientSide ? pendingServer : pendingClient;
            pending = fallback.get(playerId);
        }
        return pending;
    }

    void storeSession(UUID playerId, boolean isClientSide, AlchemyCombineSessionSnapshot snapshot) {
        if (playerId == null || snapshot == null) {
            return;
        }
        if (isClientSide) {
            sessionClient.put(playerId, snapshot);
        } else {
            sessionServer.put(playerId, snapshot);
        }
    }

    void storeServerSession(UUID playerId, AlchemyCombineSessionSnapshot snapshot) {
        if (playerId == null || snapshot == null) {
            return;
        }
        sessionServer.put(playerId, snapshot);
    }

    void storeBoard(UUID playerId, boolean isClientSide, Integer[] payload) {
        if (playerId == null || payload == null) {
            return;
        }
        if (isClientSide) {
            boardClient.put(playerId, payload);
        } else {
            boardServer.put(playerId, payload);
        }
    }

    void storeServerBoard(UUID playerId, Integer[] payload) {
        if (playerId == null || payload == null) {
            return;
        }
        boardServer.put(playerId, payload);
    }

    static List<ItemStack> copyStacks(List<ItemStack> stacks) {
        if (stacks == null) {
            return List.of();
        }
        List<ItemStack> copied = new ArrayList<>(stacks.size());
        for (ItemStack stack : stacks) {
            if (stack != null && !stack.isEmpty()) {
                copied.add(stack.copy());
            }
        }
        return copied;
    }

    static List<AlchemyCombineUI.ReservedMaterialRef> copyReservedMaterials(List<AlchemyCombineUI.ReservedMaterialRef> refs) {
        if (refs == null || refs.isEmpty()) {
            return List.of();
        }
        List<AlchemyCombineUI.ReservedMaterialRef> copied = new ArrayList<>(refs.size());
        for (AlchemyCombineUI.ReservedMaterialRef ref : refs) {
            if (ref != null) {
                copied.add(new AlchemyCombineUI.ReservedMaterialRef(ref.basketPos(), ref.slotIndex(), ref.stack().copy()));
            }
        }
        return copied;
    }
}
