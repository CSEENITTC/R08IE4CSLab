import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class TrafficLightControllertb extends AnyFlatSpec with ChiselScalatestTester {
  it should "safely transition lights upon pedestrian request" in {
    test(new TrafficLightController).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      dut.io.pedestrianButton.poke(false.B)
      dut.clock.step(1)
      dut.io.mainLight.expect(0.U) // 車道青
      dut.io.pedLight.expect(false.B) // 歩行者赤

      // ボタン押下
      dut.io.pedestrianButton.poke(true.B)
      dut.clock.step(1)
      dut.io.pedestrianButton.poke(false.B)

      // タイマ経過後に順次遷移することを確認
      for (step <- 0 until 25) {
        println(s"step $step: mainLight=${dut.io.mainLight.peekInt()}, pedLight=${dut.io.pedLight.peekBoolean()}")
        dut.clock.step(1)
      }
    }
  }
}
