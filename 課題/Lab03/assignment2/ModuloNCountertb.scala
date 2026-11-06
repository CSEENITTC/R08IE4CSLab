import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class ModuloNCountertb extends AnyFlatSpec with ChiselScalatestTester {
  it should "correctly count modulo N every 1 second with clear, en, and runLED" in {
    test(new ModuloNCounter(n = 5, width = 4, clkFreq = 4)).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      // 1. カウント動作の検証 (4クロックごとに1秒進み、modRegが 0 -> 1 -> 2 -> 3 -> 4 -> 0 と巡回)
      dut.io.clear.poke(false.B)
      dut.io.en.poke(true.B)

      for (i <- 0 until 40) {
        val expectedMod = (i / 4) % 5
        val expectedCount = (~expectedMod) & 0xF // 負論理LED出力
        dut.io.count.expect(expectedCount.U)
        println(s"cycle $i: count=${dut.io.count.peekInt()} (mod=$expectedMod), runLED=${dut.io.runLED.peekBoolean()}")
        dut.clock.step(1)
      }

      // 2. en 無効時の保持検証 (en=false でクロックが進んでもカウント値保持)
      dut.io.en.poke(false.B)
      val heldCount = dut.io.count.peekInt()
      dut.clock.step(8)
      dut.io.count.expect(heldCount.U)
      println(s"en=false held: count=${dut.io.count.peekInt()}")

      // 3. clear (最優先) の検証 (クリアで modReg=0 -> count=15 (~0.U) にリセット)
      dut.io.clear.poke(true.B)
      dut.io.en.poke(true.B)
      dut.clock.step(1)
      dut.io.count.expect(15.U) // ~0.U(4.W) = 15
      dut.io.runLED.expect(true.B) // ~false.B = true
      println(s"clear asserted: count=${dut.io.count.peekInt()} (reset to 0)")
    }
  }
}
