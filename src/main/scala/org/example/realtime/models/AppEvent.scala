package org.example.realtime.models

sealed trait AppEvent extends Product with Serializable

final case class StringResponse(value: String) extends AppEvent