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
	public String backup_command = "";
	public String clean_command = "";
	public BackupConfig() {}

	public static BackupConfig load(Path path) throws IOException {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();

		if (Files.exists(path) && Files.size(path) > 0) {
			BackupConfig config = gson.fromJson(Files.readString(path), BackupConfig.class);
			if (config != null) return config;
		}

		BackupConfig defaultConfig = new BackupConfig();
		if (path.getParent() != null) Files.createDirectories(path.getParent());
		Files.writeString(path, gson.toJson(defaultConfig));
		return defaultConfig;
	}
}