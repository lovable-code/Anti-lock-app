with open('app/src/main/java/com/example/ui/SentinelViewModel.kt', 'r') as f:
    text = f.read()

text = text.replace("            false\n        }\n    fun setTimedAutoLock", "            false\n        }\n    }\n\n    fun setTimedAutoLock")

with open('app/src/main/java/com/example/ui/SentinelViewModel.kt', 'w') as f:
    f.write(text)

