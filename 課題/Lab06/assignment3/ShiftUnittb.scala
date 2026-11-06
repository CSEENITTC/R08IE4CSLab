import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class ShiftUnittb extends AnyFlatSpec with ChiselScalatestTester {
  "ShiftUnit" should "pass" in {
    test(new ShiftUnit).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      // 1. 論理左シフト (op = false.B): 0x05 << 2 = 0x14
      dut.io.a.poke("h05".U)
      dut.io.shamt.poke(2.U)
      dut.io.op.poke(false.B)
      dut.clock.step(1)
      dut.io.res.expect("h14".U)

      // 2. 論理左シフト (上位ビットあふれ切り捨て確認): 0xB4 << 3 = 0xA0
      dut.io.a.poke("hB4".U)
      dut.io.shamt.poke(3.U)
      dut.io.op.poke(false.B)
      dut.clock.step(1)
      dut.io.res.expect("hA0".U)

      // 3. 論理右シフト (op = true.B): 0xB4 >> 2 = 0x2D
      dut.io.a.poke("hB4".U)
      dut.io.shamt.poke(2.U)
      dut.io.op.poke(true.B)
      dut.clock.step(1)
      dut.io.res.expect("h2D".U)

      // 4. 0ビットシフト確認: 0xA5 >> 0 = 0xA5
      dut.io.a.poke("hA5".U)
      dut.io.shamt.poke(0.U)
      dut.io.op.poke(true.B)
      dut.clock.step(1)
      dut.io.res.expect("hA5".U)
    }
  }
}
