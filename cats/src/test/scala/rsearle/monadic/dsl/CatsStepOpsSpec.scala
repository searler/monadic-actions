package rsearle.monadic.dsl

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


import cats.data.{OptionT, Validated, Xor, XorT}
import cats.instances.future._
import org.specs2.matcher._
import org.specs2.mutable.Specification
import rsearle.monadic.dsl.compat._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * @author Valentin Kasas
  * @author Richard Searle
  */
class CatsStepOpsSpec extends Specification with FutureAwaits with Matchers {

  sealed trait Result

  case object NotFound extends Result

  implicit val catsResult = cats[Result]

  import catsResult._

  "ActionDSL.cats" should {

    "properly promote B Xor A to Step[A]" in {
      val aRight: String Xor Int = Xor.right(42)

      await((aRight ?|| NotFound).run) mustEqual Right(42)

      val aLeft: String Xor Int = Xor.left("Error")
      await((aLeft ?|| NotFound).run) mustEqual Left(NotFound)
    }

    "properly promote Future[B Xor A] to Step[A]" in {
      val futureRight: Future[Xor[Nothing, Int]] = Future.successful(Xor.right(42))
      await((futureRight ?|| NotFound).run) mustEqual Right(42)

      val futureLeft = Future.successful(Xor.left("Error"))
      await((futureLeft ?|| NotFound).run) mustEqual Left(NotFound)
    }

    "properly promote XorT[Future, B, A] to Step[A]" in {
      val xortFutureRight: XorT[Future, Unit, Int] = XorT.fromXor[Future](Xor.right(42))
      await((xortFutureRight ?|| NotFound).run) mustEqual Right(42)

      val futureLeft: XorT[Future, String, Unit] = XorT.fromXor[Future](Xor.left("Error"))
      await((futureLeft ?|| NotFound).run) mustEqual Left(NotFound)
    }

    "properly promote OptionT[Future, A] to Step[A]" in {
      val optiontFutureRight: OptionT[Future, Int] = OptionT.fromOption[Future](Option(42))
      await((optiontFutureRight ?|| NotFound).run) mustEqual Right(42)

      val optiontFutureLeft: OptionT[Future, Unit] = OptionT.fromOption[Future](None)
      await((optiontFutureLeft ?|| NotFound).run) mustEqual Left(NotFound)
    }

    "properly promote Validated[B,A] to Step[A]" in {
      val valid = Validated.Valid(42)
      await((valid ?|| NotFound).run) mustEqual Right(42)

      val fail = Validated.Invalid("Error")
      await((fail ?|| NotFound).run) mustEqual Left(NotFound)
    }


    "properly promote Future[Validated[B,A]] to Step[A]" in {
      val valid = Future.successful(Validated.Valid(42))
      await((valid ?|| NotFound).run) mustEqual Right(42)

      val fail: Future[Validated[String, Int]] = Future.successful(Validated.Invalid("Error"))
      await((fail ?|| NotFound).run) mustEqual Left(NotFound)
    }

  }
}
