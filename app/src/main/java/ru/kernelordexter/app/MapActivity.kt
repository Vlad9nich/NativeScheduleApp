package ru.kernelordexter.app

import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStreamReader
import kotlin.math.roundToInt

// ─── Fonts ───────────────────────────────────────────────────────────────────

private val MapOswald = FontFamily(
    Font(R.font.oswald_medium, FontWeight.Medium),
    Font(R.font.oswald_bold, FontWeight.Bold)
)

private val MapManrope = FontFamily(
    Font(R.font.manrope_regular, FontWeight.Normal),
    Font(R.font.manrope_medium, FontWeight.Medium),
    Font(R.font.manrope_semibold, FontWeight.SemiBold),
    Font(R.font.manrope_bold, FontWeight.Bold),
    Font(R.font.manrope_extrabold, FontWeight.ExtraBold)
)

private val MapTypography = Typography(
    displayLarge = TextStyle(fontFamily = MapOswald),
    displayMedium = TextStyle(fontFamily = MapOswald),
    displaySmall = TextStyle(fontFamily = MapOswald),
    headlineLarge = TextStyle(fontFamily = MapOswald),
    headlineMedium = TextStyle(fontFamily = MapOswald),
    headlineSmall = TextStyle(fontFamily = MapOswald),
    titleLarge = TextStyle(fontFamily = MapOswald),
    titleMedium = TextStyle(fontFamily = MapOswald),
    titleSmall = TextStyle(fontFamily = MapOswald),
    bodyLarge = TextStyle(fontFamily = MapManrope),
    bodyMedium = TextStyle(fontFamily = MapManrope),
    bodySmall = TextStyle(fontFamily = MapManrope),
    labelLarge = TextStyle(fontFamily = MapManrope),
    labelMedium = TextStyle(fontFamily = MapManrope),
    labelSmall = TextStyle(fontFamily = MapManrope),
)

// ─── Data class for JSON deserialization ─────────────────────────────────────

data class GraphData(
    @SerializedName("floorMaps") val floorMaps: Map<String, String>,
    @SerializedName("nodes") val nodes: List<MapNode>,
    @SerializedName("edges") val edges: List<MapEdge>
)

// ─── Activity ────────────────────────────────────────────────────────────────

class MapActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val targetRoom = intent.getStringExtra("TARGET_ROOM") ?: "Неизвестно"

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = BrandBlack,
                    surface = BrandDarkGray,
                    surfaceVariant = Color(0xFF1E1E1E),
                    primary = BrandRed,
                    onBackground = Color.White,
                    onSurface = Color.White,
                    onPrimary = Color.White
                ),
                typography = MapTypography
            ) {
                val infiniteTransition = rememberInfiniteTransition(label = "bg_pulse")
                val alphaAnim by infiniteTransition.animateFloat(
                    initialValue = 0.04f,
                    targetValue = 0.08f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(4000, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "bg_alpha"
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(BrandBlack)
                        .drawBehind {
                            drawRect(
                                brush = Brush.radialGradient(
                                    colors = listOf(BrandRed.copy(alpha = alphaAnim), Color.Transparent),
                                    center = Offset(size.width / 2f, 0f),
                                    radius = size.height * 0.7f
                                )
                            )
                        }
                ) {
                    MapNavigationScreen(
                        targetRoom = targetRoom,
                        loadGraphData = {
                            val reader = InputStreamReader(assets.open("maps/graph_data.json"))
                            Gson().fromJson(reader, GraphData::class.java).also { reader.close() }
                        },
                        loadFloorBitmap = { fileName ->
                            val stream = assets.open("maps/$fileName")
                            BitmapFactory.decodeStream(stream).also { stream.close() }
                        },
                        onBack = { finish() }
                    )
                }
            }
        }
    }
}

// ─── Floor labels ────────────────────────────────────────────────────────────

private val FLOOR_LABELS = mapOf(
    0 to "Цоколь",
    1 to "1 этаж",
    2 to "2 этаж",
    3 to "3 этаж",
    4 to "4 этаж"
)

// ─── Main composable ────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapNavigationScreen(
    targetRoom: String,
    loadGraphData: () -> GraphData,
    loadFloorBitmap: (String) -> android.graphics.Bitmap,
    onBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    // ── State ────────────────────────────────────────────────────────────
    var graphData by remember { mutableStateOf<GraphData?>(null) }
    var graph by remember { mutableStateOf<Graph?>(null) }
    var routeNodes by remember { mutableStateOf<List<MapNode>>(emptyList()) }
    var selectedFloor by remember { mutableStateOf(1) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // ── Load data & compute path ─────────────────────────────────────────
    LaunchedEffect(targetRoom) {
        coroutineScope.launch {
            try {
                val data = withContext(Dispatchers.IO) { loadGraphData() }
                graphData = data

                val builtGraph = Graph(data.nodes, data.edges)
                graph = builtGraph

                // Find matching room node by id or name
                val roomNodeId = data.nodes.firstOrNull { node ->
                    node.id.equals(targetRoom, ignoreCase = true)
                }?.id ?: data.nodes.firstOrNull { node ->
                    node.name.equals(targetRoom, ignoreCase = true)
                }?.id ?: data.nodes.firstOrNull { node ->
                    node.name.equals("Room $targetRoom", ignoreCase = true)
                }?.id ?: data.nodes.firstOrNull { node ->
                    node.id.contains(targetRoom, ignoreCase = true) && node.type == "room"
                }?.id

                if (roomNodeId != null) {
                    val path = findShortestPath("entrance_F1", roomNodeId, builtGraph)
                    routeNodes = path
                    // Auto-select target room's floor
                    val targetNode = data.nodes.find { it.id == roomNodeId }
                    if (targetNode != null) {
                        selectedFloor = targetNode.floor
                    }
                } else {
                    errorMessage = "Кабинет «$targetRoom» не найден на карте"
                }
            } catch (e: Exception) {
                errorMessage = "Ошибка загрузки данных: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    // ── UI ────────────────────────────────────────────────────────────────
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Навигация ВГУИТ",
                            color = Color.White,
                            fontFamily = MapOswald,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Вход → Кабинет $targetRoom",
                            color = BrandLightGray,
                            fontFamily = MapManrope,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Назад",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.smallTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                color = BrandRed,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Построение маршрута…",
                                color = BrandLightGray,
                                fontFamily = MapManrope,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                errorMessage != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = BrandDarkGray),
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.LocationOn,
                                    contentDescription = null,
                                    tint = BrandRed,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = errorMessage ?: "",
                                    color = BrandLightGray,
                                    fontFamily = MapManrope,
                                    fontSize = 15.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                else -> {
                    // ── Floor selector ────────────────────────────────────
                    FloorSelectorRow(
                        selectedFloor = selectedFloor,
                        onFloorSelected = { selectedFloor = it }
                    )

                    // ── Map with route ────────────────────────────────────
                    Box(modifier = Modifier.weight(1f)) {
                        val gd = graphData
                        if (gd != null) {
                            val floorKey = selectedFloor.toString()
                            val floorFileName = gd.floorMaps[floorKey]

                            if (floorFileName != null) {
                                InteractiveMapView(
                                    floorFileName = floorFileName,
                                    loadFloorBitmap = loadFloorBitmap,
                                    routeNodes = routeNodes,
                                    selectedFloor = selectedFloor
                                )
                            } else {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Карта этажа недоступна",
                                        color = BrandLightGray,
                                        fontFamily = MapManrope
                                    )
                                }
                            }
                        }
                    }

                    // ── Route info card ───────────────────────────────────
                    if (routeNodes.isNotEmpty()) {
                        RouteInfoCard(routeNodes = routeNodes)
                    }
                }
            }
        }
    }
}

// ─── Floor selector row ──────────────────────────────────────────────────────

@Composable
private fun FloorSelectorRow(
    selectedFloor: Int,
    onFloorSelected: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.03f))
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        for (floor in 0..4) {
            val isSelected = floor == selectedFloor
            val bgColor by animateColorAsState(
                targetValue = if (isSelected) BrandRed else Color.Transparent,
                animationSpec = tween(durationMillis = 250),
                label = "floorBgColor"
            )
            val textColor by animateColorAsState(
                targetValue = if (isSelected) Color.White else BrandLightGray,
                animationSpec = tween(durationMillis = 250),
                label = "floorTextColor"
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .shadow(
                        elevation = if (isSelected) 8.dp else 0.dp,
                        shape = RoundedCornerShape(12.dp),
                        spotColor = BrandRed,
                        ambientColor = BrandRed
                    )
                    .clip(RoundedCornerShape(12.dp))
                    .background(bgColor)
                    .clickable { onFloorSelected(floor) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = FLOOR_LABELS[floor] ?: "$floor",
                    color = textColor,
                    fontFamily = MapManrope,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 12.sp,
                    maxLines = 1
                )
            }
        }
    }
}

// ─── Interactive map view ────────────────────────────────────────────────────

@Composable
private fun InteractiveMapView(
    floorFileName: String,
    loadFloorBitmap: (String) -> android.graphics.Bitmap,
    routeNodes: List<MapNode>,
    selectedFloor: Int
) {
    // Load floor bitmap (cached per floor file name)
    val imageBitmap = remember(floorFileName) {
        loadFloorBitmap(floorFileName).asImageBitmap()
    }

    // Zoom and pan state
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    // Reset zoom/pan when floor changes
    LaunchedEffect(selectedFloor) {
        scale = 1f
        offset = Offset.Zero
    }

    // Filter route nodes for current floor
    val floorRouteNodes = remember(routeNodes, selectedFloor) {
        filterRouteSegmentsForFloor(routeNodes, selectedFloor)
    }

    // Enclose map with simple glassmorphism to fit premium style
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.03f))
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val newScale = (scale * zoom).coerceIn(0.5f, 5f)
                    // Adjust pan for scale change
                    val scaleFactor = newScale / scale
                    val sf = scaleFactor.toFloat()
                    offset = Offset(
                        x = offset.x * sf + pan.x,
                        y = offset.y * sf + pan.y
                    )
                    scale = newScale
                }
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                )
        ) {
            // Floor map image
            Image(
                bitmap = imageBitmap,
                contentDescription = "Карта этажа $selectedFloor",
                modifier = Modifier.fillMaxSize()
            )

            // Route overlay canvas
            if (floorRouteNodes.isNotEmpty()) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val currentScale = scale.coerceAtLeast(0.5f)

                    // Draw route path segments
                    for (segment in floorRouteNodes) {
                        if (segment.size >= 2) {
                            // Draw glow / shadow path
                            val glowPath = Path().apply {
                                moveTo(segment[0].x, segment[0].y)
                                for (i in 1 until segment.size) {
                                    lineTo(segment[i].x, segment[i].y)
                                }
                            }
                            drawPath(
                                path = glowPath,
                                color = BrandRed.copy(alpha = 0.3f),
                                style = Stroke(
                                    width = 14f / currentScale.toFloat(),
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )

                            // Draw main path
                            val mainPath = Path().apply {
                                moveTo(segment[0].x, segment[0].y)
                                for (i in 1 until segment.size) {
                                    lineTo(segment[i].x, segment[i].y)
                                }
                            }
                            drawPath(
                                path = mainPath,
                                color = BrandRed,
                                style = Stroke(
                                    width = 6f / currentScale.toFloat(),
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                        }
                    }

                    // Find start and end nodes on this floor from full route
                    val firstNodeOnFloor = routeNodes.firstOrNull { it.floor == selectedFloor }
                    val lastNodeOnFloor = routeNodes.lastOrNull { it.floor == selectedFloor }

                    // Draw start circle (green) — only if entrance is on this floor
                    if (firstNodeOnFloor != null && routeNodes.first().floor == selectedFloor) {
                        // Outer glow
                        drawCircle(
                            color = Color(0xFF22C55E).copy(alpha = 0.25f),
                            radius = 24f / currentScale.toFloat(),
                            center = Offset(firstNodeOnFloor.x, firstNodeOnFloor.y)
                        )
                        // Outer ring
                        drawCircle(
                            color = Color(0xFF22C55E).copy(alpha = 0.5f),
                            radius = 16f / currentScale.toFloat(),
                            center = Offset(firstNodeOnFloor.x, firstNodeOnFloor.y),
                            style = Stroke(width = 3f / currentScale.toFloat())
                        )
                        // Inner fill
                        drawCircle(
                            color = Color(0xFF22C55E),
                            radius = 10f / currentScale.toFloat(),
                            center = Offset(firstNodeOnFloor.x, firstNodeOnFloor.y)
                        )
                    }

                    // Draw end circle (red) — only if destination is on this floor
                    if (lastNodeOnFloor != null && routeNodes.last().floor == selectedFloor) {
                        // Outer glow
                        drawCircle(
                            color = BrandRed.copy(alpha = 0.25f),
                            radius = 24f / currentScale.toFloat(),
                            center = Offset(lastNodeOnFloor.x, lastNodeOnFloor.y)
                        )
                        // Outer ring
                        drawCircle(
                            color = BrandRed.copy(alpha = 0.5f),
                            radius = 16f / currentScale.toFloat(),
                            center = Offset(lastNodeOnFloor.x, lastNodeOnFloor.y),
                            style = Stroke(width = 3f / currentScale.toFloat())
                        )
                        // Inner fill
                        drawCircle(
                            color = BrandRed,
                            radius = 10f / currentScale.toFloat(),
                            center = Offset(lastNodeOnFloor.x, lastNodeOnFloor.y)
                        )
                    }

                    // Draw transition markers (where route enters/exits this floor via stairs)
                    if (firstNodeOnFloor != null && routeNodes.first().floor != selectedFloor) {
                        // Route enters this floor (not the start)
                        drawCircle(
                            color = Color(0xFFFFA726).copy(alpha = 0.4f),
                            radius = 20f / currentScale.toFloat(),
                            center = Offset(firstNodeOnFloor.x, firstNodeOnFloor.y)
                        )
                        drawCircle(
                            color = Color(0xFFFFA726),
                            radius = 10f / currentScale.toFloat(),
                            center = Offset(firstNodeOnFloor.x, firstNodeOnFloor.y)
                        )
                    }
                    if (lastNodeOnFloor != null && routeNodes.last().floor != selectedFloor) {
                        // Route exits this floor (not the destination)
                        drawCircle(
                            color = Color(0xFFFFA726).copy(alpha = 0.4f),
                            radius = 20f / currentScale.toFloat(),
                            center = Offset(lastNodeOnFloor.x, lastNodeOnFloor.y)
                        )
                        drawCircle(
                            color = Color(0xFFFFA726),
                            radius = 10f / currentScale.toFloat(),
                            center = Offset(lastNodeOnFloor.x, lastNodeOnFloor.y)
                        )
                    }
                }
            }
        }

        // Zoom indicator
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White.copy(alpha = 0.1f))
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(
                text = "${(scale * 100).roundToInt()}%",
                color = BrandLightGray,
                fontFamily = MapManrope,
                fontSize = 11.sp
            )
        }
    }
}

// ─── Route info card ─────────────────────────────────────────────────────────

@Composable
private fun RouteInfoCard(routeNodes: List<MapNode>) {
    val floorsTraversed = remember(routeNodes) {
        routeNodes.map { it.floor }.distinct().sorted()
    }

    // Estimate time: sum of approximate segment distances → speed ~80 px/s ≈ 1m/s
    // Simple heuristic: count nodes * ~5 seconds per segment, add floor transitions
    val estimatedTimeSeconds = remember(routeNodes) {
        var totalDistance = 0f
        for (i in 1 until routeNodes.size) {
            val dx = routeNodes[i].x - routeNodes[i - 1].x
            val dy = routeNodes[i].y - routeNodes[i - 1].y
            totalDistance += kotlin.math.sqrt(dx * dx + dy * dy)
        }
        // Rough estimate: ~2 pixels per meter, walking ~1.2 m/s
        val walkingMeters = totalDistance / 2f
        val walkingSeconds = walkingMeters / 1.2f
        // Add 15 seconds per floor transition
        val floorTransitions = (floorsTraversed.size - 1).coerceAtLeast(0)
        (walkingSeconds + floorTransitions * 15).roundToInt()
    }

    val estimatedMinutes = estimatedTimeSeconds / 60
    val estimatedSecondsRemainder = estimatedTimeSeconds % 60

    val timeText = if (estimatedMinutes > 0) {
        "$estimatedMinutes мин $estimatedSecondsRemainder сек"
    } else {
        "$estimatedSecondsRemainder сек"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 12.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Route icon with red glow
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .shadow(
                        elevation = 12.dp,
                        shape = CircleShape,
                        spotColor = BrandRed,
                        ambientColor = BrandRed
                    )
                    .clip(CircleShape)
                    .background(BrandRed.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Place,
                    contentDescription = null,
                    tint = BrandRed,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Информация о маршруте",
                    color = Color.White,
                    fontFamily = MapOswald,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Floors traversed
                    Column {
                        Text(
                            text = "Этажей",
                            color = BrandLightGray,
                            fontFamily = MapManrope,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "${floorsTraversed.size}",
                            color = Color.White,
                            fontFamily = MapManrope,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    // Estimated time
                    Column {
                        Text(
                            text = "Примерное время",
                            color = BrandLightGray,
                            fontFamily = MapManrope,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "≈ $timeText",
                            color = Color.White,
                            fontFamily = MapManrope,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}

// ─── Helper: split route into segments per floor ─────────────────────────────

private fun filterRouteSegmentsForFloor(
    routeNodes: List<MapNode>,
    floor: Int
): List<List<MapNode>> {
    if (routeNodes.isEmpty()) return emptyList()

    val segments = mutableListOf<List<MapNode>>()
    var currentSegment = mutableListOf<MapNode>()

    for (node in routeNodes) {
        if (node.floor == floor) {
            currentSegment.add(node)
        } else {
            if (currentSegment.isNotEmpty()) {
                segments.add(currentSegment.toList())
                currentSegment = mutableListOf()
            }
        }
    }
    if (currentSegment.isNotEmpty()) {
        segments.add(currentSegment.toList())
    }

    return segments
}
