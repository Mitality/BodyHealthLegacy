package mitality.bodyHealthLegacy.depend;

import mitality.bodyHealthLegacy.core.BodyHealth;
import mitality.bodyHealthLegacy.util.BodyHealthUtils;
import mitality.bodyHealthLegacy.core.BodyPart;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;

public class PlaceholderAPI extends PlaceholderExpansion {

    @Override
    public @NotNull String getIdentifier() {
        return "bodyhealth";
    }

    @Override
    public @NotNull String getAuthor() {
        return "BodyHealth";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0";
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {

        if (params.startsWith("health_")) {
            String[] splitParams = params.split("_");

            if (splitParams.length >= 2) {
                StringBuilder bodyPartNameBuilder = new StringBuilder();

                for (int i = 1; i < splitParams.length; i++) {
                    if (splitParams[i].equalsIgnoreCase("rounded")) {
                        break;
                    }
                    if (i > 1) bodyPartNameBuilder.append("_");
                    bodyPartNameBuilder.append(splitParams[i].toUpperCase());
                }

                String bodyPartName = bodyPartNameBuilder.toString();
                BodyPart bodyPart;
                try {
                    bodyPart = BodyPart.valueOf(bodyPartName);
                } catch (IllegalArgumentException e) {
                    return "Invalid body part: " + bodyPartName;
                }
                if (player.isOnline()) {
                    Player onlinePlayer = player.getPlayer();
                    if (onlinePlayer != null) {
                        BodyHealth bodyHealth = BodyHealthUtils.getBodyHealth(onlinePlayer);
                        double partHealth = bodyHealth.getHealth(bodyPart);
                        if (splitParams[splitParams.length - 1].equalsIgnoreCase("rounded")) return String.valueOf((int) Math.round(partHealth));
                        return String.format("%.2f", partHealth); // Return health as a formatted string
                    }
                }
            }
        }
        if (params.startsWith("state_")) {

            String[] splitParams = params.split("_");
            if (splitParams.length > 1) {

                String bodyPartName = splitParams.length == 2 ? splitParams[1].toUpperCase() : splitParams[1].toUpperCase() + "_" + splitParams[2].toUpperCase();

                BodyPart bodyPart;
                try {
                    bodyPart = BodyPart.valueOf(bodyPartName);
                } catch (IllegalArgumentException e) {
                    return "Invalid body part: " + bodyPartName;
                }

                if (player.isOnline()) {
                    Player onlinePlayer = player.getPlayer();
                    if (onlinePlayer != null) {
                        return BodyHealthUtils.getBodyHealthState(BodyHealthUtils.getBodyHealth(onlinePlayer), bodyPart).name();
                    }
                }
            }
        }

        return null;
    }

}
