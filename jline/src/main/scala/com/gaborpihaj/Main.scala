package com.gaborpihaj

import org.jline.terminal.TerminalBuilder
import org.jline.keymap.BindingReader
import java.io.PrintWriter
import cats.data.Chain
import cats.syntax.traverse._

object TerminalControl {
  def up(n: Int = 1): String = Bytes.csi(n.toString().getBytes() ++ Array('A'.toByte)).map(_.toChar).mkString

  def down(n: Int = 1) = esc(s"${n}B")

  def forward(n: Int = 1) = esc(s"${n}C")

  def back(n: Int = 1) = esc(s"${n}D")

  def clearScreen() = esc("2J")

  def clearLine() = esc("0K")

  def move(row: Int, column: Int) = esc(s"${row};${column}H")

  def esc(s: String): String = s"\u001b[$s"
}

object Bytes {
  val escape: Byte = 27.toByte
  val leftSquareBracket: Byte = 91.toByte
  val CSI: Array[Byte] = Array(escape, leftSquareBracket)

  val leftArrowKey = csi(68.toByte)
  val rightArrowKey = csi(67.toByte)

  def csi(parameters: Byte*): Array[Byte] = CSI ++ parameters.toArray
  def csi(parameters: Array[Byte]): Array[Byte] = CSI ++ parameters
 
  //def csi(parameter: String): List = CSI.map(_.toChar).mkString
}


object Main extends App {
  val terminal = TerminalBuilder.builder()
    .system(true)
    .jansi(true)
    .build()

  val writer = terminal.writer()
  val reader = new BindingReader(terminal.reader())

  import TerminalControl._

  println(clearScreen() + move(1, 1))
  println("start...")
  println(move(terminal.getHeight(), 1))
  val prompt = "prompt here > "
  print(prompt)

  terminal.enterRawMode()
  terminal.echo(false)

  def printHistory[A](ls: Chain[A], startLine: Int, writer: PrintWriter): Unit =
    ls.zipWithIndex.foldLeft(()) {
      case (_, (a, index)) =>
        writer.print(move(startLine - index, 1)  + clearLine() + s"char: $a")
    }
  
  def readSequence(reader: BindingReader)(s: Chain[Int]): Chain[Int] = s match {
    case Chain(27)  => readSequence(reader)(s :+ reader.readCharacter())
    case Chain(27, 91) => readSequence(reader)(s :+ reader.readCharacter())
    case l => l
  }

  def debug[A](f: A => Unit): A => A = a => f.andThen(_ => a)(a)

  type ByteSeq = Chain[Int]
  type LineReaderState = (Chain[ByteSeq], Int, String)

  def keyPress(write: (String) => Unit, row: Int, promptLength: Int)(state: LineReaderState, byteSeq: ByteSeq): LineReaderState = {
    val (oldHistory, oldCursor, oldStr) = state
    val history = Chain.one(byteSeq) ++ oldHistory

    def home() = {
        write(move(row, promptLength))
        (history, 0, oldStr)
    }

    def end() = {
      val cursor = oldStr.length()
      write(move(row, promptLength + cursor))
      (history, cursor, oldStr)
    }

    byteSeq match {
      case Chain(27, 91, 68) if oldCursor > 0 =>
        write(back())
        (history, oldCursor - 1, oldStr)

      case Chain(27, 91, 67) if oldCursor < oldStr.length =>
        write(forward())
        (history, oldCursor + 1, oldStr)

      case Chain(27, 91, 70) => end()
      case Chain(5) => end()

      case Chain(27, 91, 72) => home()
      case Chain(1) => home()

      case Chain(c) if ((32 <= c && c <= 126) || 127 < c) =>
        val (front, back) = oldStr.splitAt(oldCursor)
        write(clearLine() + c.toChar + back + move(row, promptLength + oldCursor + 1))
        val newStr = front + c.toChar + back
        (history, oldCursor + 1, newStr)

      case Chain(127) if oldCursor > 0 => // Backspace
        val (old, back) = oldStr.splitAt(oldCursor)
        val front = old.dropRight(1)
        write(move(row, promptLength + oldCursor - 1) + clearLine() + back + move(row, promptLength + oldCursor - 1))
        val newStr = front + back
        (history, oldCursor - 1, newStr)

      case Chain(27, 91, 51) => // Delete
        val (front, old) = oldStr.splitAt(oldCursor)
        val back = old.drop(1)
        write(move(row, promptLength + oldCursor) + clearLine() + back + move(row, promptLength + oldCursor))
        val newStr = front + back
        (history, oldCursor, newStr)

      case _ => (history, oldCursor, oldStr)
    }
  }

  val terminalKeyPress: (LineReaderState, ByteSeq) => LineReaderState = keyPress(
    s => {writer.write(s); terminal.flush()},
    terminal.getCursorPosition(_ => ()).getY() + 1,
    prompt.length() + 1
  )
  val (_, _, str) = LazyList.continually(reader.readCharacter()).takeWhile(_ != 13)
    .map(chr => Chain.one(chr))
    .map(readSequence(reader))
    .foldLeft[LineReaderState]((Chain.empty[Chain[Int]], 0, "")) { (state, byteSeq) => 
      debug[LineReaderState]({
        case (history, _, _) =>
          val cursor = terminal.getCursorPosition(_ => ())
          val row = cursor.getY() + 1
          val col = cursor.getX() + 1

          printHistory(history.zipWithIndex.takeWhile({case (_, index) => index < terminal.getHeight() - 4}).map(_._1), row - 3, writer)
          writer.print(move(row, col))
          terminal.flush()
      })(terminalKeyPress(state, byteSeq))
    }

  writer.print(clearScreen() + move(1, 1) + s"String read: $str")
  writer.print(move(3, 1))
  terminal.flush()
  terminal.close()
}
