package com.choco.home.config;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;

@Configuration
public class FirebaseConfig {

    @Bean
    public Firestore firestore() throws Exception {

    	String firebaseJson = System.getenv("FIREBASE_JSON");
    	if (firebaseJson == null) {
    	    throw new RuntimeException("Missing FIREBASE_JSON env variable");
    	}
    	InputStream serviceAccount = new ByteArrayInputStream(firebaseJson.getBytes(StandardCharsets.UTF_8));

    	FirebaseOptions options = FirebaseOptions.builder()
    	        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
    	        .build();

        if (FirebaseApp.getApps().isEmpty()) {
            FirebaseApp.initializeApp(options);
        }

        return FirestoreClient.getFirestore();
    }
}
