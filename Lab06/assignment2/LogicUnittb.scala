import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class LogicUnittb extends AnyFlatSpec with ChiselScalatestTester {
  "LogicUnit" should "pass" in {
    test(new LogicUnit).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      dut.io.a.poke("hF0".U)
      dut.io.b.poke("h3C".U)

      // 0: AND (0xF0 & 0x3C = 0x30)
      dut.io.op.poke(0.U)
      dut.clock.step(1)
      dut.io.res.expect("h30".U)

      // 1: OR (0xF0 | 0x3C = 0xFC)
      dut.io.op.poke(1.U)
      dut.clock.step(1)
      dut.io.res.expect("hFC".U)

      // 2: XOR (0xF0 ^ 0x3C = 0xCC)
      dut.io.op.poke(2.U)
      dut.clock.step(1)
      dut.io.res.expect("hCC".U)

      // 3: NOT a (~0xF0 = 0x0F)
      dut.io.op.poke(3.U)
      dut.clock.step(1)
      dut.io.res.expect("h0F".U)
    }
  }
}
