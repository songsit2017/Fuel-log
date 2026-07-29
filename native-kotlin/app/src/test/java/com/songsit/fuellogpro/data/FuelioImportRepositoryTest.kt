package com.songsit.fuellogpro.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression coverage for parseFuelioCsv() against real Fuelio export quirks that broke
 * imports three times in a row before they were caught: the ##Log date column is literally
 * named "Data" (not "Date") and packs date+time together, "Price (optional)" is the gross
 * total while the real per-liter price lives in a separate "VolumePrice" column, the station
 * name is under "City (optional)", and ##Log/##Costs UniqueId sequences overlap so photo
 * linking must be scoped by the ##Pictures "Type" column. Fixtures below are trimmed excerpts
 * of a real user-provided .fuelio export (same column order/quoting), not synthetic data.
 */
class FuelioImportRepositoryTest {

    @Test
    fun `vehicle name is read from the Vehicle section header row`() {
        val result = parseFuelioCsv(VEHICLE_AND_LOG_CSV)
        assertEquals("V-Cross M/AT 2022", result.vehicleName)
    }

    @Test
    fun `log rows split the combined date-time column and keep the gross total`() {
        val result = parseFuelioCsv(VEHICLE_AND_LOG_CSV)
        val row = result.fuelRows.single { it.uniqueId == "204" }
        assertEquals("2026-05-26", row.date)
        assertEquals("11:20", row.time)
        assertEquals(127232.0, row.odometerKm, 0.001)
        assertEquals(72.604, row.liters, 0.001)
        // "Price (optional)" (3000.0) is the gross total, not the per-liter price.
        assertEquals(3000.0, row.amount, 0.001)
        // "VolumePrice" (41.32) is the real per-liter price — must win over amount/liters.
        assertEquals(41.32, row.pricePerLiter, 0.001)
        assertEquals("Caltex มาบยางพร - Tambon Mapyangphon", row.station)
        assertTrue(row.fullTank)
    }

    @Test
    fun `log full-tank flag treats any non-zero value as full`() {
        val result = parseFuelioCsv(VEHICLE_AND_LOG_CSV)
        val partial = result.fuelRows.single { it.uniqueId == "214" }
        assertEquals(false, partial.fullTank) // Full="0" in the fixture row
    }

    @Test
    fun `zero-amount cost rows (Cost column is 0_0) are filtered out`() {
        // Confirms idxCostPrice really is reading the "Cost" column (0.0 here), not
        // silently falling through to some other column that would have a nonzero value.
        val result = parseFuelioCsv(COSTS_CSV)
        assertTrue(result.costRows.isEmpty())
    }

    @Test
    fun `cost rows split date-time, match the real Cost column, and resolve category`() {
        val result = parseFuelioCsv(COSTS_WITH_POSITIVE_AMOUNT_CSV)
        val row = result.costRows.single()
        assertEquals("2025-03-28", row.date)
        assertEquals("10:43", row.time)
        assertEquals("เช็คระและบำรุงรักษา", row.title)
        assertEquals("บำรุงรักษา", row.category)
        assertEquals(2917.36, row.amount, 0.001)
        assertEquals("7", row.uniqueId)
    }

    @Test
    fun `pictures are scoped by Type so overlapping Log and Costs UniqueIds don't collide`() {
        val result = parseFuelioCsv(PICTURES_CSV)
        // Type=1 (log) UniqueId=204 and Type=2 (costs) UniqueId=204 both exist in the fixture
        // with different photos — a type-scoped lookup must keep them apart.
        assertEquals(listOf("log_204.jpg"), result.pictureMap["1:204"])
        assertEquals(listOf("cost_204.jpg"), result.pictureMap["2:204"])
    }

    @Test
    fun `a Costs row's picture living in a different vehicle's csv file still resolves after combining`() {
        // Real excerpt from a user's two-vehicle .fuelio export: V-Cross's own csv has the
        // ##Costs row (UniqueId 3) but its ##Pictures table has no Type=2 entry for it — the
        // actual photo entry sits in the *other* vehicle's csv (Toyota-CHR). importFuelioZip()
        // is supposed to combine every parsed entry's pictureMap before resolving any row's
        // photo (see combinedPictureMap in importFuelioZip) — this reproduces that combine step
        // exactly to check the Costs path really benefits from it the same way Log rows do.
        val vCrossResult = parseFuelioCsv(V_CROSS_COSTS_CSV)
        val toyotaResult = parseFuelioCsv(TOYOTA_PICTURES_ONLY_CSV)

        val combinedPictureMap = mutableMapOf<String, MutableList<String>>()
        for (parsed in listOf(vCrossResult, toyotaResult)) {
            for ((targetId, filenames) in parsed.pictureMap) {
                combinedPictureMap.getOrPut(targetId) { mutableListOf() } += filenames
            }
        }

        val costRow = vCrossResult.costRows.single { it.uniqueId == "3" }
        assertEquals("2024-09-11", costRow.date)
        assertEquals(91677.0, costRow.odometerKm)

        // This is exactly what resolvePhotoUri() looks up for a Costs row (rowType "2").
        val filenames = combinedPictureMap["2:${costRow.uniqueId}"]
        assertEquals(listOf("JPEG_20250607_005106_3533775571852396777.jpg"), filenames)
    }

    @Test
    fun `full real two-vehicle export resolves a Costs row's photo the same way importFuelioZip would`() {
        // Unlike every other test above (trimmed excerpts), this parses the *complete*,
        // unmodified two-vehicle .fuelio export a user reported missing Costs photos from
        // (src/test/resources/fuelio/vehicle1local.csv + vehicle2local.csv — 273 and 149 real
        // lines respectively, full ##Log history included) and replicates importFuelioZip's
        // exact combine-then-resolve pipeline, to rule out anything the trimmed fixtures above
        // might have accidentally not reproduced (a full ##Log section ahead of ##Costs, the
        // *same* Type=2 picture rows appearing in *both* files' own ##Pictures tables, etc.).
        val vehicle1 = parseFuelioCsv(readFuelioFixture("vehicle1local.csv"))
        val vehicle2 = parseFuelioCsv(readFuelioFixture("vehicle2local.csv"))

        assertEquals("V-Cross M/AT 2022", vehicle1.vehicleName)
        assertEquals("Toyota​-CHR​ Hybrid", vehicle2.vehicleName)

        val combinedPictureMap = mutableMapOf<String, MutableList<String>>()
        for (parsed in listOf(vehicle1, vehicle2)) {
            for ((targetId, filenames) in parsed.pictureMap) {
                combinedPictureMap.getOrPut(targetId) { mutableListOf() } += filenames
            }
        }

        val costRow = vehicle1.costRows.single { it.uniqueId == "3" }
        assertEquals("2024-09-11", costRow.date)
        assertEquals("10:49", costRow.time)
        assertEquals(91677.0, costRow.odometerKm)

        val resolved = combinedPictureMap["2:${costRow.uniqueId}"] ?: combinedPictureMap[costRow.uniqueId]
        assertEquals(listOf("JPEG_20250607_005106_3533775571852396777.jpg"), resolved)
    }

    private fun readFuelioFixture(name: String): String =
        checkNotNull(javaClass.classLoader?.getResourceAsStream("fuelio/$name")) { "Missing fixture $name" }
            .bufferedReader(Charsets.UTF_8).readText()

    companion object {
        private val VEHICLE_AND_LOG_CSV = """
            "## Vehicle"
            "Name","Description","DistUnit","FuelUnit","ConsumptionUnit","ImportCSVDateFormat","VIN","Insurance","Plate","Make","Model","Year","TankCount","Tank1Type","Tank2Type","Active","Tank1Capacity","Tank2Capacity","FuelUnitTank2","FuelConsumptionTank2","guid","lastupdated"
            "V-Cross M/AT 2022","","0","0","3","yyyy-MM-dd","","","","","",,"1","200","0","1","76.0","0.0","0","0","0a424bdc-e62d-4728-87be-15a06c94ca90","1784993917098"
            "## Log"
            "Data","Odo (km)","Fuel (litres)","Full","Price (optional)","km/l (optional)","latitude (optional)","longitude (optional)","City (optional)","Notes (optional)","Missed","TankNumber","FuelType","VolumePrice","StationID (optional)","ExcludeDistance","UniqueId","TankCalc","Weather","guid","lastupdated"
            "2026-07-18 15:14","128887.0","28.604","0","1000.0",,"13.0366","101.13595","ตำบล บ่อวิน - ปตท.อีสเทิร์น ขาเข้า","","0","1","201","34.96","998283","0.0","214","0.0",,"edaa2bca-7873-45c1-bf89-73d04706713a","1784362555093"
            "2026-05-26 11:20","127232.0","72.604","1","3000.0","10.99","12.99944","101.13054","Caltex มาบยางพร - Tambon Mapyangphon","","0","1","201","41.32","1015298","0.0","204","0.0",,"a0461465-d665-469b-8727-32cdb5a2888a","1784362555097"
        """.trimIndent()

        private val COSTS_CSV = """
            "## CostCategories"
            "CostTypeID","Name","priority","color","guid","lastupdated"
            "2","บำรุงรักษา","0","","c8d2ca06-a99c-436e-89f9-05ac1f61e2dc","1784993917119"
            "## Costs"
            "CostTitle","Date","Odo","CostTypeID","Notes","Cost","flag","idR","read","RemindOdo","RemindDate","isTemplate","RepeatOdo","RepeatMonths","isIncome","UniqueId","guid","lastupdated"
            "ถ่ายน้ำมันเครื่อง","2026-04-17 14:59","163233","2","","0.0","0","0","1","0","2011-01-01","0","0","0","0","2","ea7feaee-6ec3-4c7c-b1a2-dab00dd5a9c3","1776412858483"
        """.trimIndent()

        private val COSTS_WITH_POSITIVE_AMOUNT_CSV = """
            "## CostCategories"
            "CostTypeID","Name","priority","color","guid","lastupdated"
            "2","บำรุงรักษา","0","","4d2f86cc-2699-43b0-b46c-296a5379fd26","1784993917111"
            "## Costs"
            "CostTitle","Date","Odo","CostTypeID","Notes","Cost","flag","idR","read","RemindOdo","RemindDate","isTemplate","RepeatOdo","RepeatMonths","isIncome","UniqueId","guid","lastupdated"
            "เช็คระและบำรุงรักษา","2025-03-28 10:43","110609","2","","2917.36","0","0","1","0","2011-01-01","0","0","0","0","7","98e9100b-178c-407a-b743-f2252b584e24","1784993917111"
        """.trimIndent()

        private val PICTURES_CSV = """
            "## Pictures"
            "Filename","Note","Type","target_id","guid","lastupdated"
            "log_204.jpg",,"1","204",,"0"
            "cost_204.jpg",,"2","204",,"0"
        """.trimIndent()

        private val V_CROSS_COSTS_CSV = """
            "## Vehicle"
            "Name","Description","DistUnit","FuelUnit","ConsumptionUnit","ImportCSVDateFormat","VIN","Insurance","Plate","Make","Model","Year","TankCount","Tank1Type","Tank2Type","Active","Tank1Capacity","Tank2Capacity","FuelUnitTank2","FuelConsumptionTank2","guid","lastupdated"
            "V-Cross M/AT 2022","","0","0","3","yyyy-MM-dd","","","","","",,"1","200","0","1","76.0","0.0","0","0","0a424bdc-e62d-4728-87be-15a06c94ca90","1784993917098"
            "## CostCategories"
            "CostTypeID","Name","priority","color","guid","lastupdated"
            "2","บำรุงรักษา","0","","4d2f86cc-2699-43b0-b46c-296a5379fd26","1784993917111"
            "## Costs"
            "CostTitle","Date","Odo","CostTypeID","Notes","Cost","flag","idR","read","RemindOdo","RemindDate","isTemplate","RepeatOdo","RepeatMonths","isIncome","UniqueId","guid","lastupdated"
            "เช็คระยะและบำรุงรักษา","2024-09-11 10:49","91677","2","","3053.78","0","0","1","0","2011-01-01","0","0","0","0","3","bf736484-a38b-4467-9db4-91199af64021","1784993917111"
            "## Pictures"
            "Filename","Note","Type","target_id","guid","lastupdated"
            "JPEG_20260213_110934_7073527851478726632.jpg",,"1","2",,"0"
        """.trimIndent()

        private val TOYOTA_PICTURES_ONLY_CSV = """
            "## Vehicle"
            "Name","Description","DistUnit","FuelUnit","ConsumptionUnit","ImportCSVDateFormat","VIN","Insurance","Plate","Make","Model","Year","TankCount","Tank1Type","Tank2Type","Active","Tank1Capacity","Tank2Capacity","FuelUnitTank2","FuelConsumptionTank2","guid","lastupdated"
            "Toyota​-CHR​ Hybrid","","0","0","3","yyyy-MM-dd","","","","","","2019","1","700","0","1","43.0","0.0","0","0","785aef7f-5aee-4638-bc64-e8ecaeb69058","1784993917118"
            "## Pictures"
            "Filename","Note","Type","target_id","guid","lastupdated"
            "JPEG_20250607_004736_3086824833622562963.jpg",,"2","2",,"0"
            "JPEG_20250607_005106_3533775571852396777.jpg",,"2","3",,"0"
            "JPEG_20250607_005335_8766199239411375876.jpg",,"2","4",,"0"
        """.trimIndent()
    }
}
