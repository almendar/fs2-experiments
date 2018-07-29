import cats.effect.IO
import fs2.StreamApp.ExitCode
import fs2.{Scheduler, Stream, StreamApp}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
object ConcurrentApp extends StreamApp[IO] {
  override def stream(args: List[String], requestShutdown: IO[Unit]): fs2.Stream[IO, ExitCode] =
    Scheduler.apply[IO](4).flatMap { implicit S =>
      for {
        ec <- (S.awakeEvery[IO](3.second) zip greetWorld)
          .take(2)
          .drain
          .covaryOutput[ExitCode.Success.type] ++ Stream.emit(ExitCode.Success)
      } yield ec

    }

  val greetWorld: Stream[IO, Unit] = Stream.repeatEval(IO(println("Hello world")))

}
