package org.example

import java.net.URI

import zio._
import zio.blocking.Blocking
import zio.clock.Clock
import zio.duration._

object Main extends App {

  override def run(args: List[String]): ZIO[zio.ZEnv, Nothing, Int] =
    (for {
      socketClient0 <- SocketClient.makeAsyncHttpSocketClient
      inboundQueue  <- Queue.unbounded[Array[Byte]]
      outboundQueue <- Queue.unbounded[Array[Byte]]
      _ <- SocketClient.>.openConnection(new URI("wss://echo.websocket.org"), inboundQueue, outboundQueue).sandbox
            .retry(Schedule.spaced(5.seconds))
            .fork
            .provideSome[ZEnv] { base =>
              new SocketClient with Blocking with Clock {
                override val socketClient: SocketClient.Service[Any] = socketClient0.socketClient
                override val blocking: Blocking.Service[Any]         = base.blocking
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
