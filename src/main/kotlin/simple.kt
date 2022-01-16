import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.events.KeyboardEvent

fun main() {
    val canvas = document.getElementById("myCanvas") as HTMLCanvasElement
    val graphCellField = GraphCellField(50,50)
    val graphFieldEditor = GraphCellEditor(graphCellField)
    val ctx = canvas.getContext("2d") as CanvasRenderingContext2D
    var result = findShortestPath(graphCellField, GraphCellCoordinate(30,40), GraphCellCoordinate(4,5), console)
    var counter = -1

    window.onload = {

        canvas.onmousedown = { e->
                graphFieldEditor.onClickHandler(e.offsetX, e.offsetY)
                graphCellField.clearBoard()
                result = findShortestPath(graphCellField, GraphCellCoordinate(30,40), GraphCellCoordinate(4,5), console)
                console.log(result)
        }

        document.addEventListener("keydown", { event ->
            if(event is KeyboardEvent) {
                console.log("${event.keyCode}")
            }
        })
    }

    window.setInterval({
        counter = (counter + 1) % result.size
        result.getOrNull(counter)?.let{
        graphCellField.setListCells(listOf(
            it
        ))
        }

        graphCellField.drawField(ctx)
    }, 10)

}