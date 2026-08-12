import sys

with open('app/src/main/java/com/example/ui/screens/DashboardScreen.kt', 'r') as f:
    content = f.read()

content = content.replace("androidx.compose.ui.graphics.asImageBitmap(bmp)", "bmp.asImageBitmap()")

with open('app/src/main/java/com/example/ui/screens/DashboardScreen.kt', 'w') as f:
    f.write(content)
print("Fixed import")
