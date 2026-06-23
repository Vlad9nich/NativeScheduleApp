package ru.kernelordexter.app

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*
import ru.kernelordexter.app.R

val BrandBlack = Color(0xFF0A0A0A)
val BrandDarkGray = Color(0xFF171717)
val BrandRed = Color(0xFFE50914)
val BrandLightGray = Color(0xFFA3A3A3)
val GlassBackground = Color(0x171717).copy(alpha = 0.7f)
val GlassBorder = Color(0xFFFFFFFF).copy(alpha = 0.05f)

val Oswald = FontFamily(
    Font(R.font.oswald_medium, FontWeight.Medium),
    Font(R.font.oswald_bold, FontWeight.Bold)
)

val Manrope = FontFamily(
    Font(R.font.manrope_regular, FontWeight.Normal),
    Font(R.font.manrope_medium, FontWeight.Medium),
    Font(R.font.manrope_semibold, FontWeight.SemiBold),
    Font(R.font.manrope_bold, FontWeight.Bold),
    Font(R.font.manrope_extrabold, FontWeight.ExtraBold)
)

val AppTypography = Typography(
    displayLarge = TextStyle(fontFamily = Oswald, fontWeight = FontWeight.Bold, fontSize = 57.sp),
    displayMedium = TextStyle(fontFamily = Oswald, fontWeight = FontWeight.Bold, fontSize = 45.sp),
    displaySmall = TextStyle(fontFamily = Oswald, fontWeight = FontWeight.Bold, fontSize = 36.sp),
    headlineLarge = TextStyle(fontFamily = Oswald, fontWeight = FontWeight.Bold, fontSize = 32.sp),
    headlineMedium = TextStyle(fontFamily = Oswald, fontWeight = FontWeight.Medium, fontSize = 28.sp),
    headlineSmall = TextStyle(fontFamily = Oswald, fontWeight = FontWeight.Medium, fontSize = 24.sp),
    titleLarge = TextStyle(fontFamily = Oswald, fontWeight = FontWeight.Medium, fontSize = 22.sp),
    titleMedium = TextStyle(fontFamily = Oswald, fontWeight = FontWeight.Medium, fontSize = 16.sp),
    titleSmall = TextStyle(fontFamily = Oswald, fontWeight = FontWeight.Medium, fontSize = 14.sp),
    bodyLarge = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Normal, fontSize = 16.sp),
    bodyMedium = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Normal, fontSize = 14.sp),
    bodySmall = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Normal, fontSize = 12.sp),
    labelLarge = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Medium, fontSize = 14.sp),
    labelMedium = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Medium, fontSize = 12.sp),
    labelSmall = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Medium, fontSize = 11.sp)
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
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawRect(
                            brush = Brush.radialGradient(
                                colors = listOf(BrandRed.copy(alpha = 0.2f), Color.Transparent),
                                center = Offset(size.width / 2f, 0f),
                                radius = size.width
                            )
                        )
                    }

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
        val type = object : TypeToken<Map<String, List<JsonWeekSchedule>>>() {}.type
        val gson = Gson()
        return try {
            val stream = assets.open("schedule_data.json")
            val reader = InputStreamReader(stream)
            val parsed: Map<String, List<JsonWeekSchedule>> = gson.fromJson(reader, type)
            
            parsed.mapValues { (_, jsonWeeks) ->
                jsonWeeks.mapIndexed { index, jsonWeek ->
                    WeekSchedule(
                        weekNumber = index + 1,
                        days = jsonWeek.days.mapValues { (_, jsonDay) ->
                            DaySchedule(
                                isoDate = jsonDay.isoDate ?: "",
                                items = jsonDay.classes.map { jsonClass ->
                                    val typeStr = if (jsonClass.subject.startsWith("л.", true)) "Лекция"
                                                  else if (jsonClass.subject.startsWith("пр.", true) || jsonClass.subject.startsWith("лаб.", true)) "Практика"
                                                  else "Занятие"
                                    ClassInfo(
                                        id = UUID.randomUUID().toString(),
                                        subject = jsonClass.subject.removePrefix("л. ").removePrefix("пр. ").removePrefix("лаб. ").trim(),
                                        type = typeStr,
                                        teacher = jsonClass.teacher,
                                        room = jsonClass.room,
                                        time = "${jsonClass.start} - ${jsonClass.end}",
                                        color = ""
                                    )
                                }
                            )
                        }
                    )
                }
            }
        } catch (e: Exception) {
            emptyMap()
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
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

    val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()).apply { timeZone = TimeZone.getTimeZone("Europe/Moscow") } }
    val dateFormat = remember { SimpleDateFormat("dd MMMM yyyy", Locale("ru")).apply { timeZone = TimeZone.getTimeZone("Europe/Moscow") } }

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
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .shadow(12.dp, RoundedCornerShape(12.dp), spotColor = BrandRed, ambientColor = BrandRed)
                        .background(BrandRed, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.DateRange, contentDescription = "Schedule Logo", tint = Color.White, modifier = Modifier.size(28.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = activeGroup,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    fontFamily = Oswald,
                    modifier = Modifier.clickable { 
                        activeGroup = if (activeGroup == "ЗИТ-242") "ЗИТ-241" else "ЗИТ-242" 
                    }
                )
            }
            
            IconButton(
                onClick = { isEditMode = !isEditMode },
                modifier = Modifier
                    .background(GlassBackground, CircleShape)
                    .border(1.dp, GlassBorder, CircleShape)
            ) {
                Icon(
                    imageVector = if (isEditMode) Icons.Default.Check else Icons.Default.Edit,
                    contentDescription = "Edit Schedule",
                    tint = if (isEditMode) BrandRed else Color.White
                )
            }
        }

        // Live Status
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .background(GlassBackground, RoundedCornerShape(16.dp))
                .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                .padding(20.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("ВРЕМЯ ПО МОСКВЕ", color = BrandLightGray, fontSize = 12.sp, fontFamily = Oswald)
                    Text(
                        text = timeFormat.format(currentTime.time),
                        color = Color.White,
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = Oswald
                    )
                    Text(
                        text = dateFormat.format(currentTime.time),
                        color = BrandRed,
                        fontSize = 14.sp,
                        fontFamily = Manrope
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .shadow(12.dp, CircleShape, spotColor = liveStatus.color, ambientColor = liveStatus.color)
                                .background(liveStatus.color.copy(alpha = pulseAlpha), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(liveStatus.label, color = liveStatus.color, fontWeight = FontWeight.Bold, fontSize = 14.sp, fontFamily = Manrope)
                    }
                    if (liveTimeString.isNotEmpty()) {
                        Text(liveTimeString, color = BrandLightGray, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp), fontFamily = Manrope)
                    }
                }
            }
        }

        Text("РАСПИСАНИЕ", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, fontFamily = Oswald, modifier = Modifier.padding(bottom = 12.dp))

        // Week Tabs
        LazyRow(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
            items(weeks.indices.toList()) { index ->
                val isSelected = currentWeekIndex == index
                Box(
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .background(
                            if (isSelected) BrandRed else GlassBackground,
                            RoundedCornerShape(12.dp)
                        )
                        .border(1.dp, if (isSelected) Color.Transparent else GlassBorder, RoundedCornerShape(12.dp))
                        .clickable { currentWeekIndex = index }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("Неделя ${index + 1}", color = Color.White, fontFamily = Manrope, fontWeight = if(isSelected) FontWeight.Bold else FontWeight.Normal)
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

        LazyRow(modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)) {
            items(dayKeysList.size) { index ->
                val dayKey = dayKeysList[index]
                val isSelected = currentDay == dayKey
                Box(
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .background(
                            if (isSelected) Color.White.copy(alpha = 0.15f) else GlassBackground,
                            RoundedCornerShape(12.dp)
                        )
                        .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                        .clickable { 
                            currentDay = dayKey
                            coroutineScope.launch { pagerState.animateScrollToPage(index) }
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(days[dayKey] ?: "", color = Color.White, fontFamily = Manrope, fontWeight = if(isSelected) FontWeight.Bold else FontWeight.Normal)
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
                    Text("Выходной день", color = BrandLightGray, fontFamily = Manrope)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(activeDayItems, key = { it.id }) { classItem ->
                        var visible by remember { mutableStateOf(false) }
                        
                        LaunchedEffect(classItem.id) {
                            visible = true
                        }

                        AnimatedVisibility(
                            visible = visible,
                            enter = slideInVertically(initialOffsetY = { 50 }) + fadeIn(animationSpec = tween(400))
                        ) {
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
                    }
                    
                    if (isEditMode) {
                        item {
                            Button(
                                onClick = { 
                                    editingClass = null
                                    editTargetDay = pageDayKey
                                    showEditModal = true 
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                                    .shadow(8.dp, RoundedCornerShape(24.dp), spotColor = BrandRed, ambientColor = BrandRed),
                                colors = ButtonDefaults.buttonColors(containerColor = BrandRed)
                            ) {
                                Text("Добавить пару", color = Color.White, fontFamily = Manrope, fontWeight = FontWeight.Bold)
                            }
                            
                            Button(
                                onClick = { 
                                    prefs.clearCustomSchedule(activeGroup, currentWeekIndex, pageDayKey)
                                    customScheduleState++
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = GlassBackground),
                                border = BorderStroke(1.dp, GlassBorder)
                            ) {
                                Text("Сбросить изменения", color = Color.White, fontFamily = Manrope)
                            }
                        }
                    }

                    item {
                        Text(
                            "by KernelMod | Баг-репорт: @kernelmodEZ", 
                            color = BrandLightGray, 
                            fontSize = 12.sp,
                            fontFamily = Manrope,
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
            .background(GlassBackground, RoundedCornerShape(16.dp))
            .border(1.dp, if (isEditMode) BrandRed else GlassBorder, RoundedCornerShape(16.dp))
            .then(Modifier.clickable { onClick() })
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(60.dp)
        ) {
            Text(startTime, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp, fontFamily = Oswald)
            Text(endTime, color = BrandLightGray, fontSize = 12.sp, fontFamily = Manrope)
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        Box(modifier = Modifier.width(2.dp).height(40.dp).background(BrandRed.copy(alpha = 0.8f)))
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            val hasExamOrCredit = item.subject.contains("зач.", ignoreCase = true) || item.subject.contains("экз.", ignoreCase = true) || item.type.contains("зач", ignoreCase = true) || item.type.contains("экз", ignoreCase = true)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(item.type.uppercase(), color = BrandRed, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = Oswald)
                if (hasExamOrCredit) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.background(Color(0xFF8B0000), RoundedCornerShape(4.dp)).padding(horizontal = 4.dp, vertical = 2.dp)) {
                        Text("ЗАЧЕТ/ЭКЗАМЕН", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = Manrope)
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(item.subject, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp, fontFamily = Oswald, maxLines = 2)
            Spacer(modifier = Modifier.height(4.dp))
            Row {
                Text(item.room, color = BrandLightGray, fontSize = 12.sp, fontFamily = Manrope)
                Spacer(modifier = Modifier.width(8.dp))
                Text(item.teacher, color = BrandLightGray, fontSize = 12.sp, maxLines = 1, fontFamily = Manrope)
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
        title = { Text(if (initialClass == null) "Добавить пару" else "Редактировать", color = Color.White, fontFamily = Oswald) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                val textFieldColors = TextFieldDefaults.outlinedTextFieldColors(
                    textColor = Color.White,
                    unfocusedBorderColor = GlassBorder,
                    focusedBorderColor = BrandRed,
                    containerColor = GlassBackground
                )

                OutlinedTextField(
                    value = time, onValueChange = { time = it },
                    label = { Text("Время (напр. 09:00 - 10:30)", color = BrandLightGray, fontFamily = Manrope) },
                    textStyle = TextStyle(color = Color.White, fontFamily = Manrope),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    colors = textFieldColors,
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = subject, onValueChange = { subject = it },
                    label = { Text("Предмет", color = BrandLightGray, fontFamily = Manrope) },
                    textStyle = TextStyle(color = Color.White, fontFamily = Manrope),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    colors = textFieldColors,
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = type, onValueChange = { type = it },
                    label = { Text("Тип (Лекция/Практика)", color = BrandLightGray, fontFamily = Manrope) },
                    textStyle = TextStyle(color = Color.White, fontFamily = Manrope),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    colors = textFieldColors,
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = teacher, onValueChange = { teacher = it },
                    label = { Text("Преподаватель", color = BrandLightGray, fontFamily = Manrope) },
                    textStyle = TextStyle(color = Color.White, fontFamily = Manrope),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    colors = textFieldColors,
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = room, onValueChange = { room = it },
                    label = { Text("Кабинет", color = BrandLightGray, fontFamily = Manrope) },
                    textStyle = TextStyle(color = Color.White, fontFamily = Manrope),
                    modifier = Modifier.fillMaxWidth(),
                    colors = textFieldColors,
                    shape = RoundedCornerShape(12.dp)
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
                colors = ButtonDefaults.buttonColors(containerColor = BrandRed),
                modifier = Modifier.shadow(8.dp, RoundedCornerShape(24.dp), spotColor = BrandRed, ambientColor = BrandRed)
            ) { Text("Сохранить", color = Color.White, fontFamily = Manrope, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            Row {
                if (initialClass != null) {
                    TextButton(onClick = onDelete) { Text("Удалить", color = BrandRed, fontFamily = Manrope, fontWeight = FontWeight.Bold) }
                }
                TextButton(onClick = onDismiss) { Text("Отмена", color = BrandLightGray, fontFamily = Manrope) }
            }
        },
        containerColor = BrandDarkGray,
        shape = RoundedCornerShape(16.dp)
    )
}

class PreferencesManager(val context: Context) {
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
