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

    // application.yml에서 환경변수를 가져옵니다.
    @Value("${spring.datasource.url}")
    private String mainDbUrl; // 예: jdbc:mysql://host:3306/db
    @Value("${spring.datasource.username}")
    private String mainDbUser;
    @Value("${spring.datasource.password}")
    private String mainDbPassword;

    // 백업 DB (Aiven) 정보 - 환경변수로 따로 관리하는 것을 추천합니다.

    @Value("${backup.datasource.url}")
    private String backupDbUrl; // 전체 JDBC URL을 받아옵니다.
    @Value("${backup.datasource.username}")
    private String backupUser;
    @Value("${backup.datasource.password}")
    private String backupPassword;

    @Scheduled(cron = "0 0 4 * * *")
    public void backupDatabase() {
        log.info("🚀 [Backup Start] 전체 데이터베이스 백업을 시작합니다...");

        try {
            // 1. 메인 DB 호스트 파싱
            String mainHost = parseHost(mainDbUrl);
            String mainDbName = parseDbName(mainDbUrl);

            // 2. 백업 DB (Aiven) 정보 파싱 (URL에서 추출)
            String backupHost = parseHost(backupDbUrl);
            String backupPort = parsePort(backupDbUrl);
            String backupDbName = parseDbName(backupDbUrl);

            // 2. 셸 명령어 작성 (mysqldump -> mysql)
            // ProcessBuilder의 환경변수 맵을 활용해 안전하게 주입
            String command = String.format(
                    // [Source: 로컬 Docker MySQL]
                    // 1. --no-tablespaces : 아까 겪으신 'Access denied' 권한 에러 해결
                    // 2. --set-gtid-purged=OFF : DB 간 이동 시 ID 충돌 방지
                    // 3. --ssl-mode=DISABLED : 로컬 도커는 SSL 설정이 없으므로 DISABLED가 맞음
                    "mysqldump -h %s -u %s -p$MAIN_PWD --single-transaction --skip-lock-tables --routines --triggers --no-tablespaces --set-gtid-purged=OFF --ssl-mode=DISABLED %s | " +

                    // 4. --ssl-mode=REQUIRED : Aiven은 보안상 SSL 필수
                    "mysql -h %s -P %s -u %s -p$BACKUP_PWD --ssl-mode=REQUIRED %s",


                    mainHost, mainDbUser, mainDbName,
                    backupHost, backupPort, backupUser, backupDbName
            );

            // 3. 프로세스 실행
            ProcessBuilder pb = new ProcessBuilder("/bin/sh", "-c", command);
            Map<String, String> env = pb.environment();
            env.put("MAIN_PWD", mainDbPassword);
            env.put("BACKUP_PWD", backupPassword);

            Process process = pb.start();

            // 로그 출력
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.error("Backup Process Log: {}", line);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                log.info("✅ [Backup Process Finished] 백업 성공!");
            } else {
                log.error("❌ [Backup Failed] 종료 코드: {}", exitCode);
            }

        } catch (Exception e) {
            log.error("❌ [Backup Error] 백업 중 예외 발생", e);
        }
    }

    private String parseHost(String url) {
        String cleanUrl = url.replace("jdbc:mysql://", ""); // host:port/dbName...
        return cleanUrl.substring(0, cleanUrl.indexOf("/")).split(":")[0];
    }

    private String parsePort(String url) {
        String cleanUrl = url.replace("jdbc:mysql://", "");
        String hostAndPort = cleanUrl.substring(0, cleanUrl.indexOf("/"));
        if (hostAndPort.contains(":")) {
            return hostAndPort.split(":")[1];
        }
        return "3306"; // 포트 없으면 기본값
    }

    private String parseDbName(String url) {
        String cleanUrl = url.replace("jdbc:mysql://", "");
        String dbAndParams = cleanUrl.substring(cleanUrl.indexOf("/") + 1);
        if (dbAndParams.contains("?")) {
            return dbAndParams.split("\\?")[0]; // 파라미터 제거
        }
        return dbAndParams;
    }
}
