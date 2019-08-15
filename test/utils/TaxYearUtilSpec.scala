package utils

import play.api.test.FakeRequest
import uk.gov.hmrc.play.test.UnitSpec

class TaxYearUtilSpec extends UnitSpec {

  "TaxYearUtil" should {
    "extract tax year when a valid tax year is present" in {

      val taxYear = "2019"

      implicit val request = FakeRequest().withHeaders((taxYear, "2019"))

      val result = TaxYearUtil.extractTaxYear

      result shouldBe Some(taxYear)

    }
  }

}
