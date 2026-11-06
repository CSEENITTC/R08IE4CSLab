import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class PwmGeneratortb extends AnyFlatSpec with ChiselScalatestTester {
  it should "correctly generate PWM pulses according to duty and period" in {
    test(new PwmGenerator(10)).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      dut.io.period.poke(4.U) // 周期 = 5 (0, 1, 2, 3, 4)
      dut.io.duty.poke(2.U)   // デューティ比 = 2/5 (0, 1 で true)
      dut.io.en.poke(true.B)

      for (cycle <- 0 until 10) {
        val expected = (cycle % 5) < 2
        println(s"cycle=$cycle => pwmOut=${dut.io.pwmOut.peekBoolean()} (expected: $expected)")
        dut.io.pwmOut.expect(expected.B)
        dut.clock.step(1)
      }

      // en = false 時の停止検証
      dut.io.en.poke(false.B)
      dut.clock.step(1)
      dut.io.pwmOut.expect(false.B)
    }
  }
}
