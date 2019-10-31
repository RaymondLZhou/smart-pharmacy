package ca.uwaterloo.arka.pharmacy.db;

import com.google.firebase.database.Exclude;
import javafx.beans.property.ListProperty;
import javafx.beans.property.MapProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleMapProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * A POJO (Plain Old Java Object) representing a user record in the database.
 * Contains fields for the name, doctors, prescriptions, and fingerprint fields in each DB document.
 * Doctor, prescription, and fingerprint records have their own POJOs as well for ease of use (despite the fact that
 * the fingerprint is currently just a String).
 * Note that PrescriptionRecord is immutable - its ID field (the only field) cannot be changed. Everything else is
 * mutable, except the id field in UserRecord.
 */
public class UserRecord {
    
    public int id;
    private StringProperty nameProperty = new SimpleStringProperty();
    private ListProperty<String> doctorsProperty = new SimpleListProperty<>();
    private ListProperty<PrescriptionRecord> prescriptionsProperty = new SimpleListProperty<>();
    private StringProperty fingerprintProperty = new SimpleStringProperty();
    private MapProperty<String, TransactionRecord> transactionRecordProperty = new SimpleMapProperty<>();
    
    public UserRecord() {
        // No-arg constructor required for Firebase
        id = -1;
        setName("");
        setFingerprint("");
        prescriptionsProperty.setValue(FXCollections.observableList(new ArrayList<>()));
        doctorsProperty.setValue(FXCollections.observableList(new ArrayList<>()));
        transactionRecordProperty.setValue(FXCollections.observableHashMap());
    }
    
    public UserRecord(int id, String name, List<String> doctors, List<PrescriptionRecord> prescriptions,
                      String fingerprint) {
        if (name == null || doctors == null || prescriptions == null || fingerprint == null
                || doctors.contains(null) || prescriptions.contains(null)) {
            throw new NullPointerException("UserRecord cannot have any null fields");
        }
        this.id = id;
        nameProperty.set(name);
        doctorsProperty.set(FXCollections.observableList(doctors));
        prescriptionsProperty.set(FXCollections.observableList(prescriptions));
        fingerprintProperty.set(fingerprint);
    }
    
    public int getId() {
        return id;
    }

    public String getName() {
        return nameProperty.get();
    }
    
    public void setName(String name) {
        if (name == null) throw new NullPointerException("cannot have null name");
        nameProperty.set(name);
    }
    
    public StringProperty nameProperty() {
        return nameProperty;
    }
    
    public List<String> getDoctors() {
        return doctorsProperty.get();
    }

    public void setDoctors(List<String> doctors) {
        doctorsProperty.set(FXCollections.observableList(doctors));
    }

    public ListProperty<String> doctorsProperty() {
        return doctorsProperty;
    }
    
    @Exclude
    public List<PrescriptionRecord> getPrescriptionList() {
        return prescriptionsProperty.get();
    }
    
    public ListProperty<PrescriptionRecord> prescriptionsProperty() {
        return prescriptionsProperty;
    }
    
    /** To satisfy Firebase - prescriptions is technically a map */
    public Map<String, PrescriptionRecord> getPrescriptions() {
        Map<String, PrescriptionRecord> res = new HashMap<>();
        for (PrescriptionRecord prescription : getPrescriptionList()) {
            res.put("DIN_" + prescription.getDin(), prescription);
        }
        return res;
    }
    
    /** Also to satisfy Firebase */
    public void setPrescriptions(Map<String, PrescriptionRecord> map) {
        prescriptionsProperty.set(FXCollections.observableList(new ArrayList<>(map.values())));
    }
    
    public String getFingerprint() {
        return fingerprintProperty.get();
    }
    
    public void setFingerprint(String fingerprint) {
        if (fingerprint == null) throw new NullPointerException("cannot have null fingerprint");
        fingerprintProperty.set(fingerprint);
    }

    public StringProperty fingerprintProperty() {
        return fingerprintProperty;
    }
    
    // for firebase serialization - there's a record field in the DB
    public Map<String, TransactionRecord> getRecord() {
        return transactionRecordProperty.get();
    }
    
    public void setRecord(Map<String, TransactionRecord> record) {
        transactionRecordProperty.set(FXCollections.observableMap(record));
    }
    
    public MapProperty<String, TransactionRecord> transactionRecordProperty() {
        return transactionRecordProperty;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (!(obj instanceof UserRecord)) return false;
        UserRecord user = (UserRecord) obj;
        return getName().equals(user.getName())
                && getDoctors().containsAll(user.getDoctors())
                && user.getDoctors().containsAll(getDoctors())
                && getPrescriptionList().containsAll(user.getPrescriptionList())
                && user.getPrescriptionList().containsAll(getPrescriptionList())
                && Objects.equals(getFingerprint(), user.getFingerprint());
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(getName(), getDoctors(), getPrescriptionList(), getFingerprint());
    }
    
    @Override
    public String toString() {
        return "UserRecord{" +
                "id='" + id + '\'' +
                ", name='" + getName() + '\'' +
                ", doctors=" + String.join(", ", getDoctors()) +
                ", prescriptions=" + getPrescriptionList().stream().map(PrescriptionRecord::toString)
                    .collect(Collectors.joining(", ")) +
                ", fingerprint=" + getFingerprint() +
                '}';
    }
    
    /**
     * A POJO representing each prescription in the "prescriptions" field in the DB docs. Immutable.
     */
    public static class PrescriptionRecord {
        
        private int din;
        private String type;
        private long timestamp;
        private long expires;
        
        public PrescriptionRecord() {
            // no-arg constructor required for Firebase
            din = -1;
            type = "";
            timestamp = -1;
            expires = -1;
        }
        
        public PrescriptionRecord(int din) {
            this.din = din;
            type = "DIN #" + din; // TODO allow the user to choose this
            timestamp = System.currentTimeMillis();
            expires = timestamp + (7 * 24 * 60 * 60 * 1000); // expires in a week for now
        }
        
        public void setDin(int id) {
            din = id;
        }
        
        public int getDin() {
            return din;
        }
        
        public String getType() {
            return type;
        }
        
        public void setType(String type) {
            this.type = type;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
        
        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }
        
        public long getExpires() {
            return expires;
        }
        
        public void setExpires(long expires) {
            this.expires = expires;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PrescriptionRecord that = (PrescriptionRecord) o;
            return din == that.din &&
                    timestamp == that.timestamp &&
                    expires == that.expires &&
                    Objects.equals(type, that.type);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(din, type, timestamp, expires);
        }
        
        @Override
        public String toString() {
            return "PrescriptionRecord{" +
                    "din=" + din +
                    ", type='" + type + '\'' +
                    ", timestamp=" + timestamp +
                    ", expires=" + expires +
                    '}';
        }
        
    }
    
    // POJO to make Firebase happy, we don't use it but whatever we need it for Firebase
    public static class TransactionRecord {
        
        private List<Integer> dins;
        private long timestamp;
        
        public List<Integer> getDins() {
            return dins;
        }
        
        public void setDins(List<Integer> dins) {
            this.dins = dins;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
        
        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TransactionRecord that = (TransactionRecord) o;
            return timestamp == that.timestamp &&
                    Objects.equals(dins, that.dins);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(dins, timestamp);
        }
        
        @Override
        public String toString() {
            return "TransactionRecord{" +
                    "dins=" + dins +
                    ", timestamp=" + timestamp +
                    '}';
        }
        
    }
    
}
