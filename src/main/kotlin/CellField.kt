import org.w3c.dom.CanvasRenderingContext2D
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

data class GraphCellCoordinate(val x: Int, val y: Int) {
    val screenX = x * IBoardCell.CELL_WIDTH
    val screenY = y * IBoardCell.CELL_HEIGHT
}

class GraphCellEmpty(
    override val coordinate: GraphCellCoordinate
) : IBoardCell {

    override fun draw(ctx: CanvasRenderingContext2D) {
        with(ctx) {
            beginPath()
            fillStyle = "#000000"
            rect(coordinate.screenX, coordinate.screenY, IBoardCell.CELL_WIDTH, IBoardCell.CELL_HEIGHT)
            fill()
            closePath()
        }
    }
}

class GraphCellWall(
    override val coordinate: GraphCellCoordinate
) : IBoardCell {

    override fun draw(ctx: CanvasRenderingContext2D) {
        with(ctx) {
            beginPath()
            fillStyle = "#00FF00"
            rect(coordinate.screenX, coordinate.screenY, IBoardCell.CELL_WIDTH, IBoardCell.CELL_HEIGHT)
            fill()
            closePath()
        }
    }
}

class GraphCellCustomColorLine(
    override val coordinate: GraphCellCoordinate,
    val color: String = "#0000FF"
) : IBoardCell {

    override fun draw(ctx: CanvasRenderingContext2D) {
        with(ctx) {
            beginPath()
            fillStyle = color
            rect(coordinate.screenX, coordinate.screenY, IBoardCell.CELL_WIDTH, IBoardCell.CELL_HEIGHT)
            fill()
            closePath()
        }
    }
}


class GraphCellField(val width: Int, val height: Int) : ArrayList<ArrayList<IBoardCell>>() {

    init {
        this.apply {
            for (x in 0 until width) {
                val column = arrayListOf<IBoardCell>()
                for (y in 0 until height) {
                    column += GraphCellEmpty(
                        GraphCellCoordinate(x, y)
                    )
                }
                this += column
            }
        }
    }

    fun clearBoard() {
        for (x in 0 until width)
            for (y in 0 until height)
                if (this[x][y] is GraphCellCustomColorLine)
                    this[x][y] = GraphCellEmpty(GraphCellCoordinate(x, y))
    }

    fun setListCells(cells: List<IBoardCell>) {
        for (cell in cells) {
            this[cell.coordinate.x][cell.coordinate.y] = cell
        }
    }

    fun drawListCells(cxt: CanvasRenderingContext2D, cells: List<IBoardCell>) {
        for (cell in cells)
            cell.draw(cxt)
    }

    fun drawField(cxt: CanvasRenderingContext2D) {
        for (x in 0 until width)
            for (y in 0 until height)
                this[x][y].draw(cxt)
    }
}


class GraphCellEditor(private val targetBoard: GraphCellField) {

    fun setWallOnField(coordinates: GraphCellCoordinate) {
        when (targetBoard[coordinates.x][coordinates.y]) {
            is GraphCellWall -> {
                targetBoard[coordinates.x][coordinates.y] =
                    GraphCellEmpty(GraphCellCoordinate(coordinates.x, coordinates.y))
            }
            is GraphCellEmpty, is GraphCellCustomColorLine -> {
                targetBoard[coordinates.x][coordinates.y] =
                    GraphCellWall(GraphCellCoordinate(coordinates.x, coordinates.y))
            }
        }
    }

    fun transformScreenCoordinates(xClickPos: Double, yClickPos: Double): GraphCellCoordinate {
        val x = (xClickPos / (IBoardCell.CELL_WIDTH)).toInt()
        val y = (yClickPos / (IBoardCell.CELL_HEIGHT)).toInt()

        return GraphCellCoordinate(x, y)
    }
}

class GraphNodeInfo(
    val coordinate: GraphCellCoordinate,
    val weight: Int = -1,
    val fromPointCoordinate: GraphNodeInfo? = null,
    val pos: Int = 0,
    val approximateWeight: Int = -1,
) : Comparable<GraphNodeInfo> {
    override fun equals(other: Any?): Boolean {
        return if (other is GraphNodeInfo)
            coordinate == other.coordinate
        else
            false
    }

    override fun compareTo(other: GraphNodeInfo): Int {
        return weight.compareTo(other.weight)
    }
}

fun findShortestPath(
    targetBoard: GraphCellField,
    startCoordinate: GraphCellCoordinate,
    targetCoordinate: GraphCellCoordinate
): List<GraphNodeInfo> {
    val visitedNodes: MutableSet<GraphCellCoordinate> = mutableSetOf()
    val nodesForVisit: ArrayList<GraphNodeInfo> = arrayListOf()

    var currentNode = GraphNodeInfo(startCoordinate, 0, null)

    while (currentNode.coordinate != targetCoordinate) {
        val listLinkedNodes: List<IBoardCell?> = listOf(
            targetBoard.getOrNull(currentNode.coordinate.x + 1)?.getOrNull(currentNode.coordinate.y),
            targetBoard.getOrNull(currentNode.coordinate.x)?.getOrNull(currentNode.coordinate.y + 1),
            targetBoard.getOrNull(currentNode.coordinate.x - 1)?.getOrNull(currentNode.coordinate.y),
            targetBoard.getOrNull(currentNode.coordinate.x)?.getOrNull(currentNode.coordinate.y - 1)
        )

        for (graphCell in listLinkedNodes) {
            if (graphCell == null || graphCell.coordinate in visitedNodes)
                continue

            if (graphCell !is GraphCellEmpty) {
                visitedNodes.add(graphCell.coordinate)
                continue
            }

            val nodeInx = nodesForVisit.indexOfFirst { it.coordinate == graphCell.coordinate }
            if (nodeInx == -1)
                nodesForVisit.add(GraphNodeInfo(graphCell.coordinate, currentNode.weight + 1, currentNode))
            else if (nodesForVisit[nodeInx].weight > currentNode.weight + 1)
                nodesForVisit[nodeInx] = GraphNodeInfo(graphCell.coordinate, currentNode.weight + 1, currentNode)
        }

        visitedNodes.add(currentNode.coordinate)

        nodesForVisit.sort()
        if (nodesForVisit.isNotEmpty())
            currentNode = nodesForVisit.removeAt(0)
        else break
    }

    if (targetCoordinate != currentNode.coordinate)
        return listOf()

    val resultPathList: ArrayList<GraphNodeInfo> = arrayListOf()

    while (true) {
        resultPathList.add(currentNode)
        currentNode = currentNode.fromPointCoordinate ?: break
    }

    return resultPathList
}

fun calcShortestPathInfo(
    targetBoard: GraphCellField,
    startCoordinate: GraphCellCoordinate,
    targetCoordinate: GraphCellCoordinate
): List<GraphNodeInfo> {
    val visitedNodes: MutableSet<GraphCellCoordinate> = mutableSetOf()
    val nodesForVisit: ArrayList<GraphNodeInfo> = arrayListOf()
    val pathInfo: ArrayList<GraphNodeInfo> = arrayListOf()

    var currentNode = GraphNodeInfo(startCoordinate, 0, null)
    var counter = 0
    while (currentNode.coordinate != targetCoordinate) {
        val listLinkedNodes: List<IBoardCell?> = listOf(
            targetBoard.getOrNull(currentNode.coordinate.x + 1)?.getOrNull(currentNode.coordinate.y),
            targetBoard.getOrNull(currentNode.coordinate.x)?.getOrNull(currentNode.coordinate.y + 1),
            targetBoard.getOrNull(currentNode.coordinate.x - 1)?.getOrNull(currentNode.coordinate.y),
            targetBoard.getOrNull(currentNode.coordinate.x)?.getOrNull(currentNode.coordinate.y - 1)
        )

        for (graphCell in listLinkedNodes) {
            if (graphCell == null || graphCell.coordinate in visitedNodes)
                continue

            if (graphCell !is GraphCellEmpty) {
                visitedNodes.add(graphCell.coordinate)
                continue
            }

            val nodeInx = nodesForVisit.indexOfFirst { it.coordinate == graphCell.coordinate }
            if (nodeInx == -1) {
                nodesForVisit.add(GraphNodeInfo(graphCell.coordinate, currentNode.weight + 1, currentNode, counter))
                counter++
            }
            else if (nodesForVisit[nodeInx].weight > currentNode.weight + 1)
                nodesForVisit[nodeInx] = GraphNodeInfo(graphCell.coordinate, currentNode.weight + 1, currentNode, currentNode.pos)
        }

        visitedNodes.add(currentNode.coordinate)

        nodesForVisit.sort()
        if (nodesForVisit.isNotEmpty()) {
            pathInfo.add(currentNode)
            currentNode = nodesForVisit.removeAt(0)
        }
        else break
    }

    return pathInfo
}

fun calcShortestPathAstar(
    targetBoard: GraphCellField,
    startCoordinate: GraphCellCoordinate,
    targetCoordinate: GraphCellCoordinate
): List<GraphNodeInfo> {
    val visitedNodes: MutableSet<GraphCellCoordinate> = mutableSetOf()
    val nodesForVisit: ArrayList<GraphNodeInfo> = arrayListOf()

    var currentNode = GraphNodeInfo(startCoordinate, 0, null, manhattanDistance(startCoordinate, targetCoordinate))

    while (currentNode.coordinate != targetCoordinate) {
        val listLinkedNodes: List<IBoardCell?> = listOf(
            targetBoard.getOrNull(currentNode.coordinate.x + 1)?.getOrNull(currentNode.coordinate.y),
            targetBoard.getOrNull(currentNode.coordinate.x)?.getOrNull(currentNode.coordinate.y + 1),
            targetBoard.getOrNull(currentNode.coordinate.x - 1)?.getOrNull(currentNode.coordinate.y),
            targetBoard.getOrNull(currentNode.coordinate.x)?.getOrNull(currentNode.coordinate.y - 1)
        )

        for (graphCell in listLinkedNodes) {
            if (graphCell == null || graphCell.coordinate in visitedNodes)
                continue

            if (graphCell !is GraphCellEmpty) {
                visitedNodes.add(graphCell.coordinate)
                continue
            }

            val nodeInx = nodesForVisit.indexOfFirst { it.coordinate == graphCell.coordinate }
            if (nodeInx == -1)
                nodesForVisit.add(GraphNodeInfo(graphCell.coordinate, currentNode.weight + 1, currentNode, 0, manhattanDistance(graphCell.coordinate, targetCoordinate)+currentNode.weight + 1))
            else if (nodesForVisit[nodeInx].weight > currentNode.weight + 1)
                nodesForVisit[nodeInx] = GraphNodeInfo(graphCell.coordinate, currentNode.weight + 1, currentNode, 0, manhattanDistance(graphCell.coordinate, targetCoordinate)+currentNode.weight + 1)
        }

        visitedNodes.add(currentNode.coordinate)

        nodesForVisit.sortBy { it.approximateWeight }
        if (nodesForVisit.isNotEmpty())
            currentNode = nodesForVisit.removeAt(0)
        else break
    }

    if (targetCoordinate != currentNode.coordinate)
        return listOf()

    val resultPathList: ArrayList<GraphNodeInfo> = arrayListOf()

    while (true) {
        resultPathList.add(currentNode)
        currentNode = currentNode.fromPointCoordinate ?: break
    }

    return resultPathList
}

fun calcShortestPathInfoAstar(
    targetBoard: GraphCellField,
    startCoordinate: GraphCellCoordinate,
    targetCoordinate: GraphCellCoordinate
): List<GraphNodeInfo> {
    val visitedNodes: MutableSet<GraphCellCoordinate> = mutableSetOf()
    val nodesForVisit: ArrayList<GraphNodeInfo> = arrayListOf()
    val pathInfo: ArrayList<GraphNodeInfo> = arrayListOf()

    var currentNode = GraphNodeInfo(startCoordinate, 0, null, manhattanDistance(startCoordinate, targetCoordinate))
    var counter = 0
    while (currentNode.coordinate != targetCoordinate) {
        val listLinkedNodes: List<IBoardCell?> = listOf(
            targetBoard.getOrNull(currentNode.coordinate.x + 1)?.getOrNull(currentNode.coordinate.y),
            targetBoard.getOrNull(currentNode.coordinate.x)?.getOrNull(currentNode.coordinate.y + 1),
            targetBoard.getOrNull(currentNode.coordinate.x - 1)?.getOrNull(currentNode.coordinate.y),
            targetBoard.getOrNull(currentNode.coordinate.x)?.getOrNull(currentNode.coordinate.y - 1)
        )

        for (graphCell in listLinkedNodes) {
            if (graphCell == null || graphCell.coordinate in visitedNodes)
                continue

            if (graphCell !is GraphCellEmpty) {
                visitedNodes.add(graphCell.coordinate)
                continue
            }

            val nodeInx = nodesForVisit.indexOfFirst { it.coordinate == graphCell.coordinate }
            if (nodeInx == -1) {
                nodesForVisit.add(
                    GraphNodeInfo(
                        graphCell.coordinate,
                        currentNode.weight + 1,
                        currentNode,
                        counter,
                        manhattanDistance(graphCell.coordinate, targetCoordinate)+currentNode.weight + 1
                    )
                )
                counter++
            }
            else if (nodesForVisit[nodeInx].weight > currentNode.weight + 1)
                nodesForVisit[nodeInx] = GraphNodeInfo(graphCell.coordinate, currentNode.weight + 1, currentNode, currentNode.pos, manhattanDistance(graphCell.coordinate, targetCoordinate)+currentNode.weight + 1)
        }

        visitedNodes.add(currentNode.coordinate)

        nodesForVisit.sortBy { it.approximateWeight }
        if (nodesForVisit.isNotEmpty()) {
            pathInfo.add(currentNode)
            currentNode = nodesForVisit.removeAt(0)
        }
        else break
    }

    return pathInfo
}
//euclideanDistance
fun euclideanDistance(
    startCoordinate: GraphCellCoordinate,
    targetCoordinate: GraphCellCoordinate
): Int {
    return  sqrt(
        (startCoordinate.x-targetCoordinate.x).toDouble().pow(2)
            +
            (startCoordinate.y-targetCoordinate.y).toDouble().pow(2)
        ).toInt()
}
//manhattanDistance
fun manhattanDistance(
    startCoordinate: GraphCellCoordinate,
    targetCoordinate: GraphCellCoordinate
): Int {
    return abs(startCoordinate.x-targetCoordinate.x) + abs(startCoordinate.y-targetCoordinate.y)
}