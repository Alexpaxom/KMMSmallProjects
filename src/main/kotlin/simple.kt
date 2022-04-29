import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.events.KeyboardEvent

class Game {

    var currentGameStatus: GameStatus = WallsSet()

    val canvas = document.getElementById("myCanvas") as HTMLCanvasElement
    val graphCellField = GraphCellField(50,50)
    val graphFieldEditor = GraphCellEditor(graphCellField)
    val ctx = canvas.getContext("2d") as CanvasRenderingContext2D
    var shortestPath: List<GraphNodeInfo> = listOf()
    var shortestPathInfo: List<GraphNodeInfo> = listOf()
    var startPoint = GraphCellCoordinate(7,7)
    var endPoint = GraphCellCoordinate(4,5)
    var counter = 0
    var startDrawWall = false
    val pathInfoDrawLine: ArrayList<GraphCellCustomColorLine> = arrayListOf()
    var prevCoordinate: GraphCellCoordinate?= null
    var currentIntervalFunctionId: Int = 0

    init {
        window.onload = {
            setListeners()
            currentIntervalFunctionId = setIntervalFunction()
            Unit
        }

    }

    fun setListeners() {
        canvas.onmousedown = { e->
            val clickCoordinate = graphFieldEditor.transformScreenCoordinates(e.offsetX, e.offsetY)

            when(currentGameStatus) {
                is WallsSet ->{
                    startDrawWall = true
                    graphFieldEditor.setWallOnField(clickCoordinate)
                    prevCoordinate = clickCoordinate
                }
                is StartPointSet -> startPoint = clickCoordinate
                is EndPointSet -> endPoint = clickCoordinate
            }

            Unit
        }

        canvas.onmouseup = { e->
            when(currentGameStatus) {
                is WallsSet -> startDrawWall = false
            }
            Unit
        }



        canvas.onmousemove = { e->

            val moveCoordinate = graphFieldEditor.transformScreenCoordinates(e.offsetX, e.offsetY)

            if(startDrawWall && (prevCoordinate == null || prevCoordinate != moveCoordinate)) {

                prevCoordinate = moveCoordinate

                when (currentGameStatus) {
                    is WallsSet -> graphFieldEditor.setWallOnField(moveCoordinate)
                }
            }
            Unit
        }

        (document.getElementById("display") as HTMLButtonElement).addEventListener("click", {
            currentGameStatus = Started(true)
            graphCellField.drawField(ctx)
            shortestPath = calcShortestPathAstar(graphCellField, startPoint, endPoint)
            shortestPathInfo = calcShortestPathInfoAstar(graphCellField, startPoint, endPoint)
            counter = 0
            pathInfoDrawLine.clear()
            console.log("clicked")
        })

        (document.getElementById("set_start_point") as HTMLButtonElement).addEventListener("click", {
            currentGameStatus = StartPointSet()
        })

        (document.getElementById("set_end_point") as HTMLButtonElement).addEventListener("click", {
            currentGameStatus = EndPointSet()
        })

        (document.getElementById("set_wall") as HTMLButtonElement).addEventListener("click", {
            currentGameStatus = WallsSet()
        })

        (document.getElementById("clear") as HTMLButtonElement).addEventListener("click", {
            counter = 0
            shortestPath = listOf()
            shortestPathInfo = listOf()
            pathInfoDrawLine.clear()
        })

        (document.getElementById("display_frequency") as HTMLInputElement).let { displayFrequencyRange ->
            displayFrequencyRange.addEventListener("change", {
                window.clearInterval(currentIntervalFunctionId)
                currentIntervalFunctionId = setIntervalFunction((MAX_TIMEOUT+1 - displayFrequencyRange.valueAsNumber/100*MAX_TIMEOUT).toInt())
                console.log((MAX_TIMEOUT+1 - displayFrequencyRange.valueAsNumber/100*MAX_TIMEOUT).toInt())
            })
        }

        //        document.addEventListener("keydown", { event ->
        //            if(event is KeyboardEvent) {
        //                console.log("${event.keyCode}")
        //            }
        //        })
    }

    fun setIntervalFunction(drawFrequencyMilliseconds: Int = MAX_TIMEOUT/2): Int {

        return window.setInterval({
            graphCellField.drawField(ctx)

            if(shortestPathInfo.isNotEmpty() && counter<shortestPathInfo.size) {
                pathInfoDrawLine.add(GraphCellCustomColorLine(shortestPathInfo[counter].coordinate))
                counter++
            }

            graphCellField.drawListCells(ctx, pathInfoDrawLine)

            if(counter>=shortestPathInfo.size) {
                graphCellField.drawListCells(
                    ctx,
                    shortestPath.map { GraphCellCustomColorLine(it.coordinate, "#FFFF00") })
                if(currentGameStatus is Started)
                    currentGameStatus = WallsSet()
            }

        graphCellField.drawListCells(ctx, listOf(
            GraphCellCustomColorLine(startPoint, "#FF0000"),
            GraphCellCustomColorLine(endPoint, "#FFFFFF")
        ))

        }, drawFrequencyMilliseconds)
    }

    companion object {
        const val MAX_TIMEOUT = 1000
    }

}

fun main() {
    Game()
}

sealed interface GameStatus

class Started(val showCalculation: Boolean): GameStatus
class StartPointSet(): GameStatus
class EndPointSet(): GameStatus
class WallsSet(): GameStatus