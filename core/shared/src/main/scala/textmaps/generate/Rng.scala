package textmaps.generate

/** Seedable LCG — pure, no side effects, no JS Math.random(). */
class Rng(seed: Long):
  private var state: Long = seed ^ 0x5DEECE66DL

  def nextLong(): Long =
    state = (state * 0x5DEECE66DL + 0xBL) & 0xFFFFFFFFFFFFFFL
    state

  def nextInt(bound: Int): Int = ((nextLong() >>> 17) % bound.toLong).toInt.abs

  // state is masked to 56 bits (see above); >>> 3 keeps the full 53 bits nextDouble needs.
  def nextDouble(): Double = (nextLong() >>> 3).toDouble / (1L << 53).toDouble

  def nextDoubleIn(lo: Double, hi: Double): Double = lo + nextDouble() * (hi - lo)
