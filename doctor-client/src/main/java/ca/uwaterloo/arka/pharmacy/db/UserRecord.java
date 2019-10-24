package ca.uwaterloo.arka.pharmacy.db;

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
    private String name;
    private List<DoctorRecord> doctors;
    private List<PrescriptionRecord> prescriptions;
    private FaceFingerprintRecord fingerprint;
    
    public UserRecord(int id, String name, List<DoctorRecord> doctors, List<PrescriptionRecord> prescriptions,
                      FaceFingerprintRecord fingerprint) {
        if (name == null || doctors == null || prescriptions == null || fingerprint == null
                || doctors.contains(null) || prescriptions.contains(null)) {
            throw new NullPointerException("UserRecord cannot have any null fields");
        }
        this.id = id;
        this.name = name;
        this.doctors = doctors;
        this.prescriptions = prescriptions;
        this.fingerprint = fingerprint;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        if (name == null) throw new NullPointerException("cannot have null name");
        this.name = name;
    }
    
    public List<DoctorRecord> getDoctors() {
        return doctors;
    }
    
    public List<PrescriptionRecord> getPrescriptions() {
        return prescriptions;
    }
    
    public FaceFingerprintRecord getFingerprint() {
        return fingerprint;
    }
    
    public void setFingerprint(FaceFingerprintRecord fingerprint) {
        if (fingerprint == null) throw new NullPointerException("cannot have null fingerprint");
        this.fingerprint = fingerprint;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (!(obj instanceof UserRecord)) return false;
        UserRecord user = (UserRecord) obj;
        return Objects.equals(name, user.name) && doctors.containsAll(user.doctors) && user.doctors.containsAll(doctors)
                && prescriptions.containsAll(user.prescriptions) && user.prescriptions.containsAll(prescriptions)
                && Objects.equals(fingerprint, user.fingerprint);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(name, doctors, prescriptions, fingerprint);
    }
    
    @Override
    public String toString() {
        return "UserRecord{" +
                "name='" + name + '\'' +
                ", doctors=" + doctors.stream().map(DoctorRecord::toString).collect(Collectors.joining(", ")) +
                ", prescriptions=" + prescriptions.stream().map(PrescriptionRecord::toString)
                    .collect(Collectors.joining(", ")) +
                ", fingerprint=" + fingerprint +
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
