package eeg.parser

import com.github.tototoshi.csv.CSVReader
import com.typesafe.scalalogging.LazyLogging
import eeg.model.RawEEGRecord

import java.io.File
import scala.util.{Failure, Success, Try}

object CSVParser extends LazyLogging {

  private val AB_PREFIX  = "AB."
  private val COH_PREFIX = "COH."

  def parse(filePath: String): (List[RawEEGRecord], Int) = {
    logger.info(s"Démarrage du parsing : $filePath")

    val file = new File(filePath)
    if (!file.exists()) throw new IllegalArgumentException(s"Fichier introuvable : $filePath")

    val reader = CSVReader.open(file)
    try {
      val allRows = reader.allWithHeaders()
      logger.info(s"${allRows.length} lignes trouvées dans le CSV")

      val sampleHeaders = allRows.headOption.map(_.keys.toList).getOrElse(List.empty)
      val abCols  = sampleHeaders.filter(_.startsWith(AB_PREFIX))
      val cohCols = sampleHeaders.filter(_.startsWith(COH_PREFIX))
      logger.info(s"Features AB: ${abCols.size}, Features COH: ${cohCols.size}")

      var skipped = 0
      val records = allRows.flatMap { row =>
        parseRow(row, abCols, cohCols) match {
          case Success(record) => Some(record)
          case Failure(ex) =>
            logger.warn(s"Ligne ignorée (id=${row.getOrElse("no.", "?")}) : ${ex.getMessage}")
            skipped += 1
            None
        }
      }

      logger.info(s"Parsing terminé : ${records.size} records valides, $skipped ignorés")
      (records, skipped)
    } finally {
      reader.close()
    }
  }

  private def parseRow(
    row:     Map[String, String],
    abCols:  List[String],
    cohCols: List[String]
  ): Try[RawEEGRecord] = Try {
    RawEEGRecord(
      id               = parseRequiredInt(row, "no."),
      sex              = parseRequiredString(row, "sex"),
      age              = parseRequiredDouble(row, "age"),
      eegDate          = parseRequiredString(row, "eeg.date"),
      education        = parseOptionalInt(row, "education"),
      iq               = parseOptionalInt(row, "IQ"),
      mainDisorder     = parseRequiredString(row, "main.disorder"),
      specificDisorder = parseRequiredString(row, "specific.disorder"),
      abFeatures       = extractNumericFeatures(row, abCols),
      cohFeatures      = extractNumericFeatures(row, cohCols)
    )
  }

  private def parseRequiredString(row: Map[String, String], col: String): String =
    row.getOrElse(col, throw new RuntimeException(s"Colonne manquante : $col")).trim match {
      case "" => throw new RuntimeException(s"Valeur vide : $col")
      case v  => v
    }

  private def parseRequiredInt(row: Map[String, String], col: String): Int = {
    val raw = parseRequiredString(row, col)
    raw.toIntOption.getOrElse(throw new RuntimeException(s"Valeur non entière pour $col : '$raw'"))
  }

  private def parseOptionalInt(row: Map[String, String], col: String): Option[Int] =
    row.get(col).map(_.trim).flatMap {
      case "" | "NA" | "N/A" | "null" => None
      case v => v.toIntOption
    }


  private def parseRequiredDouble(row: Map[String, String], col: String): Double = {
    val raw = parseRequiredString(row, col)
    raw.toDoubleOption.getOrElse(throw new RuntimeException(s"Valeur non numérique pour $col : '$raw'"))
  }

  private def extractNumericFeatures(
    row:  Map[String, String],
    cols: List[String]
  ): Map[String, Double] =
    cols.flatMap { col =>
      row.get(col).flatMap(_.trim match {
        case "" | "NA" | "N/A" => None
        case v => v.toDoubleOption.map(col -> _)
      })
    }.toMap
}