package mitality.bodyHealthLegacy.depend;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import mitality.bodyHealthLegacy.config.Debug;
import org.bukkit.entity.Player;

public class WorldGuard {
    public static  StateFlag BODY_HEALTH_FLAG = null;

    public static void initialize() {
        try {
            Debug.log("Trying to register custom flag 'bodyhealth'...");
            StateFlag flag = new StateFlag("bodyhealth", true);
            com.sk89q.worldguard.WorldGuard.getInstance().getFlagRegistry().register(flag);
            BODY_HEALTH_FLAG = flag;
            Debug.log("Custom flag 'bodyhealth' has been registered.");
        } catch (Exception e) {
            Debug.logErr("Failed to register custom flag: " + e.getMessage());
        }
    }

    public static boolean isSystemEnabled(Player player) {
        if (BODY_HEALTH_FLAG == null) return true;

        RegionContainer container = com.sk89q.worldguard.WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regions = container.get(BukkitAdapter.adapt(player.getWorld()));
        LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(player);

        if (regions == null) return true;

        ApplicableRegionSet regionSet = regions.getApplicableRegions(BukkitAdapter.asBlockVector(player.getLocation()));
        return regionSet.testState(localPlayer, BODY_HEALTH_FLAG);
    }
}