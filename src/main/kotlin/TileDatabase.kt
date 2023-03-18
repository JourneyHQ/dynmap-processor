import java.io.File
import java.sql.Connection
import java.sql.DriverManager

class TileDatabase(
    private val jdbcUrl: String,
    private val username: String,
    private val password: String,
    private val tablePrefix: String
) {
    fun downloadAll(mapId: Int, output: String) {
        val connection: Connection = DriverManager.getConnection(
            jdbcUrl,
            username,
            password
        )

        val tableName = "${tablePrefix}_Tiles"
        val statement = connection.prepareStatement(
            "SELECT * FROM $tableName WHERE MapID = ?"
        )
        statement.setInt(1, mapId)
        val resultSet = statement.executeQuery()

        while (resultSet.next()) {
            val x = resultSet.getInt("x")
            val y = resultSet.getInt("y")
            val zoom = resultSet.getInt("zoom")
            val data = resultSet.getBytes("NewImage")

            println("Downloading: zoom=$zoom, x=$x, y=$y")

            val file = File(getFilePath(output, x, y, zoom))
            if(!file.parentFile.exists()){
                file.parentFile.mkdirs()
            }
            file.writeBytes(data)
        }

        resultSet.close()
        connection.close()
    }

    private fun getFilePath(parent: String, x: Int, y: Int, zoom: Int) = "$parent/zoom-${zoom}/${x}_${y}.png"
}