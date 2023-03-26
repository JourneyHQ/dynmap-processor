import kotlinx.serialization.Serializable
import java.io.File

data class ChunkImageFile(
    val x: Int,
    val y: Int,
    val file: File?
) {
    fun toPlacementData() = PlacementData(x, y)
}

@Serializable
data class PlacementData(
    val xIndex: Int,
    val yIndex: Int
) {
    fun chunksAround(multiplier: Int) = mutableListOf<PlacementData>().apply {
        add(PlacementData(xIndex + multiplier, yIndex))
        add(PlacementData(xIndex - multiplier, yIndex))
        add(PlacementData(xIndex, yIndex + multiplier))
        add(PlacementData(xIndex, yIndex - multiplier))
        add(PlacementData(xIndex + multiplier, yIndex + multiplier))
        add(PlacementData(xIndex - multiplier, yIndex + multiplier))
        add(PlacementData(xIndex + multiplier, yIndex - multiplier))
        add(PlacementData(xIndex - multiplier, yIndex - multiplier))
    }
}

@Serializable
data class PixelCoordinate(
    val x: Int,
    val y: Int
) {
    override fun toString() = "($x, $y)"

    operator fun minus(other: PixelCoordinate) = PixelCoordinate(x - other.x, y - other.y)
}

@Serializable
data class MinecraftCoordinate(
    val x: Int,
    val y: Int
) {
    override fun toString() = "($x, $y)"
}
