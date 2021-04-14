package com.backtobedrock.augmentedhardcore.commands;

import com.backtobedrock.augmentedhardcore.domain.data.PlayerData;
import com.backtobedrock.augmentedhardcore.domain.enums.Command;
import com.backtobedrock.augmentedhardcore.domain.enums.Permission;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.concurrent.ExecutionException;

public class CommandLives extends AbstractCommand {
    public CommandLives(CommandSender cs, String[] args) {
        super(cs, args);
    }

    @Override
    public void run() throws ExecutionException, InterruptedException {
        Command command = Command.LIVES;

        if (this.args.length == 0 && !this.hasPermission(command)) {
            return;
        } else if (!this.hasPermission(Permission.LIVES_OTHER)) {
            return;
        }

        if (!this.hasCorrectAmountOfArguments(command)) {
            return;
        }

        if (this.args.length == 0) {
            if (!this.isPlayer()) {
                return;
            }

            this.plugin.getPlayerRepository().getByPlayer(this.sender).thenAcceptAsync(this::sendSuccessMessage).get();
        } else {
            this.hasPlayedBefore(this.args[0]).thenAcceptAsync(bool -> {
                if (!bool) {
                    return;
                }

                try {
                    this.plugin.getPlayerRepository().getByPlayer(this.target).thenAcceptAsync(this::sendSuccessMessage).get();
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }).get();
        }
    }

    private void sendSuccessMessage(PlayerData playerData) {
        boolean isSender = this.cs instanceof Player && ((Player) this.cs).getUniqueId() == playerData.getPlayer().getUniqueId();
        this.cs.sendMessage(String.format("§a%s currently %s §6%s §a.",
                isSender ? "You" : playerData.getPlayer().getName(),
                isSender ? "have" : "has",
                playerData.getLives() + "§e" + (playerData.getLives() == 1 ? " life" : " lives")));
    }
}