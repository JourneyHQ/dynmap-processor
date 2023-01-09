import java.awt.Color

class Area(
    val name: String,
    val coordinates: List<MinecraftCoordinate>,
    val color: Color
) {
    val transparentColor = Color(color.red, color.green, color.blue, 50)

    val xLines = mutableListOf<Pair<MinecraftCoordinate, MinecraftCoordinate>>()
    val yLines = mutableListOf<Pair<MinecraftCoordinate, MinecraftCoordinate>>()

    private fun isCrossed(
        line1: Pair<MinecraftCoordinate, MinecraftCoordinate>,
        line2: Pair<MinecraftCoordinate, MinecraftCoordinate>
    ): Boolean {
        val direction1 = checkDirection(line1)
        val direction2 = checkDirection(line2)
        if (direction1 == null || direction2 == null)
            throw UnsupportedOperationException("Provided line(s) are tilted.")

        if (direction1 == direction2) return false

        val inLineRange = { line: Pair<MinecraftCoordinate, MinecraftCoordinate>, int: Int ->
            val range =
                if (checkDirection(line) == Direction.Y) listOf(line.first.y, line.second.y)
                else listOf(line.first.x, line.second.x)

            val sortedRange = range.sorted()

            sortedRange[0] < int && int < sortedRange[1]
        }

        val line1InLine2Range = inLineRange(line2, if (direction1 == Direction.X) line1.first.y else line1.first.x)

        val line2InLine1Range = inLineRange(line1, if (direction2 == Direction.X) line2.first.y else line2.first.x)

        return line1InLine2Range && line2InLine1Range // crossed
    }

    /**
     * Check the direction of the line.
     * @param line the line to check.
     * @return direction if line is NOT tilted ([Direction.X] or [Direction.Y]). null if it's tilted.
     */
    private fun checkDirection(line: Pair<MinecraftCoordinate, MinecraftCoordinate>): Direction? {
        val sameX = line.first.x == line.second.x
        val sameY = line.first.y == line.second.y
        return if (sameX && !sameY) Direction.Y
        else if (sameY && !sameX) Direction.X
        else null
    }

    enum class Direction {
        X, Y
    }

    init {
        val xCoordinates = coordinates.sortedWith(
            compareBy<MinecraftCoordinate> { it.y }.thenBy { it.x }
        ).groupBy { it.y }

        val yCoordinates = coordinates.sortedWith(
            compareBy<MinecraftCoordinate> { it.x }.thenBy { it.y }
        ).groupBy { it.x }

        val xLines = mutableListOf<Pair<MinecraftCoordinate, MinecraftCoordinate>>()

        val yLines = mutableListOf<Pair<MinecraftCoordinate, MinecraftCoordinate>>()

        for ((y, coordinateList) in xCoordinates) {
            if (coordinates.size % 2 != 0)
                throw IllegalStateException("$name has an odd number of points at y=$y")

            xLines.addAll(coordinateList.windowed(2, 1).map { it[0] to it[1] })
        }

        for ((x, coordinateList) in yCoordinates) {
            if (coordinates.size % 2 != 0)
                throw IllegalStateException("$name has an odd number of points at x=$x")

            yLines.addAll(coordinateList.windowed(2, 1).map { it[0] to it[1] })
        }

        for (xLine in xLines)
            for (yLine in yLines) {
                if (isCrossed(xLine, yLine))
                    throw IllegalStateException("Intersecting lines were detected in $name: ${xLine.first} to ${xLine.second} and ${yLine.first} to ${yLine.second}")
            }

        // check x line duplication
        val xRequiredLines =
            mutableListOf<Pair<MinecraftCoordinate, MinecraftCoordinate>>() // required lines to consist this area.

        val xLineGroups = xLines.groupBy { it.first.y }
        if (xLineGroups.size >= 3) {
            val yList = xLineGroups.keys
            for (xLineGroup in xLineGroups) {
                if (xLineGroup.key == yList.first() || xLineGroup.key == yList.last()) {
                    xRequiredLines.addAll(xLineGroup.value)
                    continue
                } // skip last and first

                val linesAbove = xLineGroups.filterKeys { it > xLineGroup.key }
                    .flatMap { it.value }
                    .map { { double: Double -> it.first.x < double && double < it.second.x } }

                val linesBelow = xLineGroups.filterKeys { it < xLineGroup.key }
                    .flatMap { it.value }
                    .map { { double: Double -> it.first.x < double && double < it.second.x } }

                for (line in xLineGroup.value) {
                    val centerPoint = (line.first.x + line.second.x).toDouble() / 2
                    if (linesAbove.any { it(centerPoint) } && linesBelow.any { it(centerPoint) })
                        continue
                    else xRequiredLines.add(line)
                }
            }
        } else xRequiredLines.addAll(xLines)

        // check y line duplication
        val yRequiredLines =
            mutableListOf<Pair<MinecraftCoordinate, MinecraftCoordinate>>() // required lines to consist this area.

        val yLineGroups = yLines.groupBy { it.first.x }
        if (yLineGroups.size >= 3) {
            val xList = yLineGroups.keys
            for (yLineGroup in yLineGroups) {
                if (yLineGroup.key == xList.first() || yLineGroup.key == xList.last()) {
                    yRequiredLines.addAll(yLineGroup.value)
                    continue
                } // skip last and first

                val linesAbove = yLineGroups.filterKeys { it > yLineGroup.key }
                    .flatMap { it.value }
                    .map { { double: Double -> it.first.y < double && double < it.second.y } }

                val linesBelow = yLineGroups.filterKeys { it < yLineGroup.key }
                    .flatMap { it.value }
                    .map { { double: Double -> it.first.y < double && double < it.second.y } }

                for (line in yLineGroup.value) {
                    val centerPoint = (line.first.y + line.second.y).toDouble() / 2
                    if (linesAbove.any { it(centerPoint) } && linesBelow.any { it(centerPoint) })
                        continue
                    else yRequiredLines.add(line)
                }
            }
        } else yRequiredLines.addAll(yLines)

        this.xLines.addAll(xRequiredLines)
        this.yLines.addAll(yRequiredLines)
    }
}