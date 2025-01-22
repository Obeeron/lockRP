package fr.obeeron.lockrp.listeners;

import fr.obeeron.lockrp.LRPCore;
import fr.obeeron.lockrp.LockRP;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Door;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

public class BreakListener implements Listener {

    @SuppressWarnings("unused")
    private final LockRP plugin;

    public BreakListener(LockRP plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerBreak(org.bukkit.event.block.BlockBreakEvent event) {
        Block block = event.getBlock();
        // If clicking on upper part of a door, set focus on the lower part
        if (block.getBlockData() instanceof Door door) {
            if (door.getHalf() == Door.Half.TOP) {
                block = block.getRelative(0, -1, 0);
            }
        }

        if (LRPCore.isLocked(block)) {
            event.setCancelled(true);
            tryDestroyLock(block, event.getPlayer());
        }
        else
            onBlockBreak(block, event.getPlayer());
    }

    @EventHandler
    public void onBurn(org.bukkit.event.block.BlockBurnEvent event) {
        onBlockBreak(event.getBlock(), null);
    }

    @EventHandler
    public void onExplosion(org.bukkit.event.entity.EntityExplodeEvent event) {
        for (Block block : event.blockList())
            onBlockBreak(block, null);
    }

    // Piston destroy event
    @EventHandler
    public void onPistonExtend(org.bukkit.event.block.BlockPistonExtendEvent event) {
        for (Block block : event.getBlocks())
        {
            switch (block.getPistonMoveReaction()) {
                case MOVE:
                case BREAK:
                    if (LRPCore.isLocked(block))
                        event.setCancelled(true);
                    break;
                default:
                    break;
            }
        }
    }

    // Piston retract event
    @EventHandler
    public void onPistonRetract(org.bukkit.event.block.BlockPistonRetractEvent event) {
        for (Block block : event.getBlocks())
        {
            switch (block.getPistonMoveReaction()) {
                case MOVE:
                case BREAK:
                    if (LRPCore.isLocked(block))
                        event.setCancelled(true);
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * Check if the block is a lock and if it is, remove the lock
     * @param block the block to check
     * @param player The player who is breaking the block, null if not a player
     */
    private void onBlockBreak(Block block, Player player) {
        // =============================
        // Handle direct break of a lock
        // =============================
        if (LRPCore.isLocked(block))
            tryDestroyLock(block, player);

        // =============================
        // Handle break of a door
        // =============================
        if (block.getBlockData() instanceof Door door) {
            if(door.getHalf() == Door.Half.TOP) {
                Block blockUnder = block.getRelative(0, -1, 0);
                if(blockUnder.getBlockData() instanceof Door doorUnder && doorUnder.getHalf() == Door.Half.BOTTOM)
                    onBlockBreak(blockUnder, player);
            }
        }
        // =======================================================
        // Handle cascade break of door placed on the broken block
        // =======================================================
        else{
            Block blockAbove = block.getRelative(0, 1, 0);
            if (blockAbove.getBlockData() instanceof Door door && door.getHalf() == Door.Half.BOTTOM)
                onBlockBreak(blockAbove, player);
        }
    }

    public void tryDestroyLock(Block block, Player player) {
        if (player == null || canBreakLock(player)) {
            LRPCore.unbindLock(block);
            block.getWorld().playSound(block.getLocation(), Sound.BLOCK_CHAIN_BREAK, 1, 0);
            if (player != null)
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(LockRP.config.getString("responses.destroy_lock_success")));
        }
        else
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(LockRP.config.getString("responses.destroy_lock_failure")));
    }

    private boolean canBreakLock(Player player) {
        return isLockBreaker(player.getInventory().getItemInMainHand()) ||
                (player.hasPermission(LockRP.BREAKALL_PERM) &&
                        player.getGameMode() == org.bukkit.GameMode.CREATIVE);
    }

    private boolean isLockBreaker(ItemStack itemInMainHand) {
        return itemInMainHand.getType() == Material.NETHERITE_PICKAXE ||
                itemInMainHand.getType() == Material.DIAMOND_PICKAXE ||
                itemInMainHand.getType() == Material.IRON_PICKAXE ||
                itemInMainHand.getType() == Material.GOLDEN_PICKAXE;
    }
}
