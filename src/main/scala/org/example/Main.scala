package org.example

import org.example.realtime.RealtimeClient
import org.example.realtime.api._
import org.example.realtime.models.{ OutboundMessage, SendText }
import sttp.client.asynchttpclient.zio.AsyncHttpClientZioBackend
import zio._
import zio.console.{ getStrLn, _ }
import zio.stream.ZStream

object Main extends ManagedApp {

  private val layers = AsyncHttpClientZioBackend.layer() >>> RealtimeClient.live

  val pgm: ZManaged[Console with RealtimeClient, Throwable, Unit] =
    for {
      outgoing <- ZStream
                   .fromEffect(for {
                     input   <- getStrLn
                     message = SendText(input)
                   } yield message)
                   .forever
                   .toQueueUnbounded[Throwable, OutboundMessage]
      socketFiber <- (for {
                      receiver <- realtime.connect("wss://echo.websocket.org", ZStream.fromQueue(outgoing).forever.unTake)
                      _ <- receiver
                            .collectM {
                              case m => putStrLn(s"> $m")
                            }
                            .runDrain
                            .toManaged_
                    } yield ()).fork
      _ <- putStrLn("Ready for input!").toManaged_
      _ <- socketFiber.join.toManaged_
    } yield ()

  override def run(args: List[String]): ZManaged[zio.ZEnv, Nothing, Int] =
    (for {
      _ <- pgm.provideSomeLayer[Console](layers)
    } yield 0)
      .fold(_ => 1, _ => 0)
}
