package com.akaswising.conshite;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;
import org.ppsspp.ppsspp.PpssppActivity;

import java.util.Locale;

public final class MainActivity extends AppCompatActivity {
    private static final String PREFS = "conshite_library";
    private static final String GAMES_KEY = "games";

    private final JSONArray games = new JSONArray();
    private LinearLayout gameList;
    private TextView statusText;
    private ActivityResultLauncher<String[]> gamePicker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadGames();
        gamePicker = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(),
            this::onGamePicked
        );
        buildScreen();
        renderGames();
        refreshRuntimeStatus();
    }

    private void buildScreen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(20), dp(24), dp(20), dp(24));
        page.setBackgroundColor(Color.rgb(16, 19, 26));
        scroll.addView(page);

        page.addView(text("CONSHITE", 30, true, Color.WHITE));
        TextView subtitle = text(
            "CONSOLE GAME HUB  /  PSP FIRST", 12, true,
            Color.rgb(104, 190, 255)
        );
        addTopMargin(page, subtitle, 4);

        TextView description = text(
            "Your local PlayStation Portable game library",
            14, false, Color.rgb(190, 199, 211)
        );
        addTopMargin(page, description, 6);

        LinearLayout runtimeCard = card();
        addTopMargin(page, runtimeCard, 20);
        runtimeCard.addView(text("NATIVE EMULATOR RUNTIME", 12, true,
            Color.rgb(104, 190, 255)));
        statusText = text("Checking PPSSPP native engine…", 14, false, Color.WHITE);
        addTopMargin(runtimeCard, statusText, 8);
        TextView runtimeDetail = text(
            "Java handles the Android app and library. The PPSSPP emulator, PSP CPU, " +
            "graphics and audio run in native C++ through the Android NDK.",
            12, false, Color.rgb(173, 184, 198)
        );
        addTopMargin(runtimeCard, runtimeDetail, 8);

        Button addButton = new Button(this);
        addButton.setText("＋  ADD PSP GAME FILE");
        addButton.setAllCaps(false);
        addButton.setOnClickListener(v -> gamePicker.launch(new String[]{"*/*"}));
        addTopMargin(page, addButton, 18);

        TextView libraryTitle = text("GAME LIBRARY", 16, true, Color.WHITE);
        addTopMargin(page, libraryTitle, 22);

        gameList = new LinearLayout(this);
        gameList.setOrientation(LinearLayout.VERTICAL);
        addTopMargin(page, gameList, 10);

        TextView legalNote = text(
            "No game files are bundled. Import homebrew titles or game images " +
            "you are legally entitled to use.",
            11, false, Color.rgb(150, 160, 174)
        );
        addTopMargin(page, legalNote, 18);
        setContentView(scroll);
    }

    private void refreshRuntimeStatus() {
        boolean loaded = PpssppActivity.libraryLoaded;
        statusText.setText(loaded
            ? "PPSSPP native emulator loaded"
            : "PPSSPP native library did not load for this device ABI");
        statusText.setTextColor(loaded
            ? Color.rgb(112, 220, 157)
            : Color.rgb(255, 166, 112));
        if (gameList != null) {
            renderGames();
        }
    }

    private void onGamePicked(Uri uri) {
        if (uri == null) {
            return;
        }

        String name = queryDisplayName(uri);
        int dot = name.lastIndexOf('.');
        String extension = dot >= 0
            ? name.substring(dot + 1).toLowerCase(Locale.ROOT)
            : "";

        if (!extension.equals("iso") && !extension.equals("cso")) {
            Toast.makeText(this, "Choose a PSP .iso or .cso file.", Toast.LENGTH_LONG).show();
            return;
        }

        try {
            getContentResolver().takePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
            );
        } catch (SecurityException ignored) {
            // Some document providers grant temporary access only.
        }

        String uriText = uri.toString();
        if (!containsUri(uriText)) {
            JSONObject entry = new JSONObject();
            try {
                entry.put("name", name);
                entry.put("uri", uriText);
                entry.put("format", extension.toUpperCase(Locale.ROOT));
                games.put(entry);
                saveGames();
            } catch (Exception error) {
                Toast.makeText(this, "Could not save this game entry.", Toast.LENGTH_LONG).show();
                return;
            }
        }

        renderGames();
        Toast.makeText(this, "Added " + name, Toast.LENGTH_SHORT).show();
    }

    private void renderGames() {
        gameList.removeAllViews();

        if (games.length() == 0) {
            LinearLayout emptyCard = card();
            emptyCard.addView(text("No games added yet", 16, true, Color.WHITE));
            TextView hint = text(
                "Use the button above to import a PSP ISO or CSO file.",
                12, false, Color.rgb(173, 184, 198)
            );
            addTopMargin(emptyCard, hint, 8);
            gameList.addView(emptyCard);
            return;
        }

        for (int i = 0; i < games.length(); i++) {
            JSONObject game = games.optJSONObject(i);
            if (game == null) {
                continue;
            }

            String name = game.optString("name", "Unknown game");
            String uriText = game.optString("uri", "");
            String format = game.optString("format", "ISO");

            LinearLayout gameCard = card();
            LinearLayout.LayoutParams gameParams =
                new LinearLayout.LayoutParams(-1, -2);
            gameParams.bottomMargin = dp(10);
            gameList.addView(gameCard, gameParams);

            gameCard.addView(text(name, 15, true, Color.WHITE));
            TextView metadata = text(
                "PlayStation Portable  •  " + format,
                12, false, Color.rgb(173, 184, 198)
            );
            addTopMargin(gameCard, metadata, 5);

            Button playButton = new Button(this);
            playButton.setText("▶  PLAY WITH PPSSPP");
            playButton.setAllCaps(false);
            playButton.setEnabled(PpssppActivity.libraryLoaded);
            playButton.setOnClickListener(v -> launchGame(uriText));
            addTopMargin(gameCard, playButton, 8);
        }
    }

    private void launchGame(String uriText) {
        try {
            Uri gameUri = Uri.parse(uriText);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setClass(this, PpssppActivity.class);
            intent.setDataAndType(gameUri, "*/*");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        } catch (Exception error) {
            String detail = error.getMessage() == null
                ? error.getClass().getSimpleName()
                : error.getMessage();
            Toast.makeText(this, "Could not open the game: " + detail, Toast.LENGTH_LONG).show();
        }
    }

    private String queryDisplayName(Uri uri) {
        Cursor cursor = null;
        try {
            cursor = getContentResolver().query(
                uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null
            );
            if (cursor != null && cursor.moveToFirst()) {
                int column = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (column >= 0) {
                    String name = cursor.getString(column);
                    if (name != null && !name.isEmpty()) {
                        return name;
                    }
                }
            }
        } catch (Exception ignored) {
            // Fall back to the URI when a provider has no display name.
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        String last = uri.getLastPathSegment();
        return last == null ? "Unknown game.iso" : last;
    }

    private boolean containsUri(String target) {
        for (int i = 0; i < games.length(); i++) {
            JSONObject game = games.optJSONObject(i);
            if (game != null && target.equals(game.optString("uri"))) {
                return true;
            }
        }
        return false;
    }

    private void loadGames() {
        SharedPreferences preferences = getSharedPreferences(PREFS, MODE_PRIVATE);
        String raw = preferences.getString(GAMES_KEY, "[]");
        try {
            JSONArray saved = new JSONArray(raw);
            for (int i = 0; i < saved.length(); i++) {
                JSONObject entry = saved.optJSONObject(i);
                if (entry != null && !entry.optString("uri").isEmpty()) {
                    games.put(entry);
                }
            }
        } catch (Exception ignored) {
            // Corrupt preferences must not block the GameHub.
        }
    }

    private void saveGames() {
        getSharedPreferences(PREFS, MODE_PRIVATE)
            .edit()
            .putString(GAMES_KEY, games.toString())
            .apply();
    }

    private LinearLayout card() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(14), dp(14), dp(14), dp(14));
        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.rgb(27, 34, 46));
        background.setCornerRadius(dp(14));
        layout.setBackground(background);
        return layout;
    }

    private TextView text(String value, int size, boolean bold, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        if (bold) {
            view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        }
        view.setGravity(Gravity.CENTER_VERTICAL);
        return view;
    }

    private void addTopMargin(LinearLayout parent, android.view.View child, int marginDp) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = dp(marginDp);
        parent.addView(child, params);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
