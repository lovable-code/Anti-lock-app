import sys

with open('app/src/main/java/com/example/ui/screens/DashboardScreen.kt', 'r') as f:
    lines = f.readlines()

start = -1
end = -1
brace_count = 0
in_func = False

for i, line in enumerate(lines):
    if "fun QRCodeDisplay(" in line:
        start = i
        if lines[i-1].strip() == "@Composable":
            start = i - 1
        in_func = True
    
    if in_func:
        brace_count += line.count('{')
        brace_count -= line.count('}')
        if brace_count == 0 and "}" in line and i > start + 3:
            end = i
            break

replacement = """@Composable
fun QRCodeDisplay(
    contentString: String,
    modifier: Modifier = Modifier
) {
    val bitmap = remember(contentString) {
        try {
            val writer = com.google.zxing.qrcode.QRCodeWriter()
            val bitMatrix = writer.encode(contentString, com.google.zxing.BarcodeFormat.QR_CODE, 512, 512)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bmp = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.RGB_565)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bmp.setPixel(x, y, if (bitMatrix.get(x, y)) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
                }
            }
            androidx.compose.ui.graphics.asImageBitmap(bmp)
        } catch (e: Exception) {
            null
        }
    }

    if (bitmap != null) {
        androidx.compose.foundation.Image(bitmap = bitmap, contentDescription = "QR Code", modifier = modifier)
    } else {
        Box(modifier = modifier.background(Color.White))
    }
}
"""

if start != -1 and end != -1:
    with open('app/src/main/java/com/example/ui/screens/DashboardScreen.kt', 'w') as f:
        f.writelines(lines[:start])
        f.write(replacement)
        f.writelines(lines[end+1:])
    print("Replaced QRCodeDisplay successfully.")
else:
    print("Failed to find QRCodeDisplay bounds.")
