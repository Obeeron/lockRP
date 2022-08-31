package fr.obeeron.lockrp;

import fr.obeeron.lockrp.keyring.KeyringInventoryListener;
import fr.obeeron.lockrp.listeners.BreakListener;
import fr.obeeron.lockrp.listeners.CraftListener;
import fr.obeeron.lockrp.listeners.InteractListener;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.plugin.java.JavaPlugin;


public class LockRP extends JavaPlugin {
    private static LockRP instance;

    // Default configuration confile
    public static FileConfiguration config;
    // Key item material
    public static Material KEY_MATERIAL;
    public static Material KEYRING_MATERIAL;
    // LRP permissions
    public static final String BREAKALL_PERM = "lockrp.break_any";
    public static final String LOCK_BYPASS_PERM = "lockrp.bypass";
    // Namespaced keys
    public static NamespacedKey customItemIdNSK;
    public static NamespacedKey lockIdNSK;
    public static NamespacedKey serializedKeysNSK;
    public static NamespacedKey keyringIdNSK;
    // Items
    public static String UNIVID_KEY = "key";
    public static String UNIVID_KEYRING_EMPTY = "keyring_empty";
    public static String UNIVID_KEYRING = "keyring";

    public static LockRP getPlugin() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        loadConfig();

        LRPCore.setPlugin(this);
        LRPCore.setConfig(config);

        getServer().getPluginManager().registerEvents(new InteractListener(this), this);
        getServer().getPluginManager().registerEvents(new BreakListener(this), this);
        getServer().getPluginManager().registerEvents(new CraftListener(this), this);
        getServer().getPluginManager().registerEvents(new KeyringInventoryListener(this), this);

        addRecipes();

        getLogger().info("LockRP is enabled!");
    }

    @SuppressWarnings( "deprecation" )
    private void loadConfig() {
        saveDefaultConfig();
        config = getConfig();
        getLogger().info("Config loaded!");

        KEY_MATERIAL = Material.getMaterial(config.getString("key_material", "STICK"));
        KEYRING_MATERIAL = Material.getMaterial(config.getString("keyring_material", "STICK"));
        customItemIdNSK = new NamespacedKey("universim", "id");
        lockIdNSK = new NamespacedKey(this, "lockid");
        serializedKeysNSK = new NamespacedKey(this, "keys");
        keyringIdNSK = new NamespacedKey(this, "keyringId");
    }

    public Material getKeyMaterial() {
        return KEY_MATERIAL;
    }

    @Override
    public void onDisable() {
        getLogger().info("LockRP is disabled!");
    }

    private void addRecipes() {
        ShapedRecipe keyRecipe = new ShapedRecipe(NamespacedKey.minecraft(LockRP.UNIVID_KEY), LRPCore.createBlankKey());

        keyRecipe.shape(" N ", "NIN", " I ");
        keyRecipe.setIngredient('N', Material.IRON_NUGGET);
        keyRecipe.setIngredient('I', Material.IRON_INGOT);
        getServer().addRecipe(keyRecipe);

        // ---------------------------------------------------------------

        ShapedRecipe keyRingRecipe = new ShapedRecipe(NamespacedKey.minecraft(LockRP.UNIVID_KEYRING), LRPCore.createEmptyKeyring());

        keyRingRecipe.shape(" * ", "*C*", " * ");
        keyRingRecipe.setIngredient('*', Material.CHAIN);
        keyRingRecipe.setIngredient('C', Material.CHEST);
        getServer().addRecipe(keyRingRecipe);

        // ---------------------------------------------------------------
        ShapelessRecipe keyDuplicateRecipe = new ShapelessRecipe(NamespacedKey.minecraft("key_duplicate"), new ItemStack(KEY_MATERIAL));
        keyDuplicateRecipe.addIngredient(KEY_MATERIAL);
        keyDuplicateRecipe.addIngredient(KEY_MATERIAL);
        getServer().addRecipe(keyDuplicateRecipe);

        // ---------------------------------------------------------------


        getLogger().info("Key recipe added!");
    }
}
