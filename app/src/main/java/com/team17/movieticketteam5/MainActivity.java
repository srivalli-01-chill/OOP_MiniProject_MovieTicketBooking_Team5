
package com.team17.movieticketteam5;

import android.app.Activity;
import android.os.Bundle;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.WriteBatch;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

public class MainActivity extends Activity {

    // -----------------------------
    // CONSTANTS AND FIREBASE
    // -----------------------------

    private static final long HOLD_DURATION = 180000L;

    private static final int DARK = Color.rgb(25, 30, 43);
    private static final int RED = Color.rgb(180, 34, 64);
    private static final int WHITE = Color.WHITE;
    private static final int GREEN = Color.rgb(34, 140, 92);
    private static final int ORANGE = Color.rgb(229, 155, 38);
    private static final int GRAY = Color.rgb(121, 129, 140);
    private static final int BLUE = Color.rgb(57, 100, 220);
    private static final int BACKGROUND = Color.rgb(246, 247, 250);

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private LinearLayout container;

    private final Handler handler =
            new Handler(Looper.getMainLooper());

    private Runnable refreshRunnable;

    private String currentPage = "";
    private String currentShowId = "";
    private String currentShowTitle = "";

    private boolean isAdmin = false;

    private final Set<String> selectedSeats =
            new LinkedHashSet<>();

    // -----------------------------
    // ACTIVITY STARTUP
    // -----------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (auth.getCurrentUser() == null) {
            showLogin();
        } else {
            showDashboard();
        }
    }

    // -----------------------------
    // COMMON UTILITIES
    // -----------------------------

    private String userId() {
        if (auth.getCurrentUser() == null) {
            throw new IllegalStateException(
                    "Please log in first");
        }
        return auth.getCurrentUser().getUid();
    }

    private int dp(int value) {
        return (int) (
                value *
                        getResources().getDisplayMetrics().density
        );
    }

    private void notifyUser(String message) {
        Toast.makeText(
                this,
                message,
                Toast.LENGTH_LONG
        ).show();
    }

    private void showError(Exception exception) {
        String message = exception == null
                ? "Something went wrong"
                : exception.getMessage();

        notifyUser(
                message == null
                        ? "Operation failed"
                        : message
        );
    }

    private String getString(
            DocumentSnapshot document,
            String field
    ) {
        String value = document.getString(field);
        return value == null ? "" : value;
    }

    private long getLong(
            DocumentSnapshot document,
            String field
    ) {
        Number value = (Number) document.get(field);
        return value == null ? 0 : value.longValue();
    }

    private long expiryMillis(DocumentSnapshot document) {
        Timestamp timestamp =
                document.getTimestamp("holdUntil");

        return timestamp == null
                ? 0L
                : timestamp.toDate().getTime();
    }

    private boolean isSeatHeld(DocumentSnapshot document) {
        return "HELD".equals(getString(document, "state"))
                && expiryMillis(document)
                > System.currentTimeMillis();
    }

    private boolean isOwnHold(DocumentSnapshot document) {
        return isSeatHeld(document)
                && userId().equals(
                getString(document, "heldBy")
        );
    }

    private void stopRefreshing() {
        if (refreshRunnable != null) {
            handler.removeCallbacks(refreshRunnable);
            refreshRunnable = null;
        }
    }

    // -----------------------------
    // USER INTERFACE HELPERS
    // -----------------------------

    private GradientDrawable rounded(int color) {
        GradientDrawable drawable =
                new GradientDrawable();

        drawable.setColor(color);
        drawable.setCornerRadius(dp(14));

        return drawable;
    }

    private void setupPage(String title) {

        stopRefreshing();

        currentPage = title;

        ScrollView scrollView =
                new ScrollView(this);

        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(BACKGROUND);

        container = new LinearLayout(this);
        container.setOrientation(
                LinearLayout.VERTICAL
        );

        container.setPadding(
                dp(20),
                dp(24),
                dp(20),
                dp(32)
        );

        scrollView.addView(container);
        setContentView(scrollView);

        addText(
                "MOVIETICKET  |  TEAM 5",
                16,
                RED,
                true
        );

        addText(title, 25, DARK, true);
    }

    private TextView addText(
            String text,
            int size,
            int color,
            boolean bold
    ) {

        TextView view = new TextView(this);

        view.setText(text);
        view.setTextSize(size);
        view.setTextColor(color);

        if (bold) {
            view.setTypeface(null, Typeface.BOLD);
        }

        view.setPadding(
                dp(3),
                dp(10),
                dp(3),
                dp(10)
        );

        container.addView(view);
        return view;
    }

    private EditText addInput(String hint) {
        return addInput(
                hint,
                InputType.TYPE_CLASS_TEXT
        );
    }

    private EditText addInput(
            String hint,
            int inputType
    ) {

        EditText input = new EditText(this);

        input.setHint(hint);
        input.setInputType(inputType);
        input.setTextSize(16);
        input.setSingleLine(true);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        -1,
                        dp(58)
                );

        params.bottomMargin = dp(7);

        container.addView(input, params);

        return input;
    }

    private String value(EditText input) {
        return input.getText()
                .toString()
                .trim();
    }

    private boolean validate(EditText... inputs) {

        boolean valid = true;

        for (EditText input : inputs) {

            if (value(input).isEmpty()) {
                input.setError("This field is required");
                valid = false;
            }
        }

        return valid;
    }

    private Button addButton(
            String text,
            Runnable action
    ) {

        Button button = new Button(this);

        button.setText(text);
        button.setTextSize(14);
        button.setAllCaps(false);
        button.setTextColor(WHITE);

        button.setBackgroundTintList(
                ColorStateList.valueOf(RED)
        );

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        -1,
                        dp(52)
                );

        params.topMargin = dp(9);

        container.addView(button, params);

        button.setOnClickListener(
                view -> action.run()
        );

        return button;
    }

    private void addCard(
            String heading,
            String description
    ) {

        LinearLayout card =
                new LinearLayout(this);

        card.setOrientation(
                LinearLayout.VERTICAL
        );

        card.setPadding(
                dp(15),
                dp(14),
                dp(15),
                dp(14)
        );

        card.setBackground(rounded(WHITE));
        card.setElevation(dp(2));

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        -1,
                        -2
                );

        params.topMargin = dp(12);

        container.addView(card, params);

        TextView title = new TextView(this);

        title.setText(heading);
        title.setTextSize(18);
        title.setTextColor(RED);
        title.setTypeface(null, Typeface.BOLD);

        card.addView(title);

        TextView details = new TextView(this);

        details.setText(description);
        details.setTextSize(14);
        details.setTextColor(DARK);

        details.setPadding(
                0,
                dp(8),
                0,
                0
        );

        card.addView(details);
    }

    // -----------------------------
    // MODULE 0: FIREBASE LOGIN
    // -----------------------------

    private void showLogin() {

        setupPage("Login / Registration");

        addText(
                "Sign in or create an account "
                        + "to book movie tickets.",
                15,
                DARK,
                false
        );

        EditText email = addInput(
                "Email address",
                InputType.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        );

        EditText password = addInput(
                "Password (at least 6 characters)",
                InputType.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_VARIATION_PASSWORD
        );

        addButton("Login", () -> {

            if (!validate(email, password)) {
                return;
            }

            auth.signInWithEmailAndPassword(
                    value(email),
                    value(password)
            ).addOnSuccessListener(result -> {
                showDashboard();
            }).addOnFailureListener(
                    this::showError
            );
        });

        addButton("Create new account", () -> {

            if (!validate(email, password)) {
                return;
            }

            if (value(password).length() < 6) {
                password.setError(
                        "Minimum 6 characters required"
                );
                return;
            }

            auth.createUserWithEmailAndPassword(
                    value(email),
                    value(password)
            ).addOnSuccessListener(result -> {
                notifyUser("Account created successfully");
                showDashboard();
            }).addOnFailureListener(
                    this::showError
            );
        });
    }

    // -----------------------------
    // MAIN DASHBOARD
    // -----------------------------

    private void showDashboard() {

        if (auth.getCurrentUser() == null) {
            showLogin();
            return;
        }

        setupPage("Movie Booking Dashboard");

        addText(
                "Welcome! Book movie tickets, "
                        + "manage reservations and "
                        + "view your booking history.",
                15,
                DARK,
                false
        );

        addButton(
                "Browse Movies and Shows",
                this::showMovies
        );

        addButton(
                "My Bookings",
                this::showBookings
        );

        addButton(
                "Show-wise Reports",
                this::showReports
        );

        addButton("Logout", () -> {
            auth.signOut();
            isAdmin = false;
            selectedSeats.clear();
            showLogin();
        });

        String checkingUser = userId();

        db.collection("admins")
                .document(checkingUser)
                .get()
                .addOnSuccessListener(document -> {

                    if (auth.getCurrentUser() == null
                            || !checkingUser.equals(userId())) {
                        return;
                    }

                    isAdmin = document.exists();

                    if (isAdmin
                            && "Movie Booking Dashboard"
                            .equals(currentPage)) {

                        addButton(
                                "ADMIN: Add Movie / Show",
                                this::showAdminScreen
                        );
                    }

                })
                .addOnFailureListener(this::showError);
    }

    // -----------------------------
    // MODULE 1: MOVIES AND SHOWS
    // -----------------------------

    private void showMovies() {

        setupPage("Available Movies");

        addText(
                "Select a movie and choose your seats.",
                15,
                DARK,
                false
        );

        db.collection("shows")
                .limit(100)
                .get()
                .addOnSuccessListener(query -> {

                    if (!"Available Movies".equals(currentPage)) {
                        return;
                    }

                    if (query.isEmpty()) {
                        addText(
                                "No movies available. "
                                        + "An administrator must "
                                        + "add the first show.",
                                15,
                                DARK,
                                false
                        );
                    }

                    for (DocumentSnapshot document
                            : query.getDocuments()) {

                        String id = document.getId();

                        String title =
                                getString(document, "title");

                        String theatre =
                                getString(document, "theatre");

                        String city =
                                getString(document, "city");

                        String timing =
                                getString(document, "startsAt");

                        addCard(
                                title,
                                "Theatre: " + theatre
                                        + "\nCity: " + city
                                        + "\nShow: " + timing
                        );

                        addButton(
                                "Book Seats — " + title,
                                () -> openSeatMap(id, title)
                        );
                    }

                })
                .addOnFailureListener(this::showError);

        addButton(
                "Back to Dashboard",
                this::showDashboard
        );
    }

    // -----------------------------
    // ADMIN: CREATE MOVIE SHOW
    // -----------------------------

    private void showAdminScreen() {

        if (!isAdmin) {
            notifyUser("Administrator access required");
            return;
        }

        setupPage("Admin: Add Movie Show");

        EditText movie = addInput("Movie name");

        EditText theatre = addInput(
                "Theatre and screen name"
        );

        EditText city = addInput("City");

        EditText showTime = addInput(
                "Show time (YYYY-MM-DD HH:MM)"
        );

        EditText regular = addInput(
                "Regular seat price",
                InputType.TYPE_CLASS_NUMBER
        );

        EditText premium = addInput(
                "Premium seat price",
                InputType.TYPE_CLASS_NUMBER
        );

        EditText recliner = addInput(
                "Recliner seat price",
                InputType.TYPE_CLASS_NUMBER
        );

        addButton("Publish Movie and 36 Seats", () -> {

            if (!validate(
                    movie,
                    theatre,
                    city,
                    showTime,
                    regular,
                    premium,
                    recliner
            )) {
                return;
            }

            int regularPrice;
            int premiumPrice;
            int reclinerPrice;

            try {
                regularPrice =
                        Integer.parseInt(value(regular));

                premiumPrice =
                        Integer.parseInt(value(premium));

                reclinerPrice =
                        Integer.parseInt(value(recliner));

                if (regularPrice <= 0
                        || premiumPrice <= 0
                        || reclinerPrice <= 0) {

                    throw new NumberFormatException();
                }

            } catch (NumberFormatException exception) {

                notifyUser(
                        "Prices must be positive whole numbers"
                );
                return;
            }

            Date parsedShowTime;

            try {
                SimpleDateFormat formatter =
                        new SimpleDateFormat(
                                "yyyy-MM-dd HH:mm",
                                Locale.US
                        );

                formatter.setLenient(false);

                parsedShowTime =
                        formatter.parse(value(showTime));

                if (parsedShowTime == null
                        || parsedShowTime.getTime()
                        <= System.currentTimeMillis()) {

                    notifyUser(
                            "Enter a future show date and time"
                    );
                    return;
                }

            } catch (Exception exception) {

                showTime.setError(
                        "Use YYYY-MM-DD HH:MM"
                );
                return;
            }

            DocumentReference showReference =
                    db.collection("shows").document();

            Map<String, Object> showData =
                    new HashMap<>();

            showData.put("title", value(movie));
            showData.put("theatre", value(theatre));
            showData.put("city", value(city));
            showData.put("startsAt", value(showTime));
            showData.put("createdBy", userId());

            WriteBatch batch = db.batch();

            batch.set(showReference, showData);

            for (int row = 0; row < 6; row++) {

                for (int column = 1;
                     column <= 6;
                     column++) {

                    String seatId =
                            String.valueOf(
                                    (char) ('A' + row)
                            ) + column;

                    String category;
                    int price;

                    if (row < 2) {
                        category = "REGULAR";
                        price = regularPrice;
                    } else if (row < 4) {
                        category = "PREMIUM";
                        price = premiumPrice;
                    } else {
                        category = "RECLINER";
                        price = reclinerPrice;
                    }

                    Map<String, Object> seatData =
                            new HashMap<>();

                    seatData.put(
                            "state", "AVAILABLE"
                    );

                    seatData.put(
                            "category", category
                    );

                    seatData.put("price", price);
                    seatData.put("heldBy", "");
                    seatData.put("bookingId", "");

                    seatData.put(
                            "holdUntil",
                            new Timestamp(new Date(0))
                    );

                    batch.set(
                            showReference
                                    .collection("seats")
                                    .document(seatId),
                            seatData
                    );
                }
            }

            batch.commit()
                    .addOnSuccessListener(unused -> {
                        notifyUser(
                                "Movie and seats published"
                        );
                        showMovies();
                    })
                    .addOnFailureListener(
                            this::showError
                    );
        });

        addButton(
                "Back to Dashboard",
                this::showDashboard
        );
    }

    // -----------------------------
    // MODULE 2: SEAT MAP
    // -----------------------------

    private void openSeatMap(
            String showId,
            String title
    ) {

        currentShowId = showId;
        currentShowTitle = title;

        selectedSeats.clear();

        displaySeatMap();
    }

    private DocumentReference getSeatReference(
            String showId,
            String seatId
    ) {

        return db.collection("shows")
                .document(showId)
                .collection("seats")
                .document(seatId);
    }

    private void displaySeatMap() {

        final String showId = currentShowId;

        setupPage(
                "Select Seats: " + currentShowTitle
        );

        addText(
                "GREEN: Available\n"
                        + "ORANGE: Held by another user\n"
                        + "BLUE: Held by you\n"
                        + "GRAY: Already booked",
                14,
                DARK,
                false
        );

        addText(
                "SCREEN THIS WAY",
                16,
                RED,
                true
        );

        db.collection("shows")
                .document(showId)
                .collection("seats")
                .get()
                .addOnSuccessListener(query -> {

                    if (!showId.equals(currentShowId)
                            || !currentPage.startsWith(
                            "Select Seats:")) {
                        return;
                    }

                    List<DocumentSnapshot> seats =
                            new ArrayList<>(
                                    query.getDocuments()
                            );

                    Collections.sort(
                            seats,
                            (a, b) -> a.getId()
                                    .compareTo(b.getId())
                    );

                    GridLayout grid =
                            new GridLayout(this);

                    grid.setColumnCount(6);

                    container.addView(grid);

                    int seatsInRow = 0;

                    for (DocumentSnapshot seat : seats) {

                        String seatId = seat.getId();
                        String state =
                                getString(seat, "state");

                        boolean booked =
                                "BOOKED".equals(state);

                        boolean held = isSeatHeld(seat);
                        boolean mine = isOwnHold(seat);

                        boolean available =
                                !booked && !held;

                        if (!mine) {
                            selectedSeats.remove(seatId);
                        }

                        Button seatButton =
                                new Button(this);

                        seatButton.setText(seatId);
                        seatButton.setAllCaps(false);
                        seatButton.setTextSize(11);

                        seatButton.setTextColor(WHITE);

                        int seatColor;

                        if (booked) {
                            seatColor = GRAY;
                        } else if (mine) {
                            seatColor = BLUE;
                        } else if (held) {
                            seatColor = ORANGE;
                        } else {
                            seatColor = GREEN;
                        }

                        seatButton.setBackgroundTintList(
                                ColorStateList.valueOf(
                                        seatColor
                                )
                        );

                        GridLayout.LayoutParams params =
                                new GridLayout.LayoutParams();

                        params.width = dp(48);
                        params.height = dp(48);

                        params.setMargins(
                                dp(1),
                                dp(4),
                                dp(1),
                                dp(4)
                        );

                        grid.addView(seatButton, params);

                        seatButton.setEnabled(
                                available || mine
                        );

                        seatButton.setOnClickListener(view -> {

                            if (mine) {
                                releaseSeat(seatId);
                            } else {
                                holdSeat(seatId);
                            }
                        });

                        seatsInRow++;
                    }

                    if (seatsInRow == 0) {
                        addText(
                                "No seat records found.",
                                15,
                                DARK,
                                false
                        );
                    }

                    addText(
                            "Selected seats: " + selectedSeats,
                            15,
                            DARK,
                            true
                    );

                    addButton(
                            "Confirm Booking",
                            this::confirmBooking
                    );

                    addButton(
                            "Refresh Seat Map",
                            this::displaySeatMap
                    );

                    addButton(
                            "Release Selected Holds",
                            this::releaseSelectedHolds
                    );

                    addButton(
                            "Back to Movies",
                            this::showMovies
                    );

                    refreshRunnable = () -> {

                        if (showId.equals(currentShowId)
                                && currentPage.startsWith(
                                "Select Seats:")) {

                            displaySeatMap();
                        }
                    };

                    handler.postDelayed(
                            refreshRunnable,
                            15000
                    );

                })
                .addOnFailureListener(
                        this::showError
                );
    }

    // -----------------------------
    // MODULE 3: SEAT HOLD
    // -----------------------------

    private void holdSeat(String seatId) {

        if (selectedSeats.size() >= 6) {
            notifyUser(
                    "Maximum six seats per booking"
            );
            return;
        }

        final String showId = currentShowId;
        final String owner = userId();

        DocumentReference seatReference =
                getSeatReference(showId, seatId);

        db.runTransaction(transaction -> {

            DocumentSnapshot seat =
                    transaction.get(seatReference);

            if (!seat.exists()) {
                throw new IllegalStateException(
                        "Seat not found"
                );
            }

            String state =
                    getString(seat, "state");

            long expiry = expiryMillis(seat);

            boolean activeHold =
                    "HELD".equals(state)
                            && expiry >
                            System.currentTimeMillis();

            boolean heldByAnother =
                    activeHold
                            && !owner.equals(
                            getString(seat, "heldBy")
                    );

            if ("BOOKED".equals(state)
                    || heldByAnother) {

                throw new IllegalStateException(
                        "Seat is unavailable"
                );
            }

            Map<String, Object> updates =
                    new HashMap<>();

            updates.put("state", "HELD");
            updates.put("heldBy", owner);

            updates.put(
                    "holdUntil",
                    new Timestamp(
                            new Date(
                                    System.currentTimeMillis()
                                            + HOLD_DURATION
                            )
                    )
            );

            transaction.update(
                    seatReference,
                    updates
            );

            return true;

        }).addOnSuccessListener(result -> {

            if (showId.equals(currentShowId)) {
                selectedSeats.add(seatId);
                displaySeatMap();
            }

        }).addOnFailureListener(exception -> {
            showError(exception);
            displaySeatMap();
        });
    }

    // -----------------------------
    // RELEASE SEAT
    // -----------------------------

    private void releaseSeat(String seatId) {

        final String showId = currentShowId;
        final String owner = userId();

        DocumentReference seatReference =
                getSeatReference(showId, seatId);

        db.runTransaction(transaction -> {

            DocumentSnapshot seat =
                    transaction.get(seatReference);

            if ("HELD".equals(
                    getString(seat, "state"))
                    && owner.equals(
                    getString(seat, "heldBy"))) {

                Map<String, Object> updates =
                        new HashMap<>();

                updates.put(
                        "state", "AVAILABLE"
                );

                updates.put("heldBy", "");

                updates.put(
                        "holdUntil",
                        new Timestamp(new Date(0))
                );

                transaction.update(
                        seatReference,
                        updates
                );
            }

            return true;

        }).addOnSuccessListener(result -> {

            selectedSeats.remove(seatId);
            displaySeatMap();

        }).addOnFailureListener(
                this::showError
        );
    }

    private void releaseSelectedHolds() {

        List<String> seats =
                new ArrayList<>(selectedSeats);

        if (seats.isEmpty()) {
            notifyUser("No seats selected");
            return;
        }

        // Each release is a separate Firestore transaction.
        for (String seatId : seats) {
            releaseSeat(seatId);
        }
    }

    // -----------------------------
    // MODULE 4: BOOKING
    // -----------------------------

    private void confirmBooking() {

        if (selectedSeats.isEmpty()) {
            notifyUser("Select at least one seat");
            return;
        }

        final String showId = currentShowId;
        final String movieTitle = currentShowTitle;
        final String owner = userId();

        final List<String> seatIds =
                new ArrayList<>(selectedSeats);

        if (seatIds.size() > 6) {
            notifyUser("Maximum six seats allowed");
            return;
        }

        DocumentReference bookingReference =
                db.collection("bookings").document();

        db.runTransaction(transaction -> {

            List<DocumentReference> references =
                    new ArrayList<>();

            List<DocumentSnapshot> snapshots =
                    new ArrayList<>();

            int totalPrice = 0;

            // Read every seat before writing anything.
            for (String seatId : seatIds) {

                DocumentReference seatRef =
                        getSeatReference(showId, seatId);

                references.add(seatRef);
                snapshots.add(
                        transaction.get(seatRef)
                );
            }

            for (DocumentSnapshot seat : snapshots) {

                String state =
                        getString(seat, "state");

                String heldBy =
                        getString(seat, "heldBy");

                long expiry = expiryMillis(seat);

                if (!"HELD".equals(state)
                        || !owner.equals(heldBy)
                        || expiry <=
                        System.currentTimeMillis()) {

                    throw new IllegalStateException(
                            "Seat " + seat.getId()
                                    + " is no longer held by you"
                    );
                }

                Number price =
                        (Number) seat.get("price");

                if (price == null) {
                    throw new IllegalStateException(
                            "Seat price missing"
                    );
                }

                totalPrice += price.intValue();
            }

            Map<String, Object> booking =
                    new HashMap<>();

            booking.put("uid", owner);
            booking.put("showId", showId);
            booking.put("title", movieTitle);
            booking.put("seatIds", seatIds);
            booking.put("total", totalPrice);
            booking.put("status", "ACTIVE");

            booking.put(
                    "createdAt",
                    FieldValue.serverTimestamp()
            );

            transaction.set(
                    bookingReference,
                    booking
            );

            for (DocumentReference seatRef : references) {

                Map<String, Object> updates =
                        new HashMap<>();

                updates.put("state", "BOOKED");

                updates.put(
                        "bookingId",
                        bookingReference.getId()
                );

                updates.put("heldBy", "");

                updates.put(
                        "holdUntil",
                        new Timestamp(new Date(0))
                );

                transaction.update(
                        seatRef,
                        updates
                );
            }

            return totalPrice;

        }).addOnSuccessListener(totalPrice -> {

            selectedSeats.clear();

            setupPage("Booking Successful");

            addCard(
                    "Ticket Confirmed",
                    "Movie: " + movieTitle
                            + "\nBooking ID: "
                            + bookingReference.getId()
                            + "\nSeats: " + seatIds
                            + "\nTotal: ₹" + totalPrice
                            + "\nStatus: ACTIVE"
                            + "\nPayment: Simulated checkout"
            );

            addButton(
                    "View My Bookings",
                    this::showBookings
            );

            addButton(
                    "Back to Dashboard",
                    this::showDashboard
            );

        }).addOnFailureListener(exception -> {

            showError(exception);
            displaySeatMap();
        });
    }

    // -----------------------------
    // MY BOOKINGS
    // -----------------------------

    private void showBookings() {

        setupPage("My Bookings");

        String owner = userId();

        db.collection("bookings")
                .whereEqualTo("uid", owner)
                .limit(100)
                .get()
                .addOnSuccessListener(query -> {

                    if (!"My Bookings".equals(currentPage)) {
                        return;
                    }

                    if (query.isEmpty()) {
                        addText(
                                "You have no bookings yet.",
                                15,
                                DARK,
                                false
                        );
                    }

                    for (DocumentSnapshot booking
                            : query.getDocuments()) {

                        String bookingId = booking.getId();

                        String title =
                                getString(booking, "title");

                        String status =
                                getString(booking, "status");

                        long total =
                                getLong(booking, "total");

                        Object seats =
                                booking.get("seatIds");

                        addCard(
                                title,
                                "Booking ID: " + bookingId
                                        + "\nSeats: " + seats
                                        + "\nTotal: ₹" + total
                                        + "\nStatus: " + status
                        );

                        if ("ACTIVE".equals(status)) {

                            addButton(
                                    "Cancel Booking: "
                                            + bookingId.substring(
                                            0,
                                            Math.min(
                                                    bookingId.length(),
                                                    8
                                            )
                                    ),
                                    () -> cancelBooking(
                                            bookingId
                                    )
                            );
                        }
                    }

                })
                .addOnFailureListener(
                        this::showError
                );

        addButton(
                "Back to Dashboard",
                this::showDashboard
        );
    }

    // -----------------------------
    // CANCELLATION
    // -----------------------------

    private void cancelBooking(String bookingId) {

        final String owner = userId();

        DocumentReference bookingReference =
                db.collection("bookings")
                        .document(bookingId);

        db.runTransaction(transaction -> {

            DocumentSnapshot booking =
                    transaction.get(bookingReference);

            if (!booking.exists()) {
                throw new IllegalStateException(
                        "Booking not found"
                );
            }

            if (!owner.equals(
                    getString(booking, "uid"))) {

                throw new IllegalStateException(
                        "This is not your booking"
                );
            }

            if (!"ACTIVE".equals(
                    getString(booking, "status"))) {

                throw new IllegalStateException(
                        "Booking is already cancelled"
                );
            }

            String showId =
                    getString(booking, "showId");

            @SuppressWarnings("unchecked")
            List<String> seatIds =
                    (List<String>)
                            booking.get("seatIds");

            if (seatIds == null) {
                throw new IllegalStateException(
                        "Booking has no seat information"
                );
            }

            List<DocumentReference> references =
                    new ArrayList<>();

            for (String seatId : seatIds) {

                DocumentReference seatRef =
                        getSeatReference(
                                showId,
                                seatId
                        );

                DocumentSnapshot seat =
                        transaction.get(seatRef);

                if (!"BOOKED".equals(
                        getString(seat, "state"))
                        || !bookingId.equals(
                        getString(seat, "bookingId"))) {

                    throw new IllegalStateException(
                            "Seat state does not match booking"
                    );
                }

                references.add(seatRef);
            }

            transaction.update(
                    bookingReference,
                    "status",
                    "CANCELLED"
            );

            for (DocumentReference seatRef : references) {

                Map<String, Object> updates =
                        new HashMap<>();

                updates.put(
                        "state",
                        "AVAILABLE"
                );

                updates.put("bookingId", "");

                transaction.update(
                        seatRef,
                        updates
                );
            }

            return true;

        }).addOnSuccessListener(result -> {

            notifyUser(
                    "Booking cancelled and seats released"
            );

            showBookings();

        }).addOnFailureListener(
                this::showError
        );
    }

    // -----------------------------
    // MODULE 5: BOOKING REPORTS
    // -----------------------------

    private void showReports() {

        setupPage("Show-wise Reports");

        db.collection("shows")
                .limit(100)
                .get()
                .addOnSuccessListener(shows -> {

                    db.collection("bookings")
                            .limit(1000)
                            .get()
                            .addOnSuccessListener(bookings -> {

                                if (!"Show-wise Reports"
                                        .equals(currentPage)) {
                                    return;
                                }

                                for (DocumentSnapshot show
                                        : shows.getDocuments()) {

                                    String showId =
                                            show.getId();

                                    int soldSeats = 0;
                                    long revenue = 0;

                                    for (DocumentSnapshot booking
                                            : bookings.getDocuments()) {

                                        if (!showId.equals(
                                                getString(
                                                        booking,
                                                        "showId"
                                                )
                                        )) {
                                            continue;
                                        }

                                        if (!"ACTIVE".equals(
                                                getString(
                                                        booking,
                                                        "status"
                                                )
                                        )) {
                                            continue;
                                        }

                                        Object seats =
                                                booking.get(
                                                        "seatIds"
                                                );

                                        if (seats instanceof List) {
                                            soldSeats +=
                                                    ((List<?>) seats)
                                                            .size();
                                        }

                                        revenue += getLong(
                                                booking,
                                                "total"
                                        );
                                    }

                                    addCard(
                                            getString(
                                                    show,
                                                    "title"
                                            ),
                                            "Theatre: "
                                                    + getString(
                                                    show,
                                                    "theatre"
                                            )
                                                    + "\nTickets sold: "
                                                    + soldSeats
                                                    + "\nBooking value: ₹"
                                                    + revenue
                                    );
                                }

                                addText(
                                        "Reports use the first "
                                                + "100 shows and 1000 "
                                                + "bookings. Larger "
                                                + "datasets require "
                                                + "pagination.",
                                        12,
                                        GRAY,
                                        false
                                );

                            })
                            .addOnFailureListener(
                                    this::showError
                            );

                })
                .addOnFailureListener(
                        this::showError
                );

        addButton(
                "Back to Dashboard",
                this::showDashboard
        );
    }

    // -----------------------------
    // BACK BUTTON
    // -----------------------------

    @Override
    public void onBackPressed() {

        if ("Movie Booking Dashboard"
                .equals(currentPage)) {

            super.onBackPressed();

        } else {

            selectedSeats.clear();
            showDashboard();
        }
    }

    // -----------------------------
    // CLEANUP
    // -----------------------------

    @Override
    protected void onDestroy() {
        stopRefreshing();
        super.onDestroy();
    }
}
