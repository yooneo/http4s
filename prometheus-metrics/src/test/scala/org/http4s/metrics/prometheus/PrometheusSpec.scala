package org.http4s.metrics.prometheus

import cats.effect.IO
import cats.implicits._
import java.util.UUID
import org.http4s._
import org.scalacheck.{Arbitrary, Gen}
import Prometheus.classifierFMethodWithOptionallyExcludedPath

object PrometheusSpec {

  private implicit val arbUUID: Arbitrary[UUID] =
    Arbitrary(Gen.uuid)
}

class PrometheusSpec extends Http4sSpec {

  import PrometheusSpec.arbUUID

  "classifierFMethodWithOptionallyExcludedPath" should {
    "properly exclude UUIDs" in prop {
      (method: Method, uuid: UUID, excludedValue: String, separator: String) =>
        {
          val request: Request[IO] = Request[IO](
            method = method,
            uri = Uri.unsafeFromString(s"/users/$uuid/comments")
          )

          val excludeUUIDs: String => Boolean = { str: String =>
            Either
              .catchOnly[IllegalArgumentException](UUID.fromString(str))
              .isRight
          }

          val classifier: Request[IO] => Option[String] =
            classifierFMethodWithOptionallyExcludedPath(
              exclude = excludeUUIDs,
              excludedValue = excludedValue,
              pathSeparator = separator
            )

          val result: Option[String] =
            classifier(request)

          val expected: Option[String] =
            Some(
              method.name.toLowerCase +
                separator.toLowerCase +
                "users" +
                separator.toLowerCase +
                excludedValue.toLowerCase +
                separator.toLowerCase +
                "comments"
            )

          result ==== expected
        }
    }
    "return '$method' if the path is '/'" in prop { method: Method =>
      val request: Request[IO] = Request[IO](
        method = method,
        uri = uri"""/"""
      )

      val classifier: Request[IO] => Option[String] =
        classifierFMethodWithOptionallyExcludedPath(
          _ => true,
          "*",
          "_"
        )

      val result: Option[String] =
        classifier(request)

      result ==== Some(method.name.toLowerCase)
    }
  }

}
