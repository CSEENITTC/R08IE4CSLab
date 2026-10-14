import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class Counter60tb extends AnyFlatSpec with ChiselScalatestTester {
  it should "correctly count from 00 to 59 synchronously without ripple clock" in {
    test(new Counter60).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      dut.io.clear.poke(false.B)
      dut.io.en.poke(true.B)

      for (sec <- 0 until 60) {
        val expUnits = sec % 10
        val expTens  = sec / 10
        dut.io.secUnits.expect(expUnits.U)
        dut.io.secTens.expect(expTens.U)
        if (sec == 59) {
          dut.io.cout.expect(true.B)
        } else {
          dut.io.cout.expect(false.B)
        }
        dut.clock.step(1)
      }
      // 59 の次は 00 に同期復帰
      dut.io.secUnits.expect(0.U)
      dut.io.secTens.expect(0.U)
    }
  }
}
