package com.northstarr.alist

import zio.*

object Alist extends App:
  val program =
    for
      _ <- console.putStrLn("─" * 100)

      _ <- console.putStrLn("hello world")

      _ <- console.putStrLn("─" * 100)
    yield ()

  override def run(args: List[String]) =
    program.exitCode
