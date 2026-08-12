with open('app/src/main/java/com/example/ui/screens/DashboardScreen.kt', 'r') as f:
    text = f.read()

text = text.replace("it.contains(searchQuery, ignoreCase = true)", "it.name.contains(searchQuery, ignoreCase = true) || it.packageName.contains(searchQuery, ignoreCase = true)")

with open('app/src/main/java/com/example/ui/screens/DashboardScreen.kt', 'w') as f:
    f.write(text)

print("Fixed DashboardScreen!")
