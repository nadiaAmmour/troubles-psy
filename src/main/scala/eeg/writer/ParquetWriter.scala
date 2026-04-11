package eeg.writer

import com.github.mjakubowski84.parquet4s.{ParquetWriter => Parquet4sWriter, Path}
import com.typesafe.scalalogging.LazyLogging
import eeg.model.NormalizedEEGRecord

object ParquetWriter extends LazyLogging {

  case class ParquetRecord(
                            id:                Int,
                            sex:               Int,
                            age:               Double,
                            education:         Double,
                            iq:                Double,
                            main_disorder:     String,
                            specific_disorder: String,
                            ab_features:       Map[String, Double],
                            coh_features:      Map[String, Double]
                          )

  def write(records: List[NormalizedEEGRecord], outputPath: String): Unit = {
    logger.info(s"Ecriture Parquet vers : $outputPath")

    val parquetRecords = records.map { r =>
      ParquetRecord(
        id                = r.id,
        sex               = r.sex,
        age               = r.age,
        education         = r.education,
        iq                = r.iq,
        main_disorder     = r.mainDisorder,
        specific_disorder = r.specificDisorder,
        ab_features       = r.abFeatures,
        coh_features      = r.cohFeatures
      )
    }

    Parquet4sWriter
      .of[ParquetRecord]
      .writeAndClose(Path(outputPath), parquetRecords)

    logger.info(s"Parquet ecrit : ${records.size} records")
  }
}