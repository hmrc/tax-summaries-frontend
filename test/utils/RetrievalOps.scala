package utils

import uk.gov.hmrc.auth.core.retrieve.~

object RetrievalOps {
  implicit class Ops[A](a: A) {
    def ~[B](b: B): A ~ B = new ~(a, b)
  }
}
