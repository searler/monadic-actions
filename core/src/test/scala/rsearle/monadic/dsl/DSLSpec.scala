/*
 * Copyright 2014
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rsearle.monadic.dsl

import org.specs2.mutable.Specification
import org.specs2.matcher._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}
/**
 * @author Valentin Kasas
  * @author Richard Searle
 */
class DSLSpec extends Specification with FutureAwaits with Matchers {


  "dsl" should {


    "handle future chain of String" in {
      def normalize(idStr: String) = Future.successful(idStr.trim)

      def service(id: Long) = Future.successful(Some(id * 2))

      def action(idStr: String): Step[String, String] = {
          for {
            normed <- normalize(idStr)    -| escalate
            id     <- Try(normed.toLong)  ?|| "BadRequest"
            number <- service(id)         ?|| "NotFound"
          } yield number.toString
      }


      await(action("12").run) mustEqual Right("24")
      await(action("xxx").run) mustEqual Left("BadRequest")
      await(action(null).run)must throwA[NullPointerException]

    }

    "handle future chain of types" in {

      sealed trait Res
      case class A(id:Int) extends Res
      case class B(id:Int) extends Res
      case class C(id:Int) extends Res
      case class E(err:String) extends Res

      def normalize(idStr: String) = Future.successful(idStr.trim)

      def s1(id: Int) = Future.successful(A(id))
      def s2(a:A) = Future.successful(B(a.id))
      def s3(b:B) = Future.successful(C(b.id))

      def action(id:Int): Step[Res, Res] = {
        for {
          a <- s1(id) ?|| E("Bad Id")
          b <- s2(a) ?|| E("Bad A")
          c <- s3(b)?|| E("Bad B")
        } yield c
      }

      import rsearle.monadic.dsl.stepToResult
      def f(id:Int) :Future[Res] = action(id)


      await(action(12).run) mustEqual Right(C(12))
      await((f(12))) mustEqual C(12)


    }


    "properly promote Future[A] to Step[A]" in {
      val successfulFuture = Future.successful(42)
      await((successfulFuture ?|| "NotFound").run) mustEqual Right(42)

      val failedFuture = Future.failed[Int](new NullPointerException)
      await((failedFuture ?|| "NotFound").run) mustEqual Left("NotFound")

      await((successfulFuture -| escalate).run) mustEqual Right(42)
      await((failedFuture -| escalate).run) must throwA[NullPointerException]
    }

    "properly promote Future[Option[A]] to Step[A]" in {
      val someFuture = Future.successful(Some(42))
      await((someFuture ?|| "NotFound").run) mustEqual Right(42)

      val noneFuture = Future.successful[Option[Int]](None)
      await((noneFuture ?|| "NotFound").run) mustEqual Left("NotFound")
    }


    "properly promote Future[Either[B, A]] to Step[A]" in {
      val rightFuture = Future.successful[Either[String, Int]](Right(42))
      await((rightFuture ?|| "NotFound").run) mustEqual Right(42)

      val leftFuture: Future[Either[String, Int]] = Future.successful[Either[String, Int]](Left("foo"))
      val step: Step[Int,String] = leftFuture ?| (s => s"BadRequest($s)" )
      await(step.run) must beLeft

      val result = step.run.map(_.swap.right.getOrElse("NotFound"))
      await(result) mustEqual "BadRequest(foo)"

     
    }

    
    "properly promote Option[A] to Step[A]" in {
      val some = Some(42)
      await((some ?|| "NotFound").run) mustEqual Right(42)
      
      val none = None
      await((none ?|| "NotFound").run) mustEqual Left("NotFound")
    }


    "properly promote Either[B, A] to Step[A]" in {
      val right = Right[String, Int](42)
      await((right ?|| "NotFound").run) mustEqual Right(42)

      val left = Left[String, Int]("foo")
      val step = left ?| {s => s"BadRequest($s)"}
      await(step.run) must beLeft

      val result: Future[Object] = step.run.map(_.swap.right.getOrElse("NotFound"))
     await(result) mustEqual "BadRequest(foo)"


    }


    
    "properly promote Boolean to Step[A]" in {
      await((true ?|| "NotFound").run) mustEqual Right(())
      await((false ?|| "NotFound").run) mustEqual Left("NotFound")
    }
    
    "properly promote Try[A] to Step[A]" in {
      val success = Success(42)
      await((success ?|| "NotFound").run) mustEqual Right(42)
      
      val failure = Failure(new Exception("foo"))
      val step = failure ?| {e => s"BadRequest(${e.getMessage})"}
      await(step.run) must beLeft

      val result: Future[Object] = step.run.map(_.swap.right.getOrElse("NotFound"))
      await(result) mustEqual "BadRequest(foo)"


      
    }

    "support filtering in for-comprehensions" in {
      val someValueSatisfyingPredicate: Future[Option[Int]] = Future.successful(Some(20))

      val res1 = for {
        a <- someValueSatisfyingPredicate ?|| "NotFound" if a < 42
      } yield "Ok"

      await(res1.run.map(_.merge)) mustEqual "Ok"

      val noneValue: Future[Option[Int]] = Future.successful(None)


      val res2 = for {
        a <- noneValue ?|| "NotFound" if a < 42
      } yield "Ok"

      await(res2.run.map(_.merge)) mustEqual "NotFound"


      val someValueFailingPredicate = Future.successful(Some(64))

      val res3 = for {
        a <- someValueFailingPredicate ?|| "NotFound" if a < 42
      } yield "Ok"

      await(res3.run) must throwA[NoSuchElementException]


    }
  }


}
