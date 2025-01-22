package fr.obeeron.lockrp.keyring;

import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.*;

public class KeyArrayDataType implements PersistentDataType<byte[], ItemStack[]> {

    @Override
    public Class<byte[]> getPrimitiveType() {
        return byte[].class;
    }

    @Override
    public Class<ItemStack[]> getComplexType() {
        return ItemStack[].class;
    }

    @Override
    public byte[] toPrimitive(ItemStack[] complex, PersistentDataAdapterContext context) {
        try {
            ByteArrayOutputStream io = new ByteArrayOutputStream();
            BukkitObjectOutputStream out = new BukkitObjectOutputStream(io);
            out.writeObject(complex);
            out.close();
            return io.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public ItemStack[] fromPrimitive(byte[] primitive, PersistentDataAdapterContext context) {
        try {
            InputStream is = new ByteArrayInputStream(primitive);
            BukkitObjectInputStream ois = new BukkitObjectInputStream(is);
            return (ItemStack[]) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }
}
