package com.choco.home.config;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Base64;

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

    	String firebase64 = System.getenv("FIREBASE_JSON");
    	if (firebase64 == null) {
    	    throw new RuntimeException("Missing FIREBASE_JSON env variable");
    	}

    	byte[] decodedBytes = Base64.getDecoder().decode(firebase64);
    	InputStream serviceAccount = new ByteArrayInputStream(decodedBytes);

    	FirebaseOptions options = FirebaseOptions.builder()
    	        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
    	        .build();


        if (FirebaseApp.getApps().isEmpty()) {
            FirebaseApp.initializeApp(options);
        }

        return FirestoreClient.getFirestore();
    }
}
