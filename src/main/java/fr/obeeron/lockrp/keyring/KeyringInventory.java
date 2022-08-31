package fr.obeeron.lockrp.keyring;

import fr.obeeron.lockrp.LRPCore;
import fr.obeeron.lockrp.LockRP;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;
import java.util.Objects;

public class KeyringInventory implements InventoryHolder {

    private final Inventory inventory;
    private final ItemStack keyringItem;
    private boolean emptyWhenOpened = true;

    public KeyringInventory(ItemStack keyRingItem) {
        inventory = Bukkit.createInventory(this, 9, LockRP.config.getString("keyring.inventory_screen_name", "Key ring"));
        this.keyringItem = keyRingItem;
        ItemStack[] keyRingContent = LRPCore.getKeysFromKeyring(keyRingItem);

        for (int i = 0; i < keyRingContent.length; i++) {
            inventory.setItem(i, keyRingContent[i]);

            if (keyRingContent[i] != null) {
                emptyWhenOpened = false;
            }
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void onClose(Inventory playerInventory) {
        ItemStack[] keyringContent = inventory.getContents();
        boolean empty = Arrays.stream(keyringContent).allMatch(Objects::isNull);

        if (empty)
            emptyKeyring(keyringItem);
        else
            fillKeyring(keyringItem, playerInventory, keyringContent);
    }

    private void fillKeyring(ItemStack keyringItem, Inventory playerInventory, ItemStack[] keyringContent) {
        ItemMeta keyringMeta = Objects.requireNonNull(keyringItem.getItemMeta());
        PersistentDataContainer keyringPDC = keyringMeta.getPersistentDataContainer();

        // if the keyring was empty before, fill it and check for stacked keyring
        if (this.emptyWhenOpened) {
            // If the keyring is not stacked, update the meta of the item in hand
            if (this.keyringItem.getAmount() == 1) {
                keyringPDC.set(LockRP.serializedKeysNSK, new KeyArrayDataType(), keyringContent);
                keyringPDC.set(LockRP.customItemIdNSK, PersistentDataType.STRING, LockRP.UNIVID_KEYRING);
                keyringPDC.set(LockRP.keyringIdNSK, PersistentDataType.INTEGER, LRPCore.getNextKeyringId());
            }
            // Otherwise, decrement the amount of the stacked keyring and add the filled keyring
            else {

                this.keyringItem.setAmount(this.keyringItem.getAmount() - 1);

                ItemStack filledKeyring = LRPCore.createEmptyKeyring();
                ItemMeta filledKeyringMeta = Objects.requireNonNull(filledKeyring.getItemMeta());
                PersistentDataContainer filledKeyringPDC = filledKeyringMeta.getPersistentDataContainer();

                filledKeyringPDC.set(LockRP.serializedKeysNSK, new KeyArrayDataType(), keyringContent);
                filledKeyringPDC.set(LockRP.customItemIdNSK, PersistentDataType.STRING, LockRP.UNIVID_KEYRING);
                filledKeyringPDC.set(LockRP.keyringIdNSK, PersistentDataType.INTEGER, LRPCore.getNextKeyringId());

                filledKeyring.setItemMeta(filledKeyringMeta);

                if (!playerInventory.addItem(filledKeyring).isEmpty()){
                    // Drop the filled keyring on the ground from the player
                    World world = Objects.requireNonNull(playerInventory.getLocation()).getWorld();
                    assert world != null;
                    world.dropItem(playerInventory.getLocation(), filledKeyring);
                }
            }
        }
        // Otherwise, only update its content
        else{
            keyringPDC.set(LockRP.serializedKeysNSK, new KeyArrayDataType(), keyringContent);
        }

        keyringItem.setItemMeta(keyringMeta);
    }

    private void emptyKeyring(ItemStack keyRingItem) {
        ItemMeta keyringMeta = keyRingItem.getItemMeta();
        assert keyringMeta != null;
        PersistentDataContainer keyringPDC = keyringMeta.getPersistentDataContainer();

        // If the keyring is already empty do nothing
        if (!keyringPDC.has(LockRP.keyringIdNSK, PersistentDataType.INTEGER))
            return;

        keyringPDC.remove(LockRP.keyringIdNSK);
        keyringPDC.set(LockRP.customItemIdNSK, PersistentDataType.STRING, LockRP.UNIVID_KEYRING_EMPTY);
        keyringMeta.getPersistentDataContainer().set(LockRP.serializedKeysNSK, new KeyArrayDataType(), new ItemStack[9]);
        keyRingItem.setItemMeta(keyringMeta);
    }

    public ItemStack getKeyringItem() {
        return keyringItem;
    }
}
