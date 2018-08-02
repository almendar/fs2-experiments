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
  def launcheMissles(cityId: Int): IO[AttackResult] = IO {
    val ret = scala.util.Random.nextInt()
    if (ret % 2 == 0) throw new RuntimeException(cityId.toString)
    else {
      println(s"Destroying $cityId")
      AttackResult(ret)
    }
  }

  def launcheMisslesWithRetry(cityId: Int)(implicit timer: Timer[IO]): IO[AttackResult] =
    logError(retryWithBackoff(launcheMissles(cityId), 0.seconds, 10))

  val citiesToDestroy: Vector[Int] = (1 to 100).toVector

  def run(args: List[String]): IO[ExitCode] =
    citiesToDestroy
      .map(launcheMisslesWithRetry)
      .map(_.attempt)
      .sequence
      .map { x: Vector[Either[Throwable, AttackResult]] =>
        x.flatMap(y => y.toOption)
      }
      .as(ExitCode.Success)

}
