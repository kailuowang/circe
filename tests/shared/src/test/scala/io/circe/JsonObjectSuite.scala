package io.circe

import cats.laws.discipline.eq._
import io.circe.tests.CirceSuite

class JsonObjectSuite extends CirceSuite {
  "+:" should "replace existing fields with the same key" in forAll { (j: Json, h: Json, t: List[Json]) =>
    val fields = (h :: t).zipWithIndex.map {
      case (j, i) => i.toString -> j
    }

    assert((("0" -> j) +: JsonObject.from(fields)).toList === (("0" -> j) :: fields.tail))
  }

  "size" should "return the size of the JSON object" in forAll { (js: List[Json]) =>
    val fields = js.zipWithIndex.map {
      case (j, i) => i.toString -> j
    }

    assert(JsonObject.from(fields).size === fields.size)
  }

  "withJsons" should "transform the JSON object appropriately" in forAll { (j: Json, js: List[Json]) =>
    val fields = js.zipWithIndex.map {
      case (j, i) => i.toString -> j
    }

    assert(JsonObject.from(fields).withJsons(_ => j).values === List.fill(js.size)(j))
  }

  "toList" should "return the appropriate list of key-value pairs" in forAll { (js: List[Json]) =>
    val fields = js.zipWithIndex.map {
      case (j, i) => i.toString -> j
    }.reverse

    assert(JsonObject.from(fields).toList === fields)
  }

  "values" should "return the values in the JSON object" in forAll { (js: List[Json]) =>
    val fields = js.zipWithIndex.map {
      case (j, i) => i.toString -> j
    }.reverse

    assert(JsonObject.from(fields).values === fields.map(_._2))
  }

  "traverse" should "transform the JSON object appropriately" in forAll { (js: List[Json]) =>
    val fields = js.zipWithIndex.map {
      case (j, i) => i.toString -> j
    }

    val o = JsonObject.from(fields)

    assert(o.traverse[Option](j => Some(j)) === Some(o))
  }
}
