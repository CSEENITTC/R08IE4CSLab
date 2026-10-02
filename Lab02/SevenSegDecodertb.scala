import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class SevenSegDecodertb extends AnyFlatSpec with ChiselScalatestTester {
  it should "correctly decode BCD to 7-segment display patterns" in {
    val expectedPatterns = Map(
      0  -> "b1111110", // 0: abcdef
      1  -> "b0110000", // 1: bc
      2  -> "b1101101", // 2: abdeg
      3  -> "b1111001", // 3: abcdg
      4  -> "b0110011", // 4: bcfg
      5  -> "b1011011", // 5: acdfg
      6  -> "b1011111", // 6: acdefg
      7  -> "b1110000", // 7: abc
      8  -> "b1111111", // 8: abcdefg
      9  -> "b1111011", // 9: abcdfg
      10 -> "b0000000", // invalid -> turn off
      11 -> "b0000000",
      12 -> "b0000000",
      13 -> "b0000000",
      14 -> "b0000000",
      15 -> "b0000000"
    )

    test(new SevenSegDecoder).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      for ((bcdVal, binStr) <- expectedPatterns) {
        dut.io.bcd.poke(bcdVal.U)
        dut.clock.step(1)
        val segBin = String.format("%7s", dut.io.seg.peekInt().toInt.toBinaryString).replace(' ', '0')
        println(f"bcd=$bcdVal%2d => seg=b$segBin%s (expected: $binStr%s)")
        dut.io.seg.expect(binStr.U(7.W))
      }
    }
  }
}
