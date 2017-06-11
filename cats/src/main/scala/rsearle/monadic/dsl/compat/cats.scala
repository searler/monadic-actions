package rsearle.monadic.dsl.compat

import _root_.cats.data.{OptionT, Validated, Xor, XorT}
import _root_.cats.instances.future._
import _root_.cats.{Functor, Monad}
import rsearle.monadic.dsl.{Step, StepOps}

import scala.concurrent.{ExecutionContext, Future}
import scala.language.implicitConversions

/**
  *
  * inspired by  http://underscore.io/blog/posts/2016/12/05/type-lambdas.html
  *
  * @author Valentin Kasas
  * @author Richard Searle
  */

case class cats[R]() {

  type StepR[A] = Step[A, R]

  // CatsToStepOps

  implicit def xorToStep[A, B](xor: B Xor A)(
    implicit ec: ExecutionContext): StepOps[A, B, R] = new StepOps[A, B, R] {
    override def orFailWith(failureHandler: B => R): Step[A, R] =
      Step(Future.successful(xor.leftMap(failureHandler).toEither))
  }

  implicit def validatedToStep[A, B](validated: Validated[B, A])(
    implicit ec: ExecutionContext): StepOps[A, B, R] = new StepOps[A, B, R] {
    override def orFailWith(failureHandler: B => R): Step[A, R] =
      Step(Future.successful(validated.leftMap(failureHandler).toEither))
  }

  implicit def futureXorToStep[A, B](futureXor: Future[B Xor A])(
    implicit ec: ExecutionContext): StepOps[A, B, R] = new StepOps[A, B, R] {
    override def orFailWith(failureHandler: B => R): Step[A, R] =
      Step(futureXor.map(_.leftMap(failureHandler).toEither))
  }

  implicit def xortFutureToStep[A, B](xortFuture: XorT[Future, B, A])(
    implicit ec: ExecutionContext): StepOps[A, B, R] = new StepOps[A, B, R] {
    override def orFailWith(failureHandler: B => R): Step[A, R] =
      Step(xortFuture.leftMap(failureHandler).toEither)
  }

  implicit def optiontFutureToStep[A](optiontFuture: OptionT[Future, A])(
    implicit ec: ExecutionContext): StepOps[A, Unit, R] = new StepOps[A, Unit, R] {
    override def orFailWith(failureHandler: Unit => R): Step[A, R] =
      Step(
        optiontFuture
          .cata[Either[R, A]](Left(failureHandler(())), Right(_)))
  }

  implicit def futureValidatedToStep[A, B](
                                            futureValidated: Future[Validated[B, A]])(
                                            implicit ec: ExecutionContext): StepOps[A, B, R] = new StepOps[A, B, R] {
    override def orFailWith(failureHandler: B => R): Step[A, R] =
      Step(futureValidated.map(_.leftMap(failureHandler).toEither)(ec))
  }

  // CatsStepInstances

  implicit def stepFunctor(implicit ec: ExecutionContext): Functor[StepR] =
    new Functor[StepR] {
      override def map[A, B](fa: Step[A, R])(f: (A) => B): Step[B, R] = fa map f
    }

  implicit def stepMonad(implicit ec: ExecutionContext, futureMonad: Monad[Future]): Monad[StepR] =
    new Monad[StepR] {

      override def flatMap[A, B](fa: Step[A, R])(f: (A) => Step[B, R]): Step[B, R] =
        fa flatMap f

      override def pure[A](x: A): Step[A, R] = Step.unit(x)

      override def tailRecM[A, B](a: A)(f: (A) => Step[Either[A, B], R]): Step[B, R] =
        defaultTailRecM(a)(f) // maybe not the best thing to do
    }


}
