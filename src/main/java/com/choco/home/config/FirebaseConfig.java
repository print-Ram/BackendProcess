package com.choco.home.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;

@Configuration
public class FirebaseConfig {

    @Bean
    public Firestore firestore() throws Exception {

        // Read raw JSON from environment variable
        String firebaseJson = System.getenv("FIREBASE_CONFIG_JSON");

        if (firebaseJson == null || firebaseJson.isEmpty()) {
            throw new IllegalStateException("FIREBASE_CONFIG_JSON is not set!");
        }

        ByteArrayInputStream serviceAccount = 
                new ByteArrayInputStream(firebaseJson.getBytes());

        FirebaseOptions options = new FirebaseOptions.Builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .build();

        if (FirebaseApp.getApps().isEmpty()) {
            FirebaseApp.initializeApp(options);
        }

        return FirestoreClient.getFirestore();
    }
}
