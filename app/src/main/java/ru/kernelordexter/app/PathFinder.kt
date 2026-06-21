package ru.kernelordexter.app

import java.util.PriorityQueue

data class MapNode(
    val id: String,
    val name: String,
    val x: Float,
    val y: Float
)

data class MapEdge(
    val fromId: String,
    val toId: String,
    val weight: Double
)

class PathFinder(private val nodes: List<MapNode>, private val edges: List<MapEdge>) {
    private val adjacencyList = mutableMapOf<String, MutableList<Pair<String, Double>>>()

    init {
        for (edge in edges) {
            adjacencyList.computeIfAbsent(edge.fromId) { mutableListOf() }.add(edge.toId to edge.weight)
            adjacencyList.computeIfAbsent(edge.toId) { mutableListOf() }.add(edge.fromId to edge.weight) // Assuming undirected
        }
    }

    private fun heuristicDistance(a: MapNode, b: MapNode): Double {
        return Math.hypot((a.x - b.x).toDouble(), (a.y - b.y).toDouble())
    }

    fun findPath(startId: String, endId: String): List<MapNode>? {
        val startNode = nodes.find { it.id == startId } ?: return null
        val endNode = nodes.find { it.id == endId } ?: return null

        val openSet = PriorityQueue<Pair<Double, MapNode>>(compareBy { it.first })
        val cameFrom = mutableMapOf<String, String>()

        val gScore = mutableMapOf<String, Double>().withDefault { Double.POSITIVE_INFINITY }
        gScore[startId] = 0.0

        val fScore = mutableMapOf<String, Double>().withDefault { Double.POSITIVE_INFINITY }
        fScore[startId] = heuristicDistance(startNode, endNode)

        openSet.add(fScore[startId]!! to startNode)

        while (openSet.isNotEmpty()) {
            val current = openSet.poll()?.second ?: break

            if (current.id == endId) {
                return reconstructPath(cameFrom, current.id)
            }

            val neighbors = adjacencyList[current.id] ?: emptyList()
            for ((neighborId, weight) in neighbors) {
                val tentativeGScore = gScore.getValue(current.id) + weight
                if (tentativeGScore < gScore.getValue(neighborId)) {
                    cameFrom[neighborId] = current.id
                    gScore[neighborId] = tentativeGScore
                    val neighborNode = nodes.find { it.id == neighborId }!!
                    fScore[neighborId] = tentativeGScore + heuristicDistance(neighborNode, endNode)
                    
                    if (openSet.none { it.second.id == neighborId }) {
                        openSet.add(fScore.getValue(neighborId) to neighborNode)
                    }
                }
            }
        }
        return null // No path
    }

    private fun reconstructPath(cameFrom: Map<String, String>, currentId: String): List<MapNode> {
        val path = mutableListOf<String>(currentId)
        var curr = currentId
        while (cameFrom.containsKey(curr)) {
            curr = cameFrom[curr]!!
            path.add(0, curr)
        }
        return path.map { id -> nodes.find { it.id == id }!! }
    }
}
