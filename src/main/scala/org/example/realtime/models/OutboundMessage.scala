package org.example.realtime.models

sealed trait OutboundMessage

case class SendText(text: String) extends OutboundMessage
