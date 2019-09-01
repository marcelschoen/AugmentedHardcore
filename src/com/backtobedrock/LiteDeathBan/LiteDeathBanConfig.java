package com.backtobedrock.LiteDeathBan;

import java.util.Map;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;

/**
 *
 * @author PC_Nathan
 */
public class LiteDeathBanConfig {

    private final FileConfiguration config;
    private Logger log = Bukkit.getLogger();

    private int PlayerKillBantime;
    private int MonsterKillBantime;
    private int EnvironmentKillBantime;
    private int BantimeByPlaytimePercent;
    private int BantimeByPlaytimeInterval;
    private int BantimeByPlaytimeMinimumPlayerKillBantime;
    private int BantimeByPlaytimeMinimumMonsterKillBantime;
    private int BantimeByPlaytimeMinimumEnvironmentKillBantime;
    private int CombatDeathTime;
    private boolean AnnounceLivesOnRespawn;
    private boolean BantimeByPlaytime;
    private boolean CombatDeath;

    public LiteDeathBanConfig(FileConfiguration fc) {
        this.config = fc;
        this.initialize();
    }

    private void initialize() {
        this.BantimeByPlaytime = this.checkBoolean(this.config.get("BantimeByPlaytime"), false);
        this.CombatDeath = this.checkBoolean(this.config.get("CombatDeath"), true);
        for (Map.Entry<String, Object> e : this.config.getValues(true).entrySet()) {
            switch (e.getKey()) {
                case "PlayerKillBantime":
                    if (this.BantimeByPlaytime) {
                        this.PlayerKillBantime = this.checkMin(0, e.getKey(), e.getValue(), 7200);
                    } else {
                        if (!e.getValue().equals(0)) {
                            this.PlayerKillBantime = this.checkMin(-1, e.getKey(), e.getValue(), 7200);
                        } else {
                            this.PlayerKillBantime = 0;
                            log.warning(String.format("[LDB] %s has been changed to its default value (%d) due it being 0.", e.getKey(), 7200));
                        }
                    }
                    break;
                case "MonsterKillBantime":
                    if (this.BantimeByPlaytime) {
                        this.MonsterKillBantime = this.checkMin(0, e.getKey(), e.getValue(), 7200);
                    } else {
                        if (!e.getValue().equals(0)) {
                            this.MonsterKillBantime = this.checkMin(-1, e.getKey(), e.getValue(), 7200);
                        } else {
                            this.MonsterKillBantime = 0;
                            log.warning(String.format("[LDB] %s has been changed to its default value (%d) due it being 0.", e.getKey(), 7200));
                        }
                    }
                    break;
                case "EnvironmentKillBantime":
                    if (this.BantimeByPlaytime) {
                        this.EnvironmentKillBantime = this.checkMin(0, e.getKey(), e.getValue(), 7200);
                    } else {
                        if (!e.getValue().equals(0)) {
                            this.EnvironmentKillBantime = this.checkMin(-1, e.getKey(), e.getValue(), 7200);
                        } else {
                            this.EnvironmentKillBantime = 0;
                            log.warning(String.format("[LDB] %s has been changed to its default value (%d) due it being 0.", e.getKey(), 7200));
                        }
                    }
                    break;
                case "BantimeByPlaytimePercent":
                    this.BantimeByPlaytimePercent = this.checkMin(1, e.getKey(), e.getValue(), 10);
                    break;
                case "BantimeByPlaytimeInterval":
                    this.BantimeByPlaytimeInterval = this.checkMin(1, e.getKey(), e.getValue(), 60);
                    break;
                case "BantimeByPlaytimeMinimumPlayerKillBantime":
                    this.BantimeByPlaytimeMinimumPlayerKillBantime = this.checkMin(1, e.getKey(), e.getValue(), 60);
                    break;
                case "BantimeByPlaytimeMinimumMonsterKillBantime":
                    this.BantimeByPlaytimeMinimumMonsterKillBantime = this.checkMin(1, e.getKey(), e.getValue(), 20);
                    break;
                case "BantimeByPlaytimeMinimumEnvironmentKillBantime":
                    this.BantimeByPlaytimeMinimumEnvironmentKillBantime = this.checkMin(1, e.getKey(), e.getValue(), 40);
                    break;
                case "CombatDeathTime":
                    this.CombatDeathTime = this.checkMin(0, e.getKey(), e.getValue(), 60);
                    if (this.CombatDeathTime == 0) {
                        this.CombatDeath = false;
                        log.warning(String.format("CombatDeath has been disabled due to %s being 0.", e.getKey()));
                    }
                    break;
                case "AnnounceLivesOnRespawn":
                    this.AnnounceLivesOnRespawn = this.checkBoolean(e.getValue(), true);
                    break;
                default:
                    break;
            }
        };
    }

    private int checkMin(int MIN, String key, Object value, int defaultValue) {
        int number;
        if (value instanceof Integer) {
            number = (int) value;
        } else {
            number = defaultValue;
            log.warning(String.format("[LDB] %s has been changed to its default value (%d) due it not being configured as a number.", key, defaultValue));
        }
        if (number >= MIN) {
            return number;
        } else {
            log.warning(String.format("[LDB] %s has been changed to its default value (%d) due it being below the minimum value.", key, defaultValue));
            return defaultValue;
        }
    }

    private boolean checkBoolean(Object value, boolean defaultValue) {
        if (value instanceof Boolean) {
            return (boolean) value;
        } else {
            System.out.println();
            return defaultValue;
        }
    }

    public int getPlayerKillBantime() {
        return PlayerKillBantime;
    }

    public int getMonsterKillBantime() {
        return MonsterKillBantime;
    }

    public int getEnvironmentKillBantime() {
        return EnvironmentKillBantime;
    }

    public boolean isBantimeByPlaytime() {
        return BantimeByPlaytime;
    }

    public int getBantimeByPlaytimePercent() {
        return BantimeByPlaytimePercent;
    }

    public int getBantimeByPlaytimeInterval() {
        return BantimeByPlaytimeInterval;
    }

    public boolean isCombatDeath() {
        return CombatDeath;
    }

    public int getCombatDeathTime() {
        return CombatDeathTime;
    }

    public boolean isAnnounceLivesOnRespawn() {
        return AnnounceLivesOnRespawn;
    }

    public int getBantimeByPlaytimeMinimumPlayerKillBantime() {
        return BantimeByPlaytimeMinimumPlayerKillBantime;
    }

    public int getBantimeByPlaytimeMinimumMonsterKillBantime() {
        return BantimeByPlaytimeMinimumMonsterKillBantime;
    }

    public int getBantimeByPlaytimeMinimumEnvironmentKillBantime() {
        return BantimeByPlaytimeMinimumEnvironmentKillBantime;
    }
}
