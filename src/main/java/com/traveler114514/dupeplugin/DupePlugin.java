package com.example.dupeplugin;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public final class DupePlugin extends JavaPlugin {

    private final HashMap<UUID, Long> cooldowns = new HashMap<>();
    private FileConfiguration config;
    private long cooldownTime;
    private boolean allowShulkerBoxes;
    private boolean dropOnFull;
    private List<String> enabledWorlds;

    @Override
    public void onEnable() {
        // 确保插件文件夹存在
        if (!getDataFolder().exists()) {
            getDataFolder().mkdir();
        }

        // 加载配置文件
        reloadConfig();

        getLogger().info("§a物品复制插件已启用! 冷却时间: " + cooldownTime + "秒");
    }

    @Override
    public void onDisable() {
        getLogger().info("物品复制插件已禁用");
    }

    @Override
    public void reloadConfig() {
        // 保存默认配置文件（如果不存在）
        saveDefaultConfig();
        
        // 加载配置
        config = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "config.yml"));
        cooldownTime = config.getLong("cooldown", 60) * 1000L; // 转换为毫秒
        allowShulkerBoxes = config.getBoolean("allow-shulker-boxes", true);
        dropOnFull = config.getBoolean("drop-on-full", true);
        enabledWorlds = config.getStringList("enabled-worlds");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!cmd.getName().equalsIgnoreCase("dupe")) {
            return false;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage("§c只有玩家可以使用此命令！");
            return true;
        }

        Player player = (Player) sender;
        UUID playerId = player.getUniqueId();

        // 检查世界是否启用
        if (!enabledWorlds.isEmpty() && !enabledWorlds.contains(player.getWorld().getName())) {
            player.sendMessage("§c此世界已禁用物品复制功能！");
            return true;
        }

        // 检查冷却时间
        if (cooldowns.containsKey(playerId)) {
            long currentTime = System.currentTimeMillis();
            long lastUsedTime = cooldowns.get(playerId);
            long timeLeft = (lastUsedTime + cooldownTime) - currentTime;

            if (timeLeft > 0) {
                long secondsLeft = timeLeft / 1000 + 1;
                player.sendMessage("§c复制技能冷却中，请等待 §e" + secondsLeft + "秒 §c后再试！");
                return true;
            }
        }

        ItemStack itemInHand = player.getInventory().getItemInMainHand();
        
        // 检查玩家手中是否有物品
        if (itemInHand.getType() == Material.AIR) {
            player.sendMessage("§c你手上没有物品！");
            return true;
        }
        
        // 检查潜影盒是否允许
        if (isShulkerBox(itemInHand.getType()) && !allowShulkerBoxes) {
            player.sendMessage("§c此服务器已禁用潜影盒复制！");
            return true;
        }

        ItemStack clonedItem = itemInHand.clone();
        
        // 尝试添加物品到背包
        if (player.getInventory().firstEmpty() != -1) {
            player.getInventory().addItem(clonedItem);
        } else {
            // 背包满时的处理
            if (dropOnFull) {
                player.getWorld().dropItemNaturally(player.getLocation(), clonedItem);
                player.sendMessage("§e背包已满，物品已掉落在地！");
            } else {
                player.sendMessage("§c背包已满，无法复制物品！");
                return true;
            }
        }

        // 更新冷却时间
        cooldowns.put(playerId, System.currentTimeMillis());
        player.sendMessage("§a物品已成功复制！");
        
        return true;
    }
    
    // 检查物品是否为潜影盒
    private boolean isShulkerBox(Material material) {
        return material.name().endsWith("_SHULKER_BOX") || 
               material == Material.SHULKER_BOX;
    }
    
    @Override
    public void saveDefaultConfig() {
        if (!new File(getDataFolder(), "config.yml").exists()) {
            saveResource("config.yml", false);
        }
    }
}
