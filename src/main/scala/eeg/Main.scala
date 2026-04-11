package eeg

import com.typesafe.scalalogging.LazyLogging
import eeg.cleaner.DataCleaner
import eeg.normalizer.DataNormalizer
import eeg.parser.CSVParser
import eeg.writer.ParquetWriter

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object Main extends App with LazyLogging {

  if (args.length < 2) {
    println("Usage: ms1-eeg <input_csv> <output_parquet>")
    sys.exit(1)
  }

  val inputPath  = args(0)
  val outputPath = args(1)

  val start = System.currentTimeMillis()
  logger.info("=== MS1 - EEG Preprocessor - DÉMARRAGE ===")

  // Étape 1 : Parsing
  logger.info("[1/3] PARSING...")
  val (rawRecords, skipped) = CSVParser.parse(inputPath)

  // Étape 2 : Cleaning
  logger.info("[2/3] NETTOYAGE...")
  val (cleanedRecords, cleanReport) = DataCleaner.clean(rawRecords)

  // Étape 3 : Normalisation
  logger.info("[3/3] NORMALISATION...")
  val (normalizedRecords, _) = DataNormalizer.normalize(cleanedRecords)

  // Écriture Parquet
  ParquetWriter.write(normalizedRecords, outputPath)

  val elapsed = (System.currentTimeMillis() - start) / 1000.0
  logger.info(s"=== MS1 terminé en ${elapsed}s | ${normalizedRecords.size} patients ===")
}