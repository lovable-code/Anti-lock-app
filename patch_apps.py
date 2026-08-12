with open('app/src/main/java/com/example/ui/SentinelViewModel.kt', 'r') as f:
    text = f.read()

real_apps = """
    fun getLocalInstalledApplications(): List<com.example.data.AppInfo> {
        val pm = context.packageManager
        val apps = pm.getInstalledApplications(android.content.pm.PackageManager.GET_META_DATA)
        return apps.map { app ->
            val isSystem = (app.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
            com.example.data.AppInfo(
                name = pm.getApplicationLabel(app).toString(),
                packageName = app.packageName,
                isSystemApp = isSystem
            )
        }
    }
"""

text = text.replace("    fun getLocalInstalledApplications(): List<com.example.data.AppInfo> { return emptyList() }", real_apps.strip())

with open('app/src/main/java/com/example/ui/SentinelViewModel.kt', 'w') as f:
    f.write(text)
