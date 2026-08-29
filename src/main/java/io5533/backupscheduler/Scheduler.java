package io5533.backupscheduler;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.MinecraftServer;

import java.io.IOException;

public class Scheduler {
    public static boolean paused = false;
    private static boolean backupRunning = false;

    private static int backupTick = 0;

    public static int getBackupTick() {
        return backupTick;
    }
    public static boolean isBackupRunning() {
        return backupRunning;
    }

    public static void backup(MinecraftServer server) {
        Config config = Config.getInstance();
        if (backupRunning) {
            BackupScheduler.LOGGER.warn("Backup script is still running. Skip the backup.");
            return;
        }
        backupRunning = true;
        if (config.backup_command.isEmpty()) {
            BackupScheduler.LOGGER.warn("config.backup_command is empty! Skip the backup. Tip: check the {} file.", Config.CONFIG_FILE.getPath());
            return;
        }
        final Commands commands = server.getCommands();
        final CommandSourceStack stack = server.createCommandSourceStack();

        commands.performPrefixedCommand(stack.withCallback((success1, _1) -> {

            if (success1) commands.performPrefixedCommand(stack.withCallback((success2, _2) -> {

                if (success2) {

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
                            commands.performPrefixedCommand(stack.withCallback((success3, _3) -> {
                                backupRunning = false;
                                if (!success3) BackupScheduler.LOGGER.warn("Failed to execute save-on!");
                            }), "save-on");
                        }
                    }).start();

                }
                else BackupScheduler.LOGGER.error("Backup failed!");

            }), "save-all flush");
            else BackupScheduler.LOGGER.error("Backup failed!");

        }), "save-off");
    }

    public static void clean() {
        Config config = Config.getInstance();
        if (backupRunning) {
            BackupScheduler.LOGGER.warn("Backup script is still running. Skip the clean.");
            return;
        }
        backupRunning = true;
        if (config.clean_command.isEmpty()) {
            BackupScheduler.LOGGER.warn("config.clean_command is empty! Skip the clean. Tip: check the {} file.", Config.CONFIG_FILE.getPath());
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
        backupTick %= Config.getInstance().tick;

        if (backupTick == 0) backup(server);
    }
}
