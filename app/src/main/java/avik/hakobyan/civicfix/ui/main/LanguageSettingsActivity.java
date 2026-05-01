package avik.hakobyan.civicfix.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.widget.RadioGroup;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import avik.hakobyan.civicfix.LocaleHelper;
import avik.hakobyan.civicfix.R;

public class LanguageSettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_language_settings);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        RadioGroup rgLanguage = findViewById(R.id.rgLanguage);
        String currentLang = LocaleHelper.getLanguage(this);

        if (currentLang.equals("en")) {
            rgLanguage.check(R.id.rbEnglish);
        } else if (currentLang.equals("ru")) {
            rgLanguage.check(R.id.rbRussian);
        } else if (currentLang.equals("hy")) {
            rgLanguage.check(R.id.rbArmenian);
        }

        rgLanguage.setOnCheckedChangeListener((group, checkedId) -> {
            String lang = "en";
            if (checkedId == R.id.rbRussian) {
                lang = "ru";
            } else if (checkedId == R.id.rbArmenian) {
                lang = "hy";
            }
            
            LocaleHelper.setLocale(this, lang);
            
            // Restart the app to apply changes
            Intent intent = new Intent(this, SplashActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }
}
