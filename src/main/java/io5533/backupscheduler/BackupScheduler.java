package io5533.backupscheduler;

import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.ResourceLocation;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BackupScheduler implements ModInitializer {
	public static final String MOD_ID = "backup-scheduler";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ServerTickEvents.END_SERVER_TICK.register(minecraftServer -> {
			if (minecraftServer.getPlayerCount() > 0) Scheduler.tick(minecraftServer);
		});

		if (Config.getInstance().admin_commands) CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			dispatcher.register(Commands.literal("backup-scheduler")
					.requires(source -> source.hasPermission(2))
					.then(Commands.literal("unlock")
							.executes(commandContext -> {
								CommandSourceStack sourceStack = commandContext.getSource();
								if (!Scheduler.scriptRunning) {
									sourceStack.sendFailure(Component.literal("Script is not running"));
									return 0;
								}
								Scheduler.paused = true;
								Scheduler.scriptRunning = false;

								sourceStack.sendSystemMessage(
										Component.literal(
												"WARNING: Unlocking while a script is still running may cause data corruption!"
										).withStyle(ChatFormatting.GOLD)
								);
								sourceStack.sendSystemMessage(
										Component.literal(
												"Only use this command if you are sure that the script has finished."
										).withStyle(ChatFormatting.GOLD)
								);
								sourceStack.sendSystemMessage(
										Component.literal(
												"The Backup Scheduler has been paused automatically for safety."
										).withStyle(ChatFormatting.GOLD)
								);
								sourceStack.sendSystemMessage(
										Component.literal(
												"Verify that the script has finished before using '/backup-scheduler resume'."
										).withStyle(ChatFormatting.GOLD)
								);


								commandContext.getSource().sendSuccess(() -> Component.literal("Backup scheduler is UNLOCKED"), true);

								return 1;
							})
					)
					.then(Commands.literal("lock")
							.executes(commandContext -> {
								CommandSourceStack sourceStack = commandContext.getSource();
								if (Scheduler.scriptRunning) {
									sourceStack.sendFailure(Component.literal("Script is still running"));
									return 0;
								}
								Scheduler.paused = true;
								Scheduler.scriptRunning = true;

								sourceStack.sendSystemMessage(
										Component.literal(
												"WARNING: This command manually locks the Backup Scheduler."
										).withStyle(ChatFormatting.GOLD)
								);
								sourceStack.sendSystemMessage(
										Component.literal(
												"If a script is currently running, use '/backup-scheduler pause' instead."
										).withStyle(ChatFormatting.GOLD)
								);
								sourceStack.sendSystemMessage(
										Component.literal(
												"While locked, scheduled scripts will not be executed when the tick threshold is reached."
										).withStyle(ChatFormatting.GOLD)
								);
								sourceStack.sendSystemMessage(
										Component.literal(
												"This may cause a scheduled backup or cleanup to be missed."
										).withStyle(ChatFormatting.GOLD)
								);
								sourceStack.sendSystemMessage(
										Component.literal(
												"The Backup Scheduler has been paused automatically for safety."
										).withStyle(ChatFormatting.GOLD)
								);
								sourceStack.sendSystemMessage(
										Component.literal(
												"Verify the script state before using '/backup-scheduler resume'."
										).withStyle(ChatFormatting.GOLD)
								);

								commandContext.getSource().sendSuccess(() -> Component.literal("Backup scheduler is LOCKED"), true);

								return 1;
							})
					)
					.then(Commands.literal("remain")
							.executes(commandContext -> {
								int tick = Scheduler.getBackupTick();
								int remain = Config.getInstance().backup_tick - tick;

								int sec = remain/20;
								int min = sec/60;
								sec %= 60;
								commandContext.getSource().sendSystemMessage(
										Component.literal("Current scheduler tick is "+tick+". "+remain+" ticks(about "+min+"min "+sec+"sec) remain for next backup.")
								);
								return 1;
							})
					)
					.then(Commands.literal("reload")
							.executes(commandContext -> {
								Scheduler.paused = true;
								Config.reload();
								Scheduler.paused = false;
								commandContext.getSource().sendSuccess(() -> Component.literal("Config file reloaded"), true);
								return 1;
							})
					)
					.then(Commands.literal("pause")
							.executes(commandContext -> {
								Scheduler.paused = true;
								commandContext.getSource().sendSuccess(() -> Component.literal("Backup scheduler is paused"), true);
								return 1;
							})
					)
					.then(Commands.literal("resume")
							.executes(commandContext -> {
								Scheduler.paused = false;
								commandContext.getSource().sendSuccess(() -> Component.literal("Backup scheduler is resumed"), true);
								return 1;
							})
					)
					.then(Commands.literal("backup")
							.executes(commandContext -> {
								Scheduler.backup(commandContext.getSource().getServer(), commandContext.getSource());
								commandContext.getSource().sendSuccess(() -> Component.literal("Backup started"), true);
								return 1;
							})
					)
					.then(Commands.literal("cleanup")
							.executes(commandContext -> {
								Scheduler.cleanup(commandContext.getSource());
								commandContext.getSource().sendSuccess(() -> Component.literal("Cleanup started"), true);
								return 1;
							})
					)
					.then(Commands.literal("running")
							.executes(commandContext -> {
								commandContext.getSource().sendSystemMessage(
										Component.literal(
												Scheduler.scriptRunning?
												"Script is still running" :
												"Script is not running"
										)
								);
								return 1;
							})
					)
			);
		});
	}

	public static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
	}
}
