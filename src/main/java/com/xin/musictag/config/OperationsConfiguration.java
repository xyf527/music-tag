package com.xin.musictag.config;

import com.xin.musictag.application.*;
import io.minio.*;
import org.springframework.context.annotation.*;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;
import java.time.Clock;
import java.io.ByteArrayInputStream;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(OperationsSettings.class)
public class OperationsConfiguration {
    @Bean public Clock operationClock() { return Clock.systemUTC(); }
    @Bean public BackupStore backupStore(OperationsSettings settings) {
        return new BackupStore() {
            private MinioClient client() {
                if (!settings.configured()) throw new IllegalStateException("BACKUP_CONFIGURATION");
                return MinioClient.builder().endpoint(settings.endpoint()).credentials(settings.accessKey(), settings.secretKey())
                        .httpClient(new okhttp3.OkHttpClient.Builder().connectTimeout(java.time.Duration.ofSeconds(5)).readTimeout(java.time.Duration.ofSeconds(5)).writeTimeout(java.time.Duration.ofSeconds(30)).build()).build();
            }
            public void put(String key, byte[] bytes, String hash) throws Exception {
                putStream(key,new ByteArrayInputStream(bytes),bytes.length,hash);
            }
            public void putFile(String key,java.nio.file.Path file,String hash)throws Exception {
                try(var input=java.nio.file.Files.newInputStream(file)){putStream(key,input,java.nio.file.Files.size(file),hash);}
            }
            private void putStream(String key,java.io.InputStream input,long size,String hash)throws Exception {
                if (!settings.minioEnabled()) return;
                MinioClient client = client();
                try {
                    var stat = client.statObject(StatObjectArgs.builder().bucket(settings.bucket()).object(key).build());
                    if (hash.equals(stat.userMetadata().get("sha256"))) return;
                } catch (io.minio.errors.ErrorResponseException e) {
                    if (!java.util.Set.of("NoSuchKey", "NoSuchObject").contains(e.errorResponse().code())) throw e;
                }
                client.putObject(PutObjectArgs.builder().bucket(settings.bucket()).object(key)
                        .userMetadata(java.util.Map.of("sha256", hash)).stream(input, size, -1).build());
            }
            public boolean ready() {
                if (!settings.minioEnabled()) return true;
                try { return client().bucketExists(BucketExistsArgs.builder().bucket(settings.bucket()).build()); }
                catch (Exception e) { return false; }
            }
        };
    }
}
