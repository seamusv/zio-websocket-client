package org.example

import java.net.URI

import sttp.client.asynchttpclient.zio.{ AsyncHttpClientZioBackend, ZioWebSocketHandler }
import sttp.client.basicRequest
import sttp.client.ws.WebSocket
import sttp.model.Uri
import sttp.model.ws.WebSocketFrame
import zio.blocking.Blocking
import zio.clock.Clock
import zio.{ Queue, Task, ZIO }

trait SocketClient {

  val socketClient: SocketClient.Service[Any]

}

object SocketClient {

  trait Service[R] {

    def openConnection(
      uri: URI,
      inboundQueue: Queue[Array[Byte]],
      outboundQueue: Queue[Array[Byte]]
    ): ZIO[R, Throwable, Unit]

  }

  object > extends Service[SocketClient] {
    override def openConnection(
      uri: URI,
      inboundQueue: Queue[Array[Byte]],
      outboundQueue: Queue[Array[Byte]]
    ): ZIO[SocketClient, Throwable, Unit] =
      ZIO.accessM(_.socketClient.openConnection(uri, inboundQueue, outboundQueue))
  }

  trait AsyncHttpSocketClient extends SocketClient {
    def blocking: Blocking
    def clock: Clock

    override val socketClient: Service[Any] = new Service[Any] {
      override def openConnection(uri: URI, inboundQueue: Queue[Array[Byte]], outboundQueue: Queue[Array[Byte]]): ZIO[Any, Throwable, Unit] =
        for {
          handler <- ZioWebSocketHandler()
          _ <- AsyncHttpClientZioBackend().flatMap { implicit backend =>
                println("Attempting to connect...")
                basicRequest
                  .get(Uri(uri))
                  .openWebsocket(handler)
                  .flatMap { r =>
                    println("Websocket Established!")

                    val ws: WebSocket[Task] = r.result

                    val send: ZIO[Any, Throwable, Nothing] = outboundQueue.take
                      .tap(data => ZIO.effect(println(s"<<< sending out ${data.length} bytes")).as(data))
                      .flatMap(data => ws.send(WebSocketFrame.binary(data)))
                      .forever

                    def receive: ZIO[Any, Throwable, Unit] =
                      ws.receiveBinary(true).flatMap {
                        case Right(array) =>
                          inboundQueue.offer(array) *> receive

                        case Left(_) =>
                          ZIO
                            .effectTotal(println("Connection lost..."))
                            .unit
                      }

                    ZIO.raceAll(send, List(receive))
                      .tap(_ => ZIO.effect(println("The race completed.")) *> ZIO.unit)
                  }
              }
        } yield ()
    }
  }

  def makeAsyncHttpSocketClient: ZIO[Blocking with Clock, Nothing, SocketClient] =
    ZIO.access[Blocking with Clock] { r =>
      new AsyncHttpSocketClient {
        override def blocking: Blocking = r
        override def clock: Clock       = r
      }
    }
}
