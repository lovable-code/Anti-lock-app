with open('app/src/main/java/com/example/ui/SentinelViewModel.kt', 'r') as f:
    text = f.read()

text = text.replace(
    "private val _diagnosticsProgress = kotlinx.coroutines.flow.MutableStateFlow(0f)",
    "private val _diagnosticsProgress = kotlinx.coroutines.flow.MutableStateFlow<List<String>>(emptyList())"
)

text = text.replace(
    "_diagnosticsProgress.value = 0f",
    "_diagnosticsProgress.value = listOf(\"Starting diagnostics...\")"
)

text = text.replace(
    "_diagnosticsProgress.value = i / 10f",
    "_diagnosticsProgress.value = _diagnosticsProgress.value + \"[OK] Step $i completed\""
)

with open('app/src/main/java/com/example/ui/SentinelViewModel.kt', 'w') as f:
    f.write(text)
