package org.example.realtime

import org.example.realtime.models.{ AppEvent, OutboundMessage, StringResponse }
import sttp.client._
import sttp.client.ws.WebSocket
import sttp.model.ws.WebSocketFrame
import zio.stream.{ Take, ZStream }
import zio.{ IO, Task, ZIO, ZManaged }

object Rtm {
  type MessageStream = ZStream[Any, Throwable, AppEvent]

  def parseMessage(message: String): Task[AppEvent] =
    IO.effect(StringResponse(message))

  trait Service {

    private def readMessage(ws: WebSocket[Task]): ZIO[Any, Throwable, Take[Nothing, AppEvent]] =
      ws.receiveText().flatMap(_.fold(_ => ZIO.succeed(Take.End), value => parseMessage(value).map(Take.Value(_))))

    def connect[R, E1 >: Throwable](url: String, outbound: ZStream[R, E1, OutboundMessage]): ZManaged[R with RealtimeEnv, E1, MessageStream] =
      for {
        ws <- openWebsocket(basicRequest.get(uri"$url")).toManaged_
        _ <- (for {
              queue <- outbound.toQueueUnbounded[E1, OutboundMessage]
              _ <- ZStream.fromQueue(queue).forever.unTake.zipWithIndex.foreachWhileManaged {
                    case (event, idx) =>
                      for {
                        isOpen <- ws.isOpen
                        _      <- ZIO.when(isOpen)(ws.send(WebSocketFrame.text(s"Payload: $idx - $event")))
                      } yield isOpen
                  }
            } yield ()).fork
        receive = ZStream
          .fromEffect(readMessage(ws))
          .forever
          .unTake
      } yield receive
  }

}
