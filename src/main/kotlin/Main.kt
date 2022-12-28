import java.io.File

data class ImageFile(
    val x: Number,
    val y: Number,
    val file: File
)

fun main() {
    collectImages("./images/zoom-4")
}

fun collectImages(path: String) {
    val files = File(path).listFiles()
        ?: throw UnsupportedOperationException("Images under $path not found.")

    val imageFiles = mutableListOf<ImageFile>()

    for (file in files) {
        val regex = Regex("^(-?[0-9]+)_(-?[0-9]+)\\.png$")
        val matchResult = regex.matchEntire(file.name)
        if (matchResult == null) {
            println("Skip illegal file name: ${file.name}")
            continue
        }

        val data = matchResult.groups.map { it?.value } // ex: ["1_-4.png", "1", "-4"]
        imageFiles.add(ImageFile(data[1]!!.toInt(), data[2]!!.toInt(), File(data[0]!!)))
    }

    return imageFiles
}