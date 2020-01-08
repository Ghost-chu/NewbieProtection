package com.mcsunnyside.newbieprotection;

import io.puharesource.mc.titlemanager.api.v2.TitleManagerAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

public final class NewbieProtection extends JavaPlugin implements Listener {

    ArrayList<Player> newbies = new ArrayList<>();
    int newbieTicks = 0;
    TitleManagerAPI api;

    @Override
    public void onEnable() {
        // Plugin startup logic
        saveDefaultConfig();
        reloadConfig();
        this.newbieTicks = getConfig().getInt("ticks-is-newbie", 72000);
        newbies.clear();
        Bukkit.getPluginManager().registerEvents(this, this);
        api = (TitleManagerAPI) Bukkit.getServer().getPluginManager().getPlugin("TitleManager");
        new BukkitRunnable() {
            @Override
            public void run() {
                //noinspection unchecked
                ((ArrayList<Player>)(newbies.clone())).forEach((player -> { //clone一份再forEach，线程安全
                    if(!api.hasScoreboard(player)){
                        api.giveScoreboard(player);
                    }
                    api.setScoreboardTitle(player,       ChatColor.GOLD+""+ChatColor.BOLD + "初始保护工作中");
                    api.setScoreboardValue(player, 2, ChatColor.AQUA+"");
                    api.setScoreboardValue(player, 3, ChatColor.GREEN +" ✚ 初始保护正在保护您");
                    api.setScoreboardValue(player, 4, ChatColor.BLACK+"");
                    api.setScoreboardValue(player, 5, ChatColor.YELLOW +" - 死亡掉落在此期间禁用");
                    api.setScoreboardValue(player, 6, ChatColor.YELLOW +" - 收到的所有伤害均减半");
                    api.setScoreboardValue(player, 7, ChatColor.GREEN+"");
                    int remainTicks = newbieTicks - player.getStatistic(Statistic.PLAY_ONE_MINUTE);
                    api.setScoreboardValue(player, 8, ChatColor.AQUA+" ✈ 剩余时间：" + (remainTicks/20)/60 + "分钟");
                    api.setScoreboardValue(player, 9, ChatColor.RED+"");
                    if(remainTicks < 1){
                        api.removeScoreboard(player);
                        newbies.remove(player); //因为clone的所以可以勇敢的remove
                    }
                }));
            }
        }.runTaskTimerAsynchronously(this, 0, 20);
        Bukkit.getOnlinePlayers().forEach(player ->{
            if (player.getStatistic(Statistic.PLAY_ONE_MINUTE) < newbieTicks) {
                newbies.add(player);
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onJoin(PlayerJoinEvent e) {
        if (e.getPlayer().getStatistic(Statistic.PLAY_ONE_MINUTE) < newbieTicks) {
            newbies.add(e.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onQuit(PlayerQuitEvent e) {
        newbies.remove(e.getPlayer());
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDeath(PlayerDeathEvent e) {
        if(newbies.contains(e.getEntity())){
            e.setKeepInventory(true);
            e.setKeepLevel(true);
            e.getDrops().clear();
        }
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent e) {
        if(!(e.getEntity() instanceof Player)){
            return;
        }
        Player player = (Player) e.getEntity();
        if(newbies.contains(player)){
            double damage = e.getFinalDamage();
            if(damage == 0){ //Nothing we can do
                return;
            }
            e.setDamage(damage/2); //reduce damage for newbie
        }
    }
}
