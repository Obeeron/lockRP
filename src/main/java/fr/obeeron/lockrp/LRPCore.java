package fr.obeeron.lockrp;

import com.jeff_media.customblockdata.CustomBlockData;
import fr.obeeron.lockrp.keyring.KeyArrayDataType;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.block.data.Openable;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Objects;
import java.util.Random;
import java.util.concurrent.locks.Lock;

public class LRPCore {
    private static LockRP plugin;
    private static FileConfiguration config;
    static Random random = new Random();

    public static boolean isLockable(Block block) {
        if (block == null)
            return false;
        return block.getBlockData() instanceof Openable || block.getState() instanceof Container;
    }

    public static boolean isLocked(Block block) {
        PersistentDataContainer blockData = new CustomBlockData(block, plugin);
        return blockData.has(LockRP.lockIdNSK, PersistentDataType.INTEGER);
    }

    public static boolean isKey(ItemStack item) {
        if(item == null)
            return false;
        ItemMeta meta = item.getItemMeta();
        if(meta == null)
            return false;
        return Objects.requireNonNull(item.getItemMeta()).getPersistentDataContainer().has(LockRP.lockIdNSK, PersistentDataType.INTEGER);
    }

    // Returns true if the player is holding a key (a plugin.KEY_MATERIAL named "key")
    public static boolean isBoundKey(ItemStack item) {
        if (item == null || item.getItemMeta() == null)
            return false;
        return getLockId(item) != -1;
    }

    public static void tryInteract(Player player, Block block, ItemStack item, PlayerInteractEvent event) {
        if(!canBypassLock(player) && !hasMatchingKey(player, getLockId(block))) {
            if(isBoundKey(item))
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(config.getString("responses.unlock_failure.wrong_key")));
            else
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(config.getString("responses.unlock_failure.no_key")));
            player.playSound(block.getLocation(), Sound.BLOCK_CHAIN_STEP, 1, 1);
            return;
        }

        // If the block is Openable (door, gate, trapdoor) open or close it
        if (block.getBlockData() instanceof Openable openable) {
            openable.setOpen(!openable.isOpen());
            block.setBlockData(openable);
        }
        else if (block.getState() instanceof InventoryHolder) {
            event.setCancelled(false);
        }

        block.getWorld().playSound(block.getLocation(), Sound.BLOCK_IRON_TRAPDOOR_OPEN, 1, 1.0f);
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(config.getString("responses.unlock_success")));
        player.playSound(block.getLocation(), Sound.BLOCK_CHAIN_BREAK, 1, 1);
    }

    /**
     * Create a new lock and bind the key to it.
     * @param player The player locking the block or null
     * @param block The block to lock
     * @param item The key to use
     */
    public static void tryCreateLock(Player player, Block block, ItemStack item) {
        if(isLocked(block)) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(LockRP.config.getString("responses.add_lock_failure.already_locked")));
            player.playSound(block.getLocation(), Sound.BLOCK_CHAIN_STEP, 1, 1);
            return;
        }
        boolean isBoundKey = isBoundKey(item);
        int newLockId = isBoundKey ? getLockId(item) : getNextLockId();

        if(isBoundKey || tryCreateKey(player, item, newLockId)) {
            bindBlock(block, newLockId);
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(config.getString("responses.add_lock_success")));
            plugin.getLogger().info(String.format("%s(%s) locked the block x:%d y:%d z:%d, with KeyID:%d",
                    player.getName(),player.getDisplayName(), block.getX(), block.getY(), block.getZ(), newLockId));
        }
    }

    public static boolean tryCreateKey(Player player, ItemStack key, int lockId) {
        // Exit if inventory is full
        if (player.getInventory().firstEmpty() == -1 && key.getAmount() > 1) {
            String playerResponse = LockRP.config.getString("responses.create_key_failure.inventory_full", "Your inventory is full!");
            player.sendMessage(playerResponse);
            return false;
        }

        // Set the key meta
        ItemMeta keyMeta = key.getItemMeta();
        assert keyMeta != null;
        keyMeta.addEnchant(Enchantment.LOYALTY, 1, true);
        keyMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        keyMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        keyMeta.setDisplayName("Key");

        // Set the key's lock id
        bindKey(keyMeta, lockId);

        // Create a bound key and remove the original one
        if(key.getAmount() == 1) {
            key.setItemMeta(keyMeta);
        } else {
            key.setAmount(key.getAmount() - 1);
            ItemStack boundKey = new ItemStack(LockRP.KEY_MATERIAL);
            boundKey.setItemMeta(keyMeta);
            player.getInventory().addItem(boundKey);
        }

        player.sendMessage(LockRP.config.getString("responses.create_key_info.rename_key", "You can rename your key with /name <Key name>"));

        return true;
    }

    private static boolean hasMatchingKey(Player player, int lockId) {
        for(ItemStack item : player.getInventory().getContents()) {
            if(isBoundKey(item) && getLockId(item) == lockId)
                return true;
            if(isKeyring(item)){
                ItemStack[] keyRingContent = getKeysFromKeyring(item);
                for(ItemStack key : keyRingContent) {
                    if(isBoundKey(key) && getLockId(key) == lockId)
                        return true;
                }
            }
        }
        return false;
    }

    private static boolean canBypassLock(Player player) {
        return player.hasPermission(LockRP.LOCK_BYPASS_PERM) && (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR);
    }

    /**
     * Create a lock on a block with a given id.
     * @param block The block to lock
     * @param lockId The lock id
     */
    private static void bindBlock(Block block, int lockId) {
        PersistentDataContainer blockData = new CustomBlockData(block, plugin);
        blockData.set(LockRP.lockIdNSK, PersistentDataType.INTEGER, lockId);
        block.getWorld().playSound(block.getLocation(), Sound.BLOCK_CHAIN_BREAK, 1, 1);
    }

    /**
     * Bind a key to a lock.
     * @param itemMeta The meta of the key to bind
     * @param lockId The lock to bind to.
     */
    private static void bindKey(ItemMeta itemMeta, int lockId) {
        PersistentDataContainer pdc = itemMeta.getPersistentDataContainer();
        pdc.set(LockRP.lockIdNSK, PersistentDataType.INTEGER, lockId);
    }

    private static int getNextLockId() {
        return random.nextInt(Integer.MAX_VALUE);
    }

    public static ItemStack createBlankKey() {
        ItemStack keyItem = new ItemStack(LockRP.KEY_MATERIAL);
        ItemMeta keyMeta = keyItem.getItemMeta();
        assert keyMeta != null;
        keyMeta.setDisplayName("Key");
        keyMeta.getPersistentDataContainer().set(LockRP.lockIdNSK, PersistentDataType.INTEGER, -1);
        keyMeta.getPersistentDataContainer().set(LockRP.customItemIdNSK, PersistentDataType.STRING, LockRP.UNIVID_KEY);
        keyItem.setItemMeta(keyMeta);
        return keyItem;
    }

    public static boolean isKeyring(ItemStack item) {
        if(item == null || item.getType() != LockRP.KEYRING_MATERIAL)
            return false;
        ItemMeta itemMeta = item.getItemMeta();
        if (itemMeta == null)
            return false;
        if (itemMeta.getPersistentDataContainer().has(LockRP.customItemIdNSK, PersistentDataType.INTEGER))
            return false;
        String customItemId = itemMeta.getPersistentDataContainer().get(LockRP.customItemIdNSK, PersistentDataType.STRING);
        return customItemId != null && (customItemId.equals(LockRP.UNIVID_KEYRING_EMPTY) || customItemId.equals(LockRP.UNIVID_KEYRING));
    }

    public static ItemStack createEmptyKeyring() {
        ItemStack keyRingItem = new ItemStack(LockRP.KEYRING_MATERIAL);
        ItemMeta keyRingMeta = keyRingItem.getItemMeta();
        assert keyRingMeta != null;
        keyRingMeta.setDisplayName(LockRP.config.getString("keyring.item_name", "Keyring"));
        keyRingMeta.getPersistentDataContainer().set(LockRP.customItemIdNSK, PersistentDataType.STRING, LockRP.UNIVID_KEYRING_EMPTY);
        keyRingMeta.getPersistentDataContainer().set(LockRP.serializedKeysNSK, new KeyArrayDataType(), new ItemStack[9]);
        keyRingItem.setItemMeta(keyRingMeta);

        return keyRingItem;
    }

    public static ItemStack[] getKeysFromKeyring(ItemStack keyRing) {
        ItemMeta keyRingMeta = keyRing.getItemMeta();
        assert keyRingMeta != null;
        return keyRingMeta.getPersistentDataContainer().get(LockRP.serializedKeysNSK, new KeyArrayDataType());
    }

    public static int getNextKeyringId() {
        return random.nextInt(Integer.MAX_VALUE);
    }

    public static void tryRemoveLock(Player player, Block block, ItemStack item) {
        if(!isMatching(getLockId(block), item))
        {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(config.getString("responses.remove_lock_failure.wrong_key")));
            player.playSound(block.getLocation(), Sound.BLOCK_CHAIN_STEP, 1, 1);
        }

        removeLock(block, player);
    }

    public static void unbindLock(Block block) {
        PersistentDataContainer blockData = new CustomBlockData(block, plugin);
        blockData.remove(LockRP.lockIdNSK);
    }

    public static void removeLock(Block block, Player player) {
        unbindLock(block);

        block.getWorld().playSound(block.getLocation(), Sound.BLOCK_CHAIN_BREAK, 1, 0);
        if(player != null) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(config.getString("responses.remove_lock_success")));
        }
    }

    private static int getLockId(ItemStack item) {
        Integer keyID = Objects.requireNonNull(item.getItemMeta()).getPersistentDataContainer().get(LockRP.lockIdNSK, PersistentDataType.INTEGER);
        return keyID==null?-1:keyID;
    }

    private static int getLockId(Block block) {
        PersistentDataContainer blockData = new CustomBlockData(block, plugin);
        Integer lockID = blockData.get(LockRP.lockIdNSK, PersistentDataType.INTEGER);
        return lockID==null?-1:lockID;
    }

    private static boolean isMatching(int lockId, ItemStack item) {
        if (item == null)
            return false;
        int keyId = getLockId(item);
        return keyId != -1 && keyId == lockId;
    }

    public static void setPlugin(LockRP plugin) {
        LRPCore.plugin = plugin;
    }

    public static void setConfig(FileConfiguration config) {
        LRPCore.config = config;
    }
}
