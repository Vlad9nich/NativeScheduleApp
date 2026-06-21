package ru.kernelordexter.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*
import android.content.Context
import android.content.SharedPreferences
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit

val BrandBlack = Color(0xFF0A0A0A)
val BrandDarkGray = Color(0xFF171717)
val BrandRed = Color(0xFFE50914)
val BrandLightGray = Color(0xFFA3A3A3)


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

enum class LiveStatus(val label: String, val color: Color) {
    IN_PROGRESS("Идет пара", BrandRed),
    WAITING("Ожидание", Color(0xFFFFC107)),
    FREE("Свободно", BrandLightGray)
}

data class ClassInfo(
    val id: String,
    val subject: String,
    val type: String,
    val teacher: String,
    val room: String,
    val time: String,
    val color: String
)

data class DaySchedule(
    val isoDate: String,
    val items: List<ClassInfo>
)

data class WeekSchedule(
    val weekNumber: Int,
    val days: Map<String, DaySchedule>
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val prefs = PreferencesManager(this)
        
        setContent {
            var scheduleData by remember { mutableStateOf<Map<String, List<WeekSchedule>>?>(null) }
            
            LaunchedEffect(Unit) {
                scheduleData = loadSchedule()
            }
            
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = BrandBlack,
                    surface = BrandDarkGray,
                    primary = BrandRed
                ),
                typography = AppTypography
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (scheduleData == null) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = BrandRed)
                        }
                    } else {
                        ScheduleApp(scheduleData!!, prefs)
                    }
                }
            }
        }
    }

    private fun loadSchedule(): Map<String, List<WeekSchedule>> {
        val type = object : TypeToken<Map<String, List<WeekSchedule>>>() {}.type
        val gson = Gson()
        return try {
            val stream = assets.open("schedule_data.json")
            val reader = InputStreamReader(stream)
            gson.fromJson(reader, type)
        } catch (e: Exception) {
            emptyMap()
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ScheduleApp(scheduleData: Map<String, List<WeekSchedule>>, prefs: PreferencesManager) {
    var activeGroup by remember { mutableStateOf(prefs.activeGroup) }
    var currentWeekIndex by remember { mutableStateOf(0) }
    var currentDay by remember { mutableStateOf(1) }
    
    var isEditMode by remember { mutableStateOf(false) }
    var customScheduleState by remember { mutableStateOf(0) }
    
    LaunchedEffect(activeGroup) {
        prefs.activeGroup = activeGroup
    }
    
    val weeks = scheduleData[activeGroup] ?: emptyList()
    val activeWeek = weeks.getOrNull(currentWeekIndex)
    
    var currentTime by remember { mutableStateOf(Calendar.getInstance(TimeZone.getTimeZone("Europe/Moscow"))) }

    LaunchedEffect(Unit) {
        while(true) {
            currentTime = Calendar.getInstance(TimeZone.getTimeZone("Europe/Moscow"))
            delay(1000)
        }
    }

    val todayDayKey = if (currentTime.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) 0 else currentTime.get(Calendar.DAY_OF_WEEK) - 1
    val todaySchedule = activeWeek?.days?.get(todayDayKey.toString())

    val currentHour = currentTime.get(Calendar.HOUR_OF_DAY)
    val currentMinute = currentTime.get(Calendar.MINUTE)
    val currentSecond = currentTime.get(Calendar.SECOND)
    val currentTotalSeconds = currentHour * 3600 + currentMinute * 60 + currentSecond

    var liveStatus = LiveStatus.FREE
    var liveRemainingSeconds = 0

    if (todaySchedule != null && todaySchedule.items.isNotEmpty()) {
        for (item in todaySchedule.items) {
            val times = item.time.split(" - ")
            if (times.size == 2) {
                val startParts = times[0].split(":")
                val endParts = times[1].split(":")
                if (startParts.size == 2 && endParts.size == 2) {
                    val startSeconds = startParts[0].toInt() * 3600 + startParts[1].toInt() * 60
                    val endSeconds = endParts[0].toInt() * 3600 + endParts[1].toInt() * 60
                    
                    if (currentTotalSeconds < startSeconds) {
                        if (liveStatus == LiveStatus.FREE) {
                            liveStatus = LiveStatus.WAITING
                            liveRemainingSeconds = startSeconds - currentTotalSeconds
                        }
                    } else if (currentTotalSeconds in startSeconds..endSeconds) {
                        liveStatus = LiveStatus.IN_PROGRESS
                        liveRemainingSeconds = endSeconds - currentTotalSeconds
                        break
                    }
                }
            }
        }
    }

    val liveTimeString = if (liveStatus != LiveStatus.FREE) {
        val h = liveRemainingSeconds / 3600
        val m = (liveRemainingSeconds % 3600) / 60
        val s = liveRemainingSeconds % 60
        val prefix = if (liveStatus == LiveStatus.IN_PROGRESS) "до конца: " else "до начала: "
        if (h > 0) String.format("%s%d:%02d:%02d", prefix, h, m, s) else String.format("%s%02d:%02d", prefix, m, s)
    } else {
        ""
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(48.dp).background(BrandRed, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("K", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 24.sp, fontFamily = Oswald)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = activeGroup,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    fontFamily = Oswald,
                    modifier = Modifier.clickable { 
                        activeGroup = if (activeGroup == "ЗИТ-242") "ЗИТ-241" else "ЗИТ-242" 
                    }
                )
            }
            
            IconButton(onClick = { isEditMode = !isEditMode }) {
                Icon(
                    imageVector = if (isEditMode) Icons.Default.Check else Icons.Default.Edit,
                    contentDescription = "Edit Schedule",
                    tint = Color.White
                )
            }
        }

        // Live Status
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = BrandDarkGray),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("ВРЕМЯ ПО МОСКВЕ", color = BrandLightGray, fontSize = 12.sp, fontFamily = Oswald)
                        Text(
                            text = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).apply { timeZone = TimeZone.getTimeZone("Europe/Moscow") }.format(currentTime.time),
                            color = Color.White,
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = Oswald
                        )
                        Text(
                            text = SimpleDateFormat("dd MMMM yyyy", Locale("ru")).format(currentTime.time),
                            color = BrandRed,
                            fontSize = 14.sp
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(10.dp).background(liveStatus.color.copy(alpha = pulseAlpha), CircleShape))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(liveStatus.label, color = liveStatus.color, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        if (liveTimeString.isNotEmpty()) {
                            Text(liveTimeString, color = BrandLightGray, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
            }
        }

        Text("РАСПИСАНИЕ", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, fontFamily = Oswald, modifier = Modifier.padding(bottom = 8.dp))

        // Week Tabs
        LazyRow(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
            items(weeks.indices.toList()) { index ->
                val isSelected = currentWeekIndex == index
                Box(
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .background(
                            if (isSelected) BrandRed else BrandDarkGray,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { currentWeekIndex = index }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("Неделя ${index + 1}", color = Color.White)
                }
            }
        }

        // Day Tabs
        val days = mapOf(1 to "Пн", 2 to "Вт", 3 to "Ср", 4 to "Чт", 5 to "Пт", 6 to "Сб", 0 to "Вс")
        val dayKeysList = days.keys.toList()
        val pagerState = rememberPagerState(initialPage = dayKeysList.indexOf(currentDay).takeIf { it >= 0 } ?: 0)
        val coroutineScope = rememberCoroutineScope()

        LaunchedEffect(pagerState.currentPage) {
            currentDay = dayKeysList[pagerState.currentPage]
        }

        LazyRow(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            items(dayKeysList.size) { index ->
                val dayKey = dayKeysList[index]
                val isSelected = currentDay == dayKey
                Box(
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .background(
                            if (isSelected) Color.White.copy(alpha = 0.2f) else BrandDarkGray,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { 
                            currentDay = dayKey
                            coroutineScope.launch { pagerState.animateScrollToPage(index) }
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(days[dayKey] ?: "", color = Color.White)
                }
            }
        }

        // Classes list
        var showEditModal by remember { mutableStateOf(false) }
        var editingClass by remember { mutableStateOf<ClassInfo?>(null) }
        var editTargetDay by remember { mutableStateOf("") }

        if (showEditModal) {
            EditClassModal(
                initialClass = editingClass,
                onDismiss = { showEditModal = false },
                onSave = { updatedClass ->
                    val pageScheduleItems = prefs.getCustomSchedule(activeGroup, currentWeekIndex, editTargetDay) 
                        ?: activeWeek?.days?.get(editTargetDay)?.items ?: emptyList()
                    val newList = pageScheduleItems.toMutableList()
                    val index = newList.indexOfFirst { it.id == updatedClass.id }
                    if (index >= 0) {
                        newList[index] = updatedClass
                    } else {
                        newList.add(updatedClass)
                    }
                    prefs.saveCustomSchedule(activeGroup, currentWeekIndex, editTargetDay, newList)
                    customScheduleState++
                    showEditModal = false
                },
                onDelete = {
                    if (editingClass != null) {
                        val pageScheduleItems = prefs.getCustomSchedule(activeGroup, currentWeekIndex, editTargetDay) 
                            ?: activeWeek?.days?.get(editTargetDay)?.items ?: emptyList()
                        val newList = pageScheduleItems.filter { it.id != editingClass!!.id }
                        prefs.saveCustomSchedule(activeGroup, currentWeekIndex, editTargetDay, newList)
                        customScheduleState++
                    }
                    showEditModal = false
                }
            )
        }

        HorizontalPager(
            pageCount = dayKeysList.size,
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            val pageDayKey = dayKeysList[page].toString()
            val originalDaySchedule = activeWeek?.days?.get(pageDayKey)?.items ?: emptyList()
            
            val activeDayItems = remember(activeGroup, currentWeekIndex, pageDayKey, customScheduleState) {
                prefs.getCustomSchedule(activeGroup, currentWeekIndex, pageDayKey) ?: originalDaySchedule
            }

            if (activeDayItems.isEmpty() && !isEditMode) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Выходной день", color = BrandLightGray)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(activeDayItems) { classItem ->
                        ClassRow(
                            item = classItem,
                            isEditMode = isEditMode,
                            onClick = {
                                if (isEditMode) {
                                    editingClass = classItem
                                    editTargetDay = pageDayKey
                                    showEditModal = true
                                }
                            }
                        )
                    }
                    
                    if (isEditMode) {
                        item {
                            Button(
                                onClick = { 
                                    editingClass = null
                                    editTargetDay = pageDayKey
                                    showEditModal = true 
                                },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BrandRed)
                            ) {
                                Text("Добавить пару", color = Color.White)
                            }
                            
                            Button(
                                onClick = { 
                                    prefs.clearCustomSchedule(activeGroup, currentWeekIndex, pageDayKey)
                                    customScheduleState++
                                },
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BrandDarkGray)
                            ) {
                                Text("Сбросить изменения", color = Color.White)
                            }
                        }
                    }

                    item {
                        Text(
                            "by KernelMod | Баг-репорт: @kernelmodEZ", 
                            color = BrandLightGray, 
                            fontSize = 12.sp,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp).wrapContentWidth(Alignment.CenterHorizontally)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ClassRow(item: ClassInfo, isEditMode: Boolean = false, onClick: () -> Unit = {}) {
    val times = item.time.split(" - ")
    val startTime = times.getOrNull(0) ?: ""
    val endTime = times.getOrNull(1) ?: ""

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .background(BrandDarkGray, RoundedCornerShape(12.dp))
            .border(1.dp, if (isEditMode) BrandRed else Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
            .then(if (isEditMode) Modifier.clickable { onClick() } else Modifier)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(60.dp)
        ) {
            Text(startTime, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp, fontFamily = Oswald)
            Text(endTime, color = BrandLightGray, fontSize = 12.sp)
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        Box(modifier = Modifier.width(2.dp).height(40.dp).background(BrandRed))
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            val hasExamOrCredit = item.subject.contains("зач.", ignoreCase = true) || item.subject.contains("экз.", ignoreCase = true) || item.type.contains("зач", ignoreCase = true) || item.type.contains("экз", ignoreCase = true)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(item.type.uppercase(), color = BrandRed, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = Oswald)
                if (hasExamOrCredit) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.background(Color(0xFF8B0000), RoundedCornerShape(4.dp)).padding(horizontal = 4.dp, vertical = 2.dp)) {
                        Text("ЗАЧЕТ/ЭКЗАМЕН", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Text(item.subject, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp, fontFamily = Oswald, maxLines = 2)
            Row(modifier = Modifier.padding(top = 4.dp)) {
                Text(item.room, color = BrandLightGray, fontSize = 12.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(item.teacher, color = BrandLightGray, fontSize = 12.sp, maxLines = 1)
            }
        }
        if (isEditMode) {
            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = BrandRed)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditClassModal(
    initialClass: ClassInfo?,
    onDismiss: () -> Unit,
    onSave: (ClassInfo) -> Unit,
    onDelete: () -> Unit
) {
    var subject by remember { mutableStateOf(initialClass?.subject ?: "") }
    var type by remember { mutableStateOf(initialClass?.type ?: "") }
    var teacher by remember { mutableStateOf(initialClass?.teacher ?: "") }
    var room by remember { mutableStateOf(initialClass?.room ?: "") }
    var time by remember { mutableStateOf(initialClass?.time ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialClass == null) "Добавить пару" else "Редактировать", color = Color.White) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = time, onValueChange = { time = it },
                    label = { Text("Время (напр. 09:00 - 10:30)", color = BrandLightGray) },
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = subject, onValueChange = { subject = it },
                    label = { Text("Предмет", color = BrandLightGray) },
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = type, onValueChange = { type = it },
                    label = { Text("Тип (Лекция/Практика)", color = BrandLightGray) },
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = teacher, onValueChange = { teacher = it },
                    label = { Text("Преподаватель", color = BrandLightGray) },
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = room, onValueChange = { room = it },
                    label = { Text("Кабинет", color = BrandLightGray) },
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        ClassInfo(
                            id = initialClass?.id ?: UUID.randomUUID().toString(),
                            subject = subject,
                            type = type,
                            teacher = teacher,
                            room = room,
                            time = time,
                            color = initialClass?.color ?: ""
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = BrandRed)
            ) { Text("Сохранить", color = Color.White) }
        },
        dismissButton = {
            Row {
                if (initialClass != null) {
                    TextButton(onClick = onDelete) { Text("Удалить", color = BrandRed) }
                }
                TextButton(onClick = onDismiss) { Text("Отмена", color = BrandLightGray) }
            }
        },
        containerColor = BrandDarkGray,
        titleContentColor = Color.White,
        textContentColor = Color.White
    )
}

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("schedule_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    var activeGroup: String
        get() = prefs.getString("active_group", "ЗИТ-242") ?: "ЗИТ-242"
        set(value) = prefs.edit().putString("active_group", value).apply()

    fun saveCustomSchedule(group: String, weekIndex: Int, day: String, items: List<ClassInfo>) {
        val key = "custom_${group}_${weekIndex}_$day"
        prefs.edit().putString(key, gson.toJson(items)).apply()
    }

    fun getCustomSchedule(group: String, weekIndex: Int, day: String): List<ClassInfo>? {
        val key = "custom_${group}_${weekIndex}_$day"
        val json = prefs.getString(key, null) ?: return null
        val type = object : TypeToken<List<ClassInfo>>() {}.type
        return try { gson.fromJson(json, type) } catch(e: Exception) { null }
    }
    
    fun clearCustomSchedule(group: String, weekIndex: Int, day: String) {
        val key = "custom_${group}_${weekIndex}_$day"
        prefs.edit().remove(key).apply()
    }
}
