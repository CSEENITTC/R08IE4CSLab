import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class SelfCorrectingRingCountertb extends AnyFlatSpec with ChiselScalatestTester {
  it should "cycle through 1-hot states and recover from illegal states" in {
    test(new SelfCorrectingRingCounter(4)).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      dut.io.en.poke(true.B)
      val expectedPattern = Seq("b0001", "b0010", "b0100", "b1000")

      // 正常な1-hot巡回の検証
      for (step <- 0 until 8) {
        val expected = expectedPattern(step % 4)
        println(s"step=$step => out=b${dut.io.out.peekInt().toInt.toBinaryString} (expected: $expected)")
        dut.io.out.expect(expected.U(4.W))
        dut.clock.step(1)
      }
    }
  }
}
