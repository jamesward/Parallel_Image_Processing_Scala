import java.io.File
import java.util.concurrent.{Executors, TimeUnit}

import com.sksamuel.scrimage.Image
import com.sksamuel.scrimage.filter.NoiseFilter

import scala.concurrent.{Await, BlockContext, ExecutionContext, Future}
import scala.concurrent.duration._

object Main extends App {

  val files = new File("images").listFiles().toSeq

  def doFilter(file: File): Image = {
    println(s"starting $file")
    val image = Image.fromFile(file).filter(NoiseFilter())
    println(s"finished $file")
    image
  }

  time {
    println("----------- futures (threads based on CPUs) ----------")

    implicit val ec = ExecutionContext.global

    val futures = files.map { file =>
      Future(doFilter(file))
    }

    Await.result(Future.sequence(futures), 5.minutes)
  }

  time {
    println("----------- futures (thread per file) ----------")

    val executor = Executors.newFixedThreadPool(files.size)
    implicit val ec = ExecutionContext.fromExecutor(executor)

    val futures = files.map { file =>
      Future(doFilter(file))
    }

    Await.result(Future.sequence(futures), 5.minutes)

    executor.shutdown()
  }

  time {
    println("------------------- sequential -----------------------")
    files.foreach(doFilter)
  }

  time {
    println("------------------- parallel -------------------------")
    files.par.foreach(doFilter)
  }

  def time[R](block: => R): Unit = {
    val t0 = System.nanoTime()
    val result = block
    val t1 = System.nanoTime()
    println("Elapsed time: " + Duration(t1 - t0, TimeUnit.NANOSECONDS).toSeconds + "s")
  }

}
