package org.example.collaborative_editor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CollabEditorApplication {

    public static void main(String[] args) {
        SpringApplication.run(CollabEditorApplication.class, args);
    }

}
