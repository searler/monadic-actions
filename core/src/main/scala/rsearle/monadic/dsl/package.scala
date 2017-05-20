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
package rsearle.monadic

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

import scala.language.implicitConversions

/**
  * @author Valentin Kasas
  * @author Richard Searle
  */
package object dsl {

  case object escalate



  implicit class FutureOps[A,R](future: Future[A])(implicit ec: ExecutionContext) {
    @deprecated("Use infix `-| escalate` instead", "2.0.1")
    def -| : Step[A,R] = Step(future.map(Right(_)))

    def -| (escalateWord: escalate.type): Step[A,R] = Step(future.map(Right(_)))
  }

  implicit def futureToStepOps[A,R](future: Future[A])(
      implicit ec: ExecutionContext): StepOps[A, Throwable,R] =
    new StepOps[A, Throwable, R] {
      override def orFailWith(failureHandler: (Throwable) => R) =
        fromFuture(failureHandler)(future)
    }

  implicit def fOptionToStepOps[A,R](fOption: Future[Option[A]])(
      implicit ec: ExecutionContext): StepOps[A, Unit,R] =
    new StepOps[A, Unit,R] {
      override def orFailWith(failureHandler: Unit => R) =
        fromFOption(failureHandler(()))(fOption)
    }

  implicit def fEitherToStepOps[A, B,R](fEither: Future[Either[B, A]])(
      implicit ec: ExecutionContext): StepOps[A, B,R] =
    new StepOps[A, B,R] {
      override def orFailWith(failureHandler: (B) => R) =
        fromFEither(failureHandler)(fEither)
    }

  implicit def optionToStepOps[A,R](option: Option[A])(
      implicit ec: ExecutionContext): StepOps[A, Unit,R] =
    new StepOps[A, Unit,R] {
      override def orFailWith(failureHandler: (Unit) => R) =
        fromOption(failureHandler(()))(option)
    }

  implicit def eitherToStepOps[A, B,R](either: Either[B, A])(
      implicit ec: ExecutionContext): StepOps[A, B,R] =
    new StepOps[A, B,R] {
      override def orFailWith(failureHandler: (B) => R) =
        fromEither(failureHandler)(either)
    }


  implicit def booleanToStepOps[R](boolean: Boolean)(
      implicit ec: ExecutionContext): StepOps[Unit, Unit,R] =
    new StepOps[Unit, Unit,R] {
      override def orFailWith(failureHandler: (Unit) => R) =
        fromBoolean(failureHandler(()))(boolean)
    }

  implicit def tryToStepOps[A,R](tryValue: Try[A])(
      implicit ec: ExecutionContext): StepOps[A, Throwable,R] =
    new StepOps[A, Throwable,R] {
      override def orFailWith(failureHandler: (Throwable) => R) =
        fromTry(failureHandler)(tryValue)
    }

  implicit def stepToResult[R,S <: R](step: Step[S,R])(
      implicit ec: ExecutionContext): Future[R] =
    step.run.map(_.merge)

  implicit def stepToEither[A,R](step: Step[A,R]): Future[Either[R, A]] =
    step.run

  private[dsl] def fromFuture[A,R](onFailure: Throwable => R)(
      future: Future[A])(implicit ec: ExecutionContext): Step[A,R] =
    Step(
        run = future.map(Right[R, A](_)).recover {
          case t: Throwable => Left[R, A](onFailure(t))
        }
    )

  private[dsl] def fromFOption[A,R](onNone: => R)(
      fOption: Future[Option[A]])(implicit ec: ExecutionContext): Step[A,R] =
    Step(
        run = fOption.map {
          case Some(a) => Right(a)
          case None => Left(onNone)
        }
    )

  private[dsl] def fromFEither[A, B, R](onLeft: B => R)(
      fEither: Future[Either[B, A]])(implicit ec: ExecutionContext): Step[A,R] =
    Step(
        run = fEither.map(_.left.map(onLeft))
    )

  private[dsl] def fromOption[A,R](onNone: => R)(option: Option[A])(
      implicit ec: ExecutionContext): Step[A,R] =
    Step(
        run = Future.successful(
            option.fold[Either[R, A]](Left(onNone))(Right(_)))
    )

  private[dsl] def fromEither[A, B,R](onLeft: B => R)(either: Either[B, A])(
      implicit ec: ExecutionContext): Step[A,R] =
    Step(
        run = Future.successful(either.left.map(onLeft))
    )





  private[dsl] def fromBoolean[R](onFalse: => R)(boolean: Boolean)(
      implicit ec: ExecutionContext): Step[Unit,R] =
    Step(
        run = Future.successful(if (boolean) Right(()) else Left(onFalse))
    )

  private[dsl] def fromTry[A,R](onFailure: Throwable => R)(
      tryValue: Try[A])(implicit ec: ExecutionContext): Step[A,R] =
    Step(
        run = Future.successful(tryValue match {
          case Failure(t) => Left(onFailure(t))
          case Success(v) => Right(v)
        })
    )
}
