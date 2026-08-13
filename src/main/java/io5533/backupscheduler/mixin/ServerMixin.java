package io5533.backupscheduler.mixin;

import io5533.backupscheduler.BackupConfig;
import io5533.backupscheduler.BackupScheduler;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;


@Mixin(MinecraftServer.class)
public class ServerMixin {

	@Unique
	private boolean backupRunning = false;

	@Unique
    private BackupConfig config = null;
	@Unique
    private int backupTick = 0;

	@Unique
    private void backup(MinecraftServer server) {
		if (backupRunning) {
			BackupScheduler.LOGGER.warn("Backup script is still running. Skip the backup.");
			return;
		}
		backupRunning = true;
		if (config.command.isEmpty()) {
			BackupScheduler.LOGGER.warn("config.command is empty! Skip the backup. Tip: check the config/{} file.", BackupConfig.CONFIG_NAME);
			return;
		}
		server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withCallback((success, __) -> {
			if (success) {
				new Thread(() -> {
					ProcessBuilder pb = new ProcessBuilder(config.command);
					try {
						Process process = pb.start();
						int exitCode = process.waitFor();
						if (exitCode != 0) {
							BackupScheduler.LOGGER.error("backup command exit code: {}", exitCode);
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

	@Unique
    private void tick(MinecraftServer server) {
		backupTick ++;
		backupTick %= config.tick;

		if (backupTick == 0) backup(server);
	}

	@Inject(at = @At("TAIL"), method = "tickServer")
	private void tick(CallbackInfo info) {
		MinecraftServer self = (MinecraftServer) (Object) this;
		if (self.getPlayerCount() > 0) tick(self);
	}

	@Inject(at = @At("TAIL"), method = "<init>")
	private void init(CallbackInfo info) {
		MinecraftServer self = (MinecraftServer) (Object) this;
        try {
            config = new BackupConfig(self.getServerDirectory().resolve("config").resolve(BackupConfig.CONFIG_NAME));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}