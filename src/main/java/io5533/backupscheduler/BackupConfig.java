package io5533.backupscheduler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class BackupConfig {
	public static final File CONFIG_FILE = new File("config/backup.json");
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static volatile BackupConfig instance = null;
	public static BackupConfig getInstance() {
		if (instance == null) {
			try {
				instance = BackupConfig.load();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		return instance;
	}

	public int tick = 36000;
	public boolean admin_commands = true;
	public String backup_command = "";
	public String clean_command = "";
	public BackupConfig() {}

	private static BackupConfig load() throws IOException {

		if (CONFIG_FILE.exists() && Files.size(CONFIG_FILE.toPath()) > 0) {
			BackupConfig config = GSON.fromJson(Files.readString(CONFIG_FILE.toPath()), BackupConfig.class);
			if (config != null) return config;
		}
		BackupConfig config = new BackupConfig();
		config.save();
		return config;
	}

	public void save() throws IOException {
		if (CONFIG_FILE.toPath().getParent() != null) Files.createDirectories(CONFIG_FILE.toPath().getParent());
		Files.writeString(CONFIG_FILE.toPath(), GSON.toJson(this));
	}
}