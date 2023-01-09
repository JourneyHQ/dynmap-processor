import kotlinx.serialization.Serializable

@Serializable
data class PlacementData(
    val xIndex: Int,
    val yIndex: Int
)

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
