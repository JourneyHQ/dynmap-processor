import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.double
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.path
import com.sksamuel.scrimage.nio.PngWriter
import kotlinx.serialization.json.Json
import markers.Markers
import java.io.File
import java.nio.file.Path
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


// 128 * 128
// 0-0.png -> 0,-64

class Main : CliktCommand(
    help = """
        Welcome to Dynmap Processor.  
        
        For more detailed information, please refer to https://github.com/jaoafa/DynmapProcessor#readme
""".trimIndent()
) {
    val images by option(
        "-i", "--input",
        help = "The directory of tile images."
    ).path(mustExist = true, canBeFile = false).required()

    val output by option(
        "-o", "--output",
        help = "The directory to output generated images and metadata."
    ).path(canBeFile = false).required()

    val cache by option(
        "-c", "--cache", //fixme when zoom level is different but use of cache is allowed...
        help = "Whether to allow the use of cached basemap. (Skip basemap generation from scratch)"
    ).flag(default = false)

    val zoom by option(
        "-z", "--zoom",
        help = "Specify the zoom level from 0 to 4 (4 by default)"
    ).int().default(4).check("Value must be 0 to 4") {
        it in 0..4
    }

    val grid by option(
        "-g", "--grid",
        help = "Whether to enable chunk grid."
    ).flag(default = false)

    val edit by option(
        "-e", "--edit",
        help = "Whether to enable image editing."
    ).flag(default = false)

    val markers by option(
        "-m", "--markers",
        help = "The file path to the JSON file that configures markers."
    ).path(mustExist = true, canBeDir = false)

    val trim by option(
        "-t", "--trim",
        help = "Trim to the specified area. Format: x1,y1,x2,y2"
    ).split(",").check("Length of the list must be 4. For example: 120,150,-10,10") { it.size == 4 }

    val height by option(
        "-h", "--height",
        help = "Height of the map image. Using this with the width option might cause distortion."
    ).int().check("Value must be positive.") { 0 < it }

    val width by option(
        "-w", "--width",
        help = "Width of the map image. Using this with the height option might cause distortion."
    ).int().check("Value must be positive.") { 0 < it }

    val resize by option(
        "-r", "--resize",
        help = "Scale up (or down) the output image to the specified scale rate. (0<x<1 to scale down, 1<x to scale up)"
    ).double().default(1.0).check { it > 0 }

    override fun run() {
        val chunkImageResolution = 128

        val outputString = output.toString()

        val basemapExists = MapImage.basemapFile(outputString).exists()
        val metadataExists = MapImage.metadataFile(outputString).exists()

        val mapImage =
            if (basemapExists && metadataExists && cache)
                MapImage.load(outputString, images.toString())
            else MapImage.create(outputString, images.toString(), zoom, chunkImageResolution, grid)

        if (edit) {
            val markerFile = File(this.markers.toString())

            val editedMapImage = mapImage.edit(
                if (markerFile.exists()) Json.decodeFromString(
                    Markers.serializer(),
                    markerFile.readText()
                ) else Markers(emptyList()),
                height,
                width,
                trim?.map { it.toInt() },
                resize
            )

            val timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-DD-HH-mm-ss")
            val filename = "map-${LocalDateTime.now().format(timeFormatter)}.png"

            editedMapImage.output(PngWriter.NoCompression, Path.of(outputString, filename))
            println("Image edit complete.")
        }
    }
}

fun main(args: Array<String>) = Main().main(args)

//fun main() {
//    val chunkImageResolution = 128
//    val zoom = 4
//
//    val mapImage = MapImage.create("./map/", "./images/", zoom, chunkImageResolution)
//
//    mapImage.createAreaImage(
//        Area(
//            "test1", listOf(
//                MinecraftCoordinate(1070, -1873),
//                MinecraftCoordinate(1362, -1873),
//                MinecraftCoordinate(704, -1764),
//                MinecraftCoordinate(1070, -1764),
//                MinecraftCoordinate(704, -1430),
//                MinecraftCoordinate(1362, -1430)
//            ), Color(255, 167, 250) // yuuacity
//        ),
//        Area(
//            "test2", listOf(
//                MinecraftCoordinate(-1000, 1500),
//                MinecraftCoordinate(-1500, 1500),
//                MinecraftCoordinate(-1000, 1000),
//                MinecraftCoordinate(-1500, 1000)
//            ), Color.RED
//        )
//    )
//}