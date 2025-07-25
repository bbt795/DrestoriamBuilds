package org.Drestoriam.drestoriamBuilds.Commands;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import org.Drestoriam.drestoriamBuilds.Classes.BuilderTrait;
import org.Drestoriam.drestoriamBuilds.DrestoriamBuilds;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.io.File;

import static org.Drestoriam.drestoriamBuilds.DrestoriamBuilds.*;

public class BP implements CommandExecutor {

    private DrestoriamBuilds plugin;

    public BP (DrestoriamBuilds plugin){

        this.plugin = plugin;

    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if(!(sender instanceof Player)) {
            System.out.println("[DrestoriamBuilds] Must send command via player");
            return true;
        }

        Player player = (Player) sender;

        if(args.length < 1){

            printHelp(player);
            return true;

        }

        switch(args[0]){

            case "help":
                printHelp(player);
                break;

            case "create":
                //Logic for creating blueprint
                // /bp create [schematic name] [pacing in ticks per block]

                if(args.length != 3){

                    player.sendMessage(tag + ChatColor.RED + "Did you mean /bp create [schematic name] [pacing]?");
                    return true;

                }

                if(!player.hasPermission("bp.*")){

                    player.sendMessage(tag + ChatColor.RED + "You do not have permission for this command");
                    return true;

                }

                File schemFile = new File(plugin.getDataFolder(), "schematics");
                if(!schemFile.exists()){

                    schemFile.mkdir();

                }

                String schematicName = args[1];
                int pacing = Integer.parseInt(args[2]);

                File schematicFile = new File(plugin.getDataFolder(),"schematics\\" + schematicName + ".schem");

                if(!schematicFile.exists()){

                    player.sendMessage(tag + ChatColor.RED + "This schematic file does not exist");
                    return true;

                }

                NPC npc = CitizensAPI.getNPCRegistry().createNPC(EntityType.PLAYER, "Builder");
                npc.addTrait(BuilderTrait.class);
                npc.data().setPersistent("schematicName", schematicName);
                npc.data().setPersistent("pacing", pacing);
                npc.spawn(player.getLocation());

                player.sendMessage(tag + ChatColor.GREEN + "Success");

                break;


            default:
                player.sendMessage(tag + ChatColor.RED + "Please use a valid command, like /bp help");
                break;

        }


        return true;
    }

    private void printHelp(Player player){

        player.sendMessage(ChatColor.DARK_BLUE + "-===--+ DrestoriamBuilds Help +--===-");
        player.sendMessage(ChatColor.DARK_AQUA + "- /bp help");

        if(player.hasPermission("bp.help.staff")){

            player.sendMessage(ChatColor.DARK_AQUA + "- /bp create [schematic name] [pacing]");

        }

        player.sendMessage(ChatColor.DARK_BLUE + "-===--+++++++++++++++++--===-");

    }

}
