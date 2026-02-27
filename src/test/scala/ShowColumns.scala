package eeg.utils

import scala.io.Source

object ShowColumns extends App {
  val source = Source.fromResource("dataset/EEG.machinelearing_data_BRMH.csv")
  val headerLine = source.getLines().next()  // récupère la première ligne
  source.close()

  val columns = headerLine.split(",").map(_.trim)
  println("Liste des colonnes :")
  columns.foreach(println)
}