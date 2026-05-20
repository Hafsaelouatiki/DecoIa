package hafsa.elouatiki.decoia;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_modern);

        Animation fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
        fadeIn.setDuration(1200);
        findViewById(R.id.ivSplashLogo).startAnimation(fadeIn);
        findViewById(R.id.tvSplashTitle).startAnimation(fadeIn);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            startActivity(new Intent(this, WelcomeActivity.class));
            finish();
        }, 2200);
    }
}
