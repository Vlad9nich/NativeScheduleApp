import re

file_path = "/root/NativeScheduleApp/app/src/main/java/ru/kernelordexter/app/MainActivity.kt"

with open(file_path, "r") as f:
    content = f.read()

# Add imports
imports_to_add = """import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.TextStyle
"""
content = content.replace("import androidx.compose.ui.unit.sp", imports_to_add + "import androidx.compose.ui.unit.sp")

# Define fonts and Typography
typography_def = """
val Oswald = FontFamily(Font(R.font.oswald))
val Manrope = FontFamily(Font(R.font.manrope))

val AppTypography = Typography(
    displayLarge = TextStyle(fontFamily = Oswald),
    displayMedium = TextStyle(fontFamily = Oswald),
    displaySmall = TextStyle(fontFamily = Oswald),
    headlineLarge = TextStyle(fontFamily = Oswald),
    headlineMedium = TextStyle(fontFamily = Oswald),
    headlineSmall = TextStyle(fontFamily = Oswald),
    titleLarge = TextStyle(fontFamily = Oswald),
    titleMedium = TextStyle(fontFamily = Oswald),
    titleSmall = TextStyle(fontFamily = Oswald),
    bodyLarge = TextStyle(fontFamily = Manrope),
    bodyMedium = TextStyle(fontFamily = Manrope),
    bodySmall = TextStyle(fontFamily = Manrope),
    labelLarge = TextStyle(fontFamily = Manrope),
    labelMedium = TextStyle(fontFamily = Manrope),
    labelSmall = TextStyle(fontFamily = Manrope),
)

"""
content = content.replace("enum class LiveStatus", typography_def + "enum class LiveStatus")

# Update MaterialTheme to use Typography
content = content.replace("primary = BrandRed\n                )", "primary = BrandRed\n                ),\n                typography = AppTypography")

# We want Manrope as default, so just set the typography.
# But for headers, we explicitly add `fontFamily = Oswald`.
# Headers are usually texts with size >= 18.sp or explicit headers.
# Let's find specific text elements.

replacements = {
    'Text("K", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 24.sp)': 
    'Text("K", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 24.sp, fontFamily = Oswald)',
    
    'text = activeGroup,\n                    color = Color.White,\n                    fontWeight = FontWeight.Bold,\n                    fontSize = 18.sp,':
    'text = activeGroup,\n                    color = Color.White,\n                    fontWeight = FontWeight.Bold,\n                    fontSize = 18.sp,\n                    fontFamily = Oswald,',
    
    'Text("ВРЕМЯ ПО МОСКВЕ", color = BrandLightGray, fontSize = 12.sp)':
    'Text("ВРЕМЯ ПО МОСКВЕ", color = BrandLightGray, fontSize = 12.sp, fontFamily = Oswald)',
    
    'text = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).apply { timeZone = TimeZone.getTimeZone("Europe/Moscow") }.format(currentTime.time),\n                            color = Color.White,\n                            fontSize = 40.sp,\n                            fontWeight = FontWeight.Bold':
    'text = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).apply { timeZone = TimeZone.getTimeZone("Europe/Moscow") }.format(currentTime.time),\n                            color = Color.White,\n                            fontSize = 40.sp,\n                            fontWeight = FontWeight.Bold,\n                            fontFamily = Oswald',
    
    'Text("РАСПИСАНИЕ", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))':
    'Text("РАСПИСАНИЕ", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, fontFamily = Oswald, modifier = Modifier.padding(bottom = 8.dp))',
    
    'Text(startTime, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)':
    'Text(startTime, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp, fontFamily = Oswald)',
    
    'Text(item.subject, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 2)':
    'Text(item.subject, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp, fontFamily = Oswald, maxLines = 2)',
    
    'Text(item.type.uppercase(), color = BrandRed, fontSize = 10.sp, fontWeight = FontWeight.Bold)':
    'Text(item.type.uppercase(), color = BrandRed, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = Oswald)'
}

for old_str, new_str in replacements.items():
    content = content.replace(old_str, new_str)

with open(file_path, "w") as f:
    f.write(content)

print("MainActivity.kt updated with fonts.")
