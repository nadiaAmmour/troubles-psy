package eeg.normalizer

import com.typesafe.scalalogging.LazyLogging
import eeg.model.{CleanedEEGRecord, FeatureStats, NormalizedEEGRecord}


object DataNormalizer extends LazyLogging {


  def normalize(records: List[CleanedEEGRecord]): (List[NormalizedEEGRecord], Map[String, FeatureStats]) = {

    logger.info(s"Démarrage de la normalisation sur ${records.size} records")

    if (records.isEmpty) return (List.empty, Map.empty)

    //  min/max 
    val ageStats = computeStats(records.map(_.age.toDouble))
    val eduStats = computeStats(records.map(_.education.toDouble))
    val iqStats  = computeStats(records.map(_.iq.toDouble))

    val allAbKeys  = records.head.abFeatures.keys.toList.sorted
    val allCohKeys = records.head.cohFeatures.keys.toList.sorted

    // feature EEG 
    val abStats  = computeFeatureStats(records.map(_.abFeatures), allAbKeys)
    val cohStats = computeFeatureStats(records.map(_.cohFeatures), allCohKeys)

    val allStats = Map("age" -> ageStats, "education" -> eduStats, "IQ" -> iqStats) ++ abStats ++ cohStats

    val normalized = records.map { r =>
      NormalizedEEGRecord(
        id               = r.id,
        sex              = if (r.sex == "F") 1 else 0, //  F=1, M=0
        age              = minMaxScale(r.age.toDouble, ageStats),
        education        = minMaxScale(r.education.toDouble, eduStats),
        iq               = minMaxScale(r.iq.toDouble, iqStats),
        mainDisorder     = r.mainDisorder,
        specificDisorder = r.specificDisorder,
        abFeatures       = normalizeMap(r.abFeatures, abStats),
        cohFeatures      = normalizeMap(r.cohFeatures, cohStats)
      )
    }

    logger.info(s"Normalisation terminée : ${normalized.size} records")
    (normalized, allStats)
  }

  
  private def minMaxScale(value: Double, stats: FeatureStats): Double = {
    val range = stats.max - stats.min
    if (range == 0.0) 0.0 else (value - stats.min) / range
  }

  // min et max 
  private def computeStats(values: List[Double]): FeatureStats =
    if (values.isEmpty) FeatureStats(0.0, 0.0)
    else FeatureStats(values.min, values.max)

  private def computeFeatureStats(
    featureMaps: List[Map[String, Double]],
    keys:        List[String]
  ): Map[String, FeatureStats] =
    keys.map(key => key -> computeStats(featureMaps.flatMap(_.get(key)))).toMap


  private def normalizeMap(
    features: Map[String, Double],
    stats:    Map[String, FeatureStats]
  ): Map[String, Double] =
    features.map { case (key, value) =>
      key -> minMaxScale(value, stats.getOrElse(key, FeatureStats(0.0, 1.0)))
    }
}