package eeg

import eeg.cleaner.DataCleaner
import eeg.model.{CleanedEEGRecord, RawEEGRecord}
import eeg.normalizer.DataNormalizer
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class DataCleanerSpec extends AnyFlatSpec with Matchers {

  private def makeRaw(id: Int, edu: Option[Int] = Some(12), iq: Option[Int] = Some(100)): RawEEGRecord =
    RawEEGRecord(id, "M", 30, "2024.1.1", edu, iq, "Healthy control", "Healthy control",
      Map("AB.A.delta.a.FP1" -> 10.0), Map("COH.A.delta.a.FP1.b.FP2" -> 50.0))

  "DataCleaner" should "imputer education manquante par la médiane" in {
    val (cleaned, report) = DataCleaner.clean(List(makeRaw(1, edu = Some(10)), makeRaw(2, edu = Some(14)), makeRaw(3, edu = None)))
    report.educationImputed shouldBe 1
    cleaned.find(_.id == 3).map(_.education) shouldBe Some(12)
  }

  it should "imputer IQ manquant par la médiane" in {
    val (cleaned, report) = DataCleaner.clean(List(makeRaw(1, iq = Some(90)), makeRaw(2, iq = Some(110)), makeRaw(3, iq = None)))
    report.iqImputed shouldBe 1
    cleaned.find(_.id == 3).map(_.iq) shouldBe Some(100)
  }
}

class DataNormalizerSpec extends AnyFlatSpec with Matchers {

  private def makeClean(id: Int, abVal: Double): CleanedEEGRecord =
    CleanedEEGRecord(id, "M", 30, "2024.1.1", 12, 100, "Healthy control", "Healthy control",
      Map("AB.A.delta.a.FP1" -> abVal), Map("COH.A.delta.a.FP1.b.FP2" -> 50.0))

  "DataNormalizer" should "normaliser entre 0 et 1" in {
    val (normalized, _) = DataNormalizer.normalize(List(makeClean(1, 0.0), makeClean(2, 100.0)))
    normalized.find(_.id == 1).get.abFeatures("AB.A.delta.a.FP1") shouldBe 0.0
    normalized.find(_.id == 2).get.abFeatures("AB.A.delta.a.FP1") shouldBe 1.0
  }

  it should "encoder M=0, F=1" in {
    val records = List(makeClean(1, 10.0).copy(sex = "M"), makeClean(2, 10.0).copy(sex = "F"))
    val (normalized, _) = DataNormalizer.normalize(records)
    normalized.find(_.id == 1).get.sex shouldBe 0
    normalized.find(_.id == 2).get.sex shouldBe 1
  }
}