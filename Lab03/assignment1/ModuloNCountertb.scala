import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class ModuloNCountertb extends AnyFlatSpec with ChiselScalatestTester {
  it should "correctly count modulo N with clear, load, and rollover" in {
    test(new ModuloNCounter(5, 3)).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      // 1. カウント動作の検証 (0 -> 1 -> 2 -> 3 -> 4 -> 0)
      dut.io.clear.poke(false.B)
      dut.io.load.poke(false.B)
      dut.io.en.poke(true.B)

      for (i <- 0 until 5) {
        dut.io.count.expect(i.U)
        if (i == 4) {
          dut.io.rollover.expect(true.B)
        } else {
          dut.io.rollover.expect(false.B)
        }
        println(s"cycle: count=${dut.io.count.peekInt()}, rollover=${dut.io.rollover.peekBoolean()}")
        dut.clock.step(1)
      }
      dut.io.count.expect(0.U)

      // 2. load の検証
      dut.io.load.poke(true.B)
      dut.io.loadData.poke(3.U)
      dut.clock.step(1)
      dut.io.load.poke(false.B)
      dut.io.count.expect(3.U)

      // 3. clear (最優先) の検証
      dut.io.clear.poke(true.B)
      dut.clock.step(1)
      dut.io.count.expect(0.U)
    }
  }
}
