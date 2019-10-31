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
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

class DbUserDao implements UserDao{
    
    private static FirebaseApp defaultApp = null;
    private static FirebaseDatabase database = null;
    
    private DatabaseReference ref;
    
    DbUserDao() throws IOException {
        if (defaultApp == null && database == null) {
            InputStream serviceAccount = getClass().getResourceAsStream(
                    "/ca/uwaterloo/arka/pharmacy/db/serviceAccountKey.json");
            if (serviceAccount == null) throw new IOException("No key file");

            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .setDatabaseUrl("https://smart-pharmacy-f818a.firebaseio.com")
                    .build();
            defaultApp = FirebaseApp.initializeApp(options);
            System.out.println("[DbUserDao] Initialized Firebase app of " + defaultApp.getName());
            database = FirebaseDatabase.getInstance();
        }
        
        ref = database.getReference("/arka/");
    }
    
    /**
     * Create the supplied user record in the DB.
     */
    @Override
    public void create(UserRecord user) {
        //DatabaseReference usersRef = ref.child("users");
        //Map<String, UserRecord> users = new HashMap<>();
        //users.put(Integer.toString(user.id), user);
        database.goOnline();
        DatabaseReference connectedRef = database.getReference(".info/connected");
        connectedRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                boolean connected = snapshot.getValue(Boolean.class);
                if (connected) {
                    System.out.println("connected");
                } else {
                    System.out.println("not connected");
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                System.out.println("cancelled");
            }
        });
        try {
            database.getReference("/arka/user/" + user.id).setValue(user, (error, ref1) -> {
                System.out.println("oh darn");
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Retrieve a list of user records on this page, sorted alphabetically by name. Call the callback with each
     * record retrieved. If there's an error, call the error callback.
     */
    @Override
    public void getAllSortedAlphabetically(Consumer<UserRecord> callback, Consumer<String> errorCb) {
        Query query = database.getReference("/arka/user").orderByChild("name");
        doSearch(query, callback, errorCb);
    }
    
    /**
     * Retrieve a list of user records with the given name in arbitrary order, calling callback for each one.
     */
    @Override
    public void searchByName(String name, Consumer<UserRecord> callback, Consumer<String> errorCb) {
        Query query = database.getReference("/arka/user").orderByChild("name").equalTo(name);
        doSearch(query, callback, errorCb);
    }
    
    private void doSearch(Query query, Consumer<UserRecord> callback, Consumer<String> errorCb) {
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                try {
                    System.out.println("Hello");
                    System.out.println(snapshot.getKey());
                    for (DataSnapshot record : snapshot.getChildren()) {
                        System.out.println(record.getValue(UserRecord.class));
                        Platform.runLater(() -> callback.accept(record.getValue(UserRecord.class)));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                errorCb.accept(error.getMessage());
            }
        });
    }
    
    /**
     * Update the user record on the DB with the supplied user record, or throw IOException if we can't.
     * The record to be updated can, I think, be referenced by the id field in UserRecord.
     */
    @Override
    public void update(UserRecord user) throws IOException {
        //Overwrites current user by creating a new entry
        database.goOnline();
        DatabaseReference usersRef = database.getReference("/arka/user");
        Map<String, UserRecord> users = new HashMap<>();
        users.put(Integer.toString(user.id), user);
        try {
            usersRef.setValue(users, (error, ref1) -> {
                System.out.println("updated"); 
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        // TODO Error Implementation can be done later.
    }
    
    /**
     * Delete the user record on the DB (found by ID).
     */
    @Override
    public void delete(UserRecord record) throws IOException {
        database.goOnline();
        database.getReference().child("arka").child("user").child(Integer.toString(record.id)).removeValue((error, ref1) -> {
            System.out.println("removed");
        });
        //DatabaseReference idRef = ref.child("user/" + record.id);
        //idRef.removeValueAsync();
        // TODO error implementation
    }
    
}
