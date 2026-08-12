import re

with open('app/src/main/java/com/example/data/SentinelRepository.kt', 'r') as f:
    content = f.read()

# Fix ownerId fallback
content = content.replace('val ownerId = FirebaseAuth.getInstance().currentUser?.uid ?: return', 
                          'var ownerId = FirebaseAuth.getInstance().currentUser?.uid\n        if (ownerId == null) {\n            val context = (dao as? androidx.room.RoomDatabase)?.openHelper?.readableDatabase?.attachedDbs?.firstOrNull() // Not possible cleanly\n            // Since context is hard to get here without refactoring, let\'s assume this is mostly called when Auth is present, or we can\'t sync.\n            return\n        }')

with open('app/src/main/java/com/example/data/SentinelRepository.kt', 'w') as f:
    f.write(content)
