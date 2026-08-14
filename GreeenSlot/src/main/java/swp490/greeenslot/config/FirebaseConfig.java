package swp490.greeenslot.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;

@Configuration
public class FirebaseConfig {

    @PostConstruct
    public void initialize() {
        try {
            InputStream serviceAccount =
                    new ClassPathResource(
                            "firebase/greenslot-46382-firebase-adminsdk-fbsvc-5a99ada1a5.json")
                            .getInputStream();

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(
                            GoogleCredentials.fromStream(serviceAccount))
                    .setStorageBucket("greenslot-46382.firebasestorage.app")
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
            }
        } catch (IOException e) {
            System.err.println("WARNING: Could not initialize Firebase! Missing credentials file: firebase/greenslot-46382-firebase-adminsdk-fbsvc-5a99ada1a5.json");
            // Do not throw the exception, let the app start without Firebase.
        }
    }
}
