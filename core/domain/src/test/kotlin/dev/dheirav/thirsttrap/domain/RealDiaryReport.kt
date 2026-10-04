package dev.dheirav.thirsttrap.domain

import org.junit.Test

/**
 * Scores the real diary with the real model. Opt-in and silent without data.
 *
 * Unit tests prove the arithmetic against fixtures someone wrote. They cannot
 * say whether the thing works on actual pots, which is the only question that
 * decides whether this app is worth having. This reads a TSV exported from the
 * phone by `tools/analysis/export-diary.py` and runs the shipping code over it,
 * rather than reimplementing the model somewhere easier, which would only prove
 * the reimplementation agrees with itself.
 *
 * It prints and never asserts. There is no pass or fail here, because the
 * answer is a measurement, and a test that failed when a real plant behaved
 * unexpectedly would be a test of the plant.
 *
 * The fixture is gitignored: it is a person's diary, not test data.
 *
 *   python3 tools/analysis/export-diary.py
 *   ./gradlew :core:domain:test --tests '*RealDiaryReport*' -i
 */
class RealDiaryReport {
    @Test
    fun report() {
        val txt = javaClass.classLoader.getResourceAsStream("real-diary.tsv")
            ?.bufferedReader()?.readText()
            ?: run {
                println("\nNo diary fixture. Export one first:\n" +
                    "  python3 tools/analysis/export-diary.py\n")
                return
            }

        var name = ""; var trig = 0.5
        var rs = mutableListOf<WeightReading>()
        var ws = mutableListOf<Long>(); var ps = mutableListOf<Long>()
        val out = StringBuilder()

        fun flush() {
            if (name.isEmpty() || rs.size < 3) return
            val p = Plant(id = "x", name = name, depletionTrigger = trig, anchors = null)
            val t0 = rs.first().timestampMillis
            val day = { ms: Long -> (ms - t0) / 86_400_000.0 }
            val samples = evaluatePredictions(p, rs.toList(), ws.toList(), ps.toList())
            val s = scorePredictions(samples)

            out.append("\n%-15s %d readings over %.0f days, %d waterings, trigger %.0f%%\n"
                .format(name, rs.size, day(rs.last().timestampMillis), ws.size, trig * 100))
            out.append("   watered on days: %s\n".format(ws.joinToString(", ") { "%.1f".format(day(it)) }))

            // Which cycles produced ground truth at all, and which were censored
            val st = assembleWeightState(p, rs.toList(), ws.toList(), ps.toList(), rs.last().timestampMillis)
            val anchors = st.plant.anchors
            out.append("   derived anchors: %s\n".format(
                anchors?.let { "wet %.0f g, dry %.0f g%s -> trigger %.0f g"
                    .format(it.wetGrams, it.dryGrams, if (it.dryIsProvisional) " (provisional)" else "",
                            it.triggerWeight(trig)) } ?: "none"))
            if (anchors != null) {
                val trigW = anchors.triggerWeight(trig)
                out.append("   cycles:\n")
                for ((i, seg) in st.segments.withIndex()) {
                    val rr = seg.readings.filter { !it.excluded }
                    if (rr.isEmpty()) continue
                    val lo = rr.minOf { it.grams }
                    val crossed = rr.zipWithNext().any { (a, b) -> a.grams > trigW && b.grams <= trigW }
                    out.append("     %d: days %.1f-%.1f, %d readings, low %.0f g, %s\n".format(
                        i + 1, day(rr.first().timestampMillis), day(rr.last().timestampMillis),
                        rr.size, lo,
                        if (crossed) "CROSSED -> scorable"
                        else "watered before reaching %.0f g -> censored".format(trigW)))
                }
            }
            if (s.samples == 0) { out.append("   nothing scorable\n"); return }
            out.append("   scored moments on days: %s\n".format(
                samples.joinToString(", ") { "%.1f".format(it.actualDays) }.let { "" } ))
            out.append("   model error %.2f days over %d predictions (bias %+.2f)\n"
                .format(s.medianAbsErrorDays, s.samples, s.biasDays))
            out.append("   calendar had an opinion on %d of them\n".format(s.comparedSamples))
            if (s.comparedSamples > 0) {
                out.append("     weighing  %.2f days\n".format(s.medianAbsErrorDaysCompared))
                out.append("     calendar  %.2f days\n".format(s.calendarMedianAbsErrorDays))
                out.append("     advantage %+.2f days to %s\n".format(
                    s.advantageDays, if (s.advantageDays!! > 0) "WEIGHING" else "the calendar"))
            }
        }

        for (line in txt.lines()) {
            val f = line.split("\t")
            when (f.getOrNull(0)) {
                "PLANT" -> { flush(); name = f[1]; trig = f[2].toDouble()
                             rs = mutableListOf(); ws = mutableListOf(); ps = mutableListOf() }
                "R" -> rs += WeightReading(
                    id = "r${rs.size}", plantId = "x", timestampMillis = f[1].toLong(),
                    grams = f[2].toDouble(),
                    context = runCatching { ReadingContext.valueOf(f[3]) }.getOrDefault(ReadingContext.ROUTINE),
                    excluded = f[4] == "1")
                "W" -> ws += f[1].toLong()
                "P" -> ps += f[1].toLong()
            }
        }
        flush()
        println("\n================ REAL DIARY ================" + out + "============================================\n")
    }
}
