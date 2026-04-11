package eeg.reader

import com.github.mjakubowski84.parquet4s.{Path, ParquetReader => Parquet4sReader}
import eeg.writer.ParquetWriter.ParquetRecord

object ParquetReader extends App {

  val path = Path(args(0))

  val records = Parquet4sReader.as[ParquetRecord].read(path).toList

  println(s"Nombre de lignes : ${records.size}")
  println("\n=== 5 premiers patients ===")
  records.take(5).foreach { r =>
    println(s"id=${r.id} | sex=${r.sex} | age=${r.age} | disorder=${r.main_disorder}")
    println(s"  AB features (3 premiers)  : ${r.ab_features.take(3)}")
    println(s"  COH features (3 premiers) : ${r.coh_features.take(3)}")
    println()
  }
}