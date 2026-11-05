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
import android.view.View;
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MainActivity extends AppCompatActivity {
    private static final String PREFS = "conshite_library";
    private static final String GAMES_KEY = "games";

    private final JSONArray games = new JSONArray();
    private final ExecutorService importExecutor = Executors.newSingleThreadExecutor();
    private LinearLayout gameList;
    private TextView statusText;
    private Button addButton;
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        importExecutor.shutdown();
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
        addTopMargin(page, text(
            "Your local PlayStation Portable game library",
            14, false, Color.rgb(190, 199, 211)
        ), 6);

        LinearLayout runtimeCard = card();
        addTopMargin(page, runtimeCard, 20);
        runtimeCard.addView(text("NATIVE EMULATOR RUNTIME", 12, true,
            Color.rgb(104, 190, 255)));
        statusText = text("Checking PPSSPP native engine…", 14, false, Color.WHITE);
        addTopMargin(runtimeCard, statusText, 8);
        addTopMargin(runtimeCard, text(
            "The PPSSPP emulator, PSP CPU, graphics and audio run in the native engine.",
            12, false, Color.rgb(173, 184, 198)
        ), 8);

        addButton = new Button(this);
        addButton.setText("＋  ADD PSP GAME FILE");
        addButton.setAllCaps(false);
        addButton.setOnClickListener(v -> gamePicker.launch(new String[]{"*/*"}));
        addTopMargin(page, addButton, 18);

        addTopMargin(page, text("GAME LIBRARY", 16, true, Color.WHITE), 22);
        gameList = new LinearLayout(this);
        gameList.setOrientation(LinearLayout.VERTICAL);
        addTopMargin(page, gameList, 10);

        addTopMargin(page, text(
            "Import .iso or .cso files that you are legally entitled to use. " +
            "Conshite copies imported games into its private storage so Android file-picker permissions do not expire.",
            11, false, Color.rgb(150, 160, 174)
        ), 18);
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
        if (gameList != null) renderGames();
    }

    private void onGamePicked(Uri uri) {
        if (uri == null) return;

        final String name = queryDisplayName(uri);
        int dot = name.lastIndexOf('.');
        String extension = dot >= 0
            ? name.substring(dot + 1).toLowerCase(Locale.ROOT) : "";

        if (!extension.equals("iso") && !extension.equals("cso")) {
            Toast.makeText(this, "Unsupported file. Select a PSP .iso or .cso game.", Toast.LENGTH_LONG).show();
            return;
        }
        if (containsSource(uri.toString())) {
            Toast.makeText(this, "This game is already in your library.", Toast.LENGTH_SHORT).show();
            return;
        }

        addButton.setEnabled(false);
        addButton.setText("IMPORTING GAME…");
        Toast.makeText(this, "Copying " + name + " into Conshite storage…", Toast.LENGTH_LONG).show();

        importExecutor.execute(() -> {
            File copied = null;
            String failure = null;
            try {
                File gameDirectory = new File(getFilesDir(), "games");
                if (!gameDirectory.exists() && !gameDirectory.mkdirs()) {
                    throw new java.io.IOException("Could not create the games folder.");
                }

                String safeName = name.replaceAll("[^A-Za-z0-9._-]", "_");
                if (safeName.isEmpty()) safeName = "PSP_Game." + extension;
                copied = new File(gameDirectory, System.currentTimeMillis() + "_" + safeName);

                try (InputStream input = getContentResolver().openInputStream(uri);
                     FileOutputStream output = new FileOutputStream(copied)) {
                    if (input == null) throw new java.io.IOException("Android could not open this file.");
                    byte[] buffer = new byte[1024 * 1024];
                    int count;
                    while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
                    output.getFD().sync();
                }

                if (copied.length() == 0) throw new java.io.IOException("The selected file is empty.");
            } catch (Exception error) {
                if (copied != null) copied.delete();
                failure = error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
            }

            final File importedFile = copied;
            final String importError = failure;
            runOnUiThread(() -> {
                addButton.setEnabled(true);
                addButton.setText("＋  ADD PSP GAME FILE");
                if (isFinishing() || isDestroyed()) return;

                if (importError != null || importedFile == null) {
                    Toast.makeText(this, "Game import failed: " + importError, Toast.LENGTH_LONG).show();
                    return;
                }

                try {
                    JSONObject entry = new JSONObject();
                    entry.put("name", name);
                    entry.put("path", importedFile.getAbsolutePath());
                    entry.put("sourceUri", uri.toString());
                    entry.put("format", extension.toUpperCase(Locale.ROOT));
                    entry.put("size", importedFile.length());
                    games.put(entry);
                    saveGames();
                    renderGames();
                    Toast.makeText(this, "Game added: " + name, Toast.LENGTH_SHORT).show();
                } catch (Exception error) {
                    importedFile.delete();
                    Toast.makeText(this, "Could not save the game entry.", Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    private void renderGames() {
        gameList.removeAllViews();

        if (games.length() == 0) {
            LinearLayout emptyCard = card();
            emptyCard.addView(text("No games added yet", 16, true, Color.WHITE));
            addTopMargin(emptyCard, text(
                "Add a PSP ISO or CSO file. Its game tile will appear here.",
                12, false, Color.rgb(173, 184, 198)
            ), 8);
            gameList.addView(emptyCard);
            return;
        }

        for (int i = 0; i < games.length(); i++) {
            JSONObject game = games.optJSONObject(i);
            if (game == null) continue;

            String name = game.optString("name", "Unknown PSP game");
            String path = game.optString("path", "");
            String uriText = game.optString("uri", "");
            String format = game.optString("format", "ISO");

            LinearLayout gameCard = card();
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
            params.bottomMargin = dp(10);
            gameList.addView(gameCard, params);

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            gameCard.addView(row);

            TextView logo = text(gameInitials(name), 18, true, Color.WHITE);
            logo.setGravity(Gravity.CENTER);
            GradientDrawable logoBackground = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{Color.rgb(89, 75, 166), Color.rgb(40, 137, 170)}
            );
            logoBackground.setCornerRadius(dp(10));
            logo.setBackground(logoBackground);
            row.addView(logo, new LinearLayout.LayoutParams(dp(62), dp(72)));

            LinearLayout details = new LinearLayout(this);
            details.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams detailsParams = new LinearLayout.LayoutParams(0, -2, 1);
            detailsParams.leftMargin = dp(12);
            row.addView(details, detailsParams);
            details.addView(text(name, 15, true, Color.WHITE));
            addTopMargin(details, text(
                "PLAYSTATION PORTABLE  •  " + format,
                10, true, Color.rgb(126, 190, 225)
            ), 5);
            long bytes = game.optLong("size", 0);
            addTopMargin(details, text(
                bytes > 0 ? formatSize(bytes) : "Imported game",
                11, false, Color.rgb(173, 184, 198)
            ), 4);

            Button playButton = new Button(this);
            playButton.setText("▶  PLAY");
            playButton.setAllCaps(false);
            playButton.setEnabled(PpssppActivity.libraryLoaded);
            playButton.setOnClickListener(v -> launchGame(path, uriText));
            addTopMargin(gameCard, playButton, 12);
        }
    }

    private void launchGame(String path, String uriText) {
        try {
            Uri gameUri;
            if (!path.isEmpty()) {
                File file = new File(path);
                if (!file.isFile() || !file.canRead()) {
                    Toast.makeText(this, "Game file is missing. Import it again.", Toast.LENGTH_LONG).show();
                    return;
                }
                gameUri = Uri.fromFile(file);
            } else if (!uriText.isEmpty()) {
                gameUri = Uri.parse(uriText);
                getContentResolver().takePersistableUriPermission(
                    gameUri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                );
            } else {
                Toast.makeText(this, "Game file path is missing.", Toast.LENGTH_LONG).show();
                return;
            }

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setClass(this, PpssppActivity.class);
            intent.setDataAndType(gameUri, "*/*");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        } catch (Exception error) {
            String detail = error.getMessage() == null
                ? error.getClass().getSimpleName() : error.getMessage();
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
                    if (name != null && !name.isEmpty()) return name;
                }
            }
        } catch (Exception ignored) {
            // Use the final URI segment if the provider has no display name.
        } finally {
            if (cursor != null) cursor.close();
        }
        String last = uri.getLastPathSegment();
        return last == null ? "Unknown game.iso" : last;
    }

    private boolean containsSource(String source) {
        for (int i = 0; i < games.length(); i++) {
            JSONObject game = games.optJSONObject(i);
            if (game != null && (source.equals(game.optString("sourceUri"))
                || source.equals(game.optString("uri")))) return true;
        }
        return false;
    }

    private void loadGames() {
        SharedPreferences preferences = getSharedPreferences(PREFS, MODE_PRIVATE);
        try {
            JSONArray saved = new JSONArray(preferences.getString(GAMES_KEY, "[]"));
            for (int i = 0; i < saved.length(); i++) {
                JSONObject entry = saved.optJSONObject(i);
                if (entry == null) continue;
                String path = entry.optString("path", "");
                String uri = entry.optString("uri", "");
                if ((!path.isEmpty() && new File(path).isFile()) || !uri.isEmpty()) games.put(entry);
            }
        } catch (Exception ignored) {
            // A damaged library index must not prevent the app from opening.
        }
    }

    private void saveGames() {
        getSharedPreferences(PREFS, MODE_PRIVATE)
            .edit().putString(GAMES_KEY, games.toString()).apply();
    }

    private String gameInitials(String name) {
        String clean = name.replaceAll("\\.[^.]+$", "").trim();
        String[] words = clean.split("[\\s_-]+");
        if (words.length >= 2) {
            return (words[0].substring(0, 1) + words[1].substring(0, 1)).toUpperCase(Locale.ROOT);
        }
        return clean.substring(0, Math.min(2, clean.length())).toUpperCase(Locale.ROOT);
    }

    private String formatSize(long bytes) {
        if (bytes >= 1024L * 1024L * 1024L) return String.format(Locale.US, "%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        return String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0));
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
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        view.setGravity(Gravity.CENTER_VERTICAL);
        return view;
    }

    private void addTopMargin(LinearLayout parent, View child, int marginDp) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = dp(marginDp);
        parent.addView(child, params);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
