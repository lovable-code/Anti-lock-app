with open('app/src/main/java/com/example/service/DeviceAgentManager.kt', 'r') as f:
    code = f.read()

target = """        val currentDevice = repository.getDeviceById(thisDeviceId)
        val realLoc = getRealLocation()
        val finalLat = realLoc?.first ?: currentDevice?.latitude ?: driftLatitude
        val finalLng = realLoc?.second ?: currentDevice?.longitude ?: driftLongitude

        val currentDevice = repository.getDeviceById(thisDeviceId)"""

replacement = """        val currentDevice = repository.getDeviceById(thisDeviceId)
        val realLoc = getRealLocation()
        val finalLat = realLoc?.first ?: currentDevice?.latitude ?: driftLatitude
        val finalLng = realLoc?.second ?: currentDevice?.longitude ?: driftLongitude"""

code = code.replace(target, replacement)

with open('app/src/main/java/com/example/service/DeviceAgentManager.kt', 'w') as f:
    f.write(code)

print("Fixed duplicate currentDevice declaration in DeviceAgentManager.kt")
