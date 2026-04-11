package eeg.model

case class RawEEGRecord(
  id:               Int,
  sex:              String,
  age:              Double,
  eegDate:          String,
  education:        Option[Int],
  iq:               Option[Int],
  mainDisorder:     String,
  specificDisorder: String,
  abFeatures:       Map[String, Double],
  cohFeatures:      Map[String, Double]
)

case class CleanedEEGRecord(
  id:               Int,
  sex:              String,
  age:              Double,
  eegDate:          String,
  education:        Int,
  iq:               Int,
  mainDisorder:     String,
  specificDisorder: String,
  abFeatures:       Map[String, Double],
  cohFeatures:      Map[String, Double]
)

case class NormalizedEEGRecord(
  id:               Int,
  sex:              Int,
  age:              Double,
  education:        Double,
  iq:               Double,
  mainDisorder:     String,
  specificDisorder: String,
  abFeatures:       Map[String, Double],
  cohFeatures:      Map[String, Double]
)

case class FeatureStats(min: Double, max: Double)

case class MS1Output(
  metadata:           OutputMetadata,
  normalizationStats: Map[String, FeatureStats],
  records:            List[NormalizedEEGRecord]
)

case class OutputMetadata(
  sourceFile:     String,
  totalRecords:   Int,
  removedRecords: Int,
  imputedFields:  Map[String, Int],
  featureCount:   FeatureCount,
  processedAt:    String
)

case class FeatureCount(
  abFeatures:  Int,
  cohFeatures: Int,
  total:       Int
)