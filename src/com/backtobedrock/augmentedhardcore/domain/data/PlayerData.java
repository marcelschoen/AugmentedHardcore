package com.backtobedrock.augmentedhardcore.domain.data;

import com.backtobedrock.augmentedhardcore.AugmentedHardcore;
import com.backtobedrock.augmentedhardcore.domain.Ban;
import com.backtobedrock.augmentedhardcore.domain.Killer;
import com.backtobedrock.augmentedhardcore.domain.enums.DamageCause;
import com.backtobedrock.augmentedhardcore.domain.enums.Permission;
import com.backtobedrock.augmentedhardcore.runnables.CombatTag.AbstractCombatTag;
import com.backtobedrock.augmentedhardcore.runnables.Playtime.AbstractPlaytime;
import com.backtobedrock.augmentedhardcore.runnables.Playtime.LifePartPlaytime;
import com.backtobedrock.augmentedhardcore.runnables.Playtime.MaxHealthPlaytime;
import com.backtobedrock.augmentedhardcore.utils.BanUtils;
import com.backtobedrock.augmentedhardcore.utils.EventUtils;
import com.backtobedrock.augmentedhardcore.utils.MessageUtils;
import com.backtobedrock.augmentedhardcore.utils.PlayerUtils;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class PlayerData {
    //misc
    private final AugmentedHardcore plugin = JavaPlugin.getPlugin(AugmentedHardcore.class);
    //serializable
    private final Map<Integer, Ban> bans;
    private final String lastKnownName;
    private final List<AbstractPlaytime> playtime = new ArrayList<>();
    private int timeTillNextLifePart;
    private int timeTillNextMaxHealth;
    private long reviveCooldown;
    private int lifeParts;
    private int lives;
    private String ip;
    //helpers
    private List<AbstractCombatTag> combatTag = new ArrayList<>();
    private boolean kicked = false;
    private boolean combatLogged = false;
    private Killer reviving = null;

    public PlayerData(OfflinePlayer player) {
        //serializable
        this.bans = new LinkedHashMap<>();
        this.lastKnownName = player.getName();
        this.timeTillNextLifePart = this.plugin.getConfigurations().getLivesAndLifePartsConfiguration().getPlaytimePerLifePart();
        this.timeTillNextMaxHealth = this.plugin.getConfigurations().getMaxHealthConfiguration().getPlaytimePerHalfHeart();
        this.lives = this.plugin.getConfigurations().getLivesAndLifePartsConfiguration().getLivesAtStart();
        this.lifeParts = this.plugin.getConfigurations().getLivesAndLifePartsConfiguration().getLifePartsAtStart();
        if (player.isOnline() && player.getPlayer() != null) {
            if (this.plugin.getConfigurations().getReviveConfiguration().isReviveOnFirstJoin()) {
                this.reviveCooldown = new Date().getTime();
            } else {
                GregorianCalendar cal = new GregorianCalendar();
                cal.add(Calendar.MINUTE, this.plugin.getConfigurations().getReviveConfiguration().getTimeBetweenRevives());
                this.reviveCooldown = cal.getTime().getTime();
            }
            if (player.getPlayer().getAddress() != null) {
                InetSocketAddress address = player.getPlayer().getAddress();
                this.ip = address.getHostName() == null ? address.getHostString() : address.getHostName();
            }
        }
    }

    public PlayerData(OfflinePlayer player, Map<Integer, Ban> bans, int lives, int lifeParts, int timeTillNextLifePart, int timeTillNextMaxHealth, long reviveCooldown) {
        //serializable
        this.bans = bans;
        this.lastKnownName = player.getName();
        this.setTimeTillNextLifePart(player, timeTillNextLifePart);
        this.setTimeTillNextMaxHealth(player, timeTillNextMaxHealth);
        this.setLives(player, lives);
        this.setLifeParts(player, lifeParts);
        this.setReviveCooldown(player, reviveCooldown);
        if (player.getPlayer() != null && player.getPlayer().getAddress() != null) {
            InetSocketAddress address = player.getPlayer().getAddress();
            this.ip = address.getHostName() == null ? address.getHostString() : address.getHostName();
        }
    }

    public static PlayerData deserialize(ConfigurationSection section, OfflinePlayer player) {
        AugmentedHardcore plugin = JavaPlugin.getPlugin(AugmentedHardcore.class);

        Map<Integer, Ban> cBans = new LinkedHashMap<>();
        int cLives = section.getInt("Lives", plugin.getConfigurations().getLivesAndLifePartsConfiguration().getLivesAtStart());
        int cLifeParts = section.getInt("LifeParts", plugin.getConfigurations().getLivesAndLifePartsConfiguration().getLifePartsAtStart());
        int cTimeTillNextLifePart = section.getInt("TimeTillNextLifePart", plugin.getConfigurations().getLivesAndLifePartsConfiguration().getPlaytimePerLifePart());
        int cTimeTillNextMaxHealth = section.getInt("TimeTillNextMaxHealth", plugin.getConfigurations().getMaxHealthConfiguration().getPlaytimePerHalfHeart());
        long cReviveCooldown = section.getLong("ReviveCooldown", -1L);

        //get all bans
        ConfigurationSection bansSection = section.getConfigurationSection("Bans");
        if (bansSection != null) {
            int i = 0;
            for (String e : bansSection.getKeys(false)) {
                ConfigurationSection banSection = bansSection.getConfigurationSection(e);
                if (banSection != null) {
                    Ban ban = Ban.Deserialize(banSection, player.getUniqueId());
                    if (ban != null) {
                        cBans.put(i, ban);
                        i++;
                    }
                }
            }
        }

        //get revive cooldown if not in data
        GregorianCalendar cal = new GregorianCalendar();
        cal.add(Calendar.MINUTE, plugin.getConfigurations().getReviveConfiguration().getTimeBetweenRevives());
        long maxDate = cal.getTime().getTime();
        if (cReviveCooldown == -1L) {
            if (plugin.getConfigurations().getReviveConfiguration().isReviveOnFirstJoin()) {
                cReviveCooldown = new Date().getTime();
            } else {
                cReviveCooldown = maxDate;
            }
        } else {
            cReviveCooldown = Math.min(maxDate, cReviveCooldown);
        }

        return new PlayerData(player, cBans, cLives, cLifeParts, cTimeTillNextLifePart, cTimeTillNextMaxHealth, cReviveCooldown);
    }

    public void setReviveCooldown(OfflinePlayer player, long reviveCooldown) {
        GregorianCalendar cal = new GregorianCalendar();
        cal.add(Calendar.MINUTE, this.plugin.getConfigurations().getReviveConfiguration().getTimeBetweenRevives());
        this.reviveCooldown = Math.min(reviveCooldown, cal.getTime().getTime());
        this.plugin.getPlayerRepository().updatePlayerData(player, this);
    }

    public String getIp() {
        return ip;
    }

    public int getLives() {
        return lives;
    }

    public void setLives(OfflinePlayer player, int lives) {
        this.lives = Math.max(Math.min(lives, this.plugin.getConfigurations().getLivesAndLifePartsConfiguration().getMaxLives()), 0);
        this.plugin.getPlayerRepository().updatePlayerData(player, this);
    }

    public int getLifeParts() {
        return lifeParts;
    }

    public void setLifeParts(OfflinePlayer player, int lifeParts) {
        int lifePartsPerLife = this.plugin.getConfigurations().getLivesAndLifePartsConfiguration().getLifePartsPerLife();
        int lives = Math.min(lifeParts / lifePartsPerLife, this.plugin.getConfigurations().getLivesAndLifePartsConfiguration().getMaxLives() - this.getLives());
        if (lives > 0)
            this.increaseLives(player, lives);
        this.lifeParts = Math.min(this.plugin.getConfigurations().getLivesAndLifePartsConfiguration().getMaxLifeParts(), Math.max(0, lifeParts - (lives * lifePartsPerLife)));
        this.plugin.getPlayerRepository().updatePlayerData(player, this);
    }

    public Map<Integer, Ban> getBans() {
        return bans;
    }

    public int getBanCount() {
        return this.bans.size();
    }

    private void decreaseLives(OfflinePlayer player, int amount) {
        this.setLives(player, this.getLives() - amount);
    }

    public void increaseLives(OfflinePlayer player, int amount) {
        this.setLives(player, this.getLives() + amount);
    }

    private void decreaseLifeParts(OfflinePlayer player, int amount) {
        this.setLifeParts(player, this.getLifeParts() - amount);
    }

    public void increaseLifeParts(OfflinePlayer player, int amount) {
        this.setLifeParts(player, this.getLifeParts() + amount);
    }

    public void onDeath(Player player, PlayerDeathEvent event) {
        this.unCombatTag();

        this.loseLives(player, this.plugin.getConfigurations().getLivesAndLifePartsConfiguration().getLivesLostPerDeath());

        this.loseMaxHealth(player, this.plugin.getConfigurations().getMaxHealthConfiguration().getMaxHealthDecreasePerDeath());

        if (this.plugin.getConfigurations().getLivesAndLifePartsConfiguration().isUseLives()) {
            if (lives == 0) {
                this.loseLifeParts(player, this.plugin.getConfigurations().getLivesAndLifePartsConfiguration().getLifePartsLostPerDeathBan());
                this.ban(player, event);
            } else {
                this.loseLifeParts(player, this.plugin.getConfigurations().getLivesAndLifePartsConfiguration().getLifePartsLostPerDeath());
            }
        } else {
            this.loseLifeParts(player, this.plugin.getConfigurations().getLivesAndLifePartsConfiguration().getLifePartsLostPerDeath());
            this.ban(player, event);
        }
    }

    public Killer getCombatTagger() {
        if (!this.combatTag.isEmpty()) {
            return this.combatTag.get(0).getTagger();
        }
        return null;
    }

    private void loseMaxHealth(Player player, double amount) {
        if (!this.plugin.getConfigurations().getMaxHealthConfiguration().isUseMaxHealth()) {
            return;
        }

        if (player.hasPermission(Permission.BYPASS_LOSEMAXHEALTH.getPermissionString()))
            return;

        if (this.plugin.getConfigurations().getMaxHealthConfiguration().getDisableLosingMaxHealthInWorlds().contains(player.getWorld().getName().toLowerCase()))
            return;

        this.decreaseMaxHealth(player, amount);
    }

    private void decreaseMaxHealth(Player player, double amount) {
        AttributeInstance attribute = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (attribute != null) {
            PlayerUtils.setMaxHealth(player, attribute.getBaseValue() - amount);
        }
    }

    private void increaseMaxHealth(Player player, double amount) {
        AttributeInstance attribute = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (attribute != null) {
            player.setHealth(player.getHealth() + PlayerUtils.setMaxHealth(player, attribute.getBaseValue() + amount));
        }
    }

    private void loseLives(Player player, int amount) {
        if (!this.plugin.getConfigurations().getLivesAndLifePartsConfiguration().isUseLives()) {
            return;
        }

        //check if permission to bypass
        if (player.hasPermission(Permission.BYPASS_LOSELIVES.getPermissionString()))
            return;

        //check if in disabled world
        if (this.plugin.getConfigurations().getLivesAndLifePartsConfiguration().getDisableLosingLivesInWorlds().contains(player.getWorld().getName().toLowerCase()))
            return;

        //lose lives
        this.decreaseLives(player, amount);
    }

    private void loseLifeParts(Player player, int amount) {
        if (!this.plugin.getConfigurations().getLivesAndLifePartsConfiguration().isUseLifeParts()) {
            return;
        }

        //check if permission to bypass
        if (player.hasPermission(Permission.BYPASS_LOSELIFEPARTS.getPermissionString()))
            return;

        //check if in disabled world
        if (this.plugin.getConfigurations().getLivesAndLifePartsConfiguration().getDisableLosingLifePartsInWorlds().contains(player.getWorld().getName().toLowerCase()))
            return;

        //lose life parts
        this.decreaseLifeParts(player, amount);
    }

    private void ban(Player player, PlayerDeathEvent event) {
        if (!this.plugin.getConfigurations().getBanTimesConfiguration().isUseDeathBan()) {
            return;
        }

        //Check if permission to bypass
        if (player.hasPermission(Permission.BYPASS_BAN.getPermissionString()))
            return;

        //check if in disabled world
        if (this.plugin.getConfigurations().getBanTimesConfiguration().getDisableBanInWorlds().contains(player.getWorld().getName().toLowerCase()))
            return;

        EntityDamageEvent damageEvent = event.getEntity().getLastDamageCause();

        if (damageEvent == null) {
            return;
        }

        DamageCause cause = EventUtils.getDamageCauseFromDamageEvent(this, damageEvent);
        if (cause == null) {
            return;
        }

        Killer killer = this.isReviving() ? this.getReviving() : EventUtils.getDamageEventKiller(damageEvent), tagger = this.getCombatTagger();
        if (killer != null && killer.equals(tagger)) {
            tagger = null;
        }

        Ban ban = BanUtils.getBan(player, this, cause, killer, tagger, event.getDeathMessage(), EventUtils.getDamageCauseTypeFromEntityDamageEvent(damageEvent));

        //check if not killed due to self harm if disabled
        if (!this.plugin.getConfigurations().getBanTimesConfiguration().isSelfHarmBan() && ban.getKiller() != null && ban.getKiller().getName().equals(player.getName()))
            return;

        //ban player
        if (ban.getBanTime() > 0) {
            ban.deathBan(this, player);
            this.plugin.getServerRepository().getServerData().thenAccept(serverData -> {
                //add ban to player and server data
                this.addBan(player, ban);
                serverData.addBan(player, ban);
            });
        }
    }

    public void addBan(OfflinePlayer player, Ban ban) {
        this.bans.put(this.bans.size(), ban);
        this.plugin.getPlayerRepository().updatePlayerData(player, this);
    }

    public void onRespawn(Player player) {
        if (this.plugin.getConfigurations().getLivesAndLifePartsConfiguration().isUseLives() && this.lives == 0) {
            this.setLives(player, this.plugin.getConfigurations().getLivesAndLifePartsConfiguration().getLivesAfterBan());

            if (this.plugin.getConfigurations().getLivesAndLifePartsConfiguration().getLifePartsAfterBan() != -1) {
                this.setLifeParts(player, this.plugin.getConfigurations().getLivesAndLifePartsConfiguration().getLifePartsAfterBan());
            }
        }

        if (this.plugin.getConfigurations().getMaxHealthConfiguration().isUseMaxHealth() && this.plugin.getConfigurations().getMaxHealthConfiguration().getMaxHealthAfterBan() != -1) {
            PlayerUtils.setMaxHealth(player, this.plugin.getConfigurations().getMaxHealthConfiguration().getMaxHealthAfterBan());
            AttributeInstance attribute = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            if (attribute != null) {
                player.setHealth(attribute.getBaseValue());
            }
        }
    }

    public void onEntityKill(Player player, EntityType type) {
        //gain life parts on kill
        this.onLifePartsEntityKill(player, type);

        //gain max health on kill
        this.onMaxHealthEntityKill(player, type);
    }

    private void onLifePartsEntityKill(Player player, EntityType type) {
        if (!this.plugin.getConfigurations().getLivesAndLifePartsConfiguration().isUseLifeParts()) {
            return;
        }

        if (!this.plugin.getConfigurations().getLivesAndLifePartsConfiguration().isLifePartsOnKill()) {
            return;
        }

        //check if permission to gain life parts for kill
        if (player.hasPermission(Permission.BYPASS_GAINLIFEPARTS_KILL.getPermissionString())) {
            return;
        }

        //gain life parts
        this.gainLifeParts(player, this.plugin.getConfigurations().getLivesAndLifePartsConfiguration().getLifePartsPerKill().getOrDefault(type, 0));
    }

    private void onMaxHealthEntityKill(Player player, EntityType type) {
        if (!this.plugin.getConfigurations().getMaxHealthConfiguration().isUseMaxHealth()) {
            return;
        }

        if (!this.plugin.getConfigurations().getMaxHealthConfiguration().isMaxHealthIncreaseOnKill()) {
            return;
        }

        //check if permission to gain max health for kill
        if (player.hasPermission(Permission.BYPASS_GAINMAXHEALTH_KILL.getPermissionString())) {
            return;
        }

        //increase max health
        this.gainMaxHealth(player, this.plugin.getConfigurations().getMaxHealthConfiguration().getMaxHealthIncreasePerKill().getOrDefault(type, 0D));
    }

    private void gainLifeParts(Player player, int amount) {
        //check if in disabled world
        if (this.plugin.getConfigurations().getLivesAndLifePartsConfiguration().getDisableGainingLifePartsInWorlds().contains(player.getWorld().getName().toLowerCase()))
            return;

        this.increaseLifeParts(player, amount);
    }

    private void gainMaxHealth(Player player, double amount) {
        //check if in disabled world
        if (this.plugin.getConfigurations().getMaxHealthConfiguration().getDisableGainingMaxHealthInWorlds().contains(player.getWorld().getName().toLowerCase()))
            return;

        this.increaseMaxHealth(player, amount);
    }

    public boolean isBanned(OfflinePlayer player) {
        BanList.Type type = this.getIp() == null ? BanList.Type.NAME : this.plugin.getConfigurations().getBanTimesConfiguration().getBanType();
        BanList banList = Bukkit.getBanList(type);
        return banList.isBanned(BanUtils.getBanParameter(this, player, type));
    }

    public void onCombatTag(Player player, Killer tagger) {
        if (!this.plugin.getConfigurations().getCombatTagConfiguration().isUseCombatTag()) {
            return;
        }

        //check if combat tag self enabled and if self harming
        if (!this.plugin.getConfigurations().getCombatTagConfiguration().isCombatTagSelf() && tagger.getName().equals(player.getName()))
            return;

        //check if in disabled worlds
        if (this.plugin.getConfigurations().getCombatTagConfiguration().getDisableCombatTagInWorlds().contains(player.getLocation().getWorld().getName().toLowerCase()))
            return;

        if (!this.combatTag.isEmpty()) {
            new ArrayList<>(this.combatTag).forEach(e -> e.restart(tagger));
        } else {
            this.combatTag = this.plugin.getMessages().getCombatTagNotificationsConfiguration(player, this, tagger);
            new ArrayList<>(this.combatTag).forEach(AbstractCombatTag::start);
        }
    }

    public void removeFromCombatTag(AbstractCombatTag tag) {
        this.combatTag.remove(tag);
    }

    public void unCombatTag() {
        if (!this.combatTag.isEmpty()) {
            new ArrayList<>(this.combatTag).forEach(AbstractCombatTag::stop);
        }
    }

    public void onJoin(Player player) {
        Bukkit.getScheduler().runTask(this.plugin, () -> {
            if (!this.plugin.getConfigurations().getMaxHealthConfiguration().isUseMaxHealth()) {
                AttributeInstance attribute = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
                if (attribute != null && attribute.getBaseValue() != 20D) {
                    player.setHealth(player.getHealth() + PlayerUtils.setMaxHealth(player, 20D));
                }
            } else {
                //fix visual bug from Minecraft when max health is higher than 20
                player.setHealth(player.getHealth());
            }
        });

        if (!this.playtime.isEmpty()) {
            this.playtime.forEach(AbstractPlaytime::stop);
            this.playtime.clear();
        }

        if (this.plugin.getConfigurations().getLivesAndLifePartsConfiguration().isUseLifeParts() && this.plugin.getConfigurations().getLivesAndLifePartsConfiguration().isGetLifePartsByPlaytime()) {
            this.playtime.add(new LifePartPlaytime(player, this));
        }
        if (this.plugin.getConfigurations().getMaxHealthConfiguration().isUseMaxHealth() && this.plugin.getConfigurations().getMaxHealthConfiguration().isGetMaxHealthByPlaytime()) {
            this.playtime.add(new MaxHealthPlaytime(player, this));
        }

        this.playtime.forEach(AbstractPlaytime::start);
    }

    public void decreaseTimeTillNextLifePart(Player player, int amount) {
        if (!this.plugin.getConfigurations().getLivesAndLifePartsConfiguration().isUseLifeParts()) {
            return;
        }

        if (!this.plugin.getConfigurations().getLivesAndLifePartsConfiguration().isGetLifePartsByPlaytime()) {
            return;
        }

        if (player.hasPermission(Permission.BYPASS_GAINLIFEPARTS_PLAYTIME.getPermissionString()))
            return;

        if (this.plugin.getConfigurations().getLivesAndLifePartsConfiguration().getDisableGainingLifePartsInWorlds().contains(player.getLocation().getWorld().getName().toLowerCase()))
            return;

        int decreased = this.getTimeTillNextLifePart() - amount;
        if (decreased <= 0) {
            this.gainLifeParts(player, 1);
            this.setTimeTillNextLifePart(player, this.plugin.getConfigurations().getLivesAndLifePartsConfiguration().getPlaytimePerLifePart() - Math.abs(decreased));
        } else {
            this.setTimeTillNextLifePart(player, decreased);
        }
    }

    public void decreaseTimeTillNextMaxHealth(Player player, int amount) {
        if (!this.plugin.getConfigurations().getMaxHealthConfiguration().isUseMaxHealth()) {
            return;
        }

        if (!this.plugin.getConfigurations().getMaxHealthConfiguration().isGetMaxHealthByPlaytime()) {
            return;
        }

        if (player.hasPermission(Permission.BYPASS_GAINMAXHEALTH_PLAYTIME.getPermissionString()))
            return;

        if (this.plugin.getConfigurations().getMaxHealthConfiguration().getDisableGainingMaxHealthInWorlds().contains(player.getLocation().getWorld().getName().toLowerCase()))
            return;

        int decreased = this.getTimeTillNextMaxHealth() - amount;
        if (decreased <= 0) {
            this.gainMaxHealth(player, 1D);
            this.setTimeTillNextMaxHealth(player, this.plugin.getConfigurations().getMaxHealthConfiguration().getPlaytimePerHalfHeart() - Math.abs(decreased));
        } else {
            this.setTimeTillNextMaxHealth(player, decreased);
        }
    }

    public int getTimeTillNextMaxHealth() {
        return timeTillNextMaxHealth;
    }

    public void onLeave(Player player) {
        if (!this.playtime.isEmpty()) {
            this.playtime.forEach(AbstractPlaytime::stop);
            this.playtime.clear();
        }

        if (this.plugin.getConfigurations().getCombatTagConfiguration().isUseCombatTag()) {
            if (this.isCombatTagged() && !this.plugin.getConfigurations().getCombatTagConfiguration().isCombatTagPlayerKickDeath() && this.kicked) {
                this.unCombatTag();
                this.kicked = false;
            } else if (this.isCombatTagged()) {
                this.combatLogged = true;
                player.setHealth(0.0D);
            }
        }

        this.plugin.getPlayerRepository().updatePlayerData(player, this);
        this.plugin.getPlayerRepository().removeFromPlayerCache(player);
    }

    public void onReviving(Player reviver, OfflinePlayer revivingPlayer, PlayerData revivingPlayerData) {
        if (!this.checkRevivePermissions(reviver, revivingPlayer)) {
            return;
        }

        switch (revivingPlayerData.getLives()) {
            case 0:
                //check if able to revive if deathbanned
                if (!reviver.hasPermission(Permission.GAIN_REVIVE_DEATH.getPermissionString())) {
                    reviver.sendMessage("§cYou don't have the right permission to give lives to a dead banned player.");
                    return;
                }

                this.plugin.getServerRepository().getServerData().thenAccept(serverData -> {
                    if (!this.unDeathBan(reviver, serverData, revivingPlayer, true)) return;

                    revivingPlayerData.onRevive(revivingPlayer, reviver);
                });
                break;
            default:
                //check if able to revive if alive
                if (!reviver.hasPermission(Permission.GAIN_REVIVE_ALIVE.getPermissionString())) {
                    reviver.sendMessage("§cYou don't have the right permission to give lives to an alive player.");
                    return;
                }

                revivingPlayerData.onRevive(revivingPlayer, reviver);
                break;
        }

        //decrease lives
        int amount = this.plugin.getConfigurations().getReviveConfiguration().getLivesLostOnReviving();
        this.decreaseLives(reviver, amount);

        //set revive cooldown
        GregorianCalendar cal = new GregorianCalendar();
        cal.add(Calendar.MINUTE, plugin.getConfigurations().getReviveConfiguration().getTimeBetweenRevives());
        this.setReviveCooldown(reviver, cal.getTime().getTime());

        //determine outcome
        if (this.getLives() == 0) {
            //deathban
            this.reviving = new Killer(revivingPlayer.getName(), revivingPlayer.getPlayer() == null ? null : revivingPlayer.getPlayer().getDisplayName(), EntityType.PLAYER);
            reviver.setHealth(0.0D);
        } else {
            reviver.sendMessage(String.format("§aSuccessfully given §e%s§a to §e%s§a, you have §e%s §aleft.", amount + (amount > 1 ? " lives" : " life"), revivingPlayer.getName(), this.getLives() + (this.getLives() > 1 ? " lives" : " life")));
        }
    }

    public boolean checkRevivePermissions(Player reviver, OfflinePlayer revivingPlayer) {
        //check if reviving is enabled
        if (!this.plugin.getConfigurations().getReviveConfiguration().isUseRevive()) {
            reviver.sendMessage("§cReviving is not enabled on the server.");
            return false;
        }

        //check if permission
        if (!reviver.hasPermission(Permission.REVIVE.getPermissionString()))
            return false;

        //check if not same player
        if (reviver.getUniqueId().equals(revivingPlayer.getUniqueId())) {
            reviver.sendMessage("§cYou cannot revive yourself, that would break the space-time continuum!");
            return false;
        }

        //check if in disabled world
        if (this.plugin.getConfigurations().getReviveConfiguration().getDisableReviveInWorlds().contains(reviver.getWorld().getName().toLowerCase())) {
            reviver.sendMessage(String.format("§cYou cannot revive %s while in this world (%s).", revivingPlayer.getName(), reviver.getWorld().getName()));
            return false;
        }

        //check if enough lives left to revive
        int amount = this.plugin.getConfigurations().getReviveConfiguration().getLivesLostOnReviving();
        if (this.getLives() < amount) {
            reviver.sendMessage(String.format("§cYou'll need %s in order to revive %s, you currently have %s.", amount + (amount > 1 ? " lives" : " life"), revivingPlayer.getName(), this.getLives() + (this.getLives() > 1 ? " lives" : " life")));
            return false;
        }

        //check if revive on cooldown
        if (this.isReviveOnCooldown()) {
            reviver.sendMessage(String.format("§cYou cannot revive %s for another %s.", revivingPlayer.getName(), MessageUtils.getTimeFromTicks(this.getReviveCooldownLeftInTicks(), false, true)));
            return false;
        }

        return true;
    }

    private boolean isReviveOnCooldown() {
        return this.getReviveCooldownLeftInTicks() > 0;
    }

    public int getReviveCooldownLeftInTicks() {
        return Math.max(MessageUtils.timeUnitToTicks((int) (this.reviveCooldown - new Date().getTime()), TimeUnit.MILLISECONDS), 0);
    }

    public void onRevive(OfflinePlayer reviving, Player reviver) {
        if (!this.plugin.getConfigurations().getReviveConfiguration().isUseRevive()) {
            return;
        }

        int amount = this.plugin.getConfigurations().getReviveConfiguration().getLivesGainedOnRevive();
        this.increaseLives(reviving, amount);
        if (reviving.isOnline() && reviving.getPlayer() != null)
            reviving.getPlayer().sendMessage(String.format("§a%s has successfully given you §e%s§a, you now have §e%s§a.", reviver.getName(), amount + (amount > 1 ? " lives" : " life"), this.getLives() + (this.getLives() > 1 ? " lives" : " life")));
    }

    public int getTimeTillNextLifePart() {
        return timeTillNextLifePart;
    }

    public void setTimeTillNextLifePart(OfflinePlayer player, int timeTillNextLifePart) {
        this.timeTillNextLifePart = Math.min(timeTillNextLifePart, this.plugin.getConfigurations().getLivesAndLifePartsConfiguration().getPlaytimePerLifePart());
        this.plugin.getPlayerRepository().updatePlayerData(player, this);
    }

    public void setTimeTillNextMaxHealth(OfflinePlayer player, int timeTillNextMaxHealth) {
        this.timeTillNextMaxHealth = Math.min(timeTillNextMaxHealth, this.plugin.getConfigurations().getMaxHealthConfiguration().getPlaytimePerHalfHeart());
        this.plugin.getPlayerRepository().updatePlayerData(player, this);
    }

    public void onReload(OfflinePlayer player) {
        if (player.isOnline() && player.getPlayer() != null) {
            this.onJoin(player.getPlayer());
        }
    }

    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();

        Map<Object, Object> cBans = new HashMap<>();
        this.bans.forEach((key, value) -> cBans.put(key, value.serialize()));

        map.put("Bans", cBans);
        map.put("TimeTillNextLifePart", this.timeTillNextLifePart);
        map.put("TimeTillNextMaxHealth", this.timeTillNextMaxHealth);
        map.put("LifeParts", this.lifeParts);
        map.put("Lives", this.lives);
        map.put("LastKnownPlayername", this.lastKnownName);
        map.put("ReviveCooldown", this.reviveCooldown);
        map.put("LastKnownIP", this.ip);

        return map;
    }

    public boolean isCombatTagged() {
        return !this.combatTag.isEmpty();
    }

    public void onKick() {
        this.kicked = true;
    }

    public boolean isCombatLogged() {
        return combatLogged;
    }

    public boolean isReviving() {
        return reviving != null;
    }

    public Killer getReviving() {
        return reviving;
    }

    public boolean unDeathBan(CommandSender sender, ServerData serverData, OfflinePlayer player, boolean isRevive) {
        if (!this.isBanned(player)) {
            sender.sendMessage(String.format("§c%s is not banned.", player.getName()));
            return false;
        }

        if (!serverData.isDeathBanned(player)) {
            sender.sendMessage(isRevive ? String.format("§c%s is not death banned and cannot be revived.", player.getName()) : String.format("§c%s is not death banned and cannot be unbanned by LiteDeathBan.", player.getName()));
            return false;
        }

        BanUtils.unDeathBan(player);
        return true;
    }
}