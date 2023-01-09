import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.check
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.path
import java.awt.Color


// 128 * 128
// 0-0.png -> 0,-64

// TODO
class Main : CliktCommand() {
    val tiles by option(help = "The directory of tile images.").path(
        mustExist = true,
        canBeFile = false
    )

    val output by option(help = "The directory to output generated images and metadata.").path(
        canBeFile = false
    )

    val zoom by option(help = "Zoom level (0~4)").int().default(4)
        .check("Value must be 0~4") {
            it in 0..4
        }

    val height by option(help = "")

    override fun run() {

    }
}

fun main() {
    /**
     * MinecraftCoordinate(-2,1),
     * MinecraftCoordinate(0,1),
     * MinecraftCoordinate(-2,0),
     * MinecraftCoordinate(-1,0),
     * MinecraftCoordinate(0,0),
     * MinecraftCoordinate(1,0),
     * MinecraftCoordinate(-1,-1),
     * MinecraftCoordinate(1,-1) //duplicated line
     *
     * MinecraftCoordinate(-1,1),
     * MinecraftCoordinate(0,1),
     * MinecraftCoordinate(-1,0),
     * MinecraftCoordinate(1,0),
     * MinecraftCoordinate(0,-1),
     * MinecraftCoordinate(1,-1) // intersecting line (error)
     */

    val chunkImageResolution = 128
    val zoom = 4

    val mapImage = MapImage.create("./map/", "./images/", zoom, chunkImageResolution)

    mapImage.createAreaImage(
        Area(
            "test1", listOf(
                MinecraftCoordinate(1070, -1873),
                MinecraftCoordinate(1362, -1873),
                MinecraftCoordinate(704, -1764),
                MinecraftCoordinate(1070, -1764),
                MinecraftCoordinate(704, -1430),
                MinecraftCoordinate(1362, -1430)
            ), Color(255, 167, 250) // yuuacity
        ),
        Area(
            "test2", listOf(
                MinecraftCoordinate(-1000, 1500),
                MinecraftCoordinate(-1500, 1500),
                MinecraftCoordinate(-1000, 1000),
                MinecraftCoordinate(-1500, 1000)
            ), Color.RED
        )
    )
}