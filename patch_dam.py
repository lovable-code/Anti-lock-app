with open('app/src/main/java/com/example/service/DeviceAgentManager.kt', 'r') as f:
    code = f.read()

old_get_loc = """            val loc = fusedLocationClient.getCurrentLocation(com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, null).await()
            if (loc != null) {
                Pair(loc.latitude, loc.longitude)
            } else {
                null
            }"""

new_get_loc = """            val loc = fusedLocationClient.getCurrentLocation(com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, null).await()
            if (loc != null) {
                Pair(loc.latitude, loc.longitude)
            } else {
                val lastLoc = fusedLocationClient.lastLocation.await()
                if (lastLoc != null) Pair(lastLoc.latitude, lastLoc.longitude) else null
            }"""

code = code.replace(old_get_loc, new_get_loc)

old_enroll = """        val realLoc = getRealLocation()
        val finalLat = realLoc?.first ?: driftLatitude
        val finalLng = realLoc?.second ?: driftLongitude"""

new_enroll = """        val currentDevice = repository.getDeviceById(thisDeviceId)
        val realLoc = getRealLocation()
        val finalLat = realLoc?.first ?: currentDevice?.latitude ?: driftLatitude
        val finalLng = realLoc?.second ?: currentDevice?.longitude ?: driftLongitude"""

code = code.replace(old_enroll, new_enroll)

with open('app/src/main/java/com/example/service/DeviceAgentManager.kt', 'w') as f:
    f.write(code)

print("Updated DeviceAgentManager.kt")
