package ca.uwaterloo.arka.pharmacy.db;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A testing implementation of UserDao. Used only for testing purposes before the DB is complete. Just implements
 * the various methods in a non-persistent list.
 */
class TestingUserDao implements UserDao {
    
    private static final int PAGE_SIZE = 5;
    private List<UserRecord> records = new ArrayList<>();
    
    @Override
    public void create(UserRecord user) {
        if (user == null) throw new NullPointerException("cannot have null user");
        records.add(user);
    }
    
    @Override
    public List<UserRecord> getAllSortedAlphabetically(int page) {
        List<UserRecord> res = new ArrayList<>();
        for (int i = page*PAGE_SIZE; i < Math.min((page+1)*PAGE_SIZE, records.size()); i++) {
            res.add(records.get(i));
        }
        return res;
    }
    
    @Override
    public int getNumPages() {
        return (records.size() - 1) / PAGE_SIZE + 1;
    }
    
    @Override
    public List<UserRecord> searchByName(String name) {
        return records.stream().filter(record -> record.getName().equals(name)).collect(Collectors.toList());
    }
    
    @Override
    public void update(UserRecord record) {
        for (int i = 0; i < records.size(); i++) {
            if (records.get(i).id == record.id) {
                records.set(i, record);
                break;
            }
        }
    }
    
    @Override
    public void delete(UserRecord record) {
        for (int i = 0; i < records.size(); i++) {
            if (records.get(i).id == record.id) {
                records.remove(i);
                break;
            }
        }
    }
    
}