package eeg.test

import eeg.cleaner.DataCleaner
import eeg.model._
import eeg.normalizer.DataNormalizer
import eeg.parser.CSVParser
import eeg.writer.JsonWriter

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object EEGPipelineTest {

  def main(args: Array[String]): Unit = {
    if (args.length != 2) {
      println("Usage : EEGPipelineTest <input_csv_path> <output_json_path>")
      System.exit(1)
    }

    val inputCsv  = args(0)
    val outputJson = args(1)

    // --------------------------
    // 1. Parsing CSV
    // --------------------------
    val (rawRecords, skippedRows) = CSVParser.parse(inputCsv)
    println(s"CSV parsed : ${rawRecords.size} valid rows, $skippedRows skipped")

    // --------------------------
    // 2. Cleaning Data
    // --------------------------
    val (cleanedRecords, cleaningReport) = DataCleaner.clean(rawRecords)
    println(s"Data cleaned : ${cleanedRecords.size} records kept, ${cleaningReport.removedRecords} removed")
    println(s"Fields imputed : education=${cleaningReport.educationImputed}, iq=${cleaningReport.iqImputed}, " +
      s"AB=${cleaningReport.abFeaturesImputed}, COH=${cleaningReport.cohFeaturesImputed}")

    // --------------------------
    // 3. Normalizing Data
    // --------------------------
    val (normalizedRecords, normalizationStats) = DataNormalizer.normalize(cleanedRecords)
    println(s"Data normalized : ${normalizedRecords.size} records")

    // --------------------------
    // 4. Preparing MS1Output
    // --------------------------
    val nowStr = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    val featureCount = FeatureCount(
      abFeatures  = if (cleanedRecords.nonEmpty) cleanedRecords.head.abFeatures.size else 0,
      cohFeatures = if (cleanedRecords.nonEmpty) cleanedRecords.head.cohFeatures.size else 0,
      total       = if (cleanedRecords.nonEmpty) cleanedRecords.head.abFeatures.size + cleanedRecords.head.cohFeatures.size else 0
    )
    val imputedFields = Map(
      "education" -> cleaningReport.educationImputed,
      "iq"        -> cleaningReport.iqImputed,
      "abFeatures" -> cleaningReport.abFeaturesImputed,
      "cohFeatures" -> cleaningReport.cohFeaturesImputed
    )
    val metadata = OutputMetadata(
      sourceFile     = inputCsv,
      totalRecords   = rawRecords.size,
      removedRecords = cleaningReport.removedRecords,
      imputedFields  = imputedFields,
      featureCount   = featureCount,
      processedAt    = nowStr
    )

    val output = MS1Output(
      metadata           = metadata,
      normalizationStats = normalizationStats,
      records            = normalizedRecords
    )

    // --------------------------
    // 5. Writing JSON
    // --------------------------
    JsonWriter.writeFullJson(output, outputJson)
    println(s"Output JSON written to $outputJson")
  }
}