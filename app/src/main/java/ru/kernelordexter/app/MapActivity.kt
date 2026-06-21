package ru.kernelordexter.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.graphics.BitmapFactory

class MapActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val targetRoom = intent.getStringExtra("TARGET_ROOM") ?: "Неизвестно"

        setContent {
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
                    MapScreen(targetRoom = targetRoom, loadBitmap = {
                        val stream = assets.open("maps/floor_1.jpg")
                        BitmapFactory.decodeStream(stream)
                    })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(targetRoom: String, loadBitmap: () -> android.graphics.Bitmap) {
    val bitmap = remember { loadBitmap().asImageBitmap() }
    
    // Zoom & Pan state
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    // Mock graph data for demonstration
    // In production, this should be loaded from graph_data.json
    val nodes = listOf(
        MapNode("entrance", "Вход", 500f, 1000f),
        MapNode("hall", "Холл", 500f, 800f),
        MapNode("stairs_1", "Лестница 1", 300f, 800f),
        MapNode(targetRoom, targetRoom, 300f, 500f) // Mock coordinate for target room
    )
    val edges = listOf(
        MapEdge("entrance", "hall", 200.0),
        MapEdge("hall", "stairs_1", 200.0),
        MapEdge("stairs_1", targetRoom, 300.0)
    )
    
    val pathFinder = remember { PathFinder(nodes, edges) }
    val route = remember(targetRoom) { pathFinder.findPath("entrance", targetRoom) }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Навигация ВГУИТ", color = Color.White, fontFamily = Oswald) },
            colors = TopAppBarDefaults.smallTopAppBarColors(containerColor = BrandDarkGray)
        )
        
        Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text("Маршрут: Вход -> Кабинет $targetRoom", color = BrandLightGray, fontSize = 14.sp)
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(0.5f, 5f)
                        offset += pan
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
                // Map Image
                Image(
                    bitmap = bitmap,
                    contentDescription = "Map",
                    modifier = Modifier.fillMaxSize()
                )
                
                // Draw route on top
                if (route != null && route.size > 1) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val path = Path()
                        val startNode = route[0]
                        path.moveTo(startNode.x, startNode.y)
                        for (i in 1 until route.size) {
                            val node = route[i]
                            path.lineTo(node.x, node.y)
                        }
                        
                        drawPath(
                            path = path,
                            color = BrandRed,
                            style = Stroke(width = 8f / scale)
                        )
                        
                        drawCircle(color = Color.Green, radius = 12f / scale, center = Offset(route.first().x, route.first().y))
                        drawCircle(color = BrandRed, radius = 12f / scale, center = Offset(route.last().x, route.last().y))
                    }
                }
            }
        }
    }
}
