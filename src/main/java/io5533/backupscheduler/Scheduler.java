package io5533.backupscheduler;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

public class Scheduler {
    public static boolean paused = false;
    public static boolean scriptRunning = false;

    private static int backupTick = 0;

    public static int getBackupTick() {
        return backupTick;
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
        if (scriptRunning) {
            logWarn(origin, "Script is still running. Skip the backup.");
            return;
        }
        if (config.backup_command.isEmpty()) {
            logWarn(origin, "config.backup_command is empty! Skip the backup. Tip: check the " + Config.CONFIG_FILE.getPath() + " file.");
            return;
        }
        scriptRunning = true;
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
                                scriptRunning = false;
                                if (!success3) logWarn(origin, "Failed to execute save-on!");
                                logInfo(origin, "Backup completed");
                            }), "save-on");
                        }
                    }).start();

                }
                else {
                    logError(origin, "Backup failed! (save-all flush)");
                    scriptRunning = false;
                }

            }), "save-all flush");
            else {
                logError(origin, "Backup failed! (save-off)");
                scriptRunning = false;
            }

        }), "save-off");
    }

    public static void cleanup() {
        cleanup(null);
    }
    public static void cleanup(@Nullable CommandSourceStack origin) {
        Config config = Config.getInstance();
        if (scriptRunning) {
            logWarn(origin, "Script is still running. Skip the cleanup.");
            return;
        }
        if (config.cleanup_command.isEmpty()) {
            logWarn(origin, "config.cleanup_command is empty! Skip the cleanup. Tip: check the " + Config.CONFIG_FILE.getPath() + " file.");
            return;
        }
        scriptRunning = true;
        new Thread(() -> {
            ProcessBuilder pb = new ProcessBuilder(config.cleanup_command);
            try {
                Process process = pb.start();
                int exitCode = process.waitFor();
                if (exitCode != 0) {
                    logError(origin, "cleanup_command exit code: " + exitCode);
                }
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                scriptRunning = false;
                logInfo(origin, "Cleanup completed");
            }
        }).start();
    }

    public static void tick(MinecraftServer server) {
        if (paused) return;

        backupTick ++;
        backupTick %= Config.getInstance().backup_tick;

        if (backupTick == 0) backup(server);
    }
}
