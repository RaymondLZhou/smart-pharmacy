const bigInt = require('big-integer');
const functions = require('firebase-functions');
const admin = require('firebase-admin');

admin.initializeApp();
let db = admin.database();

const MIN_TIME_BETWEEN_DISPENSES = 60;

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

exports.pharmacy_get = functions.https.onRequest(async (req, res) => {
  if (req.body.version !== '0') {
    res.status(400).send('Bad version number: only version 0 allowed');
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
    
    // find the prescriptions to dispense
    let badDins = [];
    let timestamp = Math.floor(Date.now() / 1000); // convert ms => s
    for (let key in users[bestId].record) {
      if (timestamp <= users[bestId].record[key].timestamp + MIN_TIME_BETWEEN_DISPENSES) {
        // don't dispense these dins
        badDins = badDins.concat(users[bestId].record[key].dins);
      }
    }
    let prescriptions = Object.values(users[bestId].prescriptions);
    prescriptions = prescriptions.filter(presc => timestamp <= presc.expires).filter(presc => !badDins.includes(presc.din));
    
    // construct response object
    let resObj = {
      "version": "0",
      "success": true,
      "id": 1234567890,
      "prescriptions": prescriptions
    };
    res.status(200).send(resObj).end();
    
    // add record of access - not *really* a problem if it fails
    ref.child(bestId).child('record').push({ // push => generate random key
      "dins": prescriptions.map(presc => presc.din),
      "timestamp": timestamp
    }, error => {
      if (error) {
        console.warn('Could not write record of access to user ' + bestId);
      }
    });
  }, err => {
    res.status(500).send(err).end();
  });
});
