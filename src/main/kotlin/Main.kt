import java.io.File

data class ImageFile(
    val x: Number,
    val y: Number,
    val file: File
)

fun main() {
    val files = File("./images/zoom-4").listFiles()
        ?: throw UnsupportedOperationException("Images under ./images not found.")

    val imageFiles = mutableListOf<ImageFile>()

    for (file in files) {
        val regex = Regex("^(-?[0-9]+)_(-?[0-9]+)\\.png$")
        val matchResult = regex.matchEntire(file.name)
        if (matchResult == null) {
            println("Skip illegal file name: ${file.name}")
            continue
        }

        val data = matchResult.groups.map { it?.value }
        imageFiles.add(ImageFile(data[1]!!.toInt(), data[2]!!.toInt(), File(data[0]!!)))
    }
    println(imageFiles)
}