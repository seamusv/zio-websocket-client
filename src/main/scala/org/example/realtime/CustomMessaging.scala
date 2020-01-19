package org.example.realtime

import org.example.api.realtime
import org.example.realtime.models.OutboundMessage
import sttp.client._
import sttp.client.ws.WebSocket
import sttp.model.ws.WebSocketFrame
import zio.stream.{ Take, ZStream }
import zio.{ IO, Task, ZIO, ZManaged }

trait CustomMessaging {
  val customMessaging: CustomMessaging.Service[Any]
}

object CustomMessaging {

  def parseMessage(message: String): Task[String] =
    IO.effect(message)

  trait Service[R] {

    private def readMessage(ws: WebSocket[Task]): ZIO[Any, Throwable, Take[Nothing, String]] =
      ws.receiveText().flatMap(_.fold(_ => ZIO.succeed(Take.End), value => parseMessage(value).map(Take.Value(_))))

    def connect[R0 <: R, E1 >: Throwable](url: String, outbound: ZStream[R0, E1, OutboundMessage]): ZManaged[R0 with RealtimeClient, Throwable, ZStream[Any, Throwable, String]] =
      for {
        ws <- realtime.openWebsocket(basicRequest.get(uri"$url")).toManaged_
        _ <- (for {
              queue <- outbound.toQueueUnbounded[E1, OutboundMessage]
              _ <- ZStream.fromQueue(queue).forever.unTake.zipWithIndex.foreachManaged {
                    case (event, idx) =>
                      ws.send(WebSocketFrame.text(s"Payload: $idx - $event"))
                  }
            } yield ()).fork
        receive = ZStream
          .fromEffect(readMessage(ws))
          .forever
          .unTake
      } yield receive
  }

}
