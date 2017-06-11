package rsearle.monadic.dsl

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}


/**
  * @author Valentin Kasas
  * @author Richard Searle
  */
trait FutureAwaits {


  import java.util.concurrent.TimeUnit


  /**
    * Block until a Promise is redeemed with the specified timeout.
    */
  def await[T](future: Future[T], timeout: Long = 1, unit: TimeUnit =TimeUnit.SECONDS): T =
    Await.result(future, Duration(timeout, unit))

}
