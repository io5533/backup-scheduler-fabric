package io5533.backupscheduler;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BackupScheduler implements ModInitializer {
	public static final String MOD_ID = "backup-scheduler";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			dispatcher.register(Commands.literal("backup-scheduler")
					.requires(Commands.hasPermission(Commands.LEVEL_ADMINS))
					.then(Commands.literal("remain")
							.executes(commandContext -> {
								int tick = Scheduler.getBackupTick();
								int remain = Scheduler.config.tick - tick;
								commandContext.getSource().sendSystemMessage(
										Component.literal("[Backup] Current backup tick is "+tick+". "+remain+" ticks(about "+(remain/20)+"sec) remain for next backup.")
								);
								return 1;
							})
					)
					.then(Commands.literal("pause")
							.executes(commandContext -> {
								Scheduler.paused = true;
								commandContext.getSource().sendSystemMessage(
										Component.literal("[Backup] Backup scheduler is paused.")
								);
								return 1;
							})
					)
					.then(Commands.literal("resume")
							.executes(commandContext -> {
								Scheduler.paused = false;
								commandContext.getSource().sendSystemMessage(
										Component.literal("[Backup] Backup scheduler is resumed.")
								);
								return 1;
							})
					)
					.then(Commands.literal("backup")
							.executes(commandContext -> {
								Scheduler.backup(commandContext.getSource().getServer());
								commandContext.getSource().sendSystemMessage(
										Component.literal("[Backup] Starting the backup...")
								);
								return 1;
							})
					)
					.then(Commands.literal("isBackupRunning")
							.executes(commandContext -> {
								commandContext.getSource().sendSystemMessage(
										Component.literal(
												Scheduler.isBackupRunning()?
												"[Backup] The backup script is still running.":
												"[Backup] The backup script is not running."
										)
								);
								return 1;
							})
					)
			);
		});
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
