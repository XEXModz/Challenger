package com.challenger.battle;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.*;

public class BattleManager {

    // Active battles: both directions stored for quick lookup
    private static final Map<UUID, UUID> activeBattles = new HashMap<>();

    // Pending requests: challenger UUID -> target UUID
    private static final Map<UUID, UUID> pendingRequests = new HashMap<>();

    // Request timestamps for expiry (30 seconds)
    private static final Map<UUID, Long> requestTimestamps = new HashMap<>();

    // Cooldown for "PvP disabled" message (per attacker, 3 seconds)
    private static final Map<UUID, Long> pvpMessageCooldown = new HashMap<>();

    private static final long REQUEST_TIMEOUT_MS = 30_000;
    private static final long PVP_MSG_COOLDOWN_MS = 3_000;

    // --- Send a battle request ---
    public static void sendRequest(ServerPlayer challenger, ServerPlayer target) {
        cleanExpiredRequests();

        if (challenger.getUUID().equals(target.getUUID())) {
            challenger.sendSystemMessage(Component.literal("§cYou can't battle yourself!"));
            return;
        }
        if (isInBattle(challenger)) {
            challenger.sendSystemMessage(Component.literal("§cYou are already in a battle!"));
            return;
        }
        if (isInBattle(target)) {
            challenger.sendSystemMessage(Component.literal("§c" + target.getName().getString() + " is already in a battle!"));
            return;
        }
        if (hasPendingRequest(challenger)) {
            challenger.sendSystemMessage(Component.literal("§cYou already have a pending battle request!"));
            return;
        }

        pendingRequests.put(challenger.getUUID(), target.getUUID());
        requestTimestamps.put(challenger.getUUID(), System.currentTimeMillis());

        challenger.sendSystemMessage(Component.literal(
            "§6⚔ Battle request sent to §e" + target.getName().getString() + "§6! Waiting for response..."));

        target.sendSystemMessage(Component.literal(
            "§6⚔ §e" + challenger.getName().getString() + " §6has challenged you to a battle!"));
        target.sendSystemMessage(Component.literal(
            "§aType §2/battle accept " + challenger.getName().getString() + " §ato accept, or §c/battle deny " + challenger.getName().getString() + " §cto decline."));
        target.sendSystemMessage(Component.literal("§7(Request expires in 30 seconds)"));
    }

    // --- Accept a battle request ---
    public static void acceptRequest(ServerPlayer target, ServerPlayer challenger) {
        cleanExpiredRequests();

        UUID challengerUUID = challenger.getUUID();
        UUID targetUUID = target.getUUID();

        if (!pendingRequests.containsKey(challengerUUID) ||
            !pendingRequests.get(challengerUUID).equals(targetUUID)) {
            target.sendSystemMessage(Component.literal("§cNo pending battle request from " + challenger.getName().getString() + "."));
            return;
        }

        pendingRequests.remove(challengerUUID);
        requestTimestamps.remove(challengerUUID);

        // Register battle both ways
        activeBattles.put(challengerUUID, targetUUID);
        activeBattles.put(targetUUID, challengerUUID);

        // Broadcast to both
        String msg = "§c⚔ BATTLE STARTED! §e" + challenger.getName().getString() + " §cvs §e" + target.getName().getString() + " §c⚔";
        challenger.sendSystemMessage(Component.literal(msg));
        target.sendSystemMessage(Component.literal(msg));
        challenger.sendSystemMessage(Component.literal("§7Type §c/battle forfeit §7to surrender."));
        target.sendSystemMessage(Component.literal("§7Type §c/battle forfeit §7to surrender."));
    }

    // --- Deny a battle request ---
    public static void denyRequest(ServerPlayer target, ServerPlayer challenger) {
        cleanExpiredRequests();

        UUID challengerUUID = challenger.getUUID();
        UUID targetUUID = target.getUUID();

        if (!pendingRequests.containsKey(challengerUUID) ||
            !pendingRequests.get(challengerUUID).equals(targetUUID)) {
            target.sendSystemMessage(Component.literal("§cNo pending battle request from " + challenger.getName().getString() + "."));
            return;
        }

        pendingRequests.remove(challengerUUID);
        requestTimestamps.remove(challengerUUID);

        target.sendSystemMessage(Component.literal("§7You declined the battle request from §e" + challenger.getName().getString() + "§7."));
        challenger.sendSystemMessage(Component.literal("§c" + target.getName().getString() + " declined your battle request."));
    }

    // --- Forfeit ---
    public static void forfeit(ServerPlayer player) {
        if (!isInBattle(player)) {
            player.sendSystemMessage(Component.literal("§cYou are not in a battle!"));
            return;
        }
        ServerPlayer opponent = getBattleOpponent(player);
        endBattle(player, opponent, true);
    }

    // --- Handle death event ---
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer loser)) return;
        if (!isInBattle(loser)) return;

        // Cancel the actual death, handle it manually
        event.setCanceled(true);

        ServerPlayer winner = getBattleOpponent(loser);
        endBattle(loser, winner, false);
    }

    // --- Block damage between non-battling players ---
    public static void onLivingHurt(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer victim)) return;
        if (!(event.getContainer().getSource().getEntity() instanceof ServerPlayer attacker)) return;

        // If not battling each other, cancel damage
        if (!areBattling(attacker, victim)) {
            event.setCanceled(true);
            // Only show the message every 3 seconds to avoid spam
            long now = System.currentTimeMillis();
            Long last = pvpMessageCooldown.get(attacker.getUUID());
            if (last == null || now - last > PVP_MSG_COOLDOWN_MS) {
                pvpMessageCooldown.put(attacker.getUUID(), now);
                attacker.sendSystemMessage(Component.literal("§cPvP is disabled. Use §e/battle <player> §cto challenge someone!"));
            }
        }
    }

    // --- End a battle ---
    private static void endBattle(ServerPlayer loser, ServerPlayer winner, boolean forfeit) {
        UUID loserUUID = loser.getUUID();
        UUID winnerUUID = activeBattles.get(loserUUID);

        activeBattles.remove(loserUUID);
        if (winnerUUID != null) activeBattles.remove(winnerUUID);

        // Restore loser health, keep inventory, teleport to spawn
        loser.setHealth(loser.getMaxHealth());
        teleportToSpawn(loser);

        if (winner != null) {
            String endMsg = forfeit
                ? "§e" + loser.getName().getString() + " §6forfeited! §e" + winner.getName().getString() + " §6wins! ⚔"
                : "§e" + winner.getName().getString() + " §6wins the battle against §e" + loser.getName().getString() + "§6! ⚔";
            loser.sendSystemMessage(Component.literal("§c" + (forfeit ? "You forfeited the battle." : "You lost the battle!") + " Returning to spawn..."));
            winner.sendSystemMessage(Component.literal("§a" + (forfeit ? loser.getName().getString() + " forfeited! You win!" : "You won the battle! ⚔")));
            // Broadcast to server
            if (loser.getServer() != null) {
                loser.getServer().getPlayerList().getPlayers().forEach(p ->
                    p.sendSystemMessage(Component.literal("§6[Battle] " + endMsg)));
            }
        } else {
            loser.sendSystemMessage(Component.literal("§7Battle ended."));
        }
    }

    private static void teleportToSpawn(ServerPlayer player) {
        if (player.getServer() == null) return;
        var overworld = player.getServer().overworld();
        var spawnPos = overworld.getSharedSpawnPos();
        player.teleportTo(overworld,
            spawnPos.getX() + 0.5,
            spawnPos.getY(),
            spawnPos.getZ() + 0.5,
            player.getYRot(),
            player.getXRot());
    }

    // --- Helpers ---
    public static boolean isInBattle(Player player) {
        return activeBattles.containsKey(player.getUUID());
    }

    public static boolean hasPendingRequest(Player player) {
        return pendingRequests.containsKey(player.getUUID());
    }

    public static boolean areBattling(Player a, Player b) {
        UUID opponent = activeBattles.get(a.getUUID());
        return opponent != null && opponent.equals(b.getUUID());
    }

    public static ServerPlayer getBattleOpponent(ServerPlayer player) {
        UUID opponentUUID = activeBattles.get(player.getUUID());
        if (opponentUUID == null || player.getServer() == null) return null;
        return player.getServer().getPlayerList().getPlayer(opponentUUID);
    }

    private static void cleanExpiredRequests() {
        long now = System.currentTimeMillis();
        requestTimestamps.entrySet().removeIf(entry -> {
            if (now - entry.getValue() > REQUEST_TIMEOUT_MS) {
                pendingRequests.remove(entry.getKey());
                return true;
            }
            return false;
        });
    }
}
