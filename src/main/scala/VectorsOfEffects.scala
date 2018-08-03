import cats._
import cats.instances.vector._
import cats.syntax.all._
import cats.effect._
import scala.concurrent.duration._

object VectorOfEffects extends IOApp {

  case class AttackResult(died: Int)

  def retryWithBackoff[A](ioa: IO[A], initialDelay: FiniteDuration, maxRetries: Int)(
      implicit timer: Timer[IO]): IO[A] = {

    ioa.handleErrorWith { error =>
      if (maxRetries > 0)
        IO.sleep(initialDelay) *> retryWithBackoff(ioa, initialDelay * 2, maxRetries - 1)
      else
        IO.raiseError(error)
    }
  }

  def logError[A](io: IO[A]): IO[A] = {
    io.handleErrorWith {
      case t: Throwable => IO(t.printStackTrace) *> io
    }
  }
  def launcheMissles(cityId: Int): IO[AttackResult] =
    for {
      rand <- IO(scala.util.Random.nextInt())
      _    <- IO(if (rand % 2 == 0) throw new RuntimeException(cityId.toString))
      -    <- IO(println(s"Destroying $cityId"))
    } yield AttackResult(rand)

  def launcheMisslesWithRetry(cityId: Int)(implicit timer: Timer[IO]): IO[AttackResult] =
    logError(retryWithBackoff(launcheMissles(cityId), 0.seconds, 10))

  val citiesToDestroy: Vector[Int] = (1 to 100).toVector

  def run(args: List[String]): IO[ExitCode] =
    for {
      launched <- citiesToDestroy.traverse(i => launcheMisslesWithRetry(i).attempt)
      result = launched.flatMap(_.toOption)
    } yield {
      ExitCode.Success
    }
}
