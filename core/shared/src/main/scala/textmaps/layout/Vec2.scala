package textmaps.layout

case class Vec2(x: Double, y: Double):
  def +(o: Vec2): Vec2  = Vec2(x + o.x, y + o.y)
  def -(o: Vec2): Vec2  = Vec2(x - o.x, y - o.y)
  def *(s: Double): Vec2 = Vec2(x * s, y * s)

object Vec2:
  val zero: Vec2 = Vec2(0, 0)

  def polar(angleDeg: Double, dist: Double): Vec2 =
    val r = math.toRadians(angleDeg)
    Vec2(dist * math.cos(r), dist * math.sin(r))
