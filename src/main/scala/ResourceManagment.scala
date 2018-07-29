import cats.effect.IO
import fs2.StreamApp.ExitCode
import fs2.{Chunk, Scheduler, Stream, StreamApp}

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
  def unConst(): String            = { scala.util.Random.alphanumeric.take(5).mkString }
}

object ResourceManagment extends StreamApp[IO] {

  val printer: Stream[IO, String] = for {
    res2 <- Stream.bracket(IO(new Resource1))({ r1 =>
      Stream.eval(IO(r1.open))
    }, { r1 =>
      IO(r1.close())
    })
    res3 <- Stream.bracket(IO.pure(res2))({ r2 =>
      Stream.eval(IO(r2.open))
    }, { r2 =>
      IO(r2.close())
    })
    character <- Stream.bracket(IO.pure(res3))({ r3 =>
      Stream.eval(IO(r3.open.unConst)).repeat
    }, { r3 =>
      IO(r3.close())
    })
  } yield character

  override def stream(args: List[String], requestShutdown: IO[Unit]): fs2.Stream[IO, ExitCode] =
    (printer
      .flatMap(x => Stream.chunk(Chunk.bytes((x + "\n").getBytes))))
      .through(fs2.io.stdout)
      .take(10)
      .drain
      .covaryOutput[ExitCode] ++ Stream.emit(ExitCode.Success)

}
