import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class OneHzGeneratortb extends AnyFlatSpec with ChiselScalatestTester {
  it should "correctly blink LED every 0.5s and emit 1Hz pulse every 1.0s" in {
    // テストベンチでは検証を高速化するため clkFreq = 10 (0.5秒相当 = 5サイクル, 1秒相当 = 10サイクル)
    test(new OneHzGenerator(10)).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      dut.io.clear.poke(false.B)
      dut.io.en.poke(true.B)

      // 初期状態: ledBlink = false, oneHzPulse = false
      dut.io.ledBlink.expect(false.B)
      dut.io.oneHzPulse.expect(false.B)

      // 1サイクル目〜5サイクル目 (前半0.5秒): ledBlink = false
      // cycle 4 (cnt=4) で halfPeriod-1 に到達し、次サイクル(cnt=5)で ledReg が反転して true になる
      for (c <- 0 until 4) {
        dut.io.ledBlink.expect(false.B)
        dut.io.oneHzPulse.expect(false.B)
        println(s"cycle $c (前半): led=${dut.io.ledBlink.peekBoolean()}, pulse=${dut.io.oneHzPulse.peekBoolean()}")
        dut.clock.step(1)
      }
      dut.clock.step(1) // cnt=4 -> cnt=5 遷移で反転

      // 5サイクル目〜9サイクル目 (後半0.5秒): ledBlink = true
      for (c <- 5 until 9) {
        dut.io.ledBlink.expect(true.B)
        dut.io.oneHzPulse.expect(false.B)
        println(s"cycle $c (後半): led=${dut.io.ledBlink.peekBoolean()}, pulse=${dut.io.oneHzPulse.peekBoolean()}")
        dut.clock.step(1)
      }

      // cycle 9 (cnt=9, fullPeriod-1) で oneHzPulse が 1 クロックだけ true になる
      dut.io.ledBlink.expect(true.B)
      dut.io.oneHzPulse.expect(true.B)
      println(s"cycle 9 (満了): led=${dut.io.ledBlink.peekBoolean()}, pulse=${dut.io.oneHzPulse.peekBoolean()}")

      // 次のクロックでラップアラウンドし、ledBlink は false に反転、oneHzPulse は false に戻る
      dut.clock.step(1)
      dut.io.ledBlink.expect(false.B)
      dut.io.oneHzPulse.expect(false.B)

      // clear の検証 (最優先)
      dut.clock.step(5) // カウントを進める
      dut.io.clear.poke(true.B)
      dut.clock.step(1)
      dut.io.ledBlink.expect(false.B)
      dut.io.oneHzPulse.expect(false.B)
    }
  }
}
