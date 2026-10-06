import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class SequenceDetector101tb extends AnyFlatSpec with ChiselScalatestTester {
  it should "detect overlapping 101 sequences with Moore outputs" in {
    test(new SequenceDetector101).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      // 入力系列: 1 -> 0 -> 1 -> 0 -> 1
      val inputs   = Seq(true, false, true, false, true, false)
      val expected = Seq(false, false, true, false, true, false)

      for (i <- inputs.indices) {
        dut.io.in.poke(inputs(i).B)
        dut.clock.step(1)
        println(s"step $i: in=${inputs(i)} => detected=${dut.io.detected.peekBoolean()} (expected: ${expected(i)})")
        dut.io.detected.expect(expected(i).B)
      }
    }
  }
}
