package org.example.api

import org.example.realtime.{ CustomMessaging, RealtimeClient }
import sttp.client.Request
import sttp.client.ws.WebSocket
import zio.{ Task, ZIO }

object realtime extends RealtimeClient.Service[RealtimeClient] with CustomMessaging.Service[RealtimeClient] {

  override def openWebsocket(request: Request[Either[String, String], Nothing]): ZIO[RealtimeClient, Throwable, WebSocket[Task]] =
    ZIO.accessM(_.realtimeClient.openWebsocket(request))
}
