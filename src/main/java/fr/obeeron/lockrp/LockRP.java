package fr.obeeron.lockrp;

import com.jeff_media.customblockdata.CustomBlockData;
import fr.obeeron.lockrp.listeners.BreakListener;
import fr.obeeron.lockrp.listeners.CraftListener;
import fr.obeeron.lockrp.listeners.KeyListener;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.block.data.Openable;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;
import java.util.Random;


public class LockRP extends JavaPlugin {

    public FileConfiguration config;

    public static final String BREAKALL_PERM = "lockrp.break_any";
    public static final String LOCK_BYPASS_PERM = "lockrp.bypass";

    private Material KEY_MATERIAL;

    @SuppressWarnings( "deprecation" )
    private final NamespacedKey universimIdNSK = new NamespacedKey("universim", "id");
    private final NamespacedKey lockIdNSK = new NamespacedKey(this, "lockid");


    @Override
    public void onEnable() {
        loadConfig();

        getServer().getPluginManager().registerEvents(new KeyListener(this), this);
        getServer().getPluginManager().registerEvents(new BreakListener(this), this);
        getServer().getPluginManager().registerEvents(new CraftListener(this), this);

        addRecipes();

        getLogger().info("LockRP is enabled!");
    }

    private void loadConfig() {
        saveDefaultConfig();
        getLogger().info("Config loaded!");
        config = getConfig();

        String keyMaterialStr = config.getString("key_material");
        if(keyMaterialStr == null) {
            getLogger().warning("Key materiel not indicated, defaulting to STICK");
            keyMaterialStr = "STICK";
        }
        KEY_MATERIAL = Material.getMaterial(keyMaterialStr);

    }

    public Material getKeyMaterial() {
        return KEY_MATERIAL;
    }

    @Override
    public void onDisable() {
        getLogger().info("LockRP is disabled!");
    }

    private void addRecipes() {
        ShapedRecipe keyRecipe = new ShapedRecipe(NamespacedKey.minecraft("key"), createBlankKey());

        keyRecipe.shape(" N ", "NIN", " S ");
        keyRecipe.setIngredient('N', Material.IRON_NUGGET);
        keyRecipe.setIngredient('I', Material.IRON_INGOT);
        keyRecipe.setIngredient('S', KEY_MATERIAL);
        getServer().addRecipe(keyRecipe);

        // ---------------------------------------------------------------
        ShapelessRecipe keyDuplicateRecipe = new ShapelessRecipe(NamespacedKey.minecraft("key_duplicate"), new ItemStack(KEY_MATERIAL));
        keyDuplicateRecipe.addIngredient(KEY_MATERIAL);
        keyDuplicateRecipe.addIngredient(KEY_MATERIAL);
        getServer().addRecipe(keyDuplicateRecipe);

        // ---------------------------------------------------------------


        getLogger().info("Key recipe added!");
    }



    private ItemStack createBlankKey() {
        ItemStack keyItem = new ItemStack(KEY_MATERIAL);
        ItemMeta keyMeta = keyItem.getItemMeta();
        assert keyMeta != null;
        keyMeta.setDisplayName("Key");
        keyMeta.getPersistentDataContainer().set(lockIdNSK, PersistentDataType.INTEGER, -1);
        keyMeta.getPersistentDataContainer().set(universimIdNSK, PersistentDataType.INTEGER, 1);
        keyItem.setItemMeta(keyMeta);
        return keyItem;
    }

    public boolean isLockable(Block block) {
        return block.getBlockData() instanceof Openable || block.getState() instanceof Container;
    }

    public boolean isLocked(Block block) {
        PersistentDataContainer blockData = new CustomBlockData(block, this);
        return blockData.has(lockIdNSK, PersistentDataType.INTEGER);
    }

    public boolean isKey(ItemStack item) {
        if(item == null)
            return false;
        return Objects.requireNonNull(item.getItemMeta()).getPersistentDataContainer().has(lockIdNSK, PersistentDataType.INTEGER);
    }

    // Returns true if the player is holding a key (a plugin.KEY_MATERIAL named "key")
    public boolean isBoundKey(ItemStack item) {
        if (item.getItemMeta() == null)
            return false;
        return getLockId(item) != -1;
    }

    public boolean tryRemoveLock(Player player, Block block, ItemStack item) {
        if(!isMatching(item,block))
        {
            if(player != null) {
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(config.getString("responses.remove_lock_failure.wrong_key")));
                player.playSound(block.getLocation(), Sound.BLOCK_CHAIN_STEP, 1, 1);
            }
            return false;
        }

        removeLock(block, player);
        return true;
    }

    public void unbindLock(Block block) {
        PersistentDataContainer blockData = new CustomBlockData(block, this);
        blockData.remove(lockIdNSK);
    }

    public void removeLock(Block block, Player player) {
        unbindLock(block);

        block.getWorld().playSound(block.getLocation(), Sound.BLOCK_CHAIN_BREAK, 1, 0);
        if(player != null) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(config.getString("responses.remove_lock_success")));
        }
    }

    /**
     * Create a new lock and bind the key to it.
     * @param player The player locking the block or null
     * @param block The block to lock
     * @param item The key to use
     * @return true if the lock and key was successfully created, false otherwise
     */
    public boolean tryCreateKeyLock(Player player, Block block, ItemStack item) {
        int newLockId = getNextLockId();

        if(!tryCreateKey(player, item, newLockId))
            return false;
        bind(block, newLockId);

        getLogger().info(String.format(player.getDisplayName() +" locked the block x:%d y:%d z:%d, with KeyID:%d", block.getX(), block.getY(), block.getZ(), newLockId));
        return true;
    }

    /**
     * Try to bind a door to an existing key.
     * @param player The player trying to bind the door or null
     * @param block The block to bind
     * @param item The already bound key
     */
    public void bindDoor(Player player, Block block, ItemStack item) {
        int lockId = getLockId(item);
        bind(block, lockId);
        if (player != null)
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(config.getString("responses.add_lock_success")));
    }

    /**
     * Create a lock on a block with a given id.
     * @param block The block to lock
     * @param lockId The lock id
     */
    public void bind(Block block, int lockId) {
        PersistentDataContainer blockData = new CustomBlockData(block, this);
        blockData.set(lockIdNSK, PersistentDataType.INTEGER, lockId);
        block.getWorld().playSound(block.getLocation(), Sound.BLOCK_CHAIN_BREAK, 1, 1);
    }

    /**
     * Bind a key to a lock.
     * @param itemMeta The meta of the key to bind
     * @param lockId The lock to bind to.
     */
    public void bind(ItemMeta itemMeta, int lockId) {
        PersistentDataContainer pdc = itemMeta.getPersistentDataContainer();
        pdc.set(lockIdNSK, PersistentDataType.INTEGER, lockId);
    }

    public boolean tryCreateKey(Player player, ItemStack key, int lockId) {
        // Exit if inventory is full
        if (player.getInventory().firstEmpty() == -1 && key.getAmount() > 1) {
            String playerResponse = config.getString("responses.create_key_failure.inventory_full");
            if (playerResponse != null)
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
        bind(keyMeta, lockId);

        // Create a bound key and remove the original one
        if(key.getAmount() == 1) {
            key.setItemMeta(keyMeta);
        } else {
            key.setAmount(key.getAmount() - 1);
            ItemStack boundKey = new ItemStack(KEY_MATERIAL);
            boundKey.setItemMeta(keyMeta);
            player.getInventory().addItem(boundKey);
        }

        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(config.getString("responses.add_lock_success")));
        String infoResponse = config.getString("responses.create_key_info.rename_key");
        if(infoResponse == null)
            getLogger().warning("No response for add_lock_info.rename_key");
        else
            player.sendMessage(infoResponse);

        return true;
    }

    public boolean tryUseKey(Player player, Block block, ItemStack item, PlayerInteractEvent event) {
        // Check if the key is matches the lock
        if(!isMatching(item,block) && (player == null || !canBypassLock(player))) {
            if (player != null)
            {
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(config.getString("responses.unlock_failure.wrong_key")));
                player.playSound(block.getLocation(), Sound.BLOCK_CHAIN_STEP, 1, 1);
            }
            return false;
        }

        block.getWorld().playSound(block.getLocation(), Sound.BLOCK_IRON_TRAPDOOR_OPEN, 1, 1.0f);

        // If the block is Openable (door, gate, trapdoor) open or close it
        if (block.getBlockData() instanceof Openable openable) {
            openable.setOpen(!openable.isOpen());
            block.setBlockData(openable);
        }
        else if (block.getState() instanceof InventoryHolder) {
            event.setCancelled(false);
        }

        if (player != null){
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(config.getString("responses.unlock_success")));
            player.playSound(block.getLocation(), Sound.BLOCK_CHAIN_BREAK, 1, 1);
        }
        return true;
    }

    private int getNextLockId() {
        return new Random().nextInt(Integer.MAX_VALUE);
    }

    private int getLockId(ItemStack item) {
        Integer keyID = Objects.requireNonNull(item.getItemMeta()).getPersistentDataContainer().get(lockIdNSK, PersistentDataType.INTEGER);
        return keyID==null?-1:keyID;
    }

    private int getLockId(Block block) {
        PersistentDataContainer blockData = new CustomBlockData(block, this);
        Integer lockID = blockData.get(lockIdNSK, PersistentDataType.INTEGER);
        return lockID==null?-1:lockID;
    }

    private boolean isMatching(ItemStack item, Block block) {
        int blockLockId = getLockId(block);
        int itemLockId = getLockId(item);
        return itemLockId != -1 && blockLockId == itemLockId;
    }

    private boolean canBypassLock(Player player) {
        return player.hasPermission(LOCK_BYPASS_PERM) && (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR);
    }
}
