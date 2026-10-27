package ru.example.talismans;

import net.kyori.adventure.text.Component;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class TalismansPlugin extends JavaPlugin {

    private TalismanItems talismanItems;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        talismanItems = new TalismanItems(this);

        getServer().getPluginManager().registerEvents(new DeathListener(this), this);

        PluginCommand command = getCommand("talisman");
        if (command != null) {
            TalismanCommand executor = new TalismanCommand(this);
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        }

        getLogger().info("Талисманы включены: " + TalismanType.values().length + " типов.");
    }

    public TalismanItems getTalismanItems() {
        return talismanItems;
    }

    /**
     * Пытается спасти игрока: ищет талисман в инвентаре, расходует его,
     * применяет эффект и показывает уведомления.
     *
     * @return true, если талисман сработал
     */
    public boolean trigger(Player player) {
        if (!player.hasPermission("talismans.use")) {
            return false;
        }

        for (TalismanType type : TalismanType.values()) {
            if (!talismanItems.consume(player, type)) {
                continue;
            }

            type.apply(player, this);

            DeathListener.playSaveEffects(player);
            sendTriggerMessage(player, type);
            String othersRaw = getConfig().getString("messages.trigger-others",
                            "&e%player% был спасён талисманом &6%talisman%&e!")
                    .replace("%player%", player.getName())
                    .replace("%talisman%", type.getDisplayName());
            DeathListener.notifyNearby(player, prefix().append(TalismanItems.legacy(othersRaw)), 32.0);

            return true;
        }
        return false;
    }

    private void sendTriggerMessage(Player player, TalismanType type) {
        String raw = getConfig().getString("messages.trigger",
                "&a%talisman% сработал и спас вас от смерти!");
        raw = raw.replace("%talisman%", type.getDisplayName()).replace("%player%", player.getName());
        player.sendMessage(prefix().append(TalismanItems.legacy(raw)));
    }

    public Component prefix() {
        String raw = getConfig().getString("messages.prefix", "&6[Талисманы] &r");
        return TalismanItems.legacy(raw);
    }

    /**
     * Сообщение из конфига с плейсхолдерами.
     */
    public Component msg(String key, String... replacements) {
        String raw = getConfig().getString("messages." + key, "&cНет сообщения: " + key);
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            raw = raw.replace("%" + replacements[i] + "%", replacements[i + 1]);
        }
        return prefix().append(TalismanItems.legacy(raw));
    }
}
