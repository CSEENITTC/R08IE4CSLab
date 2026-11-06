import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class Counter60tb extends AnyFlatSpec with ChiselScalatestTester {
  it should "correctly count from 00 to 59 synchronously using OneHzGenerator, mod-10, and mod-6 counters" in {
    val testClkFreq = 4 // シミュレーション高速化のため 4クロック = 1秒 とする
    test(new Counter60(clkFreq = testClkFreq)).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      dut.io.clear.poke(false.B)
      dut.io.en.poke(true.B)

      for (sec <- 0 until 60) {
        val expUnits = sec % 10
        val expTens  = sec / 10

        for (cycle <- 0 until testClkFreq) {
          dut.io.secUnits.expect(expUnits.U)
          dut.io.secTens.expect(expTens.U)
          // 59秒目の最後の1クロック (oneHzPulse が High になるサイクル) でのみ cout が true.B
          if (sec == 59 && cycle == testClkFreq - 1) {
            dut.io.cout.expect(true.B)
          } else {
            dut.io.cout.expect(false.B)
          }
          dut.clock.step(1)
        }
      }
      // 59秒の次は 00秒 に同期復帰
      dut.io.secUnits.expect(0.U)
      dut.io.secTens.expect(0.U)
      dut.io.cout.expect(false.B)
    }
  }
}
