package ru.kernelordexter.app

import kotlin.math.abs
import kotlin.math.hypot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class MapNode(
    val id: String,
    val name: String,
    val floor: Int,
    val x: Float,
    val y: Float,
    val type: String
)

data class MapEdge(
    val fromId: String,
    val toId: String,
    val weight: Float,
    val type: String
)

internal class EdgeInt(val toId: Int, val weight: Float)

class Graph(nodes: List<MapNode>, edges: List<MapEdge>) {

    val nodeToIndex: Map<String, Int>
    val indexToNode: Array<MapNode>
    val adjacencyList: Array<MutableList<EdgeInt>>
    val nodeCount: Int

    init {
        nodeCount = nodes.size
        nodeToIndex = HashMap<String, Int>(nodeCount * 2).also { map ->
            nodes.forEachIndexed { index, node ->
                map[node.id] = index
            }
        }
        indexToNode = Array(nodeCount) { i -> nodes[i] }
        adjacencyList = Array(nodeCount) { mutableListOf<EdgeInt>() }

        for (edge in edges) {
            val fromIndex = nodeToIndex[edge.fromId] ?: continue
            val toIndex = nodeToIndex[edge.toId] ?: continue
            adjacencyList[fromIndex].add(EdgeInt(toIndex, edge.weight))
            adjacencyList[toIndex].add(EdgeInt(fromIndex, edge.weight))
        }
    }
}

class FloatMinHeap(initialCapacity: Int = 128) {

    private var elements = IntArray(initialCapacity)
    private var priorities = FloatArray(initialCapacity)
    private var size = 0

    fun add(element: Int, priority: Float) {
        if (size == elements.size) {
            val newCapacity = elements.size * 2
            elements = elements.copyOf(newCapacity)
            priorities = priorities.copyOf(newCapacity)
        }
        elements[size] = element
        priorities[size] = priority
        siftUp(size)
        size++
    }

    fun poll(): Int {
        if (size == 0) return -1
        val result = elements[0]
        size--
        if (size > 0) {
            elements[0] = elements[size]
            priorities[0] = priorities[size]
            siftDown(0)
        }
        return result
    }

    fun isNotEmpty(): Boolean = size > 0

    private fun siftUp(index: Int) {
        var i = index
        while (i > 0) {
            val parent = (i - 1) / 2
            if (priorities[i] < priorities[parent]) {
                swap(i, parent)
                i = parent
            } else {
                break
            }
        }
    }

    private fun siftDown(index: Int) {
        var i = index
        while (true) {
            val left = 2 * i + 1
            val right = 2 * i + 2
            var smallest = i

            if (left < size && priorities[left] < priorities[smallest]) {
                smallest = left
            }
            if (right < size && priorities[right] < priorities[smallest]) {
                smallest = right
            }
            if (smallest != i) {
                swap(i, smallest)
                i = smallest
            } else {
                break
            }
        }
    }

    private fun swap(i: Int, j: Int) {
        val tmpElement = elements[i]
        elements[i] = elements[j]
        elements[j] = tmpElement

        val tmpPriority = priorities[i]
        priorities[i] = priorities[j]
        priorities[j] = tmpPriority
    }
}

fun calculateHeuristic(fromIndex: Int, toIndex: Int, graph: Graph): Float {
    val fromNode = graph.indexToNode[fromIndex]
    val toNode = graph.indexToNode[toIndex]
    val dx = (fromNode.x - toNode.x).toDouble()
    val dy = (fromNode.y - toNode.y).toDouble()
    val euclidean = hypot(dx, dy).toFloat()
    val floorPenalty = abs(fromNode.floor - toNode.floor) * 150f
    return euclidean + floorPenalty
}

suspend fun findShortestPath(startId: String, endId: String, graph: Graph): List<MapNode> =
    withContext(Dispatchers.Default) {
        val startIndex = graph.nodeToIndex[startId] ?: return@withContext emptyList()
        val endIndex = graph.nodeToIndex[endId] ?: return@withContext emptyList()

        val nodeCount = graph.nodeCount
        val cameFrom = IntArray(nodeCount) { -1 }
        val gScore = FloatArray(nodeCount) { Float.MAX_VALUE }

        gScore[startIndex] = 0f

        val openSet = FloatMinHeap(nodeCount)
        openSet.add(startIndex, calculateHeuristic(startIndex, endIndex, graph))

        while (openSet.isNotEmpty()) {
            val current = openSet.poll()

            if (current == endIndex) {
                return@withContext reconstructPath(cameFrom, current, graph)
            }

            val currentGScore = gScore[current]

            for (edge in graph.adjacencyList[current]) {
                val tentativeGScore = currentGScore + edge.weight
                if (tentativeGScore < gScore[edge.toId]) {
                    cameFrom[edge.toId] = current
                    gScore[edge.toId] = tentativeGScore
                    val fScore = tentativeGScore + calculateHeuristic(edge.toId, endIndex, graph)
                    openSet.add(edge.toId, fScore)
                }
            }
        }

        emptyList()
    }

private fun reconstructPath(cameFrom: IntArray, current: Int, graph: Graph): List<MapNode> {
    val path = mutableListOf<MapNode>()
    var node = current
    while (node != -1) {
        path.add(graph.indexToNode[node])
        node = cameFrom[node]
    }
    path.reverse()
    return path
}
