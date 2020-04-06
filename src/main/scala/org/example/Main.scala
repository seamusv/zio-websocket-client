package org.example

import java.io.IOException
import java.util.concurrent.TimeUnit

import org.example.realtime.RealtimeClient
import org.example.realtime.api._
import org.example.realtime.models.{ OutboundMessage, SendText }
import sttp.client.asynchttpclient.zio.AsyncHttpClientZioBackend
import sttp.client.{ SttpBackendOptions, SttpClientException }
import zio._
import zio.clock.Clock
import zio.console.{ getStrLn, _ }
import zio.duration._
import zio.random.Random
import zio.stream.ZStream

import scala.concurrent.duration.FiniteDuration

object Main extends App {

  private val sttpOptions = SttpBackendOptions.Default
    .httpProxy("localhost", 6969)
    .connectionTimeout(FiniteDuration(10, TimeUnit.SECONDS))

  private val layers = AsyncHttpClientZioBackend.layer(options = sttpOptions) >>> RealtimeClient.live

  val readConsole: ZIO[Console, IOException, SendText] = getStrLn.map(SendText)

  def retrySchedule(queue: Queue[String]): Schedule[Clock with Random, Throwable, (Duration, Throwable)] =
    Schedule.exponential(2.seconds).jittered && Schedule.doWhileM[Throwable] {
      case _: SttpClientException.ConnectException => queue.offer("Connection exception... retrying.").as(true)
      case _: SttpClientException.ReadException    => queue.offer("Read exception... dying.").as(false)
      case e                                       => queue.offer(s"Unknown error: ${e.getMessage}").as(false)
    }

  def run(args: List[String]): ZIO[ZEnv, Nothing, Int] =
    (for {
      eventMessagesQueue <- Queue.unbounded[String]
      _                  <- eventMessagesQueue.take.flatMap(console.putStrLn(_)).forever.fork
      outgoing           = ZStream.fromEffect(readConsole).forever.toQueueUnbounded[Throwable, OutboundMessage]
      _ <- outgoing.use(queue =>
            realtime
              .connect("wss://echo.websocket.org", ZStream.fromQueue(queue).forever.unTake)
              .use { stream =>
                println("CONNECTED!")
                stream.collectM {
                  case m => putStrLn(s"> $m")
                }.runDrain
              }
              .retry(retrySchedule(eventMessagesQueue))
              .repeat(Schedule.spaced(2.seconds))
              .provideSomeLayer[ZEnv](layers)
          )
    } yield 0)
      .foldM(
        err => console.putStrLn(s"Error: $err").as(1),
        ZIO.succeed(_)
      )
}
