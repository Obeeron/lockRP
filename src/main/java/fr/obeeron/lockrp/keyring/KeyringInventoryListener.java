package fr.obeeron.lockrp.keyring;

import fr.obeeron.lockrp.LRPCore;
import fr.obeeron.lockrp.LockRP;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;

public class KeyringInventoryListener implements Listener {

    private final LockRP plugin;

    public KeyringInventoryListener(LockRP plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (e.getClickedInventory() == null || !( e.getInventory().getHolder() instanceof KeyringInventory))
            return;

        KeyringInventory keyringInventory = (KeyringInventory) e.getInventory().getHolder();
        ItemStack keyRingItem = keyringInventory.getKeyringItem();

        ItemStack currentItem = e.getCurrentItem();
        ItemStack cursorItem = e.getCursor();

        // Player can only interact with keys
        if ((!LRPCore.isKey(currentItem)) && currentItem != null && currentItem.getType() != Material.AIR) {
            e.setCancelled(true);
        }
        // If players tries to shift click only allow it for keys
        else if (e.getClick() == ClickType.SHIFT_LEFT || e.getClick() == ClickType.SHIFT_RIGHT)
        {
            if (currentItem != null && !LRPCore.isKey(currentItem)) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent e) {
        // Only allow dragging of keys
        if (e.getInventory().getHolder() instanceof KeyringInventory) {
            if (!LRPCore.isKey(e.getOldCursor()))
                e.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e){
        Inventory inventory = e.getInventory();

        if (inventory.getHolder() instanceof KeyringInventory keyRingInventory &&
            e.getPlayer() instanceof Player player) {
            player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_CHAIN, 1, 1);
            keyRingInventory.onClose(e.getPlayer().getInventory());
        }
    }
}
