package com.sucy.skill.command.admin;

import com.sucy.skill.PermissionNodes;
import com.sucy.skill.SkillAPI;
import com.sucy.skill.api.PlayerSkills;
import com.sucy.skill.command.CommandHandler;
import com.sucy.skill.command.ICommand;
import com.sucy.skill.command.SenderType;
import com.sucy.skill.language.CommandNodes;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import java.util.List;

/**
 * Command to give a player experience
 */
public class CmdExpPlayer implements ICommand {

    /**
     * Executes the command
     *
     * @param handler handler for the command
     * @param plugin  plugin reference
     * @param sender  sender of the command
     * @param args    arguments
     */
    @Override
    public void execute(CommandHandler handler, Plugin plugin, CommandSender sender, String[] args) {

        SkillAPI api = (SkillAPI)plugin;

        // Requires at least 1 argument
        if (args.length >= 1) {
            PlayerSkills player;

            // Get the target
            if (args.length == 1) player = api.getPlayer(sender.getName());
            else player = api.getPlayer(args[1]);

            // Get the amount
            int amount = 0;
            try {
                amount = (int)Double.parseDouble(args[0]);
            }
            catch (Exception ex) {
                // Do nothing
            }

            // Invalid amount
            if (amount <= 0) {
                String error = api.getMessage(CommandNodes.NOT_POSITIVE, true);
                error = error.replace("{value}", args[0]);
                sender.sendMessage(error);
            }

            // Invalid target
            else if (player == null) {
                String error = api.getMessage(CommandNodes.NOT_A_PLAYER, true);
                error = error.replace("{player}", args[1]);
                sender.sendMessage(error);
            }

            // Target doesn't have a class
            else if (player.getClassName() == null) {
                String error = api.getMessage(CommandNodes.CANNOT_LEVEL, true);
                error = error.replace("{player}", player.getName());
                sender.sendMessage(error);
            }

            // Target is max level
            else if (player.getLevel() >= api.getClass(player.getClassName()).getMaxLevel()) {
                String error = api.getMessage(CommandNodes.MAX_LEVEL, true);
                error = error.replace("{player}", player.getName())
                        .replace("{level}", player.getLevel() + "");

                sender.sendMessage(error);
            }

            // Give them the experience
            else {
                player.giveExp(amount);

                // Confirmation message
                List<String> messages = api.getMessages(CommandNodes.COMPLETE + CommandNodes.EXP_PLAYER, true);
                for (String message : messages) {
                    message = message.replace("{player}", player.getName())
                            .replace("{amount}", amount + "")
                            .replace("{level}", player.getLevel() + "");

                    sender.sendMessage(message);
                }
            }
        }

        // Incorrect arguments
        else handler.displayUsage(sender);
    }

    /**
     * @return permission required for this command
     */
    @Override
    public String getPermissionNode() {
        return PermissionNodes.LEVEL;
    }

    /**
     * @return arguments used by this command
     */
    @Override
    public String getArgsString(Plugin plugin) {
        return ((SkillAPI)plugin).getMessage(CommandNodes.ARGUMENTS + CommandNodes.EXP_PLAYER, true);
    }

    /**
     * @return the description of this command
     */
    @Override
    public String getDescription(Plugin plugin) {
        return ((SkillAPI)plugin).getMessage(CommandNodes.DESCRIPTION + CommandNodes.EXP_PLAYER, true);
    }

    /**
     * @return required sender type for this command
     */
    @Override
    public SenderType getSenderType() {
        return SenderType.PLAYER_ONLY;
    }
}
