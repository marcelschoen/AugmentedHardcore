package com.backtobedrock.augmentedhardcore.commands;

import com.backtobedrock.augmentedhardcore.domain.enums.Command;
import com.backtobedrock.augmentedhardcore.utils.BanUtils;
import org.bukkit.command.CommandSender;

import java.util.concurrent.ExecutionException;

public class CommandUnDeathBan extends AbstractCommand {
    public CommandUnDeathBan(CommandSender cs, String[] args) {
        super(cs, args);
    }

    @Override
    public void run() throws ExecutionException, InterruptedException {
        Command command = Command.UNDEATHBAN;

        if (!this.hasPermission(command))
            return;

        if (!this.hasCorrectAmountOfArguments(command))
            return;

        this.hasPlayedBefore(this.args[0]).thenAcceptAsync(bool -> {
            if (!bool) {
                return;
            }

            try {
                this.plugin.getPlayerRepository().getByPlayer(this.target).thenAcceptAsync(playerData -> {
                    if (BanUtils.unDeathBan(playerData)) {
                        this.cs.sendMessage(String.format("§a%s has successfully been unbanned from a death ban.", this.target.getName()));
                    } else {
                        this.cs.sendMessage(String.format("§c%s is not death banned by %s.", this.target.getName(), this.plugin.getDescription().getName()));
                    }
                }).get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }).get();
    }
}