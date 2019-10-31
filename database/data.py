import firebase_admin
from firebase_admin import credentials
from firebase_admin import db
import json

#Class for user entries
class User():
    def __init__(self, id, name, doctors, prescriptions, fingerprint, record):
        self._id = id
        self._name = name
        self._doctors = doctors
        self._prescription = prescriptions
        self._fingerprint = fingerprint
        self._record = record

#Class for Prescription items for the prescription attribute in user
class Prescription():
    def __init__(self, type, din, timestamp, expires):
        self._type = type
        self._din = din
        self._timestamp = timestamp
        self._expires = expires

#Class for Record items for the record attribute in user
class Record():
    def __init__(self, dins, timestamp):
        self._dins = dins
        self._timestamp = timestamp

#Class for connecting to the database. Multiple instances can be instantiated.
class Firebase_Server():
    def __init__(self):
        self._cred = credentials.Certificate(
            "../doctor-client/src/main/java/ca/uwaterloo/arka/pharmacy/db/serviceAccountKey.json")
        firebase_admin.initialize_app(self._cred, {
            'databaseURL': 'https://smart-pharmacy-f818a.firebaseio.com/'
        })

        self._ref = db.reference('/arka/')
        self._useref = db.reference('arka/user/')

    def createUser (self, user):
        self._useref.child(user._id).set({
            'name': user._name,
            'doctors': user._doctors,
            'prescriptions': {},
            'fingerprint': user._fingerprint,
            'record': {},
        })
        for pres in user._prescription:
            self._useref.child(user._id).child('prescriptions').child("DIN_" + pres._din).set({
                'type': pres._type,
                'din': int(pres._din),
                'timestamp': int(pres._timestamp),
                'expires': int(pres._expires),
            })
        for record in user._record:
            self._useref.child(user._id).child('record').push().set({
                'dins': record._dins,
                'timestamp': int(record._timestamp),
            })


    def createPrescriptions (self, id, prescriptions):
        self._useref.child(id).update({
            'prescriptions': {}
        })
        for pres in prescriptions:
            self._useref.child(id).child('prescriptions').child("DIN_" + pres._din).set({
                'type': pres._type,
                'din': int(pres._din),
                'timestamp': int(pres._timestamp),
                'expires': int(pres._expires),
            })

    def updatePrescriptions (self, id, prescriptions):
        for pres in prescriptions:
            self._useref.child(id).child('prescriptions').child("DIN_" + pres._din).set({
                'type': pres._type,
                'din': int(pres._din),
                'timestamp': int(pres._timestamp),
                'expires': int(pres._expires),
            })

    def getIDfromFingerprint (self, fingerprint):
        id = self._useref.order_by_child("fingerprint").equal_to(fingerprint).get()
        for key in id:
            return key

    def getUsers (self):
        snapshot = self._useref.get()
        return snapshot

    def deleteUser (self, id):
        db.reference('arka/user/' + id).delete()

    def getPrescriptions (self, id):
        prescriptions = self._useref.child(id).child('prescriptions').get()
        return prescriptions

    def updateRecords (self, id, records):
        for rec in records:
            self._useref.child(id).child('record').push().set({
                'dins': rec._dins,
                'timestamp': int(rec._timestamp),
            })

    def getUserInfo (self, id):
        userInfo = self._useref.child(id).get()
        return userInfo

#Sample Code
'''
app = Firebase_Server()
pres = Prescription("big", "111", "222", "333")
pres2 = Prescription("pharm", "222", "333", "444")
record = Record(["111","222"], "222")
record2 = Record(["222","333"], "444")
app.createUser(User("adlskfj", "name", ["doctor1", "doctor2"], [pres], "fingerprint", [record]))
#print(app.getIDfromFingerprint("fingerprint"))
app.updatePrescriptions("adlskfj", [pres2])
#app.deleteUser("adlskfj")
#print(app.getPrescriptions("adlskfj"))
#print(app.getUsers())
print(app.updateRecords("adlskfj", [record2]))
#print (app.getUserInfo("adlskfj"))
'''

