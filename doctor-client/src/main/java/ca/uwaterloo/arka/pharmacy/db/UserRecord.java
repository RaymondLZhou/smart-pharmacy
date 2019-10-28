package ca.uwaterloo.arka.pharmacy.db;

import javafx.beans.property.*;
import javafx.collections.FXCollections;

import java.util.List;
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
    
    public final int id;
    private StringProperty nameProperty = new SimpleStringProperty();
    private ListProperty<DoctorRecord> doctorsProperty = new SimpleListProperty<>();
    private ListProperty<PrescriptionRecord> prescriptionsProperty = new SimpleListProperty<>();
    private ObjectProperty<FaceFingerprintRecord> fingerprintProperty = new SimpleObjectProperty<>();
    
    public UserRecord(int id, String name, List<DoctorRecord> doctors, List<PrescriptionRecord> prescriptions,
                      FaceFingerprintRecord fingerprint) {
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
    
    public List<DoctorRecord> getDoctors() {
        return doctorsProperty.get();
    }
    
    public ListProperty<DoctorRecord> doctorsProperty() {
        return doctorsProperty;
    }
    
    public List<PrescriptionRecord> getPrescriptions() {
        return prescriptionsProperty.get();
    }
    
    public ListProperty<PrescriptionRecord> prescriptionsProperty() {
        return prescriptionsProperty;
    }
    
    public FaceFingerprintRecord getFingerprint() {
        return fingerprintProperty.get();
    }
    
    public void setFingerprint(FaceFingerprintRecord fingerprint) {
        if (fingerprint == null) throw new NullPointerException("cannot have null fingerprint");
        fingerprintProperty.set(fingerprint);
    }

    public ObjectProperty<FaceFingerprintRecord> fingerprintProperty() {
        return fingerprintProperty;
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
                && getPrescriptions().containsAll(user.getPrescriptions())
                && user.getPrescriptions().containsAll(getPrescriptions())
                && Objects.equals(getFingerprint(), user.getFingerprint());
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(getName(), getDoctors(), getPrescriptions(), getFingerprint());
    }
    
    @Override
    public String toString() {
        return "UserRecord{" +
                "name='" + getName() + '\'' +
                ", doctors=" + getDoctors().stream().map(DoctorRecord::toString).collect(Collectors.joining(", ")) +
                ", prescriptions=" + getPrescriptions().stream().map(PrescriptionRecord::toString)
                    .collect(Collectors.joining(", ")) +
                ", fingerprint=" + getFingerprint() +
                '}';
    }

    /**
     * A POJO representing the records in the "doctor" field in the DB documents.
     */
    public static class DoctorRecord {
        private String name;
        private int id;
        
        public DoctorRecord(String name, int id) {
            if (name == null) throw new NullPointerException("cannot have null doctor name");
            this.name = name;
            this.id = id;
        }
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            if (name == null) throw new NullPointerException("cannot have null doctor name");
            this.name = name;
        }
        
        public int getId() {
            return id;
        }
        
        public void setId(int id) {
            this.id = id;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            DoctorRecord that = (DoctorRecord) o;
            return id == that.id && name.equals(that.name);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(name, id);
        }
        
        @Override
        public String toString() {
            return "DoctorRecord{" +
                    "name='" + name + '\'' +
                    ", id=" + id +
                    '}';
        }
    }

    /**
     * A POJO representing each prescription in the "prescriptions" field in the DB docs. Immutable.
     */
    public static class PrescriptionRecord {
        public final int id;
        
        public PrescriptionRecord(int id) {
            this.id = id;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PrescriptionRecord that = (PrescriptionRecord) o;
            return id == that.id;
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(id);
        }
        
        @Override
        public String toString() {
            return "PrescriptionRecord{" +
                    "id=" + id +
                    '}';
        }
    }

    /**
     * A POJO representing the face ID fingerprint field in the DB doc.
     */
    public static class FaceFingerprintRecord {
        private String data;
        
        public FaceFingerprintRecord(String data) {
            if (data == null) throw new NullPointerException("cannot have null fingerprint data");
            this.data = data;
        }
        
        public String getData() {
            return data;
        }
        
        public void setData(String data) {
            if (data == null) throw new NullPointerException("cannot have null fingerprint data");
            this.data = data;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            FaceFingerprintRecord that = (FaceFingerprintRecord) o;
            return data.equals(that.data);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(data);
        }
        
        @Override
        public String toString() {
            return "FaceFingerprint{" +
                    "data='" + data + '\'' +
                    '}';
        }
    }
    
}
