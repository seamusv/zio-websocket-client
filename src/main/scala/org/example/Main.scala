package org.example

import java.net.URI

import org.example.SocketClient.AsyncHttpSocketClient
import zio._
import zio.clock.Clock
import zio.duration._

object Main extends App {

  override def run(args: List[String]): ZIO[zio.ZEnv, Nothing, Int] =
    (for {
      inboundQueue  <- Queue.unbounded[Array[Byte]]
      outboundQueue <- Queue.unbounded[Array[Byte]]
      _ <- SocketClient.>.openConnection(new URI("ws://echo.websocket.org"), inboundQueue, outboundQueue).sandbox
            .repeat(Schedule.spaced(5.seconds))
            .fork
            .provideSome[ZEnv] { base =>
              new SocketClient with Clock {
                override val socketClient: SocketClient.Service[Any] = AsyncHttpSocketClient.socketClient
                override val clock: Clock.Service[Any]               = base.clock
              }
            }
      _ <- inboundQueue.take.flatMap(a => console.putStrLn(s"> ${new String(a)}")).forever.fork
      _ <- console.putStrLn("Enter your text and it will be echoed back.\n")
      _ <- console.getStrLn.flatMap(s => outboundQueue.offer(s.trim.getBytes)).forever.unit
    } yield ())
      .foldM(
        _ => console.putStrLn(s"Error: ") as 1,
        _ => ZIO.succeed(0)
      )
}
