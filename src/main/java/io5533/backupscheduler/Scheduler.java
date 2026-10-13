package io5533.backupscheduler;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

public class Scheduler {
    public static boolean paused = false;
    public static boolean scriptRunning = false;

    private static int backupTick = 0;

    public static int getBackupTick() {
        return backupTick;
    }

    private static boolean setAutoSave(MinecraftServer server, boolean enable) {
        boolean success = false;

        for(ServerLevel level : server.getAllLevels()) {
            if (level != null && level.noSave == enable) {
                level.noSave = !enable;
                success = true;
            }
        }

        return success;
    }

    private static boolean saveAllFlush(MinecraftServer server) {
        return server.saveEverything(true, true, true);
    }

    public static void backup(MinecraftServer server) {
        backup(server, null);
    }
    public static void backup(MinecraftServer server, @Nullable CommandSourceStack origin) {
        Config config = Config.getInstance();
        if (scriptRunning) {
            if (origin == null)
                BackupScheduler.LOGGER.error("Script is still running. Skip the backup.");
            else
                origin.sendFailure(Component.literal("Script is still running. Please try later."));
            return;
        }
        if (config.backup_command.isEmpty()) {
            if (origin == null)
                BackupScheduler.LOGGER.warn("config.backup_command is empty! Skip the backup. Tip: check the {} file.", Config.CONFIG_FILE.getPath());
            else
                origin.sendFailure(Component.literal("config.backup_command is empty! Nothing to do."));
            return;
        }

        scriptRunning = true;
        boolean wasEnabled = setAutoSave(server, false);
        if(!saveAllFlush(server)) {
            if (origin == null)
                BackupScheduler.LOGGER.error("Backup failed! (save-all flush)");
            else
                origin.sendFailure(Component.literal("Backup failed! (save-all flush)"));
            setAutoSave(server, wasEnabled);
            scriptRunning = false;
            return;
        }

        new Thread(() -> {
            ProcessBuilder pb = new ProcessBuilder(config.backup_command);
            try {
                Process process = pb.start();
                int exitCode = process.waitFor();
                if (exitCode != 0) {
                    if (origin == null)
                        BackupScheduler.LOGGER.error("backup_command exit code: {}", exitCode);
                    else
                        origin.sendFailure(Component.literal("backup_command exit code: " + exitCode));
                }
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                setAutoSave(server, wasEnabled);
                scriptRunning = false;

                if (origin == null)
                    BackupScheduler.LOGGER.info("Backup completed");
                else
                    origin.sendSuccess(() -> Component.literal("Backup completed"), true);
            }
        }).start();
    }

    public static void cleanup() {
        cleanup(null);
    }
    public static void cleanup(@Nullable CommandSourceStack origin) {
        Config config = Config.getInstance();
        if (scriptRunning) {
            if (origin == null)
                BackupScheduler.LOGGER.error("Script is still running. Skip the cleanup.");
            else
                origin.sendFailure(Component.literal("Script is still running. Please try later."));
            return;
        }
        if (config.cleanup_command.isEmpty()) {
            if (origin == null)
                BackupScheduler.LOGGER.warn("config.cleanup_command is empty! Skip the backup. Tip: check the {} file.", Config.CONFIG_FILE.getPath());
            else
                origin.sendFailure(Component.literal("config.cleanup_command is empty! Nothing to do."));
            return;
        }
        scriptRunning = true;
        new Thread(() -> {
            ProcessBuilder pb = new ProcessBuilder(config.cleanup_command);
            try {
                Process process = pb.start();
                int exitCode = process.waitFor();
                if (exitCode != 0) {
                    if (origin == null)
                        BackupScheduler.LOGGER.error("cleanup_command exit code: {}", exitCode);
                    else
                        origin.sendFailure(Component.literal("cleanup_command exit code: " + exitCode));
                }
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                scriptRunning = false;
                if (origin == null)
                    BackupScheduler.LOGGER.info("Cleanup completed");
                else
                    origin.sendSuccess(() -> Component.literal("Cleanup completed"), true);
            }
        }).start();
    }

    public static void tick(MinecraftServer server) {
        if (paused) return;

        backupTick ++;

        if (backupTick >= Config.getInstance().backup_tick) {
            backupTick = 0;
            backup(server);
        }
    }
}
