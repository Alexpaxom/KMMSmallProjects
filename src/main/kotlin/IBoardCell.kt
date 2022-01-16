import org.w3c.dom.CanvasRenderingContext2D

sealed interface IBoardCell {
    val coordinate: GraphCellCoordinate
    fun draw(ctx: CanvasRenderingContext2D)

    companion object {
        const val CELL_WIDTH = 10.0
        const val CELL_HEIGHT = 10.0
    }
}