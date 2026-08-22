package io5533.backupscheduler;

import net.minecraft.server.MinecraftServer;

import java.io.IOException;

public class Scheduler {
    public static boolean paused = false;
    private static boolean backupRunning = false;
    public static BackupConfig config = null;

    private static int backupTick = 0;

    public static int getBackupTick() {
        return backupTick;
    }
    public static boolean isBackupRunning() {
        return backupRunning;
    }

    public static void backup(MinecraftServer server) {
        if (backupRunning) {
            BackupScheduler.LOGGER.warn("Backup script is still running. Skip the backup.");
            return;
        }
        backupRunning = true;
        if (config.backup_command.isEmpty()) {
            BackupScheduler.LOGGER.warn("config.backup_command is empty! Skip the backup. Tip: check the config/{} file.", BackupConfig.CONFIG_NAME);
            return;
        }
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withCallback((success, __) -> {
            if (success) {
                new Thread(() -> {
                    ProcessBuilder pb = new ProcessBuilder(config.backup_command);
                    try {
                        Process process = pb.start();
                        int exitCode = process.waitFor();
                        if (exitCode != 0) {
                            BackupScheduler.LOGGER.error("backup_command exit code: {}", exitCode);
                        }
                    } catch (IOException | InterruptedException e) {
                        throw new RuntimeException(e);
                    } finally {
                        backupRunning = false;
                    }
                }).start();
            }
        }), "save-all flush");
    }

    public static void clean() {
        if (backupRunning) {
            BackupScheduler.LOGGER.warn("Backup script is still running. Skip the clean.");
            return;
        }
        backupRunning = true;
        if (config.clean_command.isEmpty()) {
            BackupScheduler.LOGGER.warn("config.clean_command is empty! Skip the clean. Tip: check the config/{} file.", BackupConfig.CONFIG_NAME);
            return;
        }
        new Thread(() -> {
            ProcessBuilder pb = new ProcessBuilder(config.clean_command);
            try {
                Process process = pb.start();
                int exitCode = process.waitFor();
                if (exitCode != 0) {
                    BackupScheduler.LOGGER.error("clean_command exit code: {}", exitCode);
                }
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                backupRunning = false;
            }
        }).start();
    }

    public static void tick(MinecraftServer server) {
        if (paused) return;

        backupTick ++;
        backupTick %= config.tick;

        if (backupTick == 0) backup(server);
    }
}
