import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class ArithmeticUnittb extends AnyFlatSpec with ChiselScalatestTester {
  "ArithmeticUnit" should "pass" in {
    test(new ArithmeticUnit).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      // 1. ADD: 10 + 20 = 30 (cout = false)
      dut.io.a.poke(10.U)
      dut.io.b.poke(20.U)
      dut.io.op.poke(false.B)
      dut.clock.step(1)
      dut.io.res.expect(30.U)
      dut.io.cout.expect(false.B)

      // 2. ADD with Carry: 200 + 100 = 300 -> 44 (cout = true)
      dut.io.a.poke(200.U)
      dut.io.b.poke(100.U)
      dut.io.op.poke(false.B)
      dut.clock.step(1)
      dut.io.res.expect(44.U)
      dut.io.cout.expect(true.B)

      // 3. SUB: 50 - 20 = 30 (cout = false)
      dut.io.a.poke(50.U)
      dut.io.b.poke(20.U)
      dut.io.op.poke(true.B)
      dut.clock.step(1)
      dut.io.res.expect(30.U)
      dut.io.cout.expect(false.B)

      // 4. SUB with Borrow: 10 - 20 = 246 (cout = true)
      dut.io.a.poke(10.U)
      dut.io.b.poke(20.U)
      dut.io.op.poke(true.B)
      dut.clock.step(1)
      dut.io.res.expect(246.U)
      dut.io.cout.expect(true.B)
    }
  }
}
