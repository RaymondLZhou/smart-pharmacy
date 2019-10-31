package ca.uwaterloo.arka.pharmacy.db;

import java.io.IOException;
import java.util.function.Consumer;

/**
 * An interface which the GUI app will use to access the database. The "real" implementation of this class should
 * establish connections to the database in each of these methods and perform the appropriate operation.
 * Alex: when you're connecting this app to the real database, implement this interface in something like UserDaoImpl
 * and then change newDao() to return an instance of your implementation.
 * Note: DAO stands for "database access object".
 */
public interface UserDao {

    /**
     * Return a concrete UserDao. Change this to your real implementation class when you're done it and the
     * app should use it.
     */
    static UserDao newDao() throws IOException {
        return new DbUserDao();
    }
    
    /**
     * Create the supplied user record in the DB, or throw IOException if we can't.
     */
    void create(UserRecord user) throws IOException;
    
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
     * Update the user record on the DB with the supplied user record, or throw IOException if we can't.
     * The record to be updated can, I think, be referenced by the id field in UserRecord.
     */
    void update(UserRecord record) throws IOException;

    /**
     * Delete the user record on the DB (found by ID), or throw IOException if we can't.
     */
    void delete(UserRecord record) throws IOException;
    
}
