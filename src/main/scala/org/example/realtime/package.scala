package org.example

import sttp.client.Request
import sttp.client.asynchttpclient.zio.{ SttpClient, ZioWebSocketHandler }
import sttp.client.ws.WebSocket
import zio.{ Has, Task, ZIO, ZLayer }

package object realtime {

  type RealtimeClient = Has[RealtimeClient.Service]
  type RealtimeEnv    = RealtimeClient

  object RealtimeClient {

    trait Service {
      private[realtime] def openWebsocket(request: Request[Either[String, String], Nothing]): Task[WebSocket[Task]]
    }

    val live: ZLayer[SttpClient, Nothing, RealtimeClient] =
      ZLayer.fromFunction[SttpClient, RealtimeClient.Service](client =>
        new Service {

          override private[realtime] def openWebsocket(request: Request[Either[String, String], Nothing]): Task[WebSocket[Task]] =
            ZioWebSocketHandler()
              .flatMap { handler =>
                client.get.openWebsocket(request, handler)
              }
              .map(_.result)
        }
      )

  }

  private[realtime] def openWebsocket(request: Request[Either[String, String], Nothing]): ZIO[RealtimeClient, Throwable, WebSocket[Task]] =
    ZIO.accessM(_.get.openWebsocket(request))

}
