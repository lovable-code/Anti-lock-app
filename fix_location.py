import sys

with open('app/src/main/java/com/example/ui/screens/LocationScreen.kt', 'r') as f:
    lines = f.readlines()

for i in range(len(lines)):
    if 'colors = ButtonDefaults.buttonColors(' in lines[i] and 'containerColor = if (geofenceBreached) EmeraldNeon else AlertOrange,' in lines[i+1]:
        # Delete from here to the end of the button
        start = i
        end = i + 10
        with open('app/src/main/java/com/example/ui/screens/LocationScreen.kt', 'w') as out:
            out.writelines(lines[:start])
            out.writelines(lines[end:])
        break
print("Fixed location screen")
