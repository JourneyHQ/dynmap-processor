import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.double
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.path
import kotlinx.serialization.json.Json
import markers.Markers
import java.io.File


// 128 * 128
// 0-0.png -> 0,-64

class Main : CliktCommand() {
    val images by option(
        "-i",
        help = "The directory of tile images."
    ).path(mustExist = true, canBeFile = false).required()

    val markers by option(
        "-m",
        help = "The file path to JSON file that configure markers."
    ).path(mustExist = true, canBeDir = false)

    val output by option(
        "-o",
        help = "The directory to output generated images and metadata."
    ).path(canBeFile = false).required()

    val cache by option(
        "-c",
        help = "Whether to allow the use of cached basemap. (Skip basemap generation from scratch)"
    ).flag(default = false)

    val zoom by option(
        "-z",
        help = "Zoom level 0 to 4 (4 by default)"
    ).int().default(4).check("Value must be 0 to 4") {
        it in 0..4
    }

    val height by option(
        "-h",
        help = "Height of the map image. Using with width parameter might cause distortion."
    ).int().check("Value must be positive.") { 0 < it }

    val width by option(
        "-w",
        help = "Width of the map image. Using with height parameter might cause distortion."
    ).int().check("Value must be positive.") { 0 < it }

    val trim by option(
        "-t",
        help = "Trim to a specified area. Format: x1,y1,x2,y2"
    ).split(",").check("Length of the list must be 4. For example: 120,150,-10,10") { it.size == 4 }

    val resize by option(
        "-r",
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
            else MapImage.create(outputString, images.toString(), zoom, chunkImageResolution)

        if (this.markers != null) {
            val markers = Json.decodeFromString(Markers.serializer(), File(this.markers.toString()).readText())


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