package mf.exercises

import java.util.concurrent.Executors

import cats.scalatest.EitherValues
import mf.models._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Millis, Seconds, Span}

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}

/**
  * Should fail if any call to Http.getBatch fails
  * Should ignore (drop) parsing failures if the ParsedResponse.parser fails to parse an element
  */
class Exercise4Test
    extends AnyFunSpec
    with Matchers
    with ScalaFutures
    with EitherValues {

  implicit val ec: ExecutionContextExecutor =
    ExecutionContext.fromExecutor(Executors.newFixedThreadPool(10))

  implicit val defaultPatience: PatienceConfig =
    PatienceConfig(timeout = Span(5, Seconds), interval = Span(1, Millis))

  val br = new Exercise4
  val n = 3

  describe("successful cases") {

    val xs = List(1, 2, 3, 4, 5, 6, 7, 8)
    val requests: List[Request] = xs.map(i => ValidRequest.apply(i.toString))
    val expected = xs.map(ParsedResponse.apply)

    it("Should return the input list, parsed, in order") {
      val result = br.requestBatch(requests, n)
      result.futureValue should contain theSameElementsInOrderAs expected
    }
    it("should ignore parsing failures") {
      val xs1 = List(1, 2, 3, 4).map(i => ValidRequest.apply(i.toString))
      val xs2 = List("jdsf", "dsfjsfs", "7", "8").map(ValidRequest.apply)
      val requests: List[Request] = xs1 ++ xs2
      val result = br.requestBatch(requests, n)
      val expected = List(1, 2, 3, 4, 7, 8).map(ParsedResponse.apply)
      result.futureValue should contain theSameElementsInOrderAs expected
    }

    it("should ignore any failures") {
      val xs1 = List("1", "2", "fail").map(ValidRequest.apply)
      val xs2 = List(BatchFail, BatchFail, BatchFail)
      val xs3 = List(8, 9, 10).map(i => ValidRequest.apply(i.toString))
      val requests: List[Request] = xs1 ++ xs2 ++ xs3
      val expected = List(1, 2, 8, 9, 10).map(ParsedResponse.apply)
      val result = br.requestBatch(requests, n)
      result.futureValue should contain theSameElementsInOrderAs expected
    }
  }
}
