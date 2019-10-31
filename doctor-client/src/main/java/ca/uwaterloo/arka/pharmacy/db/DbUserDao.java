package ca.uwaterloo.arka.pharmacy.db;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import javafx.application.Platform;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Consumer;

class DbUserDao implements UserDao {
    
    public void initialize() throws IOException {
        InputStream serviceAccount = DbUserDao.class.getResourceAsStream(
                "/ca/uwaterloo/arka/pharmacy/db/serviceAccountKey.json");
        if (serviceAccount == null) throw new IOException("No key file");
        
        FirebaseOptions options = new FirebaseOptions.Builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .setDatabaseUrl("https://smart-pharmacy-f818a.firebaseio.com")
                .build();
        FirebaseApp.initializeApp(options);
        System.out.println("[DbUserDao] Initialized Firebase");
    }
    
    /**
     * Create the supplied user record in the DB.
     */
    @Override
    public void create(UserRecord user, Runnable callback, Consumer<String> errorCb) {
        FirebaseDatabase.getInstance().goOnline();
        FirebaseDatabase.getInstance().getReference("/arka/user/" + user.id).setValue(user, (error, ref) -> {
            if (error == null) {
                System.out.println("[DbUserDao] Successfully created user " + user.id);
                Platform.runLater(callback);
            } else {
                Platform.runLater(() -> errorCb.accept(error.getMessage()));
            }
        });
    }
    
    /**
     * Retrieve a list of user records on this page, sorted alphabetically by name. Call the callback with each
     * record retrieved. If there's an error, call the error callback.
     */
    @Override
    public void getAllSortedAlphabetically(Consumer<UserRecord> callback, Consumer<String> errorCb) {
        Query query = FirebaseDatabase.getInstance().getReference("/arka/user").orderByChild("name");
        doSearch(query, callback, errorCb);
    }
    
    /**
     * Retrieve a list of user records with the given name in arbitrary order, calling callback for each one.
     */
    @Override
    public void searchByName(String name, Consumer<UserRecord> callback, Consumer<String> errorCb) {
        // the startAt/endAt trick is to get all users whose names start with name
        // unicode #ffff is the 'last' character so all names starting with name will be between name and name + that
        Query query = FirebaseDatabase.getInstance().getReference("/arka/user").orderByChild("name")
                .startAt(name).endAt(name + "\uffff");
        doSearch(query, callback, errorCb);
    }
    
    private void doSearch(Query query, Consumer<UserRecord> callback, Consumer<String> errorCb) {
        FirebaseDatabase.getInstance().goOnline();
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                try {
                    System.out.println("[DbUserDao] Retrieved from database:");
                    for (DataSnapshot record : snapshot.getChildren()) {
                        UserRecord userRecord = record.getValue(UserRecord.class);
                        System.out.println(userRecord);
                        Platform.runLater(() -> callback.accept(userRecord));
                    }
                } catch (Exception e) {
                    // for not silencing errors
                    e.printStackTrace();
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Platform.runLater(() -> errorCb.accept(error.getMessage()));
            }
        });
    }
    
    /**
     * Update the user record on the DB with the supplied user record, or call the error callback.
     */
    @Override
    public void update(UserRecord user, Runnable callback, Consumer<String> errorCb) {
        //Overwrites current user by creating a new entry
        FirebaseDatabase.getInstance().goOnline();
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("/arka/user/" + user.id);
        usersRef.setValue(user, (error, ref) -> {
            if (error == null) {
                System.out.println("Successfully updated user " + user.id);
                Platform.runLater(callback);
            } else {
                Platform.runLater(() -> errorCb.accept(error.getMessage()));
            }
        });
    }
    
    /**
     * Delete the user record on the DB (found by ID), or call the error callback.
     */
    @Override
    public void delete(UserRecord record, Runnable callback, Consumer<String> errorCb) {
        FirebaseDatabase.getInstance().goOnline();
        FirebaseDatabase.getInstance().getReference().child("arka").child("user").child(Integer.toString(record.id))
                .removeValue((error, ref) -> {
                    if (error == null) {
                        System.out.println("Successfully deleted user " + record.id);
                        Platform.runLater(callback);
                    } else {
                        Platform.runLater(() -> errorCb.accept(error.getMessage()));
                    }
                });
    }
    
}
