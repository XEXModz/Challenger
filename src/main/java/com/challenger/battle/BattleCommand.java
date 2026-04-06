package com.challenger.battle;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class BattleCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {

        dispatcher.register(Commands.literal("battle")

            // /battle <playerName> — send challenge
            .then(Commands.argument("player", StringArgumentType.word())
                .executes(ctx -> {
                    CommandSourceStack source = ctx.getSource();
                    ServerPlayer challenger = source.getPlayerOrException();
                    String targetName = StringArgumentType.getString(ctx, "player");
                    ServerPlayer target = source.getServer().getPlayerList().getPlayerByName(targetName);

                    if (target == null) {
                        challenger.sendSystemMessage(Component.literal("§cPlayer §e" + targetName + " §cis not online."));
                        return 0;
                    }

                    BattleManager.sendRequest(challenger, target);
                    return 1;
                }))

            // /battle accept <playerName>
            .then(Commands.literal("accept")
                .then(Commands.argument("player", StringArgumentType.word())
                    .executes(ctx -> {
                        ServerPlayer target = ctx.getSource().getPlayerOrException();
                        String challengerName = StringArgumentType.getString(ctx, "player");
                        ServerPlayer challenger = ctx.getSource().getServer().getPlayerList().getPlayerByName(challengerName);

                        if (challenger == null) {
                            target.sendSystemMessage(Component.literal("§cPlayer §e" + challengerName + " §cis not online."));
                            return 0;
                        }

                        BattleManager.acceptRequest(target, challenger);
                        return 1;
                    })))

            // /battle deny <playerName>
            .then(Commands.literal("deny")
                .then(Commands.argument("player", StringArgumentType.word())
                    .executes(ctx -> {
                        ServerPlayer target = ctx.getSource().getPlayerOrException();
                        String challengerName = StringArgumentType.getString(ctx, "player");
                        ServerPlayer challenger = ctx.getSource().getServer().getPlayerList().getPlayerByName(challengerName);

                        if (challenger == null) {
                            target.sendSystemMessage(Component.literal("§cPlayer §e" + challengerName + " §cis not online."));
                            return 0;
                        }

                        BattleManager.denyRequest(target, challenger);
                        return 1;
                    })))

            // /battle forfeit
            .then(Commands.literal("forfeit")
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    BattleManager.forfeit(player);
                    return 1;
                }))

            // /battle status
            .then(Commands.literal("status")
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    if (BattleManager.isInBattle(player)) {
                        ServerPlayer opponent = BattleManager.getBattleOpponent(player);
                        String opName = opponent != null ? opponent.getName().getString() : "Unknown";
                        player.sendSystemMessage(Component.literal("§6⚔ You are battling §e" + opName + "§6!"));
                    } else {
                        player.sendSystemMessage(Component.literal("§7You are not in a battle."));
                    }
                    return 1;
                }))
        );
    }
}
