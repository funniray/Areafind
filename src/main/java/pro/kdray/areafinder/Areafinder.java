package pro.kdray.areafinder;

import org.bukkit.plugin.java.JavaPlugin;

public final class Areafinder extends JavaPlugin {

    @Override
    public void onEnable() {
        // Plugin startup logic
        this.getCommand("/room").setExecutor(new HandleCommands(this));
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
