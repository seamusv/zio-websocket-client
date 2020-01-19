package org.example.realtime

import sttp.client._
import sttp.client.asynchttpclient.WebSocketHandler
import sttp.client.asynchttpclient.zio.ZioWebSocketHandler
import sttp.client.ws.WebSocket
import zio.{ Task, UIO, ZIO }

trait RealtimeClient {
  val realtimeClient: RealtimeClient.Service[Any]
}

object RealtimeClient {

  trait Service[R] {
    def openWebsocket(request: Request[Either[String, String], Nothing]): ZIO[R, Throwable, WebSocket[Task]]
  }

  def make(backend: SttpBackend[Task, Nothing, WebSocketHandler]): UIO[RealtimeClient] =
    UIO.effectTotal(new RealtimeClient {

      override val realtimeClient: Service[Any] = new Service[Any] {

        override def openWebsocket(request: Request[Either[String, String], Nothing]): ZIO[Any, Throwable, WebSocket[Task]] =
          ZioWebSocketHandler()
            .flatMap { handler =>
              backend.openWebsocket(request, handler)
            }
            .map(_.result)
      }
    })

}
