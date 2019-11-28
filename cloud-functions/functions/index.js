const bigInt = require('big-integer');
const functions = require('firebase-functions');
const admin = require('firebase-admin');

admin.initializeApp();
let db = admin.database();

const VERSION_NUMBER = '0';

const MIN_TIME_BETWEEN_ACCESSES_S = 30; // prevent bugs
const MIN_TIME_BETWEEN_DISPENSES_S = 60; // prevent double dispensing

function okVersion(req, res) {
  if (req.body.version !== VERSION_NUMBER) {
    res.status(400).send('Bad version number: only version 0 allowed');
    return false;
  }
  return true;
}

function unixTimestamp() { // seconds
  return Math.floor(Date.now() / 1000);
}

function base64ToFingerprint(base64) {
  // convert from base64 to buffer
  let fingerprintBuffer = Buffer.from(base64, 'base64');
  if (fingerprintBuffer.length !== 256) {
    throw Error('Expected a 256-byte (128 short) base64-encoded fingerprint, but its length was '
      + fingerprintBuffer.length + ' bytes.');
  }
  
  // convert from buffer to fingerprint array
  let fingerprint = [];
  for (let i = 0; i < 128; ++i) {
    fingerprint.push((fingerprintBuffer[2*i] << 8) + fingerprintBuffer[2*i+1]);
  }
  
  return fingerprint;
}

function squareEuclideanDistance(vector1, vector2) { // 128-vectors
  let sq = bigInt.zero;
  for (let i = 0; i < 128; ++i) {
    let diff = bigInt(vector1[i] - vector2[i]);
    sq += diff*diff;
  }
  return sq;
}

/** Find the closest user to the passed face fingerprint, respond with it, and add record of access.  */
exports.pharmacy_get = functions.https.onRequest(async (req, res) => {
  if (!okVersion(req, res)) return;
  
  if (req.body.fingerprint === undefined) {
    res.status(400).send({ "msg": "missing required field 'fingerprint'" });
    return;
  }
  
  // convert fingerprint from base64
  let fingerprintBase64 = req.body.fingerprint;
  let fingerprint;
  try {
    fingerprint = base64ToFingerprint(fingerprintBase64);
  } catch (e) {
    res.status(400).send(e.toString());
    return;
  }
  
  // get every user from the database
  let ref = db.ref('arka/user');
  ref.once('value', data => {
    let users = data.val();
    
    let bestId, bestDist = bigInt.minusOne;
    for (let userId in users) {
      // compute their distance to us
      let userFingerprint = base64ToFingerprint(users[userId].fingerprint);
      let dist = squareEuclideanDistance(fingerprint, userFingerprint);
      if (dist < bestDist || bestDist < 0) {
        bestDist = dist;
        bestId = userId;
      }
    }
    
    console.log('Closest user has id ' + bestId + ' (' + users[bestId].name + '), allowing dispensing of their prescriptions');
    
    // find the prescriptions to not dispense
    let badDins = [];
    let timestamp = unixTimestamp();
    for (let key in users[bestId].record) {
      // access and dispense records have different associated wait times
      let waitTime = (users[bestId].record[key].type === 'access') ? MIN_TIME_BETWEEN_ACCESSES_S : MIN_TIME_BETWEEN_DISPENSES_S;
      
      if (timestamp <= users[bestId].record[key].timestamp + waitTime) {
        // don't dispense these dins
        badDins = badDins.concat(users[bestId].record[key].dins);
      }
    }
    
    // construct all prescriptions to dispense
    let prescriptions = Object.values(users[bestId].prescriptions);
    prescriptions = prescriptions.filter(presc => timestamp <= presc.expires).filter(presc => !badDins.includes(presc.din));
    
    // make the new transaction
    let transacRef = db.ref('arka/transactions').push();
    let transacId = transacRef.key;
    transacRef.set({
      "userId": bestId,
      "timestamp": timestamp
    }, error => {
      if (error) {
        res.status(500).send(err).end();
        return;
      }
      
      // construct response object
      let resObj = {
        "version": "0",
        "success": true,
        "id": transacId,
        "prescriptions": prescriptions
      };
      res.status(200).send(resObj).end();
      
      // add record of access - not *really* a problem if it fails
      ref.child(bestId).child('record').push({ // push => generate random key
        "dins": prescriptions.map(presc => presc.din),
        "timestamp": timestamp,
        "type": "access"
      }, error => {
        if (error) {
          console.warn('Could not write record of access to user ' + bestId);
        }
      });
    });
  }, err => {
    res.status(500).send(err).end();
  });
});

/** Add dispense record */
exports.pharmacy_done = functions.https.onRequest((req, res) => {
  if (!okVersion(req, res)) return;
  
  if (req.body.id === undefined || req.body.din === undefined || req.body.timestamp === undefined) {
    res.status(400).send({ "msg": "missing some required fields: need all of id, din, timestamp" });
    return;
  }
  
  // get the transactions
  db.ref('arka/transactions').once('value', data => {
    let transactions = data.val();
    if (!(req.body.id in transactions)) {
      res.status(400).send({ "msg": "no transaction with that id found" });
      return;
    }
    
    let userId = transactions[req.body.id].userId;
    
    db.ref('arka/user').child(userId).child('record').push({
      "dins": req.body.din,
      "timestamp": req.body.timestamp,
      "type": "dispense"
    }, error => {
      if (error) {
        res.status(400).send(error);
      } else {
        res.status(200).send({
          "version": "0",
          "success": true,
          "error": 0
        });
      }
    });
  }, err => {
    res.status(500).send(err).end();
  });
});
