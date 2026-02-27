package eeg.cleaner

import com.typesafe.scalalogging.LazyLogging
import eeg.model.{CleanedEEGRecord, RawEEGRecord}

object DataCleaner extends LazyLogging {

  private val MIN_AB_COVERAGE  = 0.8
  private val MIN_COH_COVERAGE = 0.5

  case class CleaningReport(
                             totalInput:         Int,
                             totalOutput:        Int,
                             removedRecords:     Int,
                             educationImputed:   Int,
                             iqImputed:          Int,
                             abFeaturesImputed:  Int,
                             cohFeaturesImputed: Int
                           )

  def clean(records: List[RawEEGRecord]): (List[CleanedEEGRecord], CleaningReport) = {
    logger.info(s"Démarrage du nettoyage sur ${records.size} records")

    val medianEducation = computeMedianInt(records.flatMap(_.education))
    val medianIQ        = computeMedianInt(records.flatMap(_.iq))
    val maxAbCols       = records.map(_.abFeatures.size).maxOption.getOrElse(0)
    val maxCohCols      = records.map(_.cohFeatures.size).maxOption.getOrElse(0)
    val allAbKeys       = records.flatMap(_.abFeatures.keys).distinct
    val allCohKeys      = records.flatMap(_.cohFeatures.keys).distinct
    val abMeans         = computeColumnMeans(records.map(_.abFeatures), allAbKeys)
    val cohMeans        = computeColumnMeans(records.map(_.cohFeatures), allCohKeys)

    val (validRaw, tooIncomplete) = records.partition { r =>
      r.abFeatures.size.toDouble  / maxAbCols.max(1)  >= MIN_AB_COVERAGE &&
        r.cohFeatures.size.toDouble / maxCohCols.max(1) >= MIN_COH_COVERAGE
    }

    if (tooIncomplete.nonEmpty)
      logger.warn(s"${tooIncomplete.size} records supprimés (couverture insuffisante)")

    var educationImputed = 0
    var iqImputed        = 0
    var abImputed        = 0
    var cohImputed       = 0

    val cleaned = validRaw.map { r =>
      val edu = r.education.getOrElse { educationImputed += 1; medianEducation }
      val iq  = r.iq.getOrElse { iqImputed += 1; medianIQ }

      val abComplete = allAbKeys.map { key =>
        r.abFeatures.get(key) match {
          case Some(v) => key -> v
          case None    => abImputed += 1; key -> abMeans.getOrElse(key, 0.0)
        }
      }.toMap

      val cohComplete = allCohKeys.map { key =>
        r.cohFeatures.get(key) match {
          case Some(v) => key -> v
          case None    => cohImputed += 1; key -> cohMeans.getOrElse(key, 0.0)
        }
      }.toMap

      CleanedEEGRecord(
        id               = r.id,
        sex              = normalizeSex(r.sex),
        age              = r.age,
        eegDate          = r.eegDate,
        education        = edu,
        iq               = iq,
        mainDisorder     = r.mainDisorder.trim,
        specificDisorder = r.specificDisorder.trim,
        abFeatures       = abComplete,
        cohFeatures      = cohComplete
      )
    }

    logger.info(s"Nettoyage terminé : ${cleaned.size} records | edu=$educationImputed IQ=$iqImputed AB=$abImputed COH=$cohImputed imputés")
    (cleaned, CleaningReport(records.size, cleaned.size, tooIncomplete.size, educationImputed, iqImputed, abImputed, cohImputed))
  }

  private def normalizeSex(raw: String): String = raw.trim.toUpperCase match {
    case "M" | "MALE"   => "M"
    case "F" | "FEMALE" => "F"
    case other          => logger.warn(s"Sexe inattendu : '$other'"); other
  }

  private def computeMedianInt(values: List[Int]): Int = {
    if (values.isEmpty) return 0
    val sorted = values.sorted
    val mid    = sorted.size / 2
    if (sorted.size % 2 == 0) (sorted(mid - 1) + sorted(mid)) / 2 else sorted(mid)
  }

  private def computeColumnMeans(
                                  featureMaps: List[Map[String, Double]],
                                  allKeys:     List[String]
                                ): Map[String, Double] =
    allKeys.map { key =>
      val values = featureMaps.flatMap(_.get(key))
      key -> (if (values.isEmpty) 0.0 else values.sum / values.size)
    }.toMap
}