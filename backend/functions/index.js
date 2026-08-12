const functions = require("firebase-functions");
const admin = require("firebase-admin");
admin.initializeApp();

exports.sendCommandToDevice = functions.firestore
  .document("users/{ownerId}/devices/{deviceId}/commands/{commandId}")
  .onCreate(async (snap, context) => {
    const commandData = snap.data();
    const ownerId = context.params.ownerId;
    const deviceId = context.params.deviceId;
    
    // Fetch the target device to get its FCM token
    const deviceRef = admin.firestore().collection("users").doc(ownerId).collection("devices").doc(deviceId);
    const deviceDoc = await deviceRef.get();
    
    if (!deviceDoc.exists) {
      console.log(`Device ${deviceId} not found for owner ${ownerId}`);
      return null;
    }
    
    const device = deviceDoc.data();
    const fcmToken = device.fcmToken;
    
    if (!fcmToken) {
      console.log(`No FCM token available for device ${deviceId}`);
      return null;
    }
    
    // Construct the FCM payload
    const payload = {
      data: {
        commandId: commandData.commandId,
        targetDeviceId: commandData.targetDeviceId,
        commandType: commandData.type,
        payloadJson: commandData.payloadJson || "{}",
        senderId: commandData.senderId,
        timestamp: String(commandData.timestamp || Date.now())
      }
    };
    
    try {
      // Send FCM message to the specific device
      const response = await admin.messaging().sendToDevice(fcmToken, payload);
      console.log(`Successfully sent command to device ${deviceId}:`, response);
      
      // Update command status to DELIVERED
      await snap.ref.update({ status: "DELIVERED" });
    } catch (error) {
      console.error(`Error sending FCM message to device ${deviceId}:`, error);
      await snap.ref.update({ status: "FAILED_DELIVERY" });
    }
    
    return null;
  });
