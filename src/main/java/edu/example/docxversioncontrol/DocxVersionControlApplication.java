package edu.example.docxversioncontrol;

import edu.example.docxversioncontrol.storage.filesystem.StorageProperties;
import edu.example.docxversioncontrol.storage.filesystem.StorageService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@SpringBootApplication
@EnableConfigurationProperties(StorageProperties.class)
public class DocxVersionControlApplication implements WebMvcConfigurer {

    public static void main(String[] args) throws Exception {
        SpringApplication.run(DocxVersionControlApplication.class, args);
    }

    /**
     * Добавляет конечные точки без контроллеров
     */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("selectDocType");
    }

    /**
     * Очистка хранилища на старте приложения
     */
    @Bean
    CommandLineRunner init(StorageService storageService) {
        return (args) -> {
            storageService.deleteAll();
            storageService.init();
        };
    }
}
