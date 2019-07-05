package com.tanish2k09.sce;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.tanish2k09.sce.fragments.containerFragments.CategoryFragment;
import com.tanish2k09.sce.fragments.containerFragments.fConfigVar;
import com.tanish2k09.sce.helpers.ConfigImportExport;
import com.tanish2k09.sce.utils.ConfigCacheClass;
import com.topjohnwu.superuser.Shell;

import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private CardView noNotesCard, saveCard;
    private ConfigImportExport cig;
    private boolean runScript = false;

    static {
        /* Shell.Config methods shall be called before any shell is created
         * This is the reason why you should call it in a static block
         * The followings are some examples, check Javadoc for more details */
        Shell.Config.setFlags(Shell.FLAG_REDIRECT_STDERR);
        Shell.Config.verboseLogging(BuildConfig.DEBUG);
        Shell.Config.setTimeout(5);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setElevation(0);

        cig = new ConfigImportExport(this);

        noNotesCard = findViewById(R.id.no_notes_card);
        noNotesCard.setOnClickListener(v -> commenceConfigImport());

        saveCard = findViewById(R.id.saveButton);
        saveCard.setOnClickListener(v -> cig.saveConfig(runScript));

        getWindow().setNavigationBarColor(ContextCompat.getColor(this, R.color.black));
        Shell.su("su").submit();

        SharedPreferences sp = getSharedPreferences("settings",MODE_PRIVATE);
        if (sp.getBoolean("autoImportConfig", false))
            commenceConfigImport();

    }

    @Override
    protected void onResume() {
        super.onResume();

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                 Manifest.permission.READ_EXTERNAL_STORAGE},
                    1);
        }

        updateTheme();
    }

    private void updateTheme() {
        SharedPreferences sp = getSharedPreferences("settings",MODE_PRIVATE);
        int accent = Color.parseColor(sp.getString("accentCol", "#00bfa5"));

        saveCard.setCardBackgroundColor(accent);
        noNotesCard.setCardBackgroundColor(accent);
        runScript = sp.getBoolean("autoUpdateConfig", false);

        String color = "#121212";
        if (sp.getBoolean("useBlackNotDark", true))
            color = "#000000";

        ColorDrawable colDrawable = new ColorDrawable(Color.parseColor(color));

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setBackground(colDrawable);
        getWindow().setStatusBarColor(Color.parseColor(color));
        ConstraintLayout mainContentLayout = findViewById(R.id.mainContentLayout);
        mainContentLayout.setBackground(colDrawable);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            noNotesCard.setOutlineAmbientShadowColor(accent);
            noNotesCard.setOutlineSpotShadowColor(accent);
            saveCard.setOutlineAmbientShadowColor(accent);
            saveCard.setOutlineSpotShadowColor(accent);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        switch(id) {
            case R.id.action_import_config:
                return commenceConfigImport();

            case R.id.action_info:
                Intent infoActivityIntent = new Intent(this, InfoActivity.class);
                startActivity(infoActivityIntent);
                break;

            case R.id.action_settings:
                Intent settingsActivityIntent = new Intent(this, SettingsActivity.class);
                startActivity(settingsActivityIntent);
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 1 &&
            grantResults[0] == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(this, R.string.StoragePermDenied, Toast.LENGTH_LONG).show();
                finish();
                moveTaskToBack(true);
        }
    }

    private boolean commenceConfigImport() {
        removeConfigFragments();

        if (!cig.configImport()) {
            Toast.makeText(this, getResources().getString(R.string.swwImport), Toast.LENGTH_SHORT).show();
            return false;
        }

        int size = ConfigCacheClass.getConfiglistSize();
        Toast.makeText(this, "Found " + size + " values", Toast.LENGTH_SHORT).show();

        if (size > 0) {
            fillConfigFragments();
            noNotesCard.setVisibility(View.INVISIBLE);
            saveCard.setVisibility(View.VISIBLE);
            saveCard.setEnabled(true);
        } else {
            Toast.makeText(this, "No values could be extracted", Toast.LENGTH_SHORT).show();
        }
        return true;
    }

    private void removeConfigFragments() {
        FragmentManager fm = getSupportFragmentManager();

        for (int idx = 1; idx < ConfigCacheClass.getConfiglistSize(); ++idx) {
            CategoryFragment cf = (CategoryFragment) fm.findFragmentByTag("catFrag"+Objects.requireNonNull(ConfigCacheClass.getStringVal(idx)).getCategory());
            if (cf != null) {
                if(cf.popFragment(idx))
                    fm.beginTransaction().remove(cf).commitNow();
            }
        }
    }

    private void fillConfigFragments() {
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft;

        /* Assumes the first config is PROFILE.VERSION, so we skip that */
        for (int idx = 1; idx < ConfigCacheClass.getConfiglistSize(); ++idx) {
            fConfigVar configFragment = new fConfigVar();
            String category = Objects.requireNonNull(ConfigCacheClass.getStringVal(idx)).getCategory();
            CategoryFragment cf = (CategoryFragment) fm.findFragmentByTag("catFrag"+category);

            if (cf == null) {
                ft = fm.beginTransaction();
                cf = new CategoryFragment();
                cf.setInstanceCategory(category);
                ft.add(R.id.listLayout, cf, "catFrag"+category).commitNow();
            }

            if (!configFragment.setupCardInfo(idx))
                continue;

            cf.pushConfigVarFragment(configFragment, idx);
        }
    }
}
