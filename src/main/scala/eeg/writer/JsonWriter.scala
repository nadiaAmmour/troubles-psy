package eeg.writer


import com.typesafe.scalalogging.LazyLogging
import eeg.model._
import io.circe._
import io.circe.syntax._

import java.io.{BufferedWriter, FileWriter}
import java.nio.file.{Files, Paths}

object JsonWriter extends LazyLogging {

  implicit val featureStatsEncoder: Encoder[FeatureStats] =
    Encoder.forProduct2("min", "max")(s => (s.min, s.max))

  implicit val featureCountEncoder: Encoder[FeatureCount] =
    Encoder.forProduct3("ab_features", "coh_features", "total")(
      fc => (fc.abFeatures, fc.cohFeatures, fc.total)
    )

  implicit val metadataEncoder: Encoder[OutputMetadata] =
    Encoder.forProduct6(
      "source_file", "total_records", "removed_records",
      "imputed_fields", "feature_count", "processed_at"
    )(m => (m.sourceFile, m.totalRecords, m.removedRecords, m.imputedFields, m.featureCount, m.processedAt))

  implicit val normalizedRecordEncoder: Encoder[NormalizedEEGRecord] = r =>
    Json.obj(
      "id"                -> r.id.asJson,
      "sex"               -> r.sex.asJson,
      "age"               -> r.age.asJson,
      "education"         -> r.education.asJson,
      "iq"                -> r.iq.asJson,
      "main_disorder"     -> r.mainDisorder.asJson,
      "specific_disorder" -> r.specificDisorder.asJson,
      "ab_features"       -> r.abFeatures.asJson,
      "coh_features"      -> r.cohFeatures.asJson
    )

  implicit val ms1OutputEncoder: Encoder[MS1Output] = o =>
    Json.obj(
      "metadata"            -> o.metadata.asJson,
      "normalization_stats" -> o.normalizationStats.asJson,
      "records"             -> o.records.asJson
    )

  def writeFullJson(output: MS1Output, outputPath: String): Unit = {
    logger.info(s"Écriture JSON vers : $outputPath")
    ensureDir(outputPath)
    writeFile(outputPath, output.asJson.spaces2)
    logger.info("JSON écrit avec succès")
  }

  def writeSplitJson(output: MS1Output, basePath: String): Unit = {
    ensureDir(basePath + "_meta.json")
    writeFile(basePath + "_meta.json",
      Json.obj("metadata" -> output.metadata.asJson, "normalization_stats" -> output.normalizationStats.asJson).spaces2)
    writeFile(basePath + "_records.json", output.records.asJson.spaces2)
    logger.info(s"Split écrit : ${basePath}_meta.json + ${basePath}_records.json")
  }

  private def ensureDir(filePath: String): Unit = {
    val parent = Paths.get(filePath).getParent
    if (parent != null && !Files.exists(parent)) Files.createDirectories(parent)
  }

  private def writeFile(path: String, content: String): Unit = {
    val w = new BufferedWriter(new FileWriter(path))
    try w.write(content) finally w.close()
  }
}