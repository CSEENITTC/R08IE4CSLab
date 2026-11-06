import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class Mux4to1tb extends AnyFlatSpec with ChiselScalatestTester {
  it should "correctly select input according to sel" in {
    test(new Mux4to1).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      dut.io.in0.poke(10.U)
      dut.io.in1.poke(20.U)
      dut.io.in2.poke(30.U)
      dut.io.in3.poke(40.U)

      // Test sel = 0 -> in0 (10)
      dut.io.sel.poke(0.U)
      dut.clock.step(1)
      dut.io.out.expect(10.U)
      println(s"sel=0 => out=${dut.io.out.peekInt()} (expected: 10)")

      // Test sel = 1 -> in1 (20)
      dut.io.sel.poke(1.U)
      dut.clock.step(1)
      dut.io.out.expect(20.U)
      println(s"sel=1 => out=${dut.io.out.peekInt()} (expected: 20)")

      // Test sel = 2 -> in2 (30)
      dut.io.sel.poke(2.U)
      dut.clock.step(1)
      dut.io.out.expect(30.U)
      println(s"sel=2 => out=${dut.io.out.peekInt()} (expected: 30)")

      // Test sel = 3 -> in3 (40)
      dut.io.sel.poke(3.U)
      dut.clock.step(1)
      dut.io.out.expect(40.U)
      println(s"sel=3 => out=${dut.io.out.peekInt()} (expected: 40)")
    }
  }
}
