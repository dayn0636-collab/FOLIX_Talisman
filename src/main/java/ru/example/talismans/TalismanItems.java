package ru.example.talismans;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

/**
 * Создание и распознавание предметов-талисманов.
 */
public final class TalismanItems {

    public static final String STORAGE_KEY = "talisman_type";

    private final NamespacedKey key;

    public TalismanItems(TalismansPlugin plugin) {
        this.key = new NamespacedKey(plugin, STORAGE_KEY);
    }

    public ItemStack create(TalismanType type, int amount) {
        ItemStack item = new ItemStack(type.getMaterial(), Math.max(1, amount));
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        meta.displayName(Component.text(type.getDisplayName())
                .color(NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        for (String line : type.getLore()) {
            lore.add(legacy(line).decoration(TextDecoration.ITALIC, false));
        }
        lore.add(Component.empty());
        lore.add(Component.text("Расходуемый тотем бессмертия")
                .color(NamedTextColor.DARK_GRAY)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);

        meta.addEnchant(Enchantment.DURABILITY, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, type.getId());

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Возвращает тип талисмана предмета или null, если предмет не является талисманом.
     */
    public TalismanType typeOf(ItemStack item) {
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) {
            return null;
        }
        String stored = item.getItemMeta().getPersistentDataContainer()
                .get(key, PersistentDataType.STRING);
        return stored == null ? null : TalismanType.fromId(stored);
    }

    /**
     * Ищет первый талисман в инвентаре (в руке — приоритетнее).
     */
    public int findSlot(org.bukkit.entity.Player player, TalismanType type) {
        ItemStack[] contents = player.getInventory().getContents();
        // Основная рука и левая рука.
        if (typeOf(player.getInventory().getItemInMainHand()) == type) {
            return player.getInventory().getHeldItemSlot();
        }
        if (typeOf(player.getInventory().getItemInOffHand()) == type) {
            return 40;
        }
        for (int i = 0; i < contents.length; i++) {
            if (typeOf(contents[i]) == type) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Убирает один талисман из инвентаря. Возвращает true, если предмет был изъят.
     */
    public boolean consume(org.bukkit.entity.Player player, TalismanType type) {
        int slot = findSlot(player, type);
        if (slot < 0) {
            return false;
        }
        ItemStack item = player.getInventory().getItem(slot);
        if (item == null) {
            return false;
        }
        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
        } else {
            player.getInventory().setItem(slot, null);
        }
        player.updateInventory();
        return true;
    }

    /**
     * Цветит legacy-строку (&-коды) в Adventure-компонент.
     */
    public static Component legacy(String text) {
        return net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                .legacySection()
                .deserialize(org.bukkit.ChatColor.translateAlternateColorCodes('&', text));
    }
}
