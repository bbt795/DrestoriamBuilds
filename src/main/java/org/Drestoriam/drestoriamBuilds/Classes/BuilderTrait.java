package org.Drestoriam.drestoriamBuilds.Classes;

import com.drestoriam.drestoriammoney.DrestoriamMoney;
import com.mordonia.mcore.MCore;
import com.mordonia.mcore.MCoreAPI;
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
import java.math.BigDecimal;
import java.util.*;

import static org.Drestoriam.drestoriamBuilds.DrestoriamBuilds.tag;

@TraitName("Builder")
public class BuilderTrait extends Trait {

    public BuilderTrait() {
        super("Builder");
        buildsPlugin = JavaPlugin.getPlugin(DrestoriamBuilds.class);
        moneyPlugin = JavaPlugin.getPlugin(DrestoriamMoney.class);
        mCoreAPI = MCore.getPlugin(MCore.class).getmCoreAPI();
    }

    DrestoriamBuilds buildsPlugin = null;
    DrestoriamMoney moneyPlugin = null;
    MCoreAPI mCoreAPI = null;

    @Persist Map<Material, Integer> itemMap;
    @Persist String schematicName;
    @Persist int pacing;
    @Persist String price;
    @Persist boolean paid = false;
    @Persist boolean building = false;
    Schematic schematic;

    @Override
    public void onSpawn(){

        schematicName = this.npc.data().get("schematicName");
        pacing = this.npc.data().get("pacing");
        price = this.npc.data().get("price");

        File schematicFile = new File(buildsPlugin.getDataFolder(), "schematics/" + schematicName + ".schem");
        schematic = new Schematic(buildsPlugin, schematicFile, pacing);

        if(itemMap == null) {

            System.out.println("ItemMap is null...");
            itemMap = schematic.getSchematicMaterialData();

        }

    }

    @Override
    public void load(DataKey key){

        itemMap = new HashMap<>();
        DataKey itemKey = key.getRelative("itemMap");
        Map<String, Object> testItemMap = itemKey.getValuesDeep();

        for(String mat : testItemMap.keySet()){

            Material material = Material.matchMaterial(mat);
            itemMap.put(material, (Integer) testItemMap.get(mat));

        }

    }

    @EventHandler
    public void click(NPCRightClickEvent event){

        if(event.getNPC() != this.getNPC()) return;
        if(building) {

            event.getClicker().sendMessage(tag + ChatColor.DARK_AQUA + "Building in Progress...");
            return;

        }

        Player player = event.getClicker();

        if(!paid){

            if(!player.hasPermission("dremoney.cityleader.*")){

                player.sendMessage(tag + ChatColor.RED + "Your city leader must pay a fee of $" + price + " before construction can begin.");
                return;

            }

            String playerKingdom = mCoreAPI.getmPlayerManager().getPlayerMap().get(player.getUniqueId().toString()).getKingdom();
            BigDecimal balance = new BigDecimal(moneyPlugin.getConfig().getString("citybanks." + playerKingdom + ".balance"));
            BigDecimal priceBD = new BigDecimal(price);

            if(balance.compareTo(priceBD) < 0){

                player.sendMessage(tag + ChatColor.RED + "Your city doesn't have enough money to fund this build.");
                return;

            }

            moneyPlugin.getConfig().set("citybanks." + playerKingdom + ".balance", balance.subtract(priceBD));
            moneyPlugin.saveConfig();
            paid = true;
            player.sendMessage(tag + ChatColor.GREEN + "Successfully paid! Right click again to see the required materials!");
            return;

        }

        Inventory NPCinventory = Bukkit.createInventory(null, 27, "Material Deposit");
        event.getClicker().openInventory(NPCinventory);

    }

    @EventHandler
    public void close(InventoryCloseEvent event) {

        if (!event.getView().getTitle().equals("Material Deposit")) return;

        boolean itemsExist = false;
        Inventory NPCinventory = event.getInventory();
        ItemStack[] itemContents = NPCinventory.getContents();

        for(ItemStack item: itemContents){

            if(item != null){

                itemsExist = true;
                break;

            }

        }

        if (!itemsExist){

            cleanItems();
            event.getPlayer().sendMessage(printMaterials());
            return;

        }

        //Check if the items provided in the inventory are required
        for (ItemStack item : itemContents) {

            if(item == null) continue;

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
        for (Material material : itemMap.keySet()) {

            if(itemMap.get(material) != 0){

                cleanItems();
                event.getPlayer().sendMessage(printMaterials());
                return;

            }

        }

        //If all materials are provided, start construction
        building = true;
        Vector inverse = this.getNPC().getStoredLocation().getDirection().multiply(-1).setY(-2);

        Collection<Location> locationCollection = schematic.pasteSchematic(
                this.getNPC().getStoredLocation().add(inverse),
                (Player) this.getNPC().getEntity(),
                schematic.getPacing(),
                Schematic.Options.IGNORE_TRANSPARENT, Schematic.Options.PLACE_ANYWHERE, Schematic.Options.REALISTIC);
        if (locationCollection != null) {
            List<Location> locations = new ArrayList<>(locationCollection);
            Scheduler scheduler = new Scheduler();
            scheduler.setTask(Bukkit.getScheduler().scheduleSyncRepeatingTask(buildsPlugin, () -> {
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

    private String printMaterials(){

        StringBuilder remainingitems = new StringBuilder();
        for (Material material : itemMap.keySet()) {

            if(material.isAir()){

                continue;

            }

            if (itemMap.get(material) != 0 && remainingitems.isEmpty()) {

                remainingitems.append(ChatColor.DARK_AQUA + "-= Remaining Items =-\n");
                remainingitems.append(ChatColor.DARK_AQUA + itemMap.get(material).toString() + " " + material + "\n");

            } else if (itemMap.get(material) != 0){

                remainingitems.append(ChatColor.DARK_AQUA + itemMap.get(material).toString() + " " + material + "\n");

            }

        }

        remainingitems.append(ChatColor.DARK_AQUA + "-====++++++++====-");

        return remainingitems.toString();

    }

    private void cleanItems(){

        for (Material material : itemMap.keySet()) {

            if(material.isAir()) itemMap.put(material, 0);

            if(Tag.WALL_SIGNS.isTagged(material) || Tag.WALL_HANGING_SIGNS.isTagged(material)){

                String materialName = material.toString();
                String woodType = materialName.substring(0, materialName.indexOf('_'));
                String signType = materialName.substring(materialName.indexOf('_') + 6);

                itemMap.put(Material.getMaterial(woodType + "_" + signType), itemMap.get(Material.getMaterial(woodType + "_" + signType)) + itemMap.get(material));
                itemMap.put(material, 0);

            }


        }

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
