import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class GrayCodeCountertb extends AnyFlatSpec with ChiselScalatestTester {
  it should "guarantee hamming distance of 1 between consecutive states" in {
    test(new GrayCodeCounter).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      dut.io.clear.poke(false.B)
      dut.io.en.poke(true.B)

      var prevGray = dut.io.gray.peekInt().toInt
      for (i <- 0 until 15) {
        dut.clock.step(1)
        val curGray = dut.io.gray.peekInt().toInt
        val diff = Integer.bitCount(prevGray ^ curGray)
        println(s"bin=${dut.io.bin.peekInt()} => gray=0b${curGray.toBinaryString}, hammingDiff=$diff")
        assert(diff == 1, s"Hamming distance was not 1 at step $i")
        prevGray = curGray
      }
    }
  }
}
