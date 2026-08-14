package io5533.backupscheduler.mixin;

import io5533.backupscheduler.BackupConfig;
import io5533.backupscheduler.Scheduler;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;


@Mixin(MinecraftServer.class)
public class ServerMixin {
	@Inject(at = @At("TAIL"), method = "tickServer")
	private void tick(CallbackInfo info) {
		MinecraftServer self = (MinecraftServer) (Object) this;
		if (self.getPlayerCount() > 0) Scheduler.tick(self);
	}

	@Inject(at = @At("TAIL"), method = "<init>")
	private void init(CallbackInfo info) {
		MinecraftServer self = (MinecraftServer) (Object) this;
        try {
            Scheduler.config = new BackupConfig(self.getServerDirectory().resolve("config").resolve(BackupConfig.CONFIG_NAME));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}