package com.example.namapopup;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.navigation.NavigationView;
import android.util.Log;
import android.widget.ImageButton;

public abstract class BaseDrawerActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    protected DrawerLayout drawerLayout;
    protected NavigationView navigationView;
    public ActionBarDrawerToggle actionBarDrawerToggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    protected void setupDrawer() {
        drawerLayout = findViewById(R.id.drawer_layout);

        // Set up custom titlebar elements
        ImageButton hamburgerButton = findViewById(R.id.main_menu);

        // Set up hamburger button click listener
        hamburgerButton.setOnClickListener(v -> {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START);
            } else {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });

        // Set up NavigationView
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {

        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.dictionary) {
            Log.d("onNavigationItemSelected", "launched dictionary screen");
            switchScreen("dictionary");
        } else if (id == R.id.settings) {
            Log.d("onNavigationItemSelected", "launched dictionary screen");
            switchScreen("settings");
        } else if (id == R.id.bookmark) {
            Log.d("onNavigationItemSelected", "launched dictionary screen");
            switchScreen("bookmark");
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    public void switchScreen(String screenType) {
        Intent intent;

        switch (screenType) {
            case "dictionary":
                intent = new Intent(this, DictionaryActivity.class);
                break;
            case "settings":
                intent = new Intent(this, SettingsActivity.class);
                break;
            case "bookmark":
                intent = new Intent(this, BookmarkActivity.class);
                break;
            default:
                throw new IllegalArgumentException("Invalid screen type: " + screenType);
        }

        this.startActivity(intent);
    }


    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        if (actionBarDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}