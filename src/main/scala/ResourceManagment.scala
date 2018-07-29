import cats.effect.IO
import fs2.StreamApp.ExitCode
import fs2.{Scheduler, Stream, StreamApp}

trait PrintMe extends AutoCloseable { self =>
  println(s"Opening ${self.getClass}")
  type Res2 <: PrintMe
  def open(): Res2 = {
    create()
  }
  def create(): Res2
  override def close(): Unit = println(s"Closing ${self.getClass}")
}

class Resource1 extends PrintMe {
  type Res2 = Resource2
  override def create(): Resource2 = new Resource2
}

class Resource2 extends PrintMe {
  type Res2 = Resource3
  override def create(): Resource3 = new Resource3
}

class Resource3 extends PrintMe {
  type Res2 = Resource3
  override def create(): Resource3 = this
}

object ResourceManagment extends StreamApp[IO] {
  override def stream(args: List[String], requestShutdown: IO[Unit]): fs2.Stream[IO, ExitCode] =
    for {
      res2 <- Stream.bracket(IO(new Resource1))(use = { r1 =>
        Stream.eval(IO(r1.open))
      }, release = { r1 =>
        IO(r1.close())
      })
      res3 <- Stream.bracket(IO.pure(res2))(use = { r2 =>
        Stream.eval(IO(r2.open))
      }, release = { r2 =>
        IO(r2.close())
      })
    } yield ExitCode.Success

}
