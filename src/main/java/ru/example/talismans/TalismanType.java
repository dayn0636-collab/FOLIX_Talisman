package ru.example.talismans;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Виды талисманов: внешний вид, описание и эффект при срабатывании.
 */
public enum TalismanType {

    SWIFT("swift", "Талисман Скорохода", Material.TOTEM_OF_UNDYING,
            List.of(
                    "&7При срабатывании даёт эффект",
                    "&7«Скорость II» &aна 5 секунд&7."
            )) {
        @Override
        public void apply(Player player, TalismansPlugin plugin) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 5, 1, true, true, true));
        }
    },

    SEA_SPIRIT("sea_spirit", "Талисман Морского Духа", Material.TOTEM_OF_UNDYING,
            List.of(
                    "&7При срабатывании даёт эффекты",
                    "&7«Подводное дыхание» &aна 10 секунд&7,",
                    "&7«Грация дельфина» &aна 8 секунд&7."
            )) {
        @Override
        public void apply(Player player, TalismansPlugin plugin) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, 20 * 10, 0, true, true, true));
            player.addPotionEffect(new PotionEffect(PotionEffectType.DOLPHINS_GRACE, 20 * 8, 0, true, true, true));
        }
    },

    FIRE_SHIELD("fire_shield", "Талисман Огненного Щита", Material.TOTEM_OF_UNDYING,
            List.of(
                    "&7При срабатывании даёт эффекты",
                    "&7«Огнестойкость» &aна 8 секунд&7,",
                    "&7«Скорость I» &aна 5 секунд&7."
            )) {
        @Override
        public void apply(Player player, TalismansPlugin plugin) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 20 * 8, 0, true, true, true));
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 5, 0, true, true, true));
        }
    },

    BARRIER("barrier", "Талисман Барьера", Material.TOTEM_OF_UNDYING,
            List.of(
                    "&7При срабатывании отбрасывает",
                    "&7игроков вокруг владельца",
                    "&7и даёт эффект «Скорость I» &aна 5 секунд&7."
            )) {
        @Override
        public void apply(Player player, TalismansPlugin plugin) {
            double radius = plugin.getConfig().getDouble("barrier.radius", 6.0);
            double strength = plugin.getConfig().getDouble("barrier.strength", 1.6);
            double vertical = plugin.getConfig().getDouble("barrier.vertical", 0.55);

            Location origin = player.getLocation();
            for (Player other : player.getWorld().getPlayers()) {
                if (other.equals(player)) {
                    continue;
                }
                Vector push = other.getLocation().toVector().subtract(origin.toVector());
                double distance = push.length();
                if (distance > radius) {
                    continue;
                }
                // Чем ближе к владельцу, тем сильнее отброс.
                double scale = 1.0 - (distance / (radius * 2.0));
                Vector velocity = distance < 0.0001
                        ? new Vector(ThreadLocalRandom.current().nextDouble(-1, 1), 0, ThreadLocalRandom.current().nextDouble(-1, 1)).normalize()
                        : push.normalize();
                velocity.multiply(strength * (0.5 + scale)).setY(vertical);
                other.setVelocity(velocity);
                other.getWorld().playSound(other.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.6f, 1.6f);
                other.getWorld().spawnParticle(Particle.CLOUD, other.getLocation().add(0, 1, 0), 15, 0.4, 0.6, 0.4, 0.05);
            }

            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 5, 0, true, true, true));
        }
    },

    END("end", "Талисман Энда", Material.TOTEM_OF_UNDYING,
            List.of(
                    "&7При срабатывании случайно",
                    "&7телепортирует игрока на &a8 блоков",
                    "&7в направлении взгляда."
            )) {
        @Override
        public void apply(Player player, TalismansPlugin plugin) {
            double distance = plugin.getConfig().getDouble("ender.distance", 8.0);
            double spread = plugin.getConfig().getDouble("ender.spread", 2.0);

            ThreadLocalRandom random = ThreadLocalRandom.current();
            Vector direction = player.getLocation().getDirection().normalize();

            Location target = player.getLocation()
                    .add(direction.multiply(distance))
                    .add(random.nextDouble(-spread, spread), 0, random.nextDouble(-spread, spread));

            Location safe = findSafeLocation(player, target);
            if (safe == null) {
                // Запасной вариант: телепорт назад, чуть выше.
                safe = findSafeLocation(player, player.getLocation().add(0, 2, 0));
                if (safe == null) {
                    return;
                }
            }

            player.teleport(safe);
            player.getWorld().playSound(safe, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.2f);
            player.getWorld().spawnParticle(Particle.PORTAL, safe.clone().add(0, 1, 0), 40, 0.4, 0.8, 0.4, 0.2);
        }
    };

    private final String id;
    private final String displayName;
    private final Material material;
    private final List<String> lore;

    TalismanType(String id, String displayName, Material material, List<String> lore) {
        this.id = id;
        this.displayName = displayName;
        this.material = material;
        this.lore = lore;
    }

    /**
     * Применяет эффект талисмана к игроку в момент срабатывания.
     */
    public abstract void apply(Player player, TalismansPlugin plugin);

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Material getMaterial() {
        return material;
    }

    public List<String> getLore() {
        return lore;
    }

    public static TalismanType fromId(String id) {
        for (TalismanType type : values()) {
            if (type.id.equalsIgnoreCase(id) || type.name().equalsIgnoreCase(id) || type.displayName.equalsIgnoreCase(id)) {
                return type;
            }
        }
        return null;
    }

    public static String ids() {
        StringBuilder builder = new StringBuilder();
        for (TalismanType type : values()) {
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append(type.id);
        }
        return builder.toString();
    }

    /**
     * Ищет безопасное место для телепортации: проход (2 блока воздуха) над опорой.
     * Возвращает null, если безопасное место не найдено.
     */
    private static Location findSafeLocation(Player player, Location start) {
        Location candidate = start.clone();
        int minHeight = player.getWorld().getMinHeight() + 1;
        int maxHeight = player.getWorld().getMaxHeight() - 2;

        candidate.setY(Math.min(Math.max(candidate.getY(), minHeight), maxHeight));

        // Сначала поднимаемся, пока не освободим место.
        for (int i = 0; i < 16; i++) {
            if (isPassable(candidate)) {
                return candidate;
            }
            candidate.add(0, 1, 0);
            if (candidate.getY() > maxHeight) {
                break;
            }
        }

        // Затем ищем землю вниз от исходной точки.
        candidate = start.clone();
        candidate.setY(Math.min(Math.max(candidate.getY(), minHeight), maxHeight));
        for (int i = 0; i < 16; i++) {
            if (isPassable(candidate) && !candidate.clone().subtract(0, 1, 0).getBlock().getType().isAir()) {
                return candidate;
            }
            candidate.subtract(0, 1, 0);
            if (candidate.getY() <= minHeight) {
                break;
            }
        }
        return null;
    }

    private static boolean isPassable(Location location) {
        return location.getBlock().isPassable()
                && location.clone().add(0, 1, 0).getBlock().isPassable();
    }
}
