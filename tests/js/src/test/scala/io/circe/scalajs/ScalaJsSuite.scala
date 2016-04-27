package io.circe.scalajs

import cats.data.Xor
import io.circe.{ Decoder, Encoder }
import io.circe.generic.semiauto._
import io.circe.tests.CirceSuite
import scala.scalajs.js
import scalajs.js.Dynamic

case class Example(name: String)

object Example {
  implicit val decodeExample: Decoder[Example] = deriveDecoder
  implicit val encodeExample: Encoder[Example] = deriveEncoder
}

case class UndefOrExample(name: js.UndefOr[String])

object UndefOrExample {
  implicit val decodeUndefOrExample: Decoder[UndefOrExample] = deriveDecoder
  implicit val encodeUndefOrExample: Encoder[UndefOrExample] = deriveEncoder
}

class ScalaJsSuite extends CirceSuite {
  "decodeJs" should "decode js.Object" in forAll { (s: String) =>
    assert(decodeJs[Example](Dynamic.literal(name = s)).map(_.name) === Xor.right(s))
  }

  it should "handle undefined js.UndefOr when decoding js.Object" in {
    val res = decodeJs[UndefOrExample](Dynamic.literal(name = js.undefined))

    assert(res.map(_.name.isDefined) === Xor.right(false))
  }

  it should "handle defined js.UndefOr when decoding js.Object" in forAll { (s: String) =>
    assert(decodeJs[UndefOrExample](Dynamic.literal(name = s)).map(_.name.get) === Xor.right(s))
  }

  "asJsAny" should "encode to js.Object" in forAll { (s: String) =>
    assert(Example(s).asJsAny.asInstanceOf[js.Dynamic].name.toString === s)
  }

  it should "handle defined js.UndefOr when encoding to js.Object" in forAll { (s: String) =>
    assert(UndefOrExample(s).asJsAny.asInstanceOf[js.Dynamic].name.toString === s)
  }

  it should "handle undefined js.UndefOr when encoding to js.Object" in {
    assert(js.isUndefined(UndefOrExample(js.undefined).asJsAny.asInstanceOf[js.Dynamic].name))
  }
}
