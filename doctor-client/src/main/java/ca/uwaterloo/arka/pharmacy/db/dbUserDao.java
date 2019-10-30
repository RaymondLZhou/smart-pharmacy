package ca.uwaterloo.arka.pharmacy.db;

import java.io.*;
import java.util.*;
//Can't import this for some reason even though the central maven repo and dependicies are present in gradle.
//Because of this code cannot be tested until fixed.
import com.google.firebase.FirebaseApp;

class dbUserDao implements UserDao{
    public FirebaseApp defaultApp;
    public FirebaseDatabase database;
    public DatabaseReference ref;

    public dbUserDao() {
        FileInputStream serviceAccount = new FileInputStream("./serviceAccountKey.json");
        FirebaseOptions options = new FirebaseOptions.Builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .setDatabaseUrl("https://smart-pharmacy-f818a.firebaseio.com")
                .build();
        defaultApp = FirebaseApp.initializeApp(options);
        database = FirebaseDatabase.getInstance();
        ref = database.getReference("/arka/");
    }

    /**
     * Create the supplied user record in the DB, or throw IOException if we can't.
     */
    @Override
    public void create(UserRecord user) throws IOException{
        DatabaseReference usersRef = ref.child("users");
        Map <String, UserRecord> users = new HashMap<>();
        users.put(user.id, user);
        usersRef.setValueAsync(users);
        //Error Implementation can be done later.
    }

    /**
     * Retrieve a list of user records on this page, sorted alphabetically by name, or throw IOException if we can't.
     * (Tell me if Firebase doesn't support pagination and I'll change stuff.)
     */
    @Override
    public List<UserRecord> getAllSortedAlphabetically(int page) throws IOException{

    }

    /**
     * Retrieve the number of pages of users that getAllSortedAlphabetically could possibly return, or throw
     * IOException if we can't. (If Firebase doesn't support this, tell me.) getAllSortedAlphabetically() should
     * return non-empty lists for all page inputs >= 0 and strictly less than getNumPages().
     */
    @Override
    public int getNumPages() throws IOException{

    }

    /**
     * Retrieve a list of user records with the given name in arbitrary order, or throw IOException if we can't.
     */
    @Override
    public List<UserRecord> searchByName(String name) throws IOException{
        List<UserRecord> list = new ArrayList<>();
        ref.child("users").orderByChild("name").equalTo(name).addChildEventListener(new ChildEventListener(){
           @Override
           public void onChildAdded(DataSnapshot dataSnapshot, String prevChildKey){
                String id = dataSnapshot.getKey();
                list.add(new UserRecord(Integer.parseInt(id), id.child("doctors"), id.child("prescriptions"), id.child("fingerprint")));
           }

        });
        return list;
    }

    /**
     * Update the user record on the DB with the supplied user record, or throw IOException if we can't.
     * The record to be updated can, I think, be referenced by the id field in UserRecord.
     */
    @Override
    public void update(UserRecord record) throws IOException{
        //Overwrites current user by creating a new entry
        DatabaseReference usersRef = ref.child("users");
        Map <String, UserRecord> users = new HashMap<>();
        users.put(user.id, user);
        usersRef.setValueAsync(users);
        //Error Implementation can be done later.
    }

    /**
     * Delete the user record on the DB (found by ID), or throw IOException if we can't.
     */
    @Override
    public void delete(UserRecord record) throws IOException{
        DatabaseReference idRef = ref.child("users/" + record.id);
        idRef.remove();
    }
}
