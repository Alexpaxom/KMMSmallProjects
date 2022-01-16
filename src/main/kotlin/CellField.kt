import org.w3c.dom.CanvasRenderingContext2D
import kotlin.js.Console

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

class GraphCellPathLine(
    override val coordinate: GraphCellCoordinate
) : IBoardCell {

    override fun draw(ctx: CanvasRenderingContext2D) {
        with(ctx) {
            beginPath()
            fillStyle = "#0000FF"
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
                if(this[x][y] is GraphCellPathLine)
                    this[x][y] = GraphCellEmpty(GraphCellCoordinate(x, y))
    }

    fun setListCells(cells: List<IBoardCell>) {
        for(cell in cells) {
            this[cell.coordinate.x][cell.coordinate.y] = cell
        }
    }

    fun drawField(cxt: CanvasRenderingContext2D) {
        for (x in 0 until width)
            for (y in 0 until height)
                this[x][y].draw(cxt)
    }
}


class GraphCellEditor(private val targetBoard: GraphCellField) {
    fun onClickHandler(xClickPos: Double, yClickPos: Double) {
        val x = (xClickPos / (IBoardCell.CELL_WIDTH)).toInt()
        val y = (yClickPos / (IBoardCell.CELL_HEIGHT)).toInt()


        when (targetBoard[x][y]) {
            is GraphCellWall -> {
                targetBoard[x][y] =
                    GraphCellEmpty(GraphCellCoordinate(x, y))
            }
            is GraphCellEmpty, is GraphCellPathLine -> {
                targetBoard[x][y] =
                    GraphCellWall(GraphCellCoordinate(x, y))
            }
        }
    }
}

class GraphNodeInfo(val coordinate: GraphCellCoordinate, val weight: Int = -1, val fromPointCoordinate: GraphNodeInfo? = null): Comparable<GraphNodeInfo> {
    override fun equals(other: Any?): Boolean {
        return if(other is GraphNodeInfo)
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
    targetCoordinate: GraphCellCoordinate,
    console: Console
): List<GraphCellPathLine> {
    val visitedNodes: MutableSet<GraphCellCoordinate> = mutableSetOf()
    val nodesForVisit: ArrayList<GraphNodeInfo> = arrayListOf()

    var currentNode = GraphNodeInfo(startCoordinate, 0, null)
    var finishNode = GraphNodeInfo(targetCoordinate)

    while (currentNode != finishNode) {
        val listLinkedNodes: List<IBoardCell?> = listOf(
            targetBoard.getOrNull(currentNode.coordinate.x+1)?.getOrNull(currentNode.coordinate.y),
            targetBoard.getOrNull(currentNode.coordinate.x)?.getOrNull(currentNode.coordinate.y+1),
            targetBoard.getOrNull(currentNode.coordinate.x-1)?.getOrNull(currentNode.coordinate.y),
            targetBoard.getOrNull(currentNode.coordinate.x)?.getOrNull(currentNode.coordinate.y-1)
        )

        for(graphCell in listLinkedNodes) {
            if(graphCell == null || graphCell.coordinate in visitedNodes)
                continue

            if(graphCell !is GraphCellEmpty) {
                visitedNodes.add(graphCell.coordinate)
                continue
            }

            val nodeInx = nodesForVisit.indexOfFirst { it.coordinate == graphCell.coordinate }
            if(nodeInx == -1)
                nodesForVisit.add(GraphNodeInfo(graphCell.coordinate, currentNode.weight + 1, currentNode))
            else if(nodesForVisit[nodeInx].weight > currentNode.weight + 1)
                nodesForVisit[nodeInx] = GraphNodeInfo(graphCell.coordinate, currentNode.weight + 1, currentNode)
        }

        visitedNodes.add(currentNode.coordinate)

        nodesForVisit.sort()
        if(nodesForVisit.isNotEmpty())
            currentNode = nodesForVisit.removeAt(0)
        else break
    }

    if(finishNode.coordinate != currentNode.coordinate)
        return listOf()

    val resultPathList: ArrayList<GraphCellPathLine> = arrayListOf()

    while (true) {
        resultPathList.add(GraphCellPathLine(currentNode.coordinate))
        currentNode = currentNode.fromPointCoordinate ?: break
    }

    return resultPathList
}

