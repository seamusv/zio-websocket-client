package org.example

import java.net.URI

import sttp.client.asynchttpclient.zio.AsyncHttpClientZioBackend
import sttp.client.basicRequest
import sttp.client.ws.WebSocket
import sttp.model.Uri
import sttp.model.ws.WebSocketFrame
import zio.{ Queue, Task, ZIO, ZManaged }

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
    override val socketClient: Service[Any] = new Service[Any] {
      override def openConnection(uri: URI, inboundQueue: Queue[Array[Byte]], outboundQueue: Queue[Array[Byte]]): ZIO[Any, Throwable, Unit] = {
        val acquire: ZIO[Any, Throwable, WebSocket[Task]] = for {
          _       <- ZIO.effect(println("Attempting to acquire connection"))
          handler <- CustomZioWebsocketHandler()
          backend <- AsyncHttpClientZioBackend()
          ws      <- backend.openWebsocket(basicRequest.get(Uri(uri)), handler)
        } yield ws.result

        ZManaged.make(acquire.sandbox.either)(_ => ZIO.unit).use {
          case Right(ws) =>
            println("Connection established")
            val send: ZIO[Any, Throwable, Unit] = for {
              data <- outboundQueue.take
              _    <- ws.send(WebSocketFrame.binary(data))
            } yield ()

            def receive: ZIO[Any, Throwable, Unit] =
              ws.receiveBinary(pongOnPing = true).flatMap {
                case Right(array) =>
                  inboundQueue.offer(array) *> receive

                case Left(_) =>
                  ZIO
                    .effectTotal(println("Connection lost..."))
                    .unit
              }

            ZIO
              .raceAll(send.forever, List(receive))
              .tap(_ => ZIO.effect(println("The race completed.")) *> ZIO.unit)

          case Left(cause) =>
            println(s"Failed: ${cause.prettyPrint}")
            ZIO.unit
        }
      }
    }
  }

  object AsyncHttpSocketClient extends AsyncHttpSocketClient
}
