package com.akaswising.conshite;

import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.Inflater;

public final class MainActivity extends AppCompatActivity {
    private static final String PREFS = "conshite_library";
    private static final String GAMES_KEY = "games";

    private final JSONArray games = new JSONArray();
    private final ExecutorService importExecutor = Executors.newSingleThreadExecutor();

    private LinearLayout carousel;
    private LinearLayout hero;
    private TextView heroTitle;
    private TextView heroSubtitle;
    private ImageView heroCover;
    private Button heroPlay;
    private Button detailsButton;
    private Button coverButton;
    private Button addGameButton;
    private TextView runtimeText;
    private boolean pythonRuntimeReady;

    private int selectedGame = -1;
    private ActivityResultLauncher<String[]> gamePicker;
    private ActivityResultLauncher<String[]> coverPicker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadGames();
        initializePythonRuntime();

        gamePicker = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(),
            this::onGamePicked
        );
        coverPicker = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(),
            this::onCoverPicked
        );

        buildDashboard();
        renderDashboard();
        handleIncomingGameIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIncomingGameIntent(intent);
    }

    private void handleIncomingGameIntent(Intent intent) {
        if (intent == null || !Intent.ACTION_VIEW.equals(intent.getAction())) return;
        Uri uri = intent.getData();
        if (uri == null) {
            Toast.makeText(this, "No game file was received.", Toast.LENGTH_LONG).show();
            return;
        }
        onGamePicked(uri);
    }

    private void initializePythonRuntime() {
        try {
            if (!Python.isStarted()) {
                Python.start(new AndroidPlatform(getApplicationContext()));
            }
            Python.getInstance().getModule("conshite_runtime").callAttr("runtime_info");
            pythonRuntimeReady = true;
        } catch (Exception error) {
            pythonRuntimeReady = false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        importExecutor.shutdown();
    }

    private void buildDashboard() {
        ScrollView outer = new ScrollView(this);
        outer.setFillViewport(true);
        outer.setBackgroundColor(Color.rgb(4, 9, 16));

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(18), dp(12), dp(18), dp(24));
        outer.addView(page);

        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        page.addView(topBar);

        TextView brand = text("☰   CONSHITE   /   DASHBOARD", 14, true, Color.WHITE);
        topBar.addView(brand, new LinearLayout.LayoutParams(0, dp(44), 1));

        Button searchButton = smallButton("⌕");
        searchButton.setOnClickListener(v ->
            Toast.makeText(this, "Game search: use the game names in your library.", Toast.LENGTH_SHORT).show()
        );
        topBar.addView(searchButton);

        Button menuButton = smallButton("⋮");
        menuButton.setOnClickListener(v -> showMenu());
        topBar.addView(menuButton);

        runtimeText = text(
            PpssppActivity.libraryLoaded ? "● PSP RUNTIME READY" : "● PSP RUNTIME NOT LOADED",
            10, true,
            PpssppActivity.libraryLoaded ? Color.rgb(112, 220, 157) : Color.rgb(255, 166, 112)
        );
        addTopMargin(page, runtimeText, 4);

        hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setPadding(dp(18), dp(16), dp(18), dp(16));
        GradientDrawable heroBg = new GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            new int[]{Color.rgb(13, 39, 49), Color.rgb(17, 25, 45), Color.rgb(7, 12, 22)}
        );
        heroBg.setCornerRadius(dp(18));
        hero.setBackground(heroBg);
        addTopMargin(page, hero, 12);

        LinearLayout heroRow = new LinearLayout(this);
        heroRow.setOrientation(LinearLayout.HORIZONTAL);
        heroRow.setGravity(Gravity.CENTER_VERTICAL);
        hero.addView(heroRow);

        heroCover = new ImageView(this);
        heroCover.setScaleType(ImageView.ScaleType.CENTER_CROP);
        heroCover.setBackground(roundBackground(Color.rgb(28, 48, 67), 14));
        heroRow.addView(heroCover, new LinearLayout.LayoutParams(dp(112), dp(112)));

        LinearLayout heroText = new LinearLayout(this);
        heroText.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams heroTextParams = new LinearLayout.LayoutParams(0, -2, 1);
        heroTextParams.leftMargin = dp(14);
        heroRow.addView(heroText, heroTextParams);

        heroTitle = text("Your game library", 22, true, Color.WHITE);
        heroText.addView(heroTitle);
        heroSubtitle = text("Add a PSP ISO or CSO to get started.", 12, false,
            Color.rgb(188, 202, 218));
        addTopMargin(heroText, heroSubtitle, 7);

        LinearLayout actionRow = new LinearLayout(this);
        actionRow.setOrientation(LinearLayout.HORIZONTAL);
        actionRow.setGravity(Gravity.CENTER_VERTICAL);
        addTopMargin(hero, actionRow, 14);

        heroPlay = actionButton("▶  PLAY");
        heroPlay.setOnClickListener(v -> playSelectedGame());
        actionRow.addView(heroPlay, new LinearLayout.LayoutParams(0, dp(44), 1));

        detailsButton = actionButton("VIEW DETAILS");
        detailsButton.setOnClickListener(v -> showSelectedDetails());
        LinearLayout.LayoutParams detailsParams = new LinearLayout.LayoutParams(0, dp(44), 1);
        detailsParams.leftMargin = dp(8);
        actionRow.addView(detailsButton, detailsParams);

        coverButton = actionButton("SET COVER");
        coverButton.setOnClickListener(v -> chooseCover());
        LinearLayout.LayoutParams coverParams = new LinearLayout.LayoutParams(0, dp(44), 1);
        coverParams.leftMargin = dp(8);
        actionRow.addView(coverButton, coverParams);

        LinearLayout libraryHeader = new LinearLayout(this);
        libraryHeader.setOrientation(LinearLayout.HORIZONTAL);
        libraryHeader.setGravity(Gravity.CENTER_VERTICAL);
        addTopMargin(page, libraryHeader, 22);

        TextView libraryTitle = text("MY GAMES", 16, true, Color.WHITE);
        libraryHeader.addView(libraryTitle, new LinearLayout.LayoutParams(0, dp(42), 1));

        addGameButton = actionButton("+  ADD GAME");
        addGameButton.setOnClickListener(v -> gamePicker.launch(new String[]{"*/*"}));
        libraryHeader.addView(addGameButton);

        HorizontalScrollView horizontal = new HorizontalScrollView(this);
        horizontal.setHorizontalScrollBarEnabled(false);
        horizontal.setFillViewport(false);
        addTopMargin(page, horizontal, 8);
        carousel = new LinearLayout(this);
        carousel.setOrientation(LinearLayout.HORIZONTAL);
        carousel.setPadding(0, dp(2), dp(6), dp(10));
        horizontal.addView(carousel);

        TextView note = text(
            "Imported games are copied into Conshite's private storage. Android's original file-picker permission is not needed again.",
            11, false, Color.rgb(135, 151, 170)
        );
        addTopMargin(page, note, 12);

        setContentView(outer);
    }

    private void renderDashboard() {
        if (carousel == null) return;
        carousel.removeAllViews();
        boolean hasSelectedGame = selectedGame >= 0 && selectedGame < games.length();
        boolean runtimeReady = PpssppActivity.libraryLoaded && pythonRuntimeReady;
        runtimeText.setText(runtimeReady
            ? "● PYTHON CONTROL + PSP CORE READY"
            : (pythonRuntimeReady ? "● PSP CORE NOT LOADED" : "● PYTHON CONTROLLER NOT READY"));
        runtimeText.setTextColor(runtimeReady
            ? Color.rgb(112, 220, 157) : Color.rgb(255, 166, 112));
        heroPlay.setVisibility(hasSelectedGame ? View.VISIBLE : View.GONE);
        heroPlay.setEnabled(runtimeReady && hasSelectedGame);
        detailsButton.setVisibility(hasSelectedGame ? View.VISIBLE : View.GONE);
        coverButton.setVisibility(hasSelectedGame ? View.VISIBLE : View.GONE);

        if (games.length() == 0) {
            heroTitle.setText("Your game library");
            heroSubtitle.setText("Add a PSP ISO or CSO to get started.");
            heroCover.setImageDrawable(null);
            heroCover.setBackground(roundBackground(Color.rgb(28, 48, 67), 14));
            TextView empty = text("No games imported yet. Tap + ADD GAME.", 13, false,
                Color.rgb(180, 196, 214));
            empty.setGravity(Gravity.CENTER);
            carousel.addView(empty, new LinearLayout.LayoutParams(dp(280), dp(112)));
            return;
        }

        if (!hasSelectedGame) {
            heroTitle.setText("Select a game");
            heroSubtitle.setText("Choose a title below. Its PLAY button will appear here.");
            heroCover.setImageDrawable(null);
            heroCover.setBackground(roundBackground(Color.rgb(28, 48, 67), 14));
        } else {
            JSONObject selected = games.optJSONObject(selectedGame);
            if (selected != null) {
                String name = selected.optString("name", "Unknown PSP game");
                heroTitle.setText(name);
                heroSubtitle.setText("PLAYSTATION PORTABLE  •  " +
                    selected.optString("format", "ISO") + "  •  " +
                    formatSize(selected.optLong("size", 0)));
                setCoverOnView(heroCover, selected.optString("coverPath", ""), name, 112, 112);
            }
        }

        for (int i = 0; i < games.length(); i++) {
            JSONObject game = games.optJSONObject(i);
            if (game == null) continue;
            final int index = i;
            String name = game.optString("name", "PSP Game");

            LinearLayout tile = new LinearLayout(this);
            tile.setOrientation(LinearLayout.VERTICAL);
            tile.setGravity(Gravity.CENTER_HORIZONTAL);
            tile.setPadding(dp(5), dp(5), dp(5), dp(7));
            tile.setBackground(roundBackground(
                i == selectedGame ? Color.rgb(35, 76, 91) : Color.rgb(13, 22, 34), 12
            ));
            LinearLayout.LayoutParams tileParams =
                new LinearLayout.LayoutParams(dp(116), dp(134));
            tileParams.rightMargin = dp(9);
            carousel.addView(tile, tileParams);

            ImageView cover = new ImageView(this);
            cover.setScaleType(ImageView.ScaleType.CENTER_CROP);
            tile.addView(cover, new LinearLayout.LayoutParams(dp(104), dp(92)));
            setCoverOnView(cover, game.optString("coverPath", ""), name, 104, 92);

            TextView label = text(name, 10, true, Color.WHITE);
            label.setGravity(Gravity.CENTER);
            label.setMaxLines(2);
            LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(-1, 0, 1);
            labelParams.topMargin = dp(5);
            tile.addView(label, labelParams);

            tile.setOnClickListener(v -> {
                selectedGame = index;
                renderDashboard();
            });
            tile.setOnLongClickListener(v -> {
                selectedGame = index;
                showSelectedDetails();
                return true;
            });
        }
    }

    private void onGamePicked(Uri uri) {
        if (uri == null) return;
        String name = queryDisplayName(uri);
        int dot = name.lastIndexOf('.');
        String extension = dot >= 0
            ? name.substring(dot + 1).toLowerCase(Locale.ROOT) : "";
        if (!extension.equals("iso") && !extension.equals("cso")) {
            Toast.makeText(this, "Choose a PSP .iso or .cso file.", Toast.LENGTH_LONG).show();
            return;
        }
        if (containsSource(uri.toString())) {
            Toast.makeText(this, "This game is already in your library.", Toast.LENGTH_SHORT).show();
            return;
        }

        addGameButton.setEnabled(false);
        addGameButton.setText("IMPORTING…");
        Toast.makeText(this, "Importing game into Conshite storage…", Toast.LENGTH_SHORT).show();

        importExecutor.execute(() -> {
            File imported = null;
            String errorMessage = null;
            String coverPath = "";
            try {
                File directory = new File(getFilesDir(), "games");
                if (!directory.exists() && !directory.mkdirs())
                    throw new java.io.IOException("Could not create the private game folder.");
                String safeName = name.replaceAll("[^A-Za-z0-9._-]", "_");
                if (safeName.isEmpty()) safeName = "PSP_Game." + extension;
                imported = new File(directory, System.currentTimeMillis() + "_" + safeName);
                copyUriToFile(uri, imported);

                // PSP UMD images usually contain PSP_GAME/ICON0.PNG.
                // Extract it for the library tile when the image is present.
                byte[] embeddedIcon = PspIconExtractor.extract(imported);
                if (embeddedIcon != null && BitmapFactory.decodeByteArray(
                    embeddedIcon, 0, embeddedIcon.length) != null) {
                    File covers = new File(getFilesDir(), "covers");
                    if (!covers.exists()) covers.mkdirs();
                    File iconFile = new File(covers, "icon_" + System.currentTimeMillis() + ".png");
                    try (FileOutputStream out = new FileOutputStream(iconFile)) {
                        out.write(embeddedIcon);
                        out.getFD().sync();
                    }
                    coverPath = iconFile.getAbsolutePath();
                }
            } catch (Exception error) {
                if (imported != null) imported.delete();
                errorMessage = error.getMessage() == null
                    ? error.getClass().getSimpleName() : error.getMessage();
            }

            final File finalImported = imported;
            final String finalCover = coverPath;
            final String finalError = errorMessage;
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                addGameButton.setEnabled(true);
                addGameButton.setText("+  ADD GAME");
                if (finalError != null || finalImported == null) {
                    Toast.makeText(this, "Game import failed: " + finalError, Toast.LENGTH_LONG).show();
                    return;
                }
                try {
                    JSONObject entry = new JSONObject();
                    entry.put("name", name);
                    entry.put("path", finalImported.getAbsolutePath());
                    entry.put("sourceUri", uri.toString());
                    entry.put("format", extension.toUpperCase(Locale.ROOT));
                    entry.put("size", finalImported.length());
                    entry.put("coverPath", finalCover);
                    games.put(entry);
                    selectedGame = games.length() - 1;
                    saveGames();
                    renderDashboard();
                    Toast.makeText(this,
                        finalCover.isEmpty()
                            ? "Game added. Use SET COVER to choose its artwork."
                            : "Game added with its embedded PSP icon.",
                        Toast.LENGTH_LONG).show();
                } catch (Exception error) {
                    finalImported.delete();
                    Toast.makeText(this, "Could not save the game entry.", Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    private void onCoverPicked(Uri uri) {
        if (uri == null || selectedGame < 0 || selectedGame >= games.length()) return;
        importExecutor.execute(() -> {
            File savedCover = null;
            String failure = null;
            try {
                File directory = new File(getFilesDir(), "covers");
                if (!directory.exists() && !directory.mkdirs())
                    throw new java.io.IOException("Could not create the covers folder.");
                savedCover = new File(directory, "cover_" + System.currentTimeMillis() + ".img");
                copyUriToFile(uri, savedCover);
                BitmapFactory.Options opts = new BitmapFactory.Options();
                opts.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(savedCover.getAbsolutePath(), opts);
                if (opts.outWidth <= 0 || opts.outHeight <= 0)
                    throw new java.io.IOException("Selected file is not a valid image.");
            } catch (Exception error) {
                if (savedCover != null) savedCover.delete();
                failure = error.getMessage() == null ? "Invalid image" : error.getMessage();
            }

            final File finalCover = savedCover;
            final String finalFailure = failure;
            final int gameIndex = selectedGame;
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                if (finalFailure != null || finalCover == null) {
                    Toast.makeText(this, "Cover import failed: " + finalFailure, Toast.LENGTH_LONG).show();
                    return;
                }
                JSONObject game = games.optJSONObject(gameIndex);
                if (game == null) {
                    finalCover.delete();
                    return;
                }
                try {
                    game.put("coverPath", finalCover.getAbsolutePath());
                    saveGames();
                    renderDashboard();
                    Toast.makeText(this, "Game cover saved.", Toast.LENGTH_SHORT).show();
                } catch (Exception error) {
                    finalCover.delete();
                    Toast.makeText(this, "Could not save this cover.", Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    private void chooseCover() {
        if (selectedGame < 0 || selectedGame >= games.length()) {
            Toast.makeText(this, "Select a game first.", Toast.LENGTH_SHORT).show();
            return;
        }
        coverPicker.launch(new String[]{"image/*"});
    }

    private void playSelectedGame() {
        if (selectedGame < 0 || selectedGame >= games.length()) {
            Toast.makeText(this, "Select a game first.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!pythonRuntimeReady || !PpssppActivity.libraryLoaded) {
            Toast.makeText(this, "Python controller or PSP core is not ready.", Toast.LENGTH_LONG).show();
            return;
        }
        JSONObject game = games.optJSONObject(selectedGame);
        if (game == null) return;
        String path = game.optString("path", "");
        String oldUri = game.optString("uri", "");
        try {
            Uri gameUri;
            if (!path.isEmpty()) {
                String launchJson = Python.getInstance()
                    .getModule("conshite_runtime")
                    .callAttr("prepare_game", path).toString();
                JSONObject launchData = new JSONObject(launchJson);
                File file = new File(launchData.getString("path"));
                if (!file.isFile() || !file.canRead()) {
                    Toast.makeText(this, "Python runtime could not read the game file.", Toast.LENGTH_LONG).show();
                    return;
                }
                gameUri = Uri.fromFile(file);
            } else if (!oldUri.isEmpty()) {
                gameUri = Uri.parse(oldUri);
            } else {
                Toast.makeText(this, "Game file path is missing.", Toast.LENGTH_LONG).show();
                return;
            }

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setClass(this, PpssppActivity.class);
            intent.setDataAndType(gameUri, "application/octet-stream");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        } catch (Exception error) {
            String detail = error.getMessage() == null
                ? error.getClass().getSimpleName() : error.getMessage();
            Toast.makeText(this, "Could not open game: " + detail, Toast.LENGTH_LONG).show();
        }
    }

    private void showSelectedDetails() {
        JSONObject game = games.optJSONObject(selectedGame);
        if (game == null) return;
        String name = game.optString("name", "Unknown game");
        String details = name + "\n\nFormat: " + game.optString("format", "ISO")
            + "\nSize: " + formatSize(game.optLong("size", 0))
            + "\nStorage: Conshite private storage";
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Game details")
            .setMessage(details)
            .setPositiveButton("PLAY", (dialog, which) -> playSelectedGame())
            .setNeutralButton("CHANGE COVER", (dialog, which) -> chooseCover())
            .setNegativeButton("CLOSE", null)
            .show();
    }

    private void showMenu() {
        String[] options = {"Add PSP game", "Set selected game cover", "Game library info"};
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Conshite menu")
            .setItems(options, (dialog, which) -> {
                if (which == 0) gamePicker.launch(new String[]{"*/*"});
                else if (which == 1) chooseCover();
                else new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Game library")
                    .setMessage(games.length() + " game(s) imported. Game files and cover images are stored inside Conshite.")
                    .setPositiveButton("OK", null).show();
            }).show();
    }

    private void setCoverOnView(ImageView view, String path, String name, int width, int height) {
        view.setBackground(roundBackground(Color.rgb(25, 47, 65), 10));
        Bitmap bitmap = null;
        if (path != null && !path.isEmpty()) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 2;
            bitmap = BitmapFactory.decodeFile(path, options);
        }
        if (bitmap != null) {
            view.setImageBitmap(bitmap);
            view.setScaleType(ImageView.ScaleType.CENTER_CROP);
        } else {
            view.setImageDrawable(null);
            view.setBackground(coverPlaceholder(name));
        }
    }

    private GradientDrawable coverPlaceholder(String name) {
        int hash = name == null ? 0 : name.hashCode();
        int[][] palettes = {
            {Color.rgb(15, 93, 114), Color.rgb(31, 45, 96)},
            {Color.rgb(108, 55, 119), Color.rgb(37, 44, 83)},
            {Color.rgb(151, 83, 36), Color.rgb(71, 40, 72)},
            {Color.rgb(30, 111, 82), Color.rgb(22, 49, 73)}
        };
        int[] colors = palettes[Math.abs(hash % palettes.length)];
        GradientDrawable d = new GradientDrawable(GradientDrawable.Orientation.TL_BR, colors);
        d.setCornerRadius(dp(10));
        return d;
    }

    private void copyUriToFile(Uri uri, File destination) throws Exception {
        try (InputStream input = getContentResolver().openInputStream(uri);
             FileOutputStream output = new FileOutputStream(destination)) {
            if (input == null) throw new java.io.IOException("Android could not read the selected file.");
            byte[] buffer = new byte[1024 * 1024];
            int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            output.getFD().sync();
        }
        if (destination.length() == 0)
            throw new java.io.IOException("The selected file is empty.");
    }

    private String queryDisplayName(Uri uri) {
        Cursor cursor = null;
        try {
            cursor = getContentResolver().query(uri,
                new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int column = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (column >= 0) {
                    String result = cursor.getString(column);
                    if (result != null && !result.isEmpty()) return result;
                }
            }
        } catch (Exception ignored) {
        } finally {
            if (cursor != null) cursor.close();
        }
        String last = uri.getLastPathSegment();
        return last == null ? "PSP_Game.iso" : last;
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
                String legacyUri = entry.optString("uri", "");
                if ((!path.isEmpty() && new File(path).isFile()) || !legacyUri.isEmpty()) {
                    games.put(entry);
                }
            }
        } catch (Exception ignored) {
        }
    }

    private void saveGames() {
        getSharedPreferences(PREFS, MODE_PRIVATE)
            .edit().putString(GAMES_KEY, games.toString()).apply();
    }

    private Button actionButton(String label) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextSize(11);
        button.setAllCaps(false);
        button.setTextColor(Color.WHITE);
        button.setPadding(dp(10), 0, dp(10), 0);
        button.setBackground(roundBackground(Color.rgb(28, 70, 86), 10));
        return button;
    }

    private Button smallButton(String label) {
        Button button = actionButton(label);
        button.setMinWidth(dp(42));
        return button;
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

    private GradientDrawable roundBackground(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        return d;
    }

    private void addTopMargin(LinearLayout parent, View child, int marginDp) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = dp(marginDp);
        parent.addView(child, params);
    }

    private String formatSize(long bytes) {
        if (bytes <= 0) return "Unknown size";
        if (bytes >= 1024L * 1024L * 1024L)
            return String.format(Locale.US, "%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        return String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0));
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    // Small ISO9660 / CSO reader used only to extract PSP_GAME/ICON0.PNG for the cover tile.
    private static final class PspIconExtractor {
        private static final int SECTOR = 2048;

        static byte[] extract(File image) {
            try (ImageReader reader = new ImageReader(image)) {
                byte[] pvd = reader.readAt(16L * SECTOR, SECTOR);
                if (pvd == null || pvd.length < 190 || (pvd[0] & 0xff) != 1
                    || !new String(pvd, 1, 5, "US-ASCII").equals("CD001")) return null;
                Record root = parseRecord(pvd, 156);
                if (root == null) return null;
                Record pspGame = findInDirectory(reader, root, "PSP_GAME");
                if (pspGame == null) return null;
                Record icon = findInDirectory(reader, pspGame, "ICON0.PNG");
                if (icon == null || icon.directory || icon.length <= 0 || icon.length > 2 * 1024 * 1024)
                    return null;
                return reader.readAt((long) icon.extent * SECTOR, icon.length);
            } catch (Exception ignored) {
                return null;
            }
        }

        private static Record findInDirectory(ImageReader reader, Record directory, String wanted)
            throws Exception {
            long base = (long) directory.extent * SECTOR;
            int position = 0;
            while (position < directory.length) {
                byte[] first = reader.readAt(base + position, 1);
                if (first == null || first.length == 0) break;
                int recordLength = first[0] & 0xff;
                if (recordLength == 0) {
                    position = ((position / SECTOR) + 1) * SECTOR;
                    continue;
                }
                byte[] record = reader.readAt(base + position, recordLength);
                if (record == null || record.length < 34) break;
                Record parsed = parseRecord(record, 0);
                if (parsed != null && parsed.name != null
                    && parsed.name.equalsIgnoreCase(wanted)) return parsed;
                position += recordLength;
            }
            return null;
        }

        private static Record parseRecord(byte[] bytes, int offset) {
            if (offset < 0 || bytes.length < offset + 34) return null;
            int length = bytes[offset] & 0xff;
            if (length < 34 || bytes.length < offset + length) return null;
            int extent = intLE(bytes, offset + 2);
            int dataLength = intLE(bytes, offset + 10);
            int flags = bytes[offset + 25] & 0xff;
            int nameLength = bytes[offset + 32] & 0xff;
            if (nameLength <= 0 || offset + 33 + nameLength > bytes.length) return null;
            String name = new String(bytes, offset + 33, nameLength, java.nio.charset.StandardCharsets.US_ASCII);
            if (name.endsWith(";1")) name = name.substring(0, name.length() - 2);
            if (nameLength == 1 && (bytes[offset + 33] == 0 || bytes[offset + 33] == 1)) name = "";
            return new Record(extent, dataLength, (flags & 2) != 0, name);
        }

        private static int intLE(byte[] b, int p) {
            return (b[p] & 255) | ((b[p + 1] & 255) << 8)
                | ((b[p + 2] & 255) << 16) | ((b[p + 3] & 255) << 24);
        }

        private static final class Record {
            final int extent, length;
            final boolean directory;
            final String name;
            Record(int e, int l, boolean d, String n) {
                extent = e; length = l; directory = d; name = n;
            }
        }

        private static final class ImageReader implements AutoCloseable {
            private final RandomAccessFile file;
            private final boolean cso;
            private final long csoIndexStart;
            private final int blockSize;
            private final int align;
            private final int totalBlocks;
            private int cachedBlock = -1;
            private byte[] cachedData;

            ImageReader(File source) throws Exception {
                file = new RandomAccessFile(source, "r");
                byte[] header = new byte[24];
                file.seek(0);
                file.readFully(header);
                cso = header[0] == 'C' && header[1] == 'I' && header[2] == 'S' && header[3] == 'O';
                if (!cso) {
                    csoIndexStart = 0; blockSize = SECTOR; align = 0; totalBlocks = 0;
                } else {
                    int headerSize = intLE(header, 4);
                    long totalBytes = longLE(header, 8);
                    blockSize = intLE(header, 16);
                    align = header[21] & 0xff;
                    if (headerSize < 24 || blockSize <= 0 || blockSize > 1024 * 1024)
                        throw new java.io.IOException("Invalid CSO header");
                    csoIndexStart = headerSize;
                    totalBlocks = (int)((totalBytes + blockSize - 1) / blockSize);
                }
            }

            byte[] readAt(long offset, int length) throws Exception {
                if (length < 0 || length > 4 * 1024 * 1024) return null;
                if (!cso) {
                    if (offset < 0 || offset + length > file.length()) return null;
                    byte[] out = new byte[length];
                    file.seek(offset); file.readFully(out);
                    return out;
                }
                byte[] out = new byte[length];
                int written = 0;
                while (written < length) {
                    long absolute = offset + written;
                    int block = (int)(absolute / blockSize);
                    int inside = (int)(absolute % blockSize);
                    if (block >= totalBlocks) return null;
                    byte[] data = readCsoBlock(block);
                    if (data == null || inside >= data.length) return null;
                    int count = Math.min(length - written, data.length - inside);
                    System.arraycopy(data, inside, out, written, count);
                    written += count;
                }
                return out;
            }

            private byte[] readCsoBlock(int block) throws Exception {
                if (cachedBlock == block) return cachedData;
                long tablePos = csoIndexStart + (long)block * 4;
                file.seek(tablePos);
                int current = readIntLE(file);
                int next = readIntLE(file);
                boolean plain = (current & 0x80000000) != 0;
                long start = ((long)(current & 0x7fffffff)) << align;
                long end = ((long)(next & 0x7fffffff)) << align;
                int compressedLength = (int)(end - start);
                if (compressedLength <= 0 || compressedLength > blockSize + 65536)
                    throw new java.io.IOException("Invalid CSO block index");
                byte[] result = new byte[blockSize];
                file.seek(start);
                if (plain) {
                    file.readFully(result);
                } else {
                    byte[] compressed = new byte[compressedLength];
                    file.readFully(compressed);
                    Inflater inflater = new Inflater(true);
                    try {
                        inflater.setInput(compressed);
                        int count = inflater.inflate(result);
                        if (count <= 0) throw new java.io.IOException("Unable to decompress CSO block");
                    } finally {
                        inflater.end();
                    }
                }
                cachedBlock = block;
                cachedData = result;
                return result;
            }

            private static int readIntLE(RandomAccessFile f) throws Exception {
                int a = f.read(), b = f.read(), c = f.read(), d = f.read();
                if (d < 0) throw new java.io.EOFException();
                return a | (b << 8) | (c << 16) | (d << 24);
            }

            private static long longLE(byte[] b, int p) {
                long v = 0;
                for (int i = 7; i >= 0; i--) v = (v << 8) | (b[p + i] & 255L);
                return v;
            }

            @Override public void close() throws Exception { file.close(); }
        }
    }
}
