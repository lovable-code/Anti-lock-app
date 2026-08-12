import sys

with open('app/src/main/java/com/example/ui/screens/LocationScreen.kt', 'r') as f:
    lines = f.readlines()

for i in range(len(lines)):
    if 'Text("Set Current as Center"' in lines[i]:
        # we have 4 closing braces after this
        # let's look at what's there
        pass

