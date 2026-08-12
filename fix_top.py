import sys

with open('app/src/main/java/com/example/ui/screens/DashboardScreen.kt', 'r') as f:
    content = f.read()

content = content.replace("import androidx.compose.ui.graphics.asImageBitmappackage", "package")
content = "import androidx.compose.ui.graphics.asImageBitmap\n" + content

with open('app/src/main/java/com/example/ui/screens/DashboardScreen.kt', 'w') as f:
    f.write(content)
print("Fixed top")
