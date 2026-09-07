package io5533.backupscheduler;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.Nullable;

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

    private static void logError(@Nullable CommandSourceStack origin, String string) {
        if (origin != null) origin.sendFailure(Component.literal(string));
        BackupScheduler.LOGGER.error(string);
    }
    private static void logInfo(@Nullable CommandSourceStack origin, String string) {
        if (origin != null) origin.sendSystemMessage(Component.literal(string));
        BackupScheduler.LOGGER.info(string);
    }
    private static void logWarn(@Nullable CommandSourceStack origin, String string) {
        if (origin != null) origin.sendFailure(Component.literal(string));
        BackupScheduler.LOGGER.warn(string);
    }

    public static void backup(MinecraftServer server) {
        backup(server, null);
    }
    public static void backup(MinecraftServer server, @Nullable CommandSourceStack origin) {
        Config config = Config.getInstance();
        if (backupRunning) {
            logWarn(origin, "Backup script is still running. Skip the backup.");
            return;
        }
        backupRunning = true;
        if (config.backup_command.isEmpty()) {
            logWarn(origin, "config.backup_command is empty! Skip the backup. Tip: check the " + Config.CONFIG_FILE.getPath() + " file.");
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
                                logError(origin, "backup_command exit code: " + exitCode);
                            }
                        } catch (IOException | InterruptedException e) {
                            throw new RuntimeException(e);
                        } finally {
                            commands.performPrefixedCommand(stack.withCallback((success3, _3) -> {
                                backupRunning = false;
                                if (!success3) logWarn(origin, "Failed to execute save-on!");
                            }), "save-on");
                        }
                    }).start();

                }
                else logError(origin, "Backup failed!");

            }), "save-all flush");
            else logError(origin, "Backup failed!");

        }), "save-off");
    }

    public static void clean() {
        clean(null);
    }
    public static void clean(@Nullable CommandSourceStack origin) {
        Config config = Config.getInstance();
        if (backupRunning) {
            logWarn(origin, "Backup script is still running. Skip the clean.");
            return;
        }
        backupRunning = true;
        if (config.clean_command.isEmpty()) {
            logWarn(origin, "config.clean_command is empty! Skip the clean. Tip: check the " + Config.CONFIG_FILE.getPath() + " file.");
            return;
        }
        new Thread(() -> {
            ProcessBuilder pb = new ProcessBuilder(config.clean_command);
            try {
                Process process = pb.start();
                int exitCode = process.waitFor();
                if (exitCode != 0) {
                    logError(origin, "clean_command exit code: " + exitCode);
                }
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                backupRunning = false;
                logInfo(origin, "Cleaned");
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
