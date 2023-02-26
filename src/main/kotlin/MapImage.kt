import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.canvas.drawables.FilledRect
import com.sksamuel.scrimage.canvas.drawables.Line
import com.sksamuel.scrimage.canvas.drawables.Oval
import com.sksamuel.scrimage.canvas.drawables.Text
import com.sksamuel.scrimage.nio.PngWriter
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.Json.Default.encodeToString
import markers.*
import java.awt.Color
import java.io.File
import java.rmi.UnexpectedException
import kotlin.io.path.Path
import kotlin.io.path.createDirectory
import kotlin.io.path.exists
import kotlin.math.pow
import kotlin.math.roundToInt

data class ChunkImageFile(
    val x: Int,
    val y: Int,
    val file: File?
)

/**
 * @param path the directory path to store this map data. (like ./map_images/2023_1_4_13_14/)
 * @param chunkImagePath the directory path to chunk images.
 */
class MapImage(
    val path: String,
    val chunkImagePath: String,
    val mapImage: ImmutableImage,
    val metadata: MapMetadata
) {

    private val temp = Path(path, "temp").let {
        if (it.exists()) File(it.toUri())
        else it.createDirectory()
    }

    private fun MinecraftCoordinate.toPixelCoordinate(): PixelCoordinate {
        val chunks = 2.toDouble().pow(metadata.zoom + 2).toInt()
        val centralChunkMinecraft = MinecraftCoordinate(0, -64)

        val centralToPoint = MinecraftCoordinate(
            this.x - centralChunkMinecraft.x,
            this.y - centralChunkMinecraft.y
        )

        val mcToPixelRate = metadata.chunkImageResolution.toDouble() / (chunks * 16)
        val pixelToMcRate = 1 / mcToPixelRate

        val centralToCorner = MinecraftCoordinate(
            (-(metadata.centralChunkPixel.x * pixelToMcRate) - centralChunkMinecraft.x).toInt(),
            (-(metadata.centralChunkPixel.y * pixelToMcRate) - centralChunkMinecraft.y).toInt()
        )

        val cornerToPoint = MinecraftCoordinate(
            (centralToPoint.x - centralToCorner.x) - centralChunkMinecraft.x,
            (centralToPoint.y - centralToCorner.y) - centralChunkMinecraft.y
        )

        val xPixel = cornerToPoint.x * mcToPixelRate
        val yPixel = cornerToPoint.y * mcToPixelRate

        // check if the calculated coordinate is out of the map
        val xFullResolution = metadata.fullResolution[0]
        val yFullResolution = metadata.fullResolution[1]

        if (!((0 <= xPixel && xPixel <= xFullResolution - 1)
                    && (0 <= yPixel && yPixel <= yFullResolution - 1))
        ) throw UnsupportedOperationException(
            "x=$xPixel, y=$yPixel is out of the map (${xFullResolution}*${yFullResolution})"
        )


        return PixelCoordinate(xPixel.roundToInt(), yPixel.roundToInt())
    }

    companion object {
        val basemapFile = { parent: String -> File(Path(parent, "basemap.png").toUri()) }
        val metadataFile = { parent: String -> File(Path(parent, "metadata.json").toUri()) }

        private fun collectImages(path: String): MutableList<ChunkImageFile> {
            val files = File(path).listFiles()
                ?: throw UnsupportedOperationException("Images under $path not found.")

            val chunkImageFiles = mutableListOf<ChunkImageFile>()

            for (file in files) {
                val regex = Regex("^(-?[0-9]+)_(-?[0-9]+)\\.png$")
                val matchResult = regex.matchEntire(file.name)
                if (matchResult == null) {
                    println("Skip illegal file name: ${file.name}")
                    continue
                }

                val data = matchResult.groups.map { it?.value } // ex: ["1_-4.png", "1", "-4"]
                chunkImageFiles.add(ChunkImageFile(data[1]!!.toInt(), data[2]!!.toInt(), File("$path/${data[0]!!}")))
            }

            return chunkImageFiles
        }

        /**
         * Generate new map image from settings (or overwrite if it already exists)
         *
         * @param path the directory path to store this map data. (like ./map_images/2023_1_4_13_14/)
         * @param chunkImagePath the directory path to chunk images.
         * @param zoom the zoom level of the map. (0~4)
         * @param chunkImageResolution the resolution of chunk images.
         */
        fun create(
            path: String,
            chunkImagePath: String,
            zoom: Int,
            chunkImageResolution: Int
        ): MapImage {
            val root = Path(path)
            if (!root.exists()) root.createDirectory()

            val imageChunks = 2.toDouble().pow(zoom).toInt() // zoom-n -> 2^n
            val imageFiles = collectImages(Path(chunkImagePath, "zoom-$zoom").toString())

            val xMapData = imageFiles.sortedWith(compareBy<ChunkImageFile> { it.x }.thenBy { it.y }).groupBy { it.x }
            val yMapData = imageFiles.sortedWith(compareBy<ChunkImageFile> { it.y }.thenBy { it.x }).groupBy { it.y }

            val xZeroArea = xMapData[0]
            val yZeroArea = yMapData[0]

            if (!(xZeroArea != null && yZeroArea != null))
                throw UnsupportedOperationException("The center of the map (0,0) could not be found!")

            val xKeys = xMapData.keys
            val yKeys = yMapData.keys

            val xImages = (xKeys.max() - xKeys.min()) / imageChunks
            val yImages = (yKeys.max() - yKeys.min()) / imageChunks

            val xFullResolution = xImages * chunkImageResolution
            val yFullResolution = yImages * chunkImageResolution

            val mapImages = mutableListOf<Pair<PlacementData, ChunkImageFile>>()

            for (x in 0 until xImages) {
                for (y in 0 until yImages) {
                    val xChunk = xKeys.min() + imageChunks * x
                    val yChunk = yKeys.min() + imageChunks * y
                    mapImages.add(PlacementData(x, y) to (imageFiles.find {
                        it.x == xChunk && it.y == yChunk
                    } ?: ChunkImageFile(xChunk, yChunk, null)))
                }
            }

            var baseMap = ImmutableImage.create(xFullResolution, yFullResolution).fill(Color.BLACK)
                ?: throw UnexpectedException("Base image generation failed.")

            lateinit var centralChunkPixel: PixelCoordinate // already confirmed that 0_0.png exists

            for (mapImage in mapImages) {
                val file = mapImage.second.file ?: continue

                val isCentralChunk = mapImage.second.x == 0 && mapImage.second.y == 0

                val chunkMap = ImmutableImage.loader().fromFile(file).map {
                    if (it.x == 0 || it.y == 0)
                        if (isCentralChunk) Color.BLUE else Color.RED
                    else it.toColor().awt()
                }

                val pixelCoordinate = PixelCoordinate(
                    mapImage.first.xIndex * chunkImageResolution,
                    yFullResolution - mapImage.first.yIndex * chunkImageResolution
                ) // reversed

                baseMap = baseMap.overlay(
                    chunkMap,
                    pixelCoordinate.x,
                    pixelCoordinate.y
                )

                if (isCentralChunk) centralChunkPixel = pixelCoordinate
            }

            val metadata = MapMetadata(
                listOf(xFullResolution, yFullResolution),
                chunkImageResolution,
                listOf(xImages, yImages),
                zoom,
                centralChunkPixel
            )

            baseMap.output(PngWriter.NoCompression, basemapFile(path))
            metadataFile(path).writeText(encodeToString(MapMetadata.serializer(), metadata))

            print("Basemap generate complete.")
            return MapImage(path, chunkImagePath, baseMap, metadata)
        }

        /**
         * Load the map from existing image and metadata.
         *
         * @param path the directory path where the map data stored. (like ./map_images/2023_1_4_13_14/)
         * @param chunkImagePath the directory path to chunk images.
         */
        fun load(path: String, chunkImagePath: String): MapImage {
            val baseMap = ImmutableImage.loader().fromFile(basemapFile(path))
            val metadata = Json.decodeFromString(MapMetadata.serializer(), metadataFile(path).readText())

            println("Basemap load complete.")
            return MapImage(path, chunkImagePath, baseMap, metadata)
        }
    }

    fun edit(
        markers: Markers,
        height: Int?,
        width: Int?,
        trim: List<Int>?, // size == 4
        resize: Double
    ): ImmutableImage {
        var image = mapImage

        // marker
        for (marker in markers.markers) {
            image = drawMarker(image, marker)
        }

        // trim
        if (trim != null) {
            val start = MinecraftCoordinate(trim[0], trim[1]).toPixelCoordinate()
            val end = MinecraftCoordinate(trim[2], trim[3]).toPixelCoordinate()

            val xCoordinates = listOf(start.x, end.x).sorted()
            val yCoordinates = listOf(start.y, end.y).sorted()

            image = image.trim(
                xCoordinates[0],
                yCoordinates[0],
                (image.width - xCoordinates[1]),
                (image.height - yCoordinates[1])
            )
        }

        // height and width
        image = when {
            width != null && height == null -> image.scaleToWidth(width)
            width == null && height != null -> image.scaleToHeight(height)
            width != null && height != null -> image.scaleTo(width, height)
            else -> image
        }

        // resize
        image = image.resize(resize)

        return image
    }

    private fun drawMarker(image: ImmutableImage, marker: Marker): ImmutableImage {
        val pixelCoordinates = marker.coordinates.map { it.toPixelCoordinate() }
        return when (marker.type) {
            MarkerType.Area -> {
                val (xLines, yLines) = optimizeAreaLines(marker.name, marker.coordinates)
                drawArea(marker, pixelCoordinates, image, xLines, yLines)
            }

            MarkerType.Line ->
                image.toCanvas().draw(
                    Line(pixelCoordinates[0].x, pixelCoordinates[0].y, pixelCoordinates[1].x, pixelCoordinates[1].y) {
                        it.color = marker.color.toJavaColor()
                    }
                ).image

            MarkerType.Circle ->
                image.toCanvas().draw(
                    Oval(pixelCoordinates[0].x, pixelCoordinates[0].y, marker.radius, marker.radius) {
                        it.color = marker.color.toJavaColor()
                        it.background = marker.overlay.toJavaColor()
                    }
                ).image
        }
    }

    private fun drawArea(
        marker: Marker,
        pixelCoordinates: List<PixelCoordinate>,
        mapImage: ImmutableImage,
        xLines: List<LineMarker>,
        yLines: List<LineMarker>
    ): ImmutableImage {
        val xList = pixelCoordinates.map { it.x }
        val yList = pixelCoordinates.map { it.y }

        val originPixel = PixelCoordinate(xList.min(), yList.min())

        val width = xList.max() - xList.min() + 1
        val height = yList.max() - yList.min() + 1

        val pixelImageArea = PixelCoordinate(width, height)

        val textImage = ImmutableImage.create(pixelImageArea.x, 9)
            .toCanvas().draw(
                Text(marker.name, 0, 9) {
                    it.color = marker.color.toJavaColor()
                },
                FilledRect(0, 0, pixelImageArea.x, 9) {
                    it.color = marker.overlay.toJavaColor()
                }
            ).image

        val areaImage = ImmutableImage.create(pixelImageArea.x, pixelImageArea.y)

        val xPixelLines =
            xLines.map { (it.first.toPixelCoordinate() - originPixel) to (it.second.toPixelCoordinate() - originPixel) }
        val yPixelLines =
            yLines.map { (it.first.toPixelCoordinate() - originPixel) to (it.second.toPixelCoordinate() - originPixel) }

        return mapImage.overlay(areaImage.map { pixel ->
            val xLinesInRange =
                xPixelLines.any { (it.first.x <= pixel.x && pixel.x <= it.second.x) && pixel.y == it.first.y }
            val yLinesInRange =
                yPixelLines.any { (it.first.y <= pixel.y && pixel.y <= it.second.y) && pixel.x == it.first.x }

            val isLinesAbove =
                xPixelLines.any { pixel.y > it.first.y && (it.first.x <= pixel.x && pixel.x <= it.second.x) }
            val isLinesBelow =
                xPixelLines.any { pixel.y < it.first.y && (it.first.x <= pixel.x && pixel.x <= it.second.x) }
            val isLinesLeft =
                yPixelLines.any { pixel.x > it.first.x && (it.first.y <= pixel.y && pixel.y <= it.second.y) }
            val isLinesRight =
                yPixelLines.any { pixel.x < it.first.x && (it.first.y <= pixel.y && pixel.y <= it.second.y) }

            if (xLinesInRange || yLinesInRange) marker.color.toJavaColor() // this pixel is line.
            else if (isLinesAbove && isLinesBelow && isLinesLeft && isLinesRight) marker.overlay.toJavaColor() // this pixel is in the area to fill.
            else pixel.toColor().awt() // this pixel should be ignored

        }.overlay(textImage, 2, 2), originPixel.x, originPixel.y)
    }

//    fun createAreaImage(vararg areas: Area) {
//        var areaMapImage = mapImage
//        for (area in areas) {
//            val pixelCoordinates = area.coordinates.map { it.toPixelCoordinate() }
//            val xList = pixelCoordinates.map { it.x }
//            val yList = pixelCoordinates.map { it.y }
//
//            val originPixel = PixelCoordinate(xList.min(), yList.min())
//
//            val width = xList.max() - xList.min() + 1
//            val height = yList.max() - yList.min() + 1
//
//            val pixelImageArea = PixelCoordinate(width, height)
//
//            val textImage = ImmutableImage.create(pixelImageArea.x, 9)
//                .toCanvas().draw(
//                    Text(area.name, 0, 9) {
//                        it.color = area.color
//                    },
//                    FilledRect(0, 0, pixelImageArea.x, 9) {
//                        it.color = area.overlay
//                    }
//                ).image
//
//            val baseImage = ImmutableImage.create(pixelImageArea.x, pixelImageArea.y)
//
//            val xPixelLines =
//                area.xLines.map { (it.first.toPixelCoordinate() - originPixel) to (it.second.toPixelCoordinate() - originPixel) }
//            val yPixelLines =
//                area.yLines.map { (it.first.toPixelCoordinate() - originPixel) to (it.second.toPixelCoordinate() - originPixel) }
//
//            areaMapImage = areaMapImage.overlay(baseImage.map { pixel ->
//                val xLinesInRange =
//                    xPixelLines.any { (it.first.x <= pixel.x && pixel.x <= it.second.x) && pixel.y == it.first.y }
//                val yLinesInRange =
//                    yPixelLines.any { (it.first.y <= pixel.y && pixel.y <= it.second.y) && pixel.x == it.first.x }
//
//                val isLinesAbove =
//                    xPixelLines.any { pixel.y > it.first.y && (it.first.x <= pixel.x && pixel.x <= it.second.x) }
//                val isLinesBelow =
//                    xPixelLines.any { pixel.y < it.first.y && (it.first.x <= pixel.x && pixel.x <= it.second.x) }
//                val isLinesLeft =
//                    yPixelLines.any { pixel.x > it.first.x && (it.first.y <= pixel.y && pixel.y <= it.second.y) }
//                val isLinesRight =
//                    yPixelLines.any { pixel.x < it.first.x && (it.first.y <= pixel.y && pixel.y <= it.second.y) }
//
//                if (xLinesInRange || yLinesInRange) area.color // this pixel is line.
//                else if (isLinesAbove && isLinesBelow && isLinesLeft && isLinesRight) area.overlay // this pixel is in the area to fill.
//                else pixel.toColor().awt() // this pixel should be ignored
//
//            }.overlay(textImage, 2, 2), originPixel.x, originPixel.y)
//        }
//
//        val timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-DD-HH-mm-ss")
//        val filename = "AreaMap-${LocalDateTime.now().format(timeFormatter)}.png"
//        areaMapImage.output(PngWriter.NoCompression, Path(path, filename))
//    }
}