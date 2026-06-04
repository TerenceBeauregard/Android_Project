package fr.android.projet;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.google.android.material.navigation.NavigationView;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Ajouter la Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Ajouter le DrawerLayout + bouton hamburger
        drawerLayout = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this,
                drawerLayout,
                toolbar,
                R.string.drawer_open,
                R.string.drawer_close
        );
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // Ajouter le NavigationView
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // Fragment par défaut
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new MapFragment())
                    .commit();
            navigationView.setCheckedItem(R.id.nav_map);
        }

        if (savedInstanceState == null) {
            boolean isLandscape = (findViewById(R.id.fragment_container_side) != null);

            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new MapFragment())
                    .commit();

            if (isLandscape) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container_side, new ListFragment())
                        .commit();
            }

            navigationView.setCheckedItem(R.id.nav_map);
        }
    }

    /**
     * Changer le frament en fonction de l'item sélectionné dans le menu de navigation
     * @param item
     * @return
     */
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        Fragment fragment = null;

        int id = item.getItemId();
        if (id == R.id.nav_map) {
            fragment = new MapFragment();
        } else if (id == R.id.nav_list) {
            fragment = new ListFragment();
        } else if (id == R.id.nav_add) {
            fragment = new AddFragment();
        } else if (id == R.id.nav_stats) {
            fragment = new StatsFragment();
        }

        if (fragment != null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .commit();
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    /**
     * Ajouter les 3 points dans la Toolbar
     * @param menu
     * @return
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            // TODO : ouvrir les paramètres
            return true;
        } else if (id == R.id.action_about) {
            // TODO : ouvrir à propos
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Bouton retour ferme le drawer s'il est ouvert
    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}