import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class DigitalClockCoretb extends AnyFlatSpec with ChiselScalatestTester {
  it should "cascade seconds to minutes to hours synchronously" in {
    test(new DigitalClockCore) { dut =>
      dut.io.clear.poke(false.B)
      dut.io.enSec.poke(true.B)
      dut.clock.setTimeout(0)

      for (_ <- 0 until 999) {
        for (_ <- 0 until 999) {
          dut.clock.step(1)
        }
        println(s"Simulated time: ${dut.io.hour.peekInt()}:${dut.io.min.peekInt()}:${dut.io.sec.peekInt()}")
      }
    }
  }
}
