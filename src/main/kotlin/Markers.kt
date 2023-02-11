import kotlinx.serialization.Serializable

@Serializable
data class Marker(
    val type: MarkerType,
    val name: String,
    val points: List<MinecraftCoordinate>,
    val color: Color,
    val overlay: Color
)

@Serializable
enum class MarkerType {
    Area, Line, Circle
}

@Serializable
data class Color(
    val r: Int,
    val g: Int,
    val b: Int,
    val a: Int
) {
    init {
        if (!(r in 0..255 && g in 0..255 && b in 0..255 && a in 0..100))
            throw IllegalArgumentException("RGB value must be in the range of 0 to 255, and alpha is 0 to 100.")
    }
}