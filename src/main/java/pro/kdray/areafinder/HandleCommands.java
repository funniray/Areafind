package pro.kdray.areafinder;

import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.bukkit.selections.Selection;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Created by funniray on 3/31/18.
 */
public class HandleCommands implements CommandExecutor {

    Areafinder plugin;
    WorldEditPlugin WEPlugin;

    public HandleCommands(Areafinder plugin){
        this.plugin = plugin;
        this.WEPlugin = (WorldEditPlugin) plugin.getServer().getPluginManager().getPlugin("WorldEdit");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)){
            sender.sendMessage("You must be a player to use this command");
            return false;
        }
        Player player = (Player) sender;
        this.plugin.getServer().getScheduler().runTaskAsynchronously(this.plugin,()->{
            try {
                Selection selection = utils.find(player);
                plugin.getServer().getScheduler().runTaskAsynchronously(this.plugin,()->{
                    WEPlugin.setSelection(player,selection);
                    player.sendMessage("Set your selection!");
                });
            } catch (InvalidRegionException e1) {
                player.sendMessage("This is an invalid room!");
            }
        });

        return true;
    }
}
