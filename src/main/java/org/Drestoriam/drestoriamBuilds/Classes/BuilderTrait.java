package org.Drestoriam.drestoriamBuilds.Classes;

import net.citizensnpcs.api.event.NPCRightClickEvent;
import net.citizensnpcs.api.persistence.Persist;
import net.citizensnpcs.api.trait.Trait;
import net.citizensnpcs.api.trait.TraitName;
import net.citizensnpcs.api.util.DataKey;
import org.Drestoriam.drestoriamBuilds.DrestoriamBuilds;
import org.Drestoriam.drestoriamBuilds.SchemAPI.Scheduler;
import org.Drestoriam.drestoriamBuilds.SchemAPI.Schematic;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@TraitName("Builder")
public class BuilderTrait extends Trait {

    protected BuilderTrait() {
        super("Builder");
        plugin = JavaPlugin.getPlugin(DrestoriamBuilds.class);
    }

    DrestoriamBuilds plugin = null;

    @Persist Map<Material, Integer> itemMap;
    @Persist String schematicName;
    @Persist int pacing;
    @Persist boolean building = false;
    Schematic schematic;

    public void load(DataKey key){



    }

    public void save(DataKey key){


    }

    @EventHandler
    public void click(NPCRightClickEvent event){

        if(event.getNPC() != this.getNPC()) return;

        Inventory NPCinventory = Bukkit.createInventory((Player) this.getNPC(), 27, "Material Deposit");
        event.getClicker().openInventory(NPCinventory);

    }

    @EventHandler
    public void close(InventoryCloseEvent event) {

        if (event.getInventory().getHolder() != this.getNPC()) return;

        Inventory NPCinventory = event.getInventory();
        ItemStack[] itemContents = NPCinventory.getContents();

        //Check if the items provided in the inventory are required
        for (ItemStack item : itemContents) {

            if (itemMap.containsKey(item.getType())) {

                Material type = item.getType();
                int currentRemaining = itemMap.get(type);

                //Check if the remaining required amount is more or less than provided
                if (currentRemaining >= item.getAmount()) {

                    itemMap.put(type, currentRemaining - item.getAmount());

                //Returns items provided greater than the remaining amount
                } else {

                    //Set the material to 0 to count it as complete
                    //Then get the refunded amount to return it to the player
                    //i.e. 12 remaining, but 35 provided becomes 12-35 = -23 * -1 = 23 items refunded
                    itemMap.put(type, 0);
                    int newBalance = -1 * (currentRemaining - item.getAmount());
                    item.setAmount(newBalance);
                    event.getPlayer().getInventory().addItem(item);

                }

                //Return items to player if not required
            } else {

                event.getPlayer().getInventory().addItem(item);

            }

        }

        //Check that all items require no more, return list of remaining items if true
        StringBuilder remainingitems = new StringBuilder();
        for (Material material : itemMap.keySet()) {

            if (itemMap.get(material) != 0 && remainingitems.isEmpty()) {

                remainingitems.append(ChatColor.DARK_AQUA + "-= Remaining Items =-\n");
                remainingitems.append(ChatColor.DARK_AQUA + itemMap.get(material).toString() + " " + material.toString() + "\n");

            } else if (itemMap.get(material) != 0){

                remainingitems.append(ChatColor.DARK_AQUA + itemMap.get(material).toString() + " " + material.toString() + "\n");

            }

        }

        if(!remainingitems.isEmpty()){

            remainingitems.append(ChatColor.DARK_AQUA + "-====+====-");
            event.getPlayer().sendMessage(remainingitems.toString());
            return;

        }

        //If all materials are provided, start construction
        building = true;
        Vector inverse = this.getNPC().getStoredLocation().getDirection().multiply(-1);

        Collection<Location> locationCollection = schematic.pasteSchematic(
                this.getNPC().getStoredLocation().add(inverse.multiply(2)),
                (Player) this.getNPC(),
                schematic.getPacing(),
                Schematic.Options.IGNORE_TRANSPARENT, Schematic.Options.PLACE_ANYWHERE, Schematic.Options.REALISTIC);
        if (locationCollection != null) {
            List<Location> locations = new ArrayList<>(locationCollection);
            Scheduler scheduler = new Scheduler();
            scheduler.setTask(Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
                for (Location location : locations) {
                    if (locations.get(locations.size() - 1).getBlock().getType() != Material.AIR) {
                        scheduler.cancel();
                    } else {
                        if (location.getBlock().getType() == Material.AIR) {
                            location.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, location.getX() + 0.5D, location.getY(), location.getZ() + 0.5D, 2);
                        }
                    }
                }
            }, 0L, 40L));

        }

        this.npc.destroy();
    }

    @Override
    public void onSpawn(){

        File schematicFile = new File(plugin.getDataFolder(),"schematics\\" + schematicName + ".schem");

        schematic = new Schematic(plugin, schematicFile, pacing);
        itemMap = schematic.getSchematicMaterialData();

    }

    @Override
    public void onDespawn(){



    }

}

/*public class ExplicitHashmapPersister implements Persister<HashMap<Material, Integer>> {

    public HashMap<Material, Integer> create(DataKey root){
        return new HashMap<Material, Integer>(root.getInt("MaterialMap"));
    }

    @Override
    public void save(HashMap<Material, Integer> instance, DataKey root) {

        HashMap<Material, Integer> real = instance;
        root.setInt("MaterialMap", real.clone() );

    }

}*/
