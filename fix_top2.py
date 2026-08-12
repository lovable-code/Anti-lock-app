import sys

with open('app/src/main/java/com/example/ui/screens/DashboardScreen.kt', 'r') as f:
    content = f.read()

# find package com.example.ui.screens
idx = content.find("package com.example.ui.screens")
if idx != -1:
    content = "package com.example.ui.screens\nimport androidx.compose.ui.graphics.asImageBitmap\n" + content[idx + len("package com.example.ui.screens"):]

with open('app/src/main/java/com/example/ui/screens/DashboardScreen.kt', 'w') as f:
    f.write(content)
print("Fixed top 2")
