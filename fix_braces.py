import sys

with open('app/src/main/java/com/example/ui/screens/LocationScreen.kt', 'r') as f:
    lines = f.readlines()

new_lines = []
skip = False
for i, line in enumerate(lines):
    if 'Text("Set Current as Center"' in line:
        new_lines.append(line)
        # We need to output the correct number of closing braces here
        # Row, Column, Card, item
        new_lines.append("                        }\n")
        new_lines.append("                    }\n")
        new_lines.append("                }\n")
        new_lines.append("            }\n")
        new_lines.append("        }\n")
        skip = True
        continue
    if skip:
        if 'item {' in line or '// Selected Device' in line:
            skip = False
            new_lines.append(line)
        continue
    if not skip:
        new_lines.append(line)

with open('app/src/main/java/com/example/ui/screens/LocationScreen.kt', 'w') as f:
    f.writelines(new_lines)
print("Fixed braces")
