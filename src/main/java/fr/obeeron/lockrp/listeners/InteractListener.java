package fr.obeeron.lockrp.listeners;

import fr.obeeron.lockrp.LRPCore;
import fr.obeeron.lockrp.LockRP;
import fr.obeeron.lockrp.keyring.KeyringInventory;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Door;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class InteractListener implements Listener {

    private final LockRP plugin;

    public InteractListener(LockRP plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Block block = event.getClickedBlock();
        ItemStack item = event.getItem();

        // LocKRP only works on right hand, so return if left hand is used
        if (event.getHand() != EquipmentSlot.HAND)
            return;

        // Using a Keyring
        if(LRPCore.isKeyring(item)) {
            onKeyringInteract(player, item, event);
            return;
        }

        // return if block is not a door
        if (!LRPCore.isLockable(block))
            return;

        // If clicking on upper part of a door, set focus on the lower part
        if (block.getBlockData() instanceof Door door) {
            if (door.getHalf() == Door.Half.TOP)
                block = block.getRelative(0, -1, 0);
        }

        Action action = event.getAction();
        boolean isHoldingKey = LRPCore.isKey(item);
        boolean isHoldingBoundKey = isHoldingKey && LRPCore.isBoundKey(item);

        event.setCancelled(true);
        if (player.isSneaking() && action == Action.RIGHT_CLICK_BLOCK && isHoldingKey )
            LRPCore.tryCreateLock(player, block, item);
        else if(LRPCore.isLocked(block)){
            if(action == Action.RIGHT_CLICK_BLOCK)
                LRPCore.tryInteract(player, block, item, event);
            else if (isHoldingBoundKey && player.isSneaking())
                LRPCore.tryRemoveLock(player, block, item);
            else
                event.setCancelled(false);
        }
        else
            event.setCancelled(false);
    }

    public void onKeyringInteract(Player player, ItemStack keyRing, PlayerInteractEvent event) {
        event.setCancelled(true);
        KeyringInventory keyRingInventory = new KeyringInventory(keyRing);
        player.playSound(player.getLocation(), Sound.BLOCK_CHAIN_BREAK, 1, 1.5f);
        player.openInventory(keyRingInventory.getInventory());
    }
}
