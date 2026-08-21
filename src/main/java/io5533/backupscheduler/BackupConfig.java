package io5533.backupscheduler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class BackupConfig {
	public static final String CONFIG_NAME = "backup.json";
	public int tick = 36000;
	public boolean admin_commands = true;
	public String command = "";
	public BackupConfig(Path path) throws IOException {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();

		if (Files.exists(path)) {
			String json = Files.readString(path);
			BackupConfig config = gson.fromJson(json, BackupConfig.class);
			this.command = config.command;
			this.tick = config.tick;
			this.admin_commands = config.admin_commands;
		} else {
			Files.createDirectories(path.getParent());
			Files.writeString(path, gson.toJson(this));
		}
	}
}