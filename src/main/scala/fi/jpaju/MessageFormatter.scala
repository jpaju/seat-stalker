package fi.jpaju

import java.time.format.DateTimeFormatter

object MessageFormatter:
  private val dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm d.M.yyyy");

  def seatsAvailableMessage(restaurant: Restaurant, availableSeats: List[AvailableSeat]): String =
    val seats = availableSeats
      .map(formatAvailableSeat)
      .mkString("\n - ")

    s"Free seats available in ${restaurant.name} 🍔:\n - $seats"

  private def formatAvailableSeat(seat: AvailableSeat): String =
    val formattedTime = seat.time.format(dateTimeFormatter)
    s"${seat.seatCount} seats at $formattedTime"
