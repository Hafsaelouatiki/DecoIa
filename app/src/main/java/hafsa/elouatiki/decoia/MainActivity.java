package hafsa.elouatiki.decoia;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private TextView tvWelcome;
    private MaterialButton btnLogout, btnTry, btnMaps, btnMic, btnPlay;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private MediaRecorder recorder = null;
    private MediaPlayer player = null;
    private boolean isRecording = false;
    private String audioFileName = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();

        tvWelcome = findViewById(R.id.tvWelcome);
        btnTry = findViewById(R.id.btnTry);
        btnMaps = findViewById(R.id.btnMaps);
        btnMic = findViewById(R.id.btnMic);
        btnPlay = findViewById(R.id.btnPlay);
        btnLogout = findViewById(R.id.btnLogout);

        audioFileName = getExternalCacheDir().getAbsolutePath() + "/vocal_deco_ia.m4a";

        if (user != null) {
            recupererNomUtilisateur(user.getUid());
        } else {
            goToLogin();
        }

        btnMic.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO_PERMISSION);
            } else {
                toggleRecording();
            }
        });

        btnPlay.setOnClickListener(v -> playAudio());

        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            goToLogin();
        });
    }

    private void toggleRecording() {
        if (!isRecording) {
            recorder = new MediaRecorder();
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            recorder.setOutputFile(audioFileName);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            try {
                recorder.prepare();
                recorder.start();
                btnMic.setText("ARRÊTER L'IA");
                btnMic.setIconResource(android.R.drawable.ic_media_pause);
                btnPlay.setVisibility(View.GONE);
                isRecording = true;
            } catch (IOException e) {
                Log.e("VoiceIA", "Start failed", e);
            }
        } else {
            stopRecording();
        }
    }

    private void stopRecording() {
        if (recorder != null) {
            try {
                recorder.stop();
                recorder.release();
            } catch (RuntimeException e) {
                Log.e("VoiceIA", "Stop failed", e);
            }
            recorder = null;
        }
        btnMic.setText("PARLER À L'IA");
        btnMic.setIconResource(android.R.drawable.ic_btn_speak_now);
        btnPlay.setVisibility(View.VISIBLE);
        isRecording = false;
        Toast.makeText(this, "Message vocal prêt", Toast.LENGTH_SHORT).show();
    }

    private void playAudio() {
        if (player != null) {
            player.release();
        }
        player = new MediaPlayer();
        try {
            player.setDataSource(audioFileName);
            player.prepare();
            player.start();
            Toast.makeText(this, "Lecture en cours...", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Log.e("VoiceIA", "Playback failed", e);
            Toast.makeText(this, "Erreur de lecture", Toast.LENGTH_SHORT).show();
        }
    }

    private void recupererNomUtilisateur(String uid) {
        db.collection("users").document(uid).get().addOnSuccessListener(ds -> {
            if (ds.exists()) tvWelcome.setText("Bonjour " + ds.getString("name"));
        });
    }

    private void goToLogin() {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (recorder != null) { recorder.release(); recorder = null; }
        if (player != null) { player.release(); player = null; }
    }
}
