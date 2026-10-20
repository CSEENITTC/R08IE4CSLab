import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class ModuloNCountertb extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "ModuloNCounter"

  it should "correctly count modulo N with clear, en, and rollover" in {
    test(new ModuloNCounter(5, 3)).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      // 1. カウント動作の検証 (0 -> 1 -> 2 -> 3 -> 4 -> 0)
      dut.io.clear.poke(false.B)
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

      // 2. en 無効時の保持検証 (en=false のとき値が保持され、rollover も出ない)
      dut.io.en.poke(false.B)
      dut.clock.step(3)
      dut.io.count.expect(0.U)
      dut.io.rollover.expect(false.B)

      // 3. 1秒パルス模倣検証 (en を1サイクルだけ High にしてカウントアップ)
      println("--- 1Hz pulse emulation ---")
      for (expected <- 1 to 4) {
        // パルスなし (クロックだけ進む)
        dut.io.en.poke(false.B)
        dut.clock.step(2)
        // 1秒パルス到来 (1サイクルだけ en=true)
        dut.io.en.poke(true.B)
        dut.clock.step(1)
        dut.io.count.expect(expected.U)
        println(s"1-sec tick: count=${dut.io.count.peekInt()}")
      }

      // 4. clear (最優先) の検証 (en=true の状態でも clear が優先され 0 になる)
      dut.io.clear.poke(true.B)
      dut.io.en.poke(true.B)
      dut.clock.step(1)
      dut.io.count.expect(0.U)
      println("clear asserted: count successfully reset to 0")
    }
  }
}
