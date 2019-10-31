package ca.uwaterloo.arka.pharmacy.db;

import java.io.IOException;
import java.util.function.Consumer;

/**
 * An interface which the GUI app will use to access the database. The "real" implementation of this class should
 * establish connections to the database in each of these methods and perform the appropriate operation.
 * Note: DAO stands for "database access object".
 */
public interface UserDao {
    
    /**
     * Return a concrete UserDao.
     */
    static UserDao newDao() {
        return new DbUserDao();
    }
    
    /**
     * Initialize the connection to the database, or throw IOException if we can't.
     */
    void initialize() throws IOException;
    
    /**
     * Create the supplied user record in the DB, or call the error callback with a message if we can't.
     * Call the callback if we can.
     */
    void create(UserRecord user, Runnable callback, Consumer<String> errorCb);
    
    /**
     * Retrieve a list of user records on this page, sorted alphabetically by name. Call the callback with each
     * record retrieved. If there's an error, call the error callback.
     */
    void getAllSortedAlphabetically(Consumer<UserRecord> callback, Consumer<String> errorCb);
    
    /**
     * Retrieve a list of user records with the given name in arbitrary order, calling the callback for each one.
     * If an error is encountered, call the error callback with details.
     */
    void searchByName(String name, Consumer<UserRecord> callback, Consumer<String> errorCb);
    
    /**
     * Update the user record on the DB with the supplied user record, or call the error callback with a message
     * if we can't. Call the callback if we did.
     */
    void update(UserRecord record, Runnable callback, Consumer<String> errorCb);
    
    /**
     * Delete the user record on the DB (found by ID), or call the error callback with a message if we can't.
     * Call the callback if we can.
     */
    void delete(UserRecord record, Runnable callback, Consumer<String> errorCb);
    
}
