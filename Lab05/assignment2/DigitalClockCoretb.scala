import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class DigitalClockCoretb extends AnyFlatSpec with ChiselScalatestTester {
  it should "cascade seconds to minutes to hours synchronously" in {
    test(new DigitalClockCore).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      dut.io.clear.poke(false.B)
      dut.io.enSec.poke(true.B)

      // 60秒進めて1分になることを確認
      for (_ <- 0 until 60) {
        dut.clock.step(1)
      }
      dut.io.sec.expect(0.U)
      dut.io.min.expect(1.U)
      println(s"After 60 steps: ${dut.io.hour.peekInt()}:${dut.io.min.peekInt()}:${dut.io.sec.peekInt()}")
    }
  }
}
