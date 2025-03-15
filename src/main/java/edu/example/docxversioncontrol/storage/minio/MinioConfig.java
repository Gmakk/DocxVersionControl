package edu.example.docxversioncontrol.storage.minio;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinioConfig {
    @Value("${minio.access.username}")
    String username;
    @Value("${minio.access.password}")
    String password;
    @Value("${minio.url}")
    String minioUrl;

    @Bean
    public MinioClient createMinioClient() {
        return MinioClient.builder()
                .endpoint(minioUrl)
                .credentials(username, password)
                .build();
    }
}
