package fr.obeeron.lockrp.listeners;

import fr.obeeron.lockrp.LockRP;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Door;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class KeyListener implements Listener {

    private final LockRP plugin;

    public KeyListener(LockRP plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Block block = event.getClickedBlock();

        if ( block == null || event.getHand() != EquipmentSlot.HAND || !plugin.isLockable(block)) {
            return;
        }

        // If clicking on upper part of a door, set focus on the lower part
        if (block.getBlockData() instanceof Door door) {
            if (door.getHalf() == Door.Half.TOP) {
                block = block.getRelative(0, -1, 0);
            }
        }

        ItemStack item = event.getItem();
        boolean isHoldingKey =  item != null && plugin.isKey(item);
        boolean isHoldingBoundKey = isHoldingKey && plugin.isBoundKey(item);
        boolean isHoldingBlankKey = isHoldingKey && !isHoldingBoundKey;
        boolean isLocked = plugin.isLocked(block);

        event.setCancelled(true);

        if (isHoldingKey && player.isSneaking()) {
            if(event.getAction() == Action.RIGHT_CLICK_BLOCK)
            {
                if (isLocked) {
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(plugin.config.getString("responses.add_lock_failure.already_locked")));
                    player.playSound(block.getLocation(), Sound.BLOCK_CHAIN_STEP, 1, 1);
                }
                else if(isHoldingBlankKey)
                    plugin.tryCreateKeyLock(player, block, item);
                else
                    plugin.bindDoor(player, block, item);
            }
            else if(event.getAction() == Action.LEFT_CLICK_BLOCK)
            {
                if(isLocked && isHoldingBoundKey)
                    plugin.tryRemoveLock(player, block, item);
            }
        }
        else if (event.getAction() == Action.RIGHT_CLICK_BLOCK && isLocked) {
            if (isHoldingBoundKey)
                plugin.tryUseKey(player, block, item, event);
            else
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(plugin.config.getString("responses.unlock_failure.no_key")));
        }
        else
            event.setCancelled(false);
    }
}
