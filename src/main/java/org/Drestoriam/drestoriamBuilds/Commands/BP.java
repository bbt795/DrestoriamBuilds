package org.Drestoriam.drestoriamBuilds.Commands;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import org.Drestoriam.drestoriamBuilds.Classes.BuilderTrait;
import org.Drestoriam.drestoriamBuilds.DrestoriamBuilds;
import org.Drestoriam.drestoriamBuilds.SchemAPI.Schematic;
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

                Schematic schematic = new Schematic(plugin, schematicFile, pacing);

                NPC npc = CitizensAPI.getNPCRegistry().createNPC(EntityType.PLAYER, "Builder");
                npc.addTrait(BuilderTrait.class);
                npc.data().setPersistent("schematicName", schematicName);
                npc.data().setPersistent("pacing", pacing);
                npc.spawn(player.getLocation());

                /*Thoughts so far:

                - Create NPC at Player location
                - Have NPC store schematic name and pacing value persistently
                - Use Citizens click event to open inventory for item deposits and store block counts
                - Adapt SchematicListener into BuildTask, to remove the need for click initiation
                - Once NPC meets the blocks needed, it'll call BuildTask and start the building behind it
                - NPC despawns/is removed once building is complete

                This command would essentially just create the NPC, add the 'builder' trait, and then spawn the NPC
                Seems like that may be the right idea???
                 */

                player.sendMessage(tag + ChatColor.GREEN + "Success");

                break;

            case "update":
                //Logic for updating blueprint pace
                // /bp update [blueprint name] [pacing]

                /*

                String blueprintName = args[1];
                pacingMap.set(blueprintName, Integer.valueOf(args[2]));

                 */
                break;

            case "npchere":
                //Logic for moving NPC
                // /bp npchere
                break;

            case "npcremove":
                //Logic for removing NPC
                // /bp npcremove
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

            player.sendMessage(ChatColor.DARK_AQUA + "- /bp create [blueprint] [schematic name] [pacing]");
            player.sendMessage(ChatColor.DARK_AQUA + "- /bp update [blueprint name] [pacing in blocks/hour]");
            player.sendMessage(ChatColor.DARK_AQUA + "- /bp npchere");
            player.sendMessage(ChatColor.DARK_AQUA + "- /bp npcremove");

        }

        player.sendMessage(ChatColor.DARK_BLUE + "-===--+++++++++++++++++--===-");

    }

}
