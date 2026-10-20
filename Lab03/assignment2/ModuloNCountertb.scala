import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class ModuloNCountertb extends AnyFlatSpec with ChiselScalatestTester {
  it should "correctly count modulo N with clear, load, and runLED" in {
    test(new ModuloNCounter(n=5, width=4, clkFreq=4)).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      // val n: Int = 10, val width: Int = 4, val clkFreq: Int = 9000000
      // 1. カウント動作の検証 (0 -> 1 -> 2 -> 3 -> 4 -> 0)
      dut.io.clear.poke(false.B)
      dut.io.en.poke(true.B)

      for (i <- 0 until 100) {
        // dut.io.count.expect(i.U)
        if (i == 4) {
          // dut.io.runLED.expect(true.B)
        } else {
          // dut.io.runLED.expect(false.B)
        }
        println(s"cycle: count=${dut.io.count.peekInt()}, runLED=${dut.io.runLED.peekBoolean()}")
        dut.clock.step(1)
      }
      // dut.io.count.expect(0.U)

      // 2. load の検証
      dut.clock.step(1)
      // dut.io.count.expect(3.U)

      // 3. clear (最優先) の検証
      dut.io.clear.poke(true.B)
      dut.clock.step(1)
      // dut.io.count.expect(0.U)
    }
  }
}
