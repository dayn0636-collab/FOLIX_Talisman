package ru.example.talismans;

import net.kyori.adventure.text.Component;
import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

/**
 * Срабатывает тогда, когда талисман спасает игрока от смерти.
 *
 * Основная логика: смертельная часть урона отменяется, талисман расходуется,
 * а эффекты применяются к игроку.
 */
public final class DeathListener implements Listener {

    private static final double SAVE_HEALTH = 1.0;

    private final TalismansPlugin plugin;

    public DeathListener(TalismansPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onLethalDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
            return;
        }

        // Абсорбция (жёлтые сердца) поглощает урон первой — учитываем её.
        double healthLoss = Math.max(0.0, event.getFinalDamage() - player.getAbsorptionAmount());
        if (healthLoss < player.getHealth()) {
            return;
        }

        // Талисманы — это тоже Тотемы Бессмертия, поэтому важен порядок:
        // ванильный тотем съедает предмет из руки сам, не разбираясь в PDC-метках.
        //
        // 1) Если ванильный (обычный, без метки) тотем стоит в той руке, из которой
        //    его съела бы ваниль — не мешаем, работает обычный тотем.
        // 2) Иначе, если есть талисман — срабатываем мы первыми: урон отменяется,
        //    талисман расходуется и ваниль до него не доходит.
        if (vanillaWouldUsePlainTotem(player)) {
            return;
        }

        if (!plugin.trigger(player)) {
            return;
        }

        event.setCancelled(true);
        player.setFireTicks(0);
        if (player.getHealth() < SAVE_HEALTH) {
            player.setHealth(SAVE_HEALTH);
        }
    }

    /**
     * Ваниль съедает тотем из основной руки, затем из левой.
     * Проверяем: сработал бы ванильный тотем и является ли он обычным (без PDC-метки).
     * Талисман в руке ванилью съеден не будет — мы обработаем его сами.
     */
    private boolean vanillaWouldUsePlainTotem(Player player) {
        org.bukkit.inventory.ItemStack main = player.getInventory().getItemInMainHand();
        if (main != null && main.getType() == org.bukkit.Material.TOTEM_OF_UNDYING) {
            return plugin.getTalismanItems().typeOf(main) == null;
        }
        org.bukkit.inventory.ItemStack off = player.getInventory().getItemInOffHand();
        if (off != null && off.getType() == org.bukkit.Material.TOTEM_OF_UNDYING) {
            return plugin.getTalismanItems().typeOf(off) == null;
        }
        return false;
    }

    /**
     * Уведомление для игроков рядом с владельцем (вызывается из {@link TalismansPlugin#trigger}).
     */
    static void notifyNearby(Player owner, Component message, double radius) {
        for (Player other : owner.getWorld().getPlayers()) {
            if (other.equals(owner)) {
                continue;
            }
            if (other.getLocation().distanceSquared(owner.getLocation()) <= radius * radius) {
                other.sendMessage(message);
            }
        }
    }

    static void playSaveEffects(Player player) {
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_TOTEM_USE, 1.0f, 1.0f);
        player.getWorld().spawnParticle(org.bukkit.Particle.TOTEM,
                player.getLocation().add(0, 1, 0), 60, 0.5, 0.9, 0.5, 0.35);
    }
}
