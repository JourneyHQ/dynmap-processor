import kotlinx.serialization.Serializable

@Serializable
data class MapMetadata(
    val fullResolution: List<Int>,
    val chunkImageResolution: Int,
    val imageCount: List<Int>,
    val zoom: Int,
    val centralChunkPixel: PixelCoordinate
)