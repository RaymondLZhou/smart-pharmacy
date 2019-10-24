package ca.uwaterloo.arka.pharmacy.db;

import java.io.IOException;
import java.util.List;

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
    static UserDao newDao() {
        return new TestingUserDao();
    }
    
    /**
     * Create the supplied user record in the DB, or throw IOException if we can't.
     */
    void create(UserRecord user) throws IOException;
    
    /**
     * Retrieve a list of user records on this page, sorted alphabetically by name, or throw IOException if we can't.
     * (Tell me if Firebase doesn't support pagination and I'll change stuff.)
     */
    List<UserRecord> getAllSortedAlphabetically(int page) throws IOException;

    /**
     * Retrieve the number of pages of users that getAllSortedAlphabetically could possibly return, or throw
     * IOException if we can't. (If Firebase doesn't support this, tell me.) getAllSortedAlphabetically() should
     * return non-empty lists for all page inputs >= 0 and strictly less than getNumPages().
     */
    int getNumPages() throws IOException;
    
    /**
     * Retrieve a list of user records with the given name in arbitrary order, or throw IOException if we can't.
     */
    List<UserRecord> searchByName(String name) throws IOException;

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
