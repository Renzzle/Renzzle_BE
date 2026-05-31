package com.renzzle.backend.global.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class BackupScheduler {

    @Value("${spring.datasource.url}")
    private String mainDbUrl;
    @Value("${spring.datasource.username}")
    private String mainDbUser;
    @Value("${spring.datasource.password}")
    private String mainDbPassword;

    @Value("${backup.datasource.url}")
    private String backupDbUrl;
    @Value("${backup.datasource.username}")
    private String backupUser;
    @Value("${backup.datasource.password}")
    private String backupPassword;

    @Scheduled(cron = "0 0 4 * * *")
    public void backupDatabase() {
        log.info("[Backup Start] Starting full database backup...");

        try {
            String mainHost = parseHost(mainDbUrl);
            String mainDbName = parseDbName(mainDbUrl);

            String backupHost = parseHost(backupDbUrl);
            String backupPort = parsePort(backupDbUrl);
            String backupDbName = parseDbName(backupDbUrl);

            // Passwords injected via env (MAIN_PWD/BACKUP_PWD) so they don't leak into the process argv.
            // Source flags:
            //   --no-tablespaces      : avoid 'Access denied' on tablespace metadata
            //   --set-gtid-purged=OFF : prevent GTID conflicts when restoring to another server
            //   --ssl-mode=DISABLED   : local docker MySQL has no TLS
            // Target flag:
            //   --ssl-mode=REQUIRED   : Aiven enforces TLS
            String command = String.format(
                    "mysqldump -h %s -u %s -p$MAIN_PWD --single-transaction --skip-lock-tables --routines --triggers --no-tablespaces --set-gtid-purged=OFF --ssl-mode=DISABLED %s | " +
                    "mysql -h %s -P %s -u %s -p$BACKUP_PWD --ssl-mode=REQUIRED %s",
                    mainHost, mainDbUser, mainDbName,
                    backupHost, backupPort, backupUser, backupDbName
            );

            ProcessBuilder pb = new ProcessBuilder("/bin/sh", "-c", command);
            Map<String, String> env = pb.environment();
            env.put("MAIN_PWD", mainDbPassword);
            env.put("BACKUP_PWD", backupPassword);

            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.info("Backup Process Log: {}", line);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                log.info("[Backup Process Finished] Backup succeeded");
            } else {
                log.error("[Backup Failed] exit code: {}", exitCode);
            }

        } catch (Exception e) {
            log.error("[Backup Error] exception during backup", e);
        }
    }

    private String parseHost(String url) {
        String cleanUrl = url.replace("jdbc:mysql://", "");
        return cleanUrl.substring(0, cleanUrl.indexOf("/")).split(":")[0];
    }

    private String parsePort(String url) {
        String cleanUrl = url.replace("jdbc:mysql://", "");
        String hostAndPort = cleanUrl.substring(0, cleanUrl.indexOf("/"));
        if (hostAndPort.contains(":")) {
            return hostAndPort.split(":")[1];
        }
        return "3306";
    }

    private String parseDbName(String url) {
        String cleanUrl = url.replace("jdbc:mysql://", "");
        String dbAndParams = cleanUrl.substring(cleanUrl.indexOf("/") + 1);
        if (dbAndParams.contains("?")) {
            return dbAndParams.split("\\?")[0];
        }
        return dbAndParams;
    }
}
