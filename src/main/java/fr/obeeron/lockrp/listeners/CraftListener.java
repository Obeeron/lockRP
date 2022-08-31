package fr.obeeron.lockrp.listeners;

import fr.obeeron.lockrp.LRPCore;
import fr.obeeron.lockrp.LockRP;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;

public class CraftListener implements Listener {

    LockRP plugin;

    public CraftListener(LockRP plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onCraft(PrepareItemCraftEvent event) {
        ItemStack[] items = event.getInventory().getMatrix();

        // Check if recipe is only 2 KEY_MATERIAL
        if(isKeyDuplicateRecipe(items)) {
            handleKeyDuplicateRecipe(event, items);
        }
    }

    private void handleKeyDuplicateRecipe(PrepareItemCraftEvent event, ItemStack[] items) {
        event.getInventory().setResult(new ItemStack(Material.AIR));

        // Check if there is a blank and bound key in the recipe
        ItemStack blankKey = null;
        ItemStack boundKey = null;
        for(ItemStack item : items) {
            if(item != null) {
                if(!LRPCore.isKey(item))
                    return;
                if(LRPCore.isBoundKey(item)) {
                    if(boundKey != null)
                        return;
                    boundKey = item;
                }
                else {
                    if(blankKey != null)
                        return;
                    blankKey = item;
                }
            }
        }

        if(blankKey == null || boundKey == null)
            return;

        ItemStack result = new ItemStack(plugin.getKeyMaterial(), 2);
        result.setItemMeta(boundKey.getItemMeta());
        event.getInventory().setResult(result);
    }

    private boolean isKeyDuplicateRecipe(ItemStack[] items) {
        int nbKeys = 0;
        for(ItemStack item : items) {
            if(item != null)
            {
                if(item.getType() == plugin.getKeyMaterial())
                    nbKeys++;
                else
                    return false;
            }
        }
        return nbKeys == 2;
    }
}
