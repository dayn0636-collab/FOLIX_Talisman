package ru.example.talismans;

import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * /talisman give <player> <type> [amount]
 * /talisman list
 * /talisman reload
 */
public final class TalismanCommand implements CommandExecutor, TabCompleter {

    private final TalismansPlugin plugin;

    public TalismanCommand(TalismansPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sender.sendMessage(plugin.msg("usage"));
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "give" -> give(sender, args);
            case "list" -> list(sender);
            case "reload" -> {
                if (!sender.hasPermission("talismans.reload")) {
                    sender.sendMessage(plugin.msg("no-permission"));
                    return true;
                }
                plugin.reloadConfig();
                sender.sendMessage(plugin.msg("reload"));
            }
            default -> sender.sendMessage(plugin.msg("usage"));
        }
        return true;
    }

    private void give(CommandSender sender, String[] args) {
        if (!sender.hasPermission("talismans.give")) {
            sender.sendMessage(plugin.msg("no-permission"));
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(plugin.msg("usage"));
            return;
        }

        Player target = plugin.getServer().getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(plugin.msg("player-not-found", "player", args[1]));
            return;
        }

        TalismanType type = TalismanType.fromId(args[2]);
        if (type == null) {
            sender.sendMessage(plugin.msg("unknown-type", "type", args[2], "types", TalismanType.ids()));
            return;
        }

        int amount = 1;
        if (args.length >= 4) {
            try {
                amount = Math.max(1, Math.min(64, Integer.parseInt(args[3])));
            } catch (NumberFormatException ignored) {
                amount = 1;
            }
        }

        ItemStack item = plugin.getTalismanItems().create(type, amount);
        target.getInventory().addItem(item).values()
                .forEach(left -> target.getWorld().dropItemNaturally(target.getLocation(), left));

        sender.sendMessage(plugin.msg("given",
                "player", target.getName(),
                "talisman", type.getDisplayName(),
                "amount", String.valueOf(amount)));
    }

    private void list(CommandSender sender) {
        sender.sendMessage(plugin.prefix().append(Component.text("Доступные талисманы:")));
        for (TalismanType type : TalismanType.values()) {
            sender.sendMessage(plugin.prefix().append(
                    TalismanItems.legacy("&6" + type.getId() + " &7— &f" + type.getDisplayName())));
        }
        sender.sendMessage(plugin.prefix().append(
                TalismanItems.legacy("&7Выдача: &f/talisman give <игрок> <тип> [кол-во]")));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String[] args) {
        List<String> result = new ArrayList<>();
        if (args.length == 1) {
            for (String sub : List.of("give", "list", "reload")) {
                if (sub.startsWith(args[0].toLowerCase(Locale.ROOT))) {
                    result.add(sub);
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                if (player.getName().toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT))) {
                    result.add(player.getName());
                }
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            String prefix = args[2].toLowerCase(Locale.ROOT);
            for (TalismanType type : TalismanType.values()) {
                if (type.getId().startsWith(prefix)) {
                    result.add(type.getId());
                }
            }
        } else if (args.length == 4 && args[0].equalsIgnoreCase("give")) {
            result.add("1");
            result.add("5");
            result.add("64");
        }
        return result;
    }
}
