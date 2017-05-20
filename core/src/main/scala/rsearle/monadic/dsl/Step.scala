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

import scala.concurrent.{ExecutionContext, Future}

/**
  * @author Valentin Kasas
  * @author Richard Searle
  */
final case class Step[+A,+
R](run: Future[Either[R, A]]) {

  def map[B](f: A => B)(implicit ec: ExecutionContext) =
    copy(run = run.map(_.right.map(f)))

  def flatMap[B, S >: R ](f: A => Step[B,S])(implicit ec: ExecutionContext) =
    copy(run = run.flatMap(_.fold(err =>
                  Future.successful(Left[S, B](err)), succ =>
                  f(succ).run)))

  def withFilter(p: A => Boolean)(implicit ec: ExecutionContext): Step[A,R] =
    copy(run = run.filter {
      case Right(a) if p(a) => true
      case Left(e) => true
      case _ => false
    })

}

object Step {

  def unit[A,R](a: A): Step[A,R] = Step(Future.successful(Right(a)))

}

trait StepOps[A, B, R] {
  def orFailWith(failureHandler: B => R): Step[A,R]
  def ?|(failureHandler: B => R): Step[A,R] = orFailWith(failureHandler)
  def ?||(failureThunk: => R): Step[A,R] = orFailWith(_ => failureThunk)
}
