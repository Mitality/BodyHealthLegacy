package bodyhealth.util;

import bodyhealth.config.Config;
import bodyhealth.config.Debug;
import bodyhealth.effects.BodyHealthEffects;
import bodyhealth.core.BodyHealth;
import bodyhealth.Main;
import bodyhealth.core.BodyPart;
import bodyhealth.core.BodyPartState;
import com.tchristofferson.configupdater.ConfigUpdater;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class BodyHealthUtils {
    public static void reloadSystem() {

        for (Player player : Bukkit.getOnlinePlayers()) {
            BodyHealthEffects.removeEffectsFromPlayer(player);
        }

        // Reload and update config.yml
        Main.getInstance().saveDefaultConfig();
        File configFile = new File(Main.getInstance().getDataFolder(), "config.yml");
        try {
            ConfigUpdater.update(Main.getInstance(), "config.yml", configFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Main.getInstance().reloadConfig();

        // Load configuration internally
        Config.load(Main.getInstance().getConfig());

        for (Player player : Bukkit.getOnlinePlayers()) {
            BodyHealthEffects.addEffectsToPlayer(player);
        }
    }

    public static BodyHealth getBodyHealth(Player player) {
        return Main.playerBodyHealthMap.computeIfAbsent(player.getUniqueId(), p -> new BodyHealth(player.getUniqueId()));
    }

    public static void applyDamageWithConfig(BodyHealth bodyHealth, EntityDamageEvent.DamageCause cause, double damage) {
        ConfigurationSection config = Config.body_damage;

        for (BodyPart part : BodyPart.values()) {
            if (config.getKeys(false).contains(part.name())) {
                for (String entry : config.getStringList(part.name())) {
                    String[] data = entry.split(" ");
                    String damageType = data[0];
                    double percentage = Double.parseDouble(data[1].replace("%", ""));
                    if (cause.name().equalsIgnoreCase(damageType)) bodyHealth.applyDamage(part, damage * (percentage / 100.0));
                }
            } else {
                Debug.logErr("body_damage isn't configured for " + part.name() + "!");
            }
        }
    }

    public static void applyDamageWithConfig(BodyHealth bodyHealth, EntityDamageEvent.DamageCause cause, double damage, BodyPart part) {
        ConfigurationSection config = Config.body_damage;
        if (config.getKeys(false).contains(part.name())) {
            for (String entry : config.getStringList(part.name())) {
                String[] data = entry.split(" ");
                String damageType = data[0];
                double percentage = Double.parseDouble(data[1].replace("%", ""));
                if (cause.name().equalsIgnoreCase(damageType)) bodyHealth.applyDamage(part, damage * (percentage / 100.0));
            }
        } else {
            Debug.logErr("body_damage isn't configured for " + part.name() + "!");
        }
    }

    public static BodyPartState getBodyHealthState(BodyHealth bodyHealth, BodyPart part) {
        double currentHealth = bodyHealth.getHealth(part);
        if (currentHealth == 0) {
            return BodyPartState.BROKEN; // No health left
        } else if (currentHealth <= 25) {
            return BodyPartState.DAMAGED; // Health is low, below 25%
        } else if (currentHealth <= 50) {
            return BodyPartState.INTERMEDIATE; // Health is between 25% and 50%
        } else if (currentHealth < 100) {
            return BodyPartState.NEARLYFULL; // Health is between 50% and full
        } else {
            return BodyPartState.FULL; // Health is at maximum
        }
    }

    public static double getMaxHealth(BodyPart part, Player player) {
        double maxHealth = -1;

        if (Config.body_health.get(part.name()) instanceof Integer || Config.body_health.get(part.name()) instanceof Double) {
            maxHealth = ((Number) Config.body_health.get(part.name())).doubleValue();
        }
        else if (Config.body_health.get(part.name()) instanceof String) {
            String expression = (String) Config.body_health.get(part.name());

            expression = expression.replace("%PlayerMaxHealth%", String.valueOf(player.getMaxHealth()))
                    .replace("%PlayerCurrentHealth%", String.valueOf(player.getHealth()));

            maxHealth = evaluateExpression(expression);

            if (maxHealth == -1) {
                Debug.logErr("Invalid maxHealth expression: " + expression + " !");
                return player.getMaxHealth() / 2; // Default
            }
        }
        if (maxHealth > 0) return maxHealth;
        Debug.logErr("Invalid maxHealth for part " + part.name() + ": " + maxHealth + "! Max health has to be greater than 0!");
        return player.getMaxHealth() / 2; // Default
    }

    private static double evaluateExpression(String expression) {
        try {
            Expression e = new ExpressionBuilder(expression).build();
            return e.evaluate();
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    public static boolean canPlayerSprint(Player player) {
        BodyHealth bodyHealth = getBodyHealth(player);
        if (bodyHealth.getOngoingEffects().isEmpty()) return true;
        for (Map.Entry<BodyPart, List<String[]>> entry : bodyHealth.getOngoingEffects().entrySet()) {
            List<String[]> effectsList = entry.getValue();
            for (String[] effectParts : effectsList) {
                if (effectParts.length > 0
                        && effectParts[0].trim().equalsIgnoreCase("PREVENT_SPRINT")) return false;
            }
        }
        return true;
    }

    public static boolean canPlayerWalk(Player player) {
        BodyHealth bodyHealth = getBodyHealth(player);
        if (bodyHealth.getOngoingEffects().isEmpty()) return true;
        for (Map.Entry<BodyPart, List<String[]>> entry : bodyHealth.getOngoingEffects().entrySet()) {
            List<String[]> effectsList = entry.getValue();
            for (String[] effectParts : effectsList) {
                if (effectParts.length > 0
                        && effectParts[0].trim().equalsIgnoreCase("PREVENT_WALK")) return false;
            }
        }
        return true;
    }

    public static boolean canPlayerJump(Player player) {
        BodyHealth bodyHealth = getBodyHealth(player);
        if (bodyHealth.getOngoingEffects().isEmpty()) return true;
        for (Map.Entry<BodyPart, List<String[]>> entry : bodyHealth.getOngoingEffects().entrySet()) {
            List<String[]> effectsList = entry.getValue();
            for (String[] effectParts : effectsList) {
                if (effectParts.length > 0
                        && effectParts[0].trim().equalsIgnoreCase("PREVENT_JUMP")) return false;
            }
        }
        return true;
    }

    public static boolean canPlayerInteract(Player player, EquipmentSlot hand) {
        BodyHealth bodyHealth = getBodyHealth(player);
        if (bodyHealth.getOngoingEffects().isEmpty()) return true;
        for (Map.Entry<BodyPart, List<String[]>> entry : bodyHealth.getOngoingEffects().entrySet()) {
            List<String[]> effectsList = entry.getValue();
            for (String[] effectParts : effectsList) {
                if (effectParts.length > 0 && effectParts[0].trim().equalsIgnoreCase("PREVENT_INTERACT")
                        && effectParts[1].trim().equalsIgnoreCase(hand.name())) return false;
            }
        }
        return true;
    }

    public static boolean hasPlayerPotionEffect(Player player, PotionEffectType effect) {
        BodyHealth bodyHealth = getBodyHealth(player);
        if (bodyHealth.getOngoingEffects().isEmpty()) return false;
        for (Map.Entry<BodyPart, List<String[]>> entry : bodyHealth.getOngoingEffects().entrySet()) {
            List<String[]> effectsList = entry.getValue();
            for (String[] effectParts : effectsList) {
                if (effectParts.length > 0 && effectParts[0].trim().equalsIgnoreCase("POTION_EFFECT")
                        && effectParts[1].trim().equalsIgnoreCase(effect.getKey().getKey())) return true;
            }
        }
        return false;
    }

    public static int getHighestPotionEffectAmplifier(Player player, PotionEffectType effect) {
        BodyHealth bodyHealth = getBodyHealth(player);
        if (bodyHealth.getOngoingEffects().isEmpty()) return -1;
        int highestAmplifier = -1;
        for (Map.Entry<BodyPart, List<String[]>> entry : bodyHealth.getOngoingEffects().entrySet()) {
            List<String[]> effectsList = entry.getValue();
            for (String[] effectParts : effectsList) {
                if (effectParts.length > 0
                        && effectParts[0].trim().equalsIgnoreCase("POTION_EFFECT")
                        && effectParts[1].trim().equalsIgnoreCase(effect.getKey().getKey())
                        && Integer.parseInt(effectParts[2].trim()) > highestAmplifier)
                    highestAmplifier = Integer.parseInt(effectParts[2].trim());
            }
        }
        return highestAmplifier;
    }


    /**
     * Unnecessary method to remove invalid (leftover) effects from a players BodyHealth -> ongoingEffects.
     * This should never be necessary, meaning if this method does catch something, I messed up, but I'll
     * leave it here as an extra layer of safety in case that should ever happen ¯\_(ツ)_/¯
     */
    public static void validateEffects(Player player) {
        for (Map.Entry<BodyPart, List<String[]>> entry : getBodyHealth(player).getOngoingEffects().entrySet()) {

            BodyPart bodyPart = entry.getKey();
            List<String[]> effectsList = entry.getValue();

            ConfigurationSection bodyPartConfig = Config.effects.getConfigurationSection(bodyPart.name());
            if (bodyPartConfig == null) continue; // Nothing configured for this BodyPart

            List<String[]> validEffects = new ArrayList<>();
            for (String healthState : bodyPartConfig.getKeys(false)) {
                List<String> configEffects = bodyPartConfig.getStringList(healthState);
                for (String effect : configEffects) {
                    validEffects.add(effect.split("/"));
                }
            }

            for (String[] effectParts : effectsList) {
                if (!validEffects.contains(effectParts)) {
                    getBodyHealth(player).removeFromOngoingEffects(bodyPart, effectParts);
                    if (Config.debug_mode) Debug.log("Removing invalid effect \"" + Arrays.toString(effectParts) + "\" from player " + player.getName() + " for body part " + bodyPart.name());
                }
            }
        }
    }

}
