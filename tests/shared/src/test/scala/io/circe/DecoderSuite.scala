package io.circe

import cats.data.Xor
import cats.laws.discipline.{ MonadErrorTests, SemigroupKTests }
import cats.laws.discipline.arbitrary._
import io.circe.parser.parse
import io.circe.syntax._
import io.circe.tests.CirceSuite

class DecoderSuite extends CirceSuite with LargeNumberDecoderTests {
  checkLaws("Decoder[Int]", MonadErrorTests[Decoder, DecodingFailure].monadError[Int, Int, Int])
  checkLaws("Decoder[Int]", SemigroupKTests[Decoder].semigroupK[Int])

  "prepare" should "do nothing when used with ok" in forAll { (i: Int) =>
    assert(Decoder[Int].prepare(ACursor.ok).decodeJson(i.asJson) === Xor.right(i))
  }

  it should "move appropriately with downField" in forAll { (i: Int, k: String, m: Map[String, Int]) =>
    assert(Decoder[Int].prepare(_.downField(k)).decodeJson(m.updated(k, i).asJson) === Xor.right(i))
  }

  "emap" should "do nothing when used with right" in forAll { (i: Int) =>
    assert(Decoder[Int].emap(Xor.right).decodeJson(i.asJson) === Xor.right(i))
  }

  it should "appropriately transform the result with an operation that can't fail" in forAll { (i: Int) =>
    assert(Decoder[Int].emap(v => Xor.right(v + 1)).decodeJson(i.asJson) === Xor.right(i + 1))
  }

  it should "appropriately transform the result with an operation that may fail" in forAll { (i: Int) =>
    val decoder = Decoder[Int].emap(v => if (v % 2 == 0) Xor.right(v) else Xor.left("Odd"))
    val expected = if (i % 2 == 0) Xor.right(i) else Xor.left(DecodingFailure("Odd", Nil))

    assert(decoder.decodeJson(i.asJson) === expected)
  }

  "failedWithMessage" should "replace the message" in forAll { (json: Json) =>
    assert(Decoder.failedWithMessage[Int]("Bad").decodeJson(json) === Xor.left(DecodingFailure("Bad", Nil)))
  }

  "An optional object field decoder" should "fail appropriately" in {
    val decoder: Decoder[Option[String]] = Decoder.instance(
      _.downField("").downField("").as[Option[String]]
    )

    forAll { (json: Json) =>
      val result = decoder.apply(json.hcursor)

      assert(
        json.asObject match {
          // The top-level value isn't an object, so we should fail.
          case None => result.isLeft
          case Some(o1) => o1("") match {
            // The top-level object doesn't contain a "" key, so we should succeed emptily.
            case None => result === Xor.Right(None)
            case Some(j2) => j2.asObject match {
              // The second-level value isn't an object, so we should fail.
              case None => result.isLeft
              case Some(o2) => o2("") match {
                // The second-level object doesn't contain a "" key, so we should succeed emptily.
                case None => result === Xor.Right(None)
                // The third-level value is null, so we succeed emptily.
                case Some(j3) if j3.isNull => result === Xor.Right(None)
                case Some(j3) => j3.asString match {
                  // The third-level value isn't a string, so we should fail.
                  case None => result.isLeft
                  // The third-level value is a string, so we should have decoded it.
                  case Some(s3) => result === Xor.Right(Some(s3))
                }
              }
            }
          }
        }
      )
    }
  }

  "An optional array position decoder" should "fail appropriately" in {
    val decoder: Decoder[Option[String]] = Decoder.instance(
      _.downN(0).downN(1).as[Option[String]]
    )

    forAll { (json: Json) =>
      val result = decoder.apply(json.hcursor)

      assert(
        json.asArray match {
          // The top-level value isn't an array, so we should fail.
          case None => result.isLeft
          case Some(a1) => a1.lift(0) match {
            // The top-level array is empty, so we should succeed emptily.
            case None => result === Xor.Right(None)
            case Some(j2) => j2.asArray match {
              // The second-level value isn't an array, so we should fail.
              case None => result.isLeft
              case Some(a2) => a2.lift(1) match {
                // The second-level array doesn't have a second element, so we should succeed emptily.
                case None => result === Xor.Right(None)
                // The third-level value is null, so we succeed emptily.
                case Some(j3) if j3.isNull => result === Xor.Right(None)
                case Some(j3) => j3.asString match {
                  // The third-level value isn't a string, so we should fail.
                  case None => result.isLeft
                  // The third-level value is a string, so we should have decoded it.
                  case Some(s3) => result === Xor.Right(Some(s3))
                }
              }
            }
          }
        }
      )
    }
  }

  "Decoder[Byte]" should "fail on out-of-range values (#83)" in forAll { (l: Long) =>
    val json = Json.fromLong(l)
    val result = Decoder[Byte].apply(json.hcursor)

    assert(if (l.toByte.toLong == l) result === Xor.right(l.toByte) else result.isEmpty)
  }

  it should "fail on non-whole values (#83)" in forAll { (d: Double) =>
    val json = Json.fromDoubleOrNull(d)
    val result = Decoder[Byte].apply(json.hcursor)

    assert(d.isWhole || result.isEmpty)
  }

  it should "succeed on whole decimal values (#83)" in forAll { (v: Byte, n: Byte) =>
    val zeros = "0" * (math.abs(n.toInt) + 1)
    val Xor.Right(json) = parse(s"$v.$zeros")

    assert(Decoder[Byte].apply(json.hcursor) === Xor.right(v))
  }

  "Decoder[Short]" should "fail on out-of-range values (#83)" in forAll { (l: Long) =>
    val json = Json.fromLong(l)
    val result = Decoder[Short].apply(json.hcursor)

    assert(if (l.toShort.toLong == l) result === Xor.right(l.toShort) else result.isEmpty)
  }

  it should "fail on non-whole values (#83)" in forAll { (d: Double) =>
    val json = Json.fromDoubleOrNull(d)
    val result = Decoder[Short].apply(json.hcursor)

    assert(d.isWhole || result.isEmpty)
  }

  it should "succeed on whole decimal values (#83)" in forAll { (v: Short, n: Byte) =>
    val zeros = "0" * (math.abs(n.toInt) + 1)
    val Xor.Right(json) = parse(s"$v.$zeros")

    assert(Decoder[Short].apply(json.hcursor) === Xor.right(v))
  }

  "Decoder[Int]" should "fail on out-of-range values (#83)" in forAll { (l: Long) =>
    val json = Json.fromLong(l)
    val result = Decoder[Int].apply(json.hcursor)

    assert(if (l.toInt.toLong == l) result === Xor.right(l.toInt) else result.isEmpty)
  }

  it should "fail on non-whole values (#83)" in forAll {(d: Double) =>
    val json = Json.fromDoubleOrNull(d)
    val result = Decoder[Int].apply(json.hcursor)

    assert(d.isWhole || result.isEmpty)
  }

  it should "succeed on whole decimal values (#83)" in forAll { (v: Int, n: Byte) =>
    val zeros = "0" * (math.abs(n.toInt) + 1)
    val Xor.Right(json) = parse(s"$v.$zeros")

    assert(Decoder[Int].apply(json.hcursor) === Xor.right(v))
  }

  "Decoder[Long]" should "fail on out-of-range values (#83)" in forAll { (i: BigInt) =>
    val json = Json.fromBigDecimal(BigDecimal(i))
    val result = Decoder[Long].apply(json.hcursor)

    assert(if (BigInt(i.toLong) == i) result === Xor.right(i.toLong) else result.isEmpty)
  }

  it should "fail on non-whole values (#83)" in forAll { (d: Double) =>
    val json = Json.fromDoubleOrNull(d)
    val result = Decoder[Long].apply(json.hcursor)

    assert(d.isWhole || result.isEmpty)
  }

  "Decoder[Float]" should "attempt to parse string values as doubles (#173)" in forAll { (d: Float) =>
    val Xor.Right(json) = parse("\"" + d.toString + "\"")

    assert(Decoder[Float].apply(json.hcursor) === Xor.right(d))
  }

  "Decoder[Double]" should "attempt to parse string values as doubles (#173)" in forAll { (d: Double) =>
    val Xor.Right(json) = parse("\"" + d.toString + "\"")

    assert(Decoder[Double].apply(json.hcursor) === Xor.right(d))
  }

  "Decoder[BigInt]" should "fail when producing a value would be intractable" in {
    val Xor.Right(bigNumber) = parse("1e2147483647")

    assert(Decoder[BigInt].apply(bigNumber.hcursor).isEmpty)
  }
}
